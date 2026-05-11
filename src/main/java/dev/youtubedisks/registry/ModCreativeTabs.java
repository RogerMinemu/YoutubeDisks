package dev.youtubedisks.registry;

import dev.youtubedisks.YoutubeDisks;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public final class ModCreativeTabs {

    public static final DeferredRegister<CreativeModeTab> TABS =
        DeferredRegister.create(Registries.CREATIVE_MODE_TAB, YoutubeDisks.MODID);

    public static final Supplier<CreativeModeTab> MAIN = TABS.register("main", () ->
        CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.youtubedisks"))
            .icon(() -> new ItemStack(ModBlocks.SPEAKER.get()))
            .displayItems((params, output) -> {
                output.accept(ModItems.BLANK_DISK.get());
                output.accept(ModItems.RECORDED_DISK.get());
                output.accept(ModBlocks.DISK_RECORDER.get());
                output.accept(ModBlocks.SPEAKER.get());
            })
            .build()
    );

    private ModCreativeTabs() {}
}
