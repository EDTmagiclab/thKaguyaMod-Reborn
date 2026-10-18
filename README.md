# 辉夜姬的五难题·重铸 / THKaguya Reborn

![Minecraft](https://img.shields.io/badge/Minecraft-1.20.1-38bdf8?style=flat-square)
![Forge](https://img.shields.io/badge/Forge-47.4.28%2B-e11d48?style=flat-square)
![Java](https://img.shields.io/badge/Java-17-f59e0b?style=flat-square)
![Release](https://img.shields.io/badge/release-beta--0.1.0-a855f7?style=flat-square)

把 2014 年的东方 Project 同人 Mod **「五つの難題MOD+ ver2.90.1」**（Minecraft 1.7.10 Forge）整体移植到 **Minecraft 1.20.1 Forge** 的非官方项目。项目基于原作者打包于最终版jar中的源代码二次开发移植而来

本项目使用了人工智能辅助编写代码，但主要功能全部进行了实机测试。主要使用的模型为Deepseek V4.1 Flash

我们已经尽可能进行了测试，但测试难免有疏漏之处。如果您在游玩中发现任何问题，欢迎通过issue报告问题。报告问题时希望能附上触发条件、具体表现与latest.log

## 内容移植

我们尽可能使模组的内容与原版相同，但仍有部分取舍，其中包括：

1. 霖之助村民的工作方块为弹幕工作台（适应村民更新）

2. boss改为永久性发光效果+原生boss血条

3. 早苗/霖之助/咲夜交易改为原生村民交易界面

4. 对星星弹幕进行了微调

5. 由于mod依赖原因，诅咒的人偶暂时只在创造模式可用



## 安装

1. 安装 **Minecraft 1.20.1** + **Forge 47.4.28**（或更高的 47.x）。
2. 把 `thkaguya-beta-0.1.0.jar` 放进 `mods/` 目录。
3. 启动游戏。首次启动会在 `config/` 生成 `thkaguya-common.toml`。

**客户端与服务端都要装**：本模组含自定义实体、渲染层与音效事件。我们的测试主要基于客户端，服务端有故障请携带日志提交issue。

## 从源码构建

需要 **JDK 17** 与 **Gradle 8.x**（仓库未附带 wrapper，请直接用系统 `gradle`）：

```bash
cd thkaguya-1.20.1
gradle build
# 产物：build/libs/thkaguya-0.1.0-beta.jar
```

首次构建会下载 ForgeGradle 6 与 Minecraft 1.20.1 依赖，需要数分钟。

## 实验性玩法：妖器化（万宝槌）

原作者编写了此代码，但将其完全注释掉了。我们将其做成了**实验性功能**：

- 在 Mods 列表 → `辉夜姬的五难题·重铸` → **Config** 里打开「万宝槌·妖器化」；
- 第一次开启会弹出确认页，提示「这是已经神隐（被原作者注释掉）的功能，你确定要开启吗？」；
- 开启后**潜行 + 右键**万宝槌打开「妖器化」界面，把素材炼成对应妖器（阴阳玉→渴望鲜血的阴阳玉、隙间→隙间的折叠伞、楼观剑→楼观旋风刃）；
- 开启后进入游戏会获得挑战进度「在倒悬的天守阁中」。

配置由服务端判定，多人服务器需要管理员开启。

## 与其他mod的兼容性

我们尚未测试与其他mod（尤其是其他东方project主题mod）的兼容性，在此不做保证。如遇兼容性问题，请携带日志提交issue，我们会尽量维护

## 版权与致谢

- 原作：**「五つの難題MOD+ ver2.90.1」**（Minecraft 1.7.10），版权归原作者所有。本仓库是**非官方移植**，与原作者无隶属关系。
- 移植代码（`thKaguyaMod` 的 1.20.1 适配部分、构建脚本、文档）由本项目维护者编写。
- 东方Project原作：上海爱丽丝幻乐团
