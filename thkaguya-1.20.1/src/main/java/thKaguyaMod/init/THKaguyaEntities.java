package thKaguyaMod.init;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import thKaguyaMod.THKaguyaCore;
import thKaguyaMod.entity.effect.EntitySpellCardCircle;
import thKaguyaMod.entity.living.EntityTHFairy;
import thKaguyaMod.entity.shot.EntityDragonNeckJewel;
import thKaguyaMod.entity.shot.EntityHomingAmulet;
import thKaguyaMod.entity.shot.EntityMasterSpark;
import thKaguyaMod.entity.shot.EntityMusouFuuin;
import thKaguyaMod.entity.shot.EntityNuclearShot;
import thKaguyaMod.entity.shot.EntityOnmyoudama;
import thKaguyaMod.entity.shot.EntitySanaeWind;
import thKaguyaMod.entity.shot.EntityTHHenyoriLaser;
import thKaguyaMod.entity.shot.EntityTHLaser;
import thKaguyaMod.entity.shot.EntityTHSetLaser;
import thKaguyaMod.entity.shot.EntityTHShot;
import thKaguyaMod.entity.spellcard.EntitySpellCard;

/**
 * エンティティの登録クラス（1.20.1 移植版）
 * 原版 45 个实体，阶段 3a 先注册「最小链路」的 2 个：
 *   EntityTHFairy（妖精，弹幕基座）+ EntityTHShot（弹体基座）。
 * 其余 43 个实体在阶段 3c/3d 补回。
 */
public class THKaguyaEntities
{
    public static final DeferredRegister<EntityType<?>> ENTITIES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, THKaguyaCore.MODID);

    /** 妖精 */
    public static final RegistryObject<EntityType<EntityTHFairy>> TH_FAIRY =
            ENTITIES.register("th_fairy",
                    () -> EntityType.Builder.of(
                                    (EntityType.EntityFactory<EntityTHFairy>) (type, level) -> new EntityTHFairy(type, level),
                                    MobCategory.MONSTER)
                            .sized(0.6F, 1.5F)
                            .clientTrackingRange(64)
                            .build("th_fairy"));

    /** 弹体 */
    public static final RegistryObject<EntityType<EntityTHShot>> TH_SHOT =
            ENTITIES.register("th_shot",
                    () -> EntityType.Builder.of(
                                    (EntityType.EntityFactory<EntityTHShot>) (type, level) -> new EntityTHShot(type, level),
                                    MobCategory.MISC)
                            .sized(0.1F, 0.1F)
                            .clientTrackingRange(64)
                            .updateInterval(1)
                            .build("th_shot"));

    /** 激光弹体 */
    public static final RegistryObject<EntityType<EntityTHLaser>> TH_LASER =
            ENTITIES.register("th_laser",
                    () -> EntityType.Builder.of(
                                    (EntityType.EntityFactory<EntityTHLaser>) (type, level) -> new EntityTHLaser(type, level),
                                    MobCategory.MISC)
                            .sized(0.1F, 0.1F)
                            .clientTrackingRange(64)
                            .updateInterval(1)
                            .build("th_laser"));

    /** 符卡魔法阵 */
    public static final RegistryObject<EntityType<EntitySpellCardCircle>> SPELL_CARD_CIRCLE =
            ENTITIES.register("spell_card_circle",
                    () -> EntityType.Builder.of(
                                    (EntityType.EntityFactory<EntitySpellCardCircle>) (type, level) -> new EntitySpellCardCircle(type, level),
                                    MobCategory.MISC)
                            .sized(0.5F, 0.5F)
                            .clientTrackingRange(64)
                            .build("spell_card_circle"));

    /** 符卡实体 */
    public static final RegistryObject<EntityType<EntitySpellCard>> SPELL_CARD =
            ENTITIES.register("spell_card",
                    () -> EntityType.Builder.of(
                                    (EntityType.EntityFactory<EntitySpellCard>) (type, level) -> new EntitySpellCard(type, level),
                                    MobCategory.MISC)
                            .sized(0.4F, 0.4F)
                            .clientTrackingRange(64)
                            .build("spell_card"));

    /* ==================== 特殊弹体（阶段 3d-3）==================== */

    public static final RegistryObject<EntityType<EntityDragonNeckJewel>> DRAGON_NECK_JEWEL =
            ENTITIES.register("dragon_neck_jewel",
                    () -> EntityType.Builder.of(
                                    (EntityType.EntityFactory<EntityDragonNeckJewel>) (type, level) -> new EntityDragonNeckJewel(type, level),
                                    MobCategory.MISC)
                            .sized(0.3F, 0.3F).clientTrackingRange(64).updateInterval(1).build("dragon_neck_jewel"));

    public static final RegistryObject<EntityType<EntityHomingAmulet>> HOMING_AMULET =
            ENTITIES.register("homing_amulet",
                    () -> EntityType.Builder.of(
                                    (EntityType.EntityFactory<EntityHomingAmulet>) (type, level) -> new EntityHomingAmulet(type, level),
                                    MobCategory.MISC)
                            .sized(0.3F, 0.3F).clientTrackingRange(64).updateInterval(1).build("homing_amulet"));

    public static final RegistryObject<EntityType<EntityMasterSpark>> MASTER_SPARK =
            ENTITIES.register("master_spark",
                    () -> EntityType.Builder.of(
                                    (EntityType.EntityFactory<EntityMasterSpark>) (type, level) -> new EntityMasterSpark(type, level),
                                    MobCategory.MISC)
                            .sized(0.3F, 0.3F).clientTrackingRange(64).updateInterval(1).build("master_spark"));

    public static final RegistryObject<EntityType<EntityMusouFuuin>> MUSOU_FUUIN =
            ENTITIES.register("musou_fuuin",
                    () -> EntityType.Builder.of(
                                    (EntityType.EntityFactory<EntityMusouFuuin>) (type, level) -> new EntityMusouFuuin(type, level),
                                    MobCategory.MISC)
                            .sized(0.3F, 0.3F).clientTrackingRange(64).updateInterval(1).build("musou_fuuin"));

    public static final RegistryObject<EntityType<EntityNuclearShot>> NUCLEAR_SHOT =
            ENTITIES.register("nuclear_shot",
                    () -> EntityType.Builder.of(
                                    (EntityType.EntityFactory<EntityNuclearShot>) (type, level) -> new EntityNuclearShot(type, level),
                                    MobCategory.MISC)
                            .sized(0.3F, 0.3F).clientTrackingRange(64).updateInterval(1).build("nuclear_shot"));

    public static final RegistryObject<EntityType<EntityOnmyoudama>> ONMYOUDAMA =
            ENTITIES.register("onmyoudama",
                    () -> EntityType.Builder.of(
                                    (EntityType.EntityFactory<EntityOnmyoudama>) (type, level) -> new EntityOnmyoudama(type, level),
                                    MobCategory.MISC)
                            .sized(0.3F, 0.3F).clientTrackingRange(64).updateInterval(1).build("onmyoudama"));

    public static final RegistryObject<EntityType<EntitySanaeWind>> SANAE_WIND =
            ENTITIES.register("sanae_wind",
                    () -> EntityType.Builder.of(
                                    (EntityType.EntityFactory<EntitySanaeWind>) (type, level) -> new EntitySanaeWind(type, level),
                                    MobCategory.MISC)
                            .sized(0.3F, 0.3F).clientTrackingRange(64).updateInterval(1).build("sanae_wind"));

    public static final RegistryObject<EntityType<EntityTHHenyoriLaser>> TH_HENYORI_LASER =
            ENTITIES.register("th_henyori_laser",
                    () -> EntityType.Builder.of(
                                    (EntityType.EntityFactory<EntityTHHenyoriLaser>) (type, level) -> new EntityTHHenyoriLaser(type, level),
                                    MobCategory.MISC)
                            .sized(0.3F, 0.3F).clientTrackingRange(64).updateInterval(1).build("th_henyori_laser"));

    public static final RegistryObject<EntityType<EntityTHSetLaser>> TH_SET_LASER =
            ENTITIES.register("th_set_laser",
                    () -> EntityType.Builder.of(
                                    (EntityType.EntityFactory<EntityTHSetLaser>) (type, level) -> new EntityTHSetLaser(type, level),
                                    MobCategory.MISC)
                            .sized(0.3F, 0.3F).clientTrackingRange(64).updateInterval(1).build("th_set_laser"));
}
