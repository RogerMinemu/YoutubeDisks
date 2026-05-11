package dev.youtubedisks.network;

import dev.youtubedisks.YoutubeDisks;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * C2S — player issues a command against a Speaker block at {@code pos}.
 *
 * <p>For ADD_TRACK the payload carries the discId/title/duration. For other commands the trailing
 * fields are ignored (empty strings + 0).
 */
public record SpeakerCommandPayload(
    BlockPos pos,
    Command command,
    int intArg,
    String discId,
    String title,
    int durationSeconds
) implements CustomPacketPayload {

    public enum Command {
        PLAY, STOP, NEXT, SELECT_TRACK, ADD_TRACK
    }

    public static final Type<SpeakerCommandPayload> TYPE = new Type<>(
        ResourceLocation.fromNamespaceAndPath(YoutubeDisks.MODID, "speaker_command")
    );

    private static final StreamCodec<ByteBuf, Command> COMMAND_CODEC =
        ByteBufCodecs.idMapper(i -> Command.values()[i], Command::ordinal);

    public static final StreamCodec<ByteBuf, SpeakerCommandPayload> STREAM_CODEC = StreamCodec.composite(
        BlockPos.STREAM_CODEC,     SpeakerCommandPayload::pos,
        COMMAND_CODEC,             SpeakerCommandPayload::command,
        ByteBufCodecs.INT,         SpeakerCommandPayload::intArg,
        ByteBufCodecs.STRING_UTF8, SpeakerCommandPayload::discId,
        ByteBufCodecs.STRING_UTF8, SpeakerCommandPayload::title,
        ByteBufCodecs.INT,         SpeakerCommandPayload::durationSeconds,
        SpeakerCommandPayload::new
    );

    public static SpeakerCommandPayload play(BlockPos pos) {
        return new SpeakerCommandPayload(pos, Command.PLAY, 0, "", "", 0);
    }

    public static SpeakerCommandPayload stop(BlockPos pos) {
        return new SpeakerCommandPayload(pos, Command.STOP, 0, "", "", 0);
    }

    public static SpeakerCommandPayload next(BlockPos pos) {
        return new SpeakerCommandPayload(pos, Command.NEXT, 0, "", "", 0);
    }

    public static SpeakerCommandPayload selectTrack(BlockPos pos, int idx) {
        return new SpeakerCommandPayload(pos, Command.SELECT_TRACK, idx, "", "", 0);
    }

    public static SpeakerCommandPayload addTrack(BlockPos pos, String discId, String title, int durationSeconds) {
        return new SpeakerCommandPayload(pos, Command.ADD_TRACK, 0, discId, title, durationSeconds);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
