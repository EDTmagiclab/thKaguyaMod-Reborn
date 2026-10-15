# -*- coding: utf-8 -*-
"""把原版 109 条配方中的「非符卡」部分生成为 1.20.1 JSON 配方。

1.20.1 配方 JSON 采用 shaped / shapeless 两种，pattern 字母 → 物品 key。
原版物品名 → 1.20.1 注册名映射：
  - 本 mod 物品：原版 unlocalizedName 转 snake_case（已拆分 metadata 变体）
  - 原版 Minecraft 物品：1.7.10 名 → 1.20.1 名
"""
import os, json

BASE = os.path.dirname(os.path.abspath(__file__))
RECIPE_DIR = os.path.join(BASE, "..", "thkaguya-1.20.1", "src", "main", "resources",
                          "data", "thkaguya", "recipes")

# 原版 Minecraft 物品 1.7.10 → 1.20.1
MC_ITEMS = {
    "redstone": "minecraft:redstone",
    "ender_pearl": "minecraft:ender_pearl",
    "emerald": "minecraft:emerald",
    "glowstone_dust": "minecraft:glowstone_dust",
    "blaze_powder": "minecraft:blaze_powder",
    "gold_ingot": "minecraft:gold_ingot",
    "diamond": "minecraft:diamond",
    "leather_chestplate": "minecraft:leather_chestplate",
    "leather_helmet": "minecraft:leather_helmet",
    "egg": "minecraft:egg",
    "wheat": "minecraft:wheat",
    "feather": "minecraft:feather",
    "ghast_tear": "minecraft:ghast_tear",
    "lava_bucket": "minecraft:lava_bucket",
    "water_bucket": "minecraft:water_bucket",
    "bowl": "minecraft:bowl",
    "stick": "minecraft:stick",
    "iron_ingot": "minecraft:iron_ingot",
    "iron_sword": "minecraft:iron_sword",
    "clock": "minecraft:clock",
    "ender_eye": "minecraft:ender_eye",
    "paper": "minecraft:paper",
    "blaze_rod": "minecraft:blaze_rod",
    "flint_and_steel": "minecraft:flint_and_steel",
    "golden_sword": "minecraft:golden_sword",
    "gunpowder": "minecraft:gunpowder",
    "quartz": "minecraft:quartz",
    "nether_wart": "minecraft:nether_wart",
    "glass_bottle": "minecraft:glass_bottle",
    "slime_ball": "minecraft:slime_ball",
    "rotten_flesh": "minecraft:rotten_flesh",
}

# 原版 Minecraft 方块 1.7.10 → 1.20.1
MC_BLOCKS = {
    "obsidian": "minecraft:obsidian",
    "ice": "minecraft:ice",
    "glowstone": "minecraft:glowstone",
    "planks": "minecraft:oak_planks",
    "gold_block": "minecraft:gold_block",
    "soul_sand": "minecraft:soul_sand",
    "lapis_block": "minecraft:lapis_block",
    "redstone_block": "minecraft:redstone_block",
    "log": "minecraft:oak_log",
    "crafting_table": "minecraft:crafting_table",
    "yellow_flower": "minecraft:dandelion",
    "red_flower": "minecraft:poppy",
    "waterlily": "minecraft:lily_pad",
    "leaves": "minecraft:oak_leaves",
    "glass": "minecraft:glass",
    "piston": "minecraft:piston",
    "tripwire_hook": "minecraft:tripwire_hook",
    "packed_ice": "minecraft:packed_ice",
    "wool": "minecraft:white_wool",
}

# 原版 dye (metadata) → 1.20.1 具体染料
DYES = {
    0: "minecraft:ink_sac",       # 墨囊（原版 dye:0）
    1: "minecraft:red_dye",
    4: "minecraft:lapis_lazuli",  # 青金石（原版 dye:4）
    14: "minecraft:orange_dye",
    15: "minecraft:bone_meal",    # 骨粉（原版 dye:15）
}

def mc_item(orig):
    if orig in MC_ITEMS:
        return MC_ITEMS[orig]
    if orig in MC_BLOCKS:
        return MC_BLOCKS[orig]
    return None

def th_item(unlocalized):
    """原版 unlocalizedName → 1.20.1 注册名（snake_case）"""
    import re
    s = re.sub(r'(?<!^)(?=[A-Z])', '_', unlocalized).lower()
    s = s.replace('_-_', '_').replace('-', '_')
    return "thkaguya:" + s

# 配方定义：(输出注册名, 数量, pattern, key映射[字母 -> (type, value)])
# type: 'item' | 'block' | 'dye' | 'th'
# 这里直接存最终 json 能用的字符串

recipes = []

def shaped(out, count, pattern, keys):
    """keys: {字母: minecraft 物品名 or (dye, meta) or ('th', unlocalized)}"""
    recipe = {"type": "minecraft:crafting_shaped", "pattern": pattern}
    kk = {}
    for letter, val in keys.items():
        if isinstance(val, tuple) and val[0] == "dye":
            item = DYES.get(val[1])
        elif isinstance(val, tuple) and val[0] == "th":
            item = th_item(val[1])
        else:
            item = mc_item(val) or val
        kk[letter] = {"item": item}
    recipe["key"] = kk
    recipe["result"] = {"item": out, "count": count}
    recipes.append(recipe)

def shapeless(out, count, ingredients):
    recipe = {"type": "minecraft:crafting_shapeless", "ingredients": ingredients,
              "result": {"item": out, "count": count}}
    recipes.append(recipe)

# ============ 珍珠 8 色 ============
for name, mat in [("red_pearl", "minecraft:redstone"), ("blue_pearl", ("dye", 4)),
                  ("green_pearl", "minecraft:emerald"), ("yellow_pearl", "minecraft:glowstone_dust"),
                  ("purple_pearl", "minecraft:obsidian"), ("aqua_pearl", "minecraft:ice"),
                  ("orange_pearl", "minecraft:blaze_powder")]:
    shaped("thkaguya:" + name, 1, ["XXX", "XYX", "XXX"],
           {"X": mat, "Y": "minecraft:ender_pearl"})
shaped("thkaguya:white_pearl", 1, ["XXX", "XYX", "XXX"],
       {"X": "minecraft:quartz", "Y": "minecraft:ender_pearl"})

# ============ 五つの難題 本体 ============
shaped("thkaguya:hourai_jeweled_branch", 1, ["YRB", "GEP", " E "],
       {"Y": ("th", "yellow_pearl"), "R": ("th", "red_pearl"), "B": ("th", "blue_pearl"),
        "G": ("th", "green_pearl"), "P": ("th", "purple_pearl"), "E": "minecraft:gold_ingot"})
shaped("thkaguya:buddha_stone_bowl", 1, ["DOD", "O O", "DOD"],
       {"D": "minecraft:diamond", "O": "minecraft:obsidian"})
shaped("thkaguya:fire_rat_robe", 1, ["BDB", "BPB", "B B"],
       {"B": "minecraft:blaze_powder", "D": "minecraft:diamond", "P": "minecraft:leather_chestplate"})
shaped("thkaguya:swallow_cowrie_shell", 1, ["GGG", "FEF", "WWW"],
       {"E": "minecraft:egg", "W": "minecraft:wheat", "F": "minecraft:feather", "G": "minecraft:ghast_tear"})
shaped("thkaguya:dragon_neck_jewel", 1, ["GWG", "LDL", "GWG"],
       {"D": ("th", "aqua_pearl"), "L": "minecraft:lava_bucket", "W": "minecraft:water_bucket",
        "G": "minecraft:glowstone"})
shaped("thkaguya:kinkakuji", 1, ["WWW", "GGG"],
       {"W": "minecraft:oak_planks", "G": "minecraft:gold_block"})
shaped("minecraft:gold_block", 3, ["K"], {"K": ("th", "kinkakuji")})
shaped("thkaguya:aja_red_stone", 1, ["RRR", "RRR", "RRR"], {"R": ("th", "red_pearl")})

# ============ 星蓮船 / 花映塚 ============
shaped("thkaguya:ship_ghost_dipper", 1, [" SB", " WS", "W  "],
       {"B": "minecraft:bowl", "W": "minecraft:stick", "S": "minecraft:soul_sand"})
shaped("thkaguya:death_scythe", 1, ["III", " S ", "S  "],
       {"I": "minecraft:iron_ingot", "S": "minecraft:soul_sand"})
shaped("thkaguya:remorse_rod", 1, ["WPB", "WPB", " S "],
       {"W": ("dye", 0), "B": ("dye", 15), "S": "minecraft:stick", "P": "minecraft:oak_planks"})
shaped("thkaguya:nazrin_pendulum", 1, [" I ", "I I", " D "],
       {"I": "minecraft:iron_ingot", "D": "minecraft:diamond"})

# ============ 妖々夢 ============
shaped("thkaguya:roukanken", 1, ["  I", "FI ", "S  "],
       {"I": "minecraft:iron_ingot", "F": ("th", "soul_torch"), "S": "minecraft:iron_sword"})
shaped("thkaguya:hakurouken", 1, ["  I", "FI ", "S  "],
       {"I": "minecraft:iron_ingot", "F": ("th", "soul_torch"), "S": "minecraft:stick"})

# ============ 紅魔郷 ============
shaped("thkaguya:sakuya_watch", 1, [" Y ", "RCR", " R "],
       {"Y": ("th", "yellow_pearl"), "R": "minecraft:redstone", "C": "minecraft:clock"})
shaped("thkaguya:silver_knife_blue", 1, ["I ", " R"],
       {"R": ("dye", 4), "I": "minecraft:iron_ingot"})
shaped("thkaguya:silver_knife_red", 1, ["I ", " R"],
       {"R": "minecraft:redstone", "I": "minecraft:iron_ingot"})
shaped("thkaguya:silver_knife_green", 1, ["I ", " R"],
       {"R": "minecraft:emerald", "I": "minecraft:iron_ingot"})

# ============ スキマ / 第三の眼 等 ============
shaped("thkaguya:gap", 2, [" KR", "KEK", "RK "],
       {"K": "minecraft:obsidian", "R": ("dye", 1), "E": "minecraft:ender_eye"})
shaped("thkaguya:third_eye", 1, [" X ", "XYX", " X "],
       {"X": ("dye", 1), "Y": "minecraft:ender_eye"})
shaped("thkaguya:sorcerer_sutra_scroll", 1, ["BRB", "BRB", "BRB"],
       {"B": "minecraft:lapis_block", "R": "minecraft:redstone_block"})
shaped("thkaguya:hisou_sword", 1, [" PG", "PGP", "RP "],
       {"P": "minecraft:blaze_powder", "G": "minecraft:glowstone", "R": "minecraft:blaze_rod"})
shaped("thkaguya:yuyuko_fan", 1, ["RDL", "SPS", "TST"],
       {"D": "minecraft:diamond", "P": "minecraft:paper", "S": "minecraft:stick",
        "T": ("th", "soul_torch"), "R": "minecraft:redstone", "L": ("dye", 4)})
shaped("thkaguya:wind_miko_stick", 1, [" FP", " S ", "S  "],
       {"S": "minecraft:stick", "P": "minecraft:paper", "F": "minecraft:feather"})
shaped("thkaguya:onbashira", 1, ["PWP", " W ", " W "],
       {"W": "minecraft:oak_log", "P": "minecraft:paper"})
shaped("thkaguya:mini_hakkero", 1, ["IBI", "BFB", "IBI"],
       {"I": "minecraft:iron_ingot", "F": "minecraft:flint_and_steel", "B": "minecraft:blaze_rod"})
shaped("thkaguya:wall_passing_chisel", 1, ["GSS", "E  "],
       {"G": "minecraft:gold_ingot", "S": "minecraft:stick", "E": "minecraft:ender_pearl"})
shaped("thkaguya:toyosatomimi_sword", 1, ["DID", "DID", "DGD"],
       {"D": "minecraft:glowstone_dust", "I": "minecraft:iron_ingot", "G": "minecraft:gold_ingot"})
shaped("thkaguya:closed_third_eye", 1, ["OOO", "OTO", "OOO"],
       {"O": "minecraft:obsidian", "T": ("th", "third_eye")})
shaped("thkaguya:homing_amulet", 1, ["PPP", "PRP", "PPP"],
       {"P": "minecraft:paper", "R": ("dye", 1)})
shaped("thkaguya:yuuka_parasol", 1, ["YRO", " IR", "I Y"],
       {"O": ("th", "orange_pearl"), "I": "minecraft:iron_ingot",
        "Y": "minecraft:dandelion", "R": "minecraft:poppy"})
shaped("thkaguya:kappa_cap", 1, ["LWL", "SCS"],
       {"L": "minecraft:lily_pad", "W": "minecraft:water_bucket", "S": "minecraft:slime_ball",
        "C": "minecraft:leather_helmet"})
shaped("thkaguya:ibuki_gourd", 1, ["PLL", "LWL", "LLP"],
       {"P": "minecraft:paper", "L": "minecraft:lapis_block", "W": "minecraft:nether_wart"})
shaped("thkaguya:tengu_fan", 1, ["ORO", "LRR", "SLO"],
       {"O": ("dye", 14), "R": ("dye", 1), "L": "minecraft:oak_leaves", "S": "minecraft:stick"})
shaped("thkaguya:kappa_water_pistol", 1, ["LLL", "GGP", " TB"],
       {"L": ("dye", 4), "G": "minecraft:glass", "P": "minecraft:piston",
        "T": "minecraft:tripwire_hook", "B": "minecraft:glass_bottle"})
shaped("thkaguya:nuclear_control_rod", 1, ["BOB", "BOB", "BOB"],
       {"B": "minecraft:blaze_rod", "O": "minecraft:obsidian"})
shaped("thkaguya:yin_yang_orb", 1, ["RRQ", "REQ", "RQQ"],
       {"E": "minecraft:ender_pearl", "R": "minecraft:redstone", "Q": "minecraft:quartz"})
shaped("thkaguya:hakurei_miko_stick", 1, ["PPO", " SP", "S P"],
       {"P": "minecraft:paper", "S": "minecraft:stick", "O": ("th", "yin_yang_orb")})
shaped("thkaguya:marisa_hat", 1, [" B ", "WWW", "BBB"],
       {"B": "minecraft:black_wool", "W": "minecraft:white_wool"})
shaped("thkaguya:suwako_hat", 1, ["E E", " C "],
       {"E": "minecraft:ender_eye", "C": "minecraft:leather_helmet"})
shaped("thkaguya:gungnir", 1, ["LLR", " RL", "R L"],
       {"L": ("th", "middle_laser"), "R": "minecraft:redstone_block"})
shaped("thkaguya:laevateinn", 1, [" PB", "POP", "GP "],
       {"P": "minecraft:blaze_powder", "G": "minecraft:golden_sword",
        "O": ("th", "orange_pearl"), "B": "minecraft:blaze_rod"})

# ============ 弹体 / 激光 / 威力 ============
shaped("thkaguya:power_up_item_big", 1, ["PPP", "PPP", "PPP"],
       {"P": ("th", "power_up_item_small")})
shaped("thkaguya:power_up_item_small", 9, ["P"], {"P": ("th", "power_up_item_big")})
shapeless("thkaguya:shot_material", 64,
          [{"item": "minecraft:glowstone_dust"}, {"item": "minecraft:gunpowder"}])
# 各弹体（shot 变体）
shot_patterns = [
    ("tiny_shot", ["B"]),
    ("small_shot", ["BB"]),
    ("medium_shot", ["BB", "BB"]),
    ("big_shot", ["BBB", "BBB", "BBB"]),
    ("circle_shot", [" B ", "BBB", " B "]),
    ("small_star_shot", [" B ", "BBB", "B B"]),
    ("scale_shot", [" B ", "B B", "B B"]),
    ("butterfly_shot", ["B B", " B ", "B B"]),
    ("light_shot", ["B B", "   ", "B B"]),
    ("knife_shot", ["  B", "BB ", "BB "]),
    ("heart_shot", ["B B", "BBB", " B "]),
    ("kunai_shot", [" BB", " BB", "B  "]),
    ("talisman_shot", ["B", "B"]),
    ("rice_shot", [" B", "B "]),
    ("oval_shot", [" BB", "BBB", "BB "]),
    ("crystal_shot", ["  B", " B ", "B  "]),
    ("arrow_shot", [" B ", " B ", " B "]),
]
for name, pat in shot_patterns:
    shaped("thkaguya:" + name, 1, pat, {"B": ("th", "shot_material")})
# 星弹（二级合成）
shaped("thkaguya:star_shot", 1, ["BB"], {"B": ("th", "small_star_shot")})
shaped("thkaguya:big_light_shot", 1, ["BB"], {"B": ("th", "light_shot")})
# 激光
shaped("thkaguya:middle_laser", 1, ["L", "L", "L"], {"L": ("th", "light_shot")})
shaped("thkaguya:short_laser", 1, ["L", "L"], {"L": ("th", "light_shot")})
shaped("thkaguya:long_laser", 1, ["L", "L"], {"L": ("th", "short_laser")})

# ============ 弹幕工作台 ============
shaped("thkaguya:danmaku_crafting_table", 1, ["DDD", "DCD", "DDD"],
       {"D": ("th", "shot_material"), "C": "minecraft:crafting_table"})
shaped("thkaguya:laser_crafting_table", 1, ["DDD", "DCD", "DDD"],
       {"D": ("th", "light_shot"), "C": "minecraft:crafting_table"})

# 写入
os.makedirs(RECIPE_DIR, exist_ok=True)
written = []
for r in recipes:
    # 结果物品名作为文件名（去 namespace），去重加序号
    name = r["result"]["item"].split(":")[-1]
    written.append((name, r))

# 用结果名 + 索引命名，避免同名覆盖
from collections import Counter
cnt = Counter()
for name, r in written:
    cnt[name] += 1
    suffix = "" if cnt[name] == 1 else "_" + str(cnt[name])
    fname = name + suffix + ".json"
    with open(os.path.join(RECIPE_DIR, fname), "w", encoding="utf-8") as f:
        json.dump(r, f, ensure_ascii=False, indent=2)
    print("  ", fname)

print("共生成配方:", len(recipes))
