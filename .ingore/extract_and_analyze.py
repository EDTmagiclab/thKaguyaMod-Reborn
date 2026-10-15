import zipfile
import os
import json
from pathlib import Path
from collections import defaultdict

# 定义路径
jar_path = r"E:\git\kaguya-mod-reborn-by-ai\五つの難題MOD+ ver2.90.1-1.7.10.jar"
extract_path = r"E:\git\kaguya-mod-reborn-by-ai\extracted"

# 创建提取目录
os.makedirs(extract_path, exist_ok=True)

# 提取JAR文件
print("正在提取JAR文件...")
with zipfile.ZipFile(jar_path, 'r') as zip_ref:
    zip_ref.extractall(extract_path)
print(f"✓ 已提取到: {extract_path}\n")

# 统计分析
total_files = 0
class_files = 0
json_files = 0
file_types = defaultdict(int)
packages = set()

print("正在分析文件结构...")
for root, dirs, files in os.walk(extract_path):
    for file in files:
        total_files += 1
        file_path = os.path.join(root, file)

        # 统计文件类型
        ext = os.path.splitext(file)[1].lower()
        file_types[ext] += 1

        if ext == '.class':
            class_files += 1
            # 提取包结构
            rel_path = os.path.relpath(file_path, extract_path)
            package = os.path.dirname(rel_path).replace('\\', '.').replace('/', '.')
            if package:
                packages.add(package)
        elif ext == '.json':
            json_files += 1

# 输出统计信息
print("\n" + "="*60)
print("文件统计")
print("="*60)
print(f"总文件数: {total_files}")
print(f".class 文件数: {class_files}")
print(f".json 文件数: {json_files}")

print("\n主要文件类型分布:")
sorted_types = sorted(file_types.items(), key=lambda x: x[1], reverse=True)
for ext, count in sorted_types[:15]:
    if ext:
        print(f"  {ext}: {count}")
    else:
        print(f"  (无扩展名): {count}")

print("\n包结构 (顶层包):")
top_packages = set()
for pkg in sorted(packages):
    if pkg:
        top = pkg.split('.')[0]
        top_packages.add(top)

for pkg in sorted(top_packages):
    matching = [p for p in sorted(packages) if p.startswith(pkg + '.') or p == pkg]
    print(f"  {pkg}/ ({len(matching)} 个子包)")

# 查找并读取 mcmod.info
mcmod_path = os.path.join(extract_path, "mcmod.info")
if os.path.exists(mcmod_path):
    print("\n" + "="*60)
    print("mcmod.info 内容")
    print("="*60)
    with open(mcmod_path, 'r', encoding='utf-8') as f:
        content = f.read()
        print(content)

    # 尝试解析JSON
    try:
        data = json.loads(content)
        if isinstance(data, list) and len(data) > 0:
            mod_info = data[0]
            print("\n" + "="*60)
            print("解析的MOD信息")
            print("="*60)
            print(f"  Mod ID: {mod_info.get('modid', 'N/A')}")
            print(f"  名称: {mod_info.get('name', 'N/A')}")
            print(f"  版本: {mod_info.get('version', 'N/A')}")
            print(f"  MC版本: {mod_info.get('mcversion', 'N/A')}")
            print(f"  描述: {mod_info.get('description', 'N/A')}")
            if 'authorList' in mod_info:
                print(f"  作者: {', '.join(mod_info.get('authorList', []))}")
            if 'url' in mod_info:
                print(f"  URL: {mod_info.get('url', 'N/A')}")
    except Exception as e:
        print(f"\n解析JSON时出错: {e}")
else:
    print("\n" + "="*60)
    print("⚠ 未找到 mcmod.info 文件")
    print("="*60)

# 生成分析报告
report_path = os.path.join(extract_path, "analysis_report.txt")
with open(report_path, 'w', encoding='utf-8') as f:
    f.write("="*60 + "\n")
    f.write("JAR文件分析报告\n")
    f.write("="*60 + "\n\n")
    f.write(f"源文件: {jar_path}\n")
    f.write(f"提取位置: {extract_path}\n\n")

    f.write("文件统计:\n")
    f.write(f"  总文件数: {total_files}\n")
    f.write(f"  .class 文件数: {class_files}\n")
    f.write(f"  .json 文件数: {json_files}\n\n")

    f.write("文件类型分布:\n")
    for ext, count in sorted_types:
        if ext:
            f.write(f"  {ext}: {count}\n")
        else:
            f.write(f"  (无扩展名): {count}\n")

    f.write("\n包结构:\n")
    for pkg in sorted(packages)[:50]:  # 限制数量
        f.write(f"  {pkg}\n")

    if os.path.exists(mcmod_path):
        f.write("\n" + "="*60 + "\n")
        f.write("mcmod.info 内容:\n")
        f.write("="*60 + "\n")
        with open(mcmod_path, 'r', encoding='utf-8') as mf:
            f.write(mf.read())

print("\n" + "="*60)
print(f"分析报告已保存到: {report_path}")
print("="*60)
print("\n提取和分析完成!")
