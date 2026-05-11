package dev.youtubedisks.network;

import dev.youtubedisks.YoutubeDisks;
import dev.youtubedisks.speaker.Track;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

/**
 * S2C — full state push for a speaker at {@code pos}.
 *
 * <p>{@code elapsedMillis} is the server's "current track elapsed time" at send-time; clients
 * can use it to seek into the audio when joining mid-playback (deferred — Phase 2g).
 */
public record SpeakerStatePayload(
    BlockPos pos,
    List<Track> playlist,
    int currentIndex,
    boolean playing,
    long elapsedMillis
) implements CustomPacketPayload {

    public static final Type<SpeakerStatePayload> TYPE = new Type<>(
        ResourceLocation.fromNamespaceAndPath(YoutubeDisks.MODID, "speaker_state")
    );

    public static final StreamCodec<ByteBuf, SpeakerStatePayload> STREAM_CODEC = StreamCodec.composite(
        BlockPos.STREAM_CODEC,                         SpeakerStatePayload::pos,
        Track.STREAM_CODEC.apply(ByteBufCodecs.list()), SpeakerStatePayload::playlist,
        ByteBufCodecs.INT,                             SpeakerStatePayload::currentIndex,
        ByteBufCodecs.BOOL,                            SpeakerStatePayload::playing,
        ByteBufCodecs.VAR_LONG,                        SpeakerStatePayload::elapsedMillis,
        SpeakerStatePayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
