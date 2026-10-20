package thKaguyaMod.entity.shot;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import thKaguyaMod.THShotLib;
import thKaguyaMod.init.THKaguyaEntities;

/** 夢想封印の光弾（1.20.1 移植版） */
public class EntityMusouFuuin extends EntityTHShot
{
	public LivingEntity target;
	private float maxSize;

    public EntityMusouFuuin(EntityType<?> type, Level level)
    {
        super(type, level);
    }

    public EntityMusouFuuin(Level world, LivingEntity user, Entity source, Vec3 pos, Vec3 angle, float slope,
    		Vec3 rotate, float rotationSpeed, int rotationEnd,
    		double firstSpeed, double limitSpeed, double acceleration, Vec3 gravity, float size, float damage, LivingEntity target)
    {
        super(THKaguyaEntities.MUSOU_FUUIN.get(), world, user, source, pos, angle, slope, rotate, rotationSpeed, rotationEnd,
                firstSpeed, limitSpeed, acceleration, gravity, 0, size, damage, 0, 40, 0);
        maxSize = size;
        this.setShotSize(0.1F);
        this.target = target;
    }

	@Override
	public void specialMotion()
	{
		if(this.getShotSize() < maxSize)
		{
			setShotSize(getShotSize() + 0.1F);
		}
		if(tickCount < 15)
		{
			Vec3 m = this.getDeltaMovement();
			this.setDeltaMovement(m.scale(0.8D));
		}
		else
		{
			if(target != null)
			{
				if(!target.isAlive())
				{
					shotLimitSpeed = 0.6D;
					shotAcceleration = 0.03D;
					setVector();
					target = null;
					return;
				}
				shotLimitSpeed = 0.6D;
				shotAcceleration = 0.03D;
				Vec3 targetVec = new Vec3(target.getX() - getX(), target.getY() + target.getEyeHeight() - getY(), target.getZ() - getZ());
				targetVec = THShotLib.getVectorNomalize(targetVec);
				Vec3 rotate = THShotLib.getOuterProduct(this.getShotVector(), targetVec);
				float rotateAngle = THShotLib.getVectorAndVectorAngle(this.getShotVector(), targetVec);
				float homingLevel = 12.0F;

				if(rotateAngle > homingLevel)
				{
					rotateAngle = homingLevel;
				}
				else if(rotateAngle < -homingLevel)
				{
					rotateAngle = -homingLevel;
				}
				angle = THShotLib.getVectorFromRotation(rotate, angle, rotateAngle);
				if(!level().isClientSide)
				{
					shotAcceleration();
				}
				updateYawAndPitch();
			}
			else
			{
				shotLimitSpeed = 0.6D;
				shotAcceleration = 0.03D;
			}
		}
	}
}
