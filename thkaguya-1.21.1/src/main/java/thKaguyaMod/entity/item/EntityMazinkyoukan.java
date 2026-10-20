package thKaguyaMod.entity.item;
import net.minecraft.network.syncher.SynchedEntityData;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import thKaguyaMod.THKaguyaLib;
import thKaguyaMod.init.THKaguyaEntities;

/** 魔人経巻 */
public class EntityMazinkyoukan extends Entity
{
	/** 使用者 */
	public Player user;
	public int count;
	public float turnAngle;
	public int befUseCount;

	/** ワールド読み込み時に呼び出されるコンストラクト */
	public EntityMazinkyoukan(EntityType<?> type, Level level)
	{
		super(type, level);
	}

	public EntityMazinkyoukan(Level world)
	{
		this(THKaguyaEntities.MAZINKYOUKAN.get(), world);
	}

	/** 出現時に呼び出されるコンストラクト */
	public EntityMazinkyoukan(Level world, Player player)
	{
		this(world);
		user = player;
		xo = player.getX();
		yo = player.getY();
		zo = player.getZ();
		setPos(player.getX(), player.getY() + player.getEyeHeight(), player.getZ());//初期位置を設定(x,y,z)
		setYRot(player.getYRot());
		setXRot(0.0F);
		count = 0;
		befUseCount = 0;
		turnAngle = 0F;
	}

	/** 生成時に一度だけ呼ばれる処理 */
	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder)
	{
	}

	//当たり判定の有無　falseだと右クリックの選択ですらできない
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
		//使用者がいないなら消す
		if(!level().isClientSide && (user == null || user.isRemoved()))
		{
			discard();
			return;
		}
		super.tick();

		//使用者が右クリックをやめたら消す
		if(user != null)
		{
			if(!user.isUsingItem())
			{
				int time = count;
				time /= 3;
				{
					if(!level().isClientSide)
					{
						int removeLevel = (int)((float)time / 8.0F) + 1;

						if( (user.experienceLevel - removeLevel) < 0 )
						{
							time += (user.experienceLevel - removeLevel) * 8F;
						}
						user.giveExperienceLevels( -removeLevel );
						user.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, time * 10, 4));//スピードアップ
						user.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, time * 10, 0));//攻撃力アップ
						user.addEffect(new MobEffectInstance(MobEffects.JUMP, time * 10, 1));//跳躍力アップ
						user.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, time * 10, 0));//防御力アップ
						//entityPlayer.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 15 * 20, 1));//炎耐性
					}
				}
				if(!level().isClientSide)
				{
					discard();
				}
			}
			else
			{
				THKaguyaLib.itemEffectFollowUser(this, user, 0.5D, 0F);
				setYRot(user.getYRot());
				setXRot(0F);
			}
		}

		count++;
		if(count > 114)
		{
			count = 114;
		}
		else if(count > 50)
		{
			turnAngle += 2.88F;
		}

		if(getYRot() >  180F)setYRot(getYRot() - 360F);
		if(getYRot() < -180F)setYRot(getYRot() + 360F);
		if(getXRot() >  180F)setXRot(getXRot() - 360F);
		if(getXRot() < -180F)setXRot(getXRot() + 360F);
	}

	/**
	 * 保存するデータの書き込み
	 * @param tag : NTBタグ
	 */
	@Override
	protected void addAdditionalSaveData(CompoundTag tag)
	{
	}

	/**
	 * 保存したデータの読み込み
	 * @param tag : NBTタグ
	 */
	@Override
	protected void readAdditionalSaveData(CompoundTag tag)
	{
	}
}
