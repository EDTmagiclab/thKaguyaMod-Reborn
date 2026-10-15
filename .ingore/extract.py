#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
直接在此执行的 JAR 提取和分析脚本
"""

if __name__ == "__main__":
    import zipfile
    import os
    import json
    from pathlib import Path

    # 配置路径
    BASE_DIR = Path(r"E:\git\kaguya-mod-reborn-by-ai")
    JAR_FILE = BASE_DIR / "五つの難題MOD+ ver2.90.1-1.7.10.jar"
    EXTRACT_DIR = BASE_DIR / "extracted"
    CODE_DIR = BASE_DIR / "code"

    # 创建目录
    EXTRACT_DIR.mkdir(exist_ok=True)
    CODE_DIR.mkdir(exist_ok=True)

    print("开始处理 JAR 文件...")
    print(f"JAR: {JAR_FILE}")
    print(f"输出: {EXTRACT_DIR}")

    # 打开并分析 JAR
    with zipfile.ZipFile(JAR_FILE, 'r') as jar:
        all_files = jar.namelist()

        print(f"\n发现 {len(all_files)} 个文件")

        # 提取所有文件
        print("提取中...")
        jar.extractall(EXTRACT_DIR)

        # 分类统计
        class_files = []
        json_files = []
        png_files = []
        txt_files = []

        for f in all_files:
            if f.endswith('.class'):
                class_files.append(f)
            elif f.endswith('.json'):
                json_files.append(f)
            elif f.endswith('.png'):
                png_files.append(f)
            elif f.endswith(('.txt', '.info', '.md')):
                txt_files.append(f)

        # 输出统计
        print(f"\n文件统计:")
        print(f"  .class 文件: {len(class_files)}")
        print(f"  .json 文件: {len(json_files)}")
        print(f"  .png 文件: {len(png_files)}")
        print(f"  文本文件: {len(txt_files)}")

        # 分析包结构
        packages = {}
        for cf in class_files:
            if '/' in cf:
                parts = cf.split('/')
                pkg_path = '/'.join(parts[:-1])
                class_name = parts[-1]

                if pkg_path not in packages:
                    packages[pkg_path] = []
                packages[pkg_path].append(class_name)

        print(f"\n包结构 ({len(packages)} 个包):")
        for pkg in sorted(packages.keys())[:20]:
            print(f"  {pkg}: {len(packages[pkg])} 个类")

        if len(packages) > 20:
            print(f"  ... 还有 {len(packages) - 20} 个包")

        # 读取 mod 信息
        mod_info = {}
        if 'mcmod.info' in all_files:
            print(f"\n{'='*60}")
            print("Mod 信息 (mcmod.info)")
            print('='*60)
            content = jar.read('mcmod.info').decode('utf-8', errors='ignore')
            print(content)

            # 尝试解析 JSON
            try:
                import json
                mod_info = json.loads(content)
                if isinstance(mod_info, list) and len(mod_info) > 0:
                    mod_info = mod_info[0]
            except:
                pass

        # 保存分析结果
        analysis = {
            'jar_file': str(JAR_FILE),
            'total_files': len(all_files),
            'class_files': len(class_files),
            'json_files': len(json_files),
            'png_files': len(png_files),
            'packages': list(packages.keys()),
            'package_count': len(packages),
            'mod_info': mod_info,
            'main_classes': [c for c in class_files if '$' not in c][:50]  # 前50个主类
        }

        analysis_file = EXTRACT_DIR / 'jar_analysis.json'
        with open(analysis_file, 'w', encoding='utf-8') as f:
            json.dump(analysis, f, indent=2, ensure_ascii=False)

        print(f"\n分析结果已保存: {analysis_file}")

        # 生成类列表文件
        class_list_file = EXTRACT_DIR / 'classes.txt'
        with open(class_list_file, 'w', encoding='utf-8') as f:
            for cf in sorted(class_files):
                class_name = cf.replace('/', '.').replace('.class', '')
                f.write(f"{class_name}\n")

        print(f"类列表已保存: {class_list_file}")

        print(f"\n{'='*60}")
        print("提取完成！")
        print(f"{'='*60}")
