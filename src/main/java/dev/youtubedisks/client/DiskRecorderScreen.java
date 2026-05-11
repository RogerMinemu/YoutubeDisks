package dev.youtubedisks.client;

import dev.youtubedisks.client.recording.ClientRecordingService;
import dev.youtubedisks.client.recording.ClientRecordingService.State;
import dev.youtubedisks.client.recording.ClientRecordingService.Status;
import dev.youtubedisks.menu.DiskRecorderMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class DiskRecorderScreen extends AbstractContainerScreen<DiskRecorderMenu> {

    private static final int PANEL_BG     = 0xFFC6C6C6;
    private static final int PANEL_INSET  = 0xFF8B8B8B;
    private static final int TITLE_STRIP  = 0xFF555555;
    private static final int ACCENT_RED   = 0xFFCC2424;

    private static final int COLOR_BUSY  = 0xFFFFAA00;
    private static final int COLOR_DONE  = 0xFF40C040;
    private static final int COLOR_ERROR = 0xFFFF4040;

    private EditBox urlField;
    private Button recordButton;

    public DiskRecorderScreen(DiskRecorderMenu menu, Inventory playerInv, Component title) {
        super(menu, playerInv, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
        this.titleLabelY = 6;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void init() {
        super.init();

        int fieldX = this.leftPos + 8;
        int fieldY = this.topPos + 22;

        urlField = new EditBox(this.font, fieldX, fieldY, 160, 18, Component.empty());
        urlField.setMaxLength(256);
        urlField.setHint(Component.translatable("youtubedisks.gui.url_placeholder"));
        addRenderableWidget(urlField);
        setInitialFocus(urlField);

        recordButton = Button.builder(
            Component.translatable("youtubedisks.gui.record_button"),
            btn -> onRecord()
        ).bounds(fieldX, fieldY + 24, 160, 18).build();
        addRenderableWidget(recordButton);
    }

    private void onRecord() {
        Status status = ClientRecordingService.get().getStatus();
        if (status.isFinal()) {
            // Second click after a finished run clears the state so the user can start again.
            ClientRecordingService.get().reset();
            urlField.setValue("");
            return;
        }
        String url = urlField == null ? null : urlField.getValue();
        if (url == null || url.isBlank()) {
            return;
        }
        BlockPos pos = this.menu.getBlockPos();
        ClientRecordingService.get().startRecording(url, pos);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        Status status = ClientRecordingService.get().getStatus();
        syncWidgetsTo(status);

        super.render(g, mouseX, mouseY, partialTick);

        renderStatusLine(g, status);
    }

    private void syncWidgetsTo(Status status) {
        boolean busy = status.isBusy();
        urlField.setEditable(!busy);

        boolean canStart = !busy && !urlField.getValue().isBlank();
        recordButton.active = canStart || status.isFinal();

        Component label;
        switch (status.state()) {
            case EXTRACTING, TRANSCODING, UPLOADING ->
                label = Component.translatable("youtubedisks.gui.record_button.busy");
            case COMPLETE ->
                label = Component.translatable("youtubedisks.gui.record_button.again");
            case ERROR ->
                label = Component.translatable("youtubedisks.gui.record_button.retry");
            default ->
                label = Component.translatable("youtubedisks.gui.record_button");
        }
        recordButton.setMessage(label);
    }

    private void renderStatusLine(GuiGraphics g, Status status) {
        if (status.state() == State.IDLE) {
            return;
        }
        Component msg = statusMessage(status);
        int color = statusColor(status.state());
        int x = this.leftPos + 8;
        int y = this.topPos + 68;
        g.drawString(this.font, msg, x, y, color, false);
    }

    private static Component statusMessage(Status status) {
        return switch (status.state()) {
            case EXTRACTING  -> Component.translatable("youtubedisks.status.extracting");
            case TRANSCODING -> Component.translatable("youtubedisks.status.transcoding", status.title());
            case UPLOADING   -> Component.translatable("youtubedisks.status.uploading");
            case COMPLETE    -> Component.translatable("youtubedisks.status.complete", status.title(), formatDuration(status.durationSeconds()));
            case ERROR       -> Component.translatable("youtubedisks.status.error", status.detail());
            default          -> Component.empty();
        };
    }

    private static int statusColor(State state) {
        return switch (state) {
            case EXTRACTING, TRANSCODING, UPLOADING -> COLOR_BUSY;
            case COMPLETE                           -> COLOR_DONE;
            case ERROR                              -> COLOR_ERROR;
            default                                 -> 0xFF000000;
        };
    }

    private static String formatDuration(int seconds) {
        int mins = seconds / 60;
        int secs = seconds % 60;
        return String.format("%d:%02d", mins, secs);
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;
        g.fill(x,     y,     x + imageWidth,     y + imageHeight,     PANEL_BG);
        g.fill(x + 4, y + 4, x + imageWidth - 4, y + imageHeight - 4, PANEL_INSET);
        g.fill(x + 4, y + 4, x + imageWidth - 4, y + 18,              TITLE_STRIP);
        g.fill(x + imageWidth - 14, y + 8, x + imageWidth - 10, y + 12, ACCENT_RED);
    }
}
