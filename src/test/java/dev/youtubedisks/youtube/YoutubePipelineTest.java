package dev.youtubedisks.youtube;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Integration test: hits YouTube live, runs the full pipeline, drops a real .ogg file
 * in build/test-output.ogg for manual playback verification.
 *
 * Override the URL with -Dyoutubedisks.test.url=https://... when iterating.
 *
 * Tagged "integration" so it can be excluded from offline runs if needed via
 *   ./gradlew test --tests '*' -PexcludeTags=integration
 */
@Tag("integration")
class YoutubePipelineTest {

    private static final String DEFAULT_URL = "https://www.youtube.com/watch?v=YE7VzlLtp-4";

    private static String testUrl() {
        return System.getProperty("youtubedisks.test.url", DEFAULT_URL);
    }

    @Test
    void downloads_and_transcodes_to_valid_ogg_vorbis() throws Exception {
        String url = testUrl();
        System.out.println("[test] pipeline target: " + url);

        var pipeline = new YoutubePipeline();
        long start = System.currentTimeMillis();
        var track = pipeline.urlToOgg(url);
        long elapsed = System.currentTimeMillis() - start;

        System.out.println("[test] title:    " + track.title());
        System.out.println("[test] duration: " + track.durationSeconds() + "s");
        System.out.println("[test] ogg size: " + track.oggBytes().length + " bytes");
        System.out.println("[test] elapsed:  " + elapsed + "ms");

        assertNotNull(track.title(), "title must resolve");
        assertNotNull(track.oggBytes(), "ogg bytes must be produced");
        assertTrue(track.oggBytes().length > 10_000,
            "ogg payload should be at least 10KB; got " + track.oggBytes().length);

        byte[] head = track.oggBytes();
        assertEquals('O', (char) head[0], "Ogg magic byte 0");
        assertEquals('g', (char) head[1], "Ogg magic byte 1");
        assertEquals('g', (char) head[2], "Ogg magic byte 2");
        assertEquals('S', (char) head[3], "Ogg magic byte 3");

        Path out = Paths.get("build", "test-output.ogg");
        Files.createDirectories(out.getParent());
        Files.write(out, track.oggBytes());
        System.out.println("[test] wrote: " + out.toAbsolutePath());
    }
}
