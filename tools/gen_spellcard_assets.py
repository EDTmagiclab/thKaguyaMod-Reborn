#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
生成符卡物品图标模型 + 语言键（阶段 7-③）

数据来源（全部从现有工程/原版资产解析，不硬编码）：
  1) 符卡注册表     : thkaguya-1.20.1/src/main/java/thKaguyaMod/THKaguyaCore.java
                      commonSetup 里的 SpellCardRegistry.registerSpellCard(Xxx.class, "domain", "Name", id)
  2) 每卡 needLevel : thkaguya-1.20.1/src/main/java/thKaguyaMod/entity/spellcard/THSC_*.java 的 setNeedLevel(N)
  3) 符卡名（语言） : extract/assets/thkaguyamod/lang/<lang>.lang 的 item.thSpellCard.<id>.name
  4) 贴图存在性     : assets/thkaguya/textures/items/spellcard/<texname>.png

贴图命名规则对齐 RenderSpellCard.cardTexture()：
    name.toLowerCase().replace("'", "")

产出：
  - assets/thkaguya/models/item/spell_card.json          （overrides 入口）
  - assets/thkaguya/models/item/spell_card/<tex>.json    （卡面 layer0 + 等级边框 layer1）
  - 5 个 lang JSON 的 item.thkaguya.spell_card.<id> / itemGroup.thkaguya.spellcard / thKaguya.msg.sneSc1-4
"""

import json
import os
import re
import sys

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
PROJ = os.path.join(ROOT, "thkaguya-1.20.1")
JAVA = os.path.join(PROJ, "src", "main", "java", "thKaguyaMod")
ASSETS = os.path.join(PROJ, "src", "main", "resources", "assets", "thkaguya")
ORIG_LANG = os.path.join(ROOT, "extract", "assets", "thkaguyamod", "lang")
MODID = "thkaguya"

LANGS = ["en_us", "zh_cn", "zh_tw", "ja_jp", "ko_kr"]
ORIG_NAMES = {"en_us": "en_US", "zh_cn": "zh_CN", "zh_tw": "zh_TW", "ja_jp": "ja_JP", "ko_kr": "ko_KR"}


def parse_registry():
    """从 THKaguyaCore 解析 (id, className, registryName)"""
    src = open(os.path.join(JAVA, "THKaguyaCore.java"), encoding="utf-8").read()
    pat = re.compile(
        r'registerSpellCard\(\s*thKaguyaMod\.entity\.spellcard\.([A-Za-z0-9_]+)\.class\s*,'
        r'\s*"([^"]*)"\s*,\s*"([^"]*)"\s*,\s*(\d+)\s*\)')
    out = []
    for m in pat.finditer(src):
        out.append({"cls": m.group(1), "domain": m.group(2),
                    "name": m.group(3), "id": int(m.group(4))})
    return out


def parse_levels():
    """从 THSC_*.java 解析 className -> needLevel"""
    d = {}
    sdir = os.path.join(JAVA, "entity", "spellcard")
    for fn in os.listdir(sdir):
        if not fn.startswith("THSC_") or not fn.endswith(".java"):
            continue
        cls = fn[:-5]
        src = open(os.path.join(sdir, fn), encoding="utf-8").read()
        m = re.search(r"setNeedLevel\(\s*(\d+)\s*\)", src)
        if m:
            d[cls] = int(m.group(1))
    return d


def tex_name(registry_name):
    return registry_name.lower().replace("'", "")


def parse_orig_lang(lang):
    """原版 .lang -> {key: value}"""
    path = os.path.join(ORIG_LANG, ORIG_NAMES[lang] + ".lang")
    d = {}
    if not os.path.exists(path):
        return d
    with open(path, encoding="utf-8") as f:
        for line in f:
            line = line.strip()
            if not line or line.startswith("#") or "=" not in line:
                continue
            k, v = line.split("=", 1)
            d[k.strip()] = v.strip()
    return d


def main():
    cards = parse_registry()
    levels = parse_levels()

    if not cards:
        print("ERROR: 未解析到任何符卡注册", file=sys.stderr)
        return 1

    texdir = os.path.join(ASSETS, "textures", "items", "spellcard")
    existing = set(os.listdir(texdir)) if os.path.isdir(texdir) else set()

    model_dir = os.path.join(ASSETS, "models", "item", "spell_card")
    os.makedirs(model_dir, exist_ok=True)

    overrides = []
    report = []
    missing_tex = []

    for c in sorted(cards, key=lambda x: x["id"]):
        tn = tex_name(c["name"])
        lv = levels.get(c["cls"], 1)
        face = "%s.png" % tn
        border = "spellcard_lv%d.png" % lv
        if face not in existing:
            missing_tex.append(face)
        if border not in existing:
            missing_tex.append(border)

        # 每卡一个模型：layer0 = 卡面，layer1 = 该卡 needLevel 的边框
        model = {
            "parent": "minecraft:item/generated",
            "textures": {
                "layer0": "%s:items/spellcard/%s" % (MODID, tn),
                "layer1": "%s:items/spellcard/spellcard_lv%d" % (MODID, lv),
            },
        }
        with open(os.path.join(model_dir, tn + ".json"), "w", encoding="utf-8") as f:
            json.dump(model, f, ensure_ascii=False, indent=2)
            f.write("\n")

        overrides.append({
            "predicate": {"%s:spellcard_id" % MODID: c["id"]},
            "model": "%s:item/spell_card/%s" % (MODID, tn),
        })
        report.append((c["id"], c["cls"], c["name"], tn, lv))

    # 入口模型：默认层用通用 spell_card 贴图，overrides 按 damage 切换
    entry = {
        "parent": "minecraft:item/generated",
        "textures": {"layer0": "%s:item/spell_card" % MODID},
        "overrides": overrides,
    }
    with open(os.path.join(ASSETS, "models", "item", "spell_card.json"), "w", encoding="utf-8") as f:
        json.dump(entry, f, ensure_ascii=False, indent=2)
        f.write("\n")

    # ---- 语言键 ----
    lang_dir = os.path.join(ASSETS, "lang")
    lang_stat = []
    for lang in LANGS:
        p = os.path.join(lang_dir, lang + ".json")
        data = json.load(open(p, encoding="utf-8"))
        orig = parse_orig_lang(lang)

        added = 0
        for c in cards:
            key = "item.%s.spell_card.%d" % (MODID, c["id"])
            if key in data:
                continue
            val = orig.get("item.thSpellCard.%d.name" % c["id"])
            if val is None:
                val = orig.get("item.thSpellCard.name", "Spell Card") + " #%d" % c["id"]
            data[key] = val
            added += 1

        # 创造标签标题
        tab_key = "itemGroup.%s.spellcard" % MODID
        if tab_key not in data:
            data[tab_key] = orig.get("itemGroup.spellCard", "Spell Card")
            added += 1

        # 早苗符卡 tooltip 片段
        for i in range(1, 5):
            k = "thKaguya.msg.sneSc%d" % i
            if k in data:
                continue
            v = orig.get(k)
            if v is not None:
                data[k] = v
                added += 1

        with open(p, "w", encoding="utf-8") as f:
            json.dump(data, f, ensure_ascii=False, indent=2, sort_keys=True)
            f.write("\n")
        lang_stat.append((lang, added))

    # ---- 报告 ----
    print("符卡数: %d" % len(cards))
    for r in report:
        print("  id=%-3d %-34s %-32s tex=%-32s lv=%d" % r)
    print("生成模型: %d 个 (+1 入口 spell_card.json)" % len(cards))
    print("语言键写入: %s" % ", ".join("%s +%d" % (a, b) for a, b in lang_stat))
    if missing_tex:
        print("!! 缺失贴图 (%d): %s" % (len(sorted(set(missing_tex))), ", ".join(sorted(set(missing_tex)))))
    else:
        print("贴图齐全：卡面与边框均已存在")
    return 0


if __name__ == "__main__":
    sys.exit(main())
