package dev.youtubedisks;

import com.mojang.logging.LogUtils;
import dev.youtubedisks.registry.ModBlockEntities;
import dev.youtubedisks.registry.ModBlocks;
import dev.youtubedisks.registry.ModCreativeTabs;
import dev.youtubedisks.registry.ModDataComponents;
import dev.youtubedisks.registry.ModItems;
import dev.youtubedisks.registry.ModMenuTypes;
import dev.youtubedisks.registry.ModSoundEvents;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;

@Mod(YoutubeDisks.MODID)
public class YoutubeDisks {

    public static final String MODID = "youtubedisks";
    public static final Logger LOGGER = LogUtils.getLogger();

    public YoutubeDisks(IEventBus modBus) {
        ModDataComponents.COMPONENTS.register(modBus);
        ModSoundEvents.SOUND_EVENTS.register(modBus);
        ModBlocks.BLOCKS.register(modBus);
        ModItems.ITEMS.register(modBus);
        ModBlockEntities.BLOCK_ENTITIES.register(modBus);
        ModMenuTypes.MENU_TYPES.register(modBus);
        ModCreativeTabs.TABS.register(modBus);

        LOGGER.info("Youtube Disks initialized");
    }
}
