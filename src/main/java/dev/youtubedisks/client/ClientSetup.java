package dev.youtubedisks.client;

import dev.youtubedisks.YoutubeDisks;
import dev.youtubedisks.client.audio.DiscAudioPlayer;
import dev.youtubedisks.client.speaker.SpeakerScreen;
import dev.youtubedisks.registry.ModMenuTypes;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.common.NeoForge;

@EventBusSubscriber(modid = YoutubeDisks.MODID, value = Dist.CLIENT)
public final class ClientSetup {

    @SubscribeEvent
    public static void onRegisterMenuScreens(RegisterMenuScreensEvent event) {
        event.register(ModMenuTypes.DISK_RECORDER.get(), DiskRecorderScreen::new);
        event.register(ModMenuTypes.SPEAKER.get(),       SpeakerScreen::new);
    }

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        NeoForge.EVENT_BUS.addListener(ClientSetup::onClientTick);
        YoutubeDisks.LOGGER.info("[client] game-bus listeners installed (tick)");
    }

    private static void onClientTick(ClientTickEvent.Post event) {
        DiscAudioPlayer.tick();
    }

    private ClientSetup() {}
}
