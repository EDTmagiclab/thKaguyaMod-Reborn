# Kaguya Mod 逆向工程和移植项目

## 项目概述

本项目旨在将 Forge 1.7.10 版本的 Minecraft Mod "五つの難題MOD+ ver2.90.1" 逆向工程并分析移植到 Forge 1.20.1 的可行性。

## 文件说明

### JAR 文件
- `五つの難題MOD+ ver2.90.1-1.7.10.jar` - 原始的 1.7.10 版本 mod

### 提取工具（请选择其中一个运行）

#### 方法 1: PowerShell 脚本（推荐）
- `extract_jar.ps1` - 完整的 PowerShell 提取和分析脚本
- `run_extract_ps.bat` - 一键运行批处理文件

**使用方法:**
1. 双击 `run_extract_ps.bat`
2. 或在 PowerShell 中运行: `powershell -ExecutionPolicy Bypass -File extract_jar.ps1`

#### 方法 2: Python 脚本
- `extract.py` - Python 提取脚本
- `extract_jar_simple.py` - 简化的 Python 提取脚本  
- `run_extract.bat` - Python 脚本运行器

**使用方法:**
1. 确保已安装 Python 3.x
2. 双击 `run_extract.bat`
3. 或运行: `python extract.py`

### 输出目录结构

```
E:\git\kaguya-mod-reborn-by-ai\
├── extracted/              # JAR 文件提取的内容
│   ├── mcmod.info         # Mod 信息文件
│   ├── analysis_report.txt # 分析报告
│   ├── class_list.txt     # 所有类的列表
│   └── jar_analysis.json  # JSON 格式的分析数据
└── code/                  # 反编译后的源代码（待生成）
```

## 下一步操作

### 第一步：提取 JAR 文件
运行上述任一提取工具，将 JAR 内容提取到 `extracted/` 目录。

### 第二步：反编译（待完成）
提取完成后，需要使用 Java 反编译工具将 .class 文件转换为 .java 源代码。

推荐的反编译工具：
1. **CFR** - https://github.com/leibnitz27/cfr
2. **Fernflower** - IntelliJ IDEA 内置
3. **Procyon** - https://github.com/mstrobel/procyon

### 第三步：分析源代码
分析反编译后的代码，识别：
- Mod 的核心功能
- 使用的 Forge API
- Minecraft 版本特定的代码
- 第三方依赖

### 第四步：评估移植可行性
对比 1.7.10 和 1.20.1 之间的变化：
- 方块和物品注册系统的变化
- 渲染系统的重大改变
- 网络通信协议的更新
- 数据包（Data Pack）和资源包系统

## Minecraft 版本重大变化概述

### 1.7.10 → 1.20.1 主要变化

#### 1. 方块和物品系统
- **1.7.10**: 使用数字 ID 和 `GameRegistry`
- **1.20.1**: 使用 `ResourceLocation` 和 `DeferredRegister`

#### 2. 渲染系统
- **1.7.10**: 固定管线渲染（OpenGL 2.x）
- **1.20.1**: 现代化渲染管线（着色器系统）

#### 3. 方块状态
- **1.7.10**: 元数据系统
- **1.20.1**: 完整的 BlockState 系统

#### 4. 实体系统
- 完全重写的实体注册和渲染系统
- NBT 数据处理的变化

#### 5. 网络通信
- 从 `IMessage` 系统迁移到新的网络协议

## 许可证和版权

本项目仅用于学习和研究目的。原始 Mod 的版权归原作者所有。

## 进度追踪

- [x] 创建项目结构
- [ ] 提取 JAR 文件
- [ ] 反编译源代码
- [ ] 分析代码结构
- [ ] 评估移植可行性
- [ ] 创建移植任务书（如果可行）
