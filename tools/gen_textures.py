# -*- coding: utf-8 -*-
"""阶段2：贴图映射 + 复制贴图 + 生成 item model + 方块 model/blockstates。

原版贴图在 extract/assets/thkaguyamod/textures/，命名 camelCase，
1.20.1 注册名 snake_case。本脚本建立映射并复制到新工程 assets/thkaguya/textures/。
"""
import os, shutil, json

BASE = os.path.dirname(os.path.abspath(__file__))
SRC = os.path.join(BASE, "..", "extract", "assets", "thkaguyamod", "textures")
DST = os.path.join(BASE, "..", "thkaguya-1.20.1", "src", "main", "resources",
                   "assets", "thkaguya", "textures")
MODEL_DIR = os.path.join(BASE, "..", "thkaguya-1.20.1", "src", "main", "resources",
                         "assets", "thkaguya", "models")

# 新注册名 -> 原版贴图相对路径（相对 textures/，不含 .png）
# 独立物品：多数在 items/ 下
item_textures = {
    # 五つの難題
    "hourai_jeweled_branch": "items/houraiEda",
    "dragon_neck_jewel": "items/ryuuTama",
    "buddha_stone_bowl": "items/hotokeHachi",
    "fire_rat_robe": "armor/hinezumi_1",
    "swallow_cowrie_shell": "items/koyasugai",
    "kinkakuji": "items/kinkakuzi",
    "aja_red_stone": "items/AjaRedStone",
    # 霊夢魔理沙
    "hakurei_miko_stick": "items/HakureiOharaibou",
    "homing_amulet": "items/homingAmulet",
    "yin_yang_orb": "items/material/Onmyoudama",
    "bloodthirsty_yin_yang_orb": "items/material/Onmyoudama",
    "magic_broom": "items/magicBroom",
    "mini_hakkero": "items/miniHakkero",
    "marisa_hat": "items/MarisaHat",
    # 紅魔郷
    "icicle_sword": "items/IcicleSword",
    "sakuya_watch": "items/sakuyaWatch",
    "sakuya_stopwatch": "items/SakuyaStopWatch",
    "gungnir": "items/Gungnir",
    "laevateinn": "items/Laevateinn",
    # 妖々夢
    "cursed_decoy_doll": "items/cursedDecoyDoll",
    "hakurouken": "items/hakurouken",
    "roukanken": "items/roukanSword",
    "yuyuko_fan": "items/yuyukoOugi",
    "soul_torch": "items/soulTorch",
    "gap": "items/sukima",
    "gap_folding_umbrella": "items/GapFoldingUmbrella_base",
    # 萃夢想
    "ibuki_gourd": "items/ibukihyou",
    # 花映塚
    "yuuka_parasol": "items/yuukaParasol",
    "death_scythe": "items/deathScythe",
    "remorse_rod": "items/kaigoStick_0",
    # 文花帖
    "tengu_fan": "items/tenguFan",
    # 風神録
    "kappa_cap": "items/kappaCap",
    "kappa_water_pistol": "items/kappaWaterPistol",
    "wind_miko_stick": "items/mikoStick_S",
    "illness_recovery_charm": "items/ByoukiheiyuMamori",
    "onbashira": "items/onbashira",
    "suwako_hat": "items/Kerobou",
    # 緋想天
    "heavenly_peach": "items/heavenlyPeach",
    "hisou_sword": "items/hisousword",
    # 地霊殿
    "third_eye": "items/thirdEye_0",
    "nuclear_control_rod": "items/nuclearControlRod",
    "closed_third_eye": "items/closedThirdEye",
    # 星蓮船
    "nazrin_pendulum": "items/pendulum",
    "ship_ghost_dipper_empty": "items/hisyaku_0",
    "ship_ghost_dipper": "items/hisyaku_1",
    "sorcerer_sutra_scroll": "items/mazinKyoukan",
    # 神霊廟
    "wall_passing_chisel": "items/kabenuke",
    "toyosatomimi_sword": "items/toyosatomimiSword",
    # 輝針城
    "miracle_mallet": "items/UchidenoKoduchi",
    # その他
    "spiritual_strike_talisman": "items/SpiritualStrikeTalisman",
    # 基本アイテム
    "shot_material": "items/material/ShotMaterial",
    "point_item": "items/material/THPointItem",
    "bomb_item": "items/material/BombItem",
    "extend_item": "items/material/ExtendItem",
    "spell_card": "items/spellCard/spellcard_frame",
    # 真珠 8色
    "red_pearl": "items/material/RedPearl",
    "blue_pearl": "items/material/BluePearl",
    "green_pearl": "items/material/GreenPearl",
    "yellow_pearl": "items/material/YellowPearl",
    "purple_pearl": "items/material/PurplePearl",
    "aqua_pearl": "items/material/AquaPearl",
    "orange_pearl": "items/material/OrangePearl",
    "white_pearl": "items/material/WhitePearl",
    # 銀刀 4色
    "silver_knife_blue": "items/shot/SilverKnife_Blue",
    "silver_knife_red": "items/shot/SilverKnife_Red",
    "silver_knife_green": "items/shot/SilverKnife_Green",
    "silver_knife_white": "items/shot/SilverKnife_White",
    # 威力 2种
    "power_up_item_small": "items/material/SmallPowerUpItem",
    "power_up_item_big": "items/material/BigPowerUpItem",
    # 弾体 19种（在 items/shot/ 下）
    "small_shot": "items/shot/SmallShot",
    "tiny_shot": "items/shot/TinyShot",
    "medium_shot": "items/shot/MediumShot",
    "big_shot": "items/shot/BigShot",
    "star_shot": "items/shot/StarShot",
    "small_star_shot": "items/shot/SmallStarShot",
    "circle_shot": "items/shot/CircleShot",
    "scale_shot": "items/shot/ScaleShot",
    "butterfly_shot": "items/shot/ButterflyShot",
    "light_shot": "items/shot/LightShot",
    "knife_shot": "items/shot/KnifeShot",
    "heart_shot": "items/shot/HeartShot",
    "kunai_shot": "items/shot/KunaiShot",
    "talisman_shot": "items/shot/TalismanShot",
    "big_light_shot": "items/shot/BigLightShot",
    "rice_shot": "items/shot/RiceShot",
    "oval_shot": "items/shot/OvalShot",
    "crystal_shot": "items/shot/CrystalShot",
    "arrow_shot": "items/shot/ArrowShot",
    # レーザー 3种
    "middle_laser": "items/shot/MiddleLaser",
    "short_laser": "items/shot/ShortLaser",
    "long_laser": "items/shot/LongLaser",
}

# 方块贴图（blocks/ 下）
block_textures = {
    "danmaku_crafting_table": "blocks/danmaku_crafting_table",
    "laser_crafting_table": "blocks/danmaku_crafting_table2",
    "divine_spirit": "blocks/divinespirit_blue",
}


def main():
    copied = 0
    missing = []
    # 复制物品贴图
    item_dst = os.path.join(DST, "item")
    os.makedirs(item_dst, exist_ok=True)
    for name, rel in item_textures.items():
        src = os.path.join(SRC, rel + ".png")
        dst = os.path.join(item_dst, name + ".png")
        if os.path.exists(src):
            shutil.copy2(src, dst)
            copied += 1
        else:
            missing.append(("item", name, rel))

    # 复制方块贴图
    block_dst = os.path.join(DST, "block")
    os.makedirs(block_dst, exist_ok=True)
    for name, rel in block_textures.items():
        src = os.path.join(SRC, rel + ".png")
        dst = os.path.join(block_dst, name + ".png")
        if os.path.exists(src):
            shutil.copy2(src, dst)
            copied += 1
        else:
            missing.append(("block", name, rel))

    # 重新生成 item model（layer0 指向正确贴图）
    model_dir = os.path.join(MODEL_DIR, "item")
    os.makedirs(model_dir, exist_ok=True)
    for name in item_textures:
        model = {"parent": "minecraft:item/generated",
                 "textures": {"layer0": "thkaguya:item/" + name}}
        with open(os.path.join(model_dir, name + ".json"), "w", encoding="utf-8") as f:
            json.dump(model, f, ensure_ascii=False, indent=2)

    # 方块 model + blockstates
    bm_dir = os.path.join(MODEL_DIR, "block")
    bs_dir = os.path.join(BASE, "..", "thkaguya-1.20.1", "src", "main", "resources",
                          "assets", "thkaguya", "blockstates")
    os.makedirs(bm_dir, exist_ok=True)
    os.makedirs(bs_dir, exist_ok=True)
    for name in block_textures:
        bmodel = {"parent": "minecraft:block/cube_all",
                  "textures": {"all": "thkaguya:block/" + name}}
        with open(os.path.join(bm_dir, name + ".json"), "w", encoding="utf-8") as f:
            json.dump(bmodel, f, ensure_ascii=False, indent=2)
        bs = {"variants": {"": {"model": "thkaguya:block/" + name}}}
        with open(os.path.join(bs_dir, name + ".json"), "w", encoding="utf-8") as f:
            json.dump(bs, f, ensure_ascii=False, indent=2)
    # 方块物品 model（指向 block model）
    for name in block_textures:
        imodel = {"parent": "thkaguya:block/" + name}
        with open(os.path.join(model_dir, name + ".json"), "w", encoding="utf-8") as f:
            json.dump(imodel, f, ensure_ascii=False, indent=2)

    print("复制贴图:", copied, "张")
    if missing:
        print("缺失贴图（需人工处理）:")
        for kind, name, rel in missing:
            print("  [%s] %s -> %s.png" % (kind, name, rel))
    else:
        print("全部贴图映射成功")
    print("生成 item model:", len(item_textures), "个")
    print("生成 block model + blockstates:", len(block_textures), "套")


if __name__ == "__main__":
    main()
