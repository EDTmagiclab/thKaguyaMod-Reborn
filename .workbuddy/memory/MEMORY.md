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
- **固定管线 GL 状态 → 自定义 RenderType（弹幕/激光/魔法阵保真度的关键）**：原版弹幕几乎全部用**加性/发光**混合 + `glDepthMask(false)` + `glDisable(GL_LIGHTING)`，1.20.1 直接用 `entityTranslucent`（标准半透明）观感完全不同。统一走 `client/render/THRenderTypes.java`：
  - `BLEND_ADDITIVE` = `(SRC_ALPHA, ONE)`（最常见）；`BLEND_GLOW` = `(ONE, ONE_MINUS_SRC_COLOR)`（使い魔/風弾/御札/苦無·矢外層/激光外層/魔法陣/結界/遅延光效）；`BLEND_DOUBLE_ADD` = `(ONE, ONE)`（幽霊弾）
  - `depthWrite` 参数 = `glDepthMask`；全亮 = 顶点 `uv2(LightTexture.FULL_BRIGHT)`；`cutout(tex,cull)` = 原版 `glDisable(GL_BLEND)` 的白芯層
  - **关键事实**：`RenderStateShard` 的**静态字段**是 protected，但其**内部 shard 类及其构造函数全部 public**（javap 实测），配 public 的 `RenderType.create(...)` 即可自建 → **不需要 AccessTransformer/Mixin**
- **尺寸/贴图逐 form 核对**：原版顶点多为 `±width`（normal 1.0 / light 2.0 / familiar 1.0），移植时若用固定 `half=0.5` 的 quad 必须补 ×2，否则整体只有原版一半大；RICE/OVAL/SCALE 三类椭球弹原版都贴 `laser.png`。`animationCount < 0`（delayTime）阶段原版**只画光效不画弹体**。
- **面向相机的 billboard 必须用 NoCull 系 RenderType**：手工构造的 camera-facing quad（`mulPose(entityRenderDispatcher.cameraOrientation())` + 左下→右下→右上→左上 的顶点序）在该坐标系下被判为「裏面」，用 `entityTranslucentCull` / `entityCutout` 会被整片剔除 → **完全不可见**（实体存在、伤害正常，但看不到）。必须用 `entityTranslucent` / `entityCutoutNoCull`。
  - 实测踩坑：`RenderTHShot` 的光弾（FORM_LIGHT，导致「波与粒的境界」等 5 类来源全不可见）、気質弾、星弾、幻弾、使い魔、FORM_BIG；此前只修了 KISHITU 一次（commit a3c5366），未举一反三（commit f337d40 补齐）。
  - **原版判据**：1.7.10 原版只在 3D 形状（renderScaleShot / ButterflyShot / KunaiShot / ArrowShot / TalismanShot）里显式 `glDisable(GL_CULL_FACE) 両面描画` / `glEnable(GL_CULL_FACE) 表面描画`；**面向相机的 billboard 完全不碰剔除状态** → 移植时一律用 NoCull。
  - 3D 手工几何（`quadQuad` / `tri3`，如 kunai / arrow 外层、crystal 八面体）原版即单面剔除，保持 Cull 正确。
- **弹幕/激光实体的 yaw 约定与「180 之谜」（已彻查，勿再改错）**：`THShotLib.getYawFromVector(vx, vz) = atan2(vx, vz)`（自定义 `(sinθ, cosθ)` 参数化），因此**弹幕/激光实体存的 yaw 恰为 MC 原版 yaw 的相反数**。渲染必须写 `Axis.YP.rotationDegrees(yaw)`（**不要 180**）+ `Axis.XP.rotationDegrees(-xRot)`，等价于原版 `glRotatef(yaw); glRotatef(-pitch)`，此时局部 +Z 恒指向真实运动方向。
  - **`180 - yaw` 不是「再转 180°」而是「把方向以 X 轴镜像」**：光束方向变成 `(sin yaw, 0, -cos yaw)` → 正东/正西（yaw=±90）恰好正确、正南/正北差 180°（朝身后射）。commit a3c5366 曾据此「修魔炮」，其理由（"GameRenderer 施加 cameraYaw+180"）经 javap 复查 **不成立**（`EntityRenderDispatcher.render` 只有 pushPose + translate），已撤销（commit d7591f0）。用户实测判据：魔炮光柱必须穿过迷你八卦炉模型两枚同心圆环（= 炮口轴）的中心。
  - 与之相对，**道具实体**（MiniHakkero/MarisaBroom/Kinkakuzi/Pendulum/PrivateSquare/SakuyaStopWatch/SilverKnife/Sukima/YuukaParasol）的 yaw 是原版 vanilla 约定，原版源码本身就是 `glRotatef(180F - yaw, ...)` → 那里的 `180 - yaw` 必须保留。判据：**yaw 是 vanilla 约定 → 180-yaw；yaw 来自 atan2(vx,vz) → 裸 yaw**。
  - `Axis.YP.rotationDegrees(θ)` ≡ `glRotatef(θ, 0,1,0)`（JOML 矩阵推导 + RenderMiniHakkero 实证），不需要额外取反。
- **`EntityTHShot.angle` 未同步**：`defineSynchedData` 只同步 ANGLE_Z/ANIMATION_COUNT/SHOT_END_TIME/SHOT_ID/SHOT_SIZE，`angle` 是普通字段 → **客户端上恒为 (0,0,0)**。渲染或任何客户端逻辑都不能用它推方向/速度，只能用 yaw/xRot 或新增同步字段。
- **`ignoreFrustumCheck = true` → `noCulling` 字段（构造里直接赋值）**：渲染尺寸远大于碰撞箱的特效/载具实体必须设，否则整片被视锥剔除、表现为「完全看不到」。原版设了该项的清单：SpellCardCircle / SakuyaStopWatch / SakuyaWatch / SpiritualStrikeTalisman / YuukaParasol / Rumia / EntityTHLaser。实测踩坑：**EntitySpellCardCircle 漏抄**（bbox 0.5×0.5 vs 阵宽 7 格，阵在脚下 → 视锥外 → 魔法阵完全不可见，commit d7591f0 修复）。
- **覆写 `rideTick()` 必须自己驱动 `tick()`**：1.20.1 中乘客实体**不走** `tick()` —— `ServerLevel/ClientLevel.tickPassenger()` 调 `rideTick()`，`Entity.rideTick()` 内部才负责 `setDeltaMovement(ZERO)` + `tick()` + `positionRider()`。覆写后不调 `super.rideTick()` 或 `this.tick()` → 该实体 `tick()` 永不执行 → 消灭判定/计时器全失效。踩坑记录：`EntityHisou` 主剑不消失 + 使用者永久减速、`EntityYuukaParasol` 同症（commit e3a08eb）。注意 `super.rideTick()` 会附带 `positionRider()`，若 `tick()` 内已用 `itemEffectFollowUser` 自行定位，应只调 `if(this.canUpdate()) this.tick();`
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
