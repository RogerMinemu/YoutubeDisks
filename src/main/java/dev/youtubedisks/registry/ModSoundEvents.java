package dev.youtubedisks.registry;

import dev.youtubedisks.YoutubeDisks;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public final class ModSoundEvents {

    public static final DeferredRegister<SoundEvent> SOUND_EVENTS =
        DeferredRegister.create(Registries.SOUND_EVENT, YoutubeDisks.MODID);

    /**
     * Marker SoundEvent referenced by the {@code recorded_disc} JukeboxSong. The sounds.json
     * entry has an empty sounds[] array so vanilla audio plays nothing — our custom OpenAL
     * pipeline takes over at the same moment via {@code PlaySoundEvent}.
     */
    public static final Supplier<SoundEvent> DISC_SILENT = SOUND_EVENTS.register(
        "disc_silent",
        () -> SoundEvent.createVariableRangeEvent(
            ResourceLocation.fromNamespaceAndPath(YoutubeDisks.MODID, "disc_silent"))
    );

    public static final ResourceLocation DISC_SILENT_ID =
        ResourceLocation.fromNamespaceAndPath(YoutubeDisks.MODID, "disc_silent");

    private ModSoundEvents() {}
}
