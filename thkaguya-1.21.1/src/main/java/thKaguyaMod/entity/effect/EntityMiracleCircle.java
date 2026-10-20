package thKaguyaMod.entity.effect;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import thKaguyaMod.THShotLib;
import thKaguyaMod.init.THKaguyaEntities;

/** 風祝のお祓い棒の詠唱時のサークル（1.20.1 移植版） */
public class EntityMiracleCircle extends Entity
{
    private static final EntityDataAccessor<Integer> DATA_COLOR =
            SynchedEntityData.defineId(EntityMiracleCircle.class, EntityDataSerializers.INT);

    /** 五芒星を描いているプレイヤー */
    public Player user;
    /** 五芒星の色番号 */
    private int color;
    /** 五芒星を描いている時間 */
    public int count;
    /** 五芒星の角度 */
    public float turnAngle;
    /** 前に使用していたときのカウント */
    public int befUseCount;

    public EntityMiracleCircle(EntityType<?> type, Level level)
    {
        super(type, level);
    }

    public EntityMiracleCircle(Level world, Player entityPlayer, int cl)
    {
        this(THKaguyaEntities.MIRACLE_CIRCLE.get(), world);

        user = entityPlayer;
        Vec3 vec3 = THShotLib.getRotationVectorFromAngle(user.getYRot(), user.getXRot(), turnAngle, 3.0D);
        this.setPos(user.getX() + vec3.x, user.getY() + vec3.y + (double)user.getEyeHeight(), user.getZ() + vec3.z);
        this.setYRot(entityPlayer.getYRot());
        this.setXRot(0.0F);
        color = cl;
        setColor(color);
        count = 0;
        befUseCount = 0;
        turnAngle = 0F;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder)
    {
        builder.define(DATA_COLOR, 0);
    }

    @Override
    public void tick()
    {
        if(!level().isClientSide && user == null)
        {
            discard();
        }
        super.tick();
        if(user != null)
        {
            if(!user.isUsingItem())
            {
                if(!level().isClientSide)
                {
                    discard();
                }
            }
        }
        count++;
        if(count == 30 && getColor() < 4)
        {
            if(user != null)
            {
                EntityMiracleCircle entityMiracle = new EntityMiracleCircle(level(), user, getColor() + 1);
                if(!level().isClientSide)
                {
                    level().addFreshEntity(entityMiracle);
                }
            }
        }

        if(user != null)
        {
            Vec3 vec3 = THShotLib.getRotationVectorFromAngle(user.getYRot(), user.getXRot(), turnAngle, 3.0D);
            this.setPos(user.getX() + vec3.x, user.getY() + vec3.y + (double)user.getEyeHeight(), user.getZ() + vec3.z);
            this.setYRot(user.getYRot());
            this.setXRot(user.getXRot());
            befUseCount = getColor() * 30 + count;
        }

        if(count > 30)
        {
            turnAngle += 2.4F;
        }
    }

    /** 五芒星の色を設定 */
    public void setColor(int color)
    {
        this.entityData.set(DATA_COLOR, color);
    }

    /** 五芒星の色を返す */
    public int getColor()
    {
        return this.entityData.get(DATA_COLOR);
    }

    /** 現在の五芒星の描いた線の数を返す */
    public int getNumberOfDrewLine()
    {
        int time = count;
        if(time > 30)
        {
            return 5;
        }
        else
        {
            return time / 6 + 1;
        }
    }

    /** 今描いている線が何本目か返す */
    public int getLastLineTime()
    {
        int time = count;
        if(time > 30)
        {
            return 10;
        }
        else
        {
            return time % 6;
        }
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
