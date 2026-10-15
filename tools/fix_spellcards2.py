# -*- coding: utf-8 -*-
"""修剩余 28 个点状编译错误（第二轮）"""
import re, os

DST = r"E:\git\kaguya-mod-reborn-by-ai\thkaguya-1.20.1\src\main\java\thKaguyaMod\entity\spellcard"

def patch(name, fn):
    p = os.path.join(DST, name)
    with open(p, encoding="utf-8") as f:
        s = f.read()
    s2 = fn(s)
    if s2 != s:
        with open(p, "w", encoding="utf-8", newline="\n") as f:
            f.write(s2)
        print("patched:", name)
    else:
        print("nochange:", name)

# FujiyamaVolcano: getDistanceToEntity / createExplosion
patch("THSC_FujiyamaVolcano.java", lambda s:
    s.replace("user.getDistanceToEntity(target)", "user.distanceTo(target)")
     .replace("world.createExplosion(shot, shot.getX(), shot.getY(), shot.getZ(), 1.0F, false);",
              "world.explode(shot, shot.getX(), shot.getY(), shot.getZ(), 1.0F, Level.ExplosionInteraction.NONE);"))

# GagoujiCyclone: danmakuMob.getTarget().posX 残留
patch("THSC_GagoujiCyclone.java", lambda s:
    s.replace("danmakuMob.getTarget().posX", "danmakuMob.getTarget().getX()")
     .replace("danmakuMob.getTarget().posY", "danmakuMob.getTarget().getY()")
     .replace("danmakuMob.getTarget().posZ", "danmakuMob.getTarget().getZ()"))

# HikouchuuNest: func_147447_a → clip
def fix_hikouchuu(s):
    return s.replace(
        "HitResult movingObjectPosition = world.func_147447_a(vec3d, vec3d1, false, true, true);",
        "HitResult movingObjectPosition = world.clip(new net.minecraft.world.level.ClipContext(vec3d, vec3d1,\n"
        "            net.minecraft.world.level.ClipContext.Block.COLLIDER, net.minecraft.world.level.ClipContext.Fluid.NONE, user));")
patch("THSC_HikouchuuNest.java", fix_hikouchuu)

# IcicleFall / LittleBugStorm: shot.gravity.y = X（Vec3 final 字段）→ 重建 gravity
def fix_gravity_y(s):
    # shot.gravity.y = GRAVITY_DEFAULT;  →  shot.gravity = new Vec3(shot.gravity.x, GRAVITY_DEFAULT, shot.gravity.z);
    return re.sub(r"(\w+)\.gravity\.y\s*=\s*([^;]+);",
                  r"\1.gravity = new Vec3(\1.gravity.x, \2, \1.gravity.z);", s)
patch("THSC_IcicleFall.java", fix_gravity_y)

def fix_lbs(s):
    s = re.sub(r"(\w+)\.angle\.y\s*=\s*([^;]+);",
               r"\1.angle = new Vec3(\1.angle.x, \2, \1.angle.z);", s)
    return s
patch("THSC_LittleBugStorm.java", fix_lbs)

# KappaPororoca: shot.bound(movingObjectPosition, 0.9D, 4) → 1.20.1 无此方法，反弹逻辑简化：撞方块即消
def fix_kappa(s):
    return s.replace("shot.bound(movingObjectPosition, 0.9D, 4);",
                     "// 原版 bound 反弹逻辑暂缺（阶段 4 完整化时补），先原地消弹\n\t\t\t\t\tshot.discard();")
patch("THSC_KappaPororoca.java", fix_kappa)

# MasterSpark: lookAt.x/y/z = ... + rotateAroundY → THSC_Helper 静态辅助
def fix_masterspark(s):
    s = re.sub(
        r"(\w+)\.x\s*=\s*([^;]+);\s*\n(\s*)\1\.y\s*=\s*([^;]+);\s*\n\s*\1\.z\s*=\s*([^;]+);\s*\n\s*\1\.rotateAroundY\(([^)]+)\);",
        lambda m: "%s = THSC_Helper.rotateY(THSC_Helper.vec3(%s, %s, %s), %s);" % (
            m.group(1), m.group(2).strip(), m.group(4).strip(), m.group(5).strip(), m.group(6)),
        s)
    return s
patch("THSC_MasterSpark.java", fix_masterspark)

# NonDirectionalLaser: thkaguyaMod 大小写错（脚本生成错误）→ THKaguyaMod 包名其实就是 thKaguyaMod
# 正确做法：masterspark 音效已在 EntityMiniHakkero 存在，改用 playSoundCompat 兜底
def fix_ndl(s):
    return s.replace(
        "thkaguyaMod.entity.item.EntityMiniHakkero.playSoundMasterSpark(world, user, THKaguyaConfig.MasterSparkVol, 1.0F);",
        "playSoundCompat(world, user, \"thkaguya:masterspark\", (float)THKaguyaConfig.MasterSparkVol, 1.0F);")
patch("THSC_NonDirectionalLaser.java", fix_ndl)

# StarbowBreak: isEndTime() → isShotEndTime(); getYRot() = ... → setYRot
def fix_starbow(s):
    s = s.replace("shot.isEndTime()", "shot.isShotEndTime()")
    s = re.sub(r"(\w+)\.getYRot\(\)\s*=\s*([^;]+);", r"\1.setYRot(\2);", s)
    s = re.sub(r"(\w+)\.getXRot\(\)\s*=\s*([^;]+);", r"\1.setXRot(\2);", s)
    return s
patch("THSC_StarbowBreak.java", fix_starbow)

# YouryokuSpoiler: playSoundCompat 未定义/potion/rotation 赋值/getDistanceToEntity
def fix_ys(s):
    s = s.replace('playSoundCompat(world, user, "portal.portal", (float)(2.0F), (float)(1.9F));',
                  'world.playSound(null, user.getX(), user.getY(), user.getZ(),\n'
                  '            net.minecraft.sounds.SoundEvents.PORTAL_TRAVEL, net.minecraft.sounds.SoundSource.PLAYERS, 2.0F, 1.9F);')
    s = re.sub(r"(\w+)\.getYRot\(\)\s*=\s*([^;]+);", r"\1.setYRot(\2);", s)
    s = re.sub(r"(\w+)\.getXRot\(\)\s*=\s*([^;]+);", r"\1.setXRot(\2);", s)
    s = s.replace("shot.getDistanceToEntity(shot.user)", "shot.distanceTo(shot.user)")
    return s
patch("THSC_YouryokuSpoiler.java", fix_ys)
print("done")
