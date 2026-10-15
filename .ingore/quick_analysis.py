import zipfile
import os
import json
from pathlib import Path

# 配置路径
jar_path = r"E:\git\kaguya-mod-reborn-by-ai\五つの難題MOD+ ver2.90.1-1.7.10.jar"
extract_dir = r"E:\git\kaguya-mod-reborn-by-ai\extracted"
output_file = r"E:\git\kaguya-mod-reborn-by-ai\initial_analysis.txt"

# 创建目录
os.makedirs(extract_dir, exist_ok=True)

print("=" * 70)
print("开始分析 JAR 文件...")
print("=" * 70)

try:
    # 打开 JAR 文件
    with zipfile.ZipFile(jar_path, 'r') as jar:
        file_list = jar.namelist()

        # 提取所有文件
        print(f"\n正在提取 {len(file_list)} 个文件...")
        jar.extractall(extract_dir)
        print("✓ 提取完成")

        # 分类统计
        class_files = [f for f in file_list if f.endswith('.class')]
        json_files = [f for f in file_list if f.endswith('.json')]
        png_files = [f for f in file_list if f.endswith('.png')]
        txt_files = [f for f in file_list if f.endswith(('.txt', '.info', '.md', '.properties'))]

        # 主类（不含内部类）
        main_classes = [f for f in class_files if '$' not in f]
        inner_classes = [f for f in class_files if '$' in f]

        print(f"\n文件统计:")
        print(f"  总文件数: {len(file_list)}")
        print(f"  主类文件: {len(main_classes)}")
        print(f"  内部类文件: {len(inner_classes)}")
        print(f"  JSON 文件: {len(json_files)}")
        print(f"  PNG 文件: {len(png_files)}")
        print(f"  文本文件: {len(txt_files)}")

        # 分析包结构
        packages = {}
        for cf in main_classes:
            if '/' in cf:
                pkg = '/'.join(cf.split('/')[:-1])
                if pkg not in packages:
                    packages[pkg] = []
                packages[pkg].append(cf.split('/')[-1])

        print(f"\n包结构 (共 {len(packages)} 个包):")
        for pkg in sorted(packages.keys())[:20]:
            print(f"  {pkg}: {len(packages[pkg])} 个类")

        if len(packages) > 20:
            print(f"  ... 还有 {len(packages) - 20} 个包")

        # 读取 mcmod.info
        if 'mcmod.info' in file_list:
            print(f"\n" + "=" * 70)
            print("Mod 信息 (mcmod.info)")
            print("=" * 70)
            content = jar.read('mcmod.info').decode('utf-8', errors='ignore')
            print(content)

            # 保存 mod 信息
            with open(os.path.join(extract_dir, 'mcmod.info'), 'w', encoding='utf-8') as f:
                f.write(content)

        # 保存分析结果
        with open(output_file, 'w', encoding='utf-8') as f:
            f.write("JAR 文件分析报告\n")
            f.write("=" * 70 + "\n\n")
            f.write(f"文件: {jar_path}\n\n")
            f.write(f"总文件数: {len(file_list)}\n")
            f.write(f"主类文件: {len(main_classes)}\n")
            f.write(f"内部类文件: {len(inner_classes)}\n")
            f.write(f"JSON 文件: {len(json_files)}\n")
            f.write(f"PNG 文件: {len(png_files)}\n")
            f.write(f"包数量: {len(packages)}\n\n")

            f.write("包列表:\n")
            f.write("-" * 70 + "\n")
            for pkg in sorted(packages.keys()):
                f.write(f"{pkg}: {len(packages[pkg])} 个类\n")

            f.write("\n主要类列表:\n")
            f.write("-" * 70 + "\n")
            for cf in sorted(main_classes)[:100]:
                class_name = cf.replace('/', '.').replace('.class', '')
                f.write(f"{class_name}\n")

            if len(main_classes) > 100:
                f.write(f"\n... 还有 {len(main_classes) - 100} 个类\n")

        print(f"\n✓ 分析结果已保存: {output_file}")

        # 生成类列表
        class_list_file = os.path.join(extract_dir, 'class_list.txt')
        with open(class_list_file, 'w', encoding='utf-8') as f:
            for cf in sorted(main_classes):
                class_name = cf.replace('/', '.').replace('.class', '')
                f.write(f"{class_name}\n")

        print(f"✓ 类列表已保存: {class_list_file}")

        print("\n" + "=" * 70)
        print("提取和初步分析完成！")
        print("=" * 70)

except Exception as e:
    print(f"\n错误: {e}")
    import traceback
    traceback.print_exc()
