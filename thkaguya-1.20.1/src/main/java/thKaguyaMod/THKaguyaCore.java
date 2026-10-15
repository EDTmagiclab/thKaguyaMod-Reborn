package thKaguyaMod;

import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.entity.monster.Monster;
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
import thKaguyaMod.init.THKaguyaBlocks;
import thKaguyaMod.init.THKaguyaConfig;
import thKaguyaMod.init.THKaguyaCreativeTab;
import thKaguyaMod.init.THKaguyaEntities;
import thKaguyaMod.init.THKaguyaItems;
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

        // 注册层：物品 / 方块 / 方块实体 / 创造标签 / 实体
        THKaguyaItems.ITEMS.register(modEventBus);
        THKaguyaBlocks.BLOCKS.register(modEventBus);
        THKaguyaBlocks.BLOCK_ENTITIES.register(modEventBus);
        THKaguyaCreativeTab.CREATIVE_TABS.register(modEventBus);
        THKaguyaEntities.ENTITIES.register(modEventBus);

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
    }

    /** 自然生成规则注册 */
    private void commonSetup(FMLCommonSetupEvent event)
    {
        event.enqueueWork(() ->
        {
            // 弹幕图案注册表初始化（依赖 danmakuLevel 配置，须在配置加载后调用）
            DanmakuPatternRegistry.setDanmakuPattern();

            SpawnPlacements.register(THKaguyaEntities.TH_FAIRY.get(),
                    SpawnPlacements.Type.ON_GROUND,
                    Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                    (type, level, reason, pos, random) -> {
                        // 原版妖精在草/土/沙上自然生成；此处简化为主世界地面即可
                        return pos.getY() >= 64 && level.canSeeSky(pos);
                    });
        });
    }

    /* ==================== 保留原版 GUI ID 常量（阶段 5 GUI 迁移用）==================== */
    /** 弾幕作業台GUI */
    public static final int GUI_DANMAKU_CRAFTING_ID      = 0;
    /** レーザー弾幕作業台GUI */
    public static final int GUI_DANMAKU_CRAFTING_LASER_ID = 1;
    /** 守矢神社出張早苗さんGUI */
    public static final int GUI_MERCHANT_SANAE_ID        = 2;
    /** 打ち出の小槌GUI */
    public static final int GUI_MIRACLE_MALLET_ID        = 3;
}
