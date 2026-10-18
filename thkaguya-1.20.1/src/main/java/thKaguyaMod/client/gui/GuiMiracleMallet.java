package thKaguyaMod.client.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import thKaguyaMod.THKaguyaCore;
import thKaguyaMod.gui.ContainerMiracleMallet;

/**
 * 打ち出の小槌 GUI（1.20.1 移植版，阶段 5）
 * 原版 GuiMiracleMallet：复用弹幕工作台背景 + 素材/结果标签（原版 3 个 SWING 按钮未接线，略）。
 */
public class GuiMiracleMallet extends AbstractContainerScreen<ContainerMiracleMallet>
{
    private static final ResourceLocation BG_TEXTURE =
            new ResourceLocation(THKaguyaCore.MODID, "textures/gui/danmaku_crafting_table.png");

    public GuiMiracleMallet(ContainerMiracleMallet menu, Inventory playerInventory, Component title)
    {
        super(menu, playerInventory, title);
        this.imageWidth = 256;
        this.imageHeight = 166;
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY)
    {
        guiGraphics.drawString(this.font, Component.translatable("item.thkaguya.miracle_mallet"), 6, 6, 4210752, false);
        guiGraphics.drawString(this.font, Component.translatable("miracleMallet.material"), 134, 6, 4210752, false);
        guiGraphics.drawString(this.font, Component.translatable("miracleMallet.result"), 192, 6, 4210752, false);
        guiGraphics.drawString(this.font, Component.translatable("danmakuCrafting.material"), 26, 22, 4210752, false);
        guiGraphics.drawString(this.font, Component.translatable("container.inventory"), 48, this.imageHeight - 96 + 2, 4210752, false);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY)
    {
        guiGraphics.blit(BG_TEXTURE, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);
    }
}
