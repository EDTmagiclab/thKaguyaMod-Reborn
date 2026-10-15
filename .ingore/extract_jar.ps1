# PowerShell 脚本：提取并分析 Minecraft Mod JAR
$ErrorActionPreference = "Stop"
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Minecraft Mod JAR 提取和分析工具" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# 路径配置
$jarPath = "E:\git\kaguya-mod-reborn-by-ai\五つの難題MOD+ ver2.90.1-1.7.10.jar"
$extractDir = "E:\git\kaguya-mod-reborn-by-ai\extracted"
$codeDir = "E:\git\kaguya-mod-reborn-by-ai\code"

# 检查 JAR 文件
if (-not (Test-Path $jarPath)) {
    Write-Host "错误: 找不到 JAR 文件: $jarPath" -ForegroundColor Red
    exit 1
}

Write-Host "JAR 文件: $jarPath" -ForegroundColor Green
$fileSize = (Get-Item $jarPath).Length / 1KB
Write-Host "文件大小: $([math]::Round($fileSize, 2)) KB" -ForegroundColor Green
Write-Host ""

# 创建输出目录
New-Item -ItemType Directory -Force -Path $extractDir | Out-Null
New-Item -ItemType Directory -Force -Path $codeDir | Out-Null

Write-Host "正在提取 JAR 文件..." -ForegroundColor Yellow

# 使用 .NET 的 ZipFile 类提取
Add-Type -AssemblyName System.IO.Compression.FileSystem
[System.IO.Compression.ZipFile]::ExtractToDirectory($jarPath, $extractDir, $true)

Write-Host "提取完成！" -ForegroundColor Green
Write-Host ""

# 分析提取的文件
Write-Host "分析文件结构..." -ForegroundColor Yellow

$allFiles = Get-ChildItem -Path $extractDir -Recurse -File
$classFiles = $allFiles | Where-Object { $_.Extension -eq ".class" }
$jsonFiles = $allFiles | Where-Object { $_.Extension -eq ".json" }
$pngFiles = $allFiles | Where-Object { $_.Extension -eq ".png" }

Write-Host "统计信息:" -ForegroundColor Cyan
Write-Host "  总文件数: $($allFiles.Count)"
Write-Host "  .class 文件: $($classFiles.Count)"
Write-Host "  .json 文件: $($jsonFiles.Count)"
Write-Host "  .png 文件: $($pngFiles.Count)"
Write-Host ""

# 分析包结构
$packages = @{}
foreach ($cf in $classFiles) {
    $relativePath = $cf.FullName.Substring($extractDir.Length + 1)
    $pkgPath = Split-Path $relativePath -Parent
    if ($pkgPath) {
        $pkgPath = $pkgPath.Replace('\', '/')
        if (-not $packages.ContainsKey($pkgPath)) {
            $packages[$pkgPath] = @()
        }
        $packages[$pkgPath] += $cf.Name
    }
}

Write-Host "包结构 (共 $($packages.Count) 个包):" -ForegroundColor Cyan
$sortedPackages = $packages.Keys | Sort-Object | Select-Object -First 20
foreach ($pkg in $sortedPackages) {
    Write-Host "  $pkg`: $($packages[$pkg].Count) 个类"
}
if ($packages.Count -gt 20) {
    Write-Host "  ... 还有 $($packages.Count - 20) 个包"
}
Write-Host ""

# 读取 mcmod.info
$mcmodPath = Join-Path $extractDir "mcmod.info"
if (Test-Path $mcmodPath) {
    Write-Host "========================================" -ForegroundColor Cyan
    Write-Host "Mod 信息 (mcmod.info)" -ForegroundColor Cyan
    Write-Host "========================================" -ForegroundColor Cyan
    $mcmodContent = Get-Content $mcmodPath -Encoding UTF8 -Raw
    Write-Host $mcmodContent
    Write-Host ""
}

# 生成分析报告
$reportPath = Join-Path $extractDir "analysis_report.txt"
$report = @"
Minecraft Mod JAR 分析报告
========================================
文件: $jarPath
提取时间: $(Get-Date -Format "yyyy-MM-dd HH:mm:ss")

文件统计:
- 总文件数: $($allFiles.Count)
- .class 文件: $($classFiles.Count)
- .json 文件: $($jsonFiles.Count)
- .png 文件: $($pngFiles.Count)

包结构 (共 $($packages.Count) 个包):
$(($packages.Keys | Sort-Object | ForEach-Object { "  $_`: $($packages[$_].Count) 个类" }) -join "`n")

========================================
"@

$report | Out-File -FilePath $reportPath -Encoding UTF8

Write-Host "分析报告已保存: $reportPath" -ForegroundColor Green

# 生成类列表
$classListPath = Join-Path $extractDir "class_list.txt"
$classList = $classFiles | ForEach-Object {
    $relativePath = $_.FullName.Substring($extractDir.Length + 1)
    $className = $relativePath.Replace('\', '.').Replace('/', '.').Replace('.class', '')
    $className
} | Sort-Object

$classList | Out-File -FilePath $classListPath -Encoding UTF8
Write-Host "类列表已保存: $classListPath" -ForegroundColor Green

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "提取和分析完成！" -ForegroundColor Green
Write-Host "提取目录: $extractDir" -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Cyan
