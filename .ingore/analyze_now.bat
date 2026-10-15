@echo off
chcp 65001 >nul
echo 正在分析 JAR 文件...
echo.

python -c "import zipfile, json; jar = zipfile.ZipFile(r'E:\git\kaguya-mod-reborn-by-ai\五つの難題MOD+ ver2.90.1-1.7.10.jar'); files = jar.namelist(); classes = [f for f in files if f.endswith('.class') and '$' not in f]; print(f'总文件: {len(files)}'); print(f'主类: {len(classes)}'); mcmod = jar.read('mcmod.info').decode('utf-8', errors='ignore') if 'mcmod.info' in files else 'N/A'; print('\\nMod 信息:'); print(mcmod); pkgs = {}; [pkgs.update({'/'.join(c.split('/')[:-1]): pkgs.get('/'.join(c.split('/')[:-1]), 0) + 1}) for c in classes if '/' in c]; print(f'\\n包数量: {len(pkgs)}'); [print(f'{k}: {v}') for k, v in sorted(pkgs.items())[:15]]"

echo.
echo 完成！按任意键退出...
pause >nul
