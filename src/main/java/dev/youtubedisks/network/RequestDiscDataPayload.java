package dev.youtubedisks.network;

import dev.youtubedisks.YoutubeDisks;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * C2S — client asks the server for a disc's .ogg bytes. Sent only when the client's
 * local cache doesn't have it.
 */
public record RequestDiscDataPayload(String discId) implements CustomPacketPayload {

    public static final Type<RequestDiscDataPayload> TYPE = new Type<>(
        ResourceLocation.fromNamespaceAndPath(YoutubeDisks.MODID, "request_disc_data")
    );

    public static final StreamCodec<ByteBuf, RequestDiscDataPayload> STREAM_CODEC =
        ByteBufCodecs.STRING_UTF8.map(RequestDiscDataPayload::new, RequestDiscDataPayload::discId);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
