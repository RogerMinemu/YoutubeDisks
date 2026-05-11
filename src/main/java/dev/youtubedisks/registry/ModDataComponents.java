package dev.youtubedisks.registry;

import dev.youtubedisks.YoutubeDisks;
import dev.youtubedisks.disc.DiscData;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public final class ModDataComponents {

    public static final DeferredRegister.DataComponents COMPONENTS =
        DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, YoutubeDisks.MODID);

    public static final Supplier<DataComponentType<DiscData>> DISC_DATA = COMPONENTS.registerComponentType(
        "disc_data",
        builder -> builder
            .persistent(DiscData.CODEC)
            .networkSynchronized(DiscData.STREAM_CODEC)
    );

    private ModDataComponents() {}
}
