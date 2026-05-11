package dev.youtubedisks.youtube;

import org.schabi.newpipe.extractor.NewPipe;
import org.schabi.newpipe.extractor.ServiceList;
import org.schabi.newpipe.extractor.StreamingService;
import org.schabi.newpipe.extractor.stream.AudioStream;
import org.schabi.newpipe.extractor.stream.StreamExtractor;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Comparator;
import java.util.List;

public final class YoutubeAudioExtractor {

    private static final Object INIT_LOCK = new Object();
    private static volatile boolean initialized = false;

    public record AudioResult(byte[] data, String mimeType, String title, long durationSeconds) {}

    private final HttpClient streamClient = HttpClient.newBuilder()
        .followRedirects(HttpClient.Redirect.NORMAL)
        .connectTimeout(Duration.ofSeconds(20))
        .build();

    public AudioResult extract(String youtubeUrl) throws Exception {
        ensureInit();

        StreamingService youtube = ServiceList.YouTube;
        StreamExtractor extractor = youtube.getStreamExtractor(youtubeUrl);
        extractor.fetchPage();

        String title = extractor.getName();
        long duration = extractor.getLength();
        List<AudioStream> audioStreams = extractor.getAudioStreams();
        if (audioStreams.isEmpty()) {
            throw new IOException("No audio streams available for " + youtubeUrl);
        }

        AudioStream best = audioStreams.stream()
            .max(Comparator.comparingInt(AudioStream::getBitrate))
            .orElseThrow();

        byte[] data = downloadStream(best.getUrl());
        String mime = best.getFormat() != null ? best.getFormat().getMimeType() : "application/octet-stream";
        return new AudioResult(data, mime, title, duration);
    }

    private byte[] downloadStream(String streamUrl) throws Exception {
        HttpRequest req = HttpRequest.newBuilder(URI.create(streamUrl))
            .timeout(Duration.ofMinutes(3))
            .GET()
            .build();
        HttpResponse<byte[]> resp = streamClient.send(req, HttpResponse.BodyHandlers.ofByteArray());
        if (resp.statusCode() != 200) {
            throw new IOException("Audio stream download failed: HTTP " + resp.statusCode());
        }
        return resp.body();
    }

    private static void ensureInit() {
        if (initialized) {
            return;
        }
        synchronized (INIT_LOCK) {
            if (initialized) {
                return;
            }
            if (NewPipe.getDownloader() == null) {
                NewPipe.init(new HttpDownloader());
            }
            initialized = true;
        }
    }
}
