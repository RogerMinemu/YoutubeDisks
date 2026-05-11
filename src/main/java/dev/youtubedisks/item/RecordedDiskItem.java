package dev.youtubedisks.item;

import dev.youtubedisks.client.playback.ClientPlaybackController;
import dev.youtubedisks.disc.DiscData;
import dev.youtubedisks.registry.ModDataComponents;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;

import java.util.List;

public class RecordedDiskItem extends Item {

    public RecordedDiskItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide) {
            // Server-side: nothing to do. Playback is fully client-driven (client decodes,
            // caches, and plays — only requests bytes from server if not cached).
            return InteractionResultHolder.success(stack);
        }
        DiscData data = stack.get(ModDataComponents.DISC_DATA.get());
        if (data == null) {
            return InteractionResultHolder.pass(stack);
        }
        if (FMLEnvironment.dist == Dist.CLIENT) {
            ClientPlaybackController.toggle(data);
        }
        return InteractionResultHolder.success(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        DiscData data = stack.get(ModDataComponents.DISC_DATA.get());
        if (data == null) {
            tooltip.add(Component.translatable("youtubedisks.tooltip.empty_disc")
                .withStyle(ChatFormatting.DARK_GRAY));
            return;
        }
        tooltip.add(Component.literal(data.title()).withStyle(ChatFormatting.GRAY));
        int mins = data.durationSeconds() / 60;
        int secs = data.durationSeconds() % 60;
        tooltip.add(Component.literal(String.format("%d:%02d", mins, secs))
            .withStyle(ChatFormatting.DARK_GRAY));
    }
}
