package thKaguyaMod.entity.item;

import java.util.List;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.decoration.Painting;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import thKaguyaMod.THKaguyaLib;
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
 *   EntityGhast.attackCounter -> 1.20.1 无公开字段，裁剪
 *   EntityPlayerMP.playerNetServerHandler.setPlayerLocation -> ServerPlayer.connection.teleport
 *   "random.orb"/"random.click" -> EXPERIENCE_ORB_PICKUP / UI_BUTTON_CLICK
 */
public class EntitySakuyaWatch extends Entity
{
	/** 使用者 */
	public LivingEntity userEntity;
	private int count;
	private int mode;
	private float watchRotation;
	private boolean isSpellCard;

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
        // 原版：ignoreFrustumCheck = true（渲染阶段处理）、preventEntitySpawning / setSize(1.0F,1.0F) / yOffset（注册设定）
    	mode = 2;
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
		mode = pmode;

		isSpellCard = mode == TIME_STOP_IN_SPELLCARD;
    }

    /** 生成時に一度だけ呼ばれる処理 */
    @Override
	protected void defineSynchedData()
    {
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
    			if(!level().isClientSide)
    			{
    				discard();
    			}
    		}
    		return;
    	}

    	//スペルカード内の時間停止で、60ticksを超えているなら
    	if(mode == this.TIME_STOP_IN_SPELLCARD && tickCount > 60)
    	{
    		if(!level().isClientSide)
    		{
    			discard();
    		}
    	}
    	else if(mode == this.TIME_STOP_WITH_LIMIT && tickCount > 100)
    	{
     		//アイテム化させる
    		THKaguyaLib.itemEffectFinish(this, userEntity, THKaguyaItems.SAKUYA_WATCH.get());
    	}
    	else if(mode == this.TIME_HALF_WITH_LIMIT && tickCount > 160)
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
	    			if(mode == this.TIME_STOP)//時間停止モードなら
	    			{
	    				userEntity_p.getFoodData().addExhaustion(0.25F);//満腹度を大きく減らす
	    			}
	    			else if(mode == this.TIME_HALF)//時間減速モードなら
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
    				if(!level().isClientSide)
    				{
    					discard();
    				}
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
    				if(!level().isClientSide)
    				{
    					discard();
    				}
    			}
    		}
    	}

    	//懐中時計を使用者に追尾させる
    	THKaguyaLib.itemEffectFollowUser(this, userEntity, 1.2D, -30F);

    	if(mode == 3 && tickCount == 45)
    	{
    		mode = this.TIME_STOP;
    	}
    	if(mode == 4 && tickCount == 45)
    	{
    		mode = this.TIME_HALF;
    	}

    	int playerCount = 0;//停止空間内のプレイヤーの数を数える　誰もいなければ消滅させる

    	//***********停止空間とEntityの当たり判定を取る****************//
    	boolean dubbleWatch = false;
    	boolean pass = false;
    	double effectiveBoundary = 40.0D;//停止空間の有効範囲
    	//停止空間内のEntityを全て取得
    	List<Entity> list = level().getEntities(this, getBoundingBox().inflate(effectiveBoundary, effectiveBoundary, effectiveBoundary));

        if (list != null && list.size() > 0)//取得したリストにEntityがいるなら
        {
            for (int j1 = 0; j1 < list.size(); j1++)//取得したEntityリストの最初から最後まで見る
            {
                Entity entity = list.get(j1);
            	if(entity instanceof Player)//プレイヤーなら
            	{
            		playerCount++;//範囲内のプレイヤー数カウントを増加
            	}
            	//使用者でなく、使用者が乗っているEntityでもなく、額縁・絵画でもないなら
        		if (entity != userEntity && !entity.hasPassenger(userEntity)
        				&& !(entity instanceof ItemFrame)
        				&& !(entity instanceof Painting))
        		{
        			pass = false;
        			//時計が同時に２つ出現しているときの処理。全ての時計を無効化しアイテムに戻す。
        			if(entity instanceof EntitySakuyaWatch watch)
        			{
        				//アイテム化させる
        	    		THKaguyaLib.itemEffectFinish(watch, watch.userEntity, THKaguyaItems.SAKUYA_WATCH.get());
        	    		dubbleWatch = true;
        			}
        			if(entity instanceof EntitySpellCard spellCard)
        			{
        				if(spellCard.canMoveInTimeStop && spellCard.user.equals(this.userEntity))
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

    	if(playerCount == 0)//範囲内にプレイヤーが一人もいなかったなら
    	{
    		if(!level().isClientSide)
    		{
    			THKaguyaLib.itemEffectFinish(this, userEntity, THKaguyaItems.SAKUYA_WATCH.get());//終了処理に移る
    		}
    	}

    	if(dubbleWatch)
    	{
    		//アイテム化させる
    		THKaguyaLib.itemEffectFinish(this, userEntity, THKaguyaItems.SAKUYA_WATCH.get());
    	}

        //************************************************************************************//

		count ++;
		if(mode > 2)
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

	//時間操作空間に入っていた時の処理
	protected void inPrivateSquare(Entity hitEntity)
    {
    	//Entityが誕生してから２フレーム経過したもののみ判定
    	if(hitEntity.tickCount >= 2)
    	{
    		//====時間停止空間の場合====//
    		if(mode == this.TIME_STOP || mode == this.TIME_STOP_IN_SPELLCARD || mode == this.TIME_STOP_WITH_LIMIT)
    		{
    		//Entityを１フレーム前の状態に、できる限りもっていく処理
	    		hitEntity.setPos(hitEntity.xo, hitEntity.yo, hitEntity.zo);

	    		hitEntity.setYRot(hitEntity.yRotO);
	    		hitEntity.setXRot(hitEntity.xRotO);
	    		if(!hitEntity.onGround())
	    		{
	    			hitEntity.setDeltaMovement(0.0D, -0.0D, 0.0D);
	    		}
	    		else
	    		{
	    			hitEntity.setDeltaMovement(0.0D, hitEntity.getDeltaMovement().y, 0.0D);
	    		}
	    		hitEntity.setAirSupply(0);

	    		hitEntity.tickCount--;//誕生してからのカウントを増やさない
	    		hitEntity.fallDistance -= 0.076865F;//落下距離を変動しないように　この値はフレームに比例する
    			if(hitEntity instanceof LivingEntity living)//範囲内のEntityがLivingEntityに属しているなら
    			{
    				living.yHeadRot = living.yHeadRotO;

    				// living.attackTime++; // TODO(阶段4): 1.20 无 attackTime 字段，时停冻结动画语义不同

    				if(living instanceof Creeper entityCreeper)//Creeperに属しているなら
    				{
    					entityCreeper.setSwellDir(-1);//爆発のカウントを戻す
    				}
    				// 原版 EntityGhast.attackCounter-- -> 1.20.1 の Ghast に公開カウンタはないため裁剪

    				if(living instanceof TamableAnimal)//テイム可能なら
    				{
    					//座っている動物は重力がなくなると立つみたい　わからない程度に重力をかける処理
    					living.setDeltaMovement(living.getDeltaMovement().x, living.getDeltaMovement().y - 0.000001D, living.getDeltaMovement().z);
    				}

    				//プレイヤーの動きを止める
    				if(living instanceof ServerPlayer player)
    				{
    					player.connection.teleport(player.xo, player.yo, player.zo, player.getYRot(), player.getXRot());
    				}
    			}

	    		//加速だけは全Entityが処理していないため無理そう　　だから重力の影響を受けるものは扱いにくい
    		}
    		//====時間減速空間の場合====//
    		else if(mode == this.TIME_HALF || mode == this.TIME_HALF_WITH_LIMIT)//時間を1/2の進みにする
    		{
    			slowDownSpeed(hitEntity, 1, 2);
    		}
    		else if(mode == 3)
    		{
    			slowDownSpeed(hitEntity, 5 - (tickCount / 9), 5);
    		}
    		else if(mode == 4)
    		{
    			slowDownSpeed(hitEntity, 10 - (tickCount / 9), 10);
    		}
        }
    }

	/*時間を操る
	 * hitEntity : 操る対象
	 * timeRate  : 進む時間の倍率。減速しか扱えず、整数分の１の速度にしかできない。ex 1/2, 1/3, 1/4, 1/5...
	 */
	private void slowDownSpeed(Entity hitEntity, int timeRateNum, int timeRateDen)
	{
		float timeRate = (float)timeRateNum / (float)timeRateDen;


		if(timeRate < 1.0)
		{
			Vec3 slowMotion = hitEntity.getDeltaMovement();
			hitEntity.setDeltaMovement(
					slowMotion.x - (hitEntity.getX() - hitEntity.xo) * timeRate,
					slowMotion.y - (hitEntity.getY() - hitEntity.yo) * timeRate,
					slowMotion.z - (hitEntity.getZ() - hitEntity.zo) * timeRate);
			hitEntity.fallDistance -= 0.076865F * timeRate;//落下距離を半減（空中待機時間で変動してしまう）
			if( count % timeRateDen < timeRateNum)//偶数フレームのみ処理
			{
				hitEntity.tickCount--;//誕生してからのカウントを増やさない
				if(hitEntity instanceof LivingEntity living)
				{
					living.yHeadRot = living.yHeadRotO;

					if(living instanceof Creeper entityCreeper)
					{
						entityCreeper.setSwellDir(-1);
					}
					// 原版 EntityGhast.attackCounter-- -> 1.20.1 の Ghast に公開カウンタはないため裁剪
				}
			}
		}
		else
		{
			int timeRate2 = (int)timeRate;
			for(int i = 2; i <= timeRate2; i++)
			{
				hitEntity.tickCount++;
				hitEntity.tick();
			}
		}
	}

}