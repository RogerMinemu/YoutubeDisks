package dev.youtubedisks.server.upload;

import dev.youtubedisks.YoutubeDisks;
import dev.youtubedisks.disc.DiscData;
import dev.youtubedisks.disc.DiscStorage;
import dev.youtubedisks.network.RecordingBeginPayload;
import dev.youtubedisks.network.RecordingChunkPayload;
import dev.youtubedisks.network.RecordingEndPayload;
import dev.youtubedisks.network.RecordingResultPayload;
import dev.youtubedisks.registry.ModDataComponents;
import dev.youtubedisks.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.io.ByteArrayOutputStream;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Receives the chunked C2S upload of a freshly-recorded .ogg, accumulates the payload server-side,
 * stores it in the world's disc cache, and hands the {@code recorded_disk} ItemStack back to the
 * uploading player.
 */
public final class ServerUploadHandler {

    /** Hard cap to defend against a malicious client trying to fill the disk. */
    public static final int MAX_TOTAL_BYTES = 10 * 1024 * 1024; // 10 MB

    /** Hard cap on metadata strings to keep memory bounded. */
    private static final int MAX_TITLE_LENGTH = 256;

    private static final Map<UploadKey, UploadSession> SESSIONS = new ConcurrentHashMap<>();

    public static void handleBegin(RecordingBeginPayload payload, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (!(ctx.player() instanceof ServerPlayer sp)) {
                return;
            }
            if (payload.totalBytes() <= 0 || payload.totalBytes() > MAX_TOTAL_BYTES) {
                sendResult(sp, payload.pos(), false, "size_invalid");
                return;
            }
            String title = payload.title() == null ? "" : payload.title();
            if (title.length() > MAX_TITLE_LENGTH) {
                title = title.substring(0, MAX_TITLE_LENGTH);
            }
            UploadKey key = new UploadKey(sp.getUUID(), payload.pos());
            SESSIONS.put(key, new UploadSession(title, payload.durationSeconds(), payload.totalBytes()));
            YoutubeDisks.LOGGER.info("[upload] begin {} bytes from {} at {}", payload.totalBytes(), sp.getName().getString(), payload.pos());
        });
    }

    public static void handleChunk(RecordingChunkPayload payload, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (!(ctx.player() instanceof ServerPlayer sp)) {
                return;
            }
            UploadKey key = new UploadKey(sp.getUUID(), payload.pos());
            UploadSession session = SESSIONS.get(key);
            if (session == null) {
                YoutubeDisks.LOGGER.warn("[upload] chunk seq={} without active session from {}", payload.seq(), sp.getUUID());
                return;
            }
            if (!session.appendChunk(payload.data())) {
                SESSIONS.remove(key);
                sendResult(sp, payload.pos(), false, "size_exceeded");
            }
        });
    }

    public static void handleEnd(RecordingEndPayload payload, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (!(ctx.player() instanceof ServerPlayer sp)) {
                return;
            }
            UploadKey key = new UploadKey(sp.getUUID(), payload.pos());
            UploadSession session = SESSIONS.remove(key);
            if (session == null) {
                sendResult(sp, payload.pos(), false, "no_active_session");
                return;
            }

            byte[] ogg = session.toBytes();
            try {
                DiscStorage storage = new DiscStorage(sp.serverLevel().getServer());
                String discId = storage.store(ogg);

                // Route by what block triggered the recording. Speaker → auto-add to its
                // playlist (don't clutter the player's inventory). DiskRecorder (or anything
                // else) → hand the player a portable recorded_disk item.
                var be = sp.serverLevel().getBlockEntity(payload.pos());
                if (be instanceof dev.youtubedisks.speaker.SpeakerBlockEntity speaker) {
                    speaker.addTrack(new dev.youtubedisks.speaker.Track(discId, session.title(), session.durationSeconds()));
                    YoutubeDisks.LOGGER.info("[upload] auto-added {} to speaker @ {} for {}",
                        discId, payload.pos(), sp.getName().getString());
                } else {
                    ItemStack disc = new ItemStack(ModItems.RECORDED_DISK.get());
                    disc.set(
                        ModDataComponents.DISC_DATA.get(),
                        new DiscData(discId, session.title(), session.durationSeconds())
                    );

                    boolean added = sp.getInventory().add(disc);
                    if (!added) {
                        sp.drop(disc, false);
                    }
                }

                YoutubeDisks.LOGGER.info("[upload] finalized {} ({} bytes) for {}", discId, ogg.length, sp.getName().getString());
                sendResult(sp, payload.pos(), true, discId);
            } catch (Exception e) {
                YoutubeDisks.LOGGER.error("[upload] finalize failed for {}", sp.getName().getString(), e);
                String msg = e.getMessage() == null ? "io_error" : e.getMessage();
                sendResult(sp, payload.pos(), false, msg);
            }
        });
    }

    private static void sendResult(ServerPlayer sp, BlockPos pos, boolean ok, String message) {
        PacketDistributor.sendToPlayer(sp, new RecordingResultPayload(pos, ok, message));
    }

    private record UploadKey(UUID player, BlockPos pos) {}

    private static final class UploadSession {
        private final String title;
        private final int durationSeconds;
        private final int expectedSize;
        private final ByteArrayOutputStream buffer = new ByteArrayOutputStream();

        UploadSession(String title, int durationSeconds, int expectedSize) {
            this.title = title;
            this.durationSeconds = durationSeconds;
            this.expectedSize = expectedSize;
        }

        boolean appendChunk(byte[] data) {
            if (buffer.size() + data.length > MAX_TOTAL_BYTES) {
                return false;
            }
            buffer.write(data, 0, data.length);
            return true;
        }

        byte[] toBytes() {
            return buffer.toByteArray();
        }

        String title() {
            return title;
        }

        int durationSeconds() {
            return durationSeconds;
        }
    }

    private ServerUploadHandler() {}
}
