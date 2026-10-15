package thKaguyaMod.entity.effect;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import thKaguyaMod.init.THKaguyaEntities;

/** アイシクルソードの氷ブロック（1.20.1 移植版） */
public class EntityCirnoIceBox extends Entity
{
    private static final EntityDataAccessor<Float> DATA_WIDTH =
            SynchedEntityData.defineId(EntityCirnoIceBox.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_HEIGHT =
            SynchedEntityData.defineId(EntityCirnoIceBox.class, EntityDataSerializers.FLOAT);

    /** 凍らせた相手 */
    public LivingEntity frozen;
    private int count;
    protected float iceBoxWidth;
    protected float iceBoxHeight;

    public EntityCirnoIceBox(EntityType<?> type, Level level)
    {
        super(type, level);
    }

    public EntityCirnoIceBox(Level world, LivingEntity frozenEntity)
    {
        this(THKaguyaEntities.CIRNO_ICE_BOX.get(), world);

        frozen = frozenEntity;
        setIceBoxWidth(1.0F);
        setIceBoxHeight(frozen.getBbHeight());
        this.setPos(frozen.getX(), frozen.getY(), frozen.getZ());
    }

    @Override
    protected void defineSynchedData()
    {
        this.entityData.define(DATA_WIDTH, 0.5F);
        this.entityData.define(DATA_HEIGHT, 0.5F);
    }

    protected void setIceBoxWidth(float width)
    {
        this.entityData.set(DATA_WIDTH, width);
    }

    public float getIceBoxWidth()
    {
        return this.entityData.get(DATA_WIDTH);
    }

    protected void setIceBoxHeight(float height)
    {
        this.entityData.set(DATA_HEIGHT, height);
    }

    public float getIceBoxHeight()
    {
        return this.entityData.get(DATA_HEIGHT);
    }

    @Override
    public boolean isPushable()
    {
        return true;
    }

    @Override
    public boolean isPickable()
    {
        return true;
    }

    @Override
    public void tick()
    {
        super.tick();

        if(frozen != null)
        {
            if(!frozen.isDeadOrDying())
            {
                frozenInIce();
                // 跟随被冻结实体
                this.setPos(frozen.getX(), frozen.getY(), frozen.getZ());
            }
        }

        if(tickCount > 50)
        {
            setIceBoxHeight(getIceBoxHeight() - 0.08F);
        }
        if(frozen != null && frozen instanceof thKaguyaMod.entity.living.EntityCirno)
        {
            setIceBoxHeight(getIceBoxHeight() - 0.16F);
        }

        if(getIceBoxHeight() <= -0.7F)
        {
            if(!level().isClientSide)
            {
                discard();
            }
        }
    }

    /** 時間操作空間に入っていた時の処理：被冻结实体回退到上一帧状态并停住 */
    protected void frozenInIce()
    {
        frozen.setPos(frozen.xo, frozen.yo, frozen.zo);
        frozen.setDeltaMovement(Vec3.ZERO);
        frozen.setYRot(frozen.yRotO);
        frozen.setXRot(frozen.xRotO);

        if(frozen instanceof Creeper creeper)
        {
            creeper.setSwellDir(-1);
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag)
    {
        tag.putShort("count", (short)count);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag)
    {
        count = tag.getShort("count");
    }
}
