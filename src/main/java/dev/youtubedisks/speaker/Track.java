package dev.youtubedisks.speaker;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/** An entry in a Speaker's playlist. */
public record Track(String discId, String title, int durationSeconds) {

    public static final Codec<Track> CODEC = RecordCodecBuilder.create(i -> i.group(
        Codec.STRING.fieldOf("disc_id").forGetter(Track::discId),
        Codec.STRING.fieldOf("title").forGetter(Track::title),
        Codec.INT.fieldOf("duration_seconds").forGetter(Track::durationSeconds)
    ).apply(i, Track::new));

    public static final StreamCodec<ByteBuf, Track> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.STRING_UTF8, Track::discId,
        ByteBufCodecs.STRING_UTF8, Track::title,
        ByteBufCodecs.INT,         Track::durationSeconds,
        Track::new
    );
}
