package thKaguyaMod.item;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import thKaguyaMod.DanmakuConstants;
import thKaguyaMod.LaserData;
import thKaguyaMod.THKaguyaLib;
import thKaguyaMod.THShotLib;

/**
 * 神槍「スピア・ザ・グングニル」
 *
 * 原版 ItemSpearTheGungnir：右键 = 先宣言符卡「神槍スピア・ザ・グングニル」（No 27），
 * 宣言成立后沿玩家视线方向发射一支 3D 枪形激光（程序化顶点表渲染，
 * 由 RenderTHLaser.renderSpearTheGungnir 完成）。
 * color = DanmakuConstants.RED → form = DanmakuConstants.LASER_GUNGNIR (1)，
 * shotId = form*8 + color = 8，落入 RenderTHLaser 的 Gungnir 分支（8-15）。
 *
 * 1.20.1 迁移要点：
 * - 原版 World.playSoundAtEntity → level().playSound(null, pos, SoundEvent, SoundSource, ...)
 * - 原版 itemStack.damageItem(1, player) → itemStack.hurtAndBreak(1, player, ...)
 * - 原版 player.getLookVec() → player.getLookAngle()
 * - 原版 setMaxDamage(95) → Item.Properties.durability(95)
 * - 原版 maxStackSize = 1 → Item.Properties.stacksTo(1)
 * - 原版 setNoRepair()：1.20.1 无对应 API，未做修理不可标记（影响轻微）
 */
public class ItemSpearTheGungnir extends Item
{
    public ItemSpearTheGungnir()
    {
        super(new Item.Properties()
                .stacksTo(1)
                .durability(95));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand)
    {
        ItemStack itemStack = player.getItemInHand(hand);

        // 原版：先宣言符卡「神槍スピア・ザ・グングニル」（符卡 No 27，难度 Normal），
        // 宣言失败则不发射。checkSpellCardDeclaration 只在服务端成立（客户端恒 false），
        // 因此这里不额外判断 isClientSide —— 与原版结构一致。
        boolean spellCard = THKaguyaLib.checkSpellCardDeclaration(level, itemStack, player, 27, 2, true);

        if(spellCard)
        {
            Vec3 look = player.getLookAngle();
            // 原版 "Gungnir 激光" 偏移 0.6（zAdjustment），起始位置在玩家眼部略上
            THShotLib.createLaserA(player, player,
                    THShotLib.pos(player.getX(),
                            THShotLib.getPosYFromEye(player, 0.6D),
                            player.getZ()),
                    THShotLib.angle(-look.x, look.y, -look.z),
                    0.0D, 2.0D, 0.0D, THShotLib.gravity_Zero(),
                    LaserData.laser(DanmakuConstants.LASER_GUNGNIR, DanmakuConstants.RED,
                            1.0F, 10.0F, 14.0F, 0, 120, DanmakuConstants.GUNGNIR));

            // 原版 "mob.zombie.remedy" / "mob.wither.spawn"
            level.playSound(null, player.blockPosition(),
                    SoundEvents.ZOMBIE_VILLAGER_CURE, SoundSource.PLAYERS, 0.5F, 2.7F);
            level.playSound(null, player.blockPosition(),
                    SoundEvents.WITHER_SPAWN, SoundSource.PLAYERS, 0.2F, 10.0F);

            if(!player.getAbilities().instabuild)
            {
                itemStack.hurtAndBreak(1, player, (p) -> p.broadcastBreakEvent(hand));
            }
        }

        return InteractionResultHolder.success(itemStack);
    }

    @Override
    public boolean isEnchantable(ItemStack stack)
    {
        return false;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack)
    {
        return UseAnim.BLOCK;
    }
}
