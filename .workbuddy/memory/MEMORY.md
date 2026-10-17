# 项目长期记忆：五つの難題MOD+ 1.7.10 → 1.20.1 移植

## 定位与路径
东方同人 mod 移植到 1.20.1 Forge。新工程 `thkaguya-1.20.1/`（modid `thkaguya`，包名 `thKaguyaMod`）→ 产物 `build/libs/thkaguya-1.0.0.jar`。原版唯一权威基线 `extract/sources/java/thKaguyaMod/`（266 文件）。mod 目录 `F:\mc\versions\newserver\.minecraft\versions\1.20.1-Forge_47.4.28\mods`。**每次 build 成功必须把 jar 覆盖复制到 mods 目录**（核验 md5/mtime；勿用 `build|tail&&cp` 链，tail 恒 0 会掩盖失败）。完整论述见 `移植任务书.md`「关键迁移约定」。

## 构建环境
Forge 1.20.1 47.4.28 / ForgeGradle 6；JDK 17 `C:/Program Files/Java/jdk-17`；Gradle 8.5 `/e/git/ad-coze/arcadian-dream-fix-1.20/gradle-9.2.1/gradle-8.5/bin/gradle`；代理 `127.0.0.1:16888`（6232 端口坏）；MCPRepo 抽风用 `--offline`。

## 进度快照（2026-10-17；HEAD `dc18b1c`；jar md5 每次构建都变，版本标识只认 HEAD）

- **dc18b1c**：妖怪改「露天地表 + 无限发光」；幽灵/使魔换专用 billboard 渲染器（RenderTHPhantom/RenderFamiliar）；魔理沙帽补 ModelMarisaHat（原版 7 部件 + scale 0.4F）。
- 阶段 1–7 全结清 + 哑物品三批全清 + 批次 A `72db034`（配方/战利品表/一行级修复）+ 批次 B `1264ebe`（掉落链）+ 实测三修 `5231247` + 符卡掉落编号 `62b121f` + 幽香伞开花 `e6a18d5` 全部验收；**批次 C1 自然生成 ✅ + 批次 D 五件物品 ✅（`e895344`）**，待用户实测（新世界看刷怪 / 五件物品手感）。
- 符卡 35 张已注册（BrilliantDragonBullet 原版即注释）；核验报告 34/36 一致；Review ≈85–88%；用户批复顺序 A→B→C1，符卡配方/盔甲层接受；时停已可用（报告 §4.12 是误判，已在 `f51a746` 勘误）。
- 时停天空反色 ✅；小神霊 BER+8 色 ✅；音效核查：原版无语音资源（0 .ogg），实体音效靠蝙蝠×1.95 拟声（早苗/咲夜）与村民音（霖之助，原版如此），移植版音效三件套未搬，待用户定。

## 铁律索引（56 条；完整论述见 `移植任务书.md`「关键迁移约定」1–70）
1. **资源标识符全小写**（文件名+目录名）；`static final ResourceLocation` 类加载即校验，大写→启动崩溃。
2. **贴图目录必须 `textures/item/`/`textures/block/`（单数）**；复数→物品模型缺材质（latest.log `Missing textures in model`）。
3. **实体三大注册缺一不可**：EntityType+EntityAttributeCreationEvent+EntityRenderer。
4. **EntityType 沿构造链显式传递**，否则 getType() 全错（服务端子类/客户端父类）。
5. **yaw 两套约定**：atan2 来的（弹幕/激光）→渲染裸 yaw **绝不加 180**；vanilla 的（道具实体）→`180-yaw`。魔炮判据：光柱穿过迷你八卦炉两枚同心圆环中心。
6. **billboard 必须 NoCull RenderType**（顶点序被判裏面，cull=整片不可见）；3D 手工几何（苦无/矢/结晶）保持 Cull。
7. **弹幕混合**走 `client/render/THRenderTypes.java`（加性/発光/幽霊弾/depthWrite/全亮/白芯）；RenderStateShard 内部 shard 类与构造全 public→自建 RenderType 无需 AT/Mixin。
8. **命中搜索范围按到达距离伸展**：`getHitCheckReach()` 默认 1.0，激光覆写 `getLaserLength()+getShotSize()`（否则激光零伤害，c85ea86）。
9. **SetLaser 蓄力期光束 1/10 粗细**（anim<0 && instanceof EntityTHSetLaser）。
10. **`ignoreFrustumCheck=true`→构造里 `this.noCulling=true`**（原版清单：SpellCardCircle/SakuyaStopWatch/SakuyaWatch/SpiritualStrikeTalisman/YuukaParasol/Rumia/EntityTHLaser）。
11. **覆写 `rideTick()` 必须自己驱动 `tick()`**（`if(this.canUpdate()) this.tick();`），否则实体永不 tick（e3a08eb）。
12. **`getLightEmission`≠`getBlockLightValue`**：后者=`level.getBrightness(LightLayer.BLOCK,pos)`（赤石蓄光曾恒 0）。
13. **追踪弹改 angle 不够**：须按当前速率重定向速度并同步 `lastShotMotionX/Y/Z`（tick() 每 tick 先恢复 lastShotMotion）。
14. **`EntityTHShot.angle` 未同步**，客户端恒 (0,0,0)，渲染/客户端逻辑不能用它。
15. **1.20.1 射线选择看 `isPickable()`**（原版 canBeCollidedWith）。
16. **禁运行期 new Item/Block 实例**（intrusive holder 启动失败）→`SpecialShotRegistry` 存实例（1e9815d）；`RegistryObject#get()` 不能在 commonSetup 调。
17. **反射一律 `catch (Throwable)`+日志**，禁空 catch（最贵的陷阱）。
18. **流体**：放置后 `((ServerLevel)level).getFluidTicks().clearArea(...)` 撤销预约才静止；移除用 `setBlock(pos, AIR, Block.UPDATE_ALL_IMMEDIATE)`。
19. **禁大范围扫描清测试残留**（曾误删 141309 天然水域方块）；只清自己记录的坐标。
20. **API 差异**：`ticksExisted→tickCount`；`setSize`/`isInRangeToRenderDist` 不存在（碰撞箱由 `EntityType.Builder.sized` 定）；**`Item.getUseDuration` 默认返回 0**，用 `setItemInUse` 的道具必须覆写否则蓄力全废（1bdcc8b）；手持判定用 `getMainHandItem()`，不是 `getUseItem()`（松手即空→弹体 discard）。
21. 诊断代码验完删除，javap 核对 jar 无残留字符串。
22. **原版子类覆写的方法要逐个核对移植版是否也覆写**（0ec5093）：`EntityTHLaser` 原版覆写 `updateAngle()`（服务端专用）漏掉→固定激光 `EntityTHSetLaser` 初速恒 0→yaw/pitch 归零→恋符 5 条激光全同向（「圆筒」）。**音效三件套（`getLivingSound`/`getHurtSound`/`getDeathSound`/`getSoundPitch`）也曾整包漏搬**→早苗/咲夜丢蝙蝠×1.95 拟声（2026-10-17 核查）。自检：原版类逐个 grep `@Override`/`void tick`。
23. **`Item.inventoryTick` 在移动之前调用**（7d4bb6c）：`getDeltaMovement()`/`getBoundingBox()` 是上 tick 移动前的值；照抄 1.7.10「速度门限+当前位朝前伸判定盒」会漏判整段位移（楼观剑突进斩击全空）。修法：`getX()-xo` 兜底门限+拉伸判定盒。
24. **1.7.10 反射混合不看顶点 alpha**（7d4bb6c）：`GL_ONE, GL_ONE_MINUS_SRC_COLOR` 只取 RGB，发光层画在本体之后会把本体盖成色盘（御币弹阴陽玉被红 halo 盖死）；1.20.1 把 rgb/alpha 分开输出，改「光效在前、本体在后」。
25. **子类的便宜构造必须自己传 EntityType**（7c1de2b）：基类便宜构造写死 `TH_SHOT`，子类复用→客户端生成基类→专用渲染器成死代码（「巨大的橙色子弹」）。自检 `javap -p -c <jar> <类> | grep TH_SHOT`。**「换了实现症状一字未变」= 代码根本没执行**。
26. **1.20.1 `Level#clip` 永不返回 null**：未命中返回 `HitResult.Type.MISS`；凡 1.7.10 `if(mop != null)` 都要改成 `getType() == HitResult.Type.BLOCK`，否则「从不 miss」（已中招 3 处）。
27. **改资源文件先看 `git diff --stat`，整文件增删=行尾被改写**（b7234f7）：脚本 `newline="\n"` 把 CRLF 全转 LF，diff 3 行变 378 行；读写一律 `newline=""`。另：lang 缺 color/danmakuForm/Infinity 键，照抄 `StatCollector` 的 tooltip 会显示原始 key。
28. **资源文件行尾/编码按字节保真**（e74b14b）：`io.open(path)` 通用换行把 CRLF 读成 LF→5 个 lang 全被写成 LF。本工程 **lang=CRLF、model=LF**，都不许统一：读写走 `open(p,'rb')`/`open(p,'wb')`；lang 无 BOM。
29. **`item/generated` 的 tintIndex 就是层号**（e74b14b）：`layer0`→0、`layer1`→1；原版 `getColorFromItemStack` 的 pass 0/1 正好对应「ItemColor 只给 tintIndex 0 上色」。加 `layer1` 必须同时把 `<name>_2.png` 放进 `textures/item/`，否则图标变紫黑格。
30. **工作台/容器产出的 stack 也要换成拆分后的物品**（3c45907）：`item+damage` 拆成 N 个独立 Item 后，凡是「造出这个物品」的代码都要改，否则产物还是老物品。修法：`shotItem(type)`/`laserItem(type)`+`shotTypeOf`/`laserTypeOf`，槽位判定用 `isShotMaterialItem`/`isLaserMaterialItem`。
31. **原版靠 metadata 的原版物品在 1.20.1 也是拆开的，`getDamageValue()` 恒 0**（fd4f634）：照抄 `SELECTABLE_COLOR[damage % 16]` 读成索引 0→「放红染料出白弹」。做法：`DYE_BY_LEGACY_META` 表+`dyeColorIndex` 查表，槽位 `instanceof DyeItem`。凡 1.7.10 搬来的 `damage % N` 都这么自查。
32. **自建 `SimpleContainer` 不会通知菜单，得手动 `addListener`**（e2cb531）：`AbstractContainerMenu` 不实现 `ContainerListener`→`slotsChanged` 一次都不触发；症状：参数槽改完产物不刷新。做法：`ContainerListener recalcListener = this::slotsChanged`+输入槽 `addListener`；结果槽不挂。
33. **原版被 `/* */` 注释掉的整段是死代码，别当需求搬**（7749419，万宝槌）：`ItemUchide.onItemRightClick` 前半段（潜行右键开 GUI+射线改造掉落物）整段注释掉，原版打不开 GUI；活行为只有「饱食度>0 时 20 格内自己的弹幕 `setShotSize(+0.5F)`、播 bow、+30 耐久」+每 tick 自愈 1 点。
34. **盔甲移植**（323fb3a）：`EnumHelper.addArmorMaterial`→实现 `ArmorMaterial`；`setMaxDamage(0)`→材质槽位返回 0；`getArmorTexture` 走 `IForgeItem` 返回完整路径（原版 64×32 可用，裤子才要 layer_2）；回调改名 `hitEntity→hurtEnemy`/`onUpdate→inventoryTick`/`extinguish→clearFire`/`hasEffect→isFoil`；盔甲层贴图别放 `textures/item/` 当图标。
35. **自定义盔甲模型挂在 `head` 之下、`head` 留空**（e597eb1）：`HumanoidModel`+`CubeDeformation`+`PartPose.offset`，layer 在 `registerLayerDefinitions` 注册，`initializeClient`→`getHumanoidArmorModel`（只 HEAD 槽返回）；覆盖 `head` 会让帽子不随头转。`Item` 可变字段改 `Map<UUID, ...>` 防多玩家串号。
36. **`getPlayerPOVHitResult(..., SOURCE_ONLY)` 判水源不可靠**（3e9ea0f）：走 `ClipContext.Block.OUTLINE`，水无 outline 形状，落点常落在含水方块/池边。修法：`getFluidState(pos).is(FluidTags.WATER) && isSource()` 优先，再不中沿视线 0.1 格采样。
37. **1.20.1 `onItemRightClick` 拆成 use/useOn，对着方块右键只走 `useOn`**（5d4c12b）：javap 核实没有「useOn PASS 回落 use」；「对着某方块做点什么」必须两边都实现；`use()` 里的检测瞄准浅水时永远不执行。
38. **「全方位弹」必须绕竖直轴转，库里 `createRingShot`/`createCircleShot` 都是绕视线轴**（19ffba1）：平视时铺出竖圆墙、弹全往前飞；`ringSpan=0` 时旋转无效→全部弹同一方向。要水平 360°：`axis=new Vec3(0,1,0)` 逐发 `getVectorFromRotation` 再 `createShot(...,rotate=axis,...)`。参数调不动先算几何。
39. **时停天空反色=`ONE_MINUS_DST_COLOR`，1.20.1 用 `AFTER_SKY` 全屏 quad 复刻**（咲夜怀表）：原版球壳画在地形前→只反天空、alpha 淡出是死代码=硬切。三坑：① 事件 PoseStack 画 quad 会被相机旋转带竖→必须 GUI 正射影+`getModelViewStack()`；② `AFTER_SKY` 帧缓冲只有天空；③ 纯白负片对蓝天橙褐→青紫两遍。别用 post/invert。
40. **`Long.MIN_VALUE` 哨兵+`now-sentinel>1` 溢出成负→条件恒假**（6343b7e，天空一进游戏就变色）。正解：每 tick 先置 `false`、命中置 `true` 的布尔量。
41. **1.20.1「死了又活」=`LivingDeathEvent.setCanceled(true)`+`setHealth(max)`**（43ebc31）：`die()` 第一条就是 `ForgeHooks.onLivingDeath`，返回 true 直接 return；`hurtResistantTime`→`invulnerableTime`；`mainInventory[i]`→`getInventory().items.get(i)`；事件两侧都 fire，自判 `isClientSide`。
42. **「掉的怪不对」先查原版分支与概率**（Fafrotskies）：`pattern<=90` 分支原版就是**普通** `EntityCreeper`；`EntityDanmakuCreeper` 仅 12/25 生成；且原版弹幕苦力怕**没有自定义模型/贴图**，只能看爆炸是否撒弹幕。
43. **村民=`PoiType`+`VillagerProfession`+`VillagerTradesEvent` 三件套，谓词只能用 `ResourceKey`**（ae3eae7，霖之助）：`PoiType` 用 `DeferredRegister` 即可被识别；谓词必须 `holder.is(KEY)`（注册期 `REG.get()` 是 null 会 NPE）；override `setVillagerData` 强制写回职业。**原版 `registerChestItem()` 是空方法**→万宝槌进箱子是有意新增。
44. **`IBehaviorDispenseItem`→`DispenserBlock.registerBehavior`，在 `commonSetup` 注册**（24790d4）：`DefaultDispenseItemBehavior` 子类覆写 `execute`；方向取 `DispenserBlock.FACING`；弹物品 `shootDanmaku(..., new EntityFamiliar(level), false, ...)`；静态表两侧都要跑→`enqueueWork`。
45. ~~渲染器画血条~~ **已作废**（实机崩坏）→ 见铁律 51。
46. **商人型生物=`implements Merchant`+`openTradingScreen`，别自己搭 UI**（d4b18c0）：`setTradingPlayer` 必须先于 `openTradingScreen`；`notifyTrade` 里必须 `offer.increaseUses()`（否则次数永不消耗）；存盘 `tag.put("Offers", ...)`。**原版 `ContainerMerchantSanae`/`GuiMerchantSanae` 从来没有入口**，骨架已删。
47. **「方块本体不画、只有 TESR 画」=空模型+BER**（ebfddac，小神霊）：① barrier 式空模型；② `BlockEntityRenderer` 是 interface（implements 别 extends）；③ 注册用 `registerBlockEntityRenderer`；④ `noCollission()`；⑤ 朝向相机 `mulPose(cameraOrientation())`。**工作台原版就是普通满亮方块，别当漏项修。**
48. **空模型+BER 的方块必须实现 `EntityBlock`**（f18d2a5）：漏 `newBlockEntity`→整块不可见；同轮补 `getDrops`+8 色独立物品（旧 ID `divine_spirit` 代表蓝色）。
49. **配方/战利品表/语言键一律走 JSON**（72db034）：`data/thkaguya/recipes/*.json` = `crafting_shaped`+`pattern`+`key`+`result`；方块战利品表必须 `data/thkaguya/loot_tables/blocks/<注册名>.json`，**缺表=0 掉落**；tooltip 键漏了不报错、只显示原始 key。
50. **掉落链实体是 `extends Entity` 的 MISC 实体，1.20.1 拾取要自己写**（1264ebe）：`EntityShotMaterial`（落体+玩家靠近吸附）→ `EntityTHItem`（`DATA_ITEM_TYPE` 五型）；`defineSynchedData` 同步、NBT `putByte("ItemType")`；没有 `onCollideWithPlayer`，要 `getEntitiesOfClass(Player.class, bbox.inflate(0.3D))` + `playerTouch(player)`，`canBePickedUp` 写 `player.isShiftKeyDown()` 才是「潜行吸取」；拾取 `getInventory().add` 满则 `drop`+`ITEM_PICKUP` 音效。
51. **boss 血条改用原版 `ServerBossEvent`（铁律 45 作废）；发射器打弹幕要 `SIZE+1.0D`；刷怪蛋必须有 model json**（5231247）：① 渲染器里画头顶血条实机崩坏，改为 `EntityDanmakuMob` 里 `ServerBossEvent(PINK, PROGRESS)`+`hasBossBar()` 钩子（8 个 boss 子类 true）+`startSeenByPlayer`/`stopSeenByPlayer`+`tick` 里 `setProgress`/`setVisible`+`remove` 里 `removeAllPlayers()`。② **刷怪蛋必须有 `models/item/<id>.json`（`template_spawn_egg` 父模型）**，缺了就是紫黑块。③ 发射器 `shootDanmaku` 距离必须 `SIZE[...]+1.0D`（原版 `CommonProxy.java:640`），只给 `SIZE` 会在方块内部生成被吃掉。
52. **符卡类物品的变体在 damage 里，新建成就不带 damage 等于 0 号**（62b121f）：符卡用 `ItemStack#setDamageValue(符卡No)` 区分（`getDescriptionId` → `item.thkaguya.spell_card.<damage>`）；`new ItemStack(SPELL_CARD.get(), 1)` 直出必是 0 号梦想封印；`dropCustomDeathLoot` 必须抄原版 `new ItemStack(spell_card, 1, getUsingSpellCardNo())`。排查：所有 `new ItemStack(...SPELL_CARD` 后要么跟 `setDamageValue`，要么就是 bug。
53. **`ISpecialShot` 必须同时登记类与实例；只登记实例 = 分发被跳过**（e6a18d5，幽香伞开花不生效）：`EntityTHShot.blockHitSpecialProcess`/`specialMotion` 先 `SpecialShotRegistry.getSpecialShotClass(id)`，为 null 直接 return（连硬编码 switch 都不走）→ 弹照飞、什么也不发生。Item 实现 ISpecialShot 要两条：`registerSpecialShot(类.class, id)` + `registerSpecialShotInstance(this, id)`（1.20.1 不能 new Item）。判断命中方块用 `BlockHitResult#getBlockPos()`，别用 `shot.blockPosition()`（贴面时仍在空气格）。
54. **自然生成 = 数据文件 + `SpawnPlacements` 两半，且配置门控只能落在 `checkSpawnRules`**（`e895344`）：`data/<ns>/forge/biome_modifier/*.json` 的 `forge:add_spawns` 负责注入刷怪表，`SpawnPlacements.register` 负责「能不能生成」，少任一半都不刷；1.7.10 群系名要映射（`*Hills`→基群系、`hell`→`nether_wastes`、`mesaPlateau_F`→`badlands`），写不存在的 id（如 `badlands_plateau`）会让整个注册表加载失败、服务器起不来。原版包在 `addSpawn` 外的配置 `if`，数据驱动读不到 → 改在 `EntityDanmakuMob#checkSpawnRules` 里按 `MobSpawnType.NATURAL`/`CHUNK_GENERATION` 查 `THKaguyaConfig.allowNaturalSpawn(type)`。
55. **把哑物品改成真物品时的两个 1.20.1 命名坑**（`e895344`）：① 在 `ArmorItem`/`Item` 子类里写 `new Item.Properties()` 会报 `package Item does not exist`（简单名 `Item` 不解析成类型）→ 写全限定 `new net.minecraft.world.item.Item.Properties()`；② `Player#isWet()` 在 1.20.1 不存在 → 用 `Entity#isInWaterRainOrBubble()`（水/雨/气泡柱皆算）。另 `ArmorItem` 的 `onArmorTick` 已被标 `removal`，仍可用但会告警。

56. **自然生成的「好找」三件套：露天地表 + 永久发光 + 专属外观**（dc18b1c）：① `checkSpawnRules` 里 `level.canSeeSky(this.blockPosition())` 可以挡掉洞穴/矿洞刷怪（对 `MobSpawnType.NATURAL`/`CHUNK_GENERATION` 才判）；② 永久发光 = `addEffect(new MobEffectInstance(MobEffects.GLOWING, -1, 0, false, false))`，放在 `finalizeSpawn` 并按 `getType().getCategory() == MobCategory.MONSTER` 过滤，NPC 与刷怪蛋生成不加；③ 同一渲染器注册给多个实体 = 外观串味，见约定 70。

## 版本管理（用户约定）
每批改动 `git add -A && git commit -m "中文 message（标注阶段/批次）"`。`.gitignore` 排除 build/run/日志/*.jar；`thkaguya-1.12.2`/`1.14.4` 独立上游仓库不纳入；`.gitattributes` `* -text`。
