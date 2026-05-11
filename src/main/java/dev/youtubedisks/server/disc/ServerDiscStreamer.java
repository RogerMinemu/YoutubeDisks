package dev.youtubedisks.server.disc;

import dev.youtubedisks.YoutubeDisks;
import dev.youtubedisks.disc.DiscStorage;
import dev.youtubedisks.network.DiscDataChunkPayload;
import dev.youtubedisks.network.DiscDataEndPayload;
import dev.youtubedisks.network.RequestDiscDataPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.Arrays;

/**
 * Server-side handler for client disc-data requests. Loads the .ogg from the world's
 * disc store and chunks it down the wire to the requesting player.
 */
public final class ServerDiscStreamer {

    private static final int CHUNK_SIZE = 24 * 1024;

    /** Avoid common path-traversal injection via crafted discId strings. */
    private static boolean isValidDiscId(String discId) {
        if (discId == null || discId.isEmpty() || discId.length() > 128) {
            return false;
        }
        for (int i = 0; i < discId.length(); i++) {
            char c = discId.charAt(i);
            boolean ok = (c >= '0' && c <= '9') || (c >= 'a' && c <= 'f');
            if (!ok) {
                return false;
            }
        }
        return true;
    }

    public static void handleRequest(RequestDiscDataPayload payload, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (!(ctx.player() instanceof ServerPlayer sp)) {
                return;
            }
            String discId = payload.discId();
            if (!isValidDiscId(discId)) {
                sendEnd(sp, discId, false, "invalid_disc_id");
                return;
            }
            DiscStorage storage = new DiscStorage(sp.serverLevel().getServer());
            if (!storage.exists(discId)) {
                sendEnd(sp, discId, false, "not_found");
                return;
            }
            try {
                byte[] ogg = storage.load(discId);
                int offset = 0;
                int seq = 0;
                while (offset < ogg.length) {
                    int len = Math.min(CHUNK_SIZE, ogg.length - offset);
                    byte[] chunk = Arrays.copyOfRange(ogg, offset, offset + len);
                    PacketDistributor.sendToPlayer(sp, new DiscDataChunkPayload(discId, seq, chunk));
                    offset += len;
                    seq++;
                }
                sendEnd(sp, discId, true, "");
                YoutubeDisks.LOGGER.info("[stream] sent {} bytes of {} to {} in {} chunks", ogg.length, discId, sp.getName().getString(), seq);
            } catch (Exception e) {
                YoutubeDisks.LOGGER.error("[stream] failed for {}", discId, e);
                String msg = e.getMessage() == null ? "io_error" : e.getMessage();
                sendEnd(sp, discId, false, msg);
            }
        });
    }

    private static void sendEnd(ServerPlayer sp, String discId, boolean ok, String error) {
        PacketDistributor.sendToPlayer(sp, new DiscDataEndPayload(discId, ok, error));
    }

    private ServerDiscStreamer() {}
}
