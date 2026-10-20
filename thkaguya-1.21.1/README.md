# thkaguya-1.21.1（NeoForge 21.1 移植工程）

THKaguya（五つの難題MOD+ 重制版）的 **Minecraft 1.21.1 / NeoForge 21.1** 移植目标工程。
与现役的 `thkaguya-1.20.1/`（Forge 47.4.28）并行存在，两者互不影响。

> 规划文档：[../1.21.1移植项目规划书.md](../1.21.1移植项目规划书.md)（阶段 M0–M8、迁移面 M-01~M-14、验收 T1–T7 / V1–V6）

## 当前状态：M0 之前的"环境骨架"

本工程目前只有 **冒烟骨架**，用于证明工具链可用，不含游戏内容：

| 文件 | 作用 |
|---|---|
| `build.gradle` / `settings.gradle` / `gradle.properties` | ModDevGradle 2.0.144 + NeoForge 21.1.249 + Java 21 |
| `src/main/java/thKaguyaMod/THKaguyaCore.java` | 最小 `@Mod`，构造器注入 `IEventBus` + `ModContainer` |
| `src/main/java/thKaguyaMod/init/THKaguyaItems.java` | 一个 `smoke_item`，验证 `DeferredRegister.Items` / `DeferredItem` |
| `src/main/resources/META-INF/neoforge.mods.toml` | 1.21.1 必须显式 `modLoader` + `loaderVersion` |
| `src/main/resources/META-INF/accesstransformer.cfg` | `Creeper#explodeCreeper()`，已去掉 1.20.1 的 SRG 名 |
| `src/main/resources/pack.mcmeta` | `pack_format` 34，`supported_formats` [34, 48] |

正式内容按规划书 M1/M2 分批迁入，迁一批提交一批。

## 环境（已实测可用）

| 项 | 值 | 备注 |
|---|---|---|
| JDK | `C:\Java\jdk-21`（Temurin 21.0.12.1+1 LTS） | 指向 `C:\Java\jdk-21.0.12.1+1` 的目录联接，**不改 PATH / JAVA_HOME** |
| Gradle | `C:\Gradle\gradle-8.14.3` | 规划书里写的 9.2.1 未采用，见规划书附录 E |
| 构建插件 | `net.neoforged.moddev` 2.0.144 | Gradle 8.8+ 兼容 |
| NeoForge | 21.1.249 | Minecraft 1.21.1 |

**代理**：Gradle 的代理写在 `gradle.properties` 的 `org.gradle.jvmargs` 里（`-Dhttp(s).proxyHost=127.0.0.1 -D...proxyPort=16888`）。
注意 `systemProp.*` 形式实测**不会**传给 Gradle 守护进程，插件解析会直接失败；代理关闭时改这一行即可。

## 常用命令

```bat
g.bat build        :: 构建 build/libs/thkaguya-<version>.jar
g.bat runClient    :: 启动客户端（首次会自动生成 run/ 目录）
g.bat runServer    :: 启动服务端（首次需把 run/eula.txt 改成 eula=true）
g.bat runData      :: 数据生成，输出 src/generated/resources
g.bat clean
```

也可以直接用 `runClient.bat` / `runServer.bat` 双击运行。

> 首次 `build` 会执行 NeoForm 反编译（解压 + 反编译整个 Minecraft），**40–60 分钟属正常，不要中断**；之后增量构建是秒级到分钟级。

## 与 1.20.1 工程的差异（改代码时最容易踩的）

1. `RegistryObject` → `DeferredHolder` / `DeferredItem` / `DeferredBlock`。
2. `ItemStack.getTag()/getOrCreateTag()` → `DataComponents`（自定义组件必须同时给 `persistent` 和 `networkSynchronized`）。
3. `new ResourceLocation(...)` → `ResourceLocation.fromNamespaceAndPath(...)` / `ResourceLocation.parse(...)`。
4. 数据包目录单数化：`recipes/` → `recipe/`、`loot_tables/` → `loot_table/`、`advancements/` → `advancement/`、`forge/biome_modifier/` → `neoforge/biome_modifier/`。
5. `mods.toml` → `neoforge.mods.toml`，且 `modLoader="javafml"` + `loaderVersion="[1,)"` 必写。
6. 事件注解 `@Mod.EventBusSubscriber` → `@EventBusSubscriber`；配置 `ForgeConfigSpec` → `ModConfigSpec`。

完整清单见规划书附录 A，坑位见附录 B。
