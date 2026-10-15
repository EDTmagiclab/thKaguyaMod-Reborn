# -*- coding: utf-8 -*-
"""阶段0/1/2 静态校验：JSON 语法、配方物品引用、模型贴图完整性。

不启动游戏，快速发现资源层问题。
"""
import os, json, glob, re

BASE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.join(BASE, "..", "thkaguya-1.20.1", "src", "main", "resources")

errors = []
warnings = []

# 1. 从 THKaguyaItems.java 提取注册物品名
items_java = os.path.join(BASE, "..", "thkaguya-1.20.1", "src", "main", "java",
                          "thKaguyaMod", "init", "THKaguyaItems.java")
content = open(items_java, encoding="utf-8").read()
registered_items = set(re.findall(r'register\("([a-z0-9_]+)"\)', content))
# 方块物品（在 THKaguyaBlocks 里注册到 ITEMS）
blocks_java = os.path.join(BASE, "..", "thkaguya-1.20.1", "src", "main", "java",
                           "thKaguyaMod", "init", "THKaguyaBlocks.java")
bcontent = open(blocks_java, encoding="utf-8").read()
block_items = set(re.findall(r'registerBlockItem\(\s*"([a-z0-9_]+)"', bcontent))
registered_items |= block_items
print("已注册物品（含方块物品）:", len(registered_items))

# 2. 校验所有 JSON 语法
json_files = glob.glob(os.path.join(ROOT, "**", "*.json"), recursive=True)
print("JSON 文件总数:", len(json_files))
for jf in json_files:
    try:
        json.load(open(jf, encoding="utf-8"))
    except Exception as e:
        errors.append("JSON 语法错误: %s -> %s" % (os.path.relpath(jf, ROOT), e))

# 3. 配方引用校验
recipe_dir = os.path.join(ROOT, "data", "thkaguya", "recipes")
referenced = set()
result_items = set()
for rf in glob.glob(os.path.join(recipe_dir, "*.json")):
    r = json.load(open(rf, encoding="utf-8"))
    # 收集 key 里的 item
    if "key" in r:
        for k, v in r["key"].items():
            if isinstance(v, dict) and "item" in v:
                referenced.add(v["item"])
            elif isinstance(v, list):
                for x in v:
                    if isinstance(x, dict) and "item" in x:
                        referenced.add(x["item"])
    # shapeless ingredients
    if "ingredients" in r:
        for ing in r["ingredients"]:
            if isinstance(ing, dict) and "item" in ing:
                referenced.add(ing["item"])
    # 结果
    if "result" in r:
        res = r["result"]
        if isinstance(res, dict) and "item" in res:
            result_items.add(res["item"])

# 校验本 mod 物品引用
for item in sorted(referenced | result_items):
    if item.startswith("thkaguya:"):
        name = item.split(":")[1]
        if name not in registered_items:
            errors.append("配方引用了未注册物品: %s" % item)

# 校验 minecraft 物品（1.20.1 名称合理性）
mc_refs = sorted(set(x for x in referenced if x.startswith("minecraft:")))
print("配方引用的 minecraft 物品:", len(mc_refs), "种")

# 4. 模型/贴图完整性
models = set()
for mf in glob.glob(os.path.join(ROOT, "assets", "thkaguya", "models", "item", "*.json")):
    models.add(os.path.basename(mf)[:-5])
textures = set()
for tf in glob.glob(os.path.join(ROOT, "assets", "thkaguya", "textures", "item", "*.png")):
    textures.add(os.path.basename(tf)[:-4])

# 每个注册物品应有模型
for item in sorted(registered_items):
    if item not in models:
        warnings.append("注册物品缺少 item model: %s" % item)

# 每个模型 layer0 应指向存在的贴图
for mf in glob.glob(os.path.join(ROOT, "assets", "thkaguya", "models", "item", "*.json")):
    m = json.load(open(mf, encoding="utf-8"))
    if "textures" in m and "layer0" in m["textures"]:
        layer = m["textures"]["layer0"]
        # layer0 形如 "thkaguya:item/xxx"，取最后一段
        tname = layer.split("/")[-1]
        if tname not in textures:
            warnings.append("模型 %s 的贴图不存在: %s" % (os.path.basename(mf), layer))

print("\n========== 校验结果 ==========")
print("错误:", len(errors))
for e in errors:
    print("  [错误]", e)
print("警告:", len(warnings))
for w in warnings:
    print("  [警告]", w)

if not errors and not warnings:
    print("✅ 全部通过")
elif not errors:
    print("⚠️ 有警告但无致命错误")
else:
    print("❌ 存在需修复的错误")
