# -*- coding: utf-8 -*-
"""生成 1.20.1 lang 文件与物品/方块模型。

原版 lang 键名（camelCase / 旧格式）→ 1.20.1 键名（item.thkaguya.<snake_case>）。
"""
import os, json, re

BASE = os.path.dirname(os.path.abspath(__file__))
ASSETS = os.path.join(BASE, "..", "thkaguya-1.20.1", "src", "main", "resources", "assets", "thkaguya")
LANG_SRC = os.path.join(BASE, "..", "extract", "assets", "thkaguyamod", "lang")

def to_snake(s):
    s = re.sub(r'(?<!^)(?=[A-Z])', '_', s).lower()
    return s.replace('-', '_')

# 原版 unlocalizedName → 1.20.1 注册名（与 gen_items.py 一致）
# 独立物品：原版 item.X.name → item.thkaguya.X_snake
# 变体：原版 item.<parent>.<variant>.name → item.thkaguya.<variant>_snake

# 手动映射：原版 lang 键的 .name 前缀部分 → 新注册名
# 从 gen_items.py 的独立+变体清单构建

independent_names = [
    "houraiJewelBranch", "dragonNeckJewel", "buddhaStoneBowl", "fireRatRobe",
    "swallowCowrieShell", "kinkaku-ji", "ajaRedStone", "hakureiMikoStick",
    "homingAmulet", "yinYangOrb", "bloodthirstyYinYangOrb", "magicBroom",
    "miniHakkero", "marisaHat", "icicleSword", "sakuyaWatch", "sakuyaStopwatch",
    "spearTheGungnir", "laevateinn", "cursedDecoyDoll", "hakurouken", "roukanken",
    "yuyukoFan", "soulTorch", "gap", "gapFoldingUmbrella", "ibukiGourd",
    "yuukaParasol", "deathScythe", "remorseRod", "tenguFan", "kappaCap",
    "kappaWaterPistol", "windMikoStick", "byoukiheiyuMamori", "onbashira",
    "suwakoHat", "heavenlyPeach", "scarletPerceptionSword", "thirdEye",
    "nuclearControlRod", "closedThirdEye", "nazrinPendulum", "shipGhostDipperEmpty",
    "shipGhostDipper", "sorcererSutraScroll", "wallPassingChisel", "toyosatomimiSword",
    "miracleMallet", "spiritualStrikeTalisman", "shotMaterial", "thPointItem",
    "thBombItem", "thExtendItem", "thSpellCard",
]

# 原版 unlocalizedName → 新注册名（独立物品）
independent_map = {
    "houraiJewelBranch": "hourai_jeweled_branch",
    "dragonNeckJewel": "dragon_neck_jewel",
    "buddhaStoneBowl": "buddha_stone_bowl",
    "fireRatRobe": "fire_rat_robe",
    "swallowCowrieShell": "swallow_cowrie_shell",
    "kinkaku-ji": "kinkakuji",
    "ajaRedStone": "aja_red_stone",
    "hakureiMikoStick": "hakurei_miko_stick",
    "homingAmulet": "homing_amulet",
    "yinYangOrb": "yin_yang_orb",
    "bloodthirstyYinYangOrb": "bloodthirsty_yin_yang_orb",
    "magicBroom": "magic_broom",
    "miniHakkero": "mini_hakkero",
    "marisaHat": "marisa_hat",
    "icicleSword": "icicle_sword",
    "sakuyaWatch": "sakuya_watch",
    "sakuyaStopwatch": "sakuya_stopwatch",
    "spearTheGungnir": "gungnir",
    "laevateinn": "laevateinn",
    "cursedDecoyDoll": "cursed_decoy_doll",
    "hakurouken": "hakurouken",
    "roukanken": "roukanken",
    "yuyukoFan": "yuyuko_fan",
    "soulTorch": "soul_torch",
    "gap": "gap",
    "gapFoldingUmbrella": "gap_folding_umbrella",
    "ibukiGourd": "ibuki_gourd",
    "yuukaParasol": "yuuka_parasol",
    "deathScythe": "death_scythe",
    "remorseRod": "remorse_rod",
    "tenguFan": "tengu_fan",
    "kappaCap": "kappa_cap",
    "kappaWaterPistol": "kappa_water_pistol",
    "windMikoStick": "wind_miko_stick",
    "byoukiheiyuMamori": "illness_recovery_charm",
    "onbashira": "onbashira",
    "suwakoHat": "suwako_hat",
    "heavenlyPeach": "heavenly_peach",
    "scarletPerceptionSword": "hisou_sword",
    "thirdEye": "third_eye",
    "nuclearControlRod": "nuclear_control_rod",
    "closedThirdEye": "closed_third_eye",
    "nazrinPendulum": "nazrin_pendulum",
    "shipGhostDipperEmpty": "ship_ghost_dipper_empty",
    "shipGhostDipper": "ship_ghost_dipper",
    "sorcererSutraScroll": "sorcerer_sutra_scroll",
    "wallPassingChisel": "wall_passing_chisel",
    "toyosatomimiSword": "toyosatomimi_sword",
    "miracleMallet": "miracle_mallet",
    "spiritualStrikeTalisman": "spiritual_strike_talisman",
    "shotMaterial": "shot_material",
    "thPointItem": "point_item",
    "thBombItem": "bomb_item",
    "thExtendItem": "extend_item",
    "thSpellCard": "spell_card",
    # 变体（原版用 item.<parent>.<variant>.name 表示）
    # 珍珠
    "houraiPearl.red": "red_pearl",
    "houraiPearl.blue": "blue_pearl",
    "houraiPearl.green": "green_pearl",
    "houraiPearl.yellow": "yellow_pearl",
    "houraiPearl.purple": "purple_pearl",
    "houraiPearl.aqua": "aqua_pearl",
    "houraiPearl.orange": "orange_pearl",
    "houraiPearl.white": "white_pearl",
    # 银刀
    "silverKnife.blue": "silver_knife_blue",
    "silverKnife.red": "silver_knife_red",
    "silverKnife.green": "silver_knife_green",
    "silverKnife.white": "silver_knife_white",
    # 威力
    "thPowerItem.small": "power_up_item_small",
    "thPowerItem.big": "power_up_item_big",
    # 弹体
    "thShot.smallShot": "small_shot",
    "thShot.tinyShot": "tiny_shot",
    "thShot.mediumShot": "medium_shot",
    "thShot.bigShot": "big_shot",
    "thShot.starShot": "star_shot",
    "thShot.smallStarShot": "small_star_shot",
    "thShot.circleShot": "circle_shot",
    "thShot.scaleShot": "scale_shot",
    "thShot.butterflyShot": "butterfly_shot",
    "thShot.lightShot": "light_shot",
    "thShot.knifeShot": "knife_shot",
    "thShot.heartShot": "heart_shot",
    "thShot.kunaiShot": "kunai_shot",
    "thShot.talismanShot": "talisman_shot",
    "thShot.bigLightShot": "big_light_shot",
    "thShot.riceShot": "rice_shot",
    "thShot.ovalShot": "oval_shot",
    "thShot.crystalShot": "crystal_shot",
    "thShot.arrowShot": "arrow_shot",
    # 激光
    "thLaser.middle": "middle_laser",
    "thLaser.short": "short_laser",
    "thLaser.long": "long_laser",
}

def build_zh_cn():
    """从原版 zh_CN.lang 提取物品名，映射为新键名。"""
    src = os.path.join(LANG_SRC, "zh_CN.lang")
    out = []
    # 标签
    out.append("itemGroup.thkaguya=五つの難題MOD+")
    # 方块名
    out.append("block.thkaguya.danmaku_crafting_table=弹幕工作台")
    out.append("block.thkaguya.laser_crafting_table=激光弹幕工作台")
    out.append("block.thkaguya.divine_spirit=小神霊")

    lines = open(src, encoding="utf-8").read().splitlines()
    for line in lines:
        if "=" not in line:
            continue
        k, v = line.split("=", 1)
        k = k.strip()
        v = v.strip()
        if not v:
            continue
        # 只处理 item.<something>.name
        if not k.startswith("item.") or not k.endswith(".name"):
            continue
        mid = k[len("item."):-len(".name")]
        if mid in independent_map:
            newk = "item.thkaguya." + independent_map[mid]
            out.append("%s=%s" % (newk, v))
    return "\n".join(out) + "\n"

def build_models():
    """为每个注册物品生成 item model（generated 指向贴图）。"""
    models_dir = os.path.join(ASSETS, "models", "item")
    os.makedirs(models_dir, exist_ok=True)
    items = set(independent_map.values())
    items.update(["danmaku_crafting_table", "laser_crafting_table", "divine_spirit"])
    count = 0
    for name in sorted(items):
        model = {"parent": "minecraft:item/generated",
                 "textures": {"layer0": "thkaguya:item/" + name}}
        with open(os.path.join(models_dir, name + ".json"), "w", encoding="utf-8") as f:
            json.dump(model, f, ensure_ascii=False, indent=2)
        count += 1
    return count

if __name__ == "__main__":
    lang_dir = os.path.join(ASSETS, "lang")
    os.makedirs(lang_dir, exist_ok=True)
    zh = build_zh_cn()
    with open(os.path.join(lang_dir, "zh_cn.json"), "w", encoding="utf-8") as f:
        f.write(zh)
    print("zh_cn.json:", zh.count("\n"), "条")
    print("生成 item model:", build_models(), "个")
