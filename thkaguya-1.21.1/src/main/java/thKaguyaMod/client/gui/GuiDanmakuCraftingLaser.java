package thKaguyaMod.client.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import thKaguyaMod.THKaguyaCore;
import thKaguyaMod.gui.ContainerDanmakuCraftingLaser;

/**
 * レーザー作業台 GUI（1.20.1 移植版，阶段 5）
 * 原版 GuiDanmakuCraftingLaser：激光工作台背景 + 素材/形状/复制标签。
 */
public class GuiDanmakuCraftingLaser extends AbstractContainerScreen<ContainerDanmakuCraftingLaser>
{
    private static final ResourceLocation BG_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(THKaguyaCore.MODID, "textures/gui/laser_crafting_table.png");

    public GuiDanmakuCraftingLaser(ContainerDanmakuCraftingLaser menu, Inventory playerInventory, Component title)
    {
        super(menu, playerInventory, title);
        this.imageWidth = 256;
        this.imageHeight = 166;
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY)
    {
        guiGraphics.drawString(this.font, Component.translatable("laserCrafting.name"), 6, 6, 4210752, false);
        guiGraphics.drawString(this.font, Component.translatable("danmakuCrafting.form"), 134, 6, 4210752, false);
        guiGraphics.drawString(this.font, Component.translatable("danmakuCrafting.copy"), 192, 6, 4210752, false);
        guiGraphics.drawString(this.font, Component.translatable("danmakuCrafting.material"), 26, 22, 4210752, false);
        guiGraphics.drawString(this.font, Component.translatable("danmakuCrafting.number"), 26, 39, 4210752, false);
        guiGraphics.drawString(this.font, Component.translatable("danmakuCrafting.speed"), 26, 56, 4210752, false);
        guiGraphics.drawString(this.font, Component.translatable("danmakuCrafting.color"), 87, 22, 4210752, false);
        guiGraphics.drawString(this.font, Component.translatable("container.inventory"), 48, this.imageHeight - 96 + 2, 4210752, false);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY)
    {
        guiGraphics.blit(BG_TEXTURE, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);
    }
}
