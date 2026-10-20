package thKaguyaMod.entity.effect;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import thKaguyaMod.DanmakuConstants;
import thKaguyaMod.LaserData;
import thKaguyaMod.THShotLib;
import thKaguyaMod.init.THKaguyaEntities;

/** エイジャの赤石の光のエフェクト（1.20.1 移植版） */
public class EntityAjaRedStoneEffect extends Entity
{
    private static final EntityDataAccessor<Integer> DATA_LIGHT =
            SynchedEntityData.defineId(EntityAjaRedStoneEffect.class, EntityDataSerializers.INT);

    /** 使用者 */
    public LivingEntity user;
    /** 取り込んだ光の量 */
    public int lightLevel;
    private double length = 1.2D;

    public EntityAjaRedStoneEffect(EntityType<?> type, Level level)
    {
        super(type, level);
        lightLevel = 0;
        setLightPower(0);
    }

    public EntityAjaRedStoneEffect(Level world, LivingEntity entityLiving)
    {
        this(THKaguyaEntities.AJA_RED_STONE_EFFECT.get(), world);

        user = entityLiving;
        double x = user.getX() - Math.sin((user.getYRot()) / 180F * 3.141593F) * Math.cos(user.getXRot() / 180F * 3.141593F) * length;
        double y = user.getY() - Math.sin(user.getXRot() / 180F * 3.141593F) * length + (double)user.getEyeHeight();
        double z = user.getZ() + Math.cos((user.getYRot()) / 180F * 3.141593F) * Math.cos(user.getXRot() / 180F * 3.141593F) * length;
        this.moveTo(x, y, z, user.getYRot(), user.getXRot());
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder)
    {
        builder.define(DATA_LIGHT, 0);
    }

    /**
     * 視線（エンティティレイトレース）で拾われないようにする。
     *
     * <p>原版は canBeCollidedWith() = false で「右クリック選択すらされない」状態だった。
     * 1.20.1 のエンティティ選択は isPickable() で判定されるため、これを false にしないと
     * 使用者の目の前に浮かぶ 2 格幅の当たり判定が常に視線にヒットし、
     * Jade などのツールチップ MOD が「entity.thkaguya.aja_red_stone_effect を見ている」と
     * 表示し続けてしまう（実測で発覚）。
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
        if(!level().isClientSide && user == null)
        {
            discard();
            return;
        }
        super.tick();

        if(user != null)
        {
            if(user instanceof Player userPlayer)
            {
                if(!userPlayer.isUsingItem())
                {
                    int damage = (int)((double)lightLevel / 40.0);
                    if(damage > 30)//最大ダメージはハート15個分。頑張ってもそこまで
                    {
                        damage = 30;
                    }
                    // 原版は完全な暗闇（ブロック光照 0）だと lightLevel が増えず、
                    // 離しても永久に発射されない＝エフェクトが消えずに残り続ける。
                    // 移植版では最低 1 を保証して必ず発射されるようにする。
                    if(damage <= 0)
                    {
                        damage = 1;
                    }

                    if(damage > 0)//ダメージがあるなら
                    {
                        //レーザーを発射する
                        THShotLib.createLaserA(user, this, THShotLib.pos(getX(), getY(), getZ()), THShotLib.angle(user.getYRot(), user.getXRot()), 0.1D, 4.0D, 0.3D, THShotLib.gravity_Zero(),
                                LaserData.laser(THShotLib.RED, (float)damage * 0.01F, (float)damage * 0.3F, damage, 0, 60, DanmakuConstants.SPECIAL_AJA_REDSTONE));
                        if(!level().isClientSide)//サーバーなら
                        {
                            discard();//光のエフェクトを消滅させる
                        }
                    }
                }
            }
            else
            {
                if(!level().isClientSide)
                {
                    discard();
                }
            }
        }

        // 今ある場所の光レベルに合わせて光の量を増やす
        // 原版 worldObj.getBlockLightValue(x, y, z) は「その位置のブロック光照レベル」。
        // 1.20.1 の Level#getLightEmission は「その位置のブロック自身の発光値」で意味が違うため、
        // 正しくは getBrightness(LightLayer.BLOCK, pos)（0-15 の受けているブロック光照）を使う。
        // ※ ここを getLightEmission にすると、発光ブロックの上以外では常に 0 になり
        //   lightLevel が永久に増えず、レーザーが一切発射されなくなる（実測で発覚）。
        setLightPower(level().getBrightness(LightLayer.BLOCK, blockPosition()));
        lightLevel += getLightPower();

        if(user != null)
        {
            double x = user.getX() - Math.sin((user.getYRot()) / 180F * 3.141593F) * Math.cos(user.getXRot() / 180F * 3.141593F) * length;
            double y = user.getY() - Math.sin(user.getXRot() / 180F * 3.141593F) * length + user.getEyeHeight();
            double z = user.getZ() + Math.cos((user.getYRot()) / 180F * 3.141593F) * Math.cos(user.getXRot() / 180F * 3.141593F) * length;
            this.setYRot(user.getYHeadRot());
            this.setXRot(user.getXRot());
            this.setPos(x, y, z);
        }
    }

    /** 取り込んでいる光の量を設定 */
    public void setLightPower(int lightPower)
    {
        this.entityData.set(DATA_LIGHT, lightPower);
    }

    /** 取り込んでいる光の量を返す */
    public int getLightPower()
    {
        return this.entityData.get(DATA_LIGHT);
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag)
    {
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag)
    {
    }
}
