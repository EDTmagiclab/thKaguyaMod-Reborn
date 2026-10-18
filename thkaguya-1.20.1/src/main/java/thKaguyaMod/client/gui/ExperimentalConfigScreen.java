package thKaguyaMod.client.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.fml.ModLoadingContext;
import thKaguyaMod.init.THKaguyaConfig;

/**
 * 実験的機能の設定画面（Mod 一覧の Config ボタンから開く）。
 *
 * <p>Forge 1.20.1 の {@code ModListScreen} は、mod が
 * {@code ConfigScreenHandler.ConfigScreenFactory} 拡張点を登録しているときだけ Config ボタンを有効にする。
 * そのため {@link #register()} を mod 構築時（クライアント側のみ）に呼んでいる。</p>
 *
 * <p>現在の項目は「打ち出の小槌（万宝槌）の改造台」だけ。
 * 有効化するときは {@link ExperimentalWarningScreen}（神隠し機能の警告）を挟む。</p>
 */
public class ExperimentalConfigScreen extends Screen
{
    private final Screen parent;
    private Button malletButton;
    private Button warningButton;

    /** クライアント側でのみ呼ぶこと（mod 構築フェーズ） */
    public static void register()
    {
        ModLoadingContext.get().registerExtensionPoint(ConfigScreenHandler.ConfigScreenFactory.class,
                () -> new ConfigScreenHandler.ConfigScreenFactory(
                        (minecraft, parent) -> new ExperimentalConfigScreen(parent)));
    }

    public ExperimentalConfigScreen(Screen parent)
    {
        super(Component.translatable("config.thkaguya.experimental.title"));
        this.parent = parent;
    }

    @Override
    protected void init()
    {
        int centerX = this.width / 2;
        int top = this.height / 2 - 40;

        this.malletButton = this.addRenderableWidget(Button
                .builder(malletLabel(), button -> this.toggleMalletGui())
                .bounds(centerX - 110, top, 220, 20)
                .build());

        this.warningButton = this.addRenderableWidget(Button
                .builder(warningLabel(), button -> {
                    THKaguyaConfig.setSuppressMalletGuiWarning(!THKaguyaConfig.suppressMalletGuiWarning);
                    button.setMessage(warningLabel());
                })
                .bounds(centerX - 110, top + 26, 220, 20)
                .build());

        this.addRenderableWidget(Button
                .builder(CommonComponents.GUI_DONE, button -> this.onClose())
                .bounds(centerX - 100, this.height - 30, 200, 20)
                .build());
    }

    private Component malletLabel()
    {
        return Component.translatable("config.thkaguya.mallet_gui",
                Component.translatable(THKaguyaConfig.enableMalletGui ? "options.on" : "options.off"));
    }

    private Component warningLabel()
    {
        return Component.translatable("config.thkaguya.suppress_warning",
                Component.translatable(THKaguyaConfig.suppressMalletGuiWarning ? "options.on" : "options.off"));
    }

    private void toggleMalletGui()
    {
        // 有効 → 無効：そのまま切り替え
        if (THKaguyaConfig.enableMalletGui)
        {
            THKaguyaConfig.setEnableMalletGui(false);
            this.malletButton.setMessage(malletLabel());
            return;
        }

        // 無効 → 有効：「今後表示しない」が選ばれていれば警告なしで有効化
        if (THKaguyaConfig.suppressMalletGuiWarning)
        {
            THKaguyaConfig.setEnableMalletGui(true);
            this.malletButton.setMessage(malletLabel());
            return;
        }

        // 神隠し機能の警告（OK で有効化、キャンセルで戻る）
        this.minecraft.setScreen(new ExperimentalWarningScreen(this));
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick)
    {
        this.renderBackground(guiGraphics);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, 20, 0xFFFFFF);
        guiGraphics.drawCenteredString(this.font,
                Component.translatable("config.thkaguya.experimental.hint"),
                this.width / 2, this.height / 2 + 30, 0xA0A0A0);
    }

    @Override
    public void onClose()
    {
        this.minecraft.setScreen(this.parent);
    }
}
