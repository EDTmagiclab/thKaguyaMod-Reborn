package thKaguyaMod.entity.item;

import java.util.List;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.decoration.Painting;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import thKaguyaMod.THKaguyaLib;
import thKaguyaMod.entity.ITimeStopSource;
import thKaguyaMod.entity.TimeStopManager;
import thKaguyaMod.entity.spellcard.EntitySpellCard;
import thKaguyaMod.init.THKaguyaConfig;
import thKaguyaMod.init.THKaguyaEntities;
import thKaguyaMod.init.THKaguyaItems;

/**
 * 僅かな間、時間を停止させる空間（ストップウォッチ用）。
 *
 * <p>2026-10-22：{@code EntitySakuyaWatch} と同じく、座標の巻き戻し（{@code inPrivateSquare}）を
 * 廃止し、{@link TimeStopManager} の tick 門控に委ねる。本クラスの寿命は 40 tick なので、
 * 消滅した時点で看門狗が時間停止を解除する。</p>
 */
public class EntitySakuyaStopWatch extends Entity implements ITimeStopSource
{
	/** 使用者 */
	public LivingEntity user;
	private int count;

	/** 術者の entity id（同期用。-1 = 不明） */
	private static final EntityDataAccessor<Integer> DATA_USER_ID =
			SynchedEntityData.defineId(EntitySakuyaStopWatch.class, EntityDataSerializers.INT);
	/** {@link TimeStopManager} の FLAG_* ビット */
	private static final EntityDataAccessor<Integer> DATA_FLAGS =
			SynchedEntityData.defineId(EntitySakuyaStopWatch.class, EntityDataSerializers.INT);
	/** 停止空間の最大半径 */
	private static final EntityDataAccessor<Integer> DATA_RADIUS =
			SynchedEntityData.defineId(EntitySakuyaStopWatch.class, EntityDataSerializers.INT);

	/** ストップウォッチのコンストラクタ（引数Worldのみは必須） */
	public EntitySakuyaStopWatch(EntityType<?> type, Level level)
	{
		super(type, level);

		noCulling = true;		// 中心が画面から外れても描画される
	}

	public EntitySakuyaStopWatch(Level world)
	{
		this(THKaguyaEntities.SAKUYA_STOP_WATCH.get(), world);
	}

	/** ストップウォッチのコンストラクタ */
	public EntitySakuyaStopWatch(Level world, LivingEntity userLiving)
	{
		this(world);

		user = userLiving;
		this.entityData.set(DATA_USER_ID, userLiving == null ? -1 : userLiving.getId());
		THKaguyaLib.itemEffectFollowUser(this, user, 1.2D, -30F);
	}

	/** 生成時に一度だけ呼ばれる処理 */
	@Override
	protected void defineSynchedData()
	{
		this.entityData.define(DATA_USER_ID, -1);
		this.entityData.define(DATA_FLAGS, 0);
		this.entityData.define(DATA_RADIUS, 32);
	}

	@Override
	public boolean isTimeStopActive()
	{
		return (this.entityData.get(DATA_FLAGS) & TimeStopManager.FLAG_ACTIVE) != 0;
	}

	@Override
	public int getTimeStopMode()
	{
		// ストップウォッチは常に「時間停止」（減速モードは無い）
		return EntitySakuyaWatch.TIME_STOP;
	}

	@Override
	public int getTimeStopOwnerId()
	{
		return this.entityData.get(DATA_USER_ID);
	}

	@Override
	public int getTimeStopRadius()
	{
		return this.entityData.get(DATA_RADIUS);
	}

	@Override
	public boolean isTimeStopFreezeAll()
	{
		return (this.entityData.get(DATA_FLAGS) & TimeStopManager.FLAG_FREEZE_ALL) != 0;
	}

	/** 実際に時間操作を開始できたときに呼ぶ（サーバ専用） */
	public void markTimeStopActive(int radius, boolean freezeAll)
	{
		int flags = TimeStopManager.FLAG_ACTIVE;
		if(freezeAll)
		{
			flags |= TimeStopManager.FLAG_FREEZE_ALL;
		}
		this.entityData.set(DATA_FLAGS, flags);
		this.entityData.set(DATA_RADIUS, radius);
	}

	/**
	 * サーバ側で時間操作を開始する（{@link EntitySakuyaWatch#startTimeStop} と同じ入口）。
	 * ストップウォッチは常に時間停止なので、プレイヤー凍結は行わない。
	 */
	public boolean startTimeStop(boolean force)
	{
		if(level().isClientSide || !(level() instanceof ServerLevel serverLevel))
		{
			return false;
		}
		if(!force && !THKaguyaConfig.enableTimeStop)
		{
			return false;
		}
		if(!TimeStopManager.start(serverLevel, user, this, EntitySakuyaWatch.TIME_STOP, false))
		{
			return false;
		}
		markTimeStopActive(THKaguyaConfig.timeStopRadius,
				!THKaguyaConfig.timeStopHostileOnly);
		return true;
	}

	/**
	 * 押すことができるか
	 */
	@Override
	public boolean isPushable()
	{
		return false;
	}

	/**
	 * 他のEntityと衝突するか 右クリックできるかもこれで判定
	 */
	@Override
	public boolean isPickable()
	{
		return false;
	}

	/**
	 * 毎tick行う処理
	 */
	@Override
	public void tick()
	{
		super.tick();

		// 40ticksを超えている場合
		if( tickCount > 40 )
		{
			// 消滅させる
			if( !level().isClientSide )
			{
				discard();
			}
			return;
		}

		// 使用者がいない場合
		if( user == null )
		{
			// 消滅させる
			if( !level().isClientSide )
			{
				discard();
			}
			return;
		}
		// 使用者がいる場合
		else
		{
			// 使用者がダメージを受けた場合
			if( user.hurtTime > 0 )
			{
				// 消滅させる
				if(!level().isClientSide)
				{
						discard();
				}
				return;
			}
		}

		// ストップウォッチを使用者に追尾させる
		THKaguyaLib.itemEffectFollowUser(this, user, 1.2D, -30F);

		// ***********停止空間内の後始末（サーバ専用）**************** //
		if(!level().isClientSide)
		{
			int playerCount = 0;//停止空間内のプレイヤーの数を数える　誰もいなければ消滅させる
			boolean dubbleWatch = false;
			// 停止空間内のEntityを全て取得
			List<Entity> list = level().getEntities(this, getBoundingBox().inflate(40.0D, 40.0D, 40.0D));

			for(Entity entity : list)
			{
				// Entityがプレイヤーの場合
				if(entity instanceof Player)
				{
					playerCount++;//範囲内のプレイヤー数カウントを増加
				}

				//entityが存在し、それが使用者でない場合
				if(entity != user && !entity.hasPassenger(user)
					&& !(entity instanceof ItemFrame)
					&& !(entity instanceof Painting))
				{
					//時計が同時に２つ出現しているときの処理。全ての時計を無効化しアイテムに戻す。
					if(entity instanceof EntitySakuyaWatch watch)
					{
						THKaguyaLib.itemEffectFinish(watch, watch.userEntity, THKaguyaItems.SAKUYA_WATCH.get());
						dubbleWatch = true;
					}
					else if(entity instanceof EntitySakuyaStopWatch watch)
					{
						watch.discard();
						dubbleWatch = true;
					}
					//時間停止中でも動ける符卡だけは動かし続ける
					else if(entity instanceof EntitySpellCard spellCard
							&& spellCard.isTimeStopExempt()
							&& user != null && user.equals(spellCard.user))
					{
						spellCard.specialProcessInTimeStop();
					}
				}
			}

			// 範囲内にプレイヤーが一人もいない場合
			if( playerCount == 0 )
			{
				discard();
				return;
			}

			// 時計が２つ以上出現している場合
			if( dubbleWatch )
			{
				discard();
				return;
			}
		}

		// ************************************************************************************ //

		count ++;

		if(count % 20 == 19)//20フレーム周期で音を出す
		{
			level().playSound(null, blockPosition(), SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.5F, 6.0F);
			level().playSound(null, blockPosition(), SoundEvents.UI_BUTTON_CLICK.get(), SoundSource.PLAYERS, 0.5F, 4.0F);
		}
	}

	/**
	 * 保存するデータの書き込み
	 * @param tag : NTBタグ
	 */
	@Override
	protected void addAdditionalSaveData(CompoundTag tag)
	{
		tag.putShort("count", (short)count);
	}

	/**
	 * 保存したデータの読み込み
	 * @param tag : NBTタグ
	 */
	@Override
	protected void readAdditionalSaveData(CompoundTag tag)
	{
		count = tag.getShort("count");
	}
}
