# -*- coding: utf-8 -*-
"""从原版 THShotLib 提取激光生成方法，做 1.7.10 -> 1.20.1 API 转换，追加到移植版 THShotLib。"""
import re, os

BASE = r"E:\git\kaguya-mod-reborn-by-ai"
ORIG = os.path.join(BASE, "extract", "sources", "java", "thKaguyaMod", "THShotLib.java")
DST  = os.path.join(BASE, "thkaguya-1.20.1", "src", "main", "java", "thKaguyaMod", "THShotLib.java")

# 提取原版 1037-1509 行（createLaserA .. createSphereLaserA 结束，createLaserB 之前）
with open(ORIG, encoding="utf-8") as f:
    lines = f.read().splitlines()

# 找到 createLaserA 开始（第一个 "public static final EntityTHLaser createLaserA"）
start = None
end = None
for i, ln in enumerate(lines):
    if "public static final EntityTHLaser createLaserA" in ln and start is None:
        start = i
    if "createLaserB" in ln and "public static" in ln:
        end = i  # createLaserB 方法声明行
        break

assert start is not None and end is not None, f"未找到范围 start={start} end={end}"
body = lines[start:end]  # 不包含 createLaserB

# 去掉行尾的空格
body = [ln.rstrip() for ln in body]
# 去掉开头/结尾的空行
while body and not body[0].strip():
    body.pop(0)
while body and not body[-1].strip():
    body.pop()

text = "\n".join(body)

# ===== API 转换 =====
# 1. EntityLivingBase -> LivingEntity
text = text.replace("EntityLivingBase", "LivingEntity")
# 2. Vec3 字段 .xCoord/.yCoord/.zCoord -> .x/.y/.z（只针对变量名，不碰方法名）
text = re.sub(r"\.xCoord", ".x", text)
text = re.sub(r"\.yCoord", ".y", text)
text = re.sub(r"\.zCoord", ".z", text)
# 3. source.worldObj -> source.level()
text = text.replace("source.worldObj", "source.level()")
# 4. spawnEntityInWorld -> addFreshEntity
text = text.replace("spawnEntityInWorld", "addFreshEntity")
# 5. isRemote -> isClientSide（在 source.level() 上下文）
text = text.replace(".isRemote", ".isClientSide")

# 读目标文件
with open(DST, encoding="utf-8") as f:
    dst = f.read()

# 加 import EntityTHLaser（如果还没有）
if "import thKaguyaMod.entity.shot.EntityTHLaser;" not in dst:
    dst = dst.replace(
        "import thKaguyaMod.entity.shot.EntityTHShot;",
        "import thKaguyaMod.entity.shot.EntityTHLaser;\nimport thKaguyaMod.entity.shot.EntityTHShot;")

# 在最后一个 "}" 之前插入
idx = dst.rstrip().rfind("}")
assert idx >= 0
new_dst = dst[:idx] + "\n" + text + "\n\n" + dst[idx:]

with open(DST, "w", encoding="utf-8") as f:
    f.write(new_dst)

print(f"提取方法行数: {len(body)}")
print(f"追加完成，目标文件新大小: {len(new_dst)} 字符")
