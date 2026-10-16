# 项目长期记忆：五つの難題MOD+ 1.7.10 → 1.20.1 移植

## 定位与路径
东方同人 mod「五つの難題MOD+」移植到 1.20.1 Forge。新工程 `thkaguya-1.20.1/`（modid `thkaguya`，包名 `thKaguyaMod`）。原版源码 `extract/sources/java/thKaguyaMod/`（266 文件/53341 行）。

- 新工程：`thkaguya-1.20.1/` → 产物 `build/libs/thkaguya-1.0.0.jar`
- 工具脚本：`tools/`（gen_items/gen_recipes/gen_lang5/gen_textures/validate/gen_models/conv_spellcards 等）
- 游戏 mod 目录：`F:\mc\versions\newserver\.minecraft\versions\1.20.1-Forge_47.4.28\mods`
- **每次 build 成功后必须把 jar 覆盖复制到上述 mod 目录**（核验 md5/mtime，勿用 `build|tail&&cp` 链——tail 恒 0 会掩盖失败复制旧产物）

## 构建环境
- Forge 1.20.1 **47.4.28** / ForgeGradle 6；JDK 17 `C:/Program Files/Java/jdk-17`
- Gradle 8.5 `/e/git/ad-coze/arcadian-dream-fix-1.20/gradle-9.2.1/gradle-8.5/bin/gradle`
- 代理 `export http_proxy="http://127.0.0.1:16888" https_proxy="..."`（6232 端口坏）；MCPRepo 偶发 Connection reset → 用 `gradle build --offline`

## 铁律（务必遵守）

### 1. 资源标识符全小写
ResourceLocation 路径只允许 `[a-z0-9/._-]`，**文件名+目录名都要小写**。`static final ResourceLocation` 类加载即校验，含大写→资源重载崩溃（启动不了）。Windows 批量重命名两步（先 `*.tmp`）；git 大小写用 `git -c core.ignorecase=false`。

### 1b. 贴图目录必须是 `textures/item/` 与 `textures/block/`（单数）
1.7.10 的 `textures/items/`、`textures/blocks/`（复数）**必须**改名为 `item/`、`block/`。
1.20.1 物品/方块模型走 **blocks 图集**，图集目录源只收录 `textures/block/` 与 `textures/item/`；
放在 `items/`（复数）里 **实体渲染正常**（`getTextureLocation` 独立加载 PNG，不走图集）**但物品模型缺材质**。
这一「实体好、物品坏」的分裂现象就是该坑的指纹。
**排查方法**：看客户端 `logs/latest.log` 的 `Missing textures in model X#inventory:`（会列出 `atlas: sprite` 全限定对）。
实测案例：符卡 35 张卡面 + 5 边框因放在 `textures/items/spellcard/` 而全部缺材质（commit d983d32 修复）。

### 2. 实体三大注册缺一不可
EntityType + EntityAttributeCreationEvent + EntityRenderer，缺一生成即崩。runServer 验不了渲染缺失。

### 3. EntityType 必须沿构造链显式传递
子类完整参数构造若落到父类硬编码某 type 的 `Xxx(Level)` 构造，`getType()` 全错——服务端是正确子类、客户端按错 type 实例化父类（视觉/行为错乱，激光渲染成圆弹）。每个子类构造显式传自己 EntityType。

### 4. API 迁移高频坑
- **覆写 `rideTick()` 必须自己驱动 `tick()`**：1.20.1 中乘客实体**不走** `tick()` —— `ServerLevel/ClientLevel.tickPassenger()` 调 `rideTick()`，`Entity.rideTick()` 内部才负责 `setDeltaMovement(ZERO)` + `tick()` + `positionRider()`。覆写后不调 `super.rideTick()` 或 `this.tick()` → 该实体 `tick()` 永不执行 → 消灭判定/计时器全失效。踩坑记录：`EntityHisou` 主剑不消失 + 使用者永久减速、`EntityYuukaParasol` 同症（commit 待记）。注意 `super.rideTick()` 会附带 `positionRider()`，若 `tick()` 内已用 `itemEffectFollowUser` 自行定位，应只调 `if(this.canUpdate()) this.tick();`
- `ticksExisted`→`tickCount`（递增时机不同，`tickCount<=X` 时停判断会死锁）
- `canBeCollidedWith()`→投射物命中用 `isPickable()`
- `ignoreFrustumCheck=true`→构造里 `this.noCulling=true` 字段赋值（自定义 public 字段不同步，客户端逻辑只能用 entityData 同步字段）
- 调试日志用 `LogUtils.getLogger()`+slf4j；`System.out` 不落盘
- 伤害 `causeIndirectMagicDamage`→`level().damageSources().indirectMagic`
- 配置 `Configuration`→`ForgeConfigSpec`（静态字段须显式 `load()`，监听 ModConfigEvent）
- 渲染：`bindTexture`→`RenderSystem.setShaderTexture`；GL11 手绘→PoseStack+VertexConsumer（顶点六要素 POSITION/COLOR/UV/OVERLAY/UV2/NORMAL 缺一崩）
- 迁移渲染方法时**dispatch 处 GL 变换（rotate/scale/双 pass）与几何本体同等重要**

### 5. 版本管理（用户约定）
每完成一批改动就 `git add -A && git commit -m "中文 message（标注阶段/批次）"`。`.gitignore` 排除 build/run/日志/*.jar；`thkaguya-1.12.2`/`1.14.4` 是独立历史上游仓库不纳入；`.gitattributes` `* -text`。

## 进度快照（2026-10-16 实测复核后更新）
- ✅ 阶段 0/1/2：工程骨架 + 注册（物品注册 106 / 方块 3 / TE 1 / 配方 77 / 语言 5 / 物品模型 109 / 贴图 199）+ 资源
- ✅ 阶段 3 实体层全完成（M5 达标）：3a/3b/3c 最小链路 + 3d-1 激光 + 3d-2 符卡框架 + 3d-3 shot 10 + 3d-4 item 实体13+物品类13 + 3d-5 符卡36 + 3d-6 living 12 + 3d-7 effect 5（EntityType 44 / 实体类文件 45）
- ✅ 阶段 4 渲染：4-1 弹幕 form / 4-2 激光 / 4-3 妖精形态 / 4-4 魔法阵 / 4-5 符卡 / 4-6 effect + item 实体 3D 模型（7 模型 + 10 渲染器）/ 4-7 boss（渲染器 29，`EntityRenderersEvent` 注册）
- ✅ 阶段 5 GUI：4 套（弹幕/激光工作台 + 奇迹小槌 + 早苗商店）MenuType + AbstractContainerMenu + Screen + Slot + 方块 openScreen。早苗商店为简化骨架（原版 IMerchant 半成品，交易留阶段 7）
- 🔶 阶段 6 客户端泄漏：**代码就绪**（非 client 包 `import net.minecraft.client.*` 已清零），**剩专用服务器实测验收**
- 🔶 阶段 7 配置/事件/杂项：**配置已完成**（THKaguyaConfig 已迁 `ForgeConfigSpec`，30+ 开关生效）；剩 `LivingDeathEvent` 死亡掉落、村民霖之助交易（EntityRinnosuke/VillagerRinnosuke）、箱子战利品 loot table、音效 4 事件核对、触发物品类 ItemAjaRedStone/Hakurouken/OharaibouS

**M1–M6 里程碑全部达成**；M7（全功能交付）剩余项明细见任务书 `移植任务书.md` §0「剩余工作明细」。

## 结构决策
- 弹幕工作台拆 2 方块 `danmaku_crafting_table`+`laser_crafting_table`；物品 snake_case；lang 用 JSON；32 符卡配方暂缓（依赖符卡系统）
- 延后项：EntityRinnosuke/VillagerRinnosuke（阶段7 交易）、掉落 EntityTHItem、ItemAjaRedStone/Hakurouken/OharaibouS 触发物品、Fafurotskies 换回 EntityDanmakuCreeper、KappaPororoca 反弹、SHOT_MATERIAL 动态贴图
