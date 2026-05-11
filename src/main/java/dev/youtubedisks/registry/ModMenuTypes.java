package dev.youtubedisks.registry;

import dev.youtubedisks.YoutubeDisks;
import dev.youtubedisks.menu.DiskRecorderMenu;
import dev.youtubedisks.speaker.SpeakerMenu;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public final class ModMenuTypes {

    public static final DeferredRegister<MenuType<?>> MENU_TYPES =
        DeferredRegister.create(Registries.MENU, YoutubeDisks.MODID);

    public static final Supplier<MenuType<DiskRecorderMenu>> DISK_RECORDER = MENU_TYPES.register(
        "disk_recorder",
        () -> IMenuTypeExtension.create((containerId, inv, buf) ->
            new DiskRecorderMenu(containerId, inv, buf.readBlockPos(), ContainerLevelAccess.NULL))
    );

    public static final Supplier<MenuType<SpeakerMenu>> SPEAKER = MENU_TYPES.register(
        "speaker",
        () -> IMenuTypeExtension.create((containerId, inv, buf) ->
            new SpeakerMenu(containerId, inv, buf.readBlockPos(), ContainerLevelAccess.NULL))
    );

    private ModMenuTypes() {}
}
