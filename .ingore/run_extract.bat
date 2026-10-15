@echo off
chcp 65001 >nul
echo ========================================
echo Minecraft Mod JAR 提取器
echo ========================================
echo.

cd /d "E:\git\kaguya-mod-reborn-by-ai"

echo 正在运行 Python 提取脚本...
python extract_jar_simple.py

echo.
echo 按任意键退出...
pause >nul
