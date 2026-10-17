package thKaguyaMod.entity.living;

import static thKaguyaMod.DanmakuConstants.*;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
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
            return THKaguyaConfig.allowNaturalSpawn(this.getType());
        }
        return super.checkSpawnRules(level, reason);
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

    private void fireDanmaku()
    {
        int level = Math.max(1, Math.min(THKaguyaConfig.danmakuLevel, 4));
        float size = 0.1F + level * 0.1F;
        // 原版 ways = {8,12,20,32,44} 按 level 索引；44 超出迁移版 ways 数组范围，改用 level-1 索引（8/12/20/32）
        int[] ways = {8, 12, 20, 32};
        int way = ways[level - 1];
        float damage = 5.0F;

        Vec3 angle = THShotLib.angle_Random();
        ShotData shot = ShotData.shot(FORM_LIGHT, GREEN, size, damage);

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
