package dev.youtubedisks.registry;

import dev.youtubedisks.YoutubeDisks;
import dev.youtubedisks.block.DiskRecorderBlockEntity;
import dev.youtubedisks.speaker.SpeakerBlockEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public final class ModBlockEntities {

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
        DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, YoutubeDisks.MODID);

    public static final Supplier<BlockEntityType<DiskRecorderBlockEntity>> DISK_RECORDER = BLOCK_ENTITIES.register(
        "disk_recorder",
        () -> BlockEntityType.Builder.of(DiskRecorderBlockEntity::new, ModBlocks.DISK_RECORDER.get()).build(null)
    );

    public static final Supplier<BlockEntityType<SpeakerBlockEntity>> SPEAKER = BLOCK_ENTITIES.register(
        "speaker",
        () -> BlockEntityType.Builder.of(SpeakerBlockEntity::new, ModBlocks.SPEAKER.get()).build(null)
    );

    private ModBlockEntities() {}
}
