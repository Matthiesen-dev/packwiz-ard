package dev.matthiesen.packwiz_ard.common.client;

import dev.matthiesen.packwiz_ard.common.shared.interfaces.PackStatus;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.function.Supplier;

public final class PackWizardStatusButton extends Button {
    private final Supplier<PackStatus> statusSupplier;

    public PackWizardStatusButton(int x, int y, int width, int height, OnPress onPress, Supplier<PackStatus> statusSupplier) {
        super(x, y, width, height, Component.empty(), onPress, DEFAULT_NARRATION);
        this.statusSupplier = statusSupplier;
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        PackStatus status = statusSupplier.get();

        setMessage(Component.empty());
        setTooltip(Tooltip.create(Component.literal(status.message())));

        super.renderWidget(graphics, mouseX, mouseY, partialTick);

        ItemStack iconStack = new ItemStack(getStatusIcon(status));
        int iconX = getX() + 2;
        int iconY = getY() + (height - 16) / 2;

        graphics.renderItem(iconStack, iconX, iconY);
    }

    private static Item getStatusIcon(PackStatus status) {
        return switch (status.state()) {
            case UNCONFIGURED -> Items.GRAY_WOOL;
            case CHECKING -> Items.COMPASS;
            case INVALID_URL, INVALID_TOML, UNREACHABLE, ERROR -> Items.BARRIER;
            case UP_TO_DATE -> Items.EMERALD;
            case UPDATE_AVAILABLE -> Items.YELLOW_WOOL;
            case UPDATING -> Items.CLOCK;
            case RESTART_REQUIRED -> Items.REDSTONE_TORCH;
        };
    }
}

