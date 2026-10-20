package thKaguyaMod.dispense;

import thKaguyaMod.THStackData;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.dispenser.BlockSource;
import net.minecraft.core.Direction;
import net.minecraft.core.dispenser.DefaultDispenseItemBehavior;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.phys.Vec3;
import thKaguyaMod.DanmakuConstants;
import thKaguyaMod.THShotLib;
import thKaguyaMod.entity.item.EntitySilverKnife;
import thKaguyaMod.entity.living.EntityFamiliar;
import thKaguyaMod.init.THKaguyaItems;
import thKaguyaMod.item.ItemSilverKnife;
import thKaguyaMod.item.ItemTHShot;

/**
 * ディスペンサーのカスタム挙動（原版 {@code CommonProxy.registerDispenser()} の移植）。
 *
 * <p>原版 1.7.10 は {@code BlockDispenser.dispenseBehaviorRegistry.putObject(item, IBehaviorDispenseItem)}
 * で 2 種類（銀のナイフ／弾アイテム）を差し替えていた：
 * <ul>
 *   <li>銀のナイフ：発射口から 0.58D で射出（{@code CommonProxy.java:609-625}）</li>
 *   <li>弾アイテム：使い魔を射手役にして {@code ItemTHShot.shootDanmaku} を呼び、
 *       {@code Infinity} タグが無ければ 1 個消費（{@code CommonProxy.java:627-655}）</li>
 * </ul>
 * 1.20.1 では {@link DispenserBlock#registerBehavior(ItemLike, net.minecraft.core.dispenser.DispenseItemBehavior)}
 * に置き換わっているため、ここで {@link DefaultDispenseItemBehavior} を登録する。
 */
public final class THKaguyaDispenserBehaviors
{
    private THKaguyaDispenserBehaviors()
    {
    }

    /** 共通処理の初期化フェーズで 1 回だけ呼ぶ。 */
    public static void register()
    {
        SilverKnifeBehavior knife = new SilverKnifeBehavior();
        DispenserBlock.registerBehavior(THKaguyaItems.SILVER_KNIFE_BLUE.get(), knife);
        DispenserBlock.registerBehavior(THKaguyaItems.SILVER_KNIFE_RED.get(), knife);
        DispenserBlock.registerBehavior(THKaguyaItems.SILVER_KNIFE_GREEN.get(), knife);
        DispenserBlock.registerBehavior(THKaguyaItems.SILVER_KNIFE_WHITE.get(), knife);

        ShotItemBehavior shot = new ShotItemBehavior();
        for (DeferredHolder<net.minecraft.world.item.Item, net.minecraft.world.item.Item> item : THKaguyaItems.shotItems())
        {
            DispenserBlock.registerBehavior(item.get(), shot);
        }
    }

    /** ディスペンサーの発射方向（= 設置向き）を単位ベクトルで返す。 */
    private static Vec3 facing(BlockSource source)
    {
        Direction direction = source.state().getValue(DispenserBlock.FACING);
        return new Vec3(direction.getStepX(), direction.getStepY(), direction.getStepZ());
    }

    /** 銀のナイフ：原版 CommonProxy.java:609-625。 */
    private static final class SilverKnifeBehavior extends DefaultDispenseItemBehavior
    {
        @Override
        protected ItemStack execute(BlockSource source, ItemStack stack)
        {
            ServerLevel level = source.level();
            Vec3 angle = facing(source);
            Vec3 pos = THShotLib.pos(source.pos().getX(), source.pos().getY(), source.pos().getZ());
            int color = stack.getItem() instanceof ItemSilverKnife silverKnife
                    ? silverKnife.getKnifeColor()
                    : 0;

            level.addFreshEntity(new EntitySilverKnife(level, pos, angle, 0.58D, color));

            stack.shrink(1);
            return stack;
        }
    }

    /** 弾アイテム：原版 CommonProxy.java:627-655。 */
    private static final class ShotItemBehavior extends DefaultDispenseItemBehavior
    {
        @Override
        protected ItemStack execute(BlockSource source, ItemStack stack)
        {
            ServerLevel level = source.level();
            Vec3 angle = facing(source);
            Vec3 pos = THShotLib.pos(source.pos().getX(), source.pos().getY(), source.pos().getZ());

            if (!(stack.getItem() instanceof ItemTHShot shotItem))
            {
                return stack;
            }

            // 原版は使い魔を射手役（living）にして発射する。ワールドに追加はしない。
            // 距離は原版 CommonProxy.java:640 と同じ「弾の大きさ + 1.0D」。
            // +1.0D が無いとディスペンサー中心＝ブロック内部に弾が湧き、その場で
            // ブロックに当たって消える（＝発射されていないように見える）。
            EntityFamiliar shooter = new EntityFamiliar(level);
            shotItem.shootDanmaku(stack, shooter, false, pos, angle,
                    DanmakuConstants.SIZE[shotItem.getShotForm()] + 1.0D);

            boolean isInfinity = false;
            if (THStackData.get(stack) != null)
            {
                isInfinity = THStackData.get(stack).getBoolean("Infinity");
            }
            if (!isInfinity)
            {
                stack.shrink(1);
            }

            return stack;
        }
    }
}
