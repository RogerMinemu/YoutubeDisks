package dev.youtubedisks.client.speaker;

import dev.youtubedisks.YoutubeDisks;
import dev.youtubedisks.client.playback.ClientPlaybackController;
import dev.youtubedisks.client.audio.DiscAudioPlayer;
import dev.youtubedisks.network.SpeakerStatePayload;
import dev.youtubedisks.speaker.Track;
import net.minecraft.core.BlockPos;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Per-BlockPos client-side mirror of each known speaker's state. Updated from
 * {@link SpeakerStatePayload} arrivals. Drives {@link DiscAudioPlayer} start/stop based on
 * transitions, and gives the open screen something to render.
 */
public final class ClientSpeakerState {

    public record State(
        List<Track> playlist,
        int currentIndex,
        boolean playing,
        long elapsedMillisAtArrival,
        long arrivalSystemMillis
    ) {
        public Track currentTrack() {
            if (currentIndex < 0 || currentIndex >= playlist.size()) return null;
            return playlist.get(currentIndex);
        }

        /** Live elapsed time including local clock drift since the state arrived. */
        public long displayedElapsedMillis() {
            if (!playing) return elapsedMillisAtArrival;
            long delta = System.currentTimeMillis() - arrivalSystemMillis;
            return elapsedMillisAtArrival + Math.max(0L, delta);
        }
    }

    private static final Map<BlockPos, State> SPEAKERS = new HashMap<>();

    public static State get(BlockPos pos) {
        return SPEAKERS.getOrDefault(pos,
            new State(Collections.emptyList(), -1, false, 0L, System.currentTimeMillis()));
    }

    public static void handleState(SpeakerStatePayload payload, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            BlockPos pos = payload.pos();
            State previous = SPEAKERS.get(pos);
            State next = new State(
                new ArrayList<>(payload.playlist()),
                payload.currentIndex(),
                payload.playing(),
                payload.elapsedMillis(),
                System.currentTimeMillis()
            );
            SPEAKERS.put(pos, next);
            reconcileAudio(pos, previous, next);
        });
    }

    private static void reconcileAudio(BlockPos pos, State previous, State next) {
        boolean wasPlaying = previous != null && previous.playing && previous.currentTrack() != null;
        boolean willPlay = next.playing && next.currentTrack() != null;

        Track prevTrack = wasPlaying ? previous.currentTrack() : null;
        Track nextTrack = willPlay ? next.currentTrack() : null;

        boolean sameTrack = prevTrack != null && nextTrack != null
            && prevTrack.discId().equals(nextTrack.discId());

        if (wasPlaying && (!willPlay || !sameTrack)) {
            DiscAudioPlayer.stopAt(pos);
        }

        if (willPlay && (!wasPlaying || !sameTrack)) {
            YoutubeDisks.LOGGER.info("[speaker] state → play track={} @ {} (elapsed={}ms)",
                nextTrack.title(), pos, next.elapsedMillisAtArrival());
            ClientPlaybackController.startSpeakerPlay(nextTrack.discId(), pos);
        }
    }

    private ClientSpeakerState() {}
}
