# 项目长期记忆：五つの難題MOD+ 1.7.10 → 1.20.1 移植

## 项目定位
把东方同人 mod「五つの難題MOD+」(1.7.10 Forge) 移植到 1.20.1 Forge。新工程在 `thkaguya-1.20.1/`，modid 统一为 `thkaguya`，包名 `thKaguyaMod`。原版源码在 `extract/sources/java/thKaguyaMod/`（266 文件 / 53341 行）。

## 关键路径
- 原版源码：`extract/sources/java/thKaguyaMod/`
- 新工程：`thkaguya-1.20.1/`（build 产物 `build/libs/thkaguya-1.0.0.jar`）
- 工具脚本：`tools/`（gen_items.py / gen_recipes.py / gen_lang5.py / gen_textures.py / validate.py / append_shotgen.py）
- 用户游戏目录：`F:\mc\versions\newserver\.minecraft\versions\1.20.1-Forge_47.4.28\mods`（PCL 启动器，非默认 .minecraft）

## 构建环境
- Forge 1.20.1 **47.4.28**（已对齐用户版本），ForgeGradle 6
- JDK 17 编译：`C:/Program Files/Java/jdk-17`（用户运行时是 Java 20，17 字节码可兼容）
- Gradle 8.5：`/e/git/ad-coze/arcadian-dream-fix-1.20/gradle-9.2.1/gradle-8.5/bin/gradle`
- 代理：`export http_proxy="http://127.0.0.1:16888" https_proxy="http://127.0.0.1:16888"`（默认 6232 端口是坏的）

## 铁律（踩过的坑，务必遵守）

### 1. 资源标识符必须全小写（最大系统性坑）
- **Minecraft 1.13+ 的 ResourceLocation 路径只允许 `[a-z0-9/._-]`**，大写字母直接抛 `ResourceLocationException`。
- 贴图文件名本身也要小写（资源加载大小写敏感）。原版 1.7.10 大量 camelCase 贴图名（`Shot.png`、`FairyTexture_0.png`），迁移时必须重命名为全小写。
- `static final ResourceLocation` 在类加载时（`<clinit>`）就校验，含非法字符会在**资源重载阶段崩溃 = 游戏启动不了**（不是刷实体才崩）。
- Windows NTFS 大小写不敏感，批量重命名需两步（先 `*.tmp` 再目标名），否则 rename 静默失败。

### 2. 1.20.1 实体三大注册缺一不可
EntityType + EntityAttributeCreationEvent + EntityRenderer，缺任意一个实体生成即崩。**runServer 验证不了渲染缺失**（服务器不渲染实体），客户端才会暴露。

### 3. API 迁移速查（1.7 → 1.20）
- 伤害：`DamageSource.causeIndirectMagicDamage(a,b)` → `level().damageSources().indirectMagic(a,b)`（静态工厂全移到 DamageSources）
- **`canBeCollidedWith()` 是语义陷阱**：1.7.10 它表示「可被投射物命中」，1.20.1 它默认返回 false（连玩家也是 false）。投射物命中检测必须用 `isPickable()`（`LivingEntity.isPickable()` = `!isRemoved()`）。
- 调试日志：MC 1.20.1 必须用 `com.mojang.logging.LogUtils.getLogger()` + `org.slf4j.Logger`，`System.out.println` 不落盘 latest.log。
- `ticksExisted` → `tickCount`，**但递增时机不同**：1.7.10 由 World 框架在 `onUpdate()` 之前递增，1.20.1 在 `super.tick()`→`baseTick()` 内部递增。任何 `tickCount <= X` 做"时停/冻结"判断的代码直接搬会第一次 tick 就死锁（实体永远卡住）。
- `getSpeed()` 与 LivingEntity 冲突，须改名
- `AABB.clip()` 返回 `Optional<Vec3>` 非 HitResult
- `playSoundAtEntity` → `level().playSound(null, pos, SoundEvent, SoundSource, vol, pitch)`
- metadata(damage值)变体机制已废除 → 每个变体拆成独立 Item（物品从 64 膨胀到 91）
- `registerIcon`/`setUnlocalizedName` → DeferredRegister + JSON 模型
- 配置：`Configuration` → `ForgeConfigSpec`；**静态字段不会自动同步，必须显式 `load()`**，监听 `ModConfigEvent.Loading`/`Reloading`（包 `net.minecraftforge.fml.event.config.ModConfigEvent`）。
- `getAttackTarget()` → `getTarget()`，重写时调用父类用 `super.getTarget()`（写 `this.getTarget()` 会无限递归）。

### 3.5 版本管理（用户约定，务必遵守）
- 项目根目录已 `git init`，**每完成一个版本/一个批次改动就提交一次本地 commit**（用户明确要求）。
- 提交前 `git add -A && git commit -m "..."`；commit message 用中文，标注阶段/批次（如 `feat(3d-4): 迁移 item 实体 xxx`）。
- `.gitignore` 已排除 build/run/日志/*.jar/*.class/IDE 产物；`thkaguya-1.12.2`、`thkaguya-1.14.4` 是带独立 git 历史的上游参考仓库，**不纳入本仓库追踪**（已在 .gitignore 排除）。
- `.gitattributes` 设 `* -text`，禁止 git 改写行尾，避免 CRLF diff 噪音。

### 4. 结构决策（已定）
- 弹幕工作台拆成 2 方块：`danmaku_crafting_table` + `laser_crafting_table`
- 物品注册名统一 snake_case（91 个：55 独立 + 36 变体）
- 32 条符卡配方暂缓（依赖阶段 3d 符卡系统），已迁移 77 条非符卡配方
- lang 必须是 JSON 格式 `{"key":"value"}`（不是旧版 `key=value`）

## 进度
- ✅ 阶段 0/1/2：工程骨架 + 注册层 + 资源层（91 物品/3 方块/1 TE/77 配方/5 语言/94 模型）
- ✅ 阶段 3a/3b/3c：实体层（EntityDanmakuMob/EntityTHFairy/EntityTHShot + 弹幕生成方法 + 刷怪蛋 + 自然生成 + 属性重构）
- ✅ 阶段 4 止血：ModelTHFairy/RenderTHFairy/RenderTHShot/ClientRenderRegistry（渲染器已注册，修复了两次崩溃）
- ✅ 阶段 3d-1：激光弹体 EntityTHLaser + 激光生成方法（6 个）+ RenderTHLaser + 妖精激光分支
- ✅ 阶段 3d-2：符卡框架核心（EntitySpellCardCircle 魔法阵 + THSpellCard 基类 + ITHSpellCard + SpellCardRegistry + EntitySpellCard + THKaguyaLib 裁剪版）
- ✅ 阶段 3d-3：shot 剩余弹体 10 个（EntityDragonNeckJewel/HomingAmulet/MasterSpark/MusouFuuin/NuclearShot/Onmyoudama/SanaeWind/THHenyoriLaser/THSetLaser/ISpecialShot）
- ✅ 阶段 3d-4：item 实体 13 个全部迁移（compileJava 通过，占位渲染 RenderEmptyItem）
- ⏳ 阶段 3d-5：36 符卡类（THSC_*，3895 行）
- ⏳ 阶段 3d-6/3d-7：living 15 个 + effect 5 个
- ⏳ 阶段 4 完整：弹体按形状分贴图、妖精 11 种形态切换、激光/魔法阵/符卡完整渲染、各实体模型
- ⏳ 阶段 5/6/7：GUI、客户端泄漏修复、配置/事件/村民交易
