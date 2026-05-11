package dev.youtubedisks.client.recording;

import dev.youtubedisks.YoutubeDisks;
import dev.youtubedisks.audio.OggTranscoder;
import dev.youtubedisks.disc.DiscData;
import dev.youtubedisks.youtube.YoutubeAudioExtractor;
import net.minecraft.core.BlockPos;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Client-side singleton that runs the YouTube → .ogg pipeline on a daemon background thread,
 * then hands the bytes off to {@link ClientUploader} so the SERVER can store the disc and grant
 * the player the {@code recorded_disk} item.
 *
 * <p>State machine: {@code IDLE → EXTRACTING → TRANSCODING → UPLOADING → COMPLETE} (or {@code ERROR}).
 * The screen polls {@link #getStatus()} on every render to keep its widgets in sync.
 */
public final class ClientRecordingService {

    public enum State {
        IDLE, EXTRACTING, TRANSCODING, UPLOADING, COMPLETE, ERROR
    }

    public record Status(
        State state,
        String detail,
        String title,
        int durationSeconds,
        String discId,
        BlockPos blockPos
    ) {
        public static final Status IDLE = new Status(State.IDLE, "", "", 0, "", null);

        public boolean isBusy() {
            return state == State.EXTRACTING || state == State.TRANSCODING || state == State.UPLOADING;
        }

        public boolean isFinal() {
            return state == State.COMPLETE || state == State.ERROR;
        }
    }

    private static final ClientRecordingService INSTANCE = new ClientRecordingService();

    public static ClientRecordingService get() {
        return INSTANCE;
    }

    private final ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "YoutubeDisks-Recording");
        t.setDaemon(true);
        return t;
    });

    private volatile Status status = Status.IDLE;

    private ClientRecordingService() {}

    public Status getStatus() {
        return status;
    }

    public void startRecording(String url, BlockPos pos) {
        if (status.isBusy()) {
            return;
        }
        this.status = new Status(State.EXTRACTING, "", "", 0, "", pos);
        executor.submit(() -> runRecording(url, pos));
    }

    public void reset() {
        if (!status.isBusy()) {
            this.status = Status.IDLE;
        }
    }

    /** Called from {@link ClientUploader} on the client main thread when the server replies. */
    public void onUploadResult(boolean ok, String message) {
        Status current = this.status;
        if (current.state() != State.UPLOADING) {
            return; // stale result — ignore
        }
        if (ok) {
            this.status = new Status(
                State.COMPLETE,
                "",
                current.title(),
                current.durationSeconds(),
                message,
                current.blockPos()
            );
        } else {
            this.status = new Status(
                State.ERROR,
                message,
                current.title(),
                current.durationSeconds(),
                "",
                current.blockPos()
            );
        }
    }

    private void runRecording(String url, BlockPos pos) {
        try {
            this.status = new Status(State.EXTRACTING, "", "", 0, "", pos);
            YoutubeDisks.LOGGER.info("[recording] starting extraction for {}", url);

            YoutubeAudioExtractor.AudioResult audio = new YoutubeAudioExtractor().extract(url);
            String title = audio.title();
            int duration = (int) audio.durationSeconds();

            this.status = new Status(State.TRANSCODING, "", title, duration, "", pos);
            YoutubeDisks.LOGGER.info("[recording] transcoding {} bytes ({}) to ogg", audio.data().length, audio.mimeType());

            byte[] ogg = new OggTranscoder().toOggVorbis(audio.data(), audio.mimeType());
            String discId = DiscData.computeDiscId(ogg);

            this.status = new Status(State.UPLOADING, "", title, duration, discId, pos);
            YoutubeDisks.LOGGER.info("[recording] uploading {} bytes (discId={})", ogg.length, discId);

            // PacketDistributor is safe to call off the main thread; chunks queue on the
            // connection's event loop and arrive at the server in order.
            ClientUploader.upload(pos, title, duration, ogg);
            // State stays UPLOADING until onUploadResult is invoked.
        } catch (Exception e) {
            YoutubeDisks.LOGGER.error("[recording] failed for {}", url, e);
            String msg = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
            this.status = new Status(State.ERROR, msg, "", 0, "", pos);
        }
    }
}
