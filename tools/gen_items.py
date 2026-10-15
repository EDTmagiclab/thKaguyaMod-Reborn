# -*- coding: utf-8 -*-
"""生成 THKaguyaItems.java（含 metadata 变体拆分）与配方 JSON。"""
import os

BASE = os.path.dirname(os.path.abspath(__file__))

# 独立物品（54 个）：(注册名, 中文注释)
independent = [
    ("hourai_jeweled_branch", "蓬莱の玉の枝"),
    ("dragon_neck_jewel", "龍の頸の玉"),
    ("buddha_stone_bowl", "仏の御石の鉢"),
    ("fire_rat_robe", "火鼠の皮衣"),
    ("swallow_cowrie_shell", "燕の子安貝"),
    ("kinkakuji", "金閣寺の一枚天井"),
    ("aja_red_stone", "エイジャの赤石"),
    ("hakurei_miko_stick", "博麗のお祓い棒"),
    ("homing_amulet", "ホーミングアミュレット"),
    ("yin_yang_orb", "陰陽玉"),
    ("bloodthirsty_yin_yang_orb", "血に餓えた陰陽玉"),
    ("magic_broom", "魔法の箒"),
    ("mini_hakkero", "ミニ八卦炉"),
    ("marisa_hat", "魔理沙の帽子"),
    ("icicle_sword", "アイシクルソード"),
    ("sakuya_watch", "咲夜の懐中時計"),
    ("sakuya_stopwatch", "ストップウォッチ"),
    ("gungnir", "スピア・ザ・グングニル"),
    ("laevateinn", "レーヴァテイン"),
    ("cursed_decoy_doll", "呪いのデコイ人形"),
    ("hakurouken", "白楼剣"),
    ("roukanken", "楼観剣"),
    ("yuyuko_fan", "幽々子の扇"),
    ("soul_torch", "人魂灯"),
    ("gap", "スキマ"),
    ("gap_folding_umbrella", "隙間の折りたたみ傘"),
    ("ibuki_gourd", "伊吹瓢"),
    ("yuuka_parasol", "幽香の傘"),
    ("death_scythe", "死神の大鎌"),
    ("remorse_rod", "悔悟の棒"),
    ("tengu_fan", "天狗の団扇"),
    ("kappa_cap", "河童の帽子"),
    ("kappa_water_pistol", "河童の水鉄砲"),
    ("wind_miko_stick", "風祝のお祓い棒"),
    ("illness_recovery_charm", "病気平癒守"),
    ("onbashira", "御柱"),
    ("suwako_hat", "諏訪子の帽子"),
    ("heavenly_peach", "天界の桃"),
    ("hisou_sword", "緋想の剣"),
    ("third_eye", "第三の眼"),
    ("nuclear_control_rod", "制御棒"),
    ("closed_third_eye", "閉じた第三の眼"),
    ("nazrin_pendulum", "ナズーリンペンデュラム"),
    ("ship_ghost_dipper_empty", "舟幽霊の柄杓（空）"),
    ("ship_ghost_dipper", "舟幽霊の柄杓（水入り）"),
    ("sorcerer_sutra_scroll", "魔人経巻"),
    ("wall_passing_chisel", "壁抜けの鑿"),
    ("toyosatomimi_sword", "豊聡耳の宝剣"),
    ("miracle_mallet", "打ち出の小槌"),
    ("spiritual_strike_talisman", "霊撃札"),
    ("shot_material", "弾幕の素"),
    ("point_item", "点符アイテム"),
    ("bomb_item", "ボムアイテム"),
    ("extend_item", "エクステンドアイテム"),
    ("spell_card", "スペルカード"),
]

# metadata 变体展开：(注册名, 中文注释, 组)
variants = [
    ("red_pearl", "赤真珠", "真珠"),
    ("blue_pearl", "青真珠", "真珠"),
    ("green_pearl", "緑真珠", "真珠"),
    ("yellow_pearl", "黄真珠", "真珠"),
    ("purple_pearl", "紫真珠", "真珠"),
    ("aqua_pearl", "水真珠", "真珠"),
    ("orange_pearl", "橙真珠", "真珠"),
    ("white_pearl", "白真珠", "真珠"),
    ("tiny_shot", "粒弾", "弾"),
    ("small_shot", "小弾", "弾"),
    ("medium_shot", "中弾", "弾"),
    ("big_shot", "大弾", "弾"),
    ("star_shot", "星弾", "弾"),
    ("small_star_shot", "小星弾", "弾"),
    ("circle_shot", "輪弾", "弾"),
    ("scale_shot", "鱗弾", "弾"),
    ("butterfly_shot", "蝶弾", "弾"),
    ("light_shot", "光弾", "弾"),
    ("knife_shot", "ナイフ弾", "弾"),
    ("heart_shot", "ハート弾", "弾"),
    ("kunai_shot", "クナイ弾", "弾"),
    ("talisman_shot", "札弾", "弾"),
    ("big_light_shot", "大光弾", "弾"),
    ("rice_shot", "米弾", "弾"),
    ("oval_shot", "楕円弾", "弾"),
    ("crystal_shot", "結晶弾", "弾"),
    ("arrow_shot", "矢弾", "弾"),
    ("short_laser", "ショートレーザー", "レーザー"),
    ("middle_laser", "レーザー", "レーザー"),
    ("long_laser", "ロングレーザー", "レーザー"),
    ("silver_knife_blue", "銀のナイフ 青", "銀ナイフ"),
    ("silver_knife_red", "銀のナイフ 赤", "銀ナイフ"),
    ("silver_knife_green", "銀のナイフ 緑", "銀ナイフ"),
    ("silver_knife_white", "タイムパラドックスナイフ", "銀ナイフ"),
    ("power_up_item_small", "パワーアップアイテム 小", "パワー"),
    ("power_up_item_big", "パワーアップアイテム 大", "パワー"),
]


def to_const(name):
    return name.upper()


def gen_items():
    lines = []
    lines.append("package thKaguyaMod.init;\n")
    lines.append("import net.minecraft.world.item.Item;")
    lines.append("import net.minecraftforge.registries.DeferredRegister;")
    lines.append("import net.minecraftforge.registries.ForgeRegistries;")
    lines.append("import net.minecraftforge.registries.RegistryObject;")
    lines.append("import thKaguyaMod.THKaguyaCore;\n")
    lines.append("/**")
    lines.append(" * アイテムの登録クラス（1.20.1 移植版）")
    lines.append(" * 原版 1.7.10 用 metadata(damage值) 区分变体，1.20.1 已废除 metadata，")
    lines.append(" * 故珍珠8色/弹体19种/激光3种/银刀4色/威力2种全部拆分为独立 Item。")
    lines.append(" * 阶段 1 仅完成注册；具体行为（武器/护甲/右键）在阶段 3/6 回填。")
    lines.append(" */")
    lines.append("public class THKaguyaItems")
    lines.append("{")
    lines.append("    public static final DeferredRegister<Item> ITEMS =")
    lines.append("            DeferredRegister.create(ForgeRegistries.ITEMS, THKaguyaCore.MODID);\n")

    # 独立物品
    lines.append("    /* ==================== 独立物品 ==================== */")
    for name, comment in independent:
        lines.append("    /** %s */" % comment)
        lines.append("    public static final RegistryObject<Item> %s = register(\"%s\");"
                     % (to_const(name), name))
        lines.append("")

    # 变体分组
    current_group = None
    for name, comment, group in variants:
        if group != current_group:
            current_group = group
            lines.append("    /* ==================== %s（metadata 变体拆分）==================== */" % group)
        lines.append("    /** %s */" % comment)
        lines.append("    public static final RegistryObject<Item> %s = register(\"%s\");"
                     % (to_const(name), name))
        lines.append("")

    lines.append("    private static RegistryObject<Item> register(String name)")
    lines.append("    {")
    lines.append("        return ITEMS.register(name, () -> new Item(new Item.Properties()));")
    lines.append("    }")
    lines.append("}")
    return "\n".join(lines)


def write(path, content):
    with open(path, "w", encoding="utf-8") as f:
        f.write(content)
    print("written:", path, len(content), "bytes")


if __name__ == "__main__":
    out = os.path.join(BASE, "..", "thkaguya-1.20.1",
                       "src", "main", "java", "thKaguyaMod", "init", "THKaguyaItems.java")
    write(out, gen_items())
    print("独立物品:", len(independent), "变体:", len(variants),
          "总计:", len(independent) + len(variants))
