package dev.youtubedisks.client.playback;

import dev.youtubedisks.YoutubeDisks;
import dev.youtubedisks.client.audio.DiscAudioPlayer;
import dev.youtubedisks.client.cache.ClientDiscCache;
import dev.youtubedisks.disc.DiscData;
import dev.youtubedisks.network.DiscDataChunkPayload;
import dev.youtubedisks.network.DiscDataEndPayload;
import dev.youtubedisks.network.RequestDiscDataPayload;
import net.minecraft.core.BlockPos;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Orchestrates client-side playback:
 * <ul>
 *     <li>{@link #toggle(DiscData)} — right-click the disc in hand (head-anchored)</li>
 *     <li>{@link #startSpeakerPlay(String, BlockPos)} — speaker started our disc (positional)</li>
 * </ul>
 * Concurrent requests for the same discId share a single download accumulator; all registered
 * targets play when bytes arrive.
 */
public final class ClientPlaybackController {

    private static final Map<String, PendingDownload> PENDING = new ConcurrentHashMap<>();

    // ------------------------------------------------------------------ entry points

    public static void toggle(DiscData data) {
        String discId = data.discId();

        if (DiscAudioPlayer.isPlaying(discId)) {
            DiscAudioPlayer.stop(discId);
            return;
        }

        if (PENDING.remove(discId) != null) {
            YoutubeDisks.LOGGER.info("[playback] cancelled pending {}", discId);
            return;
        }

        ClientDiscCache cache = ClientDiscCache.get();
        if (cache.has(discId)) {
            try {
                byte[] ogg = cache.load(discId);
                DiscAudioPlayer.play(discId, ogg);
                return;
            } catch (IOException e) {
                YoutubeDisks.LOGGER.error("[playback] cache read failed for {}", discId, e);
            }
        }

        PendingDownload pending = new PendingDownload();
        pending.targets.add(new PlaybackTarget.HandAnchored());
        PENDING.put(discId, pending);
        YoutubeDisks.LOGGER.info("[playback] requesting {} from server (hand)", discId);
        PacketDistributor.sendToServer(new RequestDiscDataPayload(discId));
    }

    public static void startSpeakerPlay(String discId, BlockPos pos) {
        // No early-return on "already playing" here. The previous track was just stopped via
        // Minecraft.execute(...) which is async — the AL source might still be in the map
        // when this runs. DiscAudioPlayer.playAt handles replacement internally
        // (startPositionalSource → stopPositionalSource(pos) first), so a double start is safe.
        ClientDiscCache cache = ClientDiscCache.get();
        if (cache.has(discId)) {
            try {
                byte[] ogg = cache.load(discId);
                DiscAudioPlayer.playAt(discId, ogg, pos);
                return;
            } catch (IOException e) {
                YoutubeDisks.LOGGER.error("[playback] cache read failed for {}", discId, e);
            }
        }

        PendingDownload pending = PENDING.computeIfAbsent(discId, k -> new PendingDownload());
        boolean wasEmpty = pending.targets.isEmpty();
        pending.targets.add(new PlaybackTarget.PositionalAt(pos));
        if (wasEmpty) {
            YoutubeDisks.LOGGER.info("[playback] requesting {} from server (positional @ {})", discId, pos);
            PacketDistributor.sendToServer(new RequestDiscDataPayload(discId));
        }
    }

    // ------------------------------------------------------------------ network handlers

    public static void handleChunk(DiscDataChunkPayload payload, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            PendingDownload pending = PENDING.get(payload.discId());
            if (pending == null) return;
            pending.buffer.write(payload.data(), 0, payload.data().length);
        });
    }

    public static void handleEnd(DiscDataEndPayload payload, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            PendingDownload pending = PENDING.remove(payload.discId());
            if (pending == null) return;
            if (!payload.ok()) {
                YoutubeDisks.LOGGER.warn("[playback] server refused {}: {}", payload.discId(), payload.error());
                return;
            }
            byte[] ogg = pending.buffer.toByteArray();
            try {
                ClientDiscCache.get().store(payload.discId(), ogg);
            } catch (IOException e) {
                YoutubeDisks.LOGGER.warn("[playback] cache write failed for {}", payload.discId(), e);
            }

            for (PlaybackTarget target : pending.targets) {
                switch (target) {
                    case PlaybackTarget.HandAnchored ignored ->
                        DiscAudioPlayer.play(payload.discId(), ogg);
                    case PlaybackTarget.PositionalAt at ->
                        DiscAudioPlayer.playAt(payload.discId(), ogg, at.pos());
                }
            }
        });
    }

    // ------------------------------------------------------------------ helpers

    private static final class PendingDownload {
        final ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        final List<PlaybackTarget> targets = new ArrayList<>();
    }

    sealed interface PlaybackTarget {
        record HandAnchored() implements PlaybackTarget {}
        record PositionalAt(BlockPos pos) implements PlaybackTarget {}
    }

    private ClientPlaybackController() {}
}
