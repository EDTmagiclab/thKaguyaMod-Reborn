package thKaguyaMod.client.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import thKaguyaMod.gui.ContainerMerchantSanae;

/**
 * 早苗のアイテム交換 GUI（1.20.1 移植版，阶段 5，简化）
 * 原版 GuiMerchantSanae：商人交易界面（原版 IMerchant 为 null 半成品，
 * 完整交易留阶段 7）。此处复用原版村民 GUI 背景。
 */
public class GuiMerchantSanae extends AbstractContainerScreen<ContainerMerchantSanae>
{
    private static final ResourceLocation BG_TEXTURE =
            new ResourceLocation("textures/gui/container/villager.png");

    public GuiMerchantSanae(ContainerMerchantSanae menu, Inventory playerInventory, Component title)
    {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY)
    {
        guiGraphics.drawString(this.font, Component.translatable("entity.thkaguya.sanae"), 6, 6, 4210752, false);
        guiGraphics.drawString(this.font, Component.translatable("container.inventory"), 8, this.imageHeight - 96 + 2, 4210752, false);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY)
    {
        guiGraphics.blit(BG_TEXTURE, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);
    }
}
