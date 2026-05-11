package dev.youtubedisks.network;

import dev.youtubedisks.YoutubeDisks;
import dev.youtubedisks.client.playback.ClientPlaybackController;
import dev.youtubedisks.client.recording.ClientUploader;
import dev.youtubedisks.client.speaker.ClientSpeakerState;
import dev.youtubedisks.server.disc.ServerDiscStreamer;
import dev.youtubedisks.server.speaker.ServerSpeakerHandler;
import dev.youtubedisks.server.upload.ServerUploadHandler;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

@EventBusSubscriber(modid = YoutubeDisks.MODID)
public final class NetworkSetup {

    private static final String PROTOCOL_VERSION = "2";

    @SubscribeEvent
    public static void onRegisterPayloadHandlers(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(YoutubeDisks.MODID).versioned(PROTOCOL_VERSION);

        // --- Recording (upload) flow ---
        registrar.playToServer(RecordingBeginPayload.TYPE,  RecordingBeginPayload.STREAM_CODEC,  ServerUploadHandler::handleBegin);
        registrar.playToServer(RecordingChunkPayload.TYPE,  RecordingChunkPayload.STREAM_CODEC,  ServerUploadHandler::handleChunk);
        registrar.playToServer(RecordingEndPayload.TYPE,    RecordingEndPayload.STREAM_CODEC,    ServerUploadHandler::handleEnd);
        registrar.playToClient(RecordingResultPayload.TYPE, RecordingResultPayload.STREAM_CODEC, ClientUploader::handleResult);

        // --- Playback (download) flow ---
        registrar.playToServer(RequestDiscDataPayload.TYPE, RequestDiscDataPayload.STREAM_CODEC, ServerDiscStreamer::handleRequest);
        registrar.playToClient(DiscDataChunkPayload.TYPE,   DiscDataChunkPayload.STREAM_CODEC,   ClientPlaybackController::handleChunk);
        registrar.playToClient(DiscDataEndPayload.TYPE,     DiscDataEndPayload.STREAM_CODEC,     ClientPlaybackController::handleEnd);

        // --- Speaker control + state ---
        registrar.playToServer(SpeakerCommandPayload.TYPE, SpeakerCommandPayload.STREAM_CODEC, ServerSpeakerHandler::handleCommand);
        registrar.playToClient(SpeakerStatePayload.TYPE,   SpeakerStatePayload.STREAM_CODEC,   ClientSpeakerState::handleState);
    }

    private NetworkSetup() {}
}
