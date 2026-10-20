package thKaguyaMod.entity.item;
import net.minecraft.network.syncher.SynchedEntityData;

import net.minecraft.core.HolderLookup;
import java.util.List;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.decoration.Painting;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Ghast;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import thKaguyaMod.THKaguyaLib;
import thKaguyaMod.entity.spellcard.EntitySpellCard;
import thKaguyaMod.init.THKaguyaEntities;
import thKaguyaMod.init.THKaguyaItems;

/** 僅かな間、時間を停止させる空間 */
public class EntitySakuyaStopWatch extends Entity
{
	/** 使用者 */
	public LivingEntity user;
	private int count;
	private float watchRotation;

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
		THKaguyaLib.itemEffectFollowUser(this, user, 1.2D, -30F);
	}

	/** 生成時に一度だけ呼ばれる処理 */
	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder)
	{
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

		// ***********停止空間（時計じゃなくてもっと大きな見えない壁）とEntityの当たり判定を取る**************** //
		int playerCount = 0;//停止空間内のプレイヤーの数を数える　誰もいなければ消滅させる
		Entity entity = null;
		boolean dubbleWatch = false;
		boolean pass = false;
		double effectiveBoundary = 40.0D;//停止空間の有効範囲
		// 停止空間内のEntityを全て取得
		List<Entity> list = level().getEntities(this, getBoundingBox().inflate(effectiveBoundary, effectiveBoundary, effectiveBoundary));

		// 取得したリストにEntityがいる場合
		if (list != null && !list.isEmpty())
		{
			// 取得したEntityリストを全て見る
			for ( int j1 = 0; j1 < list.size(); j1++ )
			{
				entity = list.get(j1);//entityに取得したリストのEntityを代入

				// Entityがプレイヤーの場合
				if( entity instanceof Player )
				{
					playerCount++;//範囲内のプレイヤー数カウントを増加
				}

				//entityが存在し、それが使用者でない場合
				if (	entity != null && entity != user && entity.getFirstPassenger() != user
					&& !(entity instanceof ItemFrame)
					&& !(entity instanceof Painting))
				{
					pass = false;
					//時計が同時に２つ出現しているときの処理。全ての時計を無効化しアイテムに戻す。
					if( entity instanceof EntitySakuyaWatch )
					{
						EntitySakuyaWatch watch = (EntitySakuyaWatch)entity;
						//アイテム化させる
						THKaguyaLib.itemEffectFinish(watch, watch.userEntity, THKaguyaItems.SAKUYA_WATCH.get());
						dubbleWatch = true;
					}
					if( entity instanceof EntitySakuyaStopWatch )
					{
						EntitySakuyaStopWatch watch = (EntitySakuyaStopWatch)entity;
						//アイテム化させる
						if( !watch.level().isClientSide )
						{
							watch.discard();
						}
						dubbleWatch = true;
					}
					if(entity instanceof EntitySpellCard)
					{
						EntitySpellCard spellCard = (EntitySpellCard)entity;
						if(spellCard.canMoveInTimeStop && spellCard.user.equals(this.user))
						{
							pass = true;
							spellCard.specialProcessInTimeStop();
						}
					}
					if(!pass)
					{
						inPrivateSquare(entity);//時間操作空間の処理
					}
				}
			}
		}

		// 範囲内にプレイヤーが一人もいない場合
		if( playerCount == 0 )
		{
			// 消滅させる
			if( !level().isClientSide )
			{
				discard();
			}
			return;
		}

		// 時計が２つ以上出現している場合
		if( dubbleWatch )
		{
			// 消滅させる
			if( !level().isClientSide )
			{
				discard();
			}
			return;
		}

		// ************************************************************************************ //

		count ++;

		if(count % 20 == 19)//20フレーム周期で音を出す
		{
			level().playSound(null, blockPosition(), SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.5F, 6.0F);
			level().playSound(null, blockPosition(), SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.PLAYERS, 0.5F, 4.0F);
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



	// 時間操作空間に入っていた時の処理
	protected void inPrivateSquare( Entity hitEntity )
	{
		//Entityが誕生してから２フレーム経過したもののみ判定
		if( hitEntity.tickCount >= 2 )
		{
			//====時間停止空間処理====//

			//Entityを１フレーム前の状態に、できる限りもっていく処理
			hitEntity.setPos( hitEntity.xo, hitEntity.yo, hitEntity.zo);

			hitEntity.setYRot( hitEntity.yRotO );
			hitEntity.setXRot( hitEntity.xRotO );
			hitEntity.setDeltaMovement( 0.0D, hitEntity.getDeltaMovement().y, 0.0D );

			hitEntity.setAirSupply(0);

			hitEntity.tickCount--;//誕生してからのカウントを増やさない

			hitEntity.fallDistance -= 0.076865F;//落下距離を変動しないように

			// 範囲内のEntityが生物の場合
			if( hitEntity instanceof LivingEntity )
			{
				LivingEntity living = (LivingEntity)hitEntity;
				living.yHeadRot = living.yHeadRotO;

				// living.attackTime++; // TODO(阶段4): 1.20 无 attackTime 字段，时停冻结动画语义不同

				// クリーパーに属している場合
				if( living instanceof Creeper )
				{
					Creeper entityCreeper = (Creeper)living;

					entityCreeper.setSwellDir(-1);//爆発のカウントを戻す

				}
				// ガストに属している場合
				else if( living instanceof Ghast )
				{
					// TODO(阶段7): 原版 entityGhast.attackCounter--（1.20.1 Ghast 无 attackCounter 字段）
				}

				// テイム可能な場合
				if( living instanceof TamableAnimal )
				{
					living.setDeltaMovement(living.getDeltaMovement().x, living.getDeltaMovement().y - 0.000001D, living.getDeltaMovement().z);//座っている動物は重力がなくなると立つみたい
				}

				// プレイヤーの場合
				if( living instanceof ServerPlayer )
				{
					ServerPlayer player = (ServerPlayer)living;
					player.connection.teleport(player.xo, player.yo, player.zo, player.getYRot(), player.getXRot());
				}
			}
		}
	}

}
