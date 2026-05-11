package dev.youtubedisks.server.speaker;

import dev.youtubedisks.YoutubeDisks;
import dev.youtubedisks.disc.DiscStorage;
import dev.youtubedisks.network.SpeakerCommandPayload;
import dev.youtubedisks.speaker.SpeakerBlockEntity;
import dev.youtubedisks.speaker.Track;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public final class ServerSpeakerHandler {

    public static void handleCommand(SpeakerCommandPayload payload, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (!(ctx.player() instanceof ServerPlayer sp)) {
                return;
            }
            BlockEntity be = sp.level().getBlockEntity(payload.pos());
            if (!(be instanceof SpeakerBlockEntity speaker)) {
                YoutubeDisks.LOGGER.warn("[speaker] command {} on non-speaker block at {}",
                    payload.command(), payload.pos());
                return;
            }
            // Distance check: only allow commands from players nearby.
            if (sp.distanceToSqr(payload.pos().getCenter()) > 32 * 32) {
                YoutubeDisks.LOGGER.warn("[speaker] {} tried to command speaker out of range", sp.getName().getString());
                return;
            }

            switch (payload.command()) {
                case PLAY -> {
                    speaker.play();
                    YoutubeDisks.LOGGER.info("[speaker] {} pressed play @ {}", sp.getName().getString(), payload.pos());
                }
                case STOP -> {
                    speaker.stop();
                    YoutubeDisks.LOGGER.info("[speaker] {} pressed stop @ {}", sp.getName().getString(), payload.pos());
                }
                case NEXT -> {
                    speaker.next();
                    YoutubeDisks.LOGGER.info("[speaker] {} pressed next @ {}", sp.getName().getString(), payload.pos());
                }
                case SELECT_TRACK -> {
                    speaker.selectTrack(payload.intArg());
                    YoutubeDisks.LOGGER.info("[speaker] {} selected track {} @ {}", sp.getName().getString(), payload.intArg(), payload.pos());
                }
                case ADD_TRACK -> {
                    if (payload.discId().isEmpty()) {
                        YoutubeDisks.LOGGER.warn("[speaker] ADD_TRACK with empty discId");
                        return;
                    }
                    // Validate the disc actually exists in storage — defends against forged packets.
                    DiscStorage storage = new DiscStorage(sp.serverLevel().getServer());
                    if (!storage.exists(payload.discId())) {
                        YoutubeDisks.LOGGER.warn("[speaker] {} added missing disc {}",
                            sp.getName().getString(), payload.discId());
                        return;
                    }
                    speaker.addTrack(new Track(payload.discId(), payload.title(), payload.durationSeconds()));
                    YoutubeDisks.LOGGER.info("[speaker] {} added track {} @ {}",
                        sp.getName().getString(), payload.title(), payload.pos());
                }
            }
        });
    }

    private ServerSpeakerHandler() {}
}
