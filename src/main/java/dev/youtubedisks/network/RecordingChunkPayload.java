package dev.youtubedisks.network;

import dev.youtubedisks.YoutubeDisks;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * C2S — one chunk of an in-flight .ogg upload. Server appends to the session keyed by (player, pos).
 */
public record RecordingChunkPayload(BlockPos pos, int seq, byte[] data) implements CustomPacketPayload {

    /** Max chunk size on the wire. Safe under MC's default ~32KB packet limit. */
    public static final int MAX_CHUNK_BYTES = 32 * 1024;

    public static final Type<RecordingChunkPayload> TYPE = new Type<>(
        ResourceLocation.fromNamespaceAndPath(YoutubeDisks.MODID, "recording_chunk")
    );

    public static final StreamCodec<ByteBuf, RecordingChunkPayload> STREAM_CODEC = StreamCodec.composite(
        BlockPos.STREAM_CODEC,                   RecordingChunkPayload::pos,
        ByteBufCodecs.INT,                       RecordingChunkPayload::seq,
        ByteBufCodecs.byteArray(MAX_CHUNK_BYTES), RecordingChunkPayload::data,
        RecordingChunkPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
