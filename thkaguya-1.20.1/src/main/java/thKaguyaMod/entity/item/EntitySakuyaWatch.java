package thKaguyaMod.entity.item;

import java.util.List;

import net.minecraft.nbt.CompoundTag;
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
import thKaguyaMod.init.THKaguyaEntities;
import thKaguyaMod.init.THKaguyaItems;

/** 時間を停止させる空間（1.20.1 移植版）
 *
 * 1.7.10 -> 1.20.1 关键映射：
 *   worldObj.isRemote -> level().isClientSide、ticksExisted -> tickCount、setDead() -> discard()
 *   setSize/preventEntitySpawning/ignoreFrustumCheck/yOffset/shouldRenderInPass -> 注册与渲染器负责
 *   MovingObjectPosition(entity) 仅作实体包装 -> 直接遍历实体
 *   EntityItemFrame/EntityPainting -> ItemFrame/Painting
 *   EntityCreeper.setCreeperState(-1) -> Creeper.setSwellDir(-1)
 *   EntityPlayerMP.playerNetServerHandler.setPlayerLocation -> ServerPlayer.connection.teleport
 *   "random.orb"/"random.click" -> EXPERIENCE_ORB_PICKUP / UI_BUTTON_CLICK
 *
 * <p><b>2026-10-22 变更（时停系统落地）</b>：原版的 {@code inPrivateSquare} 是「每 tick 把周围
 * Entity 的坐标卷回 1 tick 前」，1.20.1 里改用 {@link TimeStopManager} 的 tick 门控
 * （见该类注释）。理由：坐标卷回会连玩家一起拽住，在多人服上不可接受；
 * 而且它每 tick 都要遍历 40 格内所有实体并写坐标，比自己早退昂贵得多。
 * 本类现在只负责「什么时候开始／结束」，以及把判定所需的参数同步给客户端。</p>
 */
public class EntitySakuyaWatch extends Entity implements ITimeStopSource
{
	/** 使用者 */
	public LivingEntity userEntity;
	private int count;
	private float watchRotation;
	private boolean isSpellCard;

	/** モード（サーバ専用だった private int mode を同期データへ移した。両端で同じ値を見る） */
	private static final EntityDataAccessor<Integer> DATA_MODE =
			SynchedEntityData.defineId(EntitySakuyaWatch.class, EntityDataSerializers.INT);
	/** 術者の entity id（LivingEntity は同期できないので id で持つ。-1 = 不明） */
	private static final EntityDataAccessor<Integer> DATA_USER_ID =
			SynchedEntityData.defineId(EntitySakuyaWatch.class, EntityDataSerializers.INT);
	/** {@link TimeStopManager} の FLAG_* ビット */
	private static final EntityDataAccessor<Integer> DATA_FLAGS =
			SynchedEntityData.defineId(EntitySakuyaWatch.class, EntityDataSerializers.INT);
	/** 停止空間の最大半径（サーバのコンフィグ値をクライアントへ配る） */
	private static final EntityDataAccessor<Integer> DATA_RADIUS =
			SynchedEntityData.defineId(EntitySakuyaWatch.class, EntityDataSerializers.INT);

	/**時間停止*/
	public static final int TIME_STOP = 0;
	/**特定のスペルカード内の時間停止*/
	public static final int TIME_STOP_IN_SPELLCARD = 1;
	/**時間減速1/2*/
	public static final int TIME_HALF = 2;
	/**時間制限付き時間停止*/
	public static final int TIME_STOP_WITH_LIMIT = 5;
	/**時間制限付き時間減速1/2*/
	public static final int TIME_HALF_WITH_LIMIT = 6;

    public EntitySakuyaWatch(EntityType<?> type, Level level)
    {
        super(type, level);
        // 原版：ignoreFrustumCheck = true（体の中心が画面外でも描画され続ける）
        // 1.20.1 では Entity#noCulling が EntityRenderDispatcher の視錐台カリングを無効化する。
        this.noCulling = true;
        // 原版：preventEntitySpawning / setSize(1.0F,1.0F) / yOffset（注册设定）
    	setMode(TIME_HALF);
    	// 原版：shouldRenderInPass(-2)（Forge 1.7 渲染通道，已废除）
    }

    public EntitySakuyaWatch(Level level)
    {
        this(THKaguyaEntities.SAKUYA_WATCH.get(), level);
    }

    public EntitySakuyaWatch(Level level, LivingEntity living, int pmode)
    {
        this(level);

    	userEntity = living;//使用者をshootingEntityに保存
    	THKaguyaLib.itemEffectFollowUser(this, userEntity, 1.2D, -30F);
		setMode(pmode);
		setOwnerId(living == null ? -1 : living.getId());

		isSpellCard = pmode == TIME_STOP_IN_SPELLCARD;
    }

    /** 生成時に一度だけ呼ばれる処理 */
    @Override
	protected void defineSynchedData()
    {
    	this.entityData.define(DATA_MODE, TIME_HALF);
    	this.entityData.define(DATA_USER_ID, -1);
    	this.entityData.define(DATA_FLAGS, 0);
    	this.entityData.define(DATA_RADIUS, 32);
    }

    /* ================= ITimeStopSource ================= */

    @Override
    public boolean isTimeStopActive()
    {
    	return (this.entityData.get(DATA_FLAGS) & TimeStopManager.FLAG_ACTIVE) != 0;
    }

    @Override
    public int getTimeStopMode()
    {
    	return getMode();
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

    /**
     * 実際に時間操作を開始できたときに呼ぶ（サーバ専用）。
     * ここで初めてクライアントに「凍らせてよい」ことが伝わる。
     */
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

    /* ================= モード ================= */

    public int getMode()
    {
    	return this.entityData.get(DATA_MODE);
    }

    public void setMode(int mode)
    {
    	this.entityData.set(DATA_MODE, mode);
    }

    public void setOwnerId(int id)
    {
    	this.entityData.set(DATA_USER_ID, id);
    }

	/** 押すことができるか */
    @Override
    public boolean isPushable()
    {
        return false;
    }

	/** 他のEntityと衝突するか 右クリックできるかもこれで判定 */
    @Override
    public boolean isPickable()
    {
        return false;
    }

	/** 毎tick行う処理 */
    @Override
    public void tick()
    {
        super.tick();

    	//使用者がいないなら
    	if(!level().isClientSide && userEntity == null)
    	{
    		//アイテム化させる
    		if(!isSpellCard)
    		{
    			THKaguyaLib.itemEffectFinish(this, userEntity, THKaguyaItems.SAKUYA_WATCH.get());
    		}
    		else
    		{
    			discard();
    		}
    		return;
    	}

    	//スペルカード内の時間停止で、60ticksを超えているなら
    	if(getMode() == this.TIME_STOP_IN_SPELLCARD && tickCount > 60)
    	{
    		if(!level().isClientSide)
    		{
    			discard();
    		}
    	}
    	else if(getMode() == this.TIME_STOP_WITH_LIMIT && tickCount > 100)
    	{
     		//アイテム化させる
    		THKaguyaLib.itemEffectFinish(this, userEntity, THKaguyaItems.SAKUYA_WATCH.get());
    	}
    	else if(getMode() == this.TIME_HALF_WITH_LIMIT && tickCount > 160)
    	{
    		//アイテム化させる
    		THKaguyaLib.itemEffectFinish(this, userEntity, THKaguyaItems.SAKUYA_WATCH.get());
    	}

    	//使用者がいるなら
    	if(!level().isClientSide && userEntity != null)
    	{
    		if(userEntity instanceof Player userEntity_p)
    		{
	    		if(!userEntity_p.isCreative())//クリエイティブモードでないなら
	    		{
	    			if(getMode() == this.TIME_STOP)//時間停止モードなら
	    			{
	    				userEntity_p.getFoodData().addExhaustion(0.25F);//満腹度を大きく減らす
	    			}
	    			else if(getMode() == this.TIME_HALF)//時間減速モードなら
	    			{
	    				userEntity_p.getFoodData().addExhaustion(0.10F);//満腹度を少し減らす
	    			}
	    		}
    		}
    		if(tickCount > 10 && userEntity.isShiftKeyDown())//出現から時間が経っていて、使用者がスニークモードなら
    		{
    			if(!isSpellCard)
    			{
    				THKaguyaLib.itemEffectFinish(this, userEntity, THKaguyaItems.SAKUYA_WATCH.get());//終了処理に移る
    			}
    			else
    			{
    				discard();
    			}
    		}
    		if(userEntity.hurtTime > 0)//使用者がダメージを受けたら終了
    		{
    			if(!isSpellCard)
    			{
    				THKaguyaLib.itemEffectFinish(this, userEntity, THKaguyaItems.SAKUYA_WATCH.get());//終了処理に移る
    			}
    			else
    			{
    				discard();
    			}
    		}
    	}

    	//懐中時計を使用者に追尾させる
    	THKaguyaLib.itemEffectFollowUser(this, userEntity, 1.2D, -30F);

    	if(getMode() == 3 && tickCount == 45)
    	{
    		setMode(this.TIME_STOP);
    	}
    	if(getMode() == 4 && tickCount == 45)
    	{
    		setMode(this.TIME_HALF);
    	}

    	//***********停止空間内の後始末（サーバ専用）****************//
    	if(!level().isClientSide)
    	{
    		int playerCount = 0;//停止空間内のプレイヤーの数を数える　誰もいなければ消滅させる
    		boolean dubbleWatch = false;
    		//停止空間内のEntityを全て取得
    		List<Entity> list = level().getEntities(this, getBoundingBox().inflate(40.0D, 40.0D, 40.0D));

    		for(Entity entity : list)
    		{
    			if(entity instanceof Player)//プレイヤーなら
    			{
    				playerCount++;//範囲内のプレイヤー数カウントを増加
    			}
    			//使用者でなく、使用者が乗っているEntityでもなく、額縁・絵画でもないなら
    			if(entity != userEntity && !entity.hasPassenger(userEntity)
    					&& !(entity instanceof ItemFrame)
    					&& !(entity instanceof Painting))
    			{
    				//時計が同時に２つ出現しているときの処理。全ての時計を無効化しアイテムに戻す。
    				if(entity instanceof EntitySakuyaWatch watch)
    				{
    					THKaguyaLib.itemEffectFinish(watch, watch.userEntity, THKaguyaItems.SAKUYA_WATCH.get());
    					dubbleWatch = true;
    				}
    				//時間停止中でも動ける符卡（原版 1.7.10 が残した例外）だけは動かし続ける
    				else if(entity instanceof EntitySpellCard spellCard
    						&& spellCard.isTimeStopExempt()
    						&& userEntity != null && userEntity.equals(spellCard.user))
    				{
    					spellCard.specialProcessInTimeStop();
    				}
    			}
    		}

    		if(playerCount == 0)//範囲内にプレイヤーが一人もいなかったなら
    		{
    			THKaguyaLib.itemEffectFinish(this, userEntity, THKaguyaItems.SAKUYA_WATCH.get());//終了処理に移る
    		}

    		if(dubbleWatch)
    		{
    			THKaguyaLib.itemEffectFinish(this, userEntity, THKaguyaItems.SAKUYA_WATCH.get());
    		}
    	}
        //************************************************************************************//

		count ++;
		if(getMode() > 2)
		{
			if(count % 20 == 19)
			{
	    		level().playSound(null, blockPosition(), SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.5F, 6.0F);
	    		level().playSound(null, blockPosition(), SoundEvents.UI_BUTTON_CLICK.get(), SoundSource.PLAYERS, 0.5F, 4.0F);
			}
		}
		else if(count % 20 == 19)//20フレーム周期で音を出す
    	{
    		level().playSound(null, blockPosition(), SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.5F, 6.0F);
    		level().playSound(null, blockPosition(), SoundEvents.UI_BUTTON_CLICK.get(), SoundSource.PLAYERS, 0.5F, 4.0F);
    	}
    }

    /** 保存するデータの書き込み */
    @Override
    protected void addAdditionalSaveData(CompoundTag tag)
    {
    	tag.putShort("count", (short)count);
    	tag.putBoolean("isSpellCard", isSpellCard);
    }

    /** 保存したデータの読み込み */
    @Override
    protected void readAdditionalSaveData(CompoundTag tag)
    {
    	count = tag.getShort("count");
    	isSpellCard = tag.getBoolean("isSpellCard");
    }

    // 原版 getShadowSize() -> 1.20.1 阴影半径由渲染器负责，裁剪
}
