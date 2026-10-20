package thKaguyaMod.entity.shot;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import thKaguyaMod.init.THKaguyaEntities;

/**
 * マスタースパーク　地形破壊、高威力のレーザー（1.20.1 移植版）
 */
public class EntityMasterSpark extends EntityTHSetLaser
{
	private static final EntityDataAccessor<Integer> INI_TIME =
			SynchedEntityData.defineId(EntityMasterSpark.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Integer> MAX_LENGTH =
			SynchedEntityData.defineId(EntityMasterSpark.class, EntityDataSerializers.INT);

	public int count;
	public int flagtime;
	public double speed;
	public float size;
	public float oldSize;
	public int endtime;
	public int inicount;

    public EntityMasterSpark(EntityType<?> type, Level level)
    {
        super(type, level);
    	count = 0;
    }

    public EntityMasterSpark(Level world, LivingEntity user, Entity source,
    	Vec3 pos, Vec3 angle, float slope, Vec3 rotate, float rotationSpeed, int rotationEnd,
    	int color, float width, float length, float damage, int delay, int end, int special, Entity set, double setLength, double setYOffset)
    {
        super(THKaguyaEntities.MASTER_SPARK.get(), world, user, source, pos, angle, slope, rotate, rotationSpeed, rotationEnd,
                0, width, length, damage, delay, end, special, set, setLength, setYOffset);
    }

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder)
	{
		super.defineSynchedData(builder);
		builder.define(INI_TIME, 0);
		builder.define(MAX_LENGTH, 0);
	}

	@Override
	public boolean isPickable()
	{
		return false;
	}

	@Override
	public void tick()
	{
		super.tick();

		if(!level().isClientSide && tickCount == 1)
		{
			discard();
		}
	}

	@Override
	protected void onImpact(HitResult result)
	{
		// MasterSpark 不通过 onImpact 造成伤害（地形破坏由生成瞬间处理）
	}

	public void setIniTime(int par1)
	{
		this.entityData.set(INI_TIME, par1);
	}

	public int getIniTime()
	{
		return this.entityData.get(INI_TIME);
	}

	public void setMaxLength(double par1)
	{
		this.entityData.set(MAX_LENGTH, (int)(par1 * 100.0D));
	}

	public double getMaxLength()
	{
		return (double)this.entityData.get(MAX_LENGTH) / 100.0D;
	}
}
