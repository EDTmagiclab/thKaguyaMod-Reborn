package thKaguyaMod.item;

import java.util.List;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.WaterAnimal;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.level.Level;
import thKaguyaMod.entity.EntityDivineSpirit;
import thKaguyaMod.entity.living.EntityTHFairy;

/**
 * 神子の持つ宝剣　１０人の言葉を同時に聞き取れる（1.20.1 移植版）
 *
 * 原版: extract/sources/java/thKaguyaMod/item/ItemMikoSword.java（1.7.10、265 行）
 *   原版登録は init/THKaguyaItems.java:550 の new ItemMikoSword(ToolMaterial.IRON) ＝ 基本鉄の剣と同じ。
 *   1.20.1 では Tiers.IRON を使い、耐久だけ原版素材（1.7.10 の鉄 = 251）に合わせて明示する。
 *
 * 挙動:
 *   - 右クリックで使用開始（原版 setItemInUse、継続時間 72000）。
 *     既に耐久が尽きている（damage >= maxDamage）なら原版どおり消滅（stackSize--）。
 *   - 離した時、getMaxItemUseDuration - usedTime（＝右クリックしていた tick 数、最大 50）を半径として
 *     周囲の LivingEntity から近い順に 10 体を選ぶ（原版は 11 番目をゴミ箱に使う挿入ソート）。
 *   - 選んだ相手ごとに小神霊（EntityDivineSpirit）を出し、相手の種類から色番号を決める:
 *       妖精 = 乱数（85% 1 / 10% 2 / 5% 4）、クリーパー = 2、村人 = 1、動物 = 3（手懐け済み = 4）、
 *       水棲 = 5、その他の LivingEntity = 不死 7 / 虫 6 / それ以外 0
 *   - スニーク中でなければ探知メッセージを出す（原版は StatCollector、1.20.1 は Component.translatable で
 *     クライアント側翻訳。色番号は Mob = §4、Animal = §a、それ以外 = §3）。
 *   - 最後に非クリエイティブなら探知した数だけ耐久を減らす。
 *
 * 1.20.1 側の対応:
 *   - 原版 onPlayerStoppedUsing の usedTime は「残り tick」（ItemBow と同じ）なので、
 *     recognizeDistance = 72000 - usedTime = 経過 tick。1.20.1 の releaseUsing の timeLeft も残り tick なので
 *     そのまま getUseDuration(itemStack) - timeLeft が経過 tick になる。
 *   - EntityMob → Monster、EntityWaterMob → WaterAnimal。
 *     1.20.1 の WaterAnimal は PathfinderMob 直系（Animal ではない）ため、原版の判定順のままで良い。
 *   - getCommandSenderName() → getDisplayName()（原版でコメントアウトされていた EntityList.getEntityString は使わない）。
 *   - 原版 angle2 判定のバグ（if(angle2 < -180F) angle += 360F; と angle を書き換えている）は原版どおり残す。
 *   - 耐久減少は hurtAndBreak ではなく setItemDamage 相当（setDamageValue）。原版も同様。
 */
public class ItemMikoSword extends SwordItem
{
    public ItemMikoSword()
    {
        // 原版 ToolMaterial.IRON（1.7.10 の鉄: 耐久 251 / 採掘レベル 0 / 効率 6.0F / 攻撃 +2）
        // 1.20.1 の Tiers.IRON は耐久 250 なので原版に合わせて 251 を明示
        super(Tiers.IRON, (new Item.Properties().stacksTo(1).durability(251)).attributes(SwordItem.createAttributes(Tiers.IRON, 3, -2.4F)));
    }

    /** 原版 getMaxItemUseDuration の既定値（ItemBow と同じ 72000） */
    @Override
    public int getUseDuration(ItemStack itemStack, net.minecraft.world.entity.LivingEntity entity)
    {
        return 72000;
    }

    /** 右クリックを押した瞬間の処理（原版 onItemRightClick） */
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand)
    {
        ItemStack itemStack = player.getItemInHand(hand);

        player.startUsingItem(hand);

        // 原版: 耐久が尽きている状態で使うと消える（itemStack.stackSize--）
        if(itemStack.getDamageValue() >= itemStack.getMaxDamage() && !level.isClientSide)
        {
            itemStack.shrink(1);
        }

        return InteractionResultHolder.consume(itemStack);
    }

    /** 右クリックを押し終わったときに呼び出される（原版 onPlayerStoppedUsing） */
    @Override
    public void releaseUsing(ItemStack itemStack, Level level, LivingEntity entity, int timeLeft)
    {
        if(!(entity instanceof Player))
        {
            return;
        }

        Player player = (Player)entity;

        Entity[] nearEntitys = new Entity[11];//11番目の空間はゴミ箱
        double[] nearDistance = new double[11];

        // 原版: (double)(72000 - usedTime)。usedTime は残り tick なので経過 tick が入る
        double recognizeDistance = (double)(getUseDuration(itemStack, player) - timeLeft);
        if(recognizeDistance > 50D)
        {
            recognizeDistance = 50D;
        }

        int i;
        for(i = 0; i < 11; i++)
        {
            nearEntitys[i] = null;
            nearDistance[i] = 999.9D;
        }

        List<Entity> list = level.getEntities(player,
                player.getBoundingBox().inflate(recognizeDistance, recognizeDistance, recognizeDistance));

        for(int j = 0; j < list.size(); j++)
        {
            Entity entity1 = list.get(j);
            double distance = player.distanceTo(entity1);

            if(entity1 instanceof LivingEntity)
            {
                int n = 0;
                boolean flag = false;

                while(n < 10 && !flag)
                {
                    if(nearDistance[n] > distance)
                    {
                        for(int k = 10; k > n; k--)
                        {
                            nearEntitys[k] = nearEntitys[k - 1];
                            nearDistance[k] = nearDistance[k - 1];
                        }

                        nearEntitys[n] = entity1;
                        nearDistance[n] = distance;
                        flag = true;
                    }

                    n++;
                }
            }
        }

        float angle = (360F - player.getYRot()) % 360F;
        float angle2;

        if(angle < -180F) angle += 360F;
        if(angle >  180F) angle -= 360F;

        i = 0;

        while(i < 10 && nearEntitys[i] != null)
        {
            Entity target = nearEntitys[i];

            double eyePosX = target.getX();
            double eyePosY = target.getY() + target.getEyeHeight();
            double eyePosZ = target.getZ();

            int color = 0;

            if(target instanceof EntityTHFairy)
            {
                int randomnum = player.getRandom().nextInt(100);

                if(randomnum < 85)
                {
                    color = 1;
                }
                else if(randomnum < 95)
                {
                    color = 2;
                }
                else
                {
                    color = 4;
                }
            }
            else if(target instanceof Creeper)
            {
                color = 2;
            }
            else if(target instanceof Villager)
            {
                color = 1;
            }
            else if(target instanceof Animal)
            {
                color = 3;

                if(target instanceof TamableAnimal)
                {
                    if(((TamableAnimal)target).isTame())
                    {
                        color = 4;
                    }
                }
            }
            else if(target instanceof WaterAnimal)
            {
                color = 5;
            }
            else if(target instanceof LivingEntity)
            {
                net.minecraft.world.entity.EntityType<?> targetType = ((LivingEntity)target).getType();

                if(targetType.is(net.minecraft.tags.EntityTypeTags.UNDEAD))
                {
                    color = 7;
                }
                else if(targetType.is(net.minecraft.tags.EntityTypeTags.ARTHROPOD))
                {
                    color = 6;
                }
                else
                {
                    color = 0;
                }
            }

            EntityDivineSpirit entityDivineSpirit = new EntityDivineSpirit(level, eyePosX, eyePosY, eyePosZ, color);

            if(!level.isClientSide)
            {
                level.addFreshEntity(entityDivineSpirit);
            }

            // スニークなら探知メッセージを表示しない
            if(!player.isShiftKeyDown())
            {
                double dx = target.getX() - player.getX();
                double dz = target.getZ() - player.getZ();

                angle2 = ((float)Math.atan2(dx, dz) / 3.141593F * 180F) % 360F;

                // 原版のまま（本来 angle2 を補正すべきところを angle に足している）
                if(angle2 < -180F) angle += 360F;
                if(angle2 >  180F) angle -= 360F;

                angle2 -= angle;

                String strPos;
                if(angle2 < -135F || angle2 > 135F) strPos = "thKaguya.back";
                else if(angle2 < -45F) strPos = "thKaguya.right";
                else if(angle2 <  45F) strPos = "thKaguya.front";
                else strPos = "thKaguya.left";

                String strUpDown;
                if(player.getY() > target.getY() + 3.0D) strUpDown = "thKaguya.down";
                else if(player.getY() < target.getY() - 3.0D) strUpDown = "thKaguya.up";
                else strUpDown = null;

                String strColor;
                if(target instanceof Monster) strColor = "\u00a74";
                else if(target instanceof Animal) strColor = "\u00a7a";
                else strColor = "\u00a73";

                int num = i + 1;
                String strNum = num + ". ";
                if(num != 10)
                {
                    strNum = " " + strNum;
                }

                Component name = target.getDisplayName();

                MutableComponent message = Component.literal(strColor + strNum).append(name).append(" ")
                        .append(Component.translatable(strPos)).append(" ")
                        .append(strUpDown == null ? Component.empty() : Component.translatable(strUpDown))
                        .append("  " + (int)nearDistance[i] + "m" + "  ");

                if(!level.isClientSide)
                {
                    player.sendSystemMessage(message);
                }
            }

            i++;
        }

        // 探知した数だけ耐久を減らす
        if(!player.getAbilities().instabuild)
        {
            itemStack.setDamageValue(itemStack.getDamageValue() + i);
        }
    }

    /** エンチャント可能か？（原版 getItemEnchantability = 0） */
    @Override
    public int getEnchantmentValue()
    {
        return 0;
    }

    @Override
    public boolean isEnchantable(ItemStack itemStack)
    {
        return false;
    }

    /** Forge の追加メソッド　エンチャントブックの使用を許可するか */
    @Override
    public boolean isBookEnchantable(ItemStack itemStack1, ItemStack itemStack2)
    {
        return false;
    }
}
