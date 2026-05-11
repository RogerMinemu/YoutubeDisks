package dev.youtubedisks.registry;

import dev.youtubedisks.YoutubeDisks;
import dev.youtubedisks.item.RecordedDiskItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModItems {

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(YoutubeDisks.MODID);

    public static final DeferredItem<Item> BLANK_DISK = ITEMS.registerSimpleItem(
        "blank_disk",
        new Item.Properties().stacksTo(1)
    );

    /**
     * No {@code JukeboxPlayable} data component — we use our custom Speaker block instead of
     * the vanilla jukebox. The disc still right-clicks in hand for head-anchored preview.
     */
    public static final DeferredItem<RecordedDiskItem> RECORDED_DISK = ITEMS.register(
        "recorded_disk",
        () -> new RecordedDiskItem(new Item.Properties().stacksTo(1))
    );

    public static final DeferredItem<BlockItem> DISK_RECORDER = ITEMS.registerSimpleBlockItem(ModBlocks.DISK_RECORDER);

    public static final DeferredItem<BlockItem> SPEAKER = ITEMS.registerSimpleBlockItem(ModBlocks.SPEAKER);

    private ModItems() {}
}
