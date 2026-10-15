#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Techne 模型 (1.7.10) → 1.20.1 LayerDefinition 模型类生成器（v2）。

支持三种原版写法：
  1. 标准 Techne（单变量）：
       bipedHead = new ModelRenderer(this, 0, 0);
       bipedHead.addBox(-4F, -8F, -4F, 8, 8, 8);
       bipedHead.setRotationPoint(0F, 10F, 0F);
       bipedHead.mirror = true;
       setRotation(bipedHead, 0F, 0F, 0F);            // setRotation 方法调用
  2. 数组式（逐元素 / for 循环）：
       hisou = new ModelRenderer[3];
       hisou[0] = new ModelRenderer(this, 32, 0);
       hisou[0].addBox(-2, -12, -2, 4, 8, 4, 0.0F);
       // 或：
       for(int i = 0; i < 8; i++) {
           mini[i] = new ModelRenderer(this, 32, 0);
           mini[i].addBox(-4, -10, -0.001F, 8, 2, 6, 0.0F);
           mini[i].rotateAngleZ = angle;              // 循环内变量累加
           angle += pi8;
       }
  3. rotateAngleX/Y/Z 赋值（弧度）：
       kinkakuzi.rotateAngleX = ((float)Math.PI / 2F);

表达式求值：支持 (float)/(double) 强制转换、Math.PI、MathHelper.cos/sin、
循环变量、float 字面量后缀 F/f、循环外 float 变量（含累加）。

生成：ModelXxx<T extends Entity>（默认）或 <T extends EntityDanmakuMob>（--mob）。
用法：
  python gen_models.py <ModelXxx.java>                      # dry-run：打印解析结果，不写文件
  python gen_models.py <ModelXxx.java> --write <out.java>   # 写出模型类
  python gen_models.py <ModelXxx.java> --write <out> --mob  # 生成 EntityDanmakuMob 泛型 + 动画
"""
import re
import sys
import math

# ============================================================
# 表达式求值
# ============================================================

def strip_comments(src):
    src = re.sub(r'/\*.*?\*/', '', src, flags=re.S)
    src = re.sub(r'//[^\n]*', '', src)
    return src


def to_py_expr(java_expr):
    """把 Java float 表达式转成 Python 可 eval 的表达式（不替换变量名）。"""
    s = java_expr
    s = re.sub(r'\(float\)\s*', '', s)
    s = re.sub(r'\(double\)\s*', '', s)
    s = s.replace('MathHelper.cos', 'math.cos')
    s = s.replace('MathHelper.sin', 'math.sin')
    s = s.replace('Math.cos', 'math.cos')
    s = s.replace('Math.sin', 'math.sin')
    s = s.replace('Math.PI', 'math.pi')
    s = re.sub(r'(\d+\.?\d*)[Ff]', r'\1', s)   # 去 float 后缀 0.0F / 3F
    return s


def eval_float(java_expr, env):
    """求值 Java float 表达式，返回 float。失败抛异常。"""
    return float(eval(to_py_expr(java_expr),
                      {'math': math, '__builtins__': {}}, dict(env)))


def fmt(v):
    """float -> 简洁数字字符串（Java float 字面量）。"""
    if abs(v - round(v)) < 1e-6:
        return '%d' % round(v)
    return ('%g' % v)


# ============================================================
# 代码块提取
# ============================================================

def extract_block(src, brace_idx):
    """从 brace_idx（指向 '{'）提取括号平衡的代码块，返回不含最外层 {} 的内容。"""
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


# ============================================================
# 数组 / for 循环展开
# ============================================================

def _expand_for_block(block, var, n, env):
    """展开一个 for(int var=0;var<n;var++) 循环体，返回标准语句列表（env 就地更新）。"""
    lines = []
    for i in range(n):
        for raw in block.split('\n'):
            line = raw.strip()
            if not line or line.startswith(';'):
                continue
            # 1. 数组元素引用 xxx[var] -> xxx_<i>
            line = re.sub(r'\b(\w+)\[\s*%s\s*\]' % re.escape(var), r'\1_%d' % i, line)
            # 2. 裸循环变量 -> 具体值
            line = re.sub(r'\b%s\b' % re.escape(var), str(i), line)
            # 3. 循环内对 env 变量的赋值/累加（angle += pi8），执行而非输出
            m = re.match(r'^(\w+)\s*(=|\+=|-=|\*=|/=)\s*(.+);$', line)
            if m and m.group(1) in env:
                name, op, expr = m.group(1), m.group(2), m.group(3)
                val = eval_float(expr, env)
                if op == '=':
                    env[name] = val
                elif op == '+=':
                    env[name] = env[name] + val
                elif op == '-=':
                    env[name] = env[name] - val
                elif op == '*=':
                    env[name] = env[name] * val
                elif op == '/=':
                    env[name] = env[name] / val
                continue
            # 4. 输出语句前，把 env 变量替换成具体数值
            for name, val in env.items():
                line = re.sub(r'\b%s\b' % re.escape(name), fmt(val), line)
            lines.append(line)
    return lines


def expand(body):
    """把构造器 body 展开成「标准语句列表」：
    - 提取 float 变量声明进 env
    - 展开 for 循环（就地更新 env）
    - 移除数组声明、展开非循环数组元素引用"""
    env = {}

    def repl_float(m):
        try:
            env[m.group(1)] = eval_float(m.group(2), env)
        except Exception:
            pass
        return ''

    body = re.sub(r'\bfloat\s+(\w+)\s*=\s*([^;]+);', repl_float, body)

    # 展开 for 循环
    pattern = re.compile(
        r'for\s*\(\s*int\s+(\w+)\s*=\s*0\s*;\s*\w+\s*<\s*(\d+)\s*;\s*\w+\s*\+\+\s*\)\s*\{')
    out = []
    i = 0
    for m in pattern.finditer(body):
        out.append(body[i:m.start()])
        var, n = m.group(1), int(m.group(2))
        block = extract_block(body, m.end() - 1)
        out.append('\n'.join(_expand_for_block(block, var, n, env)))
        i = m.end() - 1 + len(block) + 2
    out.append(body[i:])
    body = '\n'.join(out)

    # 移除数组声明 xxx = new ModelRenderer[N];
    body = re.sub(r'\b(\w+)\s*=\s*new\s+ModelRenderer\s*\[\s*\d+\s*\]\s*;', '', body)

    # 展开非循环数组元素 xxx[k] -> xxx_k（k 为字面量）
    body = re.sub(r'\b(\w+)\[\s*(\w+)\s*\]', r'\1_\2', body)

    return body


# ============================================================
# 部件解析
# ============================================================

def extract_args(line, method):
    """提取 line 中 method(...) 的参数（括号平衡，兼容 cos/sin 嵌套），返回参数串或 None。"""
    idx = line.find(method + '(')
    if idx < 0:
        return None
    depth = 0
    i = idx + len(method)
    while i < len(line):
        if line[i] == '(':
            depth += 1
        elif line[i] == ')':
            depth -= 1
            if depth == 0:
                return line[idx + len(method) + 1:i]
        i += 1
    return None


def split_args(args):
    """按顶层逗号切分参数（忽略嵌套括号内的逗号）。"""
    parts = []
    depth = 0
    cur = ''
    for ch in args:
        if ch == '(':
            depth += 1
        elif ch == ')':
            depth -= 1
        if ch == ',' and depth == 0:
            parts.append(cur.strip())
            cur = ''
        else:
            cur += ch
    if cur.strip():
        parts.append(cur.strip())
    return parts


def parse_renderers(body):
    """从展开后的语句解析部件列表。每个部件含 name/texU/texV/boxes/rotPoint/rot/mirror/parent。"""
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
        args = extract_args(line, '.addBox')
        if args is not None:
            # 求值每个参数成数字
            parts = [fmt(eval_float(p, {})) for p in split_args(args)]
            cur['boxes'].append(', '.join(parts))
            continue
        args = extract_args(line, '.setRotationPoint')
        if args is not None:
            cur['rotPoint'] = ', '.join(fmt(eval_float(p, {})) for p in split_args(args))
            continue
        m = re.search(r'\.rotateAngle([XYZ])\s*=\s*([^;]+)', line)
        if m:
            axis = {'X': 0, 'Y': 1, 'Z': 2}[m.group(1)]
            if cur['rot'] is None:
                cur['rot'] = ['0', '0', '0']
            cur['rot'][axis] = fmt(eval_float(m.group(2), {}))
            continue
        m = re.search(r'setRotation\(\s*\w+\s*,\s*([^)]+)\)', line)
        if m:
            cur['rot'] = [fmt(eval_float(p, {})) for p in m.group(1).split(',')]
            continue
        m = re.search(r'\.mirror\s*=\s*true', line)
        if m:
            cur['mirror'] = True
            continue
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


def parse_model(path):
    src = open(path, encoding='utf-8').read()
    cls = re.search(r'class\s+(\w+)', src).group(1)
    tw = re.search(r'textureWidth\s*=\s*(\d+)\s*;', src)
    th = re.search(r'textureHeight\s*=\s*(\d+)\s*;', src)
    tw = int(tw.group(1)) if tw else 64
    th = int(th.group(1)) if th else 64
    src = strip_comments(src)

    # 构造器 body 走「展开」（数组/循环/表达式求值）
    ctor_body = ''
    m = re.search(r'public\s+' + cls + r'\s*\([^)]*\)\s*\{', src)
    if m:
        ctor_body = expand(extract_block(src, m.end() - 1))

    # getTechneModelData / defaultAddChildSetting 原样保留（living 模型）
    extras = []
    for pat in [r'(?:protected|public)\s+void\s+getTechneModelData\s*\(\s*\)\s*\{',
                r'(?:protected|public)\s+void\s+defaultAddChildSetting\s*\(\s*\)\s*\{']:
        mm = re.search(pat, src)
        if mm:
            extras.append(extract_block(src, mm.end() - 1))
    body = ctor_body + '\n' + '\n'.join(extras)

    return cls, tw, th, parse_renderers(body)


# ============================================================
# 代码生成
# ============================================================

def _parent_chain(renderers, name):
    chain = []
    cur_name = name
    while len(chain) < 20:
        parent = None
        for r in renderers:
            if r['name'] == cur_name:
                parent = r['parent']
                break
        if not parent:
            break
        chain.insert(0, parent)
        cur_name = parent
    return chain


def _gen_create_body_layer(renderers, tw, th, indent=''):
    L = []
    L.append(indent + 'public static LayerDefinition createBodyLayer()')
    L.append(indent + '{')
    L.append(indent + '    MeshDefinition mesh = new MeshDefinition();')
    L.append(indent + '    PartDefinition root = mesh.getRoot();')
    L.append('')
    for r in renderers:
        var = r['name']
        parent_var = r['parent'] if r['parent'] else 'root'
        cb = f"CubeListBuilder.create().texOffs({r['texU']}, {r['texV']})"
        if r['mirror']:
            cb += '.mirror()'
        for box in r['boxes']:
            cb += f'.addBox({box})'
        rp = r['rotPoint'] if r['rotPoint'] else '0, 0, 0'
        rot = r['rot']
        rot_is_zero = rot is None or all(x in ('0', '0.0') for x in rot)
        if rot is not None and not rot_is_zero:
            pose = f'PartPose.offsetAndRotation({rp}, {", ".join(rot)})'
        else:
            pose = f'PartPose.offset({rp})'
        ind = indent + ('    ' if parent_var == 'root' else '    ')
        L.append(f'{ind}PartDefinition {var} = {parent_var}.addOrReplaceChild("{var}",')
        L.append(f'{ind}    {cb},')
        L.append(f'{ind}    {pose});')
    L.append('')
    L.append(indent + f'    return LayerDefinition.create(mesh, {tw}, {th});')
    L.append(indent + '}')
    return '\n'.join(L)


def _anim_part_fields(renderers):
    anim_parts = ['head', 'body', 'rightArm', 'leftArm', 'rightLeg', 'leftLeg',
                  'rightWing', 'leftWing', 'rightWing2', 'leftWing2', 'rightWing3', 'leftWing3',
                  'bipedHead', 'bipedBody', 'bipedRightArm', 'bipedLeftArm',
                  'bipedRightLeg', 'bipedLeftLeg']
    all_names = [r['name'] for r in renderers]
    return [p for p in anim_parts if p in all_names]


def gen_class(cls, tw, th, renderers, entity_type='Entity'):
    """生成模型类。entity_type: 'Entity'（item 静态）或 'EntityDanmakuMob'（living 动画）。"""
    is_mob = entity_type == 'EntityDanmakuMob'
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
    if is_mob:
        L.append('import net.minecraft.util.Mth;')
    L.append('import thKaguyaMod.entity.living.EntityDanmakuMob;')
    L.append('import net.minecraft.world.entity.Entity;')
    L.append('')
    L.append('/** 自动生成自原版 ' + cls + '（Techne → LayerDefinition） */')
    L.append('public class ' + cls + '<T extends ' + entity_type + '> extends EntityModel<T>')
    L.append('{')
    L.append('    private final ModelPart root;')
    present = _anim_part_fields(renderers) if is_mob else []
    for p in present:
        L.append(f'    private final ModelPart {p};')
    L.append('')
    L.append(f'    public {cls}(ModelPart root)')
    L.append('    {')
    L.append('        this.root = root;')
    for p in present:
        chain = _parent_chain(renderers, p)
        expr = 'root' + ''.join(f'.getChild("{c}")' for c in chain) + f'.getChild("{p}")'
        L.append(f'        this.{p} = {expr};')
    L.append('    }')
    L.append('')
    L.append(_gen_create_body_layer(renderers, tw, th))
    L.append('')
    if is_mob:
        # setupAnim
        L.append('    @Override')
        L.append('    public void setupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch)')
        L.append('    {')
        for p in present:
            if 'head' in p.lower():
                L.append(f'        if({p} != null) {{ {p}.yRot = netHeadYaw * (float)Math.PI / 180F; {p}.xRot = headPitch * (float)Math.PI / 180F; }}')
        for p in present:
            if 'wing' in p.lower():
                L.append(f'        if({p} != null) {{ {p}.yRot = Mth.cos(ageInTicks * 1.3F) * (float)Math.PI * 0.25F; }}')
        for p in present:
            if 'arm' in p.lower():
                L.append(f'        if({p} != null) {{ {p}.xRot = Mth.cos(limbSwing) * 0.7F * limbSwingAmount; }}')
        for p in present:
            if 'leg' in p.lower():
                L.append(f'        if({p} != null) {{ {p}.xRot = Mth.cos(limbSwing + (float)Math.PI) * 0.7F * limbSwingAmount; }}')
        L.append('    }')
    else:
        L.append('    @Override')
        L.append('    public void setupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch)')
        L.append('    {')
        L.append('    }')
    L.append('')
    L.append('    @Override')
    L.append('    public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha)')
    L.append('    {')
    L.append('        root.render(poseStack, buffer, packedLight, packedOverlay, red, green, blue, alpha);')
    L.append('    }')
    L.append('}')
    return '\n'.join(L)


# ============================================================
# main
# ============================================================

if __name__ == '__main__':
    args = [a for a in sys.argv[1:]]
    if not args:
        print(__doc__)
        sys.exit(1)

    path = args[0]
    write_path = None
    is_mob = False
    if '--mob' in args:
        is_mob = True
    if '--write' in args:
        wi = args.index('--write')
        write_path = args[wi + 1]

    cls, tw, th, renderers = parse_model(path)
    entity_type = 'EntityDanmakuMob' if is_mob else 'Entity'

    print(f'== {cls}  texture {tw}x{th}  {len(renderers)} parts ==')
    for r in renderers:
        boxes = ' ; '.join(r['boxes']) if r['boxes'] else '(无 box)'
        rot = (', '.join(r['rot']) if r['rot'] else '-')
        rp = r['rotPoint'] if r['rotPoint'] else '-'
        par = r['parent'] or '-'
        mir = 'M' if r['mirror'] else ''
        print(f'  [{r["name"]}] tex({r["texU"]},{r["texV"]}){mir} parent={par} rotPoint=({rp}) rot=({rot})')
        print(f'        box: {boxes}')

    if write_path:
        code = gen_class(cls, tw, th, renderers, entity_type)
        open(write_path, 'w', encoding='utf-8', newline='\n').write(code)
        print(f'written {write_path}')
