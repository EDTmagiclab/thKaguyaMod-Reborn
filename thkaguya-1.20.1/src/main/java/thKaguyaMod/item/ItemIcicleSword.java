package thKaguyaMod.item;

import java.util.List;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.level.Level;
import thKaguyaMod.entity.effect.EntityCirnoIceBox;

/**
 * アイシクルソード（冰剑）
 *
 * 原版 ItemIcicleSword extends ItemSword：攻击命中时生成 EntityCirnoIceBox 将目标冰封
 * （冻结期间目标被回退到上一帧位置并速度清零）。
 *
 * 1.20.1 迁移要点：
 * - ItemSword → SwordItem；ToolMaterial → Tier（用 Tiers.IRON，与 ItemLaevateinn 一致）
 * - hitEntity → hurtEnemy(ItemStack, LivingEntity target, LivingEntity attacker)
 * - worldObj → level()；isRemote → isClientSide；spawnEntityInWorld → addFreshEntity
 * - boundingBox → getBoundingBox()；width/height → getBbWidth()/getBbHeight()
 * - getItemEnchantability → getEnchantmentValue；isBookEnchantable → isEnchantable
 */
public class ItemIcicleSword extends SwordItem
{
    /** 原版 ToolMaterial "icicle" の maxUses = 30 */
    public static final int MAX_DAMAGE = 30;

    public ItemIcicleSword()
    {
        super(Tiers.IRON, 3, -2.4F,
                new Item.Properties()
                        .stacksTo(1)
                        .durability(MAX_DAMAGE));
    }

    public ItemIcicleSword(Tier tier)
    {
        super(tier, 3, -2.4F,
                new Item.Properties()
                        .stacksTo(1)
                        .durability(MAX_DAMAGE));
    }

    @Override
    public int getMaxDamage(ItemStack stack)
    {
        return MAX_DAMAGE;
    }

    /** 攻击命中：生成冰柜冻结目标（原版 hitEntity） */
    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker)
    {
        Level level = target.level();

        // 若目标已被冰封，则不再重复冻结（原版：查周围 EntityCirnoIceBox 的 frozen 是否为目标）
        List<EntityCirnoIceBox> boxes = level.getEntitiesOfClass(EntityCirnoIceBox.class, target.getBoundingBox());
        for(EntityCirnoIceBox box : boxes)
        {
            if(box.frozen == target)
            {
                return super.hurtEnemy(stack, target, attacker);
            }
        }

        // 大型实体不冻结（原版 width>3 或 height>3）
        if(target.getBbWidth() > 3.0F || target.getBbHeight() > 3.0F)
        {
            return super.hurtEnemy(stack, target, attacker);
        }

        EntityCirnoIceBox iceBox = new EntityCirnoIceBox(level, target);
        if(!level.isClientSide)
        {
            level.addFreshEntity(iceBox);
        }

        return super.hurtEnemy(stack, target, attacker);
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
