package thKaguyaMod.init;

import net.minecraft.world.item.Item;
import net.minecraftforge.common.ForgeSpawnEggItem;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import thKaguyaMod.THKaguyaCore;
import thKaguyaMod.item.ItemAjaRedStone;
import thKaguyaMod.item.ItemByoukiheiyuMamori;
import thKaguyaMod.item.ItemClosedThirdEye;
import thKaguyaMod.item.ItemCursedDecoyDoll;
import thKaguyaMod.item.ItemDeathScythe;
import thKaguyaMod.item.ItemDragonNeckJewel;
import thKaguyaMod.item.ItemHakurouken;
import thKaguyaMod.item.ItemHisou;
import thKaguyaMod.item.ItemHomingAmulet;
import thKaguyaMod.item.ItemIcicleSword;
import thKaguyaMod.item.ItemKappaWaterPistol;
import thKaguyaMod.item.ItemKinkakuzi;
import thKaguyaMod.item.ItemLaevateinn;
import thKaguyaMod.item.ItemMarisaBroom;
import thKaguyaMod.item.ItemMazinKyoukan;
import thKaguyaMod.item.ItemNuclearControlRod;
import thKaguyaMod.item.ItemMiniHakkero;
import thKaguyaMod.item.ItemOnbashira;
import thKaguyaMod.item.ItemPendulum;
import thKaguyaMod.item.ItemRemorseRod;
import thKaguyaMod.item.ItemSakuyaStopWatch;
import thKaguyaMod.item.ItemSakuyaWatch;
import thKaguyaMod.item.ItemSoulTorch;
import thKaguyaMod.item.ItemShipGhostDipper;
import thKaguyaMod.item.ItemSilverKnife;
import thKaguyaMod.item.ItemSpearTheGungnir;
import thKaguyaMod.item.ItemSpiritualStrikeTalisman;
import thKaguyaMod.item.ItemSukima;
import thKaguyaMod.item.ItemTHSpellCard;
import thKaguyaMod.item.ItemTenguFan;
import thKaguyaMod.item.ItemWallPassingChisel;
import thKaguyaMod.item.ItemYuyukoFan;
import thKaguyaMod.item.ItemYuukaParasol;

/**
 * アイテムの登録クラス（1.20.1 移植版）
 * 原版 1.7.10 用 metadata(damage值) 区分变体，1.20.1 已废除 metadata，
 * 故珍珠8色/弹体19种/激光3种/银刀4色/威力2种全部拆分为独立 Item。
 * 阶段 1 仅完成注册；具体行为（武器/护甲/右键）在阶段 3/6 回填。
 */
public class THKaguyaItems
{
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, THKaguyaCore.MODID);

    /* ==================== 独立物品 ==================== */
    /** 蓬莱の玉の枝 */
    public static final RegistryObject<Item> HOURAI_JEWELED_BRANCH = register("hourai_jeweled_branch");

    /** 龍の頸の玉 */
    public static final RegistryObject<Item> DRAGON_NECK_JEWEL = register("dragon_neck_jewel", () -> new ItemDragonNeckJewel());

    /** 仏の御石の鉢 */
    public static final RegistryObject<Item> BUDDHA_STONE_BOWL = register("buddha_stone_bowl");

    /** 火鼠の皮衣 */
    public static final RegistryObject<Item> FIRE_RAT_ROBE = register("fire_rat_robe");

    /** 燕の子安貝 */
    public static final RegistryObject<Item> SWALLOW_COWRIE_SHELL = register("swallow_cowrie_shell");

    /** 金閣寺の一枚天井 */
    public static final RegistryObject<Item> KINKAKUJI = register("kinkakuji", () -> new ItemKinkakuzi());

    /** エイジャの赤石 */
    public static final RegistryObject<Item> AJA_RED_STONE = register("aja_red_stone", () -> new ItemAjaRedStone());

    /** 博麗のお祓い棒 */
    public static final RegistryObject<Item> HAKUREI_MIKO_STICK = register("hakurei_miko_stick");

    /** ホーミングアミュレット（原版 damage 0） */
    public static final RegistryObject<Item> HOMING_AMULET = register("homing_amulet",
            () -> new ItemHomingAmulet(ItemHomingAmulet.TYPE_HOMING));

    /** 拡散アミュレット（原版 damage 1、1.20.1 で独立登録項に分割） */
    public static final RegistryObject<Item> DIFFUSION_AMULET = register("diffusion_amulet",
            () -> new ItemHomingAmulet(ItemHomingAmulet.TYPE_DIFFUSION));

    /** 陰陽玉 */
    public static final RegistryObject<Item> YIN_YANG_ORB = register("yin_yang_orb");

    /** 血に餓えた陰陽玉 */
    public static final RegistryObject<Item> BLOODTHIRSTY_YIN_YANG_ORB = register("bloodthirsty_yin_yang_orb");

    /** 魔法の箒 */
    public static final RegistryObject<Item> MAGIC_BROOM = register("magic_broom", () -> new ItemMarisaBroom());

    /** ミニ八卦炉 */
    public static final RegistryObject<Item> MINI_HAKKERO = register("mini_hakkero", () -> new ItemMiniHakkero());

    /** 魔理沙の帽子 */
    public static final RegistryObject<Item> MARISA_HAT = register("marisa_hat");

    /** アイシクルソード */
    public static final RegistryObject<Item> ICICLE_SWORD = register("icicle_sword", () -> new ItemIcicleSword());

    /** 咲夜の懐中時計 */
    public static final RegistryObject<Item> SAKUYA_WATCH = register("sakuya_watch", () -> new ItemSakuyaWatch());

    /** ストップウォッチ */
    public static final RegistryObject<Item> SAKUYA_STOPWATCH = register("sakuya_stopwatch", () -> new ItemSakuyaStopWatch());

    /** スピア・ザ・グングニル */
    public static final RegistryObject<Item> GUNGNIR = register("gungnir", () -> new ItemSpearTheGungnir());

    /** レーヴァテイン */
    public static final RegistryObject<Item> LAEVATEINN = register("laevateinn", () -> new ItemLaevateinn());

    /** 呪いのデコイ人形 */
    public static final RegistryObject<Item> CURSED_DECOY_DOLL = register("cursed_decoy_doll", () -> new ItemCursedDecoyDoll());

    /** 白楼剣 */
    public static final RegistryObject<Item> HAKUROUKEN = register("hakurouken", () -> new ItemHakurouken());

    /** 楼観剣 */
    public static final RegistryObject<Item> ROUKANKEN = register("roukanken");

    /** 楼観旋風刃（原版 roukanSenpuuzin，阶段 5 奇迹小槌产物） */
    public static final RegistryObject<Item> ROUKAN_SENPUUZIN = register("roukan_senpuuzin");

    /** 幽々子の扇 */
    public static final RegistryObject<Item> YUYUKO_FAN = register("yuyuko_fan", () -> new ItemYuyukoFan());

    /** 人魂灯 */
    public static final RegistryObject<Item> SOUL_TORCH = register("soul_torch", () -> new ItemSoulTorch());

    /** スキマ */
    public static final RegistryObject<Item> GAP = register("gap", () -> new ItemSukima());

    /** 隙間の折りたたみ傘 */
    public static final RegistryObject<Item> GAP_FOLDING_UMBRELLA = register("gap_folding_umbrella");

    /** 伊吹瓢 */
    public static final RegistryObject<Item> IBUKI_GOURD = register("ibuki_gourd");

    /** 幽香の傘 */
    public static final RegistryObject<Item> YUUKA_PARASOL = register("yuuka_parasol", () -> new ItemYuukaParasol());

    /** 死神の大鎌 */
    public static final RegistryObject<Item> DEATH_SCYTHE = register("death_scythe", () -> new ItemDeathScythe());

    /** 悔悟の棒 */
    public static final RegistryObject<Item> REMORSE_ROD = register("remorse_rod", () -> new ItemRemorseRod());

    /** 天狗の団扇 */
    public static final RegistryObject<Item> TENGU_FAN = register("tengu_fan", () -> new ItemTenguFan());

    /** 河童の帽子 */
    public static final RegistryObject<Item> KAPPA_CAP = register("kappa_cap");

    /** 河童の水鉄砲 */
    public static final RegistryObject<Item> KAPPA_WATER_PISTOL = register("kappa_water_pistol", () -> new ItemKappaWaterPistol());

    /** 風祝のお祓い棒 */
    public static final RegistryObject<Item> WIND_MIKO_STICK = register("wind_miko_stick");

    /** 病気平癒守 */
    public static final RegistryObject<Item> ILLNESS_RECOVERY_CHARM = register("illness_recovery_charm", () -> new ItemByoukiheiyuMamori());

    /** 御柱 */
    public static final RegistryObject<Item> ONBASHIRA = register("onbashira", () -> new ItemOnbashira());

    /** 諏訪子の帽子 */
    public static final RegistryObject<Item> SUWAKO_HAT = register("suwako_hat");

    /** 天界の桃 */
    public static final RegistryObject<Item> HEAVENLY_PEACH = register("heavenly_peach");

    /** 緋想の剣 */
    public static final RegistryObject<Item> HISOU_SWORD = register("hisou_sword", () -> new ItemHisou());

    /** 第三の眼 */
    public static final RegistryObject<Item> THIRD_EYE = register("third_eye");

    /** 制御棒 */
    public static final RegistryObject<Item> NUCLEAR_CONTROL_ROD = register("nuclear_control_rod", () -> new ItemNuclearControlRod());

    /** 閉じた第三の眼 */
    public static final RegistryObject<Item> CLOSED_THIRD_EYE = register("closed_third_eye", () -> new ItemClosedThirdEye());

    /** ナズーリンペンデュラム */
    public static final RegistryObject<Item> NAZRIN_PENDULUM = register("nazrin_pendulum", () -> new ItemPendulum());

    /** 舟幽霊の柄杓（空） */
    public static final RegistryObject<Item> SHIP_GHOST_DIPPER_EMPTY = register("ship_ghost_dipper_empty", () -> new ItemShipGhostDipper(ItemShipGhostDipper.EMPTY));

    /** 舟幽霊の柄杓（水入り） */
    public static final RegistryObject<Item> SHIP_GHOST_DIPPER = register("ship_ghost_dipper", () -> new ItemShipGhostDipper(ItemShipGhostDipper.FILL));

    /** 魔人経巻 */
    public static final RegistryObject<Item> SORCERER_SUTRA_SCROLL = register("sorcerer_sutra_scroll", () -> new ItemMazinKyoukan());

    /** 壁抜けの鑿 */
    public static final RegistryObject<Item> WALL_PASSING_CHISEL = register("wall_passing_chisel", () -> new ItemWallPassingChisel());

    /** 豊聡耳の宝剣 */
    public static final RegistryObject<Item> TOYOSATOMIMI_SWORD = register("toyosatomimi_sword");

    /** 打ち出の小槌 */
    public static final RegistryObject<Item> MIRACLE_MALLET = register("miracle_mallet");

    /** 霊撃札 */
    public static final RegistryObject<Item> SPIRITUAL_STRIKE_TALISMAN = register("spiritual_strike_talisman", () -> new ItemSpiritualStrikeTalisman());

    /** 弾幕の素 */
    public static final RegistryObject<Item> SHOT_MATERIAL = register("shot_material");

    /** レーザーの素（原版 laser_item，阶段 5 激光工作台素材） */
    public static final RegistryObject<Item> LASER_MATERIAL = register("laser_material");

    /** 点符アイテム */
    public static final RegistryObject<Item> POINT_ITEM = register("point_item");

    /** ボムアイテム */
    public static final RegistryObject<Item> BOMB_ITEM = register("bomb_item");

    /** エクステンドアイテム */
    public static final RegistryObject<Item> EXTEND_ITEM = register("extend_item");

    /** スペルカード（damage 値 = 符卡 No。符卡専用クリエイティブタブに並ぶ） */
    public static final RegistryObject<Item> SPELL_CARD = register("spell_card", () -> new ItemTHSpellCard());

    /* ==================== 刷怪蛋 ==================== */
    /** 妖精刷怪蛋（原版 eggColor 0xFFFF80 / 0xC0C000） */
    public static final RegistryObject<Item> TH_FAIRY_SPAWN_EGG = ITEMS.register("th_fairy_spawn_egg",
            () -> new ForgeSpawnEggItem(THKaguyaEntities.TH_FAIRY, 0xFFFF80, 0xC0C000, new Item.Properties()));

    /* ==================== living 刷怪蛋（阶段 3d-6）==================== */
    public static final RegistryObject<Item> CIRNO_SPAWN_EGG = ITEMS.register("cirno_spawn_egg",
            () -> new ForgeSpawnEggItem(THKaguyaEntities.CIRNO, 0x40C0FF, 0x2020FF, new Item.Properties()));
    public static final RegistryObject<Item> DANMAKU_CREEPER_SPAWN_EGG = ITEMS.register("danmaku_creeper_spawn_egg",
            () -> new ForgeSpawnEggItem(THKaguyaEntities.DANMAKU_CREEPER, 0x40C040, 0x202020, new Item.Properties()));
    public static final RegistryObject<Item> FAMILIAR_SPAWN_EGG = ITEMS.register("familiar_spawn_egg",
            () -> new ForgeSpawnEggItem(THKaguyaEntities.FAMILIAR, 0xFFE0A0, 0xC08040, new Item.Properties()));
    public static final RegistryObject<Item> MIKO_SPAWN_EGG = ITEMS.register("miko_spawn_egg",
            () -> new ForgeSpawnEggItem(THKaguyaEntities.MIKO, 0xFFE0E0, 0x800080, new Item.Properties()));
    public static final RegistryObject<Item> REIMU_SPAWN_EGG = ITEMS.register("reimu_spawn_egg",
            () -> new ForgeSpawnEggItem(THKaguyaEntities.REIMU, 0xFF4040, 0xFFFFFF, new Item.Properties()));
    public static final RegistryObject<Item> RUMIA_SPAWN_EGG = ITEMS.register("rumia_spawn_egg",
            () -> new ForgeSpawnEggItem(THKaguyaEntities.RUMIA, 0xFFE080, 0x402020, new Item.Properties()));
    public static final RegistryObject<Item> SAKUYA_SPAWN_EGG = ITEMS.register("sakuya_spawn_egg",
            () -> new ForgeSpawnEggItem(THKaguyaEntities.SAKUYA, 0xC0C0FF, 0x404080, new Item.Properties()));
    public static final RegistryObject<Item> SANAE_SPAWN_EGG = ITEMS.register("sanae_spawn_egg",
            () -> new ForgeSpawnEggItem(THKaguyaEntities.SANAE, 0x80E080, 0xFFFFFF, new Item.Properties()));
    public static final RegistryObject<Item> SUNFLOWER_FAIRY_SPAWN_EGG = ITEMS.register("sunflower_fairy_spawn_egg",
            () -> new ForgeSpawnEggItem(THKaguyaEntities.SUNFLOWER_FAIRY, 0xFFD000, 0x804000, new Item.Properties()));
    public static final RegistryObject<Item> TH_PHANTOM_SPAWN_EGG = ITEMS.register("th_phantom_spawn_egg",
            () -> new ForgeSpawnEggItem(THKaguyaEntities.TH_PHANTOM, 0xE0E0FF, 0x8080C0, new Item.Properties()));
    public static final RegistryObject<Item> TOZIKO_SPAWN_EGG = ITEMS.register("toziko_spawn_egg",
            () -> new ForgeSpawnEggItem(THKaguyaEntities.TOZIKO, 0x80FF80, 0x404040, new Item.Properties()));
    public static final RegistryObject<Item> WRIGGLE_SPAWN_EGG = ITEMS.register("wriggle_spawn_egg",
            () -> new ForgeSpawnEggItem(THKaguyaEntities.WRIGGLE, 0xC0FF40, 0x406000, new Item.Properties()));

    /* ==================== 真珠（metadata 变体拆分）==================== */
    /** 赤真珠 */
    public static final RegistryObject<Item> RED_PEARL = register("red_pearl");

    /** 青真珠 */
    public static final RegistryObject<Item> BLUE_PEARL = register("blue_pearl");

    /** 緑真珠 */
    public static final RegistryObject<Item> GREEN_PEARL = register("green_pearl");

    /** 黄真珠 */
    public static final RegistryObject<Item> YELLOW_PEARL = register("yellow_pearl");

    /** 紫真珠 */
    public static final RegistryObject<Item> PURPLE_PEARL = register("purple_pearl");

    /** 水真珠 */
    public static final RegistryObject<Item> AQUA_PEARL = register("aqua_pearl");

    /** 橙真珠 */
    public static final RegistryObject<Item> ORANGE_PEARL = register("orange_pearl");

    /** 白真珠 */
    public static final RegistryObject<Item> WHITE_PEARL = register("white_pearl");

    /* ==================== 弾（metadata 变体拆分）==================== */
    /** 粒弾 */
    public static final RegistryObject<Item> TINY_SHOT = register("tiny_shot");

    /** 小弾 */
    public static final RegistryObject<Item> SMALL_SHOT = register("small_shot");

    /** 中弾 */
    public static final RegistryObject<Item> MEDIUM_SHOT = register("medium_shot");

    /** 大弾 */
    public static final RegistryObject<Item> BIG_SHOT = register("big_shot");

    /** 星弾 */
    public static final RegistryObject<Item> STAR_SHOT = register("star_shot");

    /** 小星弾 */
    public static final RegistryObject<Item> SMALL_STAR_SHOT = register("small_star_shot");

    /** 輪弾 */
    public static final RegistryObject<Item> CIRCLE_SHOT = register("circle_shot");

    /** 鱗弾 */
    public static final RegistryObject<Item> SCALE_SHOT = register("scale_shot");

    /** 蝶弾 */
    public static final RegistryObject<Item> BUTTERFLY_SHOT = register("butterfly_shot");

    /** 光弾 */
    public static final RegistryObject<Item> LIGHT_SHOT = register("light_shot");

    /** ナイフ弾 */
    public static final RegistryObject<Item> KNIFE_SHOT = register("knife_shot");

    /** ハート弾 */
    public static final RegistryObject<Item> HEART_SHOT = register("heart_shot");

    /** クナイ弾 */
    public static final RegistryObject<Item> KUNAI_SHOT = register("kunai_shot");

    /** 札弾 */
    public static final RegistryObject<Item> TALISMAN_SHOT = register("talisman_shot");

    /** 大光弾 */
    public static final RegistryObject<Item> BIG_LIGHT_SHOT = register("big_light_shot");

    /** 米弾 */
    public static final RegistryObject<Item> RICE_SHOT = register("rice_shot");

    /** 楕円弾 */
    public static final RegistryObject<Item> OVAL_SHOT = register("oval_shot");

    /** 結晶弾 */
    public static final RegistryObject<Item> CRYSTAL_SHOT = register("crystal_shot");

    /** 矢弾 */
    public static final RegistryObject<Item> ARROW_SHOT = register("arrow_shot");

    /* ==================== レーザー（metadata 变体拆分）==================== */
    /** ショートレーザー */
    public static final RegistryObject<Item> SHORT_LASER = register("short_laser");

    /** レーザー */
    public static final RegistryObject<Item> MIDDLE_LASER = register("middle_laser");

    /** ロングレーザー */
    public static final RegistryObject<Item> LONG_LASER = register("long_laser");

    /* ==================== 銀ナイフ（metadata 变体拆分）==================== */
    /** 銀のナイフ 青 */
    public static final RegistryObject<Item> SILVER_KNIFE_BLUE = register("silver_knife_blue", () -> new ItemSilverKnife(0));

    /** 銀のナイフ 赤 */
    public static final RegistryObject<Item> SILVER_KNIFE_RED = register("silver_knife_red", () -> new ItemSilverKnife(1));

    /** 銀のナイフ 緑 */
    public static final RegistryObject<Item> SILVER_KNIFE_GREEN = register("silver_knife_green", () -> new ItemSilverKnife(2));

    /** タイムパラドックスナイフ */
    public static final RegistryObject<Item> SILVER_KNIFE_WHITE = register("silver_knife_white", () -> new ItemSilverKnife(3));

    /* ==================== パワー（metadata 变体拆分）==================== */
    /** パワーアップアイテム 小 */
    public static final RegistryObject<Item> POWER_UP_ITEM_SMALL = register("power_up_item_small");

    /** パワーアップアイテム 大 */
    public static final RegistryObject<Item> POWER_UP_ITEM_BIG = register("power_up_item_big");

    private static RegistryObject<Item> register(String name, java.util.function.Supplier<Item> supplier)
    {
        return ITEMS.register(name, supplier);
    }

    private static RegistryObject<Item> register(String name)
    {
        return ITEMS.register(name, () -> new Item(new Item.Properties()));
    }
}