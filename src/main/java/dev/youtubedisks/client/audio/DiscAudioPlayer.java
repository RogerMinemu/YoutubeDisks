package dev.youtubedisks.client.audio;

import dev.youtubedisks.YoutubeDisks;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import org.lwjgl.openal.AL10;
import org.lwjgl.stb.STBVorbis;
import org.lwjgl.stb.STBVorbisInfo;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Plays decoded .ogg Vorbis bytes through OpenAL directly. Two playback modes:
 *
 * <ul>
 *     <li><b>Hand-anchored</b> (head-locked) — right-click the recorded_disk in hand</li>
 *     <li><b>Positional</b> (world-locked at a BlockPos) — Speaker block</li>
 * </ul>
 *
 * Decode happens on a background thread; all OpenAL calls are funneled to the client main
 * thread via {@code Minecraft.execute} because OpenAL shares its context with vanilla.
 */
public final class DiscAudioPlayer {

    private static final ExecutorService DECODER = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "YoutubeDisks-AudioDecoder");
        t.setDaemon(true);
        return t;
    });

    private static final Map<String, ActivePlayback> ACTIVE_HAND = new HashMap<>();
    private static final Map<BlockPos, PositionalPlayback> ACTIVE_AT_POS = new HashMap<>();

    // ------------------------------------------------------------------ public API

    public static synchronized boolean isPlaying(String discId) {
        return ACTIVE_HAND.containsKey(discId);
    }

    public static boolean isPlayingAt(BlockPos pos) {
        return ACTIVE_AT_POS.containsKey(pos);
    }

    public static void play(String discId, byte[] oggBytes) {
        DECODER.submit(() -> {
            try {
                DecodedPcm pcm = decodeOgg(oggBytes);
                Minecraft.getInstance().execute(() -> startHandSource(discId, pcm));
            } catch (Exception e) {
                YoutubeDisks.LOGGER.error("[playback] decode failed for {}", discId, e);
            }
        });
    }

    public static void playAt(String discId, byte[] oggBytes, BlockPos pos) {
        DECODER.submit(() -> {
            try {
                DecodedPcm pcm = decodeOgg(oggBytes);
                Minecraft.getInstance().execute(() -> startPositionalSource(discId, pcm, pos));
            } catch (Exception e) {
                YoutubeDisks.LOGGER.error("[playback] decode failed for {} @ {}", discId, pos, e);
            }
        });
    }

    public static void stop(String discId) {
        Minecraft.getInstance().execute(() -> stopHandSource(discId));
    }

    public static void stopAt(BlockPos pos) {
        Minecraft.getInstance().execute(() -> stopPositionalSource(pos));
    }

    public static void stopAll() {
        Minecraft.getInstance().execute(() -> {
            for (String id : new ArrayList<>(ACTIVE_HAND.keySet())) {
                stopHandSource(id);
            }
            for (BlockPos pos : new ArrayList<>(ACTIVE_AT_POS.keySet())) {
                stopPositionalSource(pos);
            }
        });
    }

    /** Reaps natural-end (AL_STOPPED) sources every client tick. */
    public static void tick() {
        tickHandSources();
        tickPositionalSources();
    }

    // ------------------------------------------------------------------ main-thread helpers

    private static void startHandSource(String discId, DecodedPcm pcm) {
        stopHandSource(discId);

        int buffer = AL10.alGenBuffers();
        AL10.alBufferData(buffer, pcm.format(), pcm.data(), pcm.sampleRate());
        MemoryUtil.memFree(pcm.data());

        int source = AL10.alGenSources();
        AL10.alSourcei(source, AL10.AL_BUFFER, buffer);
        AL10.alSourcei(source, AL10.AL_SOURCE_RELATIVE, AL10.AL_TRUE);
        AL10.alSource3f(source, AL10.AL_POSITION, 0f, 0f, 0f);
        AL10.alSourcef(source, AL10.AL_GAIN, 1.0f);
        AL10.alSourcePlay(source);

        ACTIVE_HAND.put(discId, new ActivePlayback(source, buffer));
        YoutubeDisks.LOGGER.info("[playback] started hand {} ({} Hz, fmt={})", discId, pcm.sampleRate(), pcm.format());
    }

    private static void startPositionalSource(String discId, DecodedPcm pcm, BlockPos pos) {
        stopPositionalSource(pos);

        int buffer = AL10.alGenBuffers();
        AL10.alBufferData(buffer, pcm.format(), pcm.data(), pcm.sampleRate());
        MemoryUtil.memFree(pcm.data());

        int source = AL10.alGenSources();
        AL10.alSourcei(source, AL10.AL_BUFFER, buffer);
        AL10.alSource3f(source, AL10.AL_POSITION, pos.getX() + 0.5f, pos.getY() + 0.5f, pos.getZ() + 0.5f);
        AL10.alSourcef(source, AL10.AL_REFERENCE_DISTANCE, 6f);
        AL10.alSourcef(source, AL10.AL_MAX_DISTANCE, 64f);
        AL10.alSourcef(source, AL10.AL_GAIN, 1.0f);
        AL10.alSourcePlay(source);

        ACTIVE_AT_POS.put(pos, new PositionalPlayback(discId, source, buffer));
        YoutubeDisks.LOGGER.info("[playback] started @ {} disc={}", pos, discId);
    }

    private static void stopHandSource(String discId) {
        ActivePlayback existing = ACTIVE_HAND.remove(discId);
        if (existing != null) {
            AL10.alSourceStop(existing.source);
            releaseAl(existing.source, existing.buffer);
            YoutubeDisks.LOGGER.info("[playback] stopped hand {}", discId);
        }
    }

    private static void stopPositionalSource(BlockPos pos) {
        PositionalPlayback existing = ACTIVE_AT_POS.remove(pos);
        if (existing != null) {
            AL10.alSourceStop(existing.source);
            releaseAl(existing.source, existing.buffer);
            YoutubeDisks.LOGGER.info("[playback] stopped @ {}", pos);
        }
    }

    private static void tickHandSources() {
        if (ACTIVE_HAND.isEmpty()) return;
        Iterator<Map.Entry<String, ActivePlayback>> it = ACTIVE_HAND.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, ActivePlayback> e = it.next();
            int state = AL10.alGetSourcei(e.getValue().source, AL10.AL_SOURCE_STATE);
            if (state == AL10.AL_STOPPED) {
                releaseAl(e.getValue().source, e.getValue().buffer);
                it.remove();
                YoutubeDisks.LOGGER.info("[playback] finished hand {}", e.getKey());
            }
        }
    }

    private static void tickPositionalSources() {
        if (ACTIVE_AT_POS.isEmpty()) return;
        Iterator<Map.Entry<BlockPos, PositionalPlayback>> it = ACTIVE_AT_POS.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<BlockPos, PositionalPlayback> e = it.next();
            int state = AL10.alGetSourcei(e.getValue().source, AL10.AL_SOURCE_STATE);
            if (state == AL10.AL_STOPPED) {
                releaseAl(e.getValue().source, e.getValue().buffer);
                it.remove();
                YoutubeDisks.LOGGER.info("[playback] finished @ {}", e.getKey());
            }
        }
    }

    private static void releaseAl(int source, int buffer) {
        AL10.alDeleteSources(source);
        AL10.alDeleteBuffers(buffer);
    }

    // ------------------------------------------------------------------ decoder

    private record DecodedPcm(ShortBuffer data, int sampleRate, int format) {}

    private record ActivePlayback(int source, int buffer) {}

    private record PositionalPlayback(String discId, int source, int buffer) {}

    private static DecodedPcm decodeOgg(byte[] oggBytes) {
        ByteBuffer ogg = MemoryUtil.memAlloc(oggBytes.length);
        try {
            ogg.put(oggBytes).flip();
            try (MemoryStack stack = MemoryStack.stackPush()) {
                IntBuffer err = stack.mallocInt(1);
                long handle = STBVorbis.stb_vorbis_open_memory(ogg, err, null);
                if (handle == 0L) {
                    throw new IllegalStateException("stb_vorbis_open_memory failed: error=" + err.get(0));
                }
                try {
                    STBVorbisInfo info = STBVorbisInfo.malloc(stack);
                    STBVorbis.stb_vorbis_get_info(handle, info);
                    int channels = info.channels();
                    int sampleRate = info.sample_rate();
                    int totalSamples = STBVorbis.stb_vorbis_stream_length_in_samples(handle);

                    ShortBuffer pcm = MemoryUtil.memAllocShort(totalSamples * channels);
                    int read = STBVorbis.stb_vorbis_get_samples_short_interleaved(handle, channels, pcm);
                    pcm.limit(read * channels);

                    int format = (channels == 1) ? AL10.AL_FORMAT_MONO16 : AL10.AL_FORMAT_STEREO16;
                    return new DecodedPcm(pcm, sampleRate, format);
                } finally {
                    STBVorbis.stb_vorbis_close(handle);
                }
            }
        } finally {
            MemoryUtil.memFree(ogg);
        }
    }

    private DiscAudioPlayer() {}
}
