package thKaguyaMod.entity.item;

import java.util.List;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import thKaguyaMod.THKaguyaLib;
import thKaguyaMod.THShotLib;
import thKaguyaMod.entity.shot.EntityTHShot;
import thKaguyaMod.init.THKaguyaEntities;

/** 霊撃札 
 *  霊撃により相手を吹き飛ばし、弾も消し飛ばす
 */
public class EntitySpiritualStrikeTalisman extends Entity
{
	/** 使用者 */
	public LivingEntity userEntity;

	private int count;

	public EntitySpiritualStrikeTalisman(EntityType<?> type, Level level)
	{
		super(type, level);
		noCulling = true;//中心が画面から外れても描画される
	}

	public EntitySpiritualStrikeTalisman(Level world)
	{
		this(THKaguyaEntities.SPIRITUAL_STRIKE_TALISMAN.get(), world);
	}

	public EntitySpiritualStrikeTalisman(Level world, LivingEntity living)
	{
		this(world);

		userEntity = living;//使用者をuserEntityに保存
		THKaguyaLib.itemEffectFollowUser(this, userEntity, 2.0D, 30F);

	}

	/** 生成時に一度だけ呼ばれる処理 */
	@Override
	protected void defineSynchedData()
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

	/** 毎tick呼ばれる処理 */
	@Override
	public void tick()
	{
		super.tick();

		//10ticksで消滅
		if(tickCount > 3)
		{
			if(!level().isClientSide)
			{
				discard();
			}
		}

		//***************************//
		Entity entity = null;
		double effectiveBoundary = (double)tickCount * 4.0D;//有効範囲
		//効果範囲内のEntityを全て取得
		List<Entity> list = level().getEntities(this, getBoundingBox().inflate(effectiveBoundary, effectiveBoundary, effectiveBoundary));

		if (list != null && !list.isEmpty())//取得したリストにEntityがいるなら
		{
			for (int j1 = 0; j1 < list.size(); j1++)//取得したEntityリストの最初から最後まで見る
			{
				entity = list.get(j1);//entityに取得したリストのEntityを代入
				//entityが存在し、それが使用者でないなら
				if (entity != null && entity != userEntity && entity.getFirstPassenger() != userEntity)
				{
					double distance = entity.distanceTo(this);
					if(distance <= effectiveBoundary)
					{
						Vec3 angle = THShotLib.angle_ToPos(THShotLib.pos_Entity(this),
														THShotLib.pos(entity.getX(),  entity.getY(),  entity.getZ()));
						double effectivePower = (effectiveBoundary - distance) * 0.1D;

						if(entity instanceof LivingEntity)
						{
							if(!(entity instanceof Animal) && !(entity instanceof Villager))
							{
								entity.setDeltaMovement(
										angle.x * effectivePower,
										(/*angle.y +*/ 0.5D) * effectivePower,
										angle.z * effectivePower);
							}
						}
						else if(entity instanceof EntityTHShot)
						{
							EntityTHShot shot = (EntityTHShot)entity;

							// TODO(阶段4): shot.shotFinishBonus()（弾消 → 点アイテム変換）依赖 EntityShotMaterial，未迁移
							shot.delete();
						}
					}
				}
			}
		}

		//************************************************************************************//

	}

	/**
	 * 保存するデータの書き込み
	 * @param tag : NTBタグ
	 */
	@Override
	protected void addAdditionalSaveData(CompoundTag tag)
	{
		tag.putShort("count", (short)count);
		//tag.putBoolean("isSpellCard", isSpellCard);
	}

	/**
	 * 保存したデータの読み込み
	 * @param tag : NBTタグ
	 */
	@Override
	protected void readAdditionalSaveData(CompoundTag tag)
	{
		count = tag.getShort("count");
		//isSpellCard = tag.getBoolean("isSpellCard");
	}

	@Override
	public float getShadowRadius()
	{
		return 0.5F;
	}

}
