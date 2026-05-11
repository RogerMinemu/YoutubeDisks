package dev.youtubedisks.menu;

import dev.youtubedisks.registry.ModBlocks;
import dev.youtubedisks.registry.ModMenuTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class DiskRecorderMenu extends AbstractContainerMenu {

    private final ContainerLevelAccess access;
    private final BlockPos blockPos;

    public DiskRecorderMenu(int containerId, Inventory playerInventory, BlockPos blockPos, ContainerLevelAccess access) {
        super(ModMenuTypes.DISK_RECORDER.get(), containerId);
        this.blockPos = blockPos;
        this.access = access;

        int invX = 8;
        int invY = 84;
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(playerInventory, col + row * 9 + 9, invX + col * 18, invY + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(playerInventory, col, invX + col * 18, invY + 58));
        }
    }

    public BlockPos getBlockPos() {
        return blockPos;
    }

    @Override
    public boolean stillValid(Player player) {
        return AbstractContainerMenu.stillValid(access, player, ModBlocks.DISK_RECORDER.get());
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }
}
