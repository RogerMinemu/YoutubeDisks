package dev.youtubedisks.network;

import dev.youtubedisks.YoutubeDisks;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * S2C — server signals the end of a disc download. On {@code ok=true} the client commits
 * the accumulated bytes to its local cache and starts playback; on {@code ok=false} it
 * discards the pending accumulator and logs {@code error}.
 */
public record DiscDataEndPayload(String discId, boolean ok, String error) implements CustomPacketPayload {

    public static final Type<DiscDataEndPayload> TYPE = new Type<>(
        ResourceLocation.fromNamespaceAndPath(YoutubeDisks.MODID, "disc_data_end")
    );

    public static final StreamCodec<ByteBuf, DiscDataEndPayload> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.STRING_UTF8, DiscDataEndPayload::discId,
        ByteBufCodecs.BOOL,        DiscDataEndPayload::ok,
        ByteBufCodecs.STRING_UTF8, DiscDataEndPayload::error,
        DiscDataEndPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
