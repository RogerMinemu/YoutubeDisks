package dev.youtubedisks.block;

import dev.youtubedisks.menu.DiskRecorderMenu;
import dev.youtubedisks.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public class DiskRecorderBlockEntity extends BlockEntity implements MenuProvider {

    private String pendingUrl = "";

    public DiskRecorderBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.DISK_RECORDER.get(), pos, state);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.youtubedisks.disk_recorder");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new DiskRecorderMenu(containerId, playerInventory, worldPosition,
            ContainerLevelAccess.create(level, worldPosition));
    }

    public String getPendingUrl() {
        return pendingUrl;
    }

    public void setPendingUrl(String url) {
        this.pendingUrl = url == null ? "" : url;
        setChanged();
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider lookup) {
        super.saveAdditional(tag, lookup);
        tag.putString("PendingUrl", pendingUrl);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider lookup) {
        super.loadAdditional(tag, lookup);
        pendingUrl = tag.getString("PendingUrl");
    }
}
