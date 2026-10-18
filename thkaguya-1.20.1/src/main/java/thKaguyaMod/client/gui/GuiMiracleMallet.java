package thKaguyaMod.client.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import thKaguyaMod.THKaguyaCore;
import thKaguyaMod.gui.ContainerMiracleMallet;

/**
 * 万宝槌「妖器化」界面（1.20.1 移植版）
 *
 * <p>原版 {@code GuiMiracleMallet} 没有自己的背景图，直接借用
 * {@code textures/gui/danmaku_crafting_table.png}（弹幕工作台那张画满槽位的图），
 * 所以看起来和弹幕工作台一模一样，实际上只有 2 个槽位可用。</p>
 *
 * <p>本移植版按用户要求把它改成「妖器化」专用外观：上半部分用背景色盖掉原图上
 * 那些并不存在的槽位，只画出真正可用的「素材 → 結果」两个槽，标题写作「妖器化」。
 * 下半部分的玩家物品栏仍沿用原图。</p>
 */
public class GuiMiracleMallet extends AbstractContainerScreen<ContainerMiracleMallet>
{
    private static final ResourceLocation BG_TEXTURE =
            new ResourceLocation(THKaguyaCore.MODID, "textures/gui/danmaku_crafting_table.png");

    /** 素材槽左上角（必须与 ContainerMiracleMallet 里加的槽位一致） */
    public static final int MATERIAL_X = 99;
    public static final int MATERIAL_Y = 35;
    /** 结果槽左上角 */
    public static final int RESULT_X = 139;
    public static final int RESULT_Y = 35;

    /** 覆盖带的高度（玩家物品栏的绘制区从 y=84 开始，故止于 80） */
    private static final int PANEL_HEIGHT = 80;

    /** 原图背景色 / 槽位边框 / 槽位内部（均取样自 danmaku_crafting_table.png） */
    private static final int COLOR_BACKGROUND = 0xFFC0C0C0;
    private static final int COLOR_SLOT_BORDER = 0xFF303030;
    private static final int COLOR_SLOT_FILL = 0xFF909090;
    private static final int COLOR_FRAME_BLACK = 0xB3000000;
    private static final int COLOR_FRAME_WHITE = 0xFFFEFEFE;
    private static final int COLOR_ARROW = 0xFF404040;

    public GuiMiracleMallet(ContainerMiracleMallet menu, Inventory playerInventory, Component title)
    {
        super(menu, playerInventory, title);
        this.imageWidth = 256;
        this.imageHeight = 166;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY)
    {
        guiGraphics.blit(BG_TEXTURE, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);

        int x0 = this.leftPos;
        int y0 = this.topPos;

        // 上半分を塗りつぶし、原図に描かれている「使えない槽」を消す
        guiGraphics.fill(x0, y0, x0 + this.imageWidth, y0 + PANEL_HEIGHT, COLOR_BACKGROUND);

        // 外枠を描き直す（原テクスチャは 1px 黒 + 2px 白の縁）
        guiGraphics.fill(x0, y0, x0 + this.imageWidth, y0 + 1, COLOR_FRAME_BLACK);
        guiGraphics.fill(x0 + 1, y0 + 1, x0 + this.imageWidth - 1, y0 + 3, COLOR_FRAME_WHITE);
        guiGraphics.fill(x0, y0, x0 + 1, y0 + PANEL_HEIGHT, COLOR_FRAME_BLACK);
        guiGraphics.fill(x0 + 1, y0 + 1, x0 + 3, y0 + PANEL_HEIGHT, COLOR_FRAME_WHITE);
        guiGraphics.fill(x0 + this.imageWidth - 1, y0, x0 + this.imageWidth, y0 + PANEL_HEIGHT, COLOR_FRAME_BLACK);
        guiGraphics.fill(x0 + this.imageWidth - 3, y0 + 1, x0 + this.imageWidth - 1, y0 + PANEL_HEIGHT, COLOR_FRAME_WHITE);
        // 下のテクスチャとの区切り
        guiGraphics.fill(x0 + 1, y0 + PANEL_HEIGHT - 1, x0 + this.imageWidth - 1, y0 + PANEL_HEIGHT, COLOR_FRAME_BLACK);

        drawSlot(guiGraphics, x0 + MATERIAL_X, y0 + MATERIAL_Y);
        drawSlot(guiGraphics, x0 + RESULT_X, y0 + RESULT_Y);
        drawArrow(guiGraphics, x0 + MATERIAL_X + 20, y0 + MATERIAL_Y + 9, x0 + RESULT_X - 4);
    }

    /** 原テクスチャと同じ「1px 濃枠 + 灰色べた塗り」の槽を描く */
    private void drawSlot(GuiGraphics guiGraphics, int x, int y)
    {
        guiGraphics.fill(x - 1, y - 1, x + 18, y + 18, COLOR_SLOT_BORDER);
        guiGraphics.fill(x, y, x + 17, y + 17, COLOR_SLOT_FILL);
    }

    /** 素材 → 結果 の矢印（テクスチャに頼らず図形で描く） */
    private void drawArrow(GuiGraphics guiGraphics, int fromX, int centerY, int tipX)
    {
        guiGraphics.fill(fromX, centerY - 1, tipX - 4, centerY + 1, COLOR_ARROW);
        for (int i = 0; i < 4; i++)
        {
            guiGraphics.fill(tipX - 4 + i, centerY - i - 1, tipX - 3 + i, centerY + i + 1, COLOR_ARROW);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY)
    {
        guiGraphics.drawString(this.font, Component.translatable("gui.thkaguya.miracle_mallet.title"), 8, 8, 4210752, false);
        guiGraphics.drawString(this.font, Component.translatable("miracleMallet.material"), MATERIAL_X, MATERIAL_Y - 11, 4210752, false);
        guiGraphics.drawString(this.font, Component.translatable("miracleMallet.result"), RESULT_X, RESULT_Y - 11, 4210752, false);
        guiGraphics.drawString(this.font, Component.translatable("container.inventory"), 48, this.imageHeight - 96 + 2, 4210752, false);
    }
}
