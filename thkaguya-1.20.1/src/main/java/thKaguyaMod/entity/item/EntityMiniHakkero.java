package thKaguyaMod.entity.item;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.boss.EnderDragonPart;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import thKaguyaMod.DanmakuConstants;
import thKaguyaMod.LaserData;
import thKaguyaMod.THKaguyaCore;
import thKaguyaMod.THKaguyaLib;
import thKaguyaMod.THShotLib;
import thKaguyaMod.init.THKaguyaConfig;
import thKaguyaMod.init.THKaguyaEntities;
import thKaguyaMod.init.THKaguyaItems;

/** ミニ八卦炉（1.20.1 移植版）
 *
 * 1.7.10 -> 1.20.1 关键映射：
 *   "thkaguyamod:masterspark" -> ResourceLocation(MODID, "masterspark")（新 MODID = thkaguya）
 *   EntityDragonPart -> EnderDragonPart、EntityAnimal -> Animal、EntityVillager -> Villager
 *   getShadowSize/isValidLightLevel/getBrightnessForRender/getBrightness -> 渲染器负责，裁剪
 *   原版 onUpdate 内大段注释掉的旧マスタースパーク实现（EntityMasterSpark 直生成） -> 保持裁剪
 *   （原样为注释死代码，现由 THShotLib.createLaserB + EntityTHSetLaser 实现）
 */
public class EntityMiniHakkero extends Entity
{
	/** 使用者 */
	public LivingEntity user;
	private LivingEntity tgEntity;
	public int count;
	private float circleAngle;
	private int moveTexture;
	public int num;
	private int damage;
	private int lastTime;
	private boolean isSpellCard;

    public EntityMiniHakkero(EntityType<?> type, Level level)
    {
        super(type, level);
        // 原版：preventEntitySpawning / setSize(0.4F, 0.4F) / yOffset = 0.0F（注册与默认値担当）
    }

    public EntityMiniHakkero(Level level)
    {
        this(THKaguyaEntities.MINI_HAKKERO.get(), level);
    }

    public EntityMiniHakkero(Level level, LivingEntity entityLivingBase, int da)
    {
        this(level);

    	user = entityLivingBase;//使用者をuserに保存
        setPos(user.getX() - Math.sin(user.getYRot() / 180F * 3.141593F) * Math.cos(user.getXRot() / 180F * 3.141593F),
        		user.getY() - Math.sin(user.getXRot() / 180F * 3.141593F) + (double)user.getEyeHeight() - 0.10000000149011612D,
				user.getZ() + Math.cos(user.getYRot() / 180F * 3.141593F) * Math.cos(user.getXRot() / 180F * 3.141593F));//初期位置を設定(x,y,z)
    	setRot(user.getYRot(), user.getXRot());
    	tgEntity = null;
    	count = 0;
    	circleAngle = 0F;
    	moveTexture = 0;
    	damage = da;
    	isSpellCard = false;

    	level().playSound(null, blockPosition(),
    			SoundEvent.createVariableRangeEvent(new ResourceLocation(THKaguyaCore.MODID, "masterspark")),
    			SoundSource.PLAYERS, THKaguyaConfig.MasterSparkVol, 1.0F);
    	lastTime = 0;

    	int special = DanmakuConstants.EXPLOSION01;
    	Vec3 look = THShotLib.getVecFromAngle(getYRot(), getXRot());
    	THShotLib.createLaserB(user, this, THShotLib.pos_Entity(this), look,
    			THShotLib.rotate_Default(), 0.0F, 9999, LaserData.laser(24, 4.2F, 40.0F, 8.0F, 30, 120, special), this, 1.0D, 0.0F);
    }

    public EntityMiniHakkero(Level level, LivingEntity entityLivingBase, LivingEntity target)
    {
        this(level);

    	user = entityLivingBase;//使用者をuserに保存
        setPos(user.getX() - Math.sin(user.getYRot() / 180F * 3.141593F) * Math.cos(user.getXRot() / 180F * 3.141593F),
        		user.getY() - Math.sin(user.getXRot() / 180F * 3.141593F) + (double)user.getEyeHeight() - 0.10000000149011612D,
				user.getZ() + Math.cos(user.getYRot() / 180F * 3.141593F) * Math.cos(user.getXRot() / 180F * 3.141593F));//初期位置を設定(x,y,z)
    	setRot(user.getYRot(), user.getXRot());
    	tgEntity = target;
    	count = 0;
    	circleAngle = 0F;
    	moveTexture = 0;
    	damage = 1;
    	level().playSound(null, blockPosition(),
    			SoundEvent.createVariableRangeEvent(new ResourceLocation(THKaguyaCore.MODID, "masterspark")),
    			SoundSource.PLAYERS, THKaguyaConfig.MasterSparkVol, 1.0F);
    	lastTime = 0;

    	isSpellCard = true;

    	Vec3 look = THShotLib.getVecFromAngle(getYRot(), getXRot());
    	THShotLib.createLaserB(user, this, THShotLib.pos_Entity(this), look,
    			THShotLib.rotate_Default(), 0.0F, 9999, LaserData.laser(24, 4.2F, 60.0F, 8.0F, 30, 120, 0), this, 1.0D, 0.0F);
    }

	/** 生成時に一度だけ呼ばれる処理 */
    @Override
    protected void defineSynchedData()
    {
    }

    /** 押すことができるか？ */
    @Override
    public boolean isPushable()
    {
        return false;
    }

    //他のEntityと当たり判定を取るか
    @Override
    public boolean isPickable()
    {
    	return true;
    }

	//毎tick呼ばれる処理
	@Override
    public void tick()
    {
        super.tick();
    	if(!level().isClientSide && user == null)
    	{
    		if(!isSpellCard)
    		{
    			THKaguyaLib.itemEffectFinish(this, user, THKaguyaItems.MINI_HAKKERO.get());
    		}
    		else
    		{
    			if(!level().isClientSide)
    			{
    				discard();
    			}
    		}
    		return;
    	}

    	if(tickCount <= lastTime)
    	{
    		return;
    	}

    	circleAngle += 4.7F;

    	// 原版此处为大段注释掉的旧マスタースパーク实现（保持裁剪，见类注释）

    	if(tickCount > lastTime)
    	{
    		lastTime = tickCount;
    	}
		//時間で消滅
		if(tickCount >= 109)
		{
			if(!level().isClientSide)
			{
				if(!isSpellCard)
				{
					THKaguyaLib.itemEffectFinish(this, user, THKaguyaItems.MINI_HAKKERO.get());
				}
				else
				{
					if(!level().isClientSide)
					{
						discard();
					}
				}
			}
		}
	}

	public boolean hitCheckEx(Entity entity)
	{
		if(tgEntity == null)
		{
			return true;
		}
		else
		{
			return entity instanceof EnderDragonPart || entity instanceof LivingEntity && !(entity instanceof Animal) && !(entity instanceof Villager);
		}
	}

	/**
	 * マスタースパークの爆発の威力を返す
	 * @return 爆発の威力
	 */
	public float getExplosionLevel()
	{
		return 5.0F;
	}

	/** 保存するデータの書き込み */
	@Override
    protected void addAdditionalSaveData(CompoundTag tag)
    {
    	tag.putShort("count", (short)count);
    	tag.putShort("damage", (short)damage);
    	tag.putBoolean("isSpellCard", tgEntity != null);
    }

	/** 保存したデータの読み込み */
	@Override
    protected void readAdditionalSaveData(CompoundTag tag)
    {
    	count = tag.getShort("count");
    	damage = tag.getShort("damage");
    	isSpellCard = tag.getBoolean("isSpellCard");
    }

    // 原版 getShadowSize/isValidLightLevel/getBrightnessForRender/getBrightness -> 渲染器负责，裁剪

	public float getCircleAngle()
	{
		return circleAngle;
	}
}
