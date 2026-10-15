# -*- coding: utf-8 -*-
"""把原版 THShotLib 的弹体生成方法（createShot 系列 + ways 数组）提取并做 1.20.1 API 转换，追加到移植版 THShotLib。

转换规则：
  Vec3.createVectorHelper(x,y,z) -> new Vec3(x,y,z)
  .xCoord/.yCoord/.zCoord -> .x/.y/.z
  source.worldObj -> source.level()
  world.spawnEntityInWorld(e) -> level().addFreshEntity(e)
  world.isRemote -> level().isClientSide
  EntityLivingBase -> LivingEntity
"""
import re

SRC = r"E:\git\kaguya-mod-reborn-by-ai\extract\sources\java\thKaguyaMod\THShotLib.java"
DST = r"E:\git\kaguya-mod-reborn-by-ai\thkaguya-1.20.1\src\main\java\thKaguyaMod\THShotLib.java"

src = open(SRC, encoding="utf-8").read()

# 提取 ways 数组（122-173 行区间的内容）
ways_start = src.index("private static int ways[][]")
ways_end = src.index("//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////\n/*\t\t弾生成補助", ways_start)
ways_block = src[ways_start:ways_end].rstrip()

# 提取 createShot 到 createSphereShot 结束（createShot 定义在 211 行，到 createSphereShot 结束于 1018 行，激光前）
shot_start = src.index("public static final EntityTHShot createShot(EntityLivingBase user, Entity source, Vec3 pos,")
shot_end = src.index("/*\t\tレーザー生成系", shot_start)
shot_block = src[shot_start:shot_end].rstrip()

# 提取 playShotSound（发射音）
sound_start = src.index("public static void playShotSound(Entity entity)")
sound_end = src.index("}", sound_start) + 1
sound_block = src[sound_start:sound_end]

def convert(code):
    code = code.replace("Vec3.createVectorHelper", "new Vec3")
    code = re.sub(r"\.xCoord", ".x", code)
    code = re.sub(r"\.yCoord", ".y", code)
    code = re.sub(r"\.zCoord", ".z", code)
    code = code.replace("source.worldObj", "source.level()")
    code = code.replace("world.spawnEntityInWorld(", "level().addFreshEntity(")
    code = code.replace("world.isRemote", "level().isClientSide")
    code = code.replace("EntityLivingBase", "LivingEntity")
    code = code.replace("worldObj", "level()")
    return code

ways_block = convert(ways_block)
shot_block = convert(shot_block)
sound_block = convert(sound_block)

dst = open(DST, encoding="utf-8").read()

# 在最后一个 } 前插入
assert dst.rstrip().endswith("}"), "目标文件结尾异常"
insert_content = "\n\n" + ways_block + "\n\n" + shot_block + "\n\n" + sound_block + "\n"
dst = dst.rstrip()[:-1] + insert_content + "}\n"

open(DST, "w", encoding="utf-8").write(dst)
print("ways 数组块:", len(ways_block), "字符")
print("shot 生成块:", len(shot_block), "字符")
print("sound 块:", len(sound_block), "字符")
print("已追加到", DST)
