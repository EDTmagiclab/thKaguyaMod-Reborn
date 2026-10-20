package thKaguyaMod.client.input;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * クライアント専用キー入力取得（1.20.1 移植版）
 *
 * 原版 1.7.10 の EntityMarisaBroom.onUpdate は、本体クラス内で直接
 * <pre>
 *   Minecraft mc = Minecraft.getMinecraft();
 *   move_up = mc.gameSettings.keyBindForward.getIsKeyPressed();
 * </pre>
 * を呼んでいた（＝非 client パッケージからのクライアント漏洩）。
 * 1.20.1 では本クラスを client パッケージに隔離し、
 * EntityMarisaBroom からは
 * {@code DistExecutor.unsafeCallWhenOn(Dist.CLIENT, () -> ClientBroomInput::isPushKeyUp)}
 * 経由でのみ呼び出す。DistExecutor は Dist 不一致時に供給元を呼ばないため、
 * 専用サーバでは本クラスが一切ロードされない（NoClassDefFoundError を回避）。
 *
 * 1.7.10 -> 1.20.1 対応表:
 *   gameSettings.keyBindForward -> options.keyUp
 *   gameSettings.keyBindBack    -> options.keyDown
 *   gameSettings.keyBindLeft    -> options.keyLeft
 *   gameSettings.keyBindRight   -> options.keyRight
 *   gameSettings.keyBindJump    -> options.keyJump
 *   getIsKeyPressed()           -> isDown()
 */
@OnlyIn(Dist.CLIENT)
public final class ClientBroomInput
{
    private ClientBroomInput()
    {
    }

    /** 前進（原版 keyBindForward） */
    public static boolean isPushKeyUp()
    {
        return isDown(Minecraft.getInstance().options.keyUp);
    }

    /** 後退（原版 keyBindBack） */
    public static boolean isPushKeyBack()
    {
        return isDown(Minecraft.getInstance().options.keyDown);
    }

    /** 右（原版 keyBindRight） */
    public static boolean isPushKeyRight()
    {
        return isDown(Minecraft.getInstance().options.keyRight);
    }

    /** 左（原版 keyBindLeft） */
    public static boolean isPushKeyLeft()
    {
        return isDown(Minecraft.getInstance().options.keyLeft);
    }

    /** ジャンプ（原版 keyBindJump） */
    public static boolean isPushKeyJump()
    {
        return isDown(Minecraft.getInstance().options.keyJump);
    }

    private static boolean isDown(KeyMapping mapping)
    {
        return mapping != null && mapping.isDown();
    }
}
