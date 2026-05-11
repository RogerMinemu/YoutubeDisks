package dev.youtubedisks.disc;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Stored on a recorded_disk item as a DataComponent. Identifies a unique recording
 * by content hash (discId) and carries display metadata (title, duration).
 */
public record DiscData(String discId, String title, int durationSeconds) {

    public static final Codec<DiscData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        Codec.STRING.fieldOf("disc_id").forGetter(DiscData::discId),
        Codec.STRING.fieldOf("title").forGetter(DiscData::title),
        Codec.INT.fieldOf("duration_seconds").forGetter(DiscData::durationSeconds)
    ).apply(instance, DiscData::new));

    public static final StreamCodec<ByteBuf, DiscData> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.STRING_UTF8, DiscData::discId,
        ByteBufCodecs.STRING_UTF8, DiscData::title,
        ByteBufCodecs.INT,         DiscData::durationSeconds,
        DiscData::new
    );

    /** Stable content-addressable id for an .ogg payload (SHA-256 hex). */
    public static String computeDiscId(byte[] oggBytes) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(oggBytes);
            StringBuilder sb = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                int v = b & 0xFF;
                if (v < 16) sb.append('0');
                sb.append(Integer.toHexString(v));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available on this JVM", e);
        }
    }
}
