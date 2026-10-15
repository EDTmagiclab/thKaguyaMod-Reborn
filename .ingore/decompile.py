#!/usr/bin/env python3
"""
Minecraft Mod Decompiler
使用 CFR 反编译器来反编译 Minecraft mod
"""
import os
import sys
import zipfile
import subprocess
import shutil
from pathlib import Path

def extract_jar(jar_path, extract_dir):
    """提取 JAR 文件内容"""
    print(f"正在提取 JAR 文件: {jar_path}")
    with zipfile.ZipFile(jar_path, 'r') as zip_ref:
        zip_ref.extractall(extract_dir)
    print(f"已提取到: {extract_dir}")

def download_cfr():
    """下载 CFR 反编译器"""
    cfr_url = "https://github.com/leibnitz27/cfr/releases/latest/download/cfr.jar"
    cfr_path = "cfr.jar"

    if os.path.exists(cfr_path):
        print("CFR 反编译器已存在")
        return cfr_path

    print("正在下载 CFR 反编译器...")
    try:
        import urllib.request
        urllib.request.urlretrieve(cfr_url, cfr_path)
        print("CFR 下载完成")
        return cfr_path
    except Exception as e:
        print(f"下载 CFR 失败: {e}")
        return None

def decompile_with_cfr(jar_path, output_dir):
    """使用 CFR 反编译 JAR"""
    cfr_path = download_cfr()
    if not cfr_path:
        print("无法获取 CFR 反编译器")
        return False

    print(f"正在反编译 {jar_path}...")
    try:
        cmd = [
            "java", "-jar", cfr_path,
            jar_path,
            "--outputdir", output_dir,
            "--caseinsensitivefs", "true"
        ]
        subprocess.run(cmd, check=True)
        print(f"反编译完成，输出目录: {output_dir}")
        return True
    except Exception as e:
        print(f"反编译失败: {e}")
        return False

def main():
    # 设置路径
    jar_file = "五つの難題MOD+ ver2.90.1-1.7.10.jar"
    extract_dir = "extracted"
    code_dir = "code"

    # 创建输出目录
    os.makedirs(code_dir, exist_ok=True)

    # 反编译
    success = decompile_with_cfr(jar_file, code_dir)

    if success:
        print("\n反编译成功！")
        print(f"源代码位置: {code_dir}/")
    else:
        print("\n反编译失败，尝试手动提取...")
        extract_jar(jar_file, extract_dir)

if __name__ == "__main__":
    main()
