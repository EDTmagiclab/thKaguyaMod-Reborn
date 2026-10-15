# JAR 文件反编译指南

## 方法一：使用 IntelliJ IDEA（推荐）

IntelliJ IDEA 自带 Fernflower 反编译器，这是最简单的方法。

### 步骤：
1. 打开 IntelliJ IDEA
2. File → Open → 选择 `五つの難題MOD+ ver2.90.1-1.7.10.jar`
3. IDEA 会自动反编译并显示源代码
4. 右键点击项目根目录 → Show in Explorer
5. 将反编译的代码复制到 `code/` 目录

## 方法二：使用 JD-GUI

JD-GUI 是一个独立的 Java 反编译器，带图形界面。

### 步骤：
1. 下载 JD-GUI: https://java-decompiler.github.io/
2. 运行 JD-GUI
3. File → Open File → 选择 JAR 文件
4. File → Save All Sources
5. 解压保存的 ZIP 文件到 `code/` 目录

## 方法三：使用 CFR 命令行（自动化）

CFR 是一个现代化的 Java 反编译器。

### 下载：
```bash
# 下载 CFR
curl -L https://github.com/leibnitz27/cfr/releases/latest/download/cfr.jar -o cfr.jar
```

### 使用：
```bash
# 反编译 JAR
java -jar cfr.jar "五つの難題MOD+ ver2.90.1-1.7.10.jar" --outputdir code/

# 或使用 PowerShell
java -jar cfr.jar "五つの難題MOD+ ver2.90.1-1.7.10.jar" --outputdir code/ --caseinsensitivefs true
```

## 方法四：使用 Procyon

Procyon 是另一个优秀的反编译器。

### 下载：
```bash
# 下载 Procyon
curl -L https://github.com/mstrobel/procyon/releases/download/v0.6.0/procyon-decompiler-0.6.0.jar -o procyon.jar
```

### 使用：
```bash
# 反编译 JAR
java -jar procyon.jar -jar "五つの難題MOD+ ver2.90.1-1.7.10.jar" -o code/
```

## 自动化脚本

我已经为你准备了以下自动化脚本：

### PowerShell 脚本
- `extract_jar.ps1` - 提取 JAR 并分析结构
- 运行后会在 `extracted/` 目录生成详细的分析报告

### Python 脚本  
- `extract.py` - Python 版本的提取工具
- 可以进一步修改以集成反编译功能

## 推荐工作流程

1. **首先提取并分析**
   ```bash
   # 运行提取脚本
   powershell -ExecutionPolicy Bypass -File extract_jar.ps1
   ```

2. **查看分析报告**
   - 查看 `extracted/analysis_report.txt`
   - 查看 `extracted/mcmod.info` 了解 mod 信息
   - 查看 `extracted/class_list.txt` 了解类结构

3. **反编译源代码**
   ```bash
   # 使用 CFR（如果已下载）
   java -jar cfr.jar "五つの難題MOD+ ver2.90.1-1.7.10.jar" --outputdir code/
   ```

4. **整理代码结构**
   - 反编译完成后，代码会在 `code/` 目录
   - 按照包结构组织

## 注意事项

- 反编译的代码可能不完美，需要手动修正
- 某些混淆的代码可能难以理解
- 内部类和匿名类的命名可能不直观
- 泛型信息可能丢失

## 下一步

反编译完成后，运行：
```bash
python analyze_code.py
```

这将分析源代码并生成移植可行性报告。
