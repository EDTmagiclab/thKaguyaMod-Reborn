package thKaguyaMod.init;

import net.minecraft.world.entity.EntityType;
import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * コンフィグ設定クラス（1.20.1 移植版）
 * 原版 1.7.10 的 Forge Configuration -> 1.20.1 的 ModConfigSpec。
 * 阶段 3b：配置系统重写，实体层（EntityDanmakuMob/EntityTHFairy/EntityTHShot）依赖本类。
 */
public class THKaguyaConfig
{
    /** スキマの効果音 */
    public static String sukimaWarpSE = "mob.endermen.portal";

    /* ============ 效果相关 ============ */
    public static float SpellCardVol;
    public static float MasterSparkVol;
    public static float HisoutenVol;
    public static float DownVol;
    public static boolean UsingMasterSpark;
    public static boolean MasterSparkDestroysBlocks;
    public static boolean useDefaultGapSE;
    public static boolean useTimeStopEffect;

    /* ============ 弹幕相关 ============ */
    public static boolean spawnDanmakuMob;
    public static boolean spawnHanabeeper;
    public static boolean spawnFairy;
    public static boolean spawnPhantom;
    public static boolean spawnBoss;
    public static int fairySpawnRate;
    public static int danmakuLevel;
    public static float danmakuAccuracy;
    public static boolean danmakuOneKillMode;
    public static boolean canUseWallPassingChisel;
    public static int shotMaxNumber;
    public static int laserMaxNumber;

    /* ============ 実験的機能 ============ */
    /** 万宝槌の改造台（原作者がコメントアウトした機能）を有効にする */
    public static boolean enableMalletGui;
    /** 実験的機能の警告ダイアログを再度表示しない */
    public static boolean suppressMalletGuiWarning;

    /** 実験的機能：早苗の弾幕戦を有効にする（設定画面の名称：启用早苗弹幕战） */
    public static boolean enableSanaeBossFight;

    /** 実験的機能：咲夜の弾幕戦を有効にする（設定画面の名称：启用咲夜弹幕战） */
    public static boolean enableSakuyaBossFight;

    public static final ModConfigSpec SPEC;

    private static final ModConfigSpec.BooleanValue USING_MASTER_SPARK;
    private static final ModConfigSpec.BooleanValue MASTER_SPARK_DESTROYS_BLOCKS;
    private static final ModConfigSpec.BooleanValue CAN_USE_WALL_PASSING_CHISEL;
    private static final ModConfigSpec.IntValue SHOT_MAX_NUMBER;
    private static final ModConfigSpec.IntValue LASER_MAX_NUMBER;
    private static final ModConfigSpec.BooleanValue USE_TIME_STOP_EFFECT;
    private static final ModConfigSpec.BooleanValue USE_DEFAULT_GAP_SE;
    private static final ModConfigSpec.IntValue SPELL_CARD_VOL;
    private static final ModConfigSpec.IntValue MASTER_SPARK_VOL;
    private static final ModConfigSpec.IntValue HISOUTEN_VOL;
    private static final ModConfigSpec.IntValue DOWN_VOL;
    private static final ModConfigSpec.BooleanValue DANMAKU_ONE_KILL_MODE;
    private static final ModConfigSpec.BooleanValue SPAWN_DANMAKU_MOB;
    private static final ModConfigSpec.BooleanValue SPAWN_FAIRY;
    private static final ModConfigSpec.BooleanValue SPAWN_PHANTOM;
    private static final ModConfigSpec.BooleanValue SPAWN_BOSS;
    private static final ModConfigSpec.IntValue FAIRY_SPAWN_RATE;
    private static final ModConfigSpec.BooleanValue SPAWN_HANABEEPER;
    private static final ModConfigSpec.IntValue DANMAKU_LEVEL;
    private static final ModConfigSpec.IntValue DANMAKU_ACCURACY;
    private static final ModConfigSpec.BooleanValue ENABLE_MALLET_GUI;
    private static final ModConfigSpec.BooleanValue SUPPRESS_MALLET_GUI_WARNING;
    private static final ModConfigSpec.BooleanValue ENABLE_SANAE_BOSS_FIGHT;
    private static final ModConfigSpec.BooleanValue ENABLE_SAKUYA_BOSS_FIGHT;

    static
    {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();

        builder.push("Limit");
        USING_MASTER_SPARK = builder.comment("Can use Master Spark").define("Use Master Spark", true);
        MASTER_SPARK_DESTROYS_BLOCKS = builder.comment("Master Spark destroys blocks").define("Master Spark Destroys Blocks", true);
        CAN_USE_WALL_PASSING_CHISEL = builder.comment("Can use Wall-Passing Chisel").define("Can Use Wall-Passing Chisel", true);
        SHOT_MAX_NUMBER = builder.comment("Max shot number from shot item").defineInRange("Shot Max Number", 32, 1, 1024);
        LASER_MAX_NUMBER = builder.comment("Max laser number from laser item").defineInRange("Laser Max Number", 8, 1, 256);
        builder.pop();

        builder.push("Effect");
        USE_TIME_STOP_EFFECT = builder.comment("Appear time stop effect").define("Appear Time stop effect", true);
        USE_DEFAULT_GAP_SE = builder.comment("Use default Gap(Sukima) SE").define("Use default Gap(Sukima)SE", true);
        builder.pop();

        builder.push("SE vol");
        SPELL_CARD_VOL = builder.defineInRange("Spell Card Vol", 50, 0, 500);
        MASTER_SPARK_VOL = builder.defineInRange("Master Spark Vol", 50, 0, 500);
        HISOUTEN_VOL = builder.defineInRange("Hisouten Vol", 20, 0, 500);
        DOWN_VOL = builder.defineInRange("Down Vol", 50, 0, 500);
        builder.pop();

        builder.push("Danmaku");
        DANMAKU_ONE_KILL_MODE = builder.comment("Danmaku 1 kill mode").define("Danmaku 1 Kill Mode", false);
        SPAWN_DANMAKU_MOB = builder.define("Spawn Danmaku Mob", true);
        SPAWN_FAIRY = builder.define("Spawn Fairy", true);
        SPAWN_PHANTOM = builder.define("Spawn Phantom", true);
        SPAWN_BOSS = builder.define("Spawn Danmaku Mob Boss", true);
        FAIRY_SPAWN_RATE = builder.defineInRange("Fairy Spawn Rate(%)", 50, 0, 100);
        DANMAKU_LEVEL = builder.defineInRange("Danmaku Level", 2, 0, 4);
        DANMAKU_ACCURACY = builder.comment("Hit rate of enemy danmaku (low=0, high=100)").defineInRange("Hit Rate of Enemy Danmaku(row=0,high=100)", 80, 0, 100);
        builder.pop();

        builder.push("Other");
        SPAWN_HANABEEPER = builder.define("Spawn Hanabeeper", false);
        builder.pop();

        // 実験的機能（実験的玩法）
        // 元の 1.7.10 作者がコメントアウトした「神隠し」機能を、ユーザーが明示的に有効化する。
        builder.push("Experimental");
        ENABLE_MALLET_GUI = builder
                .comment("Enable the Miracle Mallet crafting table (the feature the original author commented out).",
                        "This is a 'kamikakushi' (god-hidden) feature: the original 1.7.10 code never opened this GUI.",
                        "When enabled: sneak + right-click with the mallet to open it. Default: false")
                .define("Enable Miracle Mallet GUI", false);
        SUPPRESS_MALLET_GUI_WARNING = builder
                .comment("Do not show the experimental feature warning dialog again.")
                .define("Suppress Miracle Mallet GUI Warning", false);
        ENABLE_SANAE_BOSS_FIGHT = builder
                .comment("启用早苗弹幕战（实验性玩法）",
                        "原版的早苗只是商人，战斗流程被作者注释掉了。",
                        "开启后：头戴诹访子的帽子、手持御柱右键早苗即可开战，并按 非符→奇迹果→非符→摩西奇迹→非符→八坂神风→非符→妖力掠夺→结束 推进。",
                        "Default: false")
                .define("Enable Sanae Danmaku Battle", false);
        ENABLE_SAKUYA_BOSS_FIGHT = builder
                .comment("启用咲夜弹幕战（实验性玩法）",
                        "开启后：手持冈格尼尔（Gungnir）右键咲夜即可开战，两张符卡：杀人玩偶 → 永恒之月。",
                        "Default: false")
                .define("Enable Sakuya Danmaku Battle", false);
        builder.pop();

        SPEC = builder.build();
    }

    /**
     * 自然生成开关（原版 CommonProxy.registerSpawn 的 5 个 If 块，批次 C1）
     *
     * <p>原版是把 addSpawn 包在 if 里，1.20.1 的 biome modifier 是数据驱动、读不到配置，
     * 所以改成在实体侧 checkSpawnRules 拒绝（效果一致：关掉的怪不会自然生成，刷怪蛋/命令仍可生成）。</p>
     */
    public static boolean allowNaturalSpawn(EntityType<?> type)
    {
        if (!spawnDanmakuMob)
        {
            // 原版最外层总开关：关掉后连妖精/幽灵/Boss 都不生成（早苗/咲夜/霖之助 不在其中）
            if (type == THKaguyaEntities.TH_FAIRY.get() || type == THKaguyaEntities.SUNFLOWER_FAIRY.get()
                    || type == THKaguyaEntities.TH_PHANTOM.get() || type == THKaguyaEntities.DANMAKU_CREEPER.get()
                    || type == THKaguyaEntities.CIRNO.get() || type == THKaguyaEntities.RUMIA.get()
                    || type == THKaguyaEntities.TOZIKO.get() || type == THKaguyaEntities.WRIGGLE.get())
            {
                return false;
            }
        }
        if (type == THKaguyaEntities.TH_FAIRY.get() || type == THKaguyaEntities.SUNFLOWER_FAIRY.get())
        {
            return spawnFairy;
        }
        if (type == THKaguyaEntities.TH_PHANTOM.get())
        {
            return spawnPhantom;
        }
        if (type == THKaguyaEntities.DANMAKU_CREEPER.get())
        {
            return spawnHanabeeper;
        }
        if (type == THKaguyaEntities.CIRNO.get() || type == THKaguyaEntities.RUMIA.get()
                || type == THKaguyaEntities.TOZIKO.get() || type == THKaguyaEntities.WRIGGLE.get())
        {
            return spawnBoss;
        }
        // 早苗/咲夜/霖之助：原版这三个不在任何开关里，永远生成
        return true;
    }

    /** 注册时加载配置值到静态字段（阶段 3b：实体层用静态字段读写） */
    public static void load()
    {
        UsingMasterSpark = USING_MASTER_SPARK.get();
        MasterSparkDestroysBlocks = MASTER_SPARK_DESTROYS_BLOCKS.get();
        canUseWallPassingChisel = CAN_USE_WALL_PASSING_CHISEL.get();
        shotMaxNumber = SHOT_MAX_NUMBER.get();
        laserMaxNumber = LASER_MAX_NUMBER.get();
        useTimeStopEffect = USE_TIME_STOP_EFFECT.get();
        useDefaultGapSE = USE_DEFAULT_GAP_SE.get();
        if(!useDefaultGapSE)
        {
            // 原版 jar 也没有 sukima.ogg 音频（sounds.json 引用但文件缺失，属上游遗留）。
            // 自定义音效名回退到同款原版音效，避免资源缺失 WARNING。
            sukimaWarpSE = "mob.endermen.portal";
        }
        SpellCardVol = (float)SPELL_CARD_VOL.get() / 100F;
        MasterSparkVol = (float)MASTER_SPARK_VOL.get() / 100F;
        HisoutenVol = (float)HISOUTEN_VOL.get() / 100F;
        DownVol = (float)DOWN_VOL.get() / 100F;
        danmakuOneKillMode = DANMAKU_ONE_KILL_MODE.get();
        spawnDanmakuMob = SPAWN_DANMAKU_MOB.get();
        spawnFairy = SPAWN_FAIRY.get();
        spawnPhantom = SPAWN_PHANTOM.get();
        spawnBoss = SPAWN_BOSS.get();
        fairySpawnRate = 100 - FAIRY_SPAWN_RATE.get();
        spawnHanabeeper = SPAWN_HANABEEPER.get();
        danmakuLevel = DANMAKU_LEVEL.get();
        danmakuAccuracy = 360F * (1F - (DANMAKU_ACCURACY.get() / 100F));

        enableMalletGui = ENABLE_MALLET_GUI.get();
        suppressMalletGuiWarning = SUPPRESS_MALLET_GUI_WARNING.get();
        enableSanaeBossFight = ENABLE_SANAE_BOSS_FIGHT.get();
        enableSakuyaBossFight = ENABLE_SAKUYA_BOSS_FIGHT.get();
    }

    /**
     * 実験的機能：万宝槌の改造台の有効／無効を書き換える（設定画面から呼ぶ）。
     *
     * <p>{@link ModConfigSpec.ConfigValue#set} は値を保持するだけでファイルには書かないため、
     * ここで {@code SPEC.save()} まで行い、静的フィールドも即時同期する（再起動不要）。</p>
     */
    public static void setEnableMalletGui(boolean value)
    {
        ENABLE_MALLET_GUI.set(value);
        if (SPEC.isLoaded())
        {
            SPEC.save();
        }
        enableMalletGui = value;
    }

    /** 実験的機能：警告ダイアログの「今後表示しない」を書き換える。 */
    public static void setSuppressMalletGuiWarning(boolean value)
    {
        SUPPRESS_MALLET_GUI_WARNING.set(value);
        if (SPEC.isLoaded())
        {
            SPEC.save();
        }
        suppressMalletGuiWarning = value;
    }

    /** 実験的機能：早苗の弾幕戦の有効／無効を書き換える（設定画面から呼ぶ）。 */
    public static void setEnableSanaeBossFight(boolean value)
    {
        ENABLE_SANAE_BOSS_FIGHT.set(value);
        if (SPEC.isLoaded())
        {
            SPEC.save();
        }
        enableSanaeBossFight = value;
    }

    /** 実験的機能：咲夜の弾幕戦の有効／無効を書き換える（設定画面から呼ぶ）。 */
    public static void setEnableSakuyaBossFight(boolean value)
    {
        ENABLE_SAKUYA_BOSS_FIGHT.set(value);
        if (SPEC.isLoaded())
        {
            SPEC.save();
        }
        enableSakuyaBossFight = value;
    }
}
