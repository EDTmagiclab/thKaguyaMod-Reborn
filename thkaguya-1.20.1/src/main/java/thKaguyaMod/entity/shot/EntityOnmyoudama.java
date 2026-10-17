package thKaguyaMod.entity.shot;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import thKaguyaMod.THShotLib;
import thKaguyaMod.init.THKaguyaItems;

/** 陰陽玉（1.20.1 移植版，阶段 3d-3 裁剪） */
public class EntityOnmyoudama extends EntityTHShot
{
	private boolean shootingFlag;
	private boolean userHit;
	private int hitTimer;

    public EntityOnmyoudama(EntityType<?> type, Level level)
    {
        super(type, level);
    }

	public EntityOnmyoudama(Level world, LivingEntity user, Entity source,
		Vec3 pos, Vec3 angle, float slope, Vec3 rotate, float rotationSpeed, int rotationEnd,
    	double firstSpeed, double limitSpeed, double acceleration,
    	Vec3 gravity, int color, float size, float damage, int delay, int end, int special)
    {
        super(world, user, source, pos, angle, slope, rotate, rotationSpeed, rotationEnd,
                firstSpeed, limitSpeed, acceleration, gravity, color, size, damage, delay, end, special);
        shootingFlag = false;
        userHit = false;
        hitTimer = 0;
    }

	@Override
	public void tick()
	{
		if(shootingFlag)
		{
			shotAcceleration = 0.0D;
		}
		if(user != null && user instanceof Player)
		{
			Player player = (Player)user;
			if(player.isUsingItem() && !shootingFlag)
			{
				Vec3 vec3 = THShotLib.getVecFromAngle(-getYRot(), -getXRot(), 0.6D);
				this.moveTo(user.getX() + vec3.x * 2, THShotLib.getPosYFromEye(user, -0.1D) + vec3.y, user.getZ() + vec3.z, -user.getYRot(), -user.getXRot());

				if(getShotSize() < 3.00F)
				{
					setShotSize(getShotSize() + 0.06F);
				}
				if(!isHoldingMikoStick(player))
				{
					if(!level().isClientSide)
					{
						discard();
					}
					return;
				}
				return;
			}
			else if(!shootingFlag)
			{
				if(!isHoldingMikoStick(player))
				{
					if(!level().isClientSide)
					{
						discard();
					}
					return;
				}
				// 原版: ticksExisted = 1（発射時に弾の寿命をリセット）
				tickCount = 1;
				shotSpeed = 0.5D;
				setVector();
				shotAcceleration = 1.0D;
				shootingFlag = true;
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

		shotDamage = getShotSize() * 6.0F;
		super.tick();
	}

	@Override
	public boolean userHitCheck(Entity entity)
	{
		return userHit;
	}

	@Override
	protected boolean isUserHit()
	{
		return true;
	}

	@Override
	public boolean entityHitSpecialProcess(Entity hitEntity)
	{
		setYRot(-getYRot());
		setXRot(-getXRot());
		setVector();
		Vec3 m = this.getDeltaMovement();
		this.setDeltaMovement(m.scale(0.8D));
		return false;
	}

	@Override
	public boolean hitCheckEx(Entity entity)
	{
		return false;
	}

	/**
	 * 手に御幣を持っているか（原版 player.inventory.getCurrentItem() の判定）
	 *
	 * 1.20.1 の player.getUseItem() は「使用中でなければ空」を返すため、
	 * 右クリックを離した瞬間に御幣ではないと誤判定し、弾が射出されずに消えてしまっていた。
	 * 原版同様「今持っているアイテム」で判定する。
	 */
	private static boolean isHoldingMikoStick(Player player)
	{
		return player.getMainHandItem().getItem() == THKaguyaItems.HAKUREI_MIKO_STICK.get()
				|| player.getOffhandItem().getItem() == THKaguyaItems.HAKUREI_MIKO_STICK.get();
	}
}
