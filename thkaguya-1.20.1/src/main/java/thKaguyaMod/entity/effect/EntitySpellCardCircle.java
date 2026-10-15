package thKaguyaMod.entity.effect;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import thKaguyaMod.init.THKaguyaEntities;

/**
 * スペルカード使用時の魔法陣のエフェクト（1.20.1 移植版）
 *
 * 阶段 3d-2：符卡魔法阵。跟随使用者，动画计数，到时间消失。
 * 渲染器阶段 4 补（RenderSpellCardCircle）。
 */
public class EntitySpellCardCircle extends Entity
{
    private static final EntityDataAccessor<Float> CIRCLE_SIZE =
            SynchedEntityData.defineId(EntitySpellCardCircle.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> CIRCLE_TYPE =
            SynchedEntityData.defineId(EntitySpellCardCircle.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> END_TIME =
            SynchedEntityData.defineId(EntitySpellCardCircle.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> ANIMATION_COUNT =
            SynchedEntityData.defineId(EntitySpellCardCircle.class, EntityDataSerializers.INT);

    /** 使用者 */
    public LivingEntity user;
    protected int lastTime;

    public EntitySpellCardCircle(EntityType<?> type, Level level)
    {
        super(type, level);
        lastTime = 0;
        setAnimationCount(0);
    }

    public EntitySpellCardCircle(Level level, LivingEntity entityLiving, int type, int end)
    {
        this(THKaguyaEntities.SPELL_CARD_CIRCLE.get(), level);

        user = entityLiving;
        setCircleType(type);
        setEndTime(end);
        double x = user.getX();
        double y = user instanceof Player ? user.getY() + 0.1D : user.getY() + user.getBbHeight() / 2.0D;
        double z = user.getZ();
        this.moveTo(x, y, z, user.getYRot(), user.getXRot());
    }

    @Override
    protected void defineSynchedData()
    {
        this.entityData.define(CIRCLE_SIZE, 0.0F);
        this.entityData.define(CIRCLE_TYPE, 0);
        this.entityData.define(END_TIME, 0);
        this.entityData.define(ANIMATION_COUNT, 0);
    }

    @Override
    public boolean isPickable()
    {
        return false;
    }

    @Override
    public void tick()
    {
        // 使用者がいないなら消滅
        if(!level().isClientSide && user == null)
        {
            discard();
            return;
        }

        super.tick();

        if(!level().isClientSide)
        {
            setAnimationCount(tickCount);
        }

        // 終了時間を過ぎたら消滅
        if(getEndTime() < tickCount && getEndTime() >= 0)
        {
            if(!level().isClientSide)
            {
                discard();
                return;
            }
        }

        // 魔法阵尺寸：前 5 tick 展开，之后按剩余时间缩小
        if(getAnimationCount() < 5)
        {
            setCircleSize(((float)getAnimationCount()) / 5.0F);
        }
        else
        {
            float endTime2 = (float)getEndTime();
            if(getCircleType() < 16 && endTime2 > 0.0F)
            {
                setCircleSize((endTime2 - (float)getAnimationCount()) / endTime2);
            }
        }

        // 跟随使用者
        if(user != null)
        {
            double x = user.getX();
            double y = user instanceof Player ? user.getY() + 0.1D : user.getY() + user.getBbHeight() / 2.0D;
            double z = user.getZ();
            float yaw = user.getYHeadRot();
            float pitch = user.getXRot();
            this.moveTo(x, y, z, yaw, pitch);
        }

        lastTime = tickCount;
    }

    public void setCircleSize(float size)
    {
        this.entityData.set(CIRCLE_SIZE, size);
    }

    public float getCircleSize()
    {
        return this.entityData.get(CIRCLE_SIZE);
    }

    public void setCircleType(int type)
    {
        this.entityData.set(CIRCLE_TYPE, type);
    }

    public int getCircleType()
    {
        return this.entityData.get(CIRCLE_TYPE);
    }

    public void setEndTime(int time)
    {
        this.entityData.set(END_TIME, time);
    }

    public int getEndTime()
    {
        return this.entityData.get(END_TIME);
    }

    public void setAnimationCount(int time)
    {
        this.entityData.set(ANIMATION_COUNT, time);
    }

    public int getAnimationCount()
    {
        return this.entityData.get(ANIMATION_COUNT);
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
