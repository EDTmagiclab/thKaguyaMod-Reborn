package thKaguyaMod.entity.living;

import static thKaguyaMod.DanmakuConstants.*;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import thKaguyaMod.ShotData;
import thKaguyaMod.THShotLib;
import thKaguyaMod.init.THKaguyaConfig;
import thKaguyaMod.init.THKaguyaEntities;

/** ハナビーパー：爆発時に弾幕をまき散らすクリーパー（1.20.1 移植版） */
public class EntityDanmakuCreeper extends Creeper
{
    /** 点燃后的弹幕引信计数（替代原版方块爆炸） */
    private int danmakuFuse;

    public EntityDanmakuCreeper(EntityType<? extends EntityDanmakuCreeper> type, Level level)
    {
        super(type, level);
        this.danmakuFuse = 0;
    }

    public EntityDanmakuCreeper(Level level)
    {
        this(THKaguyaEntities.DANMAKU_CREEPER.get(), level);
    }

    public static AttributeSupplier.Builder createAttributes()
    {
        return Creeper.createAttributes();
    }

    @Override
    public void tick()
    {
        super.tick();

        // 点燃后膨胀到引信耗尽时，用弹幕爆炸替代方块爆炸（原版注释掉了 createExplosion）
        if(!this.level().isClientSide && this.isIgnited())
        {
            this.danmakuFuse++;
            if(this.danmakuFuse >= 20)
            {
                this.fireDanmaku();
                this.discard();
            }
        }
        else
        {
            this.danmakuFuse = 0;
        }
    }

    private void fireDanmaku()
    {
        int level = Math.max(1, Math.min(THKaguyaConfig.danmakuLevel, 4));
        float size = 0.1F + level * 0.1F;
        int[] ways = {8, 12, 20, 32, 44};
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
