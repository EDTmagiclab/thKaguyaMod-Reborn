#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
直接分析 JAR 文件 - 内联执行版本
"""
import zipfile
import json
import re
from pathlib import Path

def analyze_jar():
    jar_path = Path(r"E:\git\kaguya-mod-reborn-by-ai\五つの難題MOD+ ver2.90.1-1.7.10.jar")

    if not jar_path.exists():
        return {"error": "JAR 文件不存在"}

    result = {
        "mod_info": {},
        "statistics": {},
        "packages": {},
        "file_types": {},
        "class_analysis": {}
    }

    try:
        with zipfile.ZipFile(jar_path, 'r') as jar:
            file_list = jar.namelist()

            # 文件分类
            class_files = [f for f in file_list if f.endswith('.class')]
            json_files = [f for f in file_list if f.endswith('.json')]
            png_files = [f for f in file_list if f.endswith('.png')]

            main_classes = [f for f in class_files if '$' not in f]
            inner_classes = [f for f in class_files if '$' in f]

            result["statistics"] = {
                "total_files": len(file_list),
                "main_classes": len(main_classes),
                "inner_classes": len(inner_classes),
                "json_files": len(json_files),
                "png_files": len(png_files)
            }

            # 分析包结构
            packages = {}
            for cf in main_classes:
                if '/' in cf:
                    pkg = '/'.join(cf.split('/')[:-1])
                    if pkg not in packages:
                        packages[pkg] = []
                    packages[pkg].append(cf.split('/')[-1])

            result["packages"] = {k: len(v) for k, v in packages.items()}

            # 读取 mcmod.info
            if 'mcmod.info' in file_list:
                content = jar.read('mcmod.info').decode('utf-8', errors='ignore')
                result["mod_info"]["raw"] = content
                try:
                    mod_data = json.loads(content)
                    if isinstance(mod_data, list) and len(mod_data) > 0:
                        mod_data = mod_data[0]
                    result["mod_info"]["parsed"] = mod_data
                except:
                    pass

            # 尝试读取一些类文件来分析内容
            sample_classes = main_classes[:5]  # 取前5个类
            for class_file in sample_classes:
                try:
                    class_bytes = jar.read(class_file)
                    # 简单检查是否包含某些字符串
                    class_str = str(class_bytes)

                    if 'Tessellator' in class_str or 'GL11' in class_str:
                        result["class_analysis"]["has_rendering"] = True
                    if 'GameRegistry' in class_str:
                        result["class_analysis"]["has_game_registry"] = True
                    if 'IIcon' in class_str:
                        result["class_analysis"]["has_iicon"] = True
                except:
                    pass

            result["success"] = True

    except Exception as e:
        result["error"] = str(e)

    return result

if __name__ == "__main__":
    result = analyze_jar()

    # 输出结果
    import json
    output_path = Path(r"E:\git\kaguya-mod-reborn-by-ai\jar_quick_analysis.json")
    with open(output_path, 'w', encoding='utf-8') as f:
        json.dump(result, f, indent=2, ensure_ascii=False)

    print("分析完成，结果已保存")
    print(json.dumps(result, indent=2, ensure_ascii=False))

analyze_jar()
