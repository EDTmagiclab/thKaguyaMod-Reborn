package thKaguyaMod.entity.shot;

import com.mojang.logging.LogUtils;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;
import thKaguyaMod.LaserData;
import thKaguyaMod.THShotLib;
import thKaguyaMod.init.THKaguyaEntities;

/**
 * 固定レーザー系の共通処理（1.20.1 移植版）
 *
 * 阶段 3d-3：固定激光。跟随 setting 实体，增长激光长度。
 * 裁剪：EntitySakuyaWatch（instanceof 排除检查，未迁移）——复用 EntityTHLaser 的 hitCheck/onImpact。
 */
public class EntityTHSetLaser extends EntityTHLaser
{
	private static final Logger LOGGER = LogUtils.getLogger();

	private double settingLength;
	private double settingYOffset;

	public Entity setting;

    public EntityTHSetLaser(EntityType<?> type, Level level)
    {
        super(type, level);
    }

	public EntityTHSetLaser(EntityType<?> type, Level world, LivingEntity user, Entity source,
			Vec3 pos, Vec3 angle, float slope, Vec3 rotate, float rotationSpeed, int rotationEnd,
			int color, float width, float length, float damage, int delay, int end, int special,
			Entity set, double setLength, double setYOffset)
	{
		super(type, world, user, source, pos, angle, slope, rotate, rotationSpeed, rotationEnd,
				0.0D, 0.0D, 0.0D, THShotLib.gravity_Zero(), color, width, length, damage, delay, end, special);
		setLaserLength(length);
		setMaxLaserLength(length);
		settingLength = setLength;
		settingYOffset = setYOffset;
		setting = set;
		if(!world.isClientSide)
		{
			LOGGER.info("[SetLaser] spawn color={} length={} angle=({},{},{}) set={} setLength={} setYOffset={}",
					color, length, angle.x, angle.y, angle.z, set, setLength, setYOffset);
		}
		if(set != null)
		{
			double x = set.getX() + angle.x * settingLength;
			double y = set.getY() + angle.y * settingLength + settingYOffset;
			double z = set.getZ() + angle.z * settingLength;
			this.setPos(x, y, z);
		}
	}

	public EntityTHSetLaser(Level world, LivingEntity user, Entity source,
			Vec3 pos, Vec3 angle, float slope, Vec3 rotate, float rotationSpeed, int rotationEnd,
			int color, float width, float length, float damage, int delay, int end, int special,
			Entity set, double setLength, double setYOffset)
	{
		this(THKaguyaEntities.TH_SET_LASER.get(), world, user, source, pos, angle, slope, rotate, rotationSpeed, rotationEnd,
				color, width, length, damage, delay, end, special, set, setLength, setYOffset);
	}

	public EntityTHSetLaser(Level world, LivingEntity user, Entity source,
			Vec3 pos, Vec3 angle, float slope, Vec3 rotate, float rotationSpeed, int rotationEnd,
			LaserData laser, Entity set, double setLength, double setYOffset)
	{
		this(world, user, source, pos, angle, slope, rotate, rotationSpeed, rotationEnd,
				laser.form * 8 + laser.color, laser.width, laser.length, laser.damage,
				laser.delay, laser.end, laser.special, set, setLength, setYOffset);
	}

	@Override
	public void tick()
	{
		super.tick();

		if(this.setting != null)
		{
			if(!setting.isAlive())
			{
				discard();
				return;
			}
			double x = setting.getX() + settingLength * angle.x;
			double y = setting.getY() + settingLength * angle.y + settingYOffset;
			double z = setting.getZ() + settingLength * angle.z;
			this.setPos(x, y, z);
			if(getLaserLength() < getMaxLaserLength())
			{
				setLaserLength(getLaserLength() + 6.0D);
			}
			else
			{
				setLaserLength(getMaxLaserLength());
			}
		}
		else
		{
			this.setPos(getX() + angle.x, getY() + angle.y, getZ() + angle.z);
			if(!level().isClientSide)
			{
				discard();
			}
		}
	}
}
