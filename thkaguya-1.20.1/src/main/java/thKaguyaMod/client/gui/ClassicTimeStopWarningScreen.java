package thKaguyaMod.client.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import thKaguyaMod.init.THKaguyaConfig;

/**
 * クラシック時停を有効にするときの警告画面。
 *
 * <p>クラシック時停は「40 格の箱の中、施術者以外すべて」を止めるので、
 * 公共サーバでは他人の農場・村人取引・ドロップ品まで巻き込む。
 * 設定ファイルのコメントだけでは気付かれないので、有効化の直前に必ずここを通す。</p>
 */
public class ClassicTimeStopWarningScreen extends Screen
{
    private final Screen parent;

    public ClassicTimeStopWarningScreen(Screen parent)
    {
        super(Component.translatable("config.thkaguya.time_stop_classic.warning.title"));
        this.parent = parent;
    }

    @Override
    protected void init()
    {
        int centerX = this.width / 2;
        int buttonY = this.height / 2 + 46;

        this.addRenderableWidget(Button
                .builder(CommonComponents.GUI_OK, button -> {
                    THKaguyaConfig.setUseClassicTimeStop(true);
                    this.minecraft.setScreen(this.parent);
                })
                .bounds(centerX - 105, buttonY, 100, 20)
                .build());

        this.addRenderableWidget(Button
                .builder(CommonComponents.GUI_CANCEL, button -> this.minecraft.setScreen(this.parent))
                .bounds(centerX + 5, buttonY, 100, 20)
                .build());
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick)
    {
        this.renderBackground(guiGraphics);
        super.render(guiGraphics, mouseX, mouseY, partialTick);

        guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, this.height / 2 - 60, 0xFF5555);

        String[] keys = {
                "config.thkaguya.time_stop_classic.warning.line1",
                "config.thkaguya.time_stop_classic.warning.line2",
                "config.thkaguya.time_stop_classic.warning.line3",
                "config.thkaguya.time_stop_classic.warning.line4"
        };
        int y = this.height / 2 - 34;
        for(String key : keys)
        {
            guiGraphics.drawCenteredString(this.font, Component.translatable(key), this.width / 2, y, 0xE0E0E0);
            y += 13;
        }
    }

    @Override
    public void onClose()
    {
        this.minecraft.setScreen(this.parent);
    }
}
