package dev.youtubedisks.network;

import dev.youtubedisks.YoutubeDisks;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * S2C — one chunk of an in-flight disc download. Client appends to the pending accumulator
 * keyed by discId.
 */
public record DiscDataChunkPayload(String discId, int seq, byte[] data) implements CustomPacketPayload {

    public static final int MAX_CHUNK_BYTES = 32 * 1024;

    public static final Type<DiscDataChunkPayload> TYPE = new Type<>(
        ResourceLocation.fromNamespaceAndPath(YoutubeDisks.MODID, "disc_data_chunk")
    );

    public static final StreamCodec<ByteBuf, DiscDataChunkPayload> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.STRING_UTF8,                DiscDataChunkPayload::discId,
        ByteBufCodecs.INT,                        DiscDataChunkPayload::seq,
        ByteBufCodecs.byteArray(MAX_CHUNK_BYTES), DiscDataChunkPayload::data,
        DiscDataChunkPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
