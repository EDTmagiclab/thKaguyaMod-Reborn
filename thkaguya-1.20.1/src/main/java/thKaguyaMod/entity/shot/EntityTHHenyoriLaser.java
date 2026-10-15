package thKaguyaMod.entity.shot;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

/**
 * へにょりレーザー系の共通処理（1.20.1 移植版，阶段 3d-3 简化）
 *
 * 原版有 13 段蛇形激光的 hitCheck + shotFinishBonus（生成弹材质 EntityItem）。
 * 当前复用 EntityTHLaser 的直线激光逻辑，蛇形特色与弹消材质留待阶段 4 完善。
 */
public class EntityTHHenyoriLaser extends EntityTHLaser
{
    public EntityTHHenyoriLaser(EntityType<?> type, Level level)
    {
        super(type, level);
    }
}
