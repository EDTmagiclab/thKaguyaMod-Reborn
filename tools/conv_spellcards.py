# -*- coding: utf-8 -*-
"""批量迁移原版 THSC_* 符卡类到 1.20.1（实际写入版）"""
import os, re

SRC = r"E:\git\kaguya-mod-reborn-by-ai\extract\sources\java\thKaguyaMod\entity\spellcard"
DST = r"E:\git\kaguya-mod-reborn-by-ai\thkaguya-1.20.1\src\main\java\thKaguyaMod\entity\spellcard"

# 依赖未迁移类（3d-6/7 处理）→ 本批跳过
SKIP = {
    "THSC_Fafurotskies_no_Kiseki.java",  # EntityDanmakuCreeper(未迁移)
}

def conv(src):
    # ---- import 行 ----
    src = src.replace("import net.minecraft.util.Vec3;", "import net.minecraft.world.phys.Vec3;")
    src = src.replace("import net.minecraft.entity.Entity;", "import net.minecraft.world.entity.Entity;")
    src = src.replace("import net.minecraft.entity.EntityLivingBase;", "import net.minecraft.world.entity.LivingEntity;")
    src = src.replace("import net.minecraft.world.World;", "import net.minecraft.world.level.Level;")
    src = src.replace("import net.minecraft.util.MovingObjectPosition;", "import net.minecraft.world.phys.HitResult;")
    src = src.replace("import net.minecraft.util.MathHelper;", "import net.minecraft.util.Mth;")

    # ---- 正文类型 ----
    src = src.replace("EntityLivingBase", "LivingEntity")
    src = src.replace("MovingObjectPosition", "HitResult")
    src = re.sub(r"\bMathHelper\.", "Mth.", src)
    # World 类型正文（方法参数等）→ Level（import 已换成 level.Level，但正文类型名仍是 World）
    src = re.sub(r"\bWorld\b", "Level", src)

    # ---- LivingEntity / Entity API ----
    src = re.sub(r"\b(\w+)\.getLookVec\(\)", r"\1.getLookAngle()", src)
    src = re.sub(r"\b(\w+)\.posX\b", r"\1.getX()", src)
    src = re.sub(r"\b(\w+)\.posY\b", r"\1.getY()", src)
    src = re.sub(r"\b(\w+)\.posZ\b", r"\1.getZ()", src)
    src = re.sub(r"\b(\w+)\.motionX\b", r"\1.getDeltaMovement().x", src)
    src = re.sub(r"\b(\w+)\.motionY\b", r"\1.getDeltaMovement().y", src)
    src = re.sub(r"\b(\w+)\.motionZ\b", r"\1.getDeltaMovement().z", src)
    src = re.sub(r"\b(\w+)\.rotationYaw\b", r"\1.getYRot()", src)
    src = re.sub(r"\b(\w+)\.rotationPitch\b", r"\1.getXRot()", src)
    src = re.sub(r"\b(\w+)\.isDead\b", r"\1.isDeadOrDying()", src)
    src = re.sub(r"\b(\w+)\.height\b(?!\s*\*)", r"\1.getBbHeight()", src)
    src = re.sub(r"\b(\w+)\\.getDistanceToEntity\(", r"\1.distanceTo(", src)
    src = re.sub(r"\b(\w+)\.boundingBox\b", r"\1.getBoundingBox()", src)
    src = re.sub(r"\b(\w+)\.clearActivePotions\(\)", r"\1.removeAllEffects()", src)
    src = src.replace("worldObj.isRemote", "world.isClientSide")
    src = re.sub(r"\bworld\.isRemote\b", "world.isClientSide", src)
    src = re.sub(r"\bworld\.spawnEntityInWorld\(", r"world.addFreshEntity(", src)
    src = re.sub(r"\bworldObj\b", "world", src)
    src = re.sub(r"\bticksExisted\b", "tickCount", src)
    src = src.replace(".xCoord", ".x").replace(".yCoord", ".y").replace(".zCoord", ".z")
    src = re.sub(r"\bVec3\.createVectorHelper\(", "new Vec3(", src)

    # ---- EntityTHShot API（原版方法 → 1.20.1 版方法名）----
    src = re.sub(r"\.pos_Shot\(\)", ".pos()", src)
    src = re.sub(r"\.setDead\(\)", ".discard()", src)
    src = re.sub(r"\.setSpeed\(", ".setShotSpeed(", src)
    src = re.sub(r"\.setMotion\(\)", ".updateMotion()", src)
    src = re.sub(r"\.isEntityEqual\(", ".is(", src)

    # ---- 1.20.1 Vec3 是 final 字段，不能 lookAt.x = ... ----
    # 1.7 的 Vec3 可变 + rotateAroundY。用辅助函数重建向量。
    src = re.sub(
        r"(\w+)\.x\s*=([^;]+);\s*\n(\s*)\1\.y\s*=([^;]+);\s*\n\s*\1\.z\s*=([^;]+);\s*\n\s*\1\.rotateAroundY\(([^)]+)\);",
        lambda m: "%s = THSC_Helper.rotateY(THSC_Helper.vec3(%s, %s, %s), %s);" % (
            m.group(1), m.group(2).strip(), m.group(4).strip(), m.group(5).strip(), m.group(6)),
        src)

    # ---- getDeltaMovement().x *= / = 赋值 → setDeltaMovement 重建 ----
    def fix_delta(m):
        var = m.group(1)
        return var
    # NijuuKokushichou 模式：shot.getDeltaMovement().x *= 0.90D; y/z 同 → scale(0.90)
    src = re.sub(
        r"(\w+)\.getDeltaMovement\(\)\.x\s*\*=\s*([\d.]+D);\s*\n\s*\1\.getDeltaMovement\(\)\.y\s*\*=\s*[\d.]+D;\s*\n\s*\1\.getDeltaMovement\(\)\.z\s*\*=\s*[\d.]+D;",
        lambda m: "%s.setDeltaMovement(%s.getDeltaMovement().scale(0.90D));" % (m.group(1), m.group(1)),
        src)

    # ---- Level API ----
    src = re.sub(
        r"world\.getEntitiesWithinAABBExcludingEntity\((\w+),\s*(\w+)\.getBoundingBox\(\)\.addCoord\(0\.0D,\s*0\.0D,\s*0\.0D\)\.expand\(([^)]+)\)\)",
        r"world.getEntitiesOfClass(net.minecraft.world.entity.Entity.class, \2.getBoundingBox().inflate(\3))",
        src)

    # ---- playSoundAtEntity → 1.20 playSound ----
    # "thKaguyaMod.masterspark" → SoundEvents 注册查找成本高，先用占位音效（ masterspark SoundEvent 已在 EntityMiniHakkero 定义）
    src = re.sub(
        r'world\.playSoundAtEntity\((\w+),\s*"thKaguyaMod\.masterspark",\s*([\w.]+),\s*([\d.F]+)\);',
        r'thkaguyaMod.entity.item.EntityMiniHakkero.playSoundMasterSpark(world, \1, \2, \3);',
        src)
    src = re.sub(
        r'world\.playSoundAtEntity\((\w+),\s*"([\w.]+)",\s*([\w.]+),\s*([\d.F]+)\);',
        r'playSoundCompat(world, \1, "\2", (float)(\3), (float)(\4));',
        src)

    # ---- Potion 效果（YouryokuSpoiler）----
    src = src.replace("import net.minecraft.potion.PotionEffect;",
                      "import net.minecraft.world.effect.MobEffectInstance;\nimport net.minecraft.world.effect.MobEffects;")
    src = src.replace("living.addPotionEffect(new PotionEffect(20, 20 * 20, 1))",
                      "living.addEffect(new MobEffectInstance(MobEffects.WITHER, 20 * 20, 1))")
    src = src.replace("living.addPotionEffect(new PotionEffect(18, 20 * 20, 1))",
                      "living.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 20 * 20, 1))")
    src = src.replace("living.addPotionEffect(new PotionEffect(10, 20 * 20, 1))",
                      "living.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 20 * 20, 1))")
    src = src.replace("living.addPotionEffect(new PotionEffect(5, 20 * 20, 1))",
                      "living.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 20 * 20, 1))")
    return src

def main():
    files = sorted(f for f in os.listdir(SRC) if f.startswith("THSC_") and f.endswith(".java"))
    done, skipped = [], []
    for name in files:
        if name in SKIP:
            skipped.append(name)
            continue
        with open(os.path.join(SRC, name), encoding="utf-8") as f:
            src = f.read()
        out = conv(src)
        dst_path = os.path.join(DST, name)
        with open(dst_path, "w", encoding="utf-8", newline="\n") as f:
            f.write(out)
        done.append(name)
    print("converted:", len(done))
    print("skipped:", skipped)

if __name__ == "__main__":
    main()
