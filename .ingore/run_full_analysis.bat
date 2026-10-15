@echo off
chcp 65001 >nul
setlocal enabledelayedexpansion

echo ================================================================
echo Minecraft Mod 逆向工程和移植分析 - 完整流程
echo ================================================================
echo.

cd /d "E:\git\kaguya-mod-reborn-by-ai"

echo 本脚本将执行以下步骤：
echo   1. 提取 JAR 文件
echo   2. 下载并运行 CFR 反编译器
echo   3. 分析源代码
echo   4. 生成可行性报告
echo.
echo 按任意键开始，或关闭窗口取消...
pause >nul

echo.
echo ================================================================
echo 步骤 1/3: 反编译 JAR 文件
echo ================================================================
echo.

powershell.exe -ExecutionPolicy Bypass -File "decompile_auto.ps1"

if errorlevel 1 (
    echo.
    echo 反编译失败！请检查错误信息。
    pause
    exit /b 1
)

echo.
echo ================================================================
echo 步骤 2/3: 分析源代码
echo ================================================================
echo.

python analyze_feasibility.py

if errorlevel 1 (
    echo.
    echo 分析失败！请确保已安装 Python 3.x
    pause
    exit /b 1
)

echo.
echo ================================================================
echo 步骤 3/3: 生成报告
echo ================================================================
echo.

python generate_report.py

echo.
echo ================================================================
echo 完成！
echo ================================================================
echo.
echo 生成的文件：
echo   - code/           : 反编译的源代码
echo   - extracted/      : 提取的 JAR 内容
echo   - feasibility_analysis.json : JSON 格式的分析数据
echo   - FEASIBILITY_REPORT.md     : 可行性报告
echo   - MIGRATION_PLAN.md         : 移植任务书（如果可行）
echo.
echo 请查看报告文件以了解移植可行性！
echo.
pause
