package thKaguyaMod.entity.item;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.ClipContext;
import thKaguyaMod.init.THKaguyaItems;

/**
 * パワーアップ／得点／スペルカード／エクステンドの飛び道具アイテム
 * （原版 {@code thKaguyaMod.entity.EntityTHItem} の 1.20.1 移植）。
 *
 * <p>原版 {@code EntityTHItem#onUpdate} の流れ:
 * <ol>
 *   <li>クライアントは即 return（同期された位置だけで描画）。</li>
 *   <li>16 tick 経過するまでは {@code super.onUpdate()} を呼ばない＝
 *       自前の減衰＋重力＋反射だけで飛ぶ。</li>
 *   <li>16 tick 以降は {@link EntityShotMaterial#tick()} が動く。ただし
 *       {@code target} が決まった（スニーク中のプレイヤーへ吸い寄せ）以降は自前運動を止める。</li>
 * </ol>
 *
 * <p>なお原版のブロック反射は {@code switch(sideHit)} の fall-through で
 * 同じ軸を二度反転させてしまうバグがあり、実際には反射せずに突き抜けていた。
 * ここでは意図どおり「当たった面の軸だけ反転」で実装している。
 */
public class EntityTHItem extends EntityShotMaterial
{
	public static final byte ITEM_ID_SHOTMATERIAL =  0;
	public static final byte ITEM_ID_SMALLPOWER   =  1;
	public static final byte ITEM_ID_BIGPOWER     =  2;
	public static final byte ITEM_ID_POINT        =  4;
	public static final byte ITEM_ID_BOMB         = 10;
	public static final byte ITEM_ID_EXTEND       = 11;

	private static final EntityDataAccessor<Byte> DATA_ITEM_TYPE =
			SynchedEntityData.defineId(EntityTHItem.class, EntityDataSerializers.BYTE);

	public EntityTHItem(EntityType<?> type, Level level)
	{
		super(type, level);
		this.limitSpeed = 0.5D;
	}

	public EntityTHItem(EntityType<?> type, Level level, double posX, double posY, double posZ, Vec3 angle, byte itemId)
	{
		super(type, level, posX, posY, posZ);
		this.setItemType(itemId);
		this.limitSpeed = 0.5D;
		Vec3 motion = angle.scale(this.limitSpeed);
		this.setDeltaMovement(motion);
		this.lastMotionX = motion.x;
		this.lastMotionY = motion.y;
		this.lastMotionZ = motion.z;
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder)
	{
		super.defineSynchedData(builder);
		builder.define(DATA_ITEM_TYPE, (byte)0);
	}

	public void setItemType(byte id)
	{
		this.itemId = id;
		this.entityData.set(DATA_ITEM_TYPE, id);
	}

	public byte getItemType()
	{
		return this.entityData.get(DATA_ITEM_TYPE);
	}

	private byte itemId;

	@Override
	public void addAdditionalSaveData(CompoundTag tag)
	{
		super.addAdditionalSaveData(tag);
		tag.putByte("ItemType", this.getItemType());
	}

	@Override
	public void readAdditionalSaveData(CompoundTag tag)
	{
		super.readAdditionalSaveData(tag);
		this.setItemType(tag.getByte("ItemType"));
	}

	@Override
	public void tick()
	{
		// 原版: if (worldObj.isRemote) return;
		if (this.level().isClientSide)
		{
			return;
		}

		if (this.tickCount > 16)
		{
			super.tick();
			if (this.target != null)
			{
				return;
			}
		}

		// 原版の飛翔処理：水平 0.87 減衰、下方向 -0.2 で打ち止め
		Vec3 motion = this.getDeltaMovement();
		double motionX = motion.x;
		double motionY = motion.y;
		double motionZ = motion.z;

		if (Math.abs(motionX) < 0.01D)
		{
			motionX = 0.0D;
		}
		else
		{
			motionX *= 0.87D;
		}

		if (Math.abs(motionZ) < 0.01D)
		{
			motionZ = 0.0D;
		}
		else
		{
			motionZ *= 0.87D;
		}

		if (motionY > -0.2D)
		{
			motionY -= 0.03D;
		}
		if (motionY < -0.2D)
		{
			motionY = -0.2D;
		}

		Vec3 from = this.position();
		Vec3 to = from.add(motionX, motionY, motionZ);
		BlockHitResult hit = this.level().clip(new ClipContext(from, to,
				ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));

		if (hit.getType() != HitResult.Type.MISS)
		{
			switch (hit.getDirection())
			{
				case DOWN:
					motionY = 0.0D;
					to = new Vec3(to.x, hit.getLocation().y, to.z);
					break;
				case UP:
					// 原版: 天井に当たったら消滅
					this.discard();
					return;
				case NORTH:
				case SOUTH:
					motionZ = -motionZ;
					to = new Vec3(to.x, to.y, hit.getLocation().z);
					break;
				case WEST:
				case EAST:
					motionX = -motionX;
					to = new Vec3(hit.getLocation().x, to.y, to.z);
					break;
			}
		}

		this.setDeltaMovement(motionX, motionY, motionZ);
		this.setPos(to.x, to.y, to.z);

		this.lastMotionX = motionX;
		this.lastMotionY = motionY;
		this.lastMotionZ = motionZ;

		this.checkPlayerTouch();
	}

	/** 原版は当たり判定内のプレイヤーを直接拾っていたので、同じ処理を自前で回す。 */
	private void checkPlayerTouch()
	{
		for (Player player : this.level().getEntitiesOfClass(Player.class, this.getBoundingBox().inflate(0.3D)))
		{
			if (player.isAlive() && !player.isSpectator())
			{
				this.playerTouch(player);
				return;
			}
		}
	}

	/** 原版 check(EntityPlayer) = player.isSneaking()。 */
	@Override
	protected boolean canBePickedUp(Player player)
	{
		return player.isShiftKeyDown();
	}

	@Override
	protected void moveToPlayer(Player player)
	{
		if (player != null)
		{
			this.limitSpeed = 0.3D;
			super.moveToPlayer(player);
		}
	}

	@Override
	protected void onPickup(Player player)
	{
		boolean itemGet = false;
		ItemStack stack = ItemStack.EMPTY;

		switch (this.getItemType())
		{
			case ITEM_ID_SMALLPOWER:
				stack = new ItemStack(THKaguyaItems.POWER_UP_ITEM_SMALL.get());
				break;
			case ITEM_ID_BIGPOWER:
				stack = new ItemStack(THKaguyaItems.POWER_UP_ITEM_BIG.get());
				break;
			case ITEM_ID_POINT:
				stack = new ItemStack(THKaguyaItems.POINT_ITEM.get());
				break;
			case ITEM_ID_BOMB:
				stack = new ItemStack(THKaguyaItems.BOMB_ITEM.get());
				break;
			case ITEM_ID_EXTEND:
				stack = new ItemStack(THKaguyaItems.EXTEND_ITEM.get());
				break;
			default:
				return;
		}

		itemGet = player.getInventory().add(stack);
		if (!itemGet)
		{
			// 原版: バッグが満杯なら足元にドロップ
			player.drop(stack, false);
		}
		else
		{
			player.playSound(SoundEvents.ITEM_PICKUP, 0.2F,
					((this.random.nextFloat() - this.random.nextFloat()) * 0.7F + 1.0F) * 0.3F * 4.0F);
			player.take(this, 1);
		}
	}
}
