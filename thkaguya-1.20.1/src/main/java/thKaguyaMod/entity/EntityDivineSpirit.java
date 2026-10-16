package thKaguyaMod.entity;

import java.util.List;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import thKaguyaMod.THShotLib;
import thKaguyaMod.init.THKaguyaBlocks;
import thKaguyaMod.init.THKaguyaEntities;

/**
 * 小神霊（1.20.1 移植版）
 *
 * 原版: extract/sources/java/thKaguyaMod/entity/EntityDivineSpirit.java（1.7.10、314 行）
 *   - extends Entity、setSize(1.2F, 1.2F)
 *     → 1.20.1 は Entity に setSize が無いので EntityType.Builder.sized(1.2F, 1.2F) で指定
 *       （init/THKaguyaEntities.java の DIVINE_SPIRIT）
 *   - dataWatcher 18 = count、19 = color → 1.20.1 は SynchedEntityData の DATA_COUNT / DATA_COLOR
 *   - 300tick で消滅（判定が加算より先なので 301tick 目に消える）
 *   - 10 格以内で一番近いプレイヤーへ向きを合わせ、speed を 0.03 ずつ増やす（上限 0.5、非検知時は 0 まで減）
 *   - 移動は getVecFromAngle の向きに対し「X, Y は減算・Z は加算」（原版そのままの符号）
 *   - プレイヤーと接触すると divine_spirit を渡して消滅（原版は 8 色 metadata の BlockDivineSpirit）
 *
 * 原版からの差異（1.20.1 側の制約）:
 *   - getCollisionBox()/getBoundingBox() が null を返す hack は 1.20.1 では不可（getBoundingBox は final）
 *     → 非固体化は isPushable() = false で代替
 *   - 渡す divine_spirit は 1.20.1 では単色 1 種（元は color を metadata で持たせていた）
 *   - getShadowSize()/getEntityBrightnessForRender() は 1.20.1 に存在しない
 *     → 影は RenderDivineSpirit の shadowRadius = 0、輝度は描画側の FULL_BRIGHT で再現
 */
public class EntityDivineSpirit extends Entity
{
    /** 小神霊のカウント（原版 dataWatcher 18） */
    private static final EntityDataAccessor<Integer> DATA_COUNT =
            SynchedEntityData.defineId(EntityDivineSpirit.class, EntityDataSerializers.INT);
    /** 小神霊の色（原版 dataWatcher 19） */
    private static final EntityDataAccessor<Integer> DATA_COLOR =
            SynchedEntityData.defineId(EntityDivineSpirit.class, EntityDataSerializers.INT);

    /** 小神霊の色 */
    public short color;

    /* 原版の残骸フィールド（原版でも描画側からは参照されていない） */
    private int lastTime;
    private double lastShotMotionX;
    private double lastShotMotionY;
    private double lastShotMotionZ;
    private float lastRotationYaw;
    private float lastRotationPitch;

    private double speed;

    public EntityDivineSpirit(EntityType<?> type, Level level)
    {
        super(type, level);
    }

    public EntityDivineSpirit(Level level, double posX, double posY, double posZ, int t)
    {
        this(THKaguyaEntities.DIVINE_SPIRIT.get(), level);

        setDivineSpiritColor(t);
        setDivineSpiritCount(0);
        this.moveTo(posX + getDeltaMovement().x * 0.1D,
                    posY + getDeltaMovement().y * 0.1D,
                    posZ + getDeltaMovement().z * 0.1D,
                    getYRot(), getXRot());
        this.xo = posX;
        this.yo = posY;
        this.zo = posZ;
        lastTime = 0;
        speed = 0.0D;
    }

    @Override
    protected void defineSynchedData()
    {
        this.entityData.define(DATA_COUNT, 0);
        this.entityData.define(DATA_COLOR, 0);
    }

    /** 原版 canBePushed() → 1.20.1 は isPushable() */
    @Override
    public boolean isPushable()
    {
        return false;
    }

    /**
     * 原版 isInRangeToRenderDist(d) は boundingBox.getAverageEdgeLength() * 4 * 64。
     * 1.20.1 では isInRangeToRenderDist が無く shouldRenderAtSqrDistance が同じ役割なので、
     * 既定（size * 64）より 4 倍広い原版の式をそのまま使う。
     */
    @Override
    public boolean shouldRenderAtSqrDistance(double distance)
    {
        double d = this.getBoundingBox().getSize() * 4.0D;
        d *= 64.0D;
        return distance < d * d;
    }

    /** 原版 getCollisionBorderSize() = 0.1F → 1.20.1 は getPickRadius() */
    @Override
    public float getPickRadius()
    {
        return 0.1F;
    }

    /** 原版 canBeCollidedWith() = true */
    @Override
    public boolean canBeCollidedWith()
    {
        return true;
    }

    @Override
    public boolean isPickable()
    {
        return true;
    }

    /** 移動速度を返す */
    public double getEntitySpeed()
    {
        Vec3 motion = getDeltaMovement();
        return Math.sqrt(motion.x * motion.x + motion.y * motion.y + motion.z * motion.z);
    }

    /** 毎tick行う処理 */
    @Override
    public void tick()
    {
        super.tick();

        // 300tickで消滅
        if(getDivineSpiritCount() > 300)
        {
            if(!level().isClientSide)
            {
                discard();
            }
        }

        setDivineSpiritCount(getDivineSpiritCount() + 1);

        //***********停止空間（時計じゃなくてもっと大きな見えない壁）とEntityの当たり判定を取る****************//
        double effectiveBoundary = 10.0D;//停止空間の有効範囲
        double distance = 999.0D;
        Player nearPlayer = null;

        List<Entity> list = level().getEntities(this,
                getBoundingBox().inflate(effectiveBoundary, effectiveBoundary, effectiveBoundary));

        if(list != null && !list.isEmpty())
        {
            for(Entity entity : list)
            {
                if(entity instanceof Player)
                {
                    if(distance > entity.distanceTo(this))
                    {
                        distance = entity.distanceTo(this);
                        nearPlayer = (Player)entity;
                    }
                }
            }
        }

        if(nearPlayer != null)
        {
            if(speed < 0.5D)
            {
                speed += 0.03D;
            }

            double dx = nearPlayer.getX() - getX();
            double dz = nearPlayer.getZ() - getZ();

            setYRot((float)Math.atan2(dx, dz) / (float)Math.PI * 180F);
            setXRot((float)Math.atan2(nearPlayer.getY() + nearPlayer.getEyeHeight() - getY(),
                    Math.sqrt(dx * dx + dz * dz)) / (float)Math.PI * 180F);
        }
        else
        {
            speed -= 0.03D;
            if(speed < 0.0D)
            {
                speed = 0.0D;
            }
        }

        Vec3 vec = THShotLib.getVecFromAngle(getYRot(), getXRot(), speed);
        setPos(getX() - vec.x, getY() - vec.y, getZ() + vec.z);

        if(tickCount > lastTime)
        {
            lastTime = tickCount;
            Vec3 motion = getDeltaMovement();
            lastShotMotionX = motion.x;
            lastShotMotionY = motion.y;
            lastShotMotionZ = motion.z;
            lastRotationYaw = getYRot();
            lastRotationPitch = getXRot();
        }
    }

    /** 保存するデータの書き込み（原版 writeEntityToNBT） */
    @Override
    protected void addAdditionalSaveData(CompoundTag tag)
    {
        tag.putShort("color", (short)getDivineSpiritColor());
    }

    /** 保存したデータの読み込み（原版 readEntityFromNBT） */
    @Override
    protected void readAdditionalSaveData(CompoundTag tag)
    {
        setDivineSpiritColor(tag.getShort("color"));
    }

    /**
     * プレイヤーと接触したときの処理（原版 onCollideWithPlayer）
     * 原版はインベントリに入ったかに関わらず必ず setDead していた（サーバー側のみ）。
     */
    @Override
    public void playerTouch(Player player)
    {
        if(!level().isClientSide
                && player.getInventory().add(new ItemStack(THKaguyaBlocks.DIVINE_SPIRIT_ITEM.get(), 1)))
        {
            // 原: worldObj.playSoundAtEntity(this, "random.pop", 4.0F, ((rand.nextFloat() - rand.nextFloat()) * 0.7F + 1.0F) * 0.3F)
            level().playSound(null, blockPosition(), SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 4.0F,
                    ((random.nextFloat() - random.nextFloat()) * 0.7F + 1.0F) * 0.3F);
            // 1.7: player.onItemPickup(this, 1)。1.20.1 では ItemEntity 専用のため take で代用
            player.take(this, 1);
        }

        if(!level().isClientSide)
        {
            discard();
        }
    }

    /** 小神霊のカウントを設定 */
    public void setDivineSpiritCount(int count)
    {
        this.entityData.set(DATA_COUNT, Integer.valueOf(count));
    }

    /** 小神霊のカウントを返す */
    public int getDivineSpiritCount()
    {
        return this.entityData.get(DATA_COUNT).intValue();
    }

    /** 小神霊の色を設定 */
    public void setDivineSpiritColor(int color)
    {
        this.entityData.set(DATA_COLOR, Integer.valueOf(color));
    }

    /** 小神霊の色を返す */
    public int getDivineSpiritColor()
    {
        return this.entityData.get(DATA_COLOR).intValue();
    }
}
