# -*- coding: utf-8 -*-
"""生成 registerSpellCards() 代码块（对齐原版 CommonProxy 567-596 的注册表）"""
REG = [
    ("THSC_MusouFuuin",              "MusouFuuin",                 0),
    ("THSC_MasterSpark",             "MasterSpark",                1),
    ("THSC_Kasho_no_Eimin",          "Kasho_no_Eimin",             2),
    ("THSC_MeteonicShower",          "MeteonicShower",             3),
    ("THSC_Nami_to_Tubu_no_Kyoukai", "Nami_to_Tubu_no_Kyoukai",    4),
    ("THSC_NijuuKokushichou",        "NijuuKokushichou",           5),
    ("THSC_ScarletShoot",            "ScarletShoot",               6),
    ("THSC_Zenzinrui_no_Hisouten",   "Zenzinrui_no_Hisouten",      7),
    ("THSC_SatuzinDoll",             "SatuzinDoll",                8),
    ("THSC_PerfectFreeze",           "PerfectFreeze",              9),
    ("THSC_HikouchuuNest",           "HikouchuuNest",             10),
    ("THSC_KappaPororoca",           "Kappa'sPororoca",           11),
    ("THSC_StardustReverie",         "StardustReverie",           12),
    ("THSC_Kerochan_Fuuu_ni_Makezu", "Kerochan_Fuuu_ni_Makezu",   13),
    ("THSC_MiracleFruit",            "MiracleFruit",              14),
    ("THSC_Fafurotskies_no_Kiseki",  "Fafrotskies_no_Kiseki",     15),
    ("THSC_YouryokuSpoiler",         "YouryokuSpoiler",           16),
    ("THSC_MosesMiracle",            "Moses_no_Kiseki",           17),
    ("THSC_Yasaka_no_Kamikaze",      "Yasaka_no_Kamikaze",        18),
    ("THSC_IcicleFall",              "IcicleFall",                19),
    ("THSC_StarbowBreak",            "StarbowBreak",              20),
    ("THSC_Catadioptric",            "Catadioptric",              21),
    ("THSC_Mishagujisama",           "Mishagujisama",             22),
    ("THSC_RedMagic",                "RedMagic",                  23),
    ("THSC_EternalMeek",             "EternalMeek",               24),
    ("THSC_NonDirectionalLaser",     "Non-DirectionalLaser",      25),
    ("THSC_Kachoufuugetu",           "KachoufuugetuShoufuurougetu", 26),
    ("THSC_Spear_the_Gungnir",       "Spear_the_Gungnir",         27),
    ("THSC_MoonlightRay",            "MoonlightRay",              28),
    ("THSC_HoukaKenran",             "HoukaKenran",               29),
    ("THSC_SaikouRanbu",             "SaikouRanbu",               30),
    ("THSC_FujiyamaVolcano",         "FujiyamaVolcano",           33),
    ("THSC_LittleBugStorm",          "LittleBugStorm",            34),
    ("THSC_MeteorOnEarth",           "Meteor_on_Earth",           35),
    ("THSC_GagoujiCyclone",          "GagoujiCyclone",            40),
]
lines = []
for cls, name, sid in REG:
    lines.append('        SpellCardRegistry.registerSpellCard(%s.class, "thkaguyamod", "%s", %d);' % (cls, name, sid))
print("\n".join(lines))
print(len(REG), "spellcards")
