package dev.youtubedisks.audio;

import ws.schild.jave.Encoder;
import ws.schild.jave.MultimediaObject;
import ws.schild.jave.encode.AudioAttributes;
import ws.schild.jave.encode.EncodingAttributes;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

public final class OggTranscoder {

    public byte[] toOggVorbis(byte[] inputAudio, String inputMimeOrExt) throws Exception {
        Path tmpDir = Files.createTempDirectory("ytdisk-");
        Path input = tmpDir.resolve("in." + extFor(inputMimeOrExt));
        Path output = tmpDir.resolve("out.ogg");
        try {
            Files.write(input, inputAudio);

            AudioAttributes audio = new AudioAttributes();
            audio.setCodec("libvorbis");
            // 192k ABR is near-transparent for the 128-160k Opus/AAC sources YouTube serves.
            // Pushing higher (256k+) wastes bytes — re-encoding lossy can't add quality back.
            audio.setBitRate(192_000);
            audio.setSamplingRate(48_000);
            audio.setChannels(2);

            EncodingAttributes attrs = new EncodingAttributes();
            attrs.setOutputFormat("ogg");
            attrs.setAudioAttributes(audio);

            new Encoder().encode(new MultimediaObject(input.toFile()), output.toFile(), attrs);

            return Files.readAllBytes(output);
        } finally {
            silentDelete(input);
            silentDelete(output);
            silentDelete(tmpDir);
        }
    }

    private static String extFor(String mimeOrExt) {
        if (mimeOrExt == null) {
            return "bin";
        }
        String s = mimeOrExt.toLowerCase(Locale.ROOT);
        if (s.contains("webm")) return "webm";
        if (s.contains("m4a") || s.contains("mp4")) return "m4a";
        if (s.contains("opus")) return "opus";
        if (s.contains("ogg")) return "ogg";
        if (s.contains("mpeg") || s.contains("mp3")) return "mp3";
        if (s.contains("aac")) return "aac";
        return "bin";
    }

    private static void silentDelete(Path p) {
        try {
            Files.deleteIfExists(p);
        } catch (IOException ignored) {
        }
    }
}
