package thKaguyaMod.item;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import thKaguyaMod.DanmakuConstants;
import thKaguyaMod.LaserData;
import thKaguyaMod.THShotLib;
import thKaguyaMod.entity.shot.EntityTHSetLaser;

import java.util.List;

/**
 * レーヴァテイン（莱瓦汀）
 *
 * 原版 ItemLaevateinn extends ItemSword：
 * - 近战命中附带火焰（itemRand.nextInt(3)+3 秒）
 * - 右键 = 横向挥砍（普通）或纵向挥砍（Shift + 右键）
 *   → 召唤一柄 3D 剑形火焰激光（color=RED+16，form=0，shotId=16，
 *      由 RenderTHLaser.renderLaevateinn 顶点表渲染）
 *
 * 1.20.1 迁移要点：
 * - 原版 EnumHelper.addToolMaterial 自定义 ToolMaterial → Tiers.IRON（攻击伤害 3 与原版一致）
 *   自定义 Tier 在 1.20.1 需实现 Tier 接口较繁，本期先用 Tiers.IRON
 * - 原版 hitEntity → hurtEnemy；isCreativeMode → getAbilities().instabuild
 * - 原版 itemStack.damageItem(1, player) → itemStack.hurtAndBreak(1, player, ...)
 * - 原版 world.getEntitiesWithinAABBExcludingEntity → level().getEntitiesOfClass(...)
 * - 原版 world.playSoundAtEntity(player, "mob.ghast.fireball", ...) → level().playSound(...)
 * - 原版 entity.source（public 字段）→ entity.getOwner()
 */
public class ItemLaevateinn extends SwordItem
{
    public ItemLaevateinn()
    {
        // 原版：harvestLevel=0, maxUses=95, efficiency=9.0F, damage=3.0F, enchantability=0
        // 1.20.1 Tiers.IRON：attackDamage=3.0（铁剑基础值）
        super(Tiers.IRON, 3, -2.4F,
                new Item.Properties()
                        .stacksTo(1)
                        .durability(95)
                        .fireResistant());
    }

    public ItemLaevateinn(Tier tier)
    {
        super(tier, 3, -2.4F,
                new Item.Properties()
                        .stacksTo(1)
                        .durability(95)
                        .fireResistant());
    }

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker)
    {
        boolean result = super.hurtEnemy(stack, target, attacker);
        // 命中实体点火 3-5 秒（原版 itemRand.nextInt(3)+3）
        target.setSecondsOnFire(3 + attacker.getRandom().nextInt(3));
        return result;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand)
    {
        ItemStack itemStack = player.getItemInHand(hand);

        // 若已存在同玩家的 SetLaser，则本 Item 不发射激光（原版行为）
        if(!level.isClientSide)
        {
            List<EntityTHSetLaser> existing = level.getEntitiesOfClass(EntityTHSetLaser.class,
                    player.getBoundingBox().inflate(20.0D));
            for(EntityTHSetLaser laser : existing)
            {
                if(laser.user == player)
                {
                    return InteractionResultHolder.fail(itemStack);
                }
            }
        }

        if(level.isClientSide)
        {
            return InteractionResultHolder.success(itemStack);
        }

        Vec3 move;
        Vec3 rotate;

        if(player.isShiftKeyDown())
        {
            // 纵向斩：上下方向 90° 偏移
            rotate = THShotLib.getVecFromAngle(player.getYRot() + 90F, 0F);
            move = THShotLib.getVecFromAngle(player.getYRot(), player.getXRot() - 90F);
            EntityTHSetLaser laser = THShotLib.createLaserB(player, player,
                    THShotLib.pos(0.0D, THShotLib.getPosYFromEye(player) - player.getY(), 0.0D),
                    move, rotate, -6F, 9999,
                    LaserData.laser(DanmakuConstants.RED + 16, 0.6F, 20.8F, 7.0F, 0, 30, DanmakuConstants.FIRE),
                    player, 1.5D, 1.0F);
            laser.setAngleZ(90F);
        }
        else
        {
            // 横向挥砍：与视线垂直
            Vec3 look = player.getLookAngle();
            rotate = THShotLib.getVecFromAngle(player.getYRot(), player.getXRot() - 90F);
            move = THShotLib.getVectorFromRotation(rotate.x, rotate.y, rotate.z,
                    look.x, look.y, look.z, 90F);
            THShotLib.createLaserB(player, player,
                    THShotLib.pos(0.0D, THShotLib.getPosYFromEye(player) - player.getY(), 0.0D),
                    move, rotate, -6F, 9999,
                    LaserData.laser(DanmakuConstants.RED + 16, 0.6F, 20.8F, 7.0F, 0, 30, DanmakuConstants.FIRE),
                    player, 1.5D, 1.0F);
        }

        level.playSound(null, player.blockPosition(),
                SoundEvents.GHAST_SHOOT, SoundSource.PLAYERS, 0.5F, 1.0F);

        if(!player.getAbilities().instabuild)
        {
            itemStack.hurtAndBreak(1, player, (p) -> p.broadcastBreakEvent(hand));
        }
        return InteractionResultHolder.success(itemStack);
    }

    @Override
    public int getEnchantmentValue()
    {
        return 0;
    }

    @Override
    public boolean isEnchantable(ItemStack stack)
    {
        return false;
    }
}
