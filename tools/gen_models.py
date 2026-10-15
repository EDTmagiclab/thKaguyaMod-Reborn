#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Techne 模型 (1.7.10) → 1.20.1 LayerDefinition 完整模型类生成器。
生成：ModelXxx<T extends EntityDanmakuMob>，含 createBodyLayer + 通用 setupAnim + renderToBuffer。
用法：python gen_models.py <ModelXxx.java> [输出.java]
"""
import re
import sys

def strip_comments(src):
    src = re.sub(r'/\*.*?\*/', '', src, flags=re.S)
    src = re.sub(r'//[^\n]*', '', src)
    # 规整强制转换（原版有 (float)Math.PI 这种写法，会干扰括号匹配）
    src = re.sub(r'\(float\)\s*Math\.PI', '3.141593F', src)
    src = re.sub(r'\(double\)\s*Math\.PI', '3.141593D', src)
    src = re.sub(r'\(float\)', '', src)
    src = re.sub(r'\(double\)', '', src)
    return src

def extract_block(src, brace_idx):
    """从 brace_idx（指向 '{'）提取括号平衡的代码块，返回不含最外层 {} 的内容"""
    depth = 0
    i = brace_idx
    while i < len(src):
        if src[i] == '{':
            depth += 1
        elif src[i] == '}':
            depth -= 1
            if depth == 0:
                return src[brace_idx + 1:i]
        i += 1
    return ''

def parse_model(path):
    src = open(path, encoding='utf-8').read()
    cls = re.search(r'class\s+(\w+)', src).group(1)
    tw = re.search(r'textureWidth\s*=\s*(\d+)\s*;', src)
    th = re.search(r'textureHeight\s*=\s*(\d+)\s*;', src)
    tw = int(tw.group(1)) if tw else 64
    th = int(th.group(1)) if th else 64
    src = strip_comments(src)
    # 提取部件定义所在的代码块：构造器 + getTechneModelData + defaultAddChildSetting
    bodies = []
    for m in re.finditer(r'public\s+' + cls + r'\s*\([^)]*\)\s*\{', src):
        bodies.append(extract_block(src, m.end() - 1))
    for m in re.finditer(r'(?:protected|public)\s+void\s+getTechneModelData\s*\(\s*\)\s*\{', src):
        bodies.append(extract_block(src, m.end() - 1))
    for m in re.finditer(r'(?:protected|public)\s+void\s+defaultAddChildSetting\s*\(\s*\)\s*\{', src):
        bodies.append(extract_block(src, m.end() - 1))
    body = '\n'.join(bodies)
    return cls, tw, th, body

def parse_renderers(body):
    renderers = []
    cur = None
    for line in body.split('\n'):
        m = re.search(r'(\w+)\s*=\s*new\s+ModelRenderer\(this,\s*(\d+),\s*(\d+)\)', line)
        if m:
            if cur:
                renderers.append(cur)
            cur = {'name': m.group(1), 'texU': int(m.group(2)), 'texV': int(m.group(3)),
                   'boxes': [], 'rotPoint': None, 'rot': None, 'mirror': False, 'parent': None}
            continue
        if not cur:
            continue
        m = re.search(r'\.addBox\(([^)]+)\)', line)
        if m:
            cur['boxes'].append(m.group(1).strip()); continue
        m = re.search(r'\.setRotationPoint\(([^)]+)\)', line)
        if m:
            cur['rotPoint'] = m.group(1).strip(); continue
        m = re.search(r'setRotation\(\s*\w+\s*,\s*([^)]+)\)', line)
        if m:
            cur['rot'] = m.group(1).strip(); continue
        m = re.search(r'\.mirror\s*=\s*true', line)
        if m:
            cur['mirror'] = True; continue
        m = re.search(r'(\w+)\.addChild\(\s*(?:this\.)?(\w+)\s*\)', line)
        if m:
            parent, child = m.group(1), m.group(2)
            for r in renderers + [cur]:
                if r and r['name'] == child:
                    r['parent'] = parent
            continue
    if cur:
        renderers.append(cur)
    return renderers

def parent_chain(renderers, name):
    """返回从 root 到 name 的父链（不含 name 本身），如 ['bipedBody']"""
    chain = []
    cur_name = name
    while True:
        parent = None
        for r in renderers:
            if r['name'] == cur_name:
                parent = r['parent']; break
        if not parent:
            break
        chain.insert(0, parent)
        cur_name = parent
        if len(chain) > 20:
            break
    return chain

def gen_class(cls, tw, th, renderers):
    L = []
    L.append('package thKaguyaMod.client.model.living;')
    L.append('')
    L.append('import com.mojang.blaze3d.vertex.PoseStack;')
    L.append('import com.mojang.blaze3d.vertex.VertexConsumer;')
    L.append('import net.minecraft.client.model.EntityModel;')
    L.append('import net.minecraft.client.model.geom.ModelPart;')
    L.append('import net.minecraft.client.model.geom.PartPose;')
    L.append('import net.minecraft.client.model.geom.builders.CubeListBuilder;')
    L.append('import net.minecraft.client.model.geom.builders.LayerDefinition;')
    L.append('import net.minecraft.client.model.geom.builders.MeshDefinition;')
    L.append('import net.minecraft.client.model.geom.builders.PartDefinition;')
    L.append('import net.minecraft.util.Mth;')
    L.append('import thKaguyaMod.entity.living.EntityDanmakuMob;')
    L.append('')
    L.append('/** 自动生成自原版 ' + cls + '（Techne → LayerDefinition） */')
    L.append('public class ' + cls + '<T extends EntityDanmakuMob> extends EntityModel<T>')
    L.append('{')
    L.append('    private final ModelPart root;')
    # 关键部件字段（用于动画），脚本检测存在性
    anim_parts = ['head', 'body', 'rightArm', 'leftArm', 'rightLeg', 'leftLeg',
                  'rightWing', 'leftWing', 'rightWing2', 'leftWing2', 'rightWing3', 'leftWing3',
                  'bipedHead', 'bipedBody', 'bipedRightArm', 'bipedLeftArm', 'bipedRightLeg', 'bipedLeftLeg']
    # 检测模型里实际存在的部件名
    all_names = [r['name'] for r in renderers]
    present = [p for p in anim_parts if p in all_names]
    for p in present:
        L.append(f'    private final ModelPart {p};')
    L.append('')
    L.append(f'    public {cls}(ModelPart root)')
    L.append('    {')
    L.append('        this.root = root;')
    for p in present:
        chain = parent_chain(renderers, p)
        expr = 'root' + ''.join(f'.getChild("{c}")' for c in chain) + f'.getChild("{p}")'
        L.append(f'        this.{p} = {expr};')
    L.append('    }')
    L.append('')
    L.append('    public static LayerDefinition createBodyLayer()')
    L.append('    {')
    L.append('        MeshDefinition mesh = new MeshDefinition();')
    L.append('        PartDefinition root = mesh.getRoot();')
    L.append('')
    for r in renderers:
        var = r['name']
        parent_var = r['parent'] if r['parent'] else 'root'
        cb = f'CubeListBuilder.create().texOffs({r["texU"]}, {r["texV"]})'
        if r['mirror']:
            cb += '.mirror()'
        for box in r['boxes']:
            cb += f'.addBox({box})'
        rp = r['rotPoint'] if r['rotPoint'] else '0F, 0F, 0F'
        rot_is_zero = r['rot'] is None or all(
            float(x.strip().rstrip('Ff')) == 0.0 for x in r['rot'].split(',') if x.strip())
        if r['rot'] is not None and not rot_is_zero:
            pose = f'PartPose.offsetAndRotation({rp}, {r["rot"]})'
        else:
            pose = f'PartPose.offset({rp})'
        indent = '' if parent_var == 'root' else '    '
        L.append(f'{indent}        PartDefinition {var} = {parent_var}.addOrReplaceChild("{var}",')
        L.append(f'{indent}            {cb},')
        L.append(f'{indent}            {pose});')
    L.append('')
    L.append(f'        return LayerDefinition.create(mesh, {tw}, {th});')
    L.append('    }')
    L.append('')
    # setupAnim
    L.append('    @Override')
    L.append('    public void setupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch)')
    L.append('    {')
    # 头跟随
    for p in present:
        if 'head' in p.lower():
            L.append(f'        if({p} != null) {{ {p}.yRot = netHeadYaw * (float)Math.PI / 180F; {p}.xRot = headPitch * (float)Math.PI / 180F; }}')
    # 翅膀扇动
    for p in present:
        if 'wing' in p.lower():
            L.append(f'        if({p} != null) {{ {p}.yRot = Mth.cos(ageInTicks * 1.3F) * (float)Math.PI * 0.25F; }}')
    # 手臂摆动
    for p in present:
        if 'arm' in p.lower():
            L.append(f'        if({p} != null) {{ {p}.xRot = Mth.cos(limbSwing) * 0.7F * limbSwingAmount; }}')
    # 腿摆动
    for p in present:
        if 'leg' in p.lower():
            L.append(f'        if({p} != null) {{ {p}.xRot = Mth.cos(limbSwing + (float)Math.PI) * 0.7F * limbSwingAmount; }}')
    L.append('    }')
    L.append('')
    L.append('    @Override')
    L.append('    public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha)')
    L.append('    {')
    L.append('        root.render(poseStack, buffer, packedLight, packedOverlay, red, green, blue, alpha);')
    L.append('    }')
    L.append('}')
    return '\n'.join(L)

if __name__ == '__main__':
    path = sys.argv[1]
    out = sys.argv[2] if len(sys.argv) > 2 else None
    cls, tw, th, body = parse_model(path)
    renderers = parse_renderers(body)
    code = gen_class(cls, tw, th, renderers)
    if out:
        open(out, 'w', encoding='utf-8').write(code)
        print(f'written {out} ({len(renderers)} parts)')
    else:
        print(code)
