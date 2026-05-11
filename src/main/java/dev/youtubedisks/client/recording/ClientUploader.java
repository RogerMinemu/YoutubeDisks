package dev.youtubedisks.client.recording;

import dev.youtubedisks.network.RecordingBeginPayload;
import dev.youtubedisks.network.RecordingChunkPayload;
import dev.youtubedisks.network.RecordingEndPayload;
import dev.youtubedisks.network.RecordingResultPayload;
import net.minecraft.core.BlockPos;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.Arrays;

/**
 * Client-side: splits a finished .ogg payload into network-sized chunks and ships them to the server.
 * Server replies asynchronously with a {@link RecordingResultPayload} which we route back into the
 * recording service.
 */
public final class ClientUploader {

    /** Stays well under MC's vanilla packet limit even with framing overhead. */
    private static final int CHUNK_SIZE = 24 * 1024;

    public static void upload(BlockPos pos, String title, int durationSeconds, byte[] oggBytes) {
        PacketDistributor.sendToServer(new RecordingBeginPayload(pos, title, durationSeconds, oggBytes.length));

        int offset = 0;
        int seq = 0;
        while (offset < oggBytes.length) {
            int len = Math.min(CHUNK_SIZE, oggBytes.length - offset);
            byte[] chunk = Arrays.copyOfRange(oggBytes, offset, offset + len);
            PacketDistributor.sendToServer(new RecordingChunkPayload(pos, seq, chunk));
            offset += len;
            seq++;
        }

        PacketDistributor.sendToServer(new RecordingEndPayload(pos));
    }

    /** Network handler — runs on the client. Dispatched here from {@code NetworkSetup}. */
    public static void handleResult(RecordingResultPayload payload, IPayloadContext ctx) {
        ctx.enqueueWork(() -> ClientRecordingService.get().onUploadResult(payload.ok(), payload.message()));
    }

    private ClientUploader() {}
}
