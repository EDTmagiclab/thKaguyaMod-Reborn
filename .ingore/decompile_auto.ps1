# PowerShell 脚本：下载 CFR 并反编译 JAR
$ErrorActionPreference = "Stop"
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Minecraft Mod 自动反编译工具" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# 路径配置
$baseDir = "E:\git\kaguya-mod-reborn-by-ai"
$jarFile = Join-Path $baseDir "五つの難題MOD+ ver2.90.1-1.7.10.jar"
$codeDir = Join-Path $baseDir "code"
$extractedDir = Join-Path $baseDir "extracted"
$cfrJar = Join-Path $baseDir "cfr.jar"

# 创建目录
New-Item -ItemType Directory -Force -Path $codeDir | Out-Null
New-Item -ItemType Directory -Force -Path $extractedDir | Out-Null

# 检查 JAR 文件
if (-not (Test-Path $jarFile)) {
    Write-Host "错误: 找不到 JAR 文件: $jarFile" -ForegroundColor Red
    exit 1
}

Write-Host "找到 Mod JAR: $jarFile" -ForegroundColor Green
Write-Host ""

# 第一步：提取 JAR 文件（用于分析）
Write-Host "步骤 1: 提取 JAR 文件..." -ForegroundColor Yellow
Add-Type -AssemblyName System.IO.Compression.FileSystem

try {
    [System.IO.Compression.ZipFile]::ExtractToDirectory($jarFile, $extractedDir, $true)
    Write-Host "✓ JAR 文件已提取到: $extractedDir" -ForegroundColor Green
} catch {
    Write-Host "注意: 提取时发生错误（可能文件已存在）: $_" -ForegroundColor Yellow
}

Write-Host ""

# 分析 mcmod.info
$mcmodPath = Join-Path $extractedDir "mcmod.info"
if (Test-Path $mcmodPath) {
    Write-Host "========================================" -ForegroundColor Cyan
    Write-Host "Mod 信息" -ForegroundColor Cyan
    Write-Host "========================================" -ForegroundColor Cyan
    $mcmodContent = Get-Content $mcmodPath -Encoding UTF8 -Raw
    Write-Host $mcmodContent
    Write-Host ""
}

# 第二步：检查是否有 Java
Write-Host "步骤 2: 检查 Java 环境..." -ForegroundColor Yellow
try {
    $javaVersion = java -version 2>&1 | Select-Object -First 1
    Write-Host "✓ 找到 Java: $javaVersion" -ForegroundColor Green
} catch {
    Write-Host "✗ 未找到 Java！请安装 Java Runtime Environment" -ForegroundColor Red
    Write-Host "下载地址: https://www.java.com/download/" -ForegroundColor Yellow
    exit 1
}

Write-Host ""

# 第三步：下载 CFR
Write-Host "步骤 3: 准备 CFR 反编译器..." -ForegroundColor Yellow

if (Test-Path $cfrJar) {
    Write-Host "✓ CFR 已存在" -ForegroundColor Green
} else {
    Write-Host "正在下载 CFR..." -ForegroundColor Yellow
    $cfrUrl = "https://github.com/leibnitz27/cfr/releases/download/0.152/cfr-0.152.jar"

    try {
        Invoke-WebRequest -Uri $cfrUrl -OutFile $cfrJar -UseBasicParsing
        Write-Host "✓ CFR 下载完成" -ForegroundColor Green
    } catch {
        Write-Host "✗ CFR 下载失败: $_" -ForegroundColor Red
        Write-Host "请手动下载 CFR 并放置在: $cfrJar" -ForegroundColor Yellow
        Write-Host "下载地址: https://github.com/leibnitz27/cfr/releases/latest" -ForegroundColor Yellow
        exit 1
    }
}

Write-Host ""

# 第四步：反编译
Write-Host "步骤 4: 反编译 JAR 文件..." -ForegroundColor Yellow
Write-Host "这可能需要几分钟时间，请耐心等待..." -ForegroundColor Yellow
Write-Host ""

$decompileArgs = @(
    "-jar", $cfrJar,
    $jarFile,
    "--outputdir", $codeDir,
    "--caseinsensitivefs", "true",
    "--silent", "false"
)

try {
    $process = Start-Process -FilePath "java" -ArgumentList $decompileArgs -Wait -NoNewWindow -PassThru

    if ($process.ExitCode -eq 0) {
        Write-Host "✓ 反编译完成！" -ForegroundColor Green
    } else {
        Write-Host "✗ 反编译过程出现错误（退出代码: $($process.ExitCode)）" -ForegroundColor Yellow
        Write-Host "但某些文件可能已成功反编译" -ForegroundColor Yellow
    }
} catch {
    Write-Host "✗ 反编译失败: $_" -ForegroundColor Red
    exit 1
}

Write-Host ""

# 第五步：分析反编译结果
Write-Host "步骤 5: 分析反编译结果..." -ForegroundColor Yellow

if (Test-Path $codeDir) {
    $javaFiles = Get-ChildItem -Path $codeDir -Recurse -Filter "*.java"

    Write-Host "✓ 找到 $($javaFiles.Count) 个 Java 源文件" -ForegroundColor Green
    Write-Host ""

    # 生成源文件列表
    $sourceListPath = Join-Path $baseDir "source_files.txt"
    $javaFiles | ForEach-Object {
        $_.FullName.Substring($codeDir.Length + 1)
    } | Sort-Object | Out-File -FilePath $sourceListPath -Encoding UTF8

    Write-Host "源文件列表已保存: $sourceListPath" -ForegroundColor Green

    # 显示包结构
    $packages = $javaFiles | ForEach-Object {
        $relativePath = $_.FullName.Substring($codeDir.Length + 1)
        if ($relativePath -match "(.+)[\\/][^\\/]+\.java$") {
            $matches[1].Replace('\', '/')
        }
    } | Select-Object -Unique | Sort-Object

    Write-Host ""
    Write-Host "包结构 (共 $($packages.Count) 个包):" -ForegroundColor Cyan
    $packages | Select-Object -First 20 | ForEach-Object {
        Write-Host "  $_"
    }
    if ($packages.Count -gt 20) {
        Write-Host "  ... 还有 $($packages.Count - 20) 个包"
    }
} else {
    Write-Host "✗ 未找到反编译输出目录" -ForegroundColor Red
}

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "反编译完成！" -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "源代码位置: $codeDir" -ForegroundColor Green
Write-Host "提取内容: $extractedDir" -ForegroundColor Green
Write-Host ""
Write-Host "下一步: 运行可行性分析脚本" -ForegroundColor Yellow
Write-Host "  python analyze_feasibility.py" -ForegroundColor Cyan
