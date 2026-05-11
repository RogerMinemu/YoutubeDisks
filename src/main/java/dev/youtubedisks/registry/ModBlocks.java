package dev.youtubedisks.registry;

import dev.youtubedisks.YoutubeDisks;
import dev.youtubedisks.block.DiskRecorderBlock;
import dev.youtubedisks.speaker.SpeakerBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlocks {

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(YoutubeDisks.MODID);

    public static final DeferredBlock<DiskRecorderBlock> DISK_RECORDER = BLOCKS.register(
        "disk_recorder",
        () -> new DiskRecorderBlock(BlockBehaviour.Properties.of()
            .mapColor(MapColor.METAL)
            .strength(2.5f)
            .sound(SoundType.METAL)
        )
    );

    public static final DeferredBlock<SpeakerBlock> SPEAKER = BLOCKS.register(
        "speaker",
        () -> new SpeakerBlock(BlockBehaviour.Properties.of()
            .mapColor(MapColor.COLOR_BLUE)
            .strength(2.5f)
            .sound(SoundType.METAL)
        )
    );

    private ModBlocks() {}
}
