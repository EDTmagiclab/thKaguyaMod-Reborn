import zipfile
import os

jar_path = r"E:\git\kaguya-mod-reborn-by-ai\五つの難題MOD+ ver2.90.1-1.7.10.jar"
extract_dir = r"E:\git\kaguya-mod-reborn-by-ai\extracted"

os.makedirs(extract_dir, exist_ok=True)

with zipfile.ZipFile(jar_path, 'r') as jar:
    jar.extractall(extract_dir)
    print(f"Extracted to {extract_dir}")
    print(f"Files: {len(jar.namelist())}")
