package thKaguyaMod.client.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import thKaguyaMod.init.THKaguyaConfig;

/**
 * 実験的機能を有効にするときの警告画面（神隠し機能）。
 *
 * <p>「これは既に神隠しに遭った（原作者がコメントアウトした）機能です。本当に有効にしますか？」
 * という確認 ＋「今後表示しない」トグル。OK で万宝槌の改造台を有効化する。</p>
 */
public class ExperimentalWarningScreen extends Screen
{
    private final Screen parent;
    private boolean dontAskAgain;

    public ExperimentalWarningScreen(Screen parent)
    {
        super(Component.translatable("config.thkaguya.mallet_gui.warning.title"));
        this.parent = parent;
    }

    @Override
    protected void init()
    {
        // 画面に入り直すたびに「今後表示しない」は未チェックから始める
        this.dontAskAgain = false;

        int centerX = this.width / 2;
        int buttonY = this.height / 2 + 40;

        this.addRenderableWidget(Button
                .builder(CommonComponents.GUI_OK, button -> this.confirm())
                .bounds(centerX - 165, buttonY, 100, 20)
                .build());

        this.addRenderableWidget(Button
                .builder(CommonComponents.GUI_CANCEL, button -> this.minecraft.setScreen(this.parent))
                .bounds(centerX - 50, buttonY, 100, 20)
                .build());

        this.addRenderableWidget(Button
                .builder(dontAskLabel(), button -> {
                    this.dontAskAgain = !this.dontAskAgain;
                    button.setMessage(dontAskLabel());
                })
                .bounds(centerX + 65, buttonY, 100, 20)
                .build());
    }

    private Component dontAskLabel()
    {
        return Component.translatable("config.thkaguya.mallet_gui.warning.dont_ask",
                Component.translatable(this.dontAskAgain ? "options.on" : "options.off"));
    }

    private void confirm()
    {
        THKaguyaConfig.setEnableMalletGui(true);
        if (this.dontAskAgain)
        {
            THKaguyaConfig.setSuppressMalletGuiWarning(true);
        }
        this.minecraft.setScreen(this.parent);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick)
    {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);

        int centerX = this.width / 2;
        guiGraphics.drawCenteredString(this.font, this.title, centerX, this.height / 2 - 60, 0xFF5555);
        guiGraphics.drawCenteredString(this.font,
                Component.translatable("config.thkaguya.mallet_gui.warning.body"),
                centerX, this.height / 2 - 30, 0xFFFFFF);
        guiGraphics.drawCenteredString(this.font,
                Component.translatable("config.thkaguya.mallet_gui.warning.detail"),
                centerX, this.height / 2 - 10, 0xA0A0A0);
    }

    @Override
    public void onClose()
    {
        // Esc キー＝キャンセル扱い（有効化しない）
        this.minecraft.setScreen(this.parent);
    }
}
