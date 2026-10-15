package thKaguyaMod.entity.item;

import static thKaguyaMod.DanmakuConstants.*;

import java.util.List;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import thKaguyaMod.LaserData;
import thKaguyaMod.THShotLib;
import thKaguyaMod.entity.shot.EntityTHShot;
import thKaguyaMod.init.THKaguyaConfig;
import thKaguyaMod.init.THKaguyaEntities;
import thKaguyaMod.init.THKaguyaItems;

/** スキマ（1.20.1 移植版）
 *
 * 1.7.10 -> 1.20.1 关键映射：
 *   dataWatcher(16) -> SynchedEntityData COLOR
 *   timeUntilPortal -> portalCooldown（Entity 公开字段）
 *   EntityPlayerMP.playerNetServerHandler.setPlayerLocation -> ServerPlayer.connection.teleport
 *   interactFirst -> interact(Player, InteractionHand) -> InteractionResult
 *   染料 Item.getItemById(351) + getItemDamage -> DyeItem + getDyeColor().getId()
 *   shot.angle.xCoord 可变 -> Vec3 不可变，shot.angle 整体替换
 *   "mob.endermen.portal" -> SoundEvents.ENDERMAN_TELEPORT
 */
public class EntitySukima extends Entity
{
	private static final EntityDataAccessor<Integer> COLOR =
			SynchedEntityData.defineId(EntitySukima.class, EntityDataSerializers.INT);

	private int count;

	private double warpPosX;
	private double warpPosY;
	private double warpPosZ;
	/** 使用者 */
	public LivingEntity userEntity;
	private float warpAngle;
	private boolean warp;
	private int pattern;//スキマのアニメーション用　未使用
	private int color;//スキマの色　同じ色のスキマ同士しか移動できない

    public EntitySukima(EntityType<?> type, Level level)
    {
        super(type, level);
        // 原版：preventEntitySpawning / setSize / yOffset 均由注册与默认値担当
    	pattern = 0;
    }

    public EntitySukima(Level level)
    {
        this(THKaguyaEntities.SUKIMA.get(), level);
    }

	public EntitySukima(Level level, LivingEntity entityLivingBase, double x, double y, double z, float angle)
    {
        this(level);

        setPos(x, y, z);
        setRot(angle, 0.0F);
    	count = 0;
    	pattern = 0;
    	color = 1;
    	setColor(color);
    	setNewWarpPoint();
    	userEntity = entityLivingBase;
    }

	public EntitySukima(Level level, LivingEntity entityLivingBase, double x, double y, double z, int sukimaColor)
    {
        this(level);

        setPos(x, y, z);//初期位置を設定(x,y,z)
    	float yaw = entityLivingBase.getYRot();
    	yaw = (float)Math.atan2(x - entityLivingBase.getX(), z - entityLivingBase.getZ()) / 3.141593F * 180F;
    	setRot(360F - yaw, 0.0F);
    	count = 0;
    	pattern = 0;
    	color = sukimaColor;
    	setColor(color);
    	setNewWarpPoint();
    	userEntity = entityLivingBase;
    }

	public EntitySukima(Level level, LivingEntity entityLivingBase, double x, double y, double z, float angle, int sukimaColor)
    {
        this(level);

        setPos(x, y, z);//初期位置を設定(x,y,z)
    	setRot(angle + 180F, 0.0F);
    	count = 0;
    	pattern = 0;
    	color = sukimaColor;
    	setColor(sukimaColor);
    	setNewWarpPoint();
    	userEntity = entityLivingBase;
    }

    // 原版 canTriggerWalking() -> 1.20.1 の Entity はブロックの踏み判定を発火しないため裁剪

	/** 生成時に一度だけ呼ばれる処理 */
	@Override
    protected void defineSynchedData()
    {
    	this.entityData.define(COLOR, 0);//スキマの色
    }

	//押すことができるか
	@Override
    public boolean isPushable()
    {
        return false;
    }

	//当たり判定の有無　falseだと右クリックの選択ですらできない。trueなら当たり判定が出現する
    @Override
	public boolean isPickable()
    {
        return true;
    }

	/** 毎tick呼ばれる処理 */
    @Override
    public void tick()
    {
        super.tick();

    	//直接ワープスキマは時間消滅する
    	if(getColor() == 16 || getColor() >= 32)
    	{
    		if(tickCount > 24)
    		{
    			discard();
    		}
    	}

    	// 飛光虫ネストのスキマ
    	if(getColor() == 17)
    	{
    		if(tickCount == 5)
    		{
    			THShotLib.createLaserA(userEntity, this, THShotLib.pos_Entity(this), THShotLib.angle(getYRot() - 180F, getXRot()), 0.1D, 1.4D, 0.1D, THShotLib.gravity_Zero(),
    					LaserData.laser(PURPLE, 0.1F, 4.0F, 8.0F, 0, 40, 0));
    		}
    		if(tickCount > 9)
    		{
    			discard();
    		}
    		return;
    	}

    	//***********スキマとEntityの当たり判定を取る****************//
    	List<Entity> list = level().getEntities(this, getBoundingBox().inflate(10.0D, 10.0D, 10.0D));//判定を行う箱の大きさ

		double minX, minY, minZ, maxX, maxY, maxZ;
		minX = getX() - 1.0D;
		minY = getY() - 1.5D;
		minZ = getZ() - 1.0D;
		maxX = getX() + 1.0D;
		maxY = getY() + 1.0D;
		maxZ = getZ() + 1.0D;

        if (list != null && list.size() > 0)
        {
            for (int j1 = 0; j1 < list.size(); j1++)
            {
                Entity entity = list.get(j1);
        		if( !(entity instanceof EntitySukima) &&
        			entity.getX() > minX && entity.getX() < maxX &&
        			entity.getY() > minY && entity.getY() < maxY &&
        			entity.getZ() > minZ && entity.getZ() < maxZ)
        		{
        			//通常のスキマなら新しいワープポイントを探す
        			setNewWarpPoint();
        			// 1.7: entity.timeUntilPortal == 0（传送冷却计数器）。
        			// 注意：不能写 getPortalWaitTime() == 0 —— 1.20.1 中 Player 覆写它恒返回 80，
        			// 会导致玩家传送条件永假（实测踩坑）。正确对应物是 isOnPortalCooldown()。
        			if(warp && !entity.isOnPortalCooldown() && !level().isClientSide)//ワープが有効だった場合の入ったEntityの処理
        			{
        				// 1.7: prevPos* += warpPos - pos（重设渲染插值锚点，防止瞬移残影）
        				entity.xo += warpPosX - entity.getX();
        				entity.yo += warpPosY - entity.getY();
        				entity.zo += warpPosZ - entity.getZ();
        				if(entity instanceof ServerPlayer player)
        				{
        					// 原版: setPlayerLocation(warpPosX, warpPosY + yOffset, warpPosZ, yaw, pitch)
        					// 1.20.1 yOffset 已废除；connection.teleport 会同步客户端并重置 positionRider
        					player.connection.teleport(warpPosX, warpPosY, warpPosZ, player.getYRot(), player.getXRot());
        				}
        				else
        				{
        					// 非玩家实体（含弹幕）：直接 teleportTo（服务端权威，自动广播位移包）
        					entity.teleportTo(warpPosX, warpPosY, warpPosZ);
        				}
        				if(entity instanceof EntityTHShot shot)
        				{
        					Vec3 move = THShotLib.getVecFromAngle(warpAngle - THShotLib.getAngleAndAngleSpan(getYRot(), shot.getYRot()), shot.getXRot());
        					shot.angle = THShotLib.angle(-move.x, -move.y, move.z);
        					double speed = shot.getSpeed();
        					Vec3 shotMotion = new Vec3(shot.angle.x * speed, shot.angle.y * speed, shot.angle.z * speed);
        					shot.setDeltaMovement(shotMotion);
        					shot.lastShotMotionX = shotMotion.x;
        					shot.lastShotMotionY = shotMotion.y;
        					shot.lastShotMotionZ = shotMotion.z;
        				}
        				else
        				{
        					entity.setYRot(entity.getYRot() + warpAngle);
        					Vec3 motion = entity.getDeltaMovement();
        					entity.setDeltaMovement(
        							motion.x * -Math.sin(entity.getYRot()),
        							motion.y,
        							motion.z * Math.cos(entity.getYRot()));
        				}
        				// 传送后设置冷却（先设冷却再播音效；teleportTo 内部 setOldPosAndRot
        				// 会把 xo/yo/zo 刷成新位置，上面手动加的 xo 偏移被覆盖属预期——
        				// 非玩家实体没有客户端插值问题，弹幕靠 lastShotMotion 保持连续）
        				entity.setPortalCooldown(40);//２秒間スキマの影響を受けない
        				level().playSound(null, entity.blockPosition(), sukimaWarpSound(), SoundSource.PLAYERS, 1.0F, 1.0F);
        			}
        		}
            }
        }
	}

	/** コンフィグのスキマ効果音を SoundEvent に変換（"mob.endermen.portal" -> ENDERMAN_TELEPORT） */
	private static SoundEvent sukimaWarpSound()
	{
		String name = THKaguyaConfig.sukimaWarpSE;
		if("mob.endermen.portal".equals(name))
		{
			return SoundEvents.ENDERMAN_TELEPORT;
		}
		return SoundEvent.createVariableRangeEvent(new ResourceLocation(name));
	}

	//プレイヤーが、Entityに対して右クリックしたときに呼び出される
    @Override
    public InteractionResult interact(Player player, InteractionHand hand)
    {
    	if(getColor() >= 16)
    	{
    		return InteractionResult.PASS;
    	}
    	ItemStack itemstack = player.getMainHandItem();//プレイヤーの選択しているアイテムを取得
    	if(!player.level().isClientSide)
    	{
	    	if(itemstack.getItem() instanceof DyeItem dye)//プレイヤーが染料を持っていれば
	    	{
	    		if(THKaguyaConfig.useDefaultGapSE)
    			{
    				level().playSound(null, player.blockPosition(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.5F, 0.4F);
    			}
    			else
    			{
    				level().playSound(null, player.blockPosition(), sukimaWarpSound(), SoundSource.PLAYERS, 1.0F, 1.0F);
    			}
	    		//スキマの色を、染料の色に対応する色にする（1.7 の damage値 = DyeColor.getId()）
	    		setColor(dye.getDyeColor().getId());
	    		color = getColor();
	    	}
	    	else if(itemstack.getItem() == THKaguyaItems.REMORSE_ROD.get())//悔悟の棒を持った状態
	    	{
	    		this.spawnAtLocation(THKaguyaItems.GAP.get(), 1);//アイテム：スキマをドロップさせる
	    		discard();//消滅させる
	    	}
	    	else if(player.isShiftKeyDown())//染料も持ってなく、スニークなら
	    	{
	    		this.spawnAtLocation(THKaguyaItems.GAP.get(), 1);//アイテム：スキマをドロップさせる
	    		discard();//消滅させる
	    	}
    	}
        return InteractionResult.SUCCESS;
    }

    /** 保存するデータの書き込み */
    @Override
    protected void addAdditionalSaveData(CompoundTag tag)
    {
    	tag.putShort("color", (short)getColor());
    }

    /** 保存したデータの読み込み */
    @Override
    protected void readAdditionalSaveData(CompoundTag tag)
    {
    	setColor(tag.getShort("color"));
    }

    // 原版 getShadowSize() -> 1.20.1 阴影半径由渲染器负责，裁剪

	//スキマの色を設定する
	public void setColor(int color)
	{
		this.entityData.set(COLOR, color);
	}

    //スキマの色を取得する
	public int getColor()
	{
		return this.entityData.get(COLOR);
	}

    //新しいワープポイントを取得する
	public void setNewWarpPoint()
	{
		double nearDistance = 1000.0D;//一番スキマの距離として、ありえないデタラメな値を保存
		double distance;
		EntitySukima setSukima = null;
		EntitySukima nearSukima = null;
    	List<Entity> list = level().getEntities(this, getBoundingBox().inflate(128.0D, 128.0D, 128.0D));//周囲256x256x256の立方体内が有効範囲

        if (list != null && list.size() > 0)//範囲内のEntityをチェック
        {
            for (int j1 = 0; j1 < list.size(); j1++)
            {
            	Entity entity = list.get(j1);
            	if(entity instanceof EntitySukima)//スキマのみチェック
            	{
            		setSukima = (EntitySukima)entity;
            		if( (getColor() % 32) == (setSukima.getColor() % 32) )//スキマ同士の色が同じ場合のみ判定
            		{
            			distance = distanceTo(entity);
            			if(distance < nearDistance && distance > 5.0D)//距離が５m以上で、距離が近いなら一番近いスキマとして保存
            			{
            				nearDistance = distance;
            				nearSukima = setSukima;
            			}
            		}
            	}
            }
        	if(nearSukima != null)//範囲内に有効なスキマがあればワープさせる
        	{
        		float angleNS = nearSukima.getYRot() / 180F * 3.141593F;
        		warpPosX = nearSukima.getX() + (double)Math.sin(angleNS) * 1.8D;
        		warpPosY = nearSukima.getY();
        		warpPosZ = nearSukima.getZ() - (double)Math.cos(angleNS) * 1.8D;
        		warpAngle = 180F - ((getYRot() % 360) - (nearSukima.getYRot() % 360));
        		warp = true;
        	}
        	else//範囲内に有効なスキマがなければ何も起きない
        	{
        		warp = false;
        	}
        }
	}

}