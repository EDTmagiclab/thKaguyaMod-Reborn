package thKaguyaMod.entity.item;

import java.util.List;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.decoration.Painting;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import thKaguyaMod.THKaguyaLib;
import thKaguyaMod.entity.living.EntityDanmakuMob;
import thKaguyaMod.init.THKaguyaEntities;
import thKaguyaMod.init.THKaguyaItems;

/** 呪いのデコイ人形のEntity */
public class EntityCursedDecoyDoll extends EntityDanmakuMob
{
	/** 使用者 */
	public LivingEntity userEntity;

	public EntityCursedDecoyDoll(EntityType<? extends EntityCursedDecoyDoll> type, Level level)
	{
		super(type, level);

		this.xpReward = 1;//経験値の量

		this.danmakuMobName = "";
		this.setSpecies(this.SPECIES_SHIKIGAMI_DOLL);

		this.setDanmakuPattern(NOT_ATTACK);
		this.setMaxHP(40.0F);
		this.setHealth(40.0F);
		this.setSpeed(0.0F);
		this.setAttackDistance(0.0D);
		this.setDetectionDistance(0.0D);
		this.setFlyingHeight(0);
		this.isFlyingMode = false;

		this.isSpellCardMode = false;
		this.attackInterval = 0;
	}

	public EntityCursedDecoyDoll(Level world)
	{
		this(THKaguyaEntities.CURSED_DECOY_DOLL.get(), world);
	}

	public EntityCursedDecoyDoll(Level world, LivingEntity living)
	{
		this(world);

		userEntity = living;//使用者をuserEntityに保存
		this.setPos(living.getX(), living.getY(), living.getZ());

	}

	//周りの妖精を呼び出すことができるか
	@Override
	protected boolean canFairyCall()
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

		//30ticksを超えているなら
		if(tickCount > 180)
		{
			//アイテム化させる
			if(!level().isClientSide)
			{
				discard();
			}
		}


		int playerCount = 0;//停止空間内のプレイヤーの数を数える　誰もいなければ消滅させる

		//***********停止空間（時計じゃなくてもっと大きな見えない壁）とEntityの当たり判定を取る****************//
		Entity entity = null;
		boolean dubbleWatch = false;
		boolean pass = false;
		double effectiveBoundary = 40.0D;//停止空間の有効範囲
		//停止空間内のEntityを全て取得
		List<Entity> list = level().getEntities(this, getBoundingBox().inflate(effectiveBoundary, effectiveBoundary, effectiveBoundary));

		if (list != null && !list.isEmpty())//取得したリストにEntityがいるなら
		{
			for (int j1 = 0; j1 < list.size(); j1++)//取得したEntityリストの最初から最後まで見る
			{
				entity = list.get(j1);//entityに取得したリストのEntityを代入
				if(entity instanceof Player)//プレイヤーなら
				{
					playerCount++;//範囲内のプレイヤー数カウントを増加
				}
				//entityが存在し、それが使用者でないなら
				if (entity != null && entity != userEntity && entity.getFirstPassenger() != userEntity
						&& !(entity instanceof ItemFrame)
						&& !(entity instanceof Painting))
				{
					pass = false;
					//時計が同時に２つ出現しているときの処理。全ての時計を無効化しアイテムに戻す。
					if(entity instanceof EntityCursedDecoyDoll)
					{
						EntityCursedDecoyDoll decoy = (EntityCursedDecoyDoll)entity;
						//アイテム化させる
						THKaguyaLib.itemEffectFinish(decoy, decoy.userEntity, THKaguyaItems.CURSED_DECOY_DOLL.get());
						dubbleWatch = true;
					}
					if(!pass)
					{
						inDecoyEffectedArea(entity);//呪いのデコイ人形の有効範囲に入っていた場合の処理
					}
				}
			}
		}

		if(playerCount == 0)//範囲内にプレイヤーが一人もいなかったなら
		{
			if(!level().isClientSide)
			{
				THKaguyaLib.itemEffectFinish(this, userEntity, THKaguyaItems.CURSED_DECOY_DOLL.get());//終了処理に移る
			}
		}

		if(dubbleWatch)
		{
			//アイテム化させる
			THKaguyaLib.itemEffectFinish(this, userEntity, THKaguyaItems.CURSED_DECOY_DOLL.get());
		}

		//************************************************************************************//

		this.setYRot(this.yRotO);
		this.setXRot(this.xRotO);

	}


	/**
	 *  呪いのデコイ人形の有効範囲に入っていたときの処理
	 */
	public void inDecoyEffectedArea(Entity hitEntity)
	{
		if(hitEntity instanceof PathfinderMob)
		{
			PathfinderMob effectedCreature = (PathfinderMob)hitEntity;

			if( effectedCreature.getTarget() != null )
			{
				effectedCreature.setTarget(this);
			}
		}
		if(hitEntity instanceof LivingEntity)
		{
			LivingEntity effectedLiving = (LivingEntity)hitEntity;

			effectedLiving.setLastHurtByMob(this);
		}

	}

	//生きてるときに出す音
	@Override
	protected net.minecraft.sounds.SoundEvent getAmbientSound()
	{
		return null;
	}

	//攻撃を受けたときの音
	@Override
	protected net.minecraft.sounds.SoundEvent getHurtSound(net.minecraft.world.damagesource.DamageSource source)
	{
		return null;
	}

	//倒れたときの音
	@Override
	protected net.minecraft.sounds.SoundEvent getDeathSound()
	{
		return null;
	}

}
