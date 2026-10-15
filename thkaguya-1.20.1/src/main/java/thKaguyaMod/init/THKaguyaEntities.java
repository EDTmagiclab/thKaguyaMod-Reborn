package thKaguyaMod.init;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import thKaguyaMod.THKaguyaCore;
import thKaguyaMod.entity.effect.EntitySpellCardCircle;
import thKaguyaMod.entity.item.EntityCursedDecoyDoll;
import thKaguyaMod.entity.item.EntityHisou;
import thKaguyaMod.entity.item.EntityKinkakuzi;
import thKaguyaMod.entity.item.EntityMarisaBroom;
import thKaguyaMod.entity.item.EntityMazinkyoukan;
import thKaguyaMod.entity.item.EntityMiniHakkero;
import thKaguyaMod.entity.item.EntityPendulum;
import thKaguyaMod.entity.item.EntitySakuyaStopWatch;
import thKaguyaMod.entity.item.EntitySakuyaWatch;
import thKaguyaMod.entity.item.EntitySilverKnife;
import thKaguyaMod.entity.item.EntitySpiritualStrikeTalisman;
import thKaguyaMod.entity.item.EntitySukima;
import thKaguyaMod.entity.item.EntityYuukaParasol;
import thKaguyaMod.entity.living.EntityCirno;
import thKaguyaMod.entity.living.EntityDanmakuCreeper;
import thKaguyaMod.entity.living.EntityFamiliar;
import thKaguyaMod.entity.living.EntityMiko;
import thKaguyaMod.entity.living.EntityReimu;
import thKaguyaMod.entity.living.EntityRumia;
import thKaguyaMod.entity.living.EntitySakuya;
import thKaguyaMod.entity.living.EntitySanae;
import thKaguyaMod.entity.living.EntitySunFlowerFairy;
import thKaguyaMod.entity.living.EntityTHFairy;
import thKaguyaMod.entity.living.EntityTHPhantom;
import thKaguyaMod.entity.living.EntityToziko;
import thKaguyaMod.entity.living.EntityWriggle;
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

    /* ==================== living 实体（阶段 3d-6，12 个 boss/杂鱼）==================== */

    public static final RegistryObject<EntityType<EntityCirno>> CIRNO =
            ENTITIES.register("cirno",
                    () -> EntityType.Builder.of(
                                    (EntityType.EntityFactory<EntityCirno>) (type, level) -> new EntityCirno(type, level),
                                    MobCategory.MONSTER)
                            .sized(1.0F, 1.8F).clientTrackingRange(64).build("cirno"));

    public static final RegistryObject<EntityType<EntityDanmakuCreeper>> DANMAKU_CREEPER =
            ENTITIES.register("danmaku_creeper",
                    () -> EntityType.Builder.of(
                                    (EntityType.EntityFactory<EntityDanmakuCreeper>) (type, level) -> new EntityDanmakuCreeper(type, level),
                                    MobCategory.MONSTER)
                            .sized(0.6F, 1.7F).clientTrackingRange(64).build("danmaku_creeper"));

    public static final RegistryObject<EntityType<EntityFamiliar>> FAMILIAR =
            ENTITIES.register("familiar",
                    () -> EntityType.Builder.of(
                                    (EntityType.EntityFactory<EntityFamiliar>) (type, level) -> new EntityFamiliar(type, level),
                                    MobCategory.MONSTER)
                            .sized(0.6F, 0.6F).clientTrackingRange(64).build("familiar"));

    public static final RegistryObject<EntityType<EntityMiko>> MIKO =
            ENTITIES.register("miko",
                    () -> EntityType.Builder.of(
                                    (EntityType.EntityFactory<EntityMiko>) (type, level) -> new EntityMiko(type, level),
                                    MobCategory.MONSTER)
                            .sized(1.0F, 1.8F).clientTrackingRange(64).build("miko"));

    public static final RegistryObject<EntityType<EntityReimu>> REIMU =
            ENTITIES.register("reimu",
                    () -> EntityType.Builder.of(
                                    (EntityType.EntityFactory<EntityReimu>) (type, level) -> new EntityReimu(type, level),
                                    MobCategory.MONSTER)
                            .sized(1.0F, 1.8F).clientTrackingRange(64).build("reimu"));

    public static final RegistryObject<EntityType<EntityRumia>> RUMIA =
            ENTITIES.register("rumia",
                    () -> EntityType.Builder.of(
                                    (EntityType.EntityFactory<EntityRumia>) (type, level) -> new EntityRumia(type, level),
                                    MobCategory.MONSTER)
                            .sized(1.0F, 1.8F).clientTrackingRange(64).build("rumia"));

    public static final RegistryObject<EntityType<EntitySakuya>> SAKUYA =
            ENTITIES.register("sakuya",
                    () -> EntityType.Builder.of(
                                    (EntityType.EntityFactory<EntitySakuya>) (type, level) -> new EntitySakuya(type, level),
                                    MobCategory.MONSTER)
                            .sized(1.0F, 1.8F).clientTrackingRange(64).build("sakuya"));

    public static final RegistryObject<EntityType<EntitySanae>> SANAE =
            ENTITIES.register("sanae",
                    () -> EntityType.Builder.of(
                                    (EntityType.EntityFactory<EntitySanae>) (type, level) -> new EntitySanae(type, level),
                                    MobCategory.MONSTER)
                            .sized(0.6F, 1.7F).clientTrackingRange(64).build("sanae"));

    public static final RegistryObject<EntityType<EntitySunFlowerFairy>> SUNFLOWER_FAIRY =
            ENTITIES.register("sunflower_fairy",
                    () -> EntityType.Builder.of(
                                    (EntityType.EntityFactory<EntitySunFlowerFairy>) (type, level) -> new EntitySunFlowerFairy(type, level),
                                    MobCategory.MONSTER)
                            .sized(0.9F, 1.7F).clientTrackingRange(64).build("sunflower_fairy"));

    public static final RegistryObject<EntityType<EntityTHPhantom>> TH_PHANTOM =
            ENTITIES.register("th_phantom",
                    () -> EntityType.Builder.of(
                                    (EntityType.EntityFactory<EntityTHPhantom>) (type, level) -> new EntityTHPhantom(type, level),
                                    MobCategory.MONSTER)
                            .sized(1.0F, 1.0F).clientTrackingRange(64).build("th_phantom"));

    public static final RegistryObject<EntityType<EntityToziko>> TOZIKO =
            ENTITIES.register("toziko",
                    () -> EntityType.Builder.of(
                                    (EntityType.EntityFactory<EntityToziko>) (type, level) -> new EntityToziko(type, level),
                                    MobCategory.MONSTER)
                            .sized(1.0F, 1.8F).clientTrackingRange(64).build("toziko"));

    public static final RegistryObject<EntityType<EntityWriggle>> WRIGGLE =
            ENTITIES.register("wriggle",
                    () -> EntityType.Builder.of(
                                    (EntityType.EntityFactory<EntityWriggle>) (type, level) -> new EntityWriggle(type, level),
                                    MobCategory.MONSTER)
                            .sized(1.0F, 1.8F).clientTrackingRange(64).build("wriggle"));

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

    /* ==================== item 实体（阶段 3d-4，13 个）==================== */

    public static final RegistryObject<EntityType<EntityCursedDecoyDoll>> CURSED_DECOY_DOLL =
            ENTITIES.register("cursed_decoy_doll",
                    () -> EntityType.Builder.of(
                                    (EntityType.EntityFactory<EntityCursedDecoyDoll>) (type, level) -> new EntityCursedDecoyDoll(type, level),
                                    MobCategory.MONSTER)
                            .sized(1.0F, 1.8F).clientTrackingRange(64).build("cursed_decoy_doll"));

    public static final RegistryObject<EntityType<EntityHisou>> HISOU =
            ENTITIES.register("hisou",
                    () -> EntityType.Builder.of(
                                    (EntityType.EntityFactory<EntityHisou>) (type, level) -> new EntityHisou(type, level),
                                    MobCategory.MISC)
                            .sized(4.4F, 4.4F).clientTrackingRange(64).build("hisou"));

    public static final RegistryObject<EntityType<EntityKinkakuzi>> KINKAKUZI =
            ENTITIES.register("kinkakuzi",
                    () -> EntityType.Builder.of(
                                    (EntityType.EntityFactory<EntityKinkakuzi>) (type, level) -> new EntityKinkakuzi(type, level),
                                    MobCategory.MISC)
                            .sized(6.0F, 0.3F).clientTrackingRange(64).build("kinkakuzi"));

    public static final RegistryObject<EntityType<EntityMarisaBroom>> MARISA_BROOM =
            ENTITIES.register("marisa_broom",
                    () -> EntityType.Builder.of(
                                    (EntityType.EntityFactory<EntityMarisaBroom>) (type, level) -> new EntityMarisaBroom(type, level),
                                    MobCategory.MISC)
                            .sized(0.9F, 0.5F).clientTrackingRange(64).build("marisa_broom"));

    public static final RegistryObject<EntityType<EntityMazinkyoukan>> MAZINKYOUKAN =
            ENTITIES.register("mazinkyoukan",
                    () -> EntityType.Builder.of(
                                    (EntityType.EntityFactory<EntityMazinkyoukan>) (type, level) -> new EntityMazinkyoukan(type, level),
                                    MobCategory.MISC)
                            .sized(2.0F, 2.0F).clientTrackingRange(64).build("mazinkyoukan"));

    public static final RegistryObject<EntityType<EntityMiniHakkero>> MINI_HAKKERO =
            ENTITIES.register("mini_hakkero",
                    () -> EntityType.Builder.of(
                                    (EntityType.EntityFactory<EntityMiniHakkero>) (type, level) -> new EntityMiniHakkero(type, level),
                                    MobCategory.MISC)
                            .sized(0.4F, 0.4F).clientTrackingRange(64).build("mini_hakkero"));

    public static final RegistryObject<EntityType<EntityPendulum>> PENDULUM =
            ENTITIES.register("pendulum",
                    () -> EntityType.Builder.of(
                                    (EntityType.EntityFactory<EntityPendulum>) (type, level) -> new EntityPendulum(type, level),
                                    MobCategory.MISC)
                            .sized(0.1F, 0.1F).clientTrackingRange(64).build("pendulum"));

    public static final RegistryObject<EntityType<EntitySakuyaStopWatch>> SAKUYA_STOP_WATCH =
            ENTITIES.register("sakuya_stop_watch",
                    () -> EntityType.Builder.of(
                                    (EntityType.EntityFactory<EntitySakuyaStopWatch>) (type, level) -> new EntitySakuyaStopWatch(type, level),
                                    MobCategory.MISC)
                            .sized(0.2F, 0.2F).clientTrackingRange(64).build("sakuya_stop_watch"));

    public static final RegistryObject<EntityType<EntitySakuyaWatch>> SAKUYA_WATCH =
            ENTITIES.register("sakuya_watch",
                    () -> EntityType.Builder.of(
                                    (EntityType.EntityFactory<EntitySakuyaWatch>) (type, level) -> new EntitySakuyaWatch(type, level),
                                    MobCategory.MISC)
                            .sized(1.0F, 1.0F).clientTrackingRange(64).build("sakuya_watch"));

    public static final RegistryObject<EntityType<EntitySilverKnife>> SILVER_KNIFE =
            ENTITIES.register("silver_knife",
                    () -> EntityType.Builder.of(
                                    (EntityType.EntityFactory<EntitySilverKnife>) (type, level) -> new EntitySilverKnife(type, level),
                                    MobCategory.MISC)
                            .sized(0.1F, 0.1F).clientTrackingRange(64).build("silver_knife"));

    public static final RegistryObject<EntityType<EntitySpiritualStrikeTalisman>> SPIRITUAL_STRIKE_TALISMAN =
            ENTITIES.register("spiritual_strike_talisman",
                    () -> EntityType.Builder.of(
                                    (EntityType.EntityFactory<EntitySpiritualStrikeTalisman>) (type, level) -> new EntitySpiritualStrikeTalisman(type, level),
                                    MobCategory.MISC)
                            .sized(0.2F, 0.2F).clientTrackingRange(64).build("spiritual_strike_talisman"));

    public static final RegistryObject<EntityType<EntitySukima>> SUKIMA =
            ENTITIES.register("sukima",
                    () -> EntityType.Builder.of(
                                    (EntityType.EntityFactory<EntitySukima>) (type, level) -> new EntitySukima(type, level),
                                    MobCategory.MISC)
                            .sized(1.5F, 1.0F).clientTrackingRange(64).build("sukima"));

    public static final RegistryObject<EntityType<EntityYuukaParasol>> YUUKA_PARASOL =
            ENTITIES.register("yuuka_parasol",
                    () -> EntityType.Builder.of(
                                    (EntityType.EntityFactory<EntityYuukaParasol>) (type, level) -> new EntityYuukaParasol(type, level),
                                    MobCategory.MISC)
                            .sized(0.1F, 0.1F).clientTrackingRange(64).build("yuuka_parasol"));
}
