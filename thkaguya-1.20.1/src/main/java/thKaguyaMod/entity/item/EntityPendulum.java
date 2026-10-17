package thKaguyaMod.entity.item;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import thKaguyaMod.THKaguyaLib;
import thKaguyaMod.init.THKaguyaEntities;
import thKaguyaMod.init.THKaguyaItems;

/** ナズーリンのペンデュラム
 *	指定したブロックが近くにあるか探す
 */ 
public class EntityPendulum extends Entity
{
	private static final EntityDataAccessor<Integer> SEARCH_ANGLE =
			SynchedEntityData.defineId(EntityPendulum.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Integer> SEARCH_COUNT =
			SynchedEntityData.defineId(EntityPendulum.class, EntityDataSerializers.INT);

	/** 使用者 */
	public Player user;
	private int mode;//ペンデュラムのモード
	private int searchBlockID;//探しているブロックのID
	private int searchCount;//探索したブロックの数
	private int searchLength;//探索する距離
	private int searchX;
	private int searchY;
	private int searchZ;
	private int searchMaxX;
	private int searchMaxY;
	private int searchMaxZ;
	private float pendulumRotation;
	private int count;

	private String searchBlockName;
	private ItemStack searchItem;

	public EntityPendulum(EntityType<?> type, Level level)
	{
		super(type, level);

		searchBlockID = 0;
		mode = 0;
		searchCount = 0;
		pendulumRotation = 0.0F;
	}

	public EntityPendulum(Level world)
	{
		this(THKaguyaEntities.PENDULUM.get(), world);
	}

	public EntityPendulum(Level world, Player player, int pmode, String name, ItemStack itemStack)
	{
		this(world);

		user = player;//使用者をshootingEntityに保存
		THKaguyaLib.itemEffectFollowUser(this, user, 1.2D, -30F, false, (double)user.getEyeHeight() - 0.5D);

		mode = pmode;
		if(mode == 1)
		{
			searchLength = 24;
		}
		else
		{
			searchLength = 12;
		}

		searchBlockName = name;
		searchItem = itemStack;
	}

	/** 生成時に一度だけ呼ばれる */
	@Override
	protected void defineSynchedData()
	{
		this.entityData.define(SEARCH_ANGLE, 0);
		this.entityData.define(SEARCH_COUNT, 0);
	}

	//押すことができるか
	@Override
	public boolean isPushable()
	{
		return false;
	}

	//ペンデュラムを終了した時の処理
	private void finish()
	{
		if(user instanceof Player)
		{
			Player entityPlayer = user;
			if(entityPlayer.isCreative())
			{
				discard();
				return;
			}
		}
		if(user != null)
		{
			if(!user.getInventory().add(new ItemStack(THKaguyaItems.NAZRIN_PENDULUM.get(), 1)))
			{
				//インベントリが一杯なら使用者の目の前でアイテム化
				if(!level().isClientSide)
				{
					user.drop(new ItemStack(THKaguyaItems.NAZRIN_PENDULUM.get(), 1), false);
				}
			}
		}
		else//使用者がいない場合（再起動した際など）にはその場にアイテム化
		{
			if(!level().isClientSide)
			{
				spawnAtLocation(new ItemStack(THKaguyaItems.NAZRIN_PENDULUM.get(), 1));
			}
		}
		discard();
	}

	/**
	 * 毎tick行う処理
	 */
	@Override
	public void tick()
	{
		super.tick();

		if(!level().isClientSide && user == null)
		{
			finish();
		}

		if(!level().isClientSide && user != null)
		{
			if(tickCount > 10 && user.isShiftKeyDown())
			{
				finish();
			}
			if(user.hurtTime > 0)
			{
				finish();
			}
		}

		//サーチの処理
		if(count % 60 == 0 && !level().isClientSide)
		{
			searchCount = 0;
			searchX = (int)getX() - searchLength;
			searchZ = (int)getZ() - searchLength;
			searchY = (int)getY() - searchLength;
			searchMaxX = searchX + searchLength * 2 + 1;
			searchMaxY = searchY + searchLength * 2 + 1;
			searchMaxZ = searchZ + searchLength * 2 + 1;

			String searchName;

			for(int si = searchX; si < searchMaxX; si++)
			{
				for(int sk = searchZ; sk < searchMaxZ; sk++)
				{
					for(int sj = searchY; sj < searchMaxY; sj++)
					{
						// 原版: StatCollector.translateToLocal(getUnlocalizedName() + ".name")。
						// 1.20.1 的 getDescriptionId() 已是 "block.minecraft.xxx"，再拼 ".name" 会得到不存在的键，
						// 导致 searchCount 恒为 0（灵摆不旋转）。改用 Block#getName()。
						searchName = level().getBlockState(new net.minecraft.core.BlockPos(si, sj, sk)).getBlock().getName().getString();
						if(searchName.equals(searchBlockName))
						{
							searchCount++;
						}
					}
				}
			}
			if(searchCount > 32)
			{
				searchCount = 32;
			}
			setSearchCount(searchCount);
		}

		if(getSearchCount() != 0)
		{
			setSearchAngle( getSearchAngle() +  (float)getSearchCount() / 3.0F);
			setYRot(getYRot() + 3.0F);
		}
		if(user != null)
		{
			THKaguyaLib.itemEffectFollowUser(this, user, 1.2D, -30F, false, (double)user.getEyeHeight() - 0.5D);
		}
		count ++;
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

	//サーチによる回転角度を設定する
	public void setSearchAngle(float angle)
	{
		this.entityData.set(SEARCH_ANGLE, (int)(angle * 100.0F));
	}

	//サーチによる回転角度を返す
	public float getSearchAngle()
	{
		return (float)this.entityData.get(SEARCH_ANGLE) / 100.0F;
	}

	//サーチで引っかかったブロックの数を設定する
	public void setSearchCount(int count)
	{
		this.entityData.set(SEARCH_COUNT, count);
	}

	//サーチで引っかかったブロックの数を返す
	public int getSearchCount()
	{
		return this.entityData.get(SEARCH_COUNT);
	}
}
