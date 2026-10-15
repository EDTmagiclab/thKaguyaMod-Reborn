"""
简单的 JAR 提取和分析工具
这个脚本会提取 JAR 文件并生成基本的分析报告
"""
import zipfile
import os
import sys

def main():
    # 路径设置
    jar_file = r"E:\git\kaguya-mod-reborn-by-ai\五つの難題MOD+ ver2.90.1-1.7.10.jar"
    extract_dir = r"E:\git\kaguya-mod-reborn-by-ai\extracted"

    print("=" * 60)
    print("Minecraft Mod JAR 提取器")
    print("=" * 60)

    # 检查 JAR 文件
    if not os.path.exists(jar_file):
        print(f"错误: 找不到 JAR 文件: {jar_file}")
        return

    print(f"\nJAR 文件: {jar_file}")
    print(f"文件大小: {os.path.getsize(jar_file) / 1024:.2f} KB")

    # 创建输出目录
    os.makedirs(extract_dir, exist_ok=True)

    # 提取并分析
    print(f"\n正在提取到: {extract_dir}")

    with zipfile.ZipFile(jar_file, 'r') as jar:
        file_list = jar.namelist()

        # 提取所有文件
        jar.extractall(extract_dir)

        # 分析文件
        class_files = [f for f in file_list if f.endswith('.class') and '$' not in f]
        inner_class_files = [f for f in file_list if f.endswith('.class') and '$' in f]
        json_files = [f for f in file_list if f.endswith('.json')]
        png_files = [f for f in file_list if f.endswith('.png')]

        print(f"\n提取完成!")
        print(f"总文件数: {len(file_list)}")
        print(f"主类文件: {len(class_files)}")
        print(f"内部类文件: {len(inner_class_files)}")
        print(f"JSON 文件: {len(json_files)}")
        print(f"PNG 文件: {len(png_files)}")

        # 分析包结构
        packages = set()
        for cf in class_files:
            if '/' in cf:
                pkg = '/'.join(cf.split('/')[:-1])
                packages.add(pkg)

        print(f"\n包结构 (共 {len(packages)} 个包):")
        main_packages = {}
        for pkg in packages:
            root = pkg.split('/')[0]
            if root not in main_packages:
                main_packages[root] = []
            main_packages[root].append(pkg)

        for root, pkgs in sorted(main_packages.items()):
            print(f"\n  {root}/ ({len(pkgs)} 个子包)")
            for pkg in sorted(pkgs)[:5]:
                print(f"    - {pkg}")
            if len(pkgs) > 5:
                print(f"    ... 还有 {len(pkgs) - 5} 个")

        # 读取 mcmod.info
        if 'mcmod.info' in file_list:
            print(f"\n" + "=" * 60)
            print("Mod 信息 (mcmod.info)")
            print("=" * 60)
            try:
                content = jar.read('mcmod.info').decode('utf-8', errors='ignore')
                print(content)

                # 保存到文件
                with open(os.path.join(extract_dir, 'mcmod.info'), 'w', encoding='utf-8') as f:
                    f.write(content)
            except Exception as e:
                print(f"无法读取 mcmod.info: {e}")

        # 生成文件列表
        list_file = os.path.join(extract_dir, 'file_list.txt')
        with open(list_file, 'w', encoding='utf-8') as f:
            f.write(f"JAR 文件分析\n")
            f.write(f"{'=' * 60}\n\n")
            f.write(f"总文件数: {len(file_list)}\n")
            f.write(f"主类文件: {len(class_files)}\n")
            f.write(f"内部类文件: {len(inner_class_files)}\n\n")
            f.write(f"所有文件列表:\n")
            f.write(f"{'-' * 60}\n")
            for file in sorted(file_list):
                f.write(f"{file}\n")

        print(f"\n文件列表已保存到: {list_file}")

        # 生成类列表
        class_list_file = os.path.join(extract_dir, 'class_list.txt')
        with open(class_list_file, 'w', encoding='utf-8') as f:
            f.write(f"主要类文件 (不含内部类)\n")
            f.write(f"{'=' * 60}\n\n")
            for cf in sorted(class_files):
                # 转换为 Java 类名格式
                class_name = cf.replace('/', '.').replace('.class', '')
                f.write(f"{class_name}\n")

        print(f"类列表已保存到: {class_list_file}")

    print(f"\n" + "=" * 60)
    print("提取完成！")
    print("=" * 60)

if __name__ == "__main__":
    main()
