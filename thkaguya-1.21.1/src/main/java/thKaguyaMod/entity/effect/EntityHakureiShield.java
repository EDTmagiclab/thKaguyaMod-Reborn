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
import thKaguyaMod.entity.item.EntitySakuyaWatch;
import thKaguyaMod.entity.shot.EntityTHShot;
import thKaguyaMod.init.THKaguyaEntities;

/** 霊夢の結界（1.20.1 移植版） */
public class EntityHakureiShield extends Entity
{
    public Player user;
    public int damage;
    public int count;
    public double xVec;
    public double yVec;
    public double zVec;

    public EntityHakureiShield(EntityType<?> type, Level level)
    {
        super(type, level);
        count = 4;
    }

    public EntityHakureiShield(Level world, Player entityPlayer)
    {
        this(THKaguyaEntities.HAKUREI_SHIELD.get(), world);

        user = entityPlayer;
        this.moveTo(user.getX(), user.getY() + user.getEyeHeight() - 0.4D, user.getZ(), -user.getYRot(), 0F);
        xVec = -Math.sin((double)getYRot() / 180.0D * Math.PI);
        zVec = Math.cos((double)getYRot() / 180.0D * Math.PI);
        yVec = 0.0D;
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
        }

        double width = 1.5D;
        double length = 3.0D;
        Vec3 vec3d = new Vec3(getX() - xVec * width, getY() - yVec * length / 2.0D, getZ() - zVec * width);
        Vec3 vec3d1 = new Vec3(getX() + xVec * width, getY() + yVec * length / 2.0D, getZ() + zVec * width);

        // 方块碰撞检测
        BlockHitResult blockHit = level().clip(new ClipContext(vec3d, vec3d1, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
        if(blockHit != null && blockHit.getType() != HitResult.Type.MISS)
        {
            if(!level().isClientSide)
            {
                discard();
            }
        }

        // 实体碰撞检测
        AABB aabb = new AABB(
                Math.min(vec3d.x, vec3d1.x), Math.min(vec3d.y, vec3d1.y), Math.min(vec3d.z, vec3d1.z),
                Math.max(vec3d.x, vec3d1.x), Math.max(vec3d.y, vec3d1.y), Math.max(vec3d.z, vec3d1.z)).inflate(width * 2D, length, 0.01D);
        List<Entity> list = level().getEntities(this, aabb, e -> !(e instanceof EntitySakuyaWatch));

        for(Entity entity1 : list)
        {
            Vec3 intercept = entity1.getBoundingBox().clip(vec3d, vec3d1).orElse(null);
            if(intercept != null)
            {
                onImpact(entity1);
            }
        }

        count++;
    }

    /** ブロックやEntityに当たった時の処理 */
    protected void onImpact(Entity hitEntity)
    {
        if(!level().isClientSide && hitEntity != null)
        {
            if(!(hitEntity instanceof EntityTHShot))
            {
                double slope = ((this.getX() + this.xVec) - (this.getX() - this.xVec)) / ((this.getZ() + this.zVec) - (this.getZ() - this.zVec));
                double height = this.getX() - slope * this.getZ();

                if(hitEntity.getX() > hitEntity.getZ() * slope + height)
                {
                    hitEntity.setDeltaMovement(-Math.sin(this.getYRot() / 180.0D * Math.PI), hitEntity.getDeltaMovement().y, -Math.cos(this.getYRot() / 180.0D * Math.PI));
                }
                else
                {
                    hitEntity.setDeltaMovement(Math.sin(this.getYRot() / 180.0D * Math.PI), hitEntity.getDeltaMovement().y, Math.cos(this.getYRot() / 180.0D * Math.PI));
                }

                if(hitEntity instanceof net.minecraft.world.entity.LivingEntity living)
                {
                    living.hurt(living.damageSources().indirectMagic(this, user), 1.0F);
                }
            }
            else
            {
                // EntityTHShot なら消す
                hitEntity.discard();
            }
        }
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
