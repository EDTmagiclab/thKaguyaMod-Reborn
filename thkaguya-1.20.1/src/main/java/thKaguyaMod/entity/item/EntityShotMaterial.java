package thKaguyaMod.entity.item;

import javax.annotation.Nullable;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import thKaguyaMod.THShotLib;
import thKaguyaMod.init.THKaguyaItems;

/**
 * 弾の素（原版 {@code thKaguyaMod.entity.EntityShotMaterial} の 1.20.1 移植）。
 *
 * <p>原版の挙動（{@code thKaguyaMod/entity/EntityShotMaterial.java}）:
 * <ul>
 *   <li>クライアント側は何もしない（{@code onUpdate} の先頭で {@code isRemote} なら return）。
 *       位置はバニラのトラッカー同期だけで描画される。</li>
 *   <li>300 tick で消滅。</li>
 *   <li>周囲 20 ブロックのプレイヤーを探し、いなければ即消滅（原版どおり）。
 *       見つかれば {@link #canBePickedUp(Player)} を満たすプレイヤーへ接近し、
 *       接触時に {@code shot_material} を渡す。</li>
 * </ul>
 *
 * <p>原版の {@code lastTime} による時間停止判定は、コンストラクタで -1 を代入した後
 * 一度も更新されない死んだコードだったため移植していない（原版でも発動しない）。
 */
public class EntityShotMaterial extends Entity
{
	/** 原版の弾の色（0..7）。{@link thKaguyaMod.entity.EntityDivineSpirit} と同じ並び。 */
	private static final EntityDataAccessor<Byte> DATA_COLOR =
			SynchedEntityData.defineId(EntityShotMaterial.class, EntityDataSerializers.BYTE);

	/** 接近速度の上限（原版 limitSpeed = 0.5D）。 */
	protected double limitSpeed = 0.5D;

	/** 原版 speed（接近加速度の積分値）。 */
	private double speed;

	/** 原版 lastSpeed（使っていないが互換のため保持）。 */
	private double lastSpeed;

	protected double lastMotionX;
	protected double lastMotionY;
	protected double lastMotionZ;

	@Nullable
	protected Player target;

	public EntityShotMaterial(EntityType<?> type, Level level)
	{
		super(type, level);
	}

	public EntityShotMaterial(EntityType<?> type, Level level, double posX, double posY, double posZ)
	{
		super(type, level);
		this.setPos(posX, posY, posZ);
		Vec3 motion = this.getDeltaMovement();
		this.lastMotionX = motion.x;
		this.lastMotionY = motion.y;
		this.lastMotionZ = motion.z;
		this.target = null;
	}

	@Override
	protected void defineSynchedData()
	{
		this.entityData.define(DATA_COLOR, (byte)0);
	}

	public short getShotMaterialColor()
	{
		return this.entityData.get(DATA_COLOR);
	}

	public void setShotMaterialColor(short color)
	{
		this.entityData.set(DATA_COLOR, (byte)color);
	}

	@Override
	public void addAdditionalSaveData(CompoundTag tag)
	{
		tag.putShort("color", this.getShotMaterialColor());
	}

	@Override
	public void readAdditionalSaveData(CompoundTag tag)
	{
		this.setShotMaterialColor(tag.getShort("color"));
	}

	/** 原版 canBeCollidedWith() = true。 */
	@Override
	public boolean isPickable()
	{
		return true;
	}

	/** 原版 canBePushed() = false。 */
	@Override
	public boolean isPushable()
	{
		return false;
	}

	/** 原版 getEntitySpeed()。 */
	public double getEntitySpeed()
	{
		return this.getDeltaMovement().length();
	}

	@Override
	public void tick()
	{
		// 原版 onUpdate: if (worldObj.isRemote) return;
		if (this.level().isClientSide)
		{
			return;
		}

		super.tick();

		// 原版: 300tick で消滅
		if (this.tickCount > 300)
		{
			this.discard();
			return;
		}

		if (this.target == null || !this.target.isAlive() || this.target.isSpectator())
		{
			this.target = null;
			Player near = this.findNearestPlayer(20.0D);
			if (near == null)
			{
				// 原版: 周囲 20 ブロックにプレイヤーが誰もいなければ setDead()
				this.discard();
				return;
			}
			// 原版は check(player)＝潜行中のみ target にする。
			// 潜行していないプレイヤーしかいない場合は target を立てず、
			// そのまま 300 tick の寿命で消える（ここで discard すると原版より早く消える）。
			if (!this.canBePickedUp(near))
			{
				return;
			}
			this.target = near;
		}

		this.moveToPlayer(this.target);

		Vec3 motion = this.getDeltaMovement();
		this.lastMotionX = motion.x;
		this.lastMotionY = motion.y;
		this.lastMotionZ = motion.z;
		this.lastSpeed = this.speed;
	}

	@Nullable
	protected Player findNearestPlayer(double range)
	{
		Player best = null;
		double bestDistance = Double.MAX_VALUE;
		for (Player player : this.level().getEntitiesOfClass(Player.class, this.getBoundingBox().inflate(range)))
		{
			if (!player.isAlive() || player.isSpectator())
			{
				continue;
			}
			double distance = player.distanceToSqr(this);
			if (distance < bestDistance)
			{
				bestDistance = distance;
				best = player;
			}
		}
		return best;
	}

	/** 原版 check(EntityPlayer)：既定では誰でも拾える（EntityTHItem はスニーク中のみ）。 */
	protected boolean canBePickedUp(Player player)
	{
		return true;
	}

	/** 原版 moveToPlayer(EntityPlayer)。 */
	protected void moveToPlayer(Player player)
	{
		if (this.speed < this.limitSpeed)
		{
			this.speed += 0.1D;
		}
		else
		{
			this.speed = this.limitSpeed;
		}

		Vec3 toTarget = THShotLib.angle_ToPos(this.position(), THShotLib.pos_Living(player));
		Vec3 motion = this.getDeltaMovement();
		motion = motion.add(toTarget.scale(this.limitSpeed));
		this.setDeltaMovement(motion);
		this.setPos(this.getX() + motion.x, this.getY() + motion.y, this.getZ() + motion.z);

		// 原版: rotationPitch をプレイヤー方向へ（描画側は使わないので同期のみ）
		float pitch = (float)Math.atan2(
				player.getY() + player.getEyeHeight() - this.getY(),
				Math.sqrt((player.getX() - this.getX()) * (player.getX() - this.getX())
						+ (player.getZ() - this.getZ()) * (player.getZ() - this.getZ()))) / (float)Math.PI * 180F;
		this.setXRot(pitch);
	}

	@Override
	public void playerTouch(Player player)
	{
		if (this.level().isClientSide)
		{
			return;
		}
		if (this.target != null && this.target != player)
		{
			return;
		}
		this.onPickup(player);
		this.discard();
	}

	/** 原版 onCollideWithPlayer：既定では弾の素を渡す。 */
	protected void onPickup(Player player)
	{
		if (this.giveItem(player, new ItemStack(THKaguyaItems.SHOT_MATERIAL.get())))
		{
			this.playPickupSound(player);
		}
	}

	protected boolean giveItem(Player player, ItemStack stack)
	{
		return player.getInventory().add(stack);
	}

	protected void playPickupSound(Player player)
	{
		player.playSound(SoundEvents.ITEM_PICKUP, 0.2F,
				((this.random.nextFloat() - this.random.nextFloat()) * 0.7F + 1.0F) * 0.3F * 4.0F);
		player.take(this, 1);
	}
}
