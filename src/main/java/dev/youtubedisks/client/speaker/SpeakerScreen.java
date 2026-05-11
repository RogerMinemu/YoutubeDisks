package dev.youtubedisks.client.speaker;

import dev.youtubedisks.YoutubeDisks;
import dev.youtubedisks.client.recording.ClientRecordingService;
import dev.youtubedisks.client.recording.ClientRecordingService.State;
import dev.youtubedisks.client.recording.ClientRecordingService.Status;
import dev.youtubedisks.network.SpeakerCommandPayload;
import dev.youtubedisks.speaker.SpeakerMenu;
import dev.youtubedisks.speaker.Track;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;

public class SpeakerScreen extends AbstractContainerScreen<SpeakerMenu> {

    private static final int PANEL_BG    = 0xFFC6C6C6;
    private static final int PANEL_INSET = 0xFF8B8B8B;
    private static final int TITLE_STRIP = 0xFF555555;
    private static final int ACCENT_RED  = 0xFFCC2424;
    private static final int PROGRESS_BG = 0xFF6E6E6E;
    private static final int PROGRESS_FG = 0xFFCC2424;

    private static final int COLOR_BUSY  = 0xFFFFAA00;
    private static final int COLOR_DONE  = 0xFF40C040;
    private static final int COLOR_ERROR = 0xFFFF4040;

    private static final int TRACK_ROW_HEIGHT = 11;
    private static final int VISIBLE_TRACKS = 5;

    private EditBox urlField;
    private Button playStopButton;
    private Button nextButton;
    private Button addButton;

    public SpeakerScreen(SpeakerMenu menu, Inventory playerInv, Component title) {
        super(menu, playerInv, title);
        this.imageWidth = 176;
        this.imageHeight = 240;
        this.titleLabelY = 6;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void init() {
        super.init();
        int x = this.leftPos;
        int y = this.topPos;

        playStopButton = Button.builder(Component.translatable("youtubedisks.speaker.play"), btn -> onPlayStop())
            .bounds(x + 8, y + 70, 60, 18).build();
        addRenderableWidget(playStopButton);

        nextButton = Button.builder(Component.translatable("youtubedisks.speaker.next"), btn -> onNext())
            .bounds(x + 72, y + 70, 60, 18).build();
        addRenderableWidget(nextButton);

        urlField = new EditBox(this.font, x + 24, y + 94, 144, 16, Component.empty());
        urlField.setMaxLength(256);
        urlField.setHint(Component.translatable("youtubedisks.gui.url_placeholder"));
        addRenderableWidget(urlField);

        addButton = Button.builder(Component.literal("+"), btn -> onAdd())
            .bounds(x + 8, y + 93, 14, 18).build();
        addRenderableWidget(addButton);

        setInitialFocus(urlField);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        ClientSpeakerState.State state = ClientSpeakerState.get(this.menu.getBlockPos());
        syncWidgetsTo(state);
        renderHeader(g, state);
        renderProgressBar(g, state);
        renderRecordingStatusLine(g);
        renderPlaylist(g, state, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;
        g.fill(x,     y,     x + imageWidth,     y + imageHeight,     PANEL_BG);
        g.fill(x + 4, y + 4, x + imageWidth - 4, y + imageHeight - 4, PANEL_INSET);
        g.fill(x + 4, y + 4, x + imageWidth - 4, y + 18,              TITLE_STRIP);
        // REC accent dot in title strip
        g.fill(x + imageWidth - 14, y + 8, x + imageWidth - 10, y + 12, ACCENT_RED);
    }

    // ------------------------------------------------------------------ UI helpers

    private void renderHeader(GuiGraphics g, ClientSpeakerState.State state) {
        int x = this.leftPos;
        int y = this.topPos;
        Track current = state.currentTrack();
        String title = current == null
            ? this.font.plainSubstrByWidth(I18n("youtubedisks.speaker.no_track"), 160)
            : this.font.plainSubstrByWidth(current.title(), 160);
        g.drawString(this.font, title, x + 8, y + 22, 0xFFFFFFFF, false);
    }

    private void renderProgressBar(GuiGraphics g, ClientSpeakerState.State state) {
        int x = this.leftPos;
        int y = this.topPos;
        Track current = state.currentTrack();
        int total = current == null ? 0 : current.durationSeconds();
        long elapsedSec = state.displayedElapsedMillis() / 1000L;
        if (total > 0 && elapsedSec > total) elapsedSec = total;

        int barLeft = x + 8;
        int barRight = x + imageWidth - 8;
        int barY = y + 56;
        int barH = 6;
        g.fill(barLeft, barY, barRight, barY + barH, PROGRESS_BG);
        if (total > 0) {
            int fillRight = barLeft + (int)((barRight - barLeft) * (elapsedSec / (double) total));
            g.fill(barLeft, barY, fillRight, barY + barH, PROGRESS_FG);
        }
        g.drawString(this.font, formatTime((int) elapsedSec), barLeft, barY + 8, 0xFFCCCCCC, false);
        String dur = formatTime(total);
        g.drawString(this.font, dur, barRight - this.font.width(dur), barY + 8, 0xFFCCCCCC, false);
    }

    private void renderRecordingStatusLine(GuiGraphics g) {
        Status status = ClientRecordingService.get().getStatus();
        if (status.state() == State.IDLE) return;
        Component msg = switch (status.state()) {
            case EXTRACTING  -> Component.translatable("youtubedisks.status.extracting");
            case TRANSCODING -> Component.translatable("youtubedisks.status.transcoding", status.title());
            case UPLOADING   -> Component.translatable("youtubedisks.status.uploading");
            case COMPLETE    -> Component.translatable("youtubedisks.speaker.added", status.title());
            case ERROR       -> Component.translatable("youtubedisks.status.error", status.detail());
            default          -> Component.empty();
        };
        int color = switch (status.state()) {
            case EXTRACTING, TRANSCODING, UPLOADING -> COLOR_BUSY;
            case COMPLETE                           -> COLOR_DONE;
            case ERROR                              -> COLOR_ERROR;
            default                                 -> 0xFFFFFFFF;
        };
        g.drawString(this.font, msg, this.leftPos + 8, this.topPos + 116, color, false);
    }

    private void renderPlaylist(GuiGraphics g, ClientSpeakerState.State state, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;
        int listX = x + 8;
        int listY = y + 130;
        int listWidth = imageWidth - 16;
        int listHeight = TRACK_ROW_HEIGHT * VISIBLE_TRACKS;
        g.fill(listX, listY, listX + listWidth, listY + listHeight, 0xFF454545);

        int rowCount = Math.min(VISIBLE_TRACKS, state.playlist().size());
        for (int i = 0; i < rowCount; i++) {
            Track track = state.playlist().get(i);
            int rowY = listY + i * TRACK_ROW_HEIGHT;
            boolean isCurrent = i == state.currentIndex();
            boolean hover = mouseX >= listX && mouseX < listX + listWidth
                         && mouseY >= rowY && mouseY < rowY + TRACK_ROW_HEIGHT;
            int bg = isCurrent ? 0xFF704040 : (hover ? 0xFF555555 : 0xFF454545);
            g.fill(listX, rowY, listX + listWidth, rowY + TRACK_ROW_HEIGHT, bg);
            String label = (isCurrent ? "▶ " : "  ") + track.title();
            g.drawString(this.font, this.font.plainSubstrByWidth(label, listWidth - 4),
                listX + 2, rowY + 1, 0xFFFFFFFF, false);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            int listX = this.leftPos + 8;
            int listY = this.topPos + 130;
            int listWidth = imageWidth - 16;
            if (mouseX >= listX && mouseX < listX + listWidth) {
                int rowIdx = (int) ((mouseY - listY) / TRACK_ROW_HEIGHT);
                ClientSpeakerState.State state = ClientSpeakerState.get(this.menu.getBlockPos());
                if (rowIdx >= 0 && rowIdx < state.playlist().size()) {
                    PacketDistributor.sendToServer(SpeakerCommandPayload.selectTrack(this.menu.getBlockPos(), rowIdx));
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void syncWidgetsTo(ClientSpeakerState.State state) {
        boolean canPlay = !state.playlist().isEmpty();
        playStopButton.active = canPlay;
        playStopButton.setMessage(state.playing()
            ? Component.translatable("youtubedisks.speaker.stop")
            : Component.translatable("youtubedisks.speaker.play"));
        nextButton.active = state.playlist().size() > 1;

        Status status = ClientRecordingService.get().getStatus();
        boolean recording = status.isBusy();
        urlField.setEditable(!recording);
        addButton.active = !recording && !urlField.getValue().isBlank();
    }

    private void onPlayStop() {
        ClientSpeakerState.State state = ClientSpeakerState.get(this.menu.getBlockPos());
        BlockPos pos = this.menu.getBlockPos();
        PacketDistributor.sendToServer(state.playing()
            ? SpeakerCommandPayload.stop(pos)
            : SpeakerCommandPayload.play(pos));
    }

    private void onNext() {
        PacketDistributor.sendToServer(SpeakerCommandPayload.next(this.menu.getBlockPos()));
    }

    private void onAdd() {
        String url = urlField.getValue();
        if (url == null || url.isBlank()) return;
        Status status = ClientRecordingService.get().getStatus();
        if (status.isBusy()) return;

        // The server adds the track to this speaker automatically when the recording finishes
        // (it routes by the BlockPos passed in RecordingBeginPayload). No client-side follow-up
        // packet needed — works even if the GUI is closed mid-recording.
        ClientRecordingService.get().reset();
        ClientRecordingService.get().startRecording(url, this.menu.getBlockPos());
        urlField.setValue("");
    }

    private static String I18n(String key) {
        return net.minecraft.client.resources.language.I18n.get(key);
    }

    private static String formatTime(int seconds) {
        int m = seconds / 60;
        int s = seconds % 60;
        return String.format("%d:%02d", m, s);
    }
}
