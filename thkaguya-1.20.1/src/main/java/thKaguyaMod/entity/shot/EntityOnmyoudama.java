package thKaguyaMod.entity.shot;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import thKaguyaMod.THShotLib;
import thKaguyaMod.init.THKaguyaEntities;
import thKaguyaMod.init.THKaguyaItems;

/** 陰陽玉（1.20.1 移植版，阶段 3d-3 裁剪） */
public class EntityOnmyoudama extends EntityTHShot
{
	private boolean shootingFlag;
	/**
	 * 発射直後は「まだ何にも当たらない」フラグ。
	 *
	 * <p>原版 {@code userHitCheck(Entity)} はこのフィールドをそのまま返し、
	 * {@link EntityTHShot#hitEntityCheck} は false を返したエンティティを全スキップする。
	 * つまり <b>このフラグを立て忘れると弾は永久に何にも当たらない（＝無ダメージ）</b>。
	 * 原版は onUpdate 内で「使用者に当たっていない tick」を 2 回数えてから true にする。
	 */
	private boolean userHit;
	private int hitTimer;

	/** 一時診断（原因判明後に削除すること） */
	private static final Logger DBG = LogUtils.getLogger();
	private static final boolean DBG_ON = true;
	private int dbgTicks;

    public EntityOnmyoudama(EntityType<?> type, Level level)
    {
        super(type, level);
    }

	/** 完全版コンストラクタ（自分の EntityType を明示できる） */
	public EntityOnmyoudama(EntityType<?> type, Level world, LivingEntity user, Entity source,
		Vec3 pos, Vec3 angle, float slope, Vec3 rotate, float rotationSpeed, int rotationEnd,
    	double firstSpeed, double limitSpeed, double acceleration,
    	Vec3 gravity, int color, float size, float damage, int delay, int end, int special)
    {
        super(type, world, user, source, pos, angle, slope, rotate, rotationSpeed, rotationEnd,
                firstSpeed, limitSpeed, acceleration, gravity, color, size, damage, delay, end, special);
        shootingFlag = false;
        userHit = false;
        hitTimer = 0;
    }

	/**
	 * 通常コンストラクタ。
	 *
	 * <p><b>EntityType は必ず {@code THKaguyaEntities.ONMYOUDAMA} を渡すこと。</b>
	 * 基底クラスの便宜コンストラクタ（{@code EntityTHShot(Level, LivingEntity, ...)}）を使うと
	 * EntityType が {@code TH_SHOT} になり、サーバーはスポーンパケットに TH_SHOT の ID を載せる。
	 * するとクライアントは {@code EntityTHShot} を生成し、汎用 {@code RenderTHShot} で
	 * 「巨大な橙色の四角」として描画してしまう（＝ユーザー報告「陰陽玉ではなく巨大な橙色の弾」の正体）。
	 * 専用レンダラー {@code RenderOnmyoudama} は ONMYOUDAMA 型でしか選ばれない。
	 */
	public EntityOnmyoudama(Level world, LivingEntity user, Entity source,
		Vec3 pos, Vec3 angle, float slope, Vec3 rotate, float rotationSpeed, int rotationEnd,
    	double firstSpeed, double limitSpeed, double acceleration,
    	Vec3 gravity, int color, float size, float damage, int delay, int end, int special)
    {
        this(THKaguyaEntities.ONMYOUDAMA.get(), world, user, source, pos, angle, slope,
                rotate, rotationSpeed, rotationEnd,
                firstSpeed, limitSpeed, acceleration, gravity,
                color, size, damage, delay, end, special);
    }

	/**
	 * 発射処理（原版 onUpdate の「使用をやめた瞬間」ブロック）。
	 *
	 * <p>原版は弾自身が「使用者がまだ使っているか」を見て発射するが、1.20.1 では
	 * 解放パケットと弾の tick 順序に依存して取りこぼすことがあるため、
	 * {@code ItemOharaibouR.releaseUsing} からも明示的に呼べるようにしてある
	 * （{@code shootingFlag} で二重発射は防ぐ）。
	 */
	public void launch()
	{
		if(shootingFlag)
		{
			return;
		}

		// 原版: ticksExisted = 1（発射時に弾の寿命をリセット）
		tickCount = 1;
		shotSpeed = 0.5D;
		setVector();
		// 移植版 setVector() は updateMotion() → getSpeed()（＝現在の deltaMovement の長さ）を使うため、
		// チャージ中（delta = 0）に呼ぶと初速が 0 になる。原版 setVector() は shotSpeed を使っていたので、
		// ここで明示的に shotSpeed ぶんの初速を与える（さもないと初速 0 のまま＝飛ばない）。
		this.setDeltaMovement(angle.scale(shotSpeed));
		lastShotMotionX = angle.x * shotSpeed;
		lastShotMotionY = angle.y * shotSpeed;
		lastShotMotionZ = angle.z * shotSpeed;
		// 原版 setSize(shotSize, shotSize)：1.20.1 は EntityType.Builder.sized() で
		// 固定されるため按实例変更できない（当たり判定は hitCheck() が getShotSize() で行う）
		shootingFlag = true;

		if(DBG_ON && !level().isClientSide)
		{
			DBG.info("[TH-dbg] onmyo LAUNCH t={} angle=({},{},{}) motion=({},{},{}) size={} dmg={}",
					tickCount, f(angle.x), f(angle.y), f(angle.z),
					f(getDeltaMovement().x), f(getDeltaMovement().y), f(getDeltaMovement().z),
					f(getShotSize()), f(shotDamage));
		}
	}

	public boolean isLaunched()
	{
		return shootingFlag;
	}

	@Override
	public void tick()
	{
		if(DBG_ON && !level().isClientSide && dbgTicks < 30)
		{
			dbgTicks++;
			DBG.info("[TH-dbg] onmyo t={} pos=({},{},{}) size={} dmg={} anim={} end={} speed={} motion=({},{},{}) flag={} userHit={} using={} alive={}",
					tickCount, f(getX()), f(getY()), f(getZ()), f(getShotSize()), f(shotDamage),
					getAnimationCount(), getShotEndTime(), f(getSpeed()),
					f(getDeltaMovement().x), f(getDeltaMovement().y), f(getDeltaMovement().z),
					shootingFlag, userHit,
					user instanceof Player pl && pl.isUsingItem(), isAlive());
		}

		if(user instanceof Player player)
		{
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
				launch();
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

		// 原版 159-176 行：発射直後の 2 tick は当たらない（userHit = false のまま）。
		// 立て忘れると userHitCheck() が永久 false → 全エンティティをスキップ → 無ダメージ。
		if(!userHit)
		{
			hitTimer++;
			if(hitTimer >= 2)
			{
				userHit = true;
			}
		}

		shotDamage = getShotSize() * 6.0F;
		super.tick();

		// 原版 141-157 行：地面に食い込んだら押し出して反射（回転 pitch 反転 + motionY ×0.9）
		if(!level().isClientSide)
		{
			Vec3 start = new Vec3(getX(), getY(), getZ());
			Vec3 end = new Vec3(getX(), getY() + getShotSize(), getZ());
			BlockHitResult groundHit = level().clip(new ClipContext(start, end,
					ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));

			if(groundHit != null && groundHit.getType() == HitResult.Type.BLOCK)
			{
				int blockY = groundHit.getBlockPos().getY();
				double diveY = blockY - (getY() + getShotSize());
				setPos(getX(), blockY + getShotSize() + 0.01D + diveY, getZ());
				setXRot(-getXRot());
				setVector();
				Vec3 m = this.getDeltaMovement();
				this.setDeltaMovement(m.x, m.y * 0.9D, m.z);
				lastShotMotionY *= 0.9D;
			}
		}
	}

	@Override
	public void remove(Entity.RemovalReason reason)
	{
		if(DBG_ON && !level().isClientSide)
		{
			DBG.info("[TH-dbg] onmyo REMOVE reason={} t={} pos=({},{},{}) size={} dmg={} anim={} flag={} userHit={}",
					reason, tickCount, f(getX()), f(getY()), f(getZ()),
					f(getShotSize()), f(shotDamage), getAnimationCount(), shootingFlag, userHit);
		}
		super.remove(reason);
	}

	/** 原版の意味：true になるまで全エンティティを弾く（＝発射直後の無敵時間） */
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

	/** 一時診断用の小数整形 */
	private static String f(double value)
	{
		return String.format(java.util.Locale.ROOT, "%.2f", value);
	}
}
