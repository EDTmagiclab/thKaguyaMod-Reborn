package thKaguyaMod.entity.living;

import static thKaguyaMod.DanmakuConstants.*;

import javax.annotation.Nullable;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.phys.Vec3;
import thKaguyaMod.ShotData;
import thKaguyaMod.THShotLib;
import thKaguyaMod.init.THKaguyaConfig;
import thKaguyaMod.init.THKaguyaEntities;

/**
 * ハナビーパー：爆発時に弾幕をまき散らすクリーパー（1.20.1 移植版）
 *
 * 通过 AccessTransformer 把 Creeper.explodeCreeper 提升为 protected 后重写：
 * 膨胀到最大时不破坏方块，改为撒一圈弹幕并消失（对齐原版注释掉的 createExplosion）。
 */
public class EntityDanmakuCreeper extends Creeper
{
    public EntityDanmakuCreeper(EntityType<? extends EntityDanmakuCreeper> type, Level level)
    {
        super(type, level);
    }

    public EntityDanmakuCreeper(Level level)
    {
        this(THKaguyaEntities.DANMAKU_CREEPER.get(), level);
    }

    public static AttributeSupplier.Builder createAttributes()
    {
        return Creeper.createAttributes();
    }

    /** 批次 C1：尊重配置里的 Spawn Hanabeeper 开关（原版把 addSpawn 包在 if 里） */
    @Override
    public boolean checkSpawnRules(LevelAccessor level, MobSpawnType reason)
    {
        if (reason == MobSpawnType.NATURAL || reason == MobSpawnType.CHUNK_GENERATION)
        {
            // 同 EntityDanmakuMob：只在地表（头顶露天）生成，避免刷进地下洞穴
            if (!level.canSeeSky(this.blockPosition()))
            {
                return false;
            }
            return THKaguyaConfig.allowNaturalSpawn(this.getType());
        }
        return super.checkSpawnRules(level, reason);
    }

    /** 自然生成时自带永久发光（同 EntityDanmakuMob，方便寻找） */
    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType reason,
                                        @Nullable SpawnGroupData spawnData, @Nullable CompoundTag dataTag)
    {
        SpawnGroupData result = super.finalizeSpawn(level, difficulty, reason, spawnData, dataTag);
        if ((reason == MobSpawnType.NATURAL || reason == MobSpawnType.CHUNK_GENERATION)
                && this.getType().getCategory() == net.minecraft.world.entity.MobCategory.MONSTER)
        {
            this.addEffect(new MobEffectInstance(MobEffects.GLOWING, -1, 0, false, false));
        }
        return result;
    }
    /** 膨胀到最大时：用弹幕爆炸替代方块爆炸 */
    @Override
    public void explodeCreeper()
    {
        if(!this.level().isClientSide)
        {
            this.fireDanmaku();
            this.discard();
        }
    }

    /**
     * 原版 EntityDanmakuCreeper.java:49-68 の die()：
     * 死亡時に XP を撒いたあと、爆発粒子（"explode"）を 20 個出す。
     * 移植版はこの粒子を出していなかった。
     */
    @Override
    public void die(net.minecraft.world.damagesource.DamageSource source)
    {
        super.die(source);
        if(this.level() instanceof net.minecraft.server.level.ServerLevel serverLevel)
        {
            for(int i = 0; i < 20; ++i)
            {
                double dx = this.random.nextGaussian() * 0.02D;
                double dy = this.random.nextGaussian() * 0.02D;
                double dz = this.random.nextGaussian() * 0.02D;
                serverLevel.sendParticles(net.minecraft.core.particles.ParticleTypes.EXPLOSION,
                        this.getX() + (double)(this.random.nextFloat() * this.getBbWidth() * 2.0F) - (double)this.getBbWidth(),
                        this.getY() + (double)(this.random.nextFloat() * this.getBbHeight()),
                        this.getZ() + (double)(this.random.nextFloat() * this.getBbWidth() * 2.0F) - (double)this.getBbWidth(),
                        1, dx, dy, dz, 0.0D);
            }
        }
    }

    private void fireDanmaku()
    {
        int level = Math.max(0, Math.min(THKaguyaConfig.danmakuLevel, 4));
        float size = 0.1F + level * 0.1F;
        // 原版 ways = {8,12,20,32,44}，直接用 danmakuLevel(0..4) 索引（EASY/NORMAL/HARD/LUNATIC/第 5 档）
        int[] ways = {8, 12, 20, 32, 44};
        int way = ways[level];
        float damage = 5.0F;

        Vec3 angle = THShotLib.angle_Random();
        ShotData shot = ShotData.shot(FORM_LIGHT, GREEN, size, this.isPowered() ? damage * 2F : damage);

        if(this.isPowered())
        {
            THShotLib.createSphereShot(this, this, THShotLib.pos_Living(this), angle, 0F, 1.2D, 0.01D, 0.00D, THShotLib.gravity(0.0D, -0.02D, 0.0D), shot, way, 0.0D, 0F);
            shot.color = RED;
            THShotLib.createSphereShot(this, this, THShotLib.pos_Living(this), angle, 0F, 0.5D, 0.01D, 0.00D, THShotLib.gravity(0.0D, -0.02D, 0.0D), shot, way, 0.0D, 0F);
            shot.color = YELLOW;
            THShotLib.createSphereShot(this, this, THShotLib.pos_Living(this), angle, 0F, 0.4D, 0.01D, 0.00D, THShotLib.gravity(0.0D, -0.02D, 0.0D), shot, way, 0.0D, 0F);
        }
        else
        {
            THShotLib.createSphereShot(this, this, THShotLib.pos_Living(this), angle, 0F, 0.6D, 0.01D, 0.00D, THShotLib.gravity(0.0D, -0.02D, 0.0D), shot, way, 0.0D, 0F);
            shot.color = RED;
            THShotLib.createSphereShot(this, this, THShotLib.pos_Living(this), angle, 0F, 0.5D, 0.01D, 0.00D, THShotLib.gravity(0.0D, -0.02D, 0.0D), shot, way, 0.0D, 0F);
            shot.color = YELLOW;
            THShotLib.createSphereShot(this, this, THShotLib.pos_Living(this), angle, 0F, 0.4D, 0.01D, 0.00D, THShotLib.gravity(0.0D, -0.02D, 0.0D), shot, way, 0.0D, 0F);
        }

        this.level().playSound(null, this.blockPosition(), SoundEvents.GENERIC_EXPLODE, SoundSource.HOSTILE, 4.0F, 1.0F);
    }
}
