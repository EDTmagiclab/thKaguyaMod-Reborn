#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Minecraft Mod 移植可行性分析工具
分析从 Forge 1.7.10 移植到 Forge 1.20.1 的可行性
"""

import os
import re
from pathlib import Path
from collections import defaultdict
import json

class ModAnalyzer:
    def __init__(self, code_dir, extracted_dir):
        self.code_dir = Path(code_dir)
        self.extracted_dir = Path(extracted_dir)
        self.analysis = {
            'mod_info': {},
            'statistics': {},
            'api_usage': defaultdict(int),
            'compatibility_issues': [],
            'migration_tasks': [],
            'difficulty_score': 0
        }

    def analyze_mod_info(self):
        """分析 mod 基本信息"""
        mcmod_path = self.extracted_dir / "mcmod.info"
        if mcmod_path.exists():
            try:
                with open(mcmod_path, 'r', encoding='utf-8') as f:
                    content = f.read()
                    self.analysis['mod_info'] = {
                        'raw_content': content,
                        'has_mcmod_info': True
                    }

                    # 尝试解析 JSON
                    try:
                        import json
                        mod_data = json.loads(content)
                        if isinstance(mod_data, list) and len(mod_data) > 0:
                            mod_data = mod_data[0]
                        self.analysis['mod_info']['parsed'] = mod_data
                    except:
                        pass
            except Exception as e:
                print(f"读取 mcmod.info 失败: {e}")

    def count_files(self):
        """统计文件数量"""
        if not self.code_dir.exists():
            print(f"警告: 代码目录不存在: {self.code_dir}")
            return

        java_files = list(self.code_dir.rglob("*.java"))

        self.analysis['statistics'] = {
            'total_java_files': len(java_files),
            'total_lines': 0,
            'packages': set(),
            'classes': []
        }

        for java_file in java_files:
            try:
                with open(java_file, 'r', encoding='utf-8', errors='ignore') as f:
                    lines = f.readlines()
                    self.analysis['statistics']['total_lines'] += len(lines)

                    # 提取包名
                    for line in lines[:20]:  # 通常包声明在前20行
                        if line.strip().startswith('package '):
                            pkg = line.strip().replace('package ', '').replace(';', '').strip()
                            self.analysis['statistics']['packages'].add(pkg)
                            break

                    # 提取类名
                    relative_path = java_file.relative_to(self.code_dir)
                    self.analysis['statistics']['classes'].append(str(relative_path))
            except Exception as e:
                print(f"读取文件失败 {java_file}: {e}")

        self.analysis['statistics']['packages'] = sorted(list(self.analysis['statistics']['packages']))

    def detect_api_usage(self):
        """检测使用的 Forge/Minecraft API"""
        if not self.code_dir.exists():
            return

        # 需要检测的 API 模式
        api_patterns = {
            # 1.7.10 特有的 API
            'GameRegistry': r'GameRegistry\.',
            'cpw.mods.fml': r'import\s+cpw\.mods\.fml',
            'Block.setBlockName': r'\.setBlockName\(',
            'Item.setUnlocalizedName': r'\.setUnlocalizedName\(',
            'EntityPlayer': r'EntityPlayer\s',
            'IIcon': r'IIcon\s',
            'registerBlockIcons': r'registerBlockIcons\(',
            'getIcon': r'getIcon\(',

            # 渲染相关
            'Tessellator': r'Tessellator\.',
            'GL11': r'GL11\.',
            'renderBlockByRenderType': r'renderBlockByRenderType',

            # 网络相关
            'IMessage': r'IMessage\s',
            'SimpleNetworkWrapper': r'SimpleNetworkWrapper',
            'PacketBuffer': r'PacketBuffer',

            # 实体相关
            'EntityRegistry': r'EntityRegistry\.',
            'RenderingRegistry': r'RenderingRegistry\.',

            # GUI 相关
            'GuiContainer': r'GuiContainer\s',
            'GuiScreen': r'GuiScreen\s',

            # 事件系统
            'SubscribeEvent': r'@SubscribeEvent',
            'FMLCommonHandler': r'FMLCommonHandler',
        }

        java_files = list(self.code_dir.rglob("*.java"))

        for java_file in java_files:
            try:
                with open(java_file, 'r', encoding='utf-8', errors='ignore') as f:
                    content = f.read()

                    for api_name, pattern in api_patterns.items():
                        if re.search(pattern, content):
                            self.analysis['api_usage'][api_name] += 1
            except Exception as e:
                print(f"分析文件失败 {java_file}: {e}")

    def evaluate_compatibility(self):
        """评估兼容性问题"""
        issues = []
        difficulty = 0

        # 基于 API 使用情况评估
        api_usage = self.analysis['api_usage']

        # 严重问题 - 渲染系统
        if api_usage.get('GL11', 0) > 10 or api_usage.get('Tessellator', 0) > 5:
            issues.append({
                'severity': 'HIGH',
                'category': '渲染系统',
                'description': 'Mod 使用了大量旧版固定管线渲染代码（GL11/Tessellator）',
                'impact': '1.20.1 完全重写了渲染系统，需要迁移到新的着色器系统',
                'effort': '高 - 需要重写所有自定义渲染代码',
                'score': 30
            })
            difficulty += 30

        if api_usage.get('IIcon', 0) > 0 or api_usage.get('registerBlockIcons', 0) > 0:
            issues.append({
                'severity': 'HIGH',
                'category': '材质系统',
                'description': '使用了 IIcon 材质系统',
                'impact': '1.20.1 使用完全不同的材质和模型系统（JSON 模型）',
                'effort': '高 - 需要创建 JSON 模型文件并重写材质注册代码',
                'score': 25
            })
            difficulty += 25

        # 中等问题 - 方块和物品注册
        if api_usage.get('GameRegistry', 0) > 0:
            issues.append({
                'severity': 'MEDIUM',
                'category': '注册系统',
                'description': '使用 GameRegistry 注册方块和物品',
                'impact': '1.20.1 使用 DeferredRegister 系统',
                'effort': '中 - 需要重写注册代码但逻辑相似',
                'score': 15
            })
            difficulty += 15

        if api_usage.get('Block.setBlockName', 0) > 0:
            issues.append({
                'severity': 'MEDIUM',
                'category': '命名系统',
                'description': '使用旧版 setBlockName/setUnlocalizedName',
                'impact': '1.20.1 使用 ResourceLocation 和注册名称',
                'effort': '低-中 - 简单的 API 替换',
                'score': 10
            })
            difficulty += 10

        # 网络系统
        if api_usage.get('IMessage', 0) > 0 or api_usage.get('SimpleNetworkWrapper', 0) > 0:
            issues.append({
                'severity': 'MEDIUM',
                'category': '网络通信',
                'description': '使用旧版 IMessage 网络系统',
                'impact': '1.20.1 使用新的网络协议系统',
                'effort': '中 - 需要重写网络通信代码',
                'score': 20
            })
            difficulty += 20

        # FML → Forge 迁移
        if api_usage.get('cpw.mods.fml', 0) > 0:
            issues.append({
                'severity': 'LOW',
                'category': '包名变更',
                'description': '使用 cpw.mods.fml 包',
                'impact': '包名已改为 net.minecraftforge.fml',
                'effort': '低 - 批量替换包名',
                'score': 5
            })
            difficulty += 5

        # 事件系统
        if api_usage.get('FMLCommonHandler', 0) > 0:
            issues.append({
                'severity': 'MEDIUM',
                'category': '事件总线',
                'description': '使用 FMLCommonHandler',
                'impact': '事件注册方式已更改',
                'effort': '低-中 - 需要更新事件注册代码',
                'score': 10
            })
            difficulty += 10

        self.analysis['compatibility_issues'] = issues
        self.analysis['difficulty_score'] = difficulty

    def generate_migration_tasks(self):
        """生成迁移任务列表"""
        tasks = []

        # 基础设置任务
        tasks.append({
            'phase': '准备阶段',
            'task': '设置 1.20.1 开发环境',
            'description': '安装 JDK 17+，配置 Forge MDK 1.20.1',
            'priority': 'HIGH',
            'estimated_hours': 2
        })

        tasks.append({
            'phase': '准备阶段',
            'task': '创建项目结构',
            'description': '使用 Forge 1.20.1 模板创建新项目，配置 build.gradle 和 mods.toml',
            'priority': 'HIGH',
            'estimated_hours': 2
        })

        # 基于检测到的问题生成任务
        api_usage = self.analysis['api_usage']

        if api_usage.get('GameRegistry', 0) > 0:
            tasks.append({
                'phase': '核心迁移',
                'task': '迁移注册系统',
                'description': '将 GameRegistry 替换为 DeferredRegister，更新方块、物品、实体注册代码',
                'priority': 'HIGH',
                'estimated_hours': 8
            })

        if api_usage.get('IIcon', 0) > 0 or api_usage.get('getIcon', 0) > 0:
            tasks.append({
                'phase': '核心迁移',
                'task': '迁移材质系统',
                'description': '创建 JSON 模型文件，移除 IIcon 相关代码，实现新的材质系统',
                'priority': 'HIGH',
                'estimated_hours': 16
            })

        if api_usage.get('GL11', 0) > 5 or api_usage.get('Tessellator', 0) > 3:
            tasks.append({
                'phase': '核心迁移',
                'task': '重写渲染系统',
                'description': '将固定管线渲染迁移到现代着色器系统，重写所有自定义渲染器',
                'priority': 'HIGH',
                'estimated_hours': 40
            })

        if api_usage.get('IMessage', 0) > 0:
            tasks.append({
                'phase': '核心迁移',
                'task': '迁移网络系统',
                'description': '重写网络数据包处理，使用新的网络协议',
                'priority': 'MEDIUM',
                'estimated_hours': 12
            })

        # 通用任务
        tasks.append({
            'phase': '代码清理',
            'task': '更新包名和导入',
            'description': '批量替换 cpw.mods.fml 为 net.minecraftforge.fml',
            'priority': 'MEDIUM',
            'estimated_hours': 2
        })

        tasks.append({
            'phase': '测试阶段',
            'task': '功能测试',
            'description': '测试所有 mod 功能，修复发现的问题',
            'priority': 'HIGH',
            'estimated_hours': 20
        })

        tasks.append({
            'phase': '测试阶段',
            'task': '兼容性测试',
            'description': '测试与其他常见 mod 的兼容性',
            'priority': 'MEDIUM',
            'estimated_hours': 8
        })

        self.analysis['migration_tasks'] = tasks

    def calculate_feasibility(self):
        """计算可行性"""
        difficulty = self.analysis['difficulty_score']
        total_files = self.analysis['statistics'].get('total_java_files', 0)
        total_lines = self.analysis['statistics'].get('total_lines', 0)

        # 基于规模调整难度
        if total_lines > 10000:
            difficulty += 20
        elif total_lines > 5000:
            difficulty += 10

        # 更新最终分数
        self.analysis['difficulty_score'] = difficulty

        # 判断可行性
        if difficulty < 50:
            feasibility = 'HIGH'
            recommendation = '可行 - 移植相对简单'
        elif difficulty < 100:
            feasibility = 'MEDIUM'
            recommendation = '可行 - 需要中等工作量'
        elif difficulty < 150:
            feasibility = 'LOW'
            recommendation = '困难 - 需要大量重写'
        else:
            feasibility = 'VERY_LOW'
            recommendation = '非常困难 - 几乎需要完全重写'

        self.analysis['feasibility'] = {
            'level': feasibility,
            'score': difficulty,
            'recommendation': recommendation,
            'estimated_total_hours': sum(task.get('estimated_hours', 0) for task in self.analysis['migration_tasks'])
        }

    def run_analysis(self):
        """运行完整分析"""
        print("开始分析 Minecraft Mod...")
        print("=" * 60)

        print("\n[1/6] 分析 Mod 信息...")
        self.analyze_mod_info()

        print("[2/6] 统计文件...")
        self.count_files()

        print("[3/6] 检测 API 使用...")
        self.detect_api_usage()

        print("[4/6] 评估兼容性...")
        self.evaluate_compatibility()

        print("[5/6] 生成迁移任务...")
        self.generate_migration_tasks()

        print("[6/6] 计算可行性...")
        self.calculate_feasibility()

        print("\n分析完成！")
        return self.analysis

    def save_report(self, output_file):
        """保存分析报告"""
        with open(output_file, 'w', encoding='utf-8') as f:
            json.dump(self.analysis, f, indent=2, ensure_ascii=False, default=str)
        print(f"\n详细报告已保存: {output_file}")

def main():
    base_dir = Path(r"E:\git\kaguya-mod-reborn-by-ai")
    code_dir = base_dir / "code"
    extracted_dir = base_dir / "extracted"

    analyzer = ModAnalyzer(code_dir, extracted_dir)
    analysis = analyzer.run_analysis()

    # 保存 JSON 报告
    json_report = base_dir / "feasibility_analysis.json"
    analyzer.save_report(json_report)

    # 生成可读性报告
    print("\n" + "=" * 60)
    print("可行性分析报告")
    print("=" * 60)

    # Mod 信息
    if analysis['mod_info'].get('parsed'):
        mod_data = analysis['mod_info']['parsed']
        print(f"\nMod 名称: {mod_data.get('name', 'Unknown')}")
        print(f"Mod ID: {mod_data.get('modid', 'Unknown')}")
        print(f"版本: {mod_data.get('version', 'Unknown')}")
        print(f"作者: {mod_data.get('authorList', ['Unknown'])}")
        print(f"描述: {mod_data.get('description', 'N/A')}")

    # 统计信息
    stats = analysis['statistics']
    print(f"\n代码规模:")
    print(f"  Java 文件: {stats.get('total_java_files', 0)}")
    print(f"  代码行数: {stats.get('total_lines', 0)}")
    print(f"  包数量: {len(stats.get('packages', []))}")

    # 兼容性问题
    print(f"\n兼容性问题 ({len(analysis['compatibility_issues'])} 个):")
    for issue in analysis['compatibility_issues']:
        print(f"\n  [{issue['severity']}] {issue['category']}")
        print(f"    问题: {issue['description']}")
        print(f"    影响: {issue['impact']}")
        print(f"    工作量: {issue['effort']}")

    # 可行性结论
    feasibility = analysis['feasibility']
    print(f"\n" + "=" * 60)
    print(f"可行性评估")
    print("=" * 60)
    print(f"难度评分: {feasibility['score']}/200")
    print(f"可行性等级: {feasibility['level']}")
    print(f"建议: {feasibility['recommendation']}")
    print(f"预计总工时: {feasibility['estimated_total_hours']} 小时")

    print(f"\n详细的 JSON 报告: {json_report}")
    print(f"\n下一步: 查看报告并决定是否继续移植")

if __name__ == "__main__":
    main()
