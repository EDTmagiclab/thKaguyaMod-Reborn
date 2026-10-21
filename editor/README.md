# THKaguya 可视化弹幕编辑器

纯前端、零依赖的符卡编辑器：**拖时间轴 → 看 3D 预览 → 导出 Java 符卡类 / Schema 1.0 JSON**。

目标不是「画一张好看的图」，而是让编辑器里看到的弹幕和游戏里 `EntityTHShot.tick()` 真正跑出来的一致：
`danmaku-core.js` 逐行复刻了 `THShotLib.create*` 的几何、`EntityTHShot` 的物理（延迟、加速、重力、自转、旋转终止）
以及 `sphere` 的 `ways[][]` 配比表，所以预览里的轨迹就是游戏里的轨迹。

## 快速开始

直接用浏览器打开 `danmaku-editor.html`（`file://` 就行，不需要服务器、不需要构建）：

```bash
# Windows
start editor\danmaku-editor.html

# 想直接看某张预设
start "editor\danmaku-editor.html#moon"
```

界面分三栏：左边符卡信息 + 事件列表，中间 3D 预览 + 时间轴，右边选中事件的参数表单。
右上角「导出」有四个页签：**Java 代码 / JSON / Markdown 报告 / 导入**。

常用快捷键：`空格` 播放暂停、`←/→` 单 tick 步进、`Home/End` 跳到首尾、
`Ctrl+Z / Ctrl+Shift+Z` 撤销重做、`Ctrl+S` 立即存档、`Ctrl+E` 打开导出。
画布上按住拖动转相机、滚轮缩放视角。

## 文件构成

| 文件 | 作用 |
| --- | --- |
| `danmaku-core.js` | 核心库：表达式引擎、`THShotLib` 几何、物理仿真、Java 生成、Schema 1.0 双向转换、校验。浏览器挂 `window.DMK`，Node 里 `require` 同源。 |
| `presets.js` | 8 张示例符卡（含复刻自 `json-spellcards/` 的 46/47/48 号）。编辑器、预览渲染、自检共用这一份。 |
| `editor-app.js` | 界面层：三栏布局、画布、时间轴、表单、撤销、`localStorage` 存档、导出弹窗。 |
| `danmaku-editor.html` | 外壳：CSS + DOM，只负责按顺序加载上面三个脚本。 |
| `test-core.js` | 核心层断言（147 项）：几何/物理/表达式/Schema 往返/Java 生成/全部预设。 |
| `test-ui.js` | 界面层冒烟（87 项）：手写 DOM 桩 + `node:vm` 把界面真跑一遍。 |
| `render-preview.js` | 离线出图：把仿真结果光栅化成 PNG，用来肉眼检查弹幕形状。 |
| `verify-java.js` | 把生成的 Java 拿去**真编译**：所有预设 + 一张覆盖 18 个 op 的合成卡 + 一张表达式压力卡，用 mod 自己的 classpath 跑 `javac`。 |

```bash
node editor/test-core.js        # 核心层 147 项
node editor/test-ui.js          # 界面层 87 项
node editor/render-preview.js saikou    # 出 4 帧预览图 out-saikou.png
node editor/render-preview.js --all     # 所有预设各出一张
node editor/verify-java.js              # 生成的 Java 是否真的能编译
```

`verify-java.js` 依赖 `thkaguya-1.21.1/build/compile-cp.txt`（NeoForge 构建脚本产出的类路径缓存，
跑过一次 `g.bat build` 就有）；文件不存在时它会打印提示并以 0 退出。

## 编辑模型

一张符卡 = 元数据 + `vars` 宏 + `events` 列表：

```
{ name, nameEn, slug, id, needLevel, endTime, removeTime, originalUser, circleColor,
  maxTick, level, userYaw, vars: { 名字: 表达式 }, specials: {...}, events: [...] }
```

- **events[i].when**：`{kind:'at', a}` / `{kind:'range', a, b}`（左闭右开）/ `{kind:'every', a, b?}`，
  不填就是第 0 tick 触发一次。
- **events[i].guard**：可选的额外条件表达式，比如 `time % 30 < 10 && time % 2 == 1`。
- **events[i].op**：18 个 op，见下。**所有数值字段都可以写表达式**，输入框里算不出来会标红。
- **vars**：懒求值宏。写 `a = "time*6 + i*(360/7)"`，事件里直接用 `a`，每发弹各算各的
  （`i` 是批量序号 —— 这是复刻「7 发弹各转一个角度」这类效果的关键）。

批量字段按 Schema 1.0 收在嵌套对象里：`spread{way, span, base}`（扇状）、`spray{way, span, base}`（环/球状），
表单会自动把旧写法（扁平的 `way`/`wideAngle`/`ringSpan`）迁移进去。

### 支持的 op

| 类别 | op |
| --- | --- |
| 弹 | `shot` `wide` `circle` `ring` `random_ring` `sphere` `nuclear` |
| 激光 | `laser_a` `laser_b` `wide_laser` `circle_laser` `ring_laser` `random_ring_laser` `sphere_laser` |
| 动作 | `remove_danmaku` `explosion_effect` `spawn` `sound` |

发射点 `anchor`：`user_eye`（默认）/ `card` / `target` / `user_feet` / `vec{x,y,z}`。
朝向 `aim`：`look`（默认）/ `target` / `yawpitch{yaw,pitch}` / `dir{x,y,z}`。

### 表达式

求值在「度」上，可用：

- 变量 `time`（≡`tick`）`level` `i` `way` `userYaw` `userPitch` `bbHeight` `cardYaw` `cardPitch` `look.x/y/z` `pi` `e`
- 函数 `sin cos tan sqrt abs floor ceil round min max pow rand() randInt(n) yawOf(x,z) pitchOf(x,y,z)`
- 表值 `SIZE[FORM_TINY]`、`DAMAGE[FORM_RICE]`、`@SIZE[9]`；`^` 是右结合幂

`sin/cos/tan` 吃度数（和 `THShotLib.radTrans` 一致）。导出 Java 时表达式会被翻译成
`Math.sin(x * Math.PI / 180.0)`、`SIZE[(int)(x) % 32]` 这种能直接编译的形式。

## 导出

- **Java**：生成 `package thKaguyaMod.entity.spellcard;` 下 `class THSC_<slug> extends THSpellCard`，
  构造函数写好 `setNeedLevel/setEndTime/setRemoveTime/setOriginalUserName`，事件展开进 `spellcard_main()`。
  按钮能直接下载成 `<slug>.java`，丢进 `thkaguya-1.21.1/src/main/java/thKaguyaMod/entity/spellcard/` 即可。
  生成器对齐的是 `THShotLib.java` 里**真实存在的重载**（例如 `laser_a` 只调 9 参
  `createLaserA(user, source, pos, angle, first, limit, accel, gravity, laser)`），不会产出编译不过的调用。
  `node editor/verify-java.js` 会把生成结果拿去真编译一遍。
  **注意 slug 别和已有的类重名**：`THSC_<slug>.java` 已存在时再丢一份进去会重复定义，
  预设里的复刻卡都带 `Demo` 后缀（`THSC_SaikouRanbuDemo` 等）就是为了让人放心改。
- **JSON**：`符卡JSON设计文档.md` 的 Schema 1.0（蛇形字段、嵌套 `spread`/`spray`、`when` 四种写法、
  `vars`/`specials`/`notes`/`circle_color`）。**可以反向导入**：把 `json-spellcards/` 里的卡丢进
  「导入」页签就能继续编辑，导出再导入是幂等的。
- **Markdown**：事件表 + 问题清单，贴进 issue / PR 说明用。
- 打开页面时自动存档到 `localStorage`（键名 `thkaguya.dmk.v1`），刷新不丢；「新建」清空重来。

## 已知限制（有意为之，不是漏做）

- **`specials` 状态机不做可视化**：它是嵌套的动作 DSL（`set`/`rotate`/`respawn`/`next`/`special_end`），
  表单化会失真，目前只原样读写 JSON、Java 导出不处理。编辑器会在左侧给出提示。
- **预览是简化 sprite**：形状用圆/星/菱形/心/椭圆近似，不做 `RenderTHShot` 的混合模式与拖尾。
- **不模拟命中**：没有玩家判定、没有消弹，HUD 只统计「同屏峰值 / 累计 / 时长」。
- 原版的两处怪癖被如实保留并加了警告：
  - `wide` 系列 `way < 2` 时 `span = wideAngle / (way - 1)` 会除零；
  - `createCircleLaserA` 内部按 `way + 1` 发，实际比填的多 1 束；
  - `sphere` 的 `way ≥ 45` 时原版直接 `return`（不发射）；
  - 弹体 `damage ≤ 0` 在游戏里第一个 tick 就消失。
- `float` 字段（`size`/`damage`/`width`/`length`/角度）在 Java 里输出 `1.0F` 字面量，
  表达式则包成 `(float)(...)`；`int` 字段包成 `(int)(...)`。别手改成 `(a+b)F`，Java 不接受。

## 给接手的人

- 想加新 op：在 `danmaku-core.js` 的 `OP_INFO` 里登记（`batch` / `data` / 各项能力标志），
  然后补三处 —— `expandEvent` 的仿真分支、`eventJava` 的生成分支、`eventToSchema`/`eventFromSchema` 的映射。
  表单和校验会凭 `OP_INFO` 自动跟上，`test-core.js` 的 `[14]` 组会强制每个 op 都有 Java 生成结果。
- 改完一定要跑 `node editor/test-core.js`，再用 headless Chrome 截图核对界面：

  ```bash
  chrome --headless=new --disable-gpu --hide-scrollbars --window-size=1500,900 \
         --virtual-time-budget=4000 --screenshot=shot.png \
         --user-data-dir="%TEMP%\dmkshot" "file:///<abs>/editor/danmaku-editor.html#saikou"
  ```
