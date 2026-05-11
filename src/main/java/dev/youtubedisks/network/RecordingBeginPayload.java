package dev.youtubedisks.network;

import dev.youtubedisks.YoutubeDisks;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * C2S — client announces it is about to upload a recorded .ogg for the given recorder block.
 */
public record RecordingBeginPayload(BlockPos pos, String title, int durationSeconds, int totalBytes)
    implements CustomPacketPayload {

    public static final Type<RecordingBeginPayload> TYPE = new Type<>(
        ResourceLocation.fromNamespaceAndPath(YoutubeDisks.MODID, "recording_begin")
    );

    public static final StreamCodec<ByteBuf, RecordingBeginPayload> STREAM_CODEC = StreamCodec.composite(
        BlockPos.STREAM_CODEC,     RecordingBeginPayload::pos,
        ByteBufCodecs.STRING_UTF8, RecordingBeginPayload::title,
        ByteBufCodecs.INT,         RecordingBeginPayload::durationSeconds,
        ByteBufCodecs.INT,         RecordingBeginPayload::totalBytes,
        RecordingBeginPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
