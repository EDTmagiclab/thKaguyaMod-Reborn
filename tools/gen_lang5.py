# -*- coding: utf-8 -*-
"""生成 5 种语言的 1.20.1 lang JSON（en_US / ja_JP / ko_KR / zh_CN / zh_TW）。

原版 .lang 的键名是 camelCase 旧格式，映射到 1.20.1 的 snake_case 注册名。
复用 gen_lang_models.py 里的 independent_map 逻辑。
"""
import os, re, json

BASE = os.path.dirname(os.path.abspath(__file__))
ASSETS = os.path.join(BASE, "..", "thkaguya-1.20.1", "src", "main", "resources", "assets", "thkaguya")
LANG_SRC = os.path.join(BASE, "..", "extract", "assets", "thkaguyamod", "lang")

# 原版 unlocalizedName（或 item.<parent>.<variant>） -> 新注册名
name_map = {
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
    "thPowerItem": "power_item",
    "thShot": "shot_item",
    "thLaser": "laser_item",
    "houraiPearl": "hourai_pearl",
    "silverKnife": "silver_knife",
    "homingAmulet.homing": "homing_amulet",
    "homingAmulet.diffusion": "diffusion_amulet",
    "houraiPearl.red": "red_pearl",
    "houraiPearl.blue": "blue_pearl",
    "houraiPearl.green": "green_pearl",
    "houraiPearl.yellow": "yellow_pearl",
    "houraiPearl.purple": "purple_pearl",
    "houraiPearl.aqua": "aqua_pearl",
    "houraiPearl.orange": "orange_pearl",
    "houraiPearl.white": "white_pearl",
    "silverKnife.blue": "silver_knife_blue",
    "silverKnife.red": "silver_knife_red",
    "silverKnife.green": "silver_knife_green",
    "silverKnife.white": "silver_knife_white",
    "thPowerItem.small": "power_up_item_small",
    "thPowerItem.big": "power_up_item_big",
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
    "thLaser.middle": "middle_laser",
    "thLaser.short": "short_laser",
    "thLaser.long": "long_laser",
}

# 标签/方块名（各语言）
TAB_NAME = {
    "en_us": "Itutu no Nandai MOD+",
    "ja_jp": "五つの難題MOD+",
    "ko_kr": "五つの難題MOD+",
    "zh_cn": "五つの難題MOD+",
    "zh_tw": "五つの難題MOD+",
}
BLOCK_NAMES = {
    "danmaku_crafting_table": {
        "en_us": "Danmaku Crafting Table", "ja_jp": "弾幕作業台",
        "ko_kr": "탄막 작업대", "zh_cn": "弹幕工作台", "zh_tw": "彈幕工作台"},
    "laser_crafting_table": {
        "en_us": "Laser Crafting Table", "ja_jp": "レーザー弾幕作業台",
        "ko_kr": "레이저 작업대", "zh_cn": "激光弹幕工作台", "zh_tw": "雷射彈幕工作台"},
    "divine_spirit": {
        "en_us": "Divine Spirit", "ja_jp": "小神霊",
        "ko_kr": "소신령", "zh_cn": "小神霊", "zh_tw": "小神霊"},
}


def build(lang_file, out_name):
    src = os.path.join(LANG_SRC, lang_file)
    d = {}
    d["itemGroup.thkaguya"] = TAB_NAME[out_name]
    for bname, tr in BLOCK_NAMES.items():
        d["block.thkaguya." + bname] = tr.get(out_name, tr["en_us"])

    lines = open(src, encoding="utf-8").read().splitlines()
    for line in lines:
        if "=" not in line:
            continue
        k, v = line.split("=", 1)
        k = k.strip()
        v = v.strip()
        if not v or not k.startswith("item.") or not k.endswith(".name"):
            continue
        mid = k[len("item."):-len(".name")]
        if mid in name_map:
            d["item.thkaguya." + name_map[mid]] = v
    return d


if __name__ == "__main__":
    lang_dir = os.path.join(ASSETS, "lang")
    os.makedirs(lang_dir, exist_ok=True)
    files = [("en_US.lang", "en_us"), ("ja_JP.lang", "ja_jp"),
             ("ko_KR.lang", "ko_kr"), ("zh_CN.lang", "zh_cn"),
             ("zh_TW.lang", "zh_tw")]
    for f, name in files:
        d = build(f, name)
        with open(os.path.join(lang_dir, name + ".json"), "w", encoding="utf-8") as out:
            json.dump(d, out, ensure_ascii=False, indent=2)
        print("%s: %d 条" % (name, len(d)))
