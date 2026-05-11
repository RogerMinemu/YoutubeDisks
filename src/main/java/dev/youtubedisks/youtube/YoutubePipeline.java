package dev.youtubedisks.youtube;

import dev.youtubedisks.audio.OggTranscoder;

public final class YoutubePipeline {

    private final YoutubeAudioExtractor extractor;
    private final OggTranscoder transcoder;

    public YoutubePipeline() {
        this(new YoutubeAudioExtractor(), new OggTranscoder());
    }

    public YoutubePipeline(YoutubeAudioExtractor extractor, OggTranscoder transcoder) {
        this.extractor = extractor;
        this.transcoder = transcoder;
    }

    public record Track(String title, long durationSeconds, byte[] oggBytes) {}

    public Track urlToOgg(String youtubeUrl) throws Exception {
        var audio = extractor.extract(youtubeUrl);
        byte[] ogg = transcoder.toOggVorbis(audio.data(), audio.mimeType());
        return new Track(audio.title(), audio.durationSeconds(), ogg);
    }
}
