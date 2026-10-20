package thKaguyaMod.entity.effect;
import net.minecraft.network.syncher.SynchedEntityData;

import net.minecraft.core.HolderLookup;
import java.util.List;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import thKaguyaMod.ShotData;
import thKaguyaMod.THShotLib;
import thKaguyaMod.entity.shot.EntityTHLaser;
import thKaguyaMod.entity.shot.EntityTHShot;
import thKaguyaMod.init.THKaguyaEntities;

/** 白楼剣の跳ね返すシールド（1.20.1 移植版） */
public class EntityHakurouReflecter extends Entity
{
    /** 使用者 */
    public Player user;
    public int damage;
    public int count;
    public double xVec;
    public double yVec;
    public double zVec;

    public EntityHakurouReflecter(EntityType<?> type, Level level)
    {
        super(type, level);
        count = 4;
    }

    public EntityHakurouReflecter(Level world, Player entityPlayer)
    {
        this(THKaguyaEntities.HAKUROU_REFLECTER.get(), world);

        user = entityPlayer;
        Vec3 vec3 = THShotLib.getVecFromAngle(user.getYRot(), user.getXRot(), 1.0D);
        this.moveTo(user.getX() + vec3.x, user.getY() + vec3.y + user.getEyeHeight(), user.getZ() + vec3.z, -user.getYRot(), user.getXRot());
        Vec3 vec3_2 = THShotLib.getVecFromAngle(getYRot(), getXRot(), 0.18D);
        xVec = vec3_2.x;
        zVec = vec3_2.z;
        yVec = vec3_2.y;
        count = 0;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder)
    {
    }

    @Override
    public boolean isPushable()
    {
        return false;
    }

    @Override
    public boolean isPickable()
    {
        return true;
    }

    @Override
    public void tick()
    {
        super.tick();

        if(!level().isClientSide)
        {
            if(user == null || user.isDeadOrDying())
            {
                discard();
            }
            if(tickCount >= 20)
            {
                discard();
            }
        }

        this.setDeltaMovement(xVec, yVec, zVec);
        this.setPos(getX() - xVec, getY() + yVec, getZ() + zVec);

        double width = 0.5D;
        double length = 0.5D;
        Vec3 vec3d = new Vec3(getX(), getY(), getZ());
        Vec3 vec3d1 = new Vec3(getX() + xVec, getY() + yVec, getZ() + zVec);

        // 方块碰撞检测
        BlockHitResult blockHit = level().clip(new ClipContext(vec3d, vec3d1, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
        if(blockHit != null && blockHit.getType() != HitResult.Type.MISS)
        {
            if(!level().isClientSide)
            {
                discard();
            }
        }

        // 实体碰撞检测（反射弹幕）
        AABB aabb = new AABB(
                Math.min(vec3d.x, vec3d1.x), Math.min(vec3d.y, vec3d1.y), Math.min(vec3d.z, vec3d1.z),
                Math.max(vec3d.x, vec3d1.x), Math.max(vec3d.y, vec3d1.y), Math.max(vec3d.z, vec3d1.z)).inflate(width * 2D, width * 2D, width * 2D);
        List<Entity> list = level().getEntities(this, aabb, e -> e instanceof EntityTHShot && !(e instanceof EntityTHLaser));

        for(Entity entity1 : list)
        {
            EntityTHShot shot = (EntityTHShot)entity1;
            if(user != null && user != shot.user)
            {
                ShotData reflectShot = ShotData.shot(THShotLib.FORM_SCALE, THShotLib.AQUA, THShotLib.SIZE[THShotLib.FORM_SCALE], 3.0F, 0, 40, 0);
                THShotLib.createShot(user, THShotLib.pos_Entity(shot), THShotLib.angle(-getYRot(), getXRot()), 0.3D, 0.7D, 0.05D, reflectShot);

                if(!level().isClientSide)
                {
                    entity1.discard();
                }
                shot.playSound(net.minecraft.sounds.SoundEvents.ARROW_SHOOT, 0.5F, 0.4F / (random.nextFloat() * 4F + 0.8F));
            }
        }

        count++;
    }

    @Override
    public boolean hurt(net.minecraft.world.damagesource.DamageSource source, float amount)
    {
        return false;
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
