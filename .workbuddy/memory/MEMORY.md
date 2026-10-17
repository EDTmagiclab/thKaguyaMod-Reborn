# 项目长期记忆：五つの難題MOD+ 1.7.10 → 1.20.1 移植

## 定位与路径
东方同人 mod 移植到 1.20.1 Forge。新工程 `thkaguya-1.20.1/`（modid `thkaguya`，包名 `thKaguyaMod`）→ 产物 `build/libs/thkaguya-1.0.0.jar`。原版唯一权威基线 `extract/sources/java/thKaguyaMod/`（266 文件）。工具脚本 `tools/`。游戏 mod 目录 `F:\mc\versions\newserver\.minecraft\versions\1.20.1-Forge_47.4.28\mods`。
**每次 build 成功后必须把 jar 覆盖复制到 mods 目录**（核验 md5/mtime；勿用 `build|tail&&cp` 链，tail 恒 0 会掩盖失败复制旧产物）。

## 构建环境
Forge 1.20.1 47.4.28 / ForgeGradle 6；JDK 17 `C:/Program Files/Java/jdk-17`；Gradle 8.5 `/e/git/ad-coze/arcadian-dream-fix-1.20/gradle-9.2.1/gradle-8.5/bin/gradle`；代理 `127.0.0.1:16888`（6232 端口坏）；MCPRepo 抽风用 `--offline`。

## 铁律（完整 28 条见 `移植任务书.md`「关键迁移约定」，此处只列最贵重的坑）
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
20. `ticksExisted`→`tickCount`（时机不同，`<=X` 时停判断死锁）；`releaseUsing` 的 `timeLeft` 是剩余 tick，按住时长=`getUseDuration-timeLeft`；`setSize`/`isInRangeToRenderDist` 不存在（碰撞箱由 EntityType.Builder.sized 定；渲染距离覆写 `shouldRenderAtSqrDistance`）；`Properties.durability(N)` 优先于 `Tier.getUses()`。
21. 诊断代码（启动自检/全链路日志）验完删除，javap 核对 jar 无残留字符串。

## 版本管理（用户约定）
每批改动 `git add -A && git commit -m "中文 message（标注阶段/批次）"`。`.gitignore` 排除 build/run/日志/*.jar；`thkaguya-1.12.2`/`1.14.4` 独立上游仓库不纳入；`.gitattributes` `* -text`。

## 进度快照（2026-10-17；HEAD `395a03b`，jar md5 `63AB5738C77137EBFCCB2D1AB2849865`）
- ✅ 阶段 0/1/2（注册 107 物品/3 方块/配方 77/语言 5/模型 109/贴图 199）；阶段 3 实体层全完成（EntityType 44）；阶段 4 渲染全完成（含 4-8 保真度对齐 ff82ac8/f337d40/d7591f0/c85ea86）；阶段 5 GUI 4 套；阶段 6 已结清（服务器+联机实测通过）
- 🔶 阶段 7：配置✅、触发物品✅；剩死亡事件/村民霖之助/发射器+loot/音效核对
- ✅ 哑物品批次 1 22/22（f580315+395a03b）；剩余真哑 31 项（批次 2 的 24 弹/激光素材 + 批次 3 的 7 重活），见 `哑物品清理任务书.md`
- 符卡 35 张已注册（ID 0-30/33/34/35/40；BrilliantDragonBullet 原版即注释，勿"补"）；**逐卡弹幕形状与原版的比对尚未做**（DoD 第 4 条）
