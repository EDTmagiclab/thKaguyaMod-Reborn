package thKaguyaMod.entity.shot;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import thKaguyaMod.ShotData;
import thKaguyaMod.THShotLib;
import thKaguyaMod.init.THKaguyaItems;

/**
 * お空の核弾（1.20.1 移植版，阶段 3d-3 裁剪）
 *
 * 裁剪：EntitySakuyaWatch（instanceof 检查）、THSC_YouryokuSpoiler（SPECIAL_SPOILER01=1600/02=1601 硬编码）。
 */
public class EntityNuclearShot extends EntityTHShot
{
	/** すでに放たれたかどうかのフラグ */
	public boolean shootingFlag;

	// THSC_YouryokuSpoiler 的特殊弹 ID（符卡类 3d-5 才迁移，此处硬编码）
	private static final int SPECIAL_SPOILER01 = 1600;
	private static final int SPECIAL_SPOILER02 = 1601;

    public EntityNuclearShot(EntityType<?> type, Level level)
    {
        super(type, level);
    }

	public EntityNuclearShot(Level world, LivingEntity user, Entity entity,
		Vec3 pos, Vec3 angle, float slope,
		Vec3 rotate, float rotationSpeed, int rotationEnd,
    	double firstSpeed, double limitSpeed, double acceleration, Vec3 gravity,
    	int color, float size, float damage, int delay, int end, int special)
    {
        super(world, user, entity, pos, angle, slope, rotate, rotationSpeed, rotationEnd,
                firstSpeed, limitSpeed, acceleration, gravity, color, size, damage, delay, end, special);
        shootingFlag = false;
    }

	public EntityNuclearShot(Level world, LivingEntity user, Entity entity,
			Vec3 pos, Vec3 angle, float slope,
			Vec3 rotate, float rotationSpeed, int rotationEnd,
	    	double firstSpeed, double limitSpeed, double acceleration, Vec3 gravity,
	    	ShotData shot)
    {
        this(world, user, entity, pos, angle, slope, rotate, rotationSpeed, rotationEnd,
                firstSpeed, limitSpeed, acceleration, gravity, shot.color, shot.size, shot.damage, shot.delay, shot.end, shot.special);
    }

	@Override
	public void tick()
	{
		if(user != null && user instanceof Player)
		{
			Player player = (Player)user;
			if(player.isUsingItem() && !shootingFlag)
			{
				Vec3 vec3 = THShotLib.getVecFromAngle(-getYRot() + 20F, -getXRot(), 0.3D + getShotSize() / 2.0D);
				this.moveTo(user.getX() + vec3.x, THShotLib.getPosYFromEye(user, -0.1D) + vec3.y, user.getZ() + vec3.z, -user.getYRot(), -user.getXRot());

				if(getShotSize() < 6.00F)
				{
					setShotSize(getShotSize() + 0.06F);
				}
				if(player.getUseItem().getItem() != THKaguyaItems.NUCLEAR_CONTROL_ROD.get())
				{
					if(!level().isClientSide)
					{
						level().explode(this, this.getX(), this.getY(), this.getZ(), getShotSize(), true, Level.ExplosionInteraction.TNT);
						discard();
					}
					return;
				}
				return;
			}
			else if(!shootingFlag)
			{
				if(player.getUseItem().getItem() != THKaguyaItems.NUCLEAR_CONTROL_ROD.get())
				{
					if(!level().isClientSide)
					{
						level().explode(this, this.getX(), this.getY(), this.getZ(), getShotSize(), true, Level.ExplosionInteraction.TNT);
						discard();
					}
					return;
				}
				setVector();
				shotAcceleration = 0.2D;
				shootingFlag = true;
				// 原版 1.7.10 はここで ticksExisted = 1 に戻し、遅延演出（getAnimationCount() < 0 の
				// 単発光弾描画）から通常の核弾描画へ即座に切り替えていた。
				// 1.20.1 の animationCount は -delay から 0 まで戻るまで遅延扱いのままなので、
				// 発射時に正の値へ進めておかないと「橙色の小さい弾」が出続けてしまう。
				setAnimationCount(1);
			}
		}
		else if(user == null)
		{
			if(!level().isClientSide)
			{
				discard();
				return;
			}
		}

		shotDamage = getShotSize() * 8.0F;
		super.tick();
	}

	@Override
	public boolean userHitCheck(Entity entity)
	{
		if(this.shotSpecial == SPECIAL_SPOILER02 || this.shotSpecial == SPECIAL_SPOILER01)
		{
			return true;
		}
		return !entity.is(user);
	}
}
