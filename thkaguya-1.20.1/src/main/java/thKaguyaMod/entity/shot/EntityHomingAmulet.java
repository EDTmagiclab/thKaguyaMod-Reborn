package thKaguyaMod.entity.shot;

import static thKaguyaMod.DanmakuConstants.*;

import java.util.List;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import thKaguyaMod.ShotData;
import thKaguyaMod.THShotLib;

/** ホーミングアミュレット（1.20.1 移植版） */
public class EntityHomingAmulet extends EntityTHShot
{
	private static final EntityDataAccessor<String> OWNER_NAME =
			SynchedEntityData.defineId(EntityHomingAmulet.class, EntityDataSerializers.STRING);

    public EntityHomingAmulet(EntityType<?> type, Level level)
    {
        super(type, level);
    }

    public EntityHomingAmulet(Level world, LivingEntity user, Entity source,
    		Vec3 pos, Vec3 angle, float slope, Vec3 rotate, float rotationSpeed, int rotationEnd,
        	double firstSpeed, double limitSpeed, double acceleration,
        	Vec3 gravity, int color, float size, float damage, int delay, int end, int special)
    {
    	super(world, user, source, pos, angle, slope, rotate, rotationSpeed, rotationEnd,
                firstSpeed, limitSpeed, acceleration, gravity, color, size, damage, delay, end, special);
    	this.setOwner("");
    }

	@Override
	protected void defineSynchedData()
	{
		super.defineSynchedData();
		this.entityData.define(OWNER_NAME, "");
	}

	@Override
	public void specialMotion()
	{
		// 大きな拡散アミュレットで、16tick 目なら拡散
		if(tickCount == 16 && this.getShotColor() == 3)
		{
			if(user != null)
			{
				double vectorXG = -Math.sin(user.getYRot() / 180F * Math.PI) * Math.cos(user.getXRot() / 180F * Math.PI) * 0.09D;
				double vectorYG = -Math.sin(user.getXRot() / 180F * Math.PI) * 0.09D;
				double vectorZG =  Math.cos(user.getYRot() / 180F * Math.PI) * Math.cos(user.getXRot() / 180F * Math.PI) * 0.09D;
				ShotData shot = ShotData.shot(FORM_AMULET, BLUE, 0.4F, 5.0F, 0, 60);
				THShotLib.createSphereShot(user, user, THShotLib.pos_Entity(this), angle, 0F, THShotLib.rotate_Default(), 0F, 9999,
						0.6D, 0.6D, 0.0D, THShotLib.gravity(vectorXG, vectorYG, vectorZG), shot, 32, 0.0D, this.random.nextFloat() * 360F);
			}

			if(!level().isClientSide)
			{
				discard();
			}
			return;
		}
		// 小さな拡散アミュレットなら特殊な処理はしない
		if(getShotColor() >= 2)
		{
			return;
		}

		// 追尾
		Vec3 motion = this.getDeltaMovement();
		AABB area = this.getBoundingBox().expandTowards(motion.x, motion.y, motion.z).inflate(24.0D);
		List<Entity> list = level().getEntities(this, area);

		LivingEntity nearEntity = null;
		double nearDistance = 999.9D;
		float nearAngle = 180F;
		double nearValue = nearDistance * THShotLib.halfAbsSin(nearAngle / 180F * (float)Math.PI);
		Vec3 shotVec = this.getShotVector();

		for(Entity entitys : list)
		{
			if(!(entitys instanceof LivingEntity) || entitys instanceof Animal || entitys instanceof Villager
					|| entitys == source || entitys == user)
			{
				continue;
			}
			LivingEntity entity1 = (LivingEntity)entitys;
			if(!entity1.isAlive())
			{
				continue;
			}

			Vec3 start = new Vec3(getX(), getY(), getZ());
			Vec3 end = new Vec3(entity1.getX(), entity1.getY() + entity1.getEyeHeight(), entity1.getZ());
			var blockHit = level().clip(new net.minecraft.world.level.ClipContext(
					start, end, net.minecraft.world.level.ClipContext.Block.COLLIDER,
					net.minecraft.world.level.ClipContext.Fluid.NONE, this));
			if(blockHit != null && blockHit.getType() == net.minecraft.world.phys.HitResult.Type.BLOCK)
			{
				continue;
			}

			Vec3 targetVec = new Vec3(entity1.getX() - getX(), entity1.getY() + entity1.getEyeHeight() - getY(), entity1.getZ() - getZ());
			targetVec = THShotLib.getVectorNomalize(targetVec);
			float angleSpan = Math.abs(THShotLib.getVectorAndVectorAngle(shotVec, targetVec));
			double toEntity1Distance = this.position().distanceTo(new Vec3(entity1.getX(), entity1.getY() + entity1.getEyeHeight(), entity1.getZ()));
			double value = toEntity1Distance * THShotLib.halfAbsSin(angleSpan / 180F * (float)Math.PI);
			if(nearValue > value)
			{
				nearEntity = entity1;
				nearAngle = angleSpan;
				nearValue = value;
				nearDistance = toEntity1Distance;
			}
		}

		if(nearEntity != null)
		{
			float homingLevel = 4.0F;
			Vec3 targetVec = new Vec3(nearEntity.getX() - getX(), nearEntity.getY() + nearEntity.getEyeHeight() - getY(), nearEntity.getZ() - getZ());
			targetVec = THShotLib.getVectorNomalize(targetVec);
			Vec3 rotate = THShotLib.getOuterProduct(this.getShotVector(), targetVec);
			float rotateAngle = THShotLib.getVectorAndVectorAngle(shotVec, targetVec);
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
				this.updateMotion();
				this.updateAngle();
			}
		}
	}

	public String getOwnerName()
	{
		return this.entityData.get(OWNER_NAME);
	}

	public void setOwner(String str)
	{
		this.entityData.set(OWNER_NAME, str);
	}
}
