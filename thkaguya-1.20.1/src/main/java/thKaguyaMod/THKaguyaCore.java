package thKaguyaMod;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import thKaguyaMod.entity.living.EntityDanmakuMob;
import thKaguyaMod.entity.living.EntityTHFairy;
import thKaguyaMod.entity.item.EntityCursedDecoyDoll;
import thKaguyaMod.init.THKaguyaBlocks;
import thKaguyaMod.init.THKaguyaConfig;
import thKaguyaMod.init.THKaguyaCreativeTab;
import thKaguyaMod.init.THKaguyaEntities;
import thKaguyaMod.init.THKaguyaItems;
import thKaguyaMod.init.THKaguyaSounds;
import thKaguyaMod.init.THKaguyaVillagers;
import thKaguyaMod.registry.DanmakuPatternRegistry;

/**
 * 五つの難題MOD+ のコアクラス（1.20.1 移植版）
 * 原版 1.7.10 的 @Mod 入口，改写为 Forge 1.20.1 的 DeferredRegister 体系。
 */
@Mod(THKaguyaCore.MODID)
public class THKaguyaCore
{
    /** 模组 ID（统一为全小写，原版为 THKaguyaMod 大小写混乱） */
    public static final String MODID = "thkaguya";

    public THKaguyaCore()
    {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        // 配置系统（阶段 3b）
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, THKaguyaConfig.SPEC);

        // 実験的機能の設定画面（Mod 一覧の「Config」ボタン）はクライアント専用。
        // FMLEnvironment.dist を見て分岐するので、専用サーバーではクラス参照が発生しない。
        if (net.minecraftforge.fml.loading.FMLEnvironment.dist == net.minecraftforge.api.distmarker.Dist.CLIENT)
        {
            thKaguyaMod.client.gui.ExperimentalConfigScreen.register();
        }

        // 注册层：物品 / 方块 / 方块实体 / 创造标签 / 实体 / GUI 菜单
        THKaguyaItems.ITEMS.register(modEventBus);
        THKaguyaBlocks.BLOCKS.register(modEventBus);
        THKaguyaBlocks.BLOCK_ENTITIES.register(modEventBus);
        THKaguyaCreativeTab.CREATIVE_TABS.register(modEventBus);
        THKaguyaEntities.ENTITIES.register(modEventBus);
        // 自定义音效事件（4 种：masterspark / spellcard / sukima / down）
        THKaguyaSounds.SOUND_EVENTS.register(modEventBus);
        // 村民体系（阶段 7 补完）：霖之助的兴趣点（弹幕工作台）+ 职业
        THKaguyaVillagers.POI_TYPES.register(modEventBus);
        THKaguyaVillagers.PROFESSIONS.register(modEventBus);
        thKaguyaMod.init.THKaguyaMenuTypes.MENU_TYPES.register(modEventBus);

        // 实体属性注册（阶段 3b：属性系统）
        modEventBus.addListener(this::registerEntityAttributes);
        // 通用设置（阶段 3c：自然生成规则 + 弹幕图案注册表初始化）
        modEventBus.addListener(this::commonSetup);

        // 配置加载/重载：把 ForgeConfigSpec 值刷到静态字段（阶段 3b 修复）
        modEventBus.addListener(this::onConfigLoad);
        modEventBus.addListener(this::onConfigReload);
    }

    /** 配置加载时把值刷到静态字段 */
    private void onConfigLoad(ModConfigEvent.Loading event)
    {
        if(event.getConfig().getSpec() == THKaguyaConfig.SPEC)
        {
            THKaguyaConfig.load();
        }
    }

    /** 配置重载时把值刷到静态字段 */
    private void onConfigReload(ModConfigEvent.Reloading event)
    {
        if(event.getConfig().getSpec() == THKaguyaConfig.SPEC)
        {
            THKaguyaConfig.load();
        }
    }

    /** 注册实体属性（1.20.1 要求显式注册，否则实体生成时崩溃） */
    private void registerEntityAttributes(EntityAttributeCreationEvent event)
    {
        event.put(THKaguyaEntities.TH_FAIRY.get(), EntityTHFairy.createAttributes().build());
        event.put(THKaguyaEntities.CURSED_DECOY_DOLL.get(), EntityCursedDecoyDoll.createAttributes().build());

        // living 实体（阶段 3d-6）
        event.put(THKaguyaEntities.CIRNO.get(), thKaguyaMod.entity.living.EntityCirno.createAttributes().build());
        event.put(THKaguyaEntities.DANMAKU_CREEPER.get(), thKaguyaMod.entity.living.EntityDanmakuCreeper.createAttributes().build());
        event.put(THKaguyaEntities.FAMILIAR.get(), thKaguyaMod.entity.living.EntityFamiliar.createAttributes().build());
        event.put(THKaguyaEntities.MIKO.get(), thKaguyaMod.entity.living.EntityMiko.createAttributes().build());
        event.put(THKaguyaEntities.REIMU.get(), thKaguyaMod.entity.living.EntityReimu.createAttributes().build());
        event.put(THKaguyaEntities.RUMIA.get(), thKaguyaMod.entity.living.EntityRumia.createAttributes().build());
        event.put(THKaguyaEntities.SAKUYA.get(), thKaguyaMod.entity.living.EntitySakuya.createAttributes().build());
        event.put(THKaguyaEntities.SANAE.get(), thKaguyaMod.entity.living.EntitySanae.createAttributes().build());
        event.put(THKaguyaEntities.SUNFLOWER_FAIRY.get(), thKaguyaMod.entity.living.EntitySunFlowerFairy.createAttributes().build());
        event.put(THKaguyaEntities.TH_PHANTOM.get(), thKaguyaMod.entity.living.EntityTHPhantom.createAttributes().build());
        event.put(THKaguyaEntities.TOZIKO.get(), thKaguyaMod.entity.living.EntityToziko.createAttributes().build());
        event.put(THKaguyaEntities.WRIGGLE.get(), thKaguyaMod.entity.living.EntityWriggle.createAttributes().build());

        // 村民系（阶段 7 补完）：霖之助直接复用村民属性
        event.put(THKaguyaEntities.RINNOSUKE.get(), Villager.createAttributes().build());
    }

    /** 自然生成规则注册 */
    private void commonSetup(FMLCommonSetupEvent event)
    {
        event.enqueueWork(() ->
        {
            // 弹幕图案注册表初始化（依赖 danmakuLevel 配置，须在配置加载后调用）
            DanmakuPatternRegistry.setDanmakuPattern();

            // 注意：RegistryObject#get() は commonSetup では呼んではいけない
            //   （「Some intrusive holders were not registered」で専用サーバーが起動失敗する）。
            //   Item インスタンスを実行時に new するのも同じ理由で禁止。

            // 符卡注册（阶段 3d-5，对齐原版 CommonProxy.registerSpellCards）
            thKaguyaMod.registry.SpellCardRegistry.registerSpellCard(thKaguyaMod.entity.spellcard.THSC_MusouFuuin.class, "thkaguyamod", "MusouFuuin", 0);
            thKaguyaMod.registry.SpellCardRegistry.registerSpellCard(thKaguyaMod.entity.spellcard.THSC_MasterSpark.class, "thkaguyamod", "MasterSpark", 1);
            thKaguyaMod.registry.SpellCardRegistry.registerSpellCard(thKaguyaMod.entity.spellcard.THSC_Kasho_no_Eimin.class, "thkaguyamod", "Kasho_no_Eimin", 2);
            thKaguyaMod.registry.SpellCardRegistry.registerSpellCard(thKaguyaMod.entity.spellcard.THSC_MeteonicShower.class, "thkaguyamod", "MeteonicShower", 3);
            thKaguyaMod.registry.SpellCardRegistry.registerSpellCard(thKaguyaMod.entity.spellcard.THSC_Nami_to_Tubu_no_Kyoukai.class, "thkaguyamod", "Nami_to_Tubu_no_Kyoukai", 4);
            thKaguyaMod.registry.SpellCardRegistry.registerSpellCard(thKaguyaMod.entity.spellcard.THSC_NijuuKokushichou.class, "thkaguyamod", "NijuuKokushichou", 5);
            thKaguyaMod.registry.SpellCardRegistry.registerSpellCard(thKaguyaMod.entity.spellcard.THSC_ScarletShoot.class, "thkaguyamod", "ScarletShoot", 6);
            thKaguyaMod.registry.SpellCardRegistry.registerSpellCard(thKaguyaMod.entity.spellcard.THSC_Zenzinrui_no_Hisouten.class, "thkaguyamod", "Zenzinrui_no_Hisouten", 7);
            thKaguyaMod.registry.SpellCardRegistry.registerSpellCard(thKaguyaMod.entity.spellcard.THSC_SatuzinDoll.class, "thkaguyamod", "SatuzinDoll", 8);
            thKaguyaMod.registry.SpellCardRegistry.registerSpellCard(thKaguyaMod.entity.spellcard.THSC_PerfectFreeze.class, "thkaguyamod", "PerfectFreeze", 9);
            thKaguyaMod.registry.SpellCardRegistry.registerSpellCard(thKaguyaMod.entity.spellcard.THSC_HikouchuuNest.class, "thkaguyamod", "HikouchuuNest", 10);
            thKaguyaMod.registry.SpellCardRegistry.registerSpellCard(thKaguyaMod.entity.spellcard.THSC_KappaPororoca.class, "thkaguyamod", "Kappa'sPororoca", 11);
            thKaguyaMod.registry.SpellCardRegistry.registerSpellCard(thKaguyaMod.entity.spellcard.THSC_StardustReverie.class, "thkaguyamod", "StardustReverie", 12);
            thKaguyaMod.registry.SpellCardRegistry.registerSpellCard(thKaguyaMod.entity.spellcard.THSC_Kerochan_Fuuu_ni_Makezu.class, "thkaguyamod", "Kerochan_Fuuu_ni_Makezu", 13);
            thKaguyaMod.registry.SpellCardRegistry.registerSpellCard(thKaguyaMod.entity.spellcard.THSC_MiracleFruit.class, "thkaguyamod", "MiracleFruit", 14);
            thKaguyaMod.registry.SpellCardRegistry.registerSpellCard(thKaguyaMod.entity.spellcard.THSC_Fafurotskies_no_Kiseki.class, "thkaguyamod", "Fafrotskies_no_Kiseki", 15);
            thKaguyaMod.registry.SpellCardRegistry.registerSpellCard(thKaguyaMod.entity.spellcard.THSC_YouryokuSpoiler.class, "thkaguyamod", "YouryokuSpoiler", 16);
            thKaguyaMod.registry.SpellCardRegistry.registerSpellCard(thKaguyaMod.entity.spellcard.THSC_MosesMiracle.class, "thkaguyamod", "Moses_no_Kiseki", 17);
            thKaguyaMod.registry.SpellCardRegistry.registerSpellCard(thKaguyaMod.entity.spellcard.THSC_Yasaka_no_Kamikaze.class, "thkaguyamod", "Yasaka_no_Kamikaze", 18);
            thKaguyaMod.registry.SpellCardRegistry.registerSpellCard(thKaguyaMod.entity.spellcard.THSC_IcicleFall.class, "thkaguyamod", "IcicleFall", 19);
            thKaguyaMod.registry.SpellCardRegistry.registerSpellCard(thKaguyaMod.entity.spellcard.THSC_StarbowBreak.class, "thkaguyamod", "StarbowBreak", 20);
            thKaguyaMod.registry.SpellCardRegistry.registerSpellCard(thKaguyaMod.entity.spellcard.THSC_Catadioptric.class, "thkaguyamod", "Catadioptric", 21);
            thKaguyaMod.registry.SpellCardRegistry.registerSpellCard(thKaguyaMod.entity.spellcard.THSC_Mishagujisama.class, "thkaguyamod", "Mishagujisama", 22);
            thKaguyaMod.registry.SpellCardRegistry.registerSpellCard(thKaguyaMod.entity.spellcard.THSC_RedMagic.class, "thkaguyamod", "RedMagic", 23);
            thKaguyaMod.registry.SpellCardRegistry.registerSpellCard(thKaguyaMod.entity.spellcard.THSC_EternalMeek.class, "thkaguyamod", "EternalMeek", 24);
            thKaguyaMod.registry.SpellCardRegistry.registerSpellCard(thKaguyaMod.entity.spellcard.THSC_NonDirectionalLaser.class, "thkaguyamod", "Non-DirectionalLaser", 25);
            thKaguyaMod.registry.SpellCardRegistry.registerSpellCard(thKaguyaMod.entity.spellcard.THSC_Kachoufuugetu.class, "thkaguyamod", "KachoufuugetuShoufuurougetu", 26);
            thKaguyaMod.registry.SpellCardRegistry.registerSpellCard(thKaguyaMod.entity.spellcard.THSC_Spear_the_Gungnir.class, "thkaguyamod", "Spear_the_Gungnir", 27);
            thKaguyaMod.registry.SpellCardRegistry.registerSpellCard(thKaguyaMod.entity.spellcard.THSC_MoonlightRay.class, "thkaguyamod", "MoonlightRay", 28);
            thKaguyaMod.registry.SpellCardRegistry.registerSpellCard(thKaguyaMod.entity.spellcard.THSC_HoukaKenran.class, "thkaguyamod", "HoukaKenran", 29);
            thKaguyaMod.registry.SpellCardRegistry.registerSpellCard(thKaguyaMod.entity.spellcard.THSC_SaikouRanbu.class, "thkaguyamod", "SaikouRanbu", 30);
            thKaguyaMod.registry.SpellCardRegistry.registerSpellCard(thKaguyaMod.entity.spellcard.THSC_FujiyamaVolcano.class, "thkaguyamod", "FujiyamaVolcano", 33);
            thKaguyaMod.registry.SpellCardRegistry.registerSpellCard(thKaguyaMod.entity.spellcard.THSC_LittleBugStorm.class, "thkaguyamod", "LittleBugStorm", 34);
            thKaguyaMod.registry.SpellCardRegistry.registerSpellCard(thKaguyaMod.entity.spellcard.THSC_MeteorOnEarth.class, "thkaguyamod", "Meteor_on_Earth", 35);
            thKaguyaMod.registry.SpellCardRegistry.registerSpellCard(thKaguyaMod.entity.spellcard.THSC_GagoujiCyclone.class, "thkaguyamod", "GagoujiCyclone", 40);

            SpawnPlacements.register(THKaguyaEntities.TH_FAIRY.get(),
                    SpawnPlacements.Type.ON_GROUND,
                    Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                    (type, level, reason, pos, random) -> {
                        // 原版妖精在草/土/沙上自然生成；此处简化为主世界地面即可
                        return pos.getY() >= 64 && level.canSeeSky(pos);
                    });

            // 霖之助：忠实原版 EntityRinnosuke#getCanSpawnHere
            SpawnPlacements.register(THKaguyaEntities.RINNOSUKE.get(),
                    SpawnPlacements.Type.ON_GROUND,
                    Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                    (type, level, reason, pos, random) -> {
                        // 原版 rand.nextInt(100) < 98 → false（2% 才允许）
                        if (random.nextInt(100) < 98)
                        {
                            return false;
                        }
                        // 原版：脚下四周方块完全相同时拒绝生成（避开绝对平坦的地面）
                        BlockPos below = pos.below();
                        var ground = level.getBlockState(below);
                        return !(level.getBlockState(below.west()).equals(ground)
                                && level.getBlockState(below.east()).equals(ground)
                                && level.getBlockState(below.north()).equals(ground)
                                && level.getBlockState(below.south()).equals(ground));
                    });

            // 批次 C1：其余 9 类自然生成的 SpawnPlacements（原版 CommonProxy 的 addSpawn 只有 monster 默认规则 → 这里取其中「暗处才生成」那一半，
            // 早苗/咲夜/霖之助 当年是 ambient 类别 → 这里给它们「任何光照都允许」，白天也能遇到）
            SpawnPlacements.register(THKaguyaEntities.CIRNO.get(), SpawnPlacements.Type.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, (type, level, reason, pos, random) -> Monster.isDarkEnoughToSpawn(level, pos, random));
            SpawnPlacements.register(THKaguyaEntities.RUMIA.get(), SpawnPlacements.Type.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, (type, level, reason, pos, random) -> Monster.isDarkEnoughToSpawn(level, pos, random));
            SpawnPlacements.register(THKaguyaEntities.TOZIKO.get(), SpawnPlacements.Type.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, (type, level, reason, pos, random) -> Monster.isDarkEnoughToSpawn(level, pos, random));
            SpawnPlacements.register(THKaguyaEntities.WRIGGLE.get(), SpawnPlacements.Type.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, (type, level, reason, pos, random) -> Monster.isDarkEnoughToSpawn(level, pos, random));
            SpawnPlacements.register(THKaguyaEntities.TH_PHANTOM.get(), SpawnPlacements.Type.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, (type, level, reason, pos, random) -> Monster.isDarkEnoughToSpawn(level, pos, random));
            SpawnPlacements.register(THKaguyaEntities.SUNFLOWER_FAIRY.get(), SpawnPlacements.Type.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, (type, level, reason, pos, random) -> Monster.isDarkEnoughToSpawn(level, pos, random));
            SpawnPlacements.register(THKaguyaEntities.DANMAKU_CREEPER.get(), SpawnPlacements.Type.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, (type, level, reason, pos, random) -> Monster.isDarkEnoughToSpawn(level, pos, random));
            SpawnPlacements.register(THKaguyaEntities.SANAE.get(), SpawnPlacements.Type.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                    (type, level, reason, pos, random) -> true);
            SpawnPlacements.register(THKaguyaEntities.SAKUYA.get(), SpawnPlacements.Type.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                    (type, level, reason, pos, random) -> true);
            // A15：原版 registerEntitySpawn 里霊夢/神子两条 addSpawn 存在，但 1.20.1 之前没注册 SpawnPlacements，
            // 会落到「无生成规则」的默认分支。这里按原版 ambient 类别的语义补上（任何光照都允许）。
            SpawnPlacements.register(THKaguyaEntities.REIMU.get(), SpawnPlacements.Type.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                    (type, level, reason, pos, random) -> true);
            SpawnPlacements.register(THKaguyaEntities.MIKO.get(), SpawnPlacements.Type.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                    (type, level, reason, pos, random) -> true);

            // ディスペンサー挙動（原版 CommonProxy.registerDispenser，阶段 7 补完）
            //   銀のナイフ 4 色 → EntitySilverKnife 射出 / 弾アイテム 19 種 → 弾幕発射
            thKaguyaMod.dispense.THKaguyaDispenserBehaviors.register();
        });
    }

    /* ==================== 保留原版 GUI ID 常量（阶段 5 GUI 迁移用）==================== */
    /** 弾幕作業台GUI */
    public static final int GUI_DANMAKU_CRAFTING_ID      = 0;
    /** レーザー弾幕作業台GUI */
    public static final int GUI_DANMAKU_CRAFTING_LASER_ID = 1;
    /* 原版 GUI_MERCHANT_SANAE_ID = 2 已删除：早苗/咲夜的商店改走原版 MerchantMenu，
       原版的 ContainerMerchantSanae / GuiMerchantSanae 本身也没有任何入口（openGui 调用被注释）。 */
    /** 打ち出の小槌GUI */
    public static final int GUI_MIRACLE_MALLET_ID        = 3;
}
