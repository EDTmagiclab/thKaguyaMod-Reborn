# 项目长期记忆：五つの難題MOD+ 1.7.10 → 1.20.1 移植

## 定位与路径
东方同人 mod 移植到 1.20.1 Forge。新工程 `thkaguya-1.20.1/`（modid `thkaguya`，包名 `thKaguyaMod`）→ 产物 `build/libs/thkaguya-1.0.0.jar`。原版唯一权威基线 `extract/sources/java/thKaguyaMod/`（266 文件）。工具脚本 `tools/`。游戏 mod 目录 `F:\mc\versions\newserver\.minecraft\versions\1.20.1-Forge_47.4.28\mods`。
**每次 build 成功后必须把 jar 覆盖复制到 mods 目录**（核验 md5/mtime；勿用 `build|tail&&cp` 链，tail 恒 0 会掩盖失败复制旧产物）。

## 构建环境
Forge 1.20.1 47.4.28 / ForgeGradle 6；JDK 17 `C:/Program Files/Java/jdk-17`；Gradle 8.5 `/e/git/ad-coze/arcadian-dream-fix-1.20/gradle-9.2.1/gradle-8.5/bin/gradle`；代理 `127.0.0.1:16888`（6232 端口坏）；MCPRepo 抽风用 `--offline`。

## 铁律（完整 41 条见 `移植任务书.md`「关键迁移约定」，此处只列最贵重的坑）
1. **资源标识符全小写**（文件名+目录名）；`static final ResourceLocation` 类加载即校验，大写→启动崩溃。
2. **贴图目录必须 `textures/item/` 与 `textures/block/`（单数）**。复数目录→实体渲染正常但物品模型缺材质（指纹：latest.log 的 `Missing textures in model`）。符卡 35 张卡面曾因此全缺材质（d983d32 修）。
3. **实体三大注册缺一不可**：EntityType + EntityAttributeCreationEvent + EntityRenderer。
4. **EntityType 必须沿构造链显式传递**，否则 getType() 全错（服务端子类/客户端父类）。
5. **yaw 两套约定（勿再改错）**：弹幕/激光 yaw 来自 `atan2(vx,vz)`（= MC yaw 相反数）→ 渲染写 `Axis.YP.rotationDegrees(yaw)` **绝不加 180**；道具实体 yaw 是 vanilla 约定 → 保留 `180 - yaw`。判据：yaw 来自 atan2 → 裸 yaw；vanilla → 180-yaw。魔炮判据：光柱须穿过迷你八卦炉两枚同心圆环中心。
6. **面向相机的 billboard 必须用 NoCull 系 RenderType**（顶点序左下→右下→右上→左上在该坐标系被判裏面，cull=整片剔除→完全不可见）。3D 手工几何（kunai/arrow/crystal）原版即单面剔除，保持 Cull。
7. **弹幕混合模式**走 `client/render/THRenderTypes.java`：加性=`(SRC_ALPHA,ONE)`；発光=`(ONE,ONE_MINUS_SRC_COLOR)`；幽霊弾=`(ONE,ONE)`；`depthWrite`=glDepthMask；全亮=uv2(FULL_BRIGHT)；白芯层=cutout。`RenderStateShard` 静态字段 protected 但内部 shard 类与构造全 public → 自建 RenderType 无需 AT/Mixin。
8. **激光/弹幕命中搜索范围按到达距离伸展**：`getHitCheckReach()` 默认 1.0，激光覆写 `getLaserLength()+getShotSize()`（否则激光零伤害，c85ea86）。
9. **SetLaser 蓄力期光束 1/10 粗细**（anim<0 && instanceof EntityTHSetLaser）。
10. **`ignoreFrustumCheck=true` → 构造里 `this.noCulling=true`**。原版清单：SpellCardCircle/SakuyaStopWatch/SakuyaWatch/SpiritualStrikeTalisman/YuukaParasol/Rumia/EntityTHLaser。
11. **覆写 `rideTick()` 必须自己驱动 `tick()`**（`if(this.canUpdate()) this.tick();`），否则实体永不 tick（e3a08eb）。
12. **`getLightEmission`≠`getBlockLightValue`**：后者对应 `level.getBrightness(LightLayer.BLOCK,pos)`（赤石蓄光曾恒 0）。
13. **追踪弹改 angle 不够**：须按当前速率重定向速度并同步 `lastShotMotionX/Y/Z`（tick() 每 tick 先恢复 lastShotMotion）。
14. **`EntityTHShot.angle` 未同步**，客户端恒 (0,0,0)，渲染/客户端逻辑不能用它。
15. **1.20.1 射线选择看 `isPickable()`**（原版 canBeCollidedWith）。
16. **禁止运行期 new Item/Block 实例**（intrusive holder 启动失败）→ `SpecialShotRegistry` 存实例（1e9815d）。同理 `RegistryObject#get()` 不能在 commonSetup 调。
17. **反射一律 `catch (Throwable)`+打日志**，禁空 catch（最贵的陷阱）。
18. **流体**：放置后 `((ServerLevel)level).getFluidTicks().clearArea(...)` 撤销预约才静止；移除用 `setBlock(pos, AIR, Block.UPDATE_ALL_IMMEDIATE)`（removeBlock 对流体是空操作）。
19. **禁大范围扫描清测试残留**（曾误删 141309 天然水域方块）；只清自己记录的坐标。
20. `ticksExisted`→`tickCount`（时机不同，`<=X` 时停判断死锁）；`releaseUsing` 的 `timeLeft` 是剩余 tick，按住时长=`getUseDuration-timeLeft`；`setSize`/`isInRangeToRenderDist` 不存在（碰撞箱由 EntityType.Builder.sized 定；渲染距离覆写 `shouldRenderAtSqrDistance`）；`Properties.durability(N)` 优先于 `Tier.getUses()`。**1.20.1 的 `Item.getUseDuration` 默认返回 0**——凡 1.7.10 用 `setItemInUse` 的道具都必须覆写它，否则 `startUsingItem` 立即结束、蓄力/突进/射出全废（`1bdcc8b`）；原版 `player.inventory.getCurrentItem()` 要写 `getMainHandItem()`/`getOffhandItem()`，**不是** `getUseItem()`（松手瞬间即空 → 误判换手 → 弹体 `discard()`）。
21. 诊断代码（启动自检/全链路日志）验完删除，javap 核对 jar 无残留字符串。
22. **原版子类覆写的方法要逐个核对移植版是否也覆写**（`0ec5093`）：`EntityTHLaser` 在原版覆写了 `updateAngle()`（用 `angle` 算朝向、服务端专用），移植版漏掉 → 继承基类的「用 `deltaMovement` 算」版本 → 固定激光 `EntityTHSetLaser` 初速恒 0 → yaw/pitch 每 tick 归零 → 恋符「Non-Directional Laser」5 条激光全部同向（用户实测「形成一个圆筒」）。自检法：拿原版实体/道具类逐个 grep `@Override` 与 `void tick`，确认移植版有同名覆写。
23. **玩家背包的 `Item.inventoryTick` 在移动之前调用**（`7d4bb6c`）：javap 实测 `Player.tick()` 中 `Inventory.tick()` 在 offset 113、`LivingEntity.tick()` 在 140，所以 `getDeltaMovement()` 拿的是上一 tick 末尾乘过摩擦的值（地上 6.0×0.546≒3.28），`getBoundingBox()` 也是移动前的位置。1.7.10 `onUpdate` 却在移动之后 → 照抄「速度门限 + 当前位置朝前伸判定盒」的写法会漏判整段位移（楼观剑突进斩击全空）。修法：用 `getX()-xo`（前 tick 实际位移）兜底门限，判定盒 `move(-d).expandTowards(2d).expandTowards(motion).inflate(1.0)`。
24. **1.7.10 的反射混合不看顶点 alpha**（`7d4bb6c`）：`GL_ONE, GL_ONE_MINUS_SRC_COLOR` 只取 RGB，发光层若画在不透明本体之后，会把本体盖成一块近乎不透明的色盘（御币弹阴陽玉被红色 halo 盖死 → 看着像「巨大橙色子弹」）。1.20.1 的 `entity_translucent` 着色器把 rgb/alpha 分开输出，改「光效在前、本体在后」即可。另：临时诊断日志验完必须删干净（本次清了 5 处，自检 `grep -rn "一時診断|\[TH\]|\[thkaguya\]" src/main/java`）。
25. **子类的便宜构造必须自己传 EntityType**（`7c1de2b`）：`EntityTHShot(Level, LivingEntity, ...)` 内部写死 `this(THKaguyaEntities.TH_SHOT.get(), ...)`（`EntityTHShot.java:163-173`）。`EntityOnmyoudama` 复用它 → 服务端实体类型是 `TH_SHOT` → 刷怪包让客户端生成**基类 `EntityTHShot`** → `ClientRenderRegistry.java:181` 给 `ONMYOUDAMA` 注册的 `RenderOnmyoudama` 成了死代码，画面一直走通用 `RenderTHShot`（「巨大的橙色子弹」）。修法：子类加 `EntityType<?>` 首参构造转发基类 `EntityTHShot(EntityType<?>, Level, ...)`，便宜构造 `this(THKaguyaEntities.<自己>.get(), ...)`。自检 `javap -p -c <jar> <实体类> | grep TH_SHOT`。**「换了实现症状一字未变」= 那段代码根本没执行**，先查对象是哪个类/哪个 EntityType，别再改渲染细节。

26. **1.20.1 `Level#clip` 永不返回 null**：未命中时返回 `HitResult.Type.MISS` 的 `BlockHitResult`。凡从 1.7.10 `rayTraceBlocks` 移植来的 `if(mop != null)` 判空都必须改成 `getType() == HitResult.Type.BLOCK`，否则「从不 miss」——本工程已中招 3 处（御币弹满蓄力冲天、`clipBlocks` 落地即消、隙间卡永不生成）。另：构建产物 jar 的 md5 每次构建都不同（时间戳），版本标识只认 git HEAD。

27. **改资源文件（lang/model/清单）先看 `git diff --stat`，整文件增删=行尾被改写**（`b7234f7`）：脚本往 `zh_cn.json` 插 3 行时 `newline="\n"` 把 CRLF 全转 LF，diff 从 3 行变 378 行。读写一律 `newline=""`（或用 `edit` 定点替换），发现整文件重写就 `git checkout --` 重来。另：`item.thkaguya.*` 的 lang 缺 `thKaguya.color.*`/`thKaguya.danmakuForm.*`/`Infinity`，用原版 `StatCollector` 写法的 tooltip 会显示原始 key。

28. **资源文件行尾/编码要按字节保真**（`e74b14b`）：`io.open(path)` 的通用换行会把 CRLF 读成 LF，`'\r\n' in text` 判成 False → 把 CRLF 的 5 个 lang 文件全写成 LF（diff 从 5 行变 425 行，只能 `git checkout --` 重来）。本工程 **lang 是 CRLF、model 是 LF**，两条路都不许「统一成一种」：读写都走 `open(p,'rb')`/`open(p,'wb')`，按字节判 EOL 再按原样写回。另：en_us/zh_cn 曾带 UTF-8 BOM（游戏能容忍，但 PowerShell `ConvertFrom-Json` 与 diff 都会脏），统一无 BOM。

29. **1.20.1 `item/generated` 的 tintIndex 就是层号**（`e74b14b`）：`layer0`→tintIndex 0、`layer1`→1。原版 `getColorFromItemStack(stack, pass)` 的 pass 0（形状染色）/pass 1（白）正好对应「ItemColor 只给 tintIndex 0 上色、其余返回 -1」。模型加 `layer1` 的同时必须把 `<name>_2.png` 放进 `textures/item/`，否则图标变紫黑格（原版叠加层在 `extract/assets/thkaguyamod/textures/items/shot/<Camel>_2.png`，注意同目录还有 `BigShot__org.png` 等备份文件不要拷）。

## 版本管理（用户约定）
每批改动 `git add -A && git commit -m "中文 message（标注阶段/批次）"`。`.gitignore` 排除 build/run/日志/*.jar；`thkaguya-1.12.2`/`1.14.4` 独立上游仓库不纳入；`.gitattributes` `* -text`。

## 进度快照（2026-10-17；HEAD `08753c4`；jar md5 每次构建都变，本次 `789CBF6F0AE3D4B5BB75AD6B4A4F93E6`，版本标识只认 HEAD）
- ✅ 阶段 0/1/2（注册 107 物品/3 方块/配方 77/语言 5/模型 109/贴图 199）；阶段 3 实体层全完成（EntityType 44）；阶段 4 渲染全完成（含 4-8 保真度对齐 ff82ac8/f337d40/d7591f0/c85ea86）；阶段 5 GUI 4 套；阶段 6 已结清（服务器+联机实测通过）
- 🔶 阶段 7：配置✅、触发物品✅；剩死亡事件/村民霖之助/发射器+loot/音效核对
- ✅ 哑物品批次 1 22/22（f580315+395a03b）；用户实测后修复 3 处（`46d32cb`：楼观剑 `getUseDuration`、御币弹手持判定 + 专用 `RenderOnmyoudama`）；第二轮实测后修复 2 处（`7d4bb6c`：楼观剑突进斩击的位移判定、御币弹光效绘制顺序 + 补回 `bound()` 反弹，顺带修好 ScarletShoot 等 BOUND01/02/04 弹体）；第三轮实测后修掉御币弹的真正根因（`7c1de2b`：EntityType 串到 `TH_SHOT` 导致专用渲染器失效 + `userHit` 未武装导致无伤害 + 地面反射缺失，见铁律 25）；剩余真哑 31 项（批次 2 的 24 弹/激光素材 + 批次 3 的 7 重活），见 `哑物品清理任务书.md`
- ✅ 哑物品批次 2 完成 24 项中 23 项（`b7234f7`）：19 种弹（`ItemTHShot`，六种弹幕形状 + NBT 参数 + 创造模式 Infinity）、3 种激光（`ItemTHLaser`，长度 5/2/8）、弹幕之素（`ItemShotMaterial` 右键试射）；`laser_material` 原版本就无右键行为。弹按 NBT Color 着色（`ClientRenderRegistry.registerItemColors`）
- ✅ 小打磨（`e74b14b` 资源+语言、`08753c4` 删诊断）：19 张原版 `_2` 高光层 + 19 个模型加 `layer1`（layer0 染色/layer1 不着色）；五语言统一 **211 键**（ja_jp/ko_kr/zh_tw 从原版 `.lang` 取译者原文，含 `danmakuCrafting.*`/`laserCrafting.name`/`thKaguya.color.0-9`/`thKaguya.danmakuForm.0-6`/`entity.thkaguya.*`+刷怪蛋/`miracleMallet.*`/`thKaguya.gravityFree`/`laser_material`/`third_eye`/`Infinity`），剥 BOM；`ItemTHShot` tooltip 的 GFree/Infinity 改为可翻译组件；`EntityOnmyoudama` 的 `DBG_ON`/`[TH-dbg]` 诊断全部删除。实测清单：仓库根 `测试说明-批次2与小打磨.md`。**待用户实测**
- 符卡 35 张已注册（ID 0-30/33/34/35/40；BrilliantDragonBullet 原版即注释，勿"补"）；逐卡弹幕形状比对**已做**（`符卡弹幕形状核验报告.md`：36 对 `THSC_*` 中 34/36 一致，2 张卡片偏差 + 4 项框架级发现，均待用户确认后再修）
