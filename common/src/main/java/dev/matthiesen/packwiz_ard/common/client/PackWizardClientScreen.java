package dev.matthiesen.packwiz_ard.common.client;

import dev.matthiesen.packwiz_ard.common.shared.config.PWConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.NotNull;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

public final class PackWizardClientScreen extends Screen {
    private static final DateTimeFormatter STATUS_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault());
    private static final int BUTTON_WIDTH = 200;
    private static final int BUTTON_HEIGHT = 20;
    private static final int BUTTON_SPACING = 4;
    private static final int INFO_LINE_SPACING = 11;
    private static final int PANEL_PADDING = 8;

    private final Screen parent;

    private Button refreshButton;
    private Button updateButton;
    private Button refreshModeButton;
    private Button doneButton;

    public PackWizardClientScreen(Screen parent) {
        super(Component.translatable("packwiz_ard.client.screen.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        PackWizardClientCommon.INSTANCE.refreshStatus();

        int buttonX = this.width / 2 - BUTTON_WIDTH / 2;
        int buttonY = getButtonStartY();

        refreshButton = Button.builder(Component.translatable("packwiz_ard.client.button.refresh"), button -> PackWizardClientCommon.INSTANCE.refreshStatus())
                .bounds(buttonX, buttonY, BUTTON_WIDTH, BUTTON_HEIGHT)
                .build();
        updateButton = Button.builder(Component.translatable("packwiz_ard.client.button.update"), button -> PackWizardClientCommon.INSTANCE.startUpdate())
                .bounds(buttonX, buttonY + (BUTTON_HEIGHT + BUTTON_SPACING), BUTTON_WIDTH, BUTTON_HEIGHT)
                .build();
        refreshModeButton = Button.builder(Component.literal(""), button -> {
            boolean enabled = !PWConfig.CLIENT_CONFIG.autoRefreshEnabled.get();
            PWConfig.CLIENT_CONFIG.autoRefreshEnabled.set(enabled);
            PWConfig.CLIENT_CONFIG.autoRefreshEnabled.save();
            refreshControls();
        }).bounds(buttonX, buttonY + ((BUTTON_HEIGHT + BUTTON_SPACING) * 2), BUTTON_WIDTH, BUTTON_HEIGHT).build();
        doneButton = Button.builder(Component.translatable("gui.done"), button -> onClose())
                .bounds(buttonX, buttonY + ((BUTTON_HEIGHT + BUTTON_SPACING) * 3), BUTTON_WIDTH, BUTTON_HEIGHT)
                .build();

        addRenderableWidget(refreshButton);
        addRenderableWidget(updateButton);
        addRenderableWidget(refreshModeButton);
        addRenderableWidget(doneButton);
        refreshControls();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void onClose() {
        Minecraft minecraft = this.minecraft;
        if (minecraft != null) {
            minecraft.setScreen(parent);
        }
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        renderBackground(graphics, mouseX, mouseY, delta);
        refreshControls();
        super.render(graphics, mouseX, mouseY, delta);
        drawInfoPanel(graphics);
    }

    private void drawInfoPanel(GuiGraphics graphics) {
        int centerX = this.width / 2;
        int y = 16;
        int infoWidth = Math.min(360, this.width - 40);

        PackTomlStatus status = PackWizardClientCommon.INSTANCE.getStatus();
        graphics.drawCenteredString(this.font, this.title, centerX, y, 0xFFFFFF);
        y += 14;

        List<FormattedCharSequence> linkLines = this.font.split(Component.translatable(
                "packwiz_ard.client.screen.link",
                shortenLink(PackWizardClientCommon.INSTANCE.getPackTomlLink())
        ), infoWidth - (PANEL_PADDING * 2));
        List<FormattedCharSequence> validationLines = this.font.split(
                Component.translatable("packwiz_ard.client.screen.validation", status.message()),
                infoWidth - (PANEL_PADDING * 2)
        );
        List<FormattedCharSequence> refreshModeLines = this.font.split(Component.translatable(
                "packwiz_ard.client.screen.refresh_mode",
                PWConfig.CLIENT_CONFIG.autoRefreshEnabled.get()
                        ? Component.translatable("packwiz_ard.client.screen.refresh_mode.auto", PWConfig.CLIENT_CONFIG.autoRefreshIntervalSeconds.get())
                        : Component.translatable("packwiz_ard.client.screen.refresh_mode.manual")
        ), infoWidth - (PANEL_PADDING * 2));

        String checkedAt = status.checkedAtMillis() > 0L ? STATUS_TIME_FORMAT.format(Instant.ofEpochMilli(status.checkedAtMillis())) : "-";
        List<FormattedCharSequence> checkedAtLines = this.font.split(
                Component.translatable("packwiz_ard.client.screen.last_checked", checkedAt),
                infoWidth - (PANEL_PADDING * 2)
        );
        List<FormattedCharSequence> restartLines = status.restartRequired()
                ? this.font.split(Component.translatable("packwiz_ard.client.screen.restart_required"), infoWidth - (PANEL_PADDING * 2))
                : List.of();

        int panelTop = y + 4;
        int lineCount = linkLines.size() + validationLines.size() + refreshModeLines.size() + checkedAtLines.size() + restartLines.size();
        int spacerCount = 4 + (status.restartRequired() ? 1 : 0);
        int panelHeight = (PANEL_PADDING * 2) + (lineCount * INFO_LINE_SPACING) + (spacerCount * 2);

        int panelLeft = centerX - (infoWidth / 2);
        int panelRight = centerX + (infoWidth / 2);
        int panelBottom = panelTop + panelHeight;

        // Vanilla-like subtle info container.
        graphics.fill(panelLeft, panelTop, panelRight, panelBottom, 0x88000000);
        graphics.fill(panelLeft, panelTop, panelRight, panelTop + 1, 0x66FFFFFF);
        graphics.fill(panelLeft, panelBottom - 1, panelRight, panelBottom, 0x66000000);
        graphics.fill(panelLeft, panelTop, panelLeft + 1, panelBottom, 0x66FFFFFF);
        graphics.fill(panelRight - 1, panelTop, panelRight, panelBottom, 0x66000000);

        int textX = panelLeft + PANEL_PADDING;
        int textY = panelTop + PANEL_PADDING;
        textY = drawWrappedLeft(graphics, linkLines, textX, textY, 0xE0E0E0);
        textY += 2;
        textY = drawWrappedLeft(graphics, validationLines, textX, textY, statusColor(status));
        textY += 2;
        textY = drawWrappedLeft(graphics, refreshModeLines, textX, textY, 0xD0D0D0);
        textY += 2;
        textY = drawWrappedLeft(graphics, checkedAtLines, textX, textY, 0xD0D0D0);

        if (status.restartRequired()) {
            textY += 2;
            drawWrappedLeft(graphics, restartLines, textX, textY, 0xFF6B6B);
        }
    }

    private int drawWrappedLeft(GuiGraphics graphics, List<FormattedCharSequence> lines, int x, int startY, int color) {
        int y = startY;
        for (FormattedCharSequence line : lines) {
            graphics.drawString(this.font, line, x, y, color, false);
            y += INFO_LINE_SPACING;
        }
        return y + 2;
    }

    private int getInfoPanelBottom() {
        int infoWidth = Math.min(360, this.width - 40);
        PackTomlStatus status = PackWizardClientCommon.INSTANCE.getStatus();

        int lineCount = 0;
        lineCount += this.font.split(Component.translatable(
                "packwiz_ard.client.screen.link",
                shortenLink(PackWizardClientCommon.INSTANCE.getPackTomlLink())
        ), infoWidth - (PANEL_PADDING * 2)).size();
        lineCount += this.font.split(
                Component.translatable("packwiz_ard.client.screen.validation", status.message()),
                infoWidth - (PANEL_PADDING * 2)
        ).size();
        lineCount += this.font.split(Component.translatable(
                "packwiz_ard.client.screen.refresh_mode",
                PWConfig.CLIENT_CONFIG.autoRefreshEnabled.get()
                        ? Component.translatable("packwiz_ard.client.screen.refresh_mode.auto", PWConfig.CLIENT_CONFIG.autoRefreshIntervalSeconds.get())
                        : Component.translatable("packwiz_ard.client.screen.refresh_mode.manual")
        ), infoWidth - (PANEL_PADDING * 2)).size();

        String checkedAt = status.checkedAtMillis() > 0L ? STATUS_TIME_FORMAT.format(Instant.ofEpochMilli(status.checkedAtMillis())) : "-";
        lineCount += this.font.split(
                Component.translatable("packwiz_ard.client.screen.last_checked", checkedAt),
                infoWidth - (PANEL_PADDING * 2)
        ).size();

        if (status.restartRequired()) {
            lineCount += this.font.split(Component.translatable("packwiz_ard.client.screen.restart_required"), infoWidth - (PANEL_PADDING * 2)).size();
        }

        int spacerCount = 4 + (status.restartRequired() ? 1 : 0);
        int panelHeight = (PANEL_PADDING * 2) + (lineCount * INFO_LINE_SPACING) + (spacerCount * 2);
        return 16 + 14 + 4 + panelHeight;
    }

    private int getButtonStartY() {
        int buttonBlockHeight = (BUTTON_HEIGHT * 4) + (BUTTON_SPACING * 3);
        int candidate = getInfoPanelBottom() + 12;
        int maxAllowed = this.height - buttonBlockHeight - 10;
        return Mth.clamp(candidate, 86, maxAllowed);
    }

    private void refreshControls() {
        PackTomlStatus status = PackWizardClientCommon.INSTANCE.getStatus();
        boolean autoRefreshEnabled = PWConfig.CLIENT_CONFIG.autoRefreshEnabled.get();
        int intervalSeconds = PWConfig.CLIENT_CONFIG.autoRefreshIntervalSeconds.get();

        if (refreshButton != null) {
            refreshButton.active = !PackWizardClientCommon.INSTANCE.isValidationInProgress() && !status.restartRequired();
        }

        if (updateButton != null) {
            updateButton.setMessage(status.restartRequired()
                    ? Component.translatable("packwiz_ard.client.button.restart_required")
                    : Component.translatable("packwiz_ard.client.button.update"));
            updateButton.active = status.canUpdate() && !PackWizardClientCommon.INSTANCE.isUpdateInProgress() && !PackWizardClientCommon.INSTANCE.isValidationInProgress();
        }

        if (refreshModeButton != null) {
            refreshModeButton.setMessage(autoRefreshEnabled
                    ? Component.translatable("packwiz_ard.client.button.auto_refresh", intervalSeconds)
                    : Component.translatable("packwiz_ard.client.button.manual_refresh"));
            refreshModeButton.active = !status.restartRequired();
        }

        if (doneButton != null) {
            doneButton.active = true;
        }
    }

    private static int statusColor(PackTomlStatus status) {
        return switch (status.state()) {
            case UNCONFIGURED -> 0xAAAAAA;
            case CHECKING, UPDATING -> 0xE0B64A;
            case INVALID_URL, INVALID_TOML, UNREACHABLE, ERROR, RESTART_REQUIRED -> 0xFF5555;
            case UP_TO_DATE -> 0x55FF55;
            case UPDATE_AVAILABLE -> 0xFFAA33;
        };
    }

    private static String shortenLink(String link) {
        if (link == null || link.isBlank()) {
            return "(not configured)";
        }

        if (link.length() <= 110) {
            return link;
        }

        return link.substring(0, Mth.clamp(110, 0, link.length())) + "...";
    }
}

