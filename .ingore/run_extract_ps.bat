@echo off
chcp 65001 >nul
echo ========================================
echo 正在运行 JAR 提取脚本...
echo ========================================
echo.

cd /d "E:\git\kaguya-mod-reborn-by-ai"

powershell.exe -ExecutionPolicy Bypass -File "extract_jar.ps1"

echo.
echo 完成！
pause
