#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
报告生成器 - 将分析结果转换为 Markdown 报告
"""

import json
from pathlib import Path
from datetime import datetime

def load_analysis(json_file):
    """加载分析结果"""
    try:
        with open(json_file, 'r', encoding='utf-8') as f:
            return json.load(f)
    except FileNotFoundError:
        print(f"错误: 找不到分析文件 {json_file}")
        print("请先运行 analyze_feasibility.py")
        return None
    except json.JSONDecodeError as e:
        print(f"错误: JSON 解析失败: {e}")
        return None

def generate_feasibility_report(analysis, output_file):
    """生成可行性报告"""

    with open(output_file, 'w', encoding='utf-8') as f:
        f.write("# Minecraft Mod 移植可行性分析报告\n\n")
        f.write(f"**生成时间**: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}\n\n")
        f.write("---\n\n")

        # 1. Mod 信息
        f.write("## 1. Mod 基本信息\n\n")

        mod_info = analysis.get('mod_info', {})
        if mod_info.get('parsed'):
            data = mod_info['parsed']
            f.write(f"- **Mod 名称**: {data.get('name', 'Unknown')}\n")
            f.write(f"- **Mod ID**: {data.get('modid', 'Unknown')}\n")
            f.write(f"- **当前版本**: {data.get('version', 'Unknown')}\n")
            f.write(f"- **Minecraft 版本**: 1.7.10\n")
            f.write(f"- **目标版本**: 1.20.1\n")

            authors = data.get('authorList', data.get('authors', ['Unknown']))
            if isinstance(authors, list):
                f.write(f"- **作者**: {', '.join(authors)}\n")
            else:
                f.write(f"- **作者**: {authors}\n")

            if data.get('description'):
                f.write(f"- **描述**: {data['description']}\n")

            if data.get('url'):
                f.write(f"- **网址**: {data['url']}\n")
        else:
            f.write("*无法解析 Mod 信息*\n")

        f.write("\n")

        # 2. 代码规模分析
        f.write("## 2. 代码规模分析\n\n")

        stats = analysis.get('statistics', {})
        f.write(f"- **Java 源文件数**: {stats.get('total_java_files', 0)}\n")
        f.write(f"- **总代码行数**: {stats.get('total_lines', 0):,}\n")
        f.write(f"- **包数量**: {len(stats.get('packages', []))}\n\n")

        if stats.get('packages'):
            f.write("### 主要包结构\n\n")
            packages = stats['packages'][:15]
            for pkg in packages:
                f.write(f"- `{pkg}`\n")
            if len(stats['packages']) > 15:
                f.write(f"\n*... 还有 {len(stats['packages']) - 15} 个包*\n")
            f.write("\n")

        # 3. API 使用情况
        f.write("## 3. API 使用情况\n\n")

        api_usage = analysis.get('api_usage', {})
        if api_usage:
            f.write("检测到以下 1.7.10 API 的使用情况：\n\n")
            f.write("| API | 使用次数 | 迁移难度 |\n")
            f.write("|-----|---------|----------|\n")

            difficulty_map = {
                'GL11': '高',
                'Tessellator': '高',
                'IIcon': '高',
                'registerBlockIcons': '高',
                'getIcon': '高',
                'renderBlockByRenderType': '高',
                'GameRegistry': '中',
                'IMessage': '中',
                'SimpleNetworkWrapper': '中',
                'EntityRegistry': '中',
                'RenderingRegistry': '中',
                'Block.setBlockName': '低',
                'Item.setUnlocalizedName': '低',
                'cpw.mods.fml': '低',
            }

            for api, count in sorted(api_usage.items(), key=lambda x: x[1], reverse=True):
                difficulty = difficulty_map.get(api, '中')
                f.write(f"| `{api}` | {count} | {difficulty} |\n")

            f.write("\n")
        else:
            f.write("*无法检测 API 使用（可能代码未反编译）*\n\n")

        # 4. 兼容性问题
        f.write("## 4. 兼容性问题分析\n\n")

        issues = analysis.get('compatibility_issues', [])
        if issues:
            # 按严重性分组
            high_issues = [i for i in issues if i['severity'] == 'HIGH']
            medium_issues = [i for i in issues if i['severity'] == 'MEDIUM']
            low_issues = [i for i in issues if i['severity'] == 'LOW']

            if high_issues:
                f.write("### 🔴 高严重性问题\n\n")
                for issue in high_issues:
                    f.write(f"#### {issue['category']}\n\n")
                    f.write(f"**问题描述**: {issue['description']}\n\n")
                    f.write(f"**影响**: {issue['impact']}\n\n")
                    f.write(f"**工作量**: {issue['effort']}\n\n")
                    f.write(f"**难度分数**: {issue['score']}/200\n\n")

            if medium_issues:
                f.write("### 🟡 中等严重性问题\n\n")
                for issue in medium_issues:
                    f.write(f"#### {issue['category']}\n\n")
                    f.write(f"**问题描述**: {issue['description']}\n\n")
                    f.write(f"**影响**: {issue['impact']}\n\n")
                    f.write(f"**工作量**: {issue['effort']}\n\n")

            if low_issues:
                f.write("### 🟢 低严重性问题\n\n")
                for issue in low_issues:
                    f.write(f"- **{issue['category']}**: {issue['description']}\n")
                f.write("\n")
        else:
            f.write("*未检测到兼容性问题（需要先反编译代码）*\n\n")

        # 5. 可行性评估
        f.write("## 5. 移植可行性评估\n\n")

        feasibility = analysis.get('feasibility', {})
        score = feasibility.get('score', 0)
        level = feasibility.get('level', 'UNKNOWN')

        f.write(f"### 综合评分\n\n")
        f.write(f"**难度评分**: {score}/200\n\n")

        # 可视化评分
        bar_length = min(int(score / 10), 20)
        bar = "█" * bar_length + "░" * (20 - bar_length)
        f.write(f"```\n{bar} {score}%\n```\n\n")

        # 可行性等级
        level_emoji = {
            'HIGH': '✅',
            'MEDIUM': '⚠️',
            'LOW': '❌',
            'VERY_LOW': '🚫'
        }

        f.write(f"### {level_emoji.get(level, '❓')} 可行性等级: {level}\n\n")
        f.write(f"**建议**: {feasibility.get('recommendation', 'N/A')}\n\n")
        f.write(f"**预计总工时**: {feasibility.get('estimated_total_hours', 0)} 小时\n\n")

        # 6. 结论和建议
        f.write("## 6. 结论和建议\n\n")

        if level in ['HIGH', 'MEDIUM']:
            f.write("### ✅ 移植可行\n\n")
            f.write("根据分析，该 Mod 可以移植到 Forge 1.20.1，但需要注意以下几点：\n\n")

            if high_issues:
                f.write("**主要挑战**：\n\n")
                for issue in high_issues:
                    f.write(f"- {issue['category']}: {issue['description']}\n")
                f.write("\n")

            f.write("**建议的行动计划**：\n\n")
            f.write("1. 阅读生成的 `MIGRATION_PLAN.md` 文件\n")
            f.write("2. 设置 Forge 1.20.1 开发环境\n")
            f.write("3. 按照优先级逐步迁移各个系统\n")
            f.write("4. 每完成一个模块就进行测试\n")
            f.write("5. 与原作者沟通（如果可能）\n\n")

        elif level == 'LOW':
            f.write("### ⚠️ 移植困难但可能\n\n")
            f.write("该 Mod 的移植存在较大挑战，需要大量重写代码。建议：\n\n")
            f.write("- 评估是否值得投入如此多的时间\n")
            f.write("- 考虑分阶段实现，先实现核心功能\n")
            f.write("- 寻找有经验的团队成员协助\n")
            f.write("- 预留充足的开发和测试时间\n\n")

        else:
            f.write("### ❌ 移植非常困难\n\n")
            f.write("该 Mod 的移植难度极高，几乎需要完全重写。建议：\n\n")
            f.write("- 考虑是否有必要移植，还是开发一个新的替代品\n")
            f.write("- 如果必须移植，建议作为长期项目规划\n")
            f.write("- 组建专业团队，制定详细的开发计划\n")
            f.write("- 考虑逐步实现功能，而不是一次性移植所有功能\n\n")

        # 7. 参考资源
        f.write("## 7. 参考资源\n\n")
        f.write("### Forge 1.20.1 官方文档\n\n")
        f.write("- [Forge 官方文档](https://docs.minecraftforge.net/)\n")
        f.write("- [Minecraft Forge GitHub](https://github.com/MinecraftForge/MinecraftForge)\n\n")

        f.write("### 迁移指南\n\n")
        f.write("- [1.7 to 1.8 Migration Primer](https://forge.gemwire.uk/wiki/Upgrading_to_1.8)\n")
        f.write("- [Forge Community Wiki](https://forge.gemwire.uk/wiki/Main_Page)\n")
        f.write("- [Minecraft Modding Discord](https://discord.gg/minecraftforge)\n\n")

        f.write("### 相关工具\n\n")
        f.write("- [CFR Decompiler](https://github.com/leibnitz27/cfr)\n")
        f.write("- [MCP Mappings](https://github.com/MinecraftForge/MCPConfig)\n")
        f.write("- [Parchment Mappings](https://github.com/ParchmentMC/Parchment)\n\n")

        f.write("---\n\n")
        f.write("*本报告由自动化工具生成，仅供参考。实际移植难度可能因具体情况而异。*\n")

    print(f"✓ 可行性报告已生成: {output_file}")

def generate_migration_plan(analysis, output_file):
    """生成移植任务书"""

    feasibility = analysis.get('feasibility', {})
    level = feasibility.get('level', 'UNKNOWN')

    # 只有在可行的情况下生成详细的任务书
    if level not in ['HIGH', 'MEDIUM']:
        print(f"⚠️  移植难度为 {level}，不生成详细任务书")
        return

    with open(output_file, 'w', encoding='utf-8') as f:
        f.write("# Minecraft Mod 移植任务书\n\n")
        f.write(f"**目标**: 将 Mod 从 Forge 1.7.10 移植到 Forge 1.20.1\n\n")
        f.write(f"**预计工时**: {feasibility.get('estimated_total_hours', 0)} 小时\n\n")
        f.write(f"**生成时间**: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}\n\n")
        f.write("---\n\n")

        # Mod 信息
        mod_info = analysis.get('mod_info', {}).get('parsed', {})
        if mod_info:
            f.write("## Mod 信息\n\n")
            f.write(f"- **名称**: {mod_info.get('name', 'Unknown')}\n")
            f.write(f"- **ID**: {mod_info.get('modid', 'Unknown')}\n")
            f.write(f"- **当前版本**: {mod_info.get('version', 'Unknown')}\n\n")

        # 任务列表
        f.write("## 移植任务列表\n\n")

        tasks = analysis.get('migration_tasks', [])
        if tasks:
            # 按阶段分组
            phases = {}
            for task in tasks:
                phase = task.get('phase', '其他')
                if phase not in phases:
                    phases[phase] = []
                phases[phase].append(task)

            # 按预定义顺序输出阶段
            phase_order = ['准备阶段', '核心迁移', '代码清理', '测试阶段', '其他']

            for phase in phase_order:
                if phase in phases:
                    f.write(f"### {phase}\n\n")

                    for task in phases[phase]:
                        priority = task.get('priority', 'MEDIUM')
                        priority_emoji = {'HIGH': '🔴', 'MEDIUM': '🟡', 'LOW': '🟢'}.get(priority, '⚪')

                        f.write(f"#### {priority_emoji} {task['task']}\n\n")
                        f.write(f"**描述**: {task['description']}\n\n")
                        f.write(f"**优先级**: {priority}\n\n")
                        f.write(f"**预计工时**: {task.get('estimated_hours', 0)} 小时\n\n")

                        if task.get('dependencies'):
                            f.write(f"**依赖**: {', '.join(task['dependencies'])}\n\n")

                        f.write("**完成标准**:\n\n")
                        f.write("- [ ] 代码编译无错误\n")
                        f.write("- [ ] 功能测试通过\n")
                        f.write("- [ ] 代码审查完成\n\n")
                        f.write("---\n\n")

        # 详细的迁移指南
        f.write("## 详细迁移指南\n\n")

        issues = analysis.get('compatibility_issues', [])

        f.write("### 1. 环境准备\n\n")
        f.write("#### 必需软件\n\n")
        f.write("- **JDK**: Java 17 或更高版本\n")
        f.write("- **IDE**: IntelliJ IDEA 2023+ (推荐) 或 Eclipse\n")
        f.write("- **Gradle**: 8.1+ (通常由 Gradle Wrapper 管理)\n")
        f.write("- **Git**: 用于版本控制\n\n")

        f.write("#### Forge MDK 设置\n\n")
        f.write("```bash\n")
        f.write("# 1. 下载 Forge 1.20.1 MDK\n")
        f.write("# 访问: https://files.minecraftforge.net/net/minecraftforge/forge/\n\n")
        f.write("# 2. 解压并初始化\n")
        f.write("cd forge-1.20.1-mdk\n")
        f.write("./gradlew genIntellijRuns\n\n")
        f.write("# 3. 导入 IDE\n")
        f.write("# File -> Open -> 选择项目目录\n")
        f.write("```\n\n")

        # 针对检测到的问题提供详细指导
        if any(i['category'] == '注册系统' for i in issues):
            f.write("### 2. 注册系统迁移\n\n")
            f.write("#### 1.7.10 代码示例\n\n")
            f.write("```java\n")
            f.write("// 旧版本\n")
            f.write("GameRegistry.registerBlock(myBlock, \"my_block\");\n")
            f.write("GameRegistry.registerItem(myItem, \"my_item\");\n")
            f.write("```\n\n")

            f.write("#### 1.20.1 代码示例\n\n")
            f.write("```java\n")
            f.write("// 新版本 - 使用 DeferredRegister\n")
            f.write("public class ModBlocks {\n")
            f.write("    public static final DeferredRegister<Block> BLOCKS = \n")
            f.write("        DeferredRegister.create(ForgeRegistries.BLOCKS, MODID);\n\n")
            f.write("    public static final RegistryObject<Block> MY_BLOCK = \n")
            f.write("        BLOCKS.register(\"my_block\", () -> new MyBlock());\n")
            f.write("}\n\n")
            f.write("// 在主 Mod 类中\n")
            f.write("ModBlocks.BLOCKS.register(modEventBus);\n")
            f.write("```\n\n")

        if any(i['category'] == '渲染系统' for i in issues):
            f.write("### 3. 渲染系统迁移\n\n")
            f.write("**重要**: 1.20.1 完全移除了固定管线渲染，必须使用现代渲染管线。\n\n")

            f.write("#### 迁移步骤\n\n")
            f.write("1. **移除所有 GL11 调用**\n")
            f.write("2. **使用 PoseStack 和 VertexConsumer**\n")
            f.write("3. **实现 RenderType**\n")
            f.write("4. **创建自定义着色器（如需要）**\n\n")

            f.write("#### 示例代码\n\n")
            f.write("```java\n")
            f.write("// 1.20.1 渲染代码\n")
            f.write("@Override\n")
            f.write("public void render(Entity entity, float yaw, float partialTicks, \n")
            f.write("                   PoseStack poseStack, MultiBufferSource buffer, int light) {\n")
            f.write("    VertexConsumer consumer = buffer.getBuffer(RenderType.entityCutout(TEXTURE));\n")
            f.write("    // 使用 consumer 进行渲染\n")
            f.write("}\n")
            f.write("```\n\n")

        if any(i['category'] == '材质系统' for i in issues):
            f.write("### 4. 材质和模型系统\n\n")
            f.write("#### JSON 模型文件\n\n")
            f.write("1.20.1 使用 JSON 格式定义方块和物品模型。\n\n")

            f.write("**方块模型示例** (`assets/modid/models/block/my_block.json`):\n\n")
            f.write("```json\n")
            f.write("{\n")
            f.write("  \"parent\": \"block/cube_all\",\n")
            f.write("  \"textures\": {\n")
            f.write("    \"all\": \"modid:block/my_block\"\n")
            f.write("  }\n")
            f.write("}\n")
            f.write("```\n\n")

            f.write("**方块状态文件** (`assets/modid/blockstates/my_block.json`):\n\n")
            f.write("```json\n")
            f.write("{\n")
            f.write("  \"variants\": {\n")
            f.write("    \"\": { \"model\": \"modid:block/my_block\" }\n")
            f.write("  }\n")
            f.write("}\n")
            f.write("```\n\n")

        # 测试计划
        f.write("## 测试计划\n\n")
        f.write("### 单元测试\n\n")
        f.write("- [ ] 方块和物品注册测试\n")
        f.write("- [ ] 配方系统测试\n")
        f.write("- [ ] 数据序列化测试\n\n")

        f.write("### 功能测试\n\n")
        f.write("- [ ] 所有方块可以正常放置和破坏\n")
        f.write("- [ ] 所有物品可以正常获取和使用\n")
        f.write("- [ ] GUI 正常显示和交互\n")
        f.write("- [ ] 实体正常生成和行为\n")
        f.write("- [ ] 渲染效果正确\n\n")

        f.write("### 兼容性测试\n\n")
        f.write("- [ ] 与原版 Minecraft 兼容\n")
        f.write("- [ ] 与常见 Mod (JEI, REI) 兼容\n")
        f.write("- [ ] 多人模式测试\n")
        f.write("- [ ] 性能测试\n\n")

        # 风险和注意事项
        f.write("## 风险和注意事项\n\n")
        f.write("### 潜在风险\n\n")
        f.write("1. **API 变化**: 某些 API 可能已被完全移除\n")
        f.write("2. **行为差异**: Minecraft 内部逻辑可能已改变\n")
        f.write("3. **性能问题**: 新版本的性能特性可能不同\n")
        f.write("4. **兼容性**: 与其他 Mod 的兼容性需要额外测试\n\n")

        f.write("### 注意事项\n\n")
        f.write("- 定期备份代码\n")
        f.write("- 使用版本控制系统\n")
        f.write("- 保持与社区的沟通\n")
        f.write("- 参考其他成功的移植案例\n")
        f.write("- 不要试图一次性完成所有功能\n\n")

        # 里程碑
        f.write("## 项目里程碑\n\n")

        total_hours = feasibility.get('estimated_total_hours', 0)

        f.write("### 里程碑 1: 基础框架 (25%)\n")
        f.write(f"**预计时间**: {int(total_hours * 0.25)} 小时\n\n")
        f.write("- [ ] 开发环境设置完成\n")
        f.write("- [ ] 基本项目结构创建\n")
        f.write("- [ ] Mod 可以加载（即使功能未实现）\n\n")

        f.write("### 里程碑 2: 核心功能 (50%)\n")
        f.write(f"**预计时间**: {int(total_hours * 0.5)} 小时\n\n")
        f.write("- [ ] 方块和物品注册完成\n")
        f.write("- [ ] 基本功能实现\n")
        f.write("- [ ] 核心逻辑迁移完成\n\n")

        f.write("### 里程碑 3: 完整功能 (75%)\n")
        f.write(f"**预计时间**: {int(total_hours * 0.75)} 小时\n\n")
        f.write("- [ ] 所有功能实现\n")
        f.write("- [ ] 渲染系统完成\n")
        f.write("- [ ] GUI 完成\n\n")

        f.write("### 里程碑 4: 发布就绪 (100%)\n")
        f.write(f"**预计时间**: {total_hours} 小时\n\n")
        f.write("- [ ] 所有测试通过\n")
        f.write("- [ ] 文档完成\n")
        f.write("- [ ] 已知问题修复\n")
        f.write("- [ ] 准备发布\n\n")

        f.write("---\n\n")
        f.write("*本任务书基于代码分析自动生成，具体任务可能需要根据实际情况调整。*\n")

    print(f"✓ 移植任务书已生成: {output_file}")

def main():
    base_dir = Path(r"E:\git\kaguya-mod-reborn-by-ai")
    json_file = base_dir / "feasibility_analysis.json"

    # 加载分析结果
    analysis = load_analysis(json_file)
    if not analysis:
        return

    # 生成可行性报告
    feasibility_report = base_dir / "FEASIBILITY_REPORT.md"
    generate_feasibility_report(analysis, feasibility_report)

    # 生成移植任务书
    migration_plan = base_dir / "MIGRATION_PLAN.md"
    generate_migration_plan(analysis, migration_plan)

    print("\n" + "=" * 60)
    print("报告生成完成！")
    print("=" * 60)
    print(f"\n生成的文件：")
    print(f"  1. {feasibility_report}")
    print(f"  2. {migration_plan}")
    print(f"\n请查看这些文件以了解详细信息。")

if __name__ == "__main__":
    main()
