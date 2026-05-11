package dev.youtubedisks.network;

import dev.youtubedisks.YoutubeDisks;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * S2C — server's verdict on a finalized upload. {@code message} carries the discId on success,
 * or an error description on failure.
 */
public record RecordingResultPayload(BlockPos pos, boolean ok, String message) implements CustomPacketPayload {

    public static final Type<RecordingResultPayload> TYPE = new Type<>(
        ResourceLocation.fromNamespaceAndPath(YoutubeDisks.MODID, "recording_result")
    );

    public static final StreamCodec<ByteBuf, RecordingResultPayload> STREAM_CODEC = StreamCodec.composite(
        BlockPos.STREAM_CODEC,     RecordingResultPayload::pos,
        ByteBufCodecs.BOOL,        RecordingResultPayload::ok,
        ByteBufCodecs.STRING_UTF8, RecordingResultPayload::message,
        RecordingResultPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
