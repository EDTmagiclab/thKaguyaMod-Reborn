package thKaguyaMod.entity.effect;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import thKaguyaMod.DanmakuConstants;
import thKaguyaMod.LaserData;
import thKaguyaMod.THShotLib;
import thKaguyaMod.init.THKaguyaEntities;

/** エイジャの赤石の光のエフェクト（1.20.1 移植版） */
public class EntityAjaRedStoneEffect extends Entity
{
    private static final EntityDataAccessor<Integer> DATA_LIGHT =
            SynchedEntityData.defineId(EntityAjaRedStoneEffect.class, EntityDataSerializers.INT);

    /** 使用者 */
    public LivingEntity user;
    /** 取り込んだ光の量 */
    public int lightLevel;
    private double length = 1.2D;

    public EntityAjaRedStoneEffect(EntityType<?> type, Level level)
    {
        super(type, level);
        lightLevel = 0;
        setLightPower(0);
    }

    public EntityAjaRedStoneEffect(Level world, LivingEntity entityLiving)
    {
        this(THKaguyaEntities.AJA_RED_STONE_EFFECT.get(), world);

        user = entityLiving;
        double x = user.getX() - Math.sin((user.getYRot()) / 180F * 3.141593F) * Math.cos(user.getXRot() / 180F * 3.141593F) * length;
        double y = user.getY() - Math.sin(user.getXRot() / 180F * 3.141593F) * length + (double)user.getEyeHeight();
        double z = user.getZ() + Math.cos((user.getYRot()) / 180F * 3.141593F) * Math.cos(user.getXRot() / 180F * 3.141593F) * length;
        this.moveTo(x, y, z, user.getYRot(), user.getXRot());
    }

    @Override
    protected void defineSynchedData()
    {
        this.entityData.define(DATA_LIGHT, 0);
    }

    @Override
    public void tick()
    {
        if(!level().isClientSide && user == null)
        {
            discard();
            return;
        }
        super.tick();

        if(user != null)
        {
            if(user instanceof Player userPlayer)
            {
                if(!userPlayer.isUsingItem())
                {
                    int damage = (int)((double)lightLevel / 40.0);
                    if(damage > 30)
                    {
                        damage = 30;
                    }
                    if(damage > 0)
                    {
                        THShotLib.createLaserA(user, this, THShotLib.pos(getX(), getY(), getZ()), THShotLib.angle(user.getYRot(), user.getXRot()), 0.1D, 4.0D, 0.3D, THShotLib.gravity_Zero(),
                                LaserData.laser(THShotLib.RED, (float)damage * 0.01F, (float)damage * 0.3F, damage, 0, 60, DanmakuConstants.SPECIAL_AJA_REDSTONE));
                        if(!level().isClientSide)
                        {
                            discard();
                        }
                    }
                }
            }
            else
            {
                if(!level().isClientSide)
                {
                    discard();
                }
            }
        }

        setLightPower(level().getLightEmission(new BlockPos((int)getX(), (int)getY(), (int)getZ())));
        lightLevel += getLightPower();

        if(user != null)
        {
            double x = user.getX() - Math.sin((user.getYRot()) / 180F * 3.141593F) * Math.cos(user.getXRot() / 180F * 3.141593F) * length;
            double y = user.getY() - Math.sin(user.getXRot() / 180F * 3.141593F) * length + user.getEyeHeight();
            double z = user.getZ() + Math.cos((user.getYRot()) / 180F * 3.141593F) * Math.cos(user.getXRot() / 180F * 3.141593F) * length;
            this.setYRot(user.getYHeadRot());
            this.setXRot(user.getXRot());
            this.setPos(x, y, z);
        }
    }

    /** 取り込んでいる光の量を設定 */
    public void setLightPower(int lightPower)
    {
        this.entityData.set(DATA_LIGHT, lightPower);
    }

    /** 取り込んでいる光の量を返す */
    public int getLightPower()
    {
        return this.entityData.get(DATA_LIGHT);
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag)
    {
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag)
    {
    }
}
