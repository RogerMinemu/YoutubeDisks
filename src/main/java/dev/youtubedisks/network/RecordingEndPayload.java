package dev.youtubedisks.network;

import dev.youtubedisks.YoutubeDisks;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * C2S — client signals the upload session is complete; the server now finalizes the disc.
 */
public record RecordingEndPayload(BlockPos pos) implements CustomPacketPayload {

    public static final Type<RecordingEndPayload> TYPE = new Type<>(
        ResourceLocation.fromNamespaceAndPath(YoutubeDisks.MODID, "recording_end")
    );

    public static final StreamCodec<ByteBuf, RecordingEndPayload> STREAM_CODEC =
        BlockPos.STREAM_CODEC.map(RecordingEndPayload::new, RecordingEndPayload::pos);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
