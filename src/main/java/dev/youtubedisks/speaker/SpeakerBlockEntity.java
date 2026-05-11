package dev.youtubedisks.speaker;

import dev.youtubedisks.network.SpeakerStatePayload;
import dev.youtubedisks.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SpeakerBlockEntity extends BlockEntity implements MenuProvider {

    private final List<Track> playlist = new ArrayList<>();
    private int currentIndex = -1;
    private boolean playing = false;
    private long playStartMillis = 0L;

    public SpeakerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.SPEAKER.get(), pos, state);
    }

    // ------------------------------------------------------------------ accessors

    public List<Track> getPlaylist() {
        return Collections.unmodifiableList(playlist);
    }

    public int getCurrentIndex() {
        return currentIndex;
    }

    public boolean isPlaying() {
        return playing;
    }

    @Nullable
    public Track currentTrack() {
        if (currentIndex < 0 || currentIndex >= playlist.size()) {
            return null;
        }
        return playlist.get(currentIndex);
    }

    public long elapsedMillis() {
        if (!playing || playStartMillis == 0L) {
            return 0L;
        }
        return Math.max(0L, System.currentTimeMillis() - playStartMillis);
    }

    // ------------------------------------------------------------------ mutators (server-side)

    public void addTrack(Track track) {
        playlist.add(track);
        if (currentIndex < 0) {
            currentIndex = 0;
        }
        setChanged();
        broadcastState();
    }

    public void selectTrack(int idx) {
        if (idx < 0 || idx >= playlist.size()) {
            return;
        }
        currentIndex = idx;
        playing = false;
        playStartMillis = 0L;
        setChanged();
        broadcastState();
    }

    public void play() {
        if (playlist.isEmpty()) {
            return;
        }
        if (currentIndex < 0) {
            currentIndex = 0;
        }
        playing = true;
        playStartMillis = System.currentTimeMillis();
        setChanged();
        broadcastState();
    }

    public void stop() {
        playing = false;
        playStartMillis = 0L;
        setChanged();
        broadcastState();
    }

    public void next() {
        if (playlist.isEmpty()) {
            return;
        }
        boolean wasPlaying = playing;
        currentIndex = (currentIndex + 1) % playlist.size();
        playStartMillis = wasPlaying ? System.currentTimeMillis() : 0L;
        setChanged();
        broadcastState();
    }

    // ------------------------------------------------------------------ server tick (auto-advance)

    public static void serverTick(Level level, BlockPos pos, BlockState state, SpeakerBlockEntity be) {
        if (!be.playing) return;
        Track current = be.currentTrack();
        if (current == null) {
            be.stop();
            return;
        }
        long elapsed = System.currentTimeMillis() - be.playStartMillis;
        if (elapsed < (long) current.durationSeconds() * 1000L) {
            return;
        }
        // Track ended.
        if (be.currentIndex + 1 < be.playlist.size()) {
            be.currentIndex++;
            be.playStartMillis = System.currentTimeMillis();
            be.setChanged();
            be.broadcastState();
        } else {
            // End of playlist
            be.playing = false;
            be.playStartMillis = 0L;
            be.setChanged();
            be.broadcastState();
        }
    }

    // ------------------------------------------------------------------ networking

    public void broadcastState() {
        if (!(level instanceof ServerLevel sl)) return;
        SpeakerStatePayload payload = buildStatePayload();
        PacketDistributor.sendToPlayersTrackingChunk(sl, new ChunkPos(worldPosition), payload);
    }

    public void sendStateTo(ServerPlayer player) {
        PacketDistributor.sendToPlayer(player, buildStatePayload());
    }

    private SpeakerStatePayload buildStatePayload() {
        return new SpeakerStatePayload(
            worldPosition,
            new ArrayList<>(playlist),
            currentIndex,
            playing,
            elapsedMillis()
        );
    }

    // ------------------------------------------------------------------ menu

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.youtubedisks.speaker");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new SpeakerMenu(containerId, playerInventory, worldPosition,
            ContainerLevelAccess.create(level, worldPosition));
    }

    // ------------------------------------------------------------------ NBT

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider lookup) {
        super.saveAdditional(tag, lookup);
        ListTag list = new ListTag();
        for (Track t : playlist) {
            CompoundTag tt = new CompoundTag();
            tt.putString("disc_id", t.discId());
            tt.putString("title", t.title());
            tt.putInt("duration_seconds", t.durationSeconds());
            list.add(tt);
        }
        tag.put("Playlist", list);
        tag.putInt("CurrentIndex", currentIndex);
        // Deliberately NOT persisting `playing`/`playStartMillis` — system time can't survive a
        // world reload, so a paused-at-load semantics keeps things consistent.
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider lookup) {
        super.loadAdditional(tag, lookup);
        playlist.clear();
        ListTag list = tag.getList("Playlist", Tag.TAG_COMPOUND);
        for (Tag t : list) {
            CompoundTag tt = (CompoundTag) t;
            playlist.add(new Track(
                tt.getString("disc_id"),
                tt.getString("title"),
                tt.getInt("duration_seconds")
            ));
        }
        currentIndex = tag.contains("CurrentIndex") ? tag.getInt("CurrentIndex") : -1;
        playing = false;
        playStartMillis = 0L;
    }
}
