/*
 * danmaku-core.js — THShotLib 数学层 + 物理层 + 代码生成（无依赖，浏览器 / Node 双用）
 *
 * 逐行对照来源（1.20.1 移植版 / 1.21.1 移植版同名文件）：
 *   thKaguyaMod/THShotLib.java        createShot / createWideShot / createCircleShot /
 *                                     createRingShot / createRandomRingShot / createSphereShot /
 *                                     createLaserA / createLaserB / createWideLaserA /
 *                                     createCircleLaserA / createRingLaserA / createSphereLaserA /
 *                                     createRandomRingLaser / createNuclearShot /
 *                                     danmakuRemove / explosionEffect
 *   thKaguyaMod/entity/shot/EntityTHShot.java  tick() / shotAcceleration()
 *   thKaguyaMod/DanmakuConstants.java          FORM / SIZE / DAMAGE / 颜色
 *
 * 不做的事（原文明说）：
 *   - specials 状态机（§7 ISpecialShot 需要世界实例）→ 只做 JSON 原样透传，Java 导出不含
 *   - 命中判定 hitCheck()
 *   - 渲染层（RenderTHShot）的混合模式 → 预览用简化 sprite
 */
(function (root) {
  'use strict';

  const DEG = Math.PI / 180;

  /* ---------------- 向量（对齐 net.minecraft.world.phys.Vec3 用到的部分） ---------------- */
  const v = (x, y, z) => ({ x, y, z });
  const vadd = (a, b) => v(a.x + b.x, a.y + b.y, a.z + b.z);
  const vsub = (a, b) => v(a.x - b.x, a.y - b.y, a.z - b.z);
  const vscale = (a, s) => v(a.x * s, a.y * s, a.z * s);
  const vlen = a => Math.sqrt(a.x * a.x + a.y * a.y + a.z * a.z);
  const vdot = (a, b) => a.x * b.x + a.y * b.y + a.z * b.z;
  const vcross = (a, b) => v(a.y * b.z - a.z * b.y, a.z * b.x - a.x * b.z, a.x * b.y - a.y * b.x);
  function vnorm(a) {
    const l = vlen(a);
    if (l < 1e-4) return v(0, 0, 0);
    const k = 1 / l;
    return v(a.x * k, a.y * k, a.z * k);
  }
  // 与 a 垂直的单位向量（优先用世界 Y 轴做叉积，退化时换 X 轴）
  function perpendicular(a) {
    const u = Math.abs(a.y) < 0.9 ? v(0, 1, 0) : v(1, 0, 0);
    return vnorm(vcross(u, a));
  }

  /* ---------------- THShotLib 角度 / 旋转（逐行） ---------------- */
  // THShotLib.getVecFromAngle(yaw,pitch,force)  :376-390
  function fromAngle(yaw, pitch, force) {
    const f = force === undefined ? 1 : force;
    const yr = yaw * DEG, pr = pitch * DEG;
    return v(-Math.sin(yr) * Math.cos(pr) * f, -Math.sin(pr) * f, Math.cos(yr) * Math.cos(pr) * f);
  }
  const yawOf = (x, z) => Math.atan2(x, z) / DEG;                              // :393
  const pitchOf = (x, y, z) => Math.atan2(y, Math.sqrt(x * x + z * z)) / DEG;  // :399

  // THShotLib.getVectorFromRotation(axis, vec, angle) :440-456（罗德里格斯公式，逐项照抄）
  function rotateAround(axis, vec, deg) {
    const rx = axis.x, ry = axis.y, rz = axis.z;
    const ax = vec.x, ay = vec.y, az = vec.z;
    const a = deg * DEG, s = Math.sin(a), c = Math.cos(a), k = 1 - c;
    return v(
      (rx * rx * k + c) * ax + (rx * ry * k - rz * s) * ay + (rz * rx * k + ry * s) * az,
      (rx * ry * k + rz * s) * ax + (ry * ry * k + c) * ay + (ry * rz * k - rx * s) * az,
      (rz * rx * k - ry * s) * ax + (ry * rz * k + rx * s) * ay + (rz * rz * k + c) * az
    );
  }

  const rotateDefault = () => v(0, 1, 0);                                       // :170
  const gravityZero = () => v(0, 0, 0);
  const GRAVITY_DEFAULT = -0.03;

  /* ---------------- 常量表（DanmakuConstants 1:1） ---------------- */
  const FORMS = {
    SMALL: 0, TINY: 1, MEDIUM: 2, PEARL: 3, CIRCLE: 4, LIGHT: 5, SCALE: 6, BUTTERFLY: 7,
    SMALLSTAR: 8, STAR: 9, RICE: 10, CRYSTAL: 11, HEART: 12, KUNAI: 13, TALISMAN: 14,
    BIGLIGHT: 15, OVAL: 16, FAMILIAR: 17, ARROW: 18,
    PHANTOM: 26, AMULET: 27, KNIFE: 28, WIND: 29, BIG: 30, KISHITU: 31
  };
  const FORM_NAMES = Object.keys(FORMS);
  // DanmakuConstants.SIZE / DAMAGE（下标 = form）
  const SIZE = [0.30, 0.15, 0.50, 0.30, 0.30, 0.30, 0.15, 0.30,
    0.25, 0.50, 0.15, 0.15, 0.50, 0.15, 0.15, 0.60,
    0.50, 1.00, 0.15, 0, 0, 0, 0, 0,
    0, 0, 0, 0.40, 0.30, 1.00, 0.90, 0.90];
  const DAMAGE = [2.4, 1.0, 2.8, 5.0, 2.0, 3.4, 1.6, 2.8,
    1.6, 3.0, 1.2, 1.8, 3.2, 2.6, 1.0, 6.8,
    3.0, 1.0, 3.0, 0, 0, 0, 0, 0,
    0, 0, 0, 5.0, 2.6, 4.0, 5.0, 5.0];

  const COLORS = { RED: 0, BLUE: 1, GREEN: 2, YELLOW: 3, PURPLE: 4, AQUA: 5, ORANGE: 6, WHITE: 7, RANDOM: 8, RAINBOW: 9 };
  // 预览用配色（贴近游戏内 8 色 sheet 的观感）
  const PALETTE = ['#ff4d63', '#4d86ff', '#3fd76a', '#ffe14d', '#b06bff', '#43e0e0', '#ff9b3d', '#ffffff'];
  const PALETTE_BRIGHT = ['#ffd0d6', '#cfdcff', '#c8f7d6', '#fff6c0', '#e6d4ff', '#ccfbf5', '#ffe0c0', '#ffffff'];

  const LASER_FORMS = { NORMAL: 0, GUNGNIR: 1, LAEVATAIN: 2 };
  const LASER_FORM_NAMES = ['LASER_NORMAL', 'LASER_GUNGNIR', 'LASER_LAEVATAIN'];

  // THSpellCard.java:49-141 的原角色常量（生成类 extends THSpellCard，写裸常量名才能编译）
  const USER_NAMES = [
    'OLD', 'REIMU', 'MARISA', 'RUMIA', 'DAIYOUSEI', 'CIRNO', 'MEIRIN', 'KOAKUMA', 'PATCHOULI', 'SAKUYA',
    'REMILIA', 'FLANDORE', 'LETTY', 'CHEN', 'ALICE', 'LILY', 'LUNASA', 'MERLIN', 'LYRICA', 'YOUMU',
    'YUYUKO', 'RAN', 'YUKARI', 'SUIKA', 'WRIGGLE', 'MYSTIA', 'KEINE', 'TEI', 'UDONGEIN', 'EIRIN',
    'KAGUYA', 'MOKOU', 'AYA', 'MEDICINE', 'YUUKA', 'KOMACHI', 'SHIKIEIKI', 'SHIZUHA', 'MINORIKO', 'HINA',
    'NITORI', 'MOMIZI', 'SANAE', 'KANAKO', 'SUWAKO', 'IKU', 'TENSHI', 'KISUME', 'YAMAME', 'PARSEE',
    'YUUGI', 'SATORI', 'RIN', 'UTUHO', 'KOISHI', 'NAZRIN', 'KOGASA', 'ICHIRIN', 'MINAMITU', 'SHOU',
    'BYAKUREN', 'NUE', 'HATATE', 'KYOUKO', 'YOSHIKA', 'SEIGA', 'TOZIKO', 'FUTO', 'MIKO', 'MAMIZOU',
    'KOKORO', 'WAKASAGIHIME', 'SEKIBANKI', 'KAGEROU', 'BENBEN', 'YATUHASHI', 'SEIJA', 'SHINMYOUMARU',
    'RAIKO', 'RINNOSUKE', 'TOKIKO', 'SUNNY', 'LUNAR', 'STAR', 'AKYUU', 'TOYOHIME', 'YORIHIME', 'REISEN',
    'KASEN', 'KOSUZU', 'RENKO', 'MAERIBERRY', 'OTHER'
  ];

  // THShotLib.ways[][] :459-510 —— 球面弹的 45 行配比表（sphere 的 way 上限就是它的长度）
  const WAYS = [
    [0], [1], [1, 1], [1, 0, 2, 0], [1, 0, 3, 0], [1, 3, 1], [1, 4, 1], [1, 5, 1],
    [1, 3, 3, 1], [1, 5, 3, 0], [1, 4, 4, 1],
    [1, 6, 4, 0], [1, 5, 5, 1], [1, 3, 5, 3, 1], [1, 3, 6, 3, 1], [1, 4, 5, 4, 1],
    [1, 4, 6, 4, 1], [1, 4, 7, 4, 1], [1, 3, 5, 5, 3, 1], [1, 3, 6, 6, 3], [1, 3, 6, 6, 3, 1],
    [1, 5, 9, 5, 1], [1, 3, 7, 7, 3, 1], [1, 6, 9, 6, 1], [1, 4, 7, 7, 4, 1], [1, 3, 5, 7, 5, 3, 1],
    [1, 3, 5, 8, 5, 3, 1], [1, 3, 5, 9, 5, 3, 1], [1, 3, 6, 8, 6, 3, 1], [1, 3, 6, 9, 6, 3, 1],
    [1, 4, 6, 8, 6, 4, 1],
    [1, 3, 6, 11, 6, 3, 1], [1, 3, 6, 12, 6, 3, 1], [0], [0], [0], [0], [0], [0], [0], [0],
    [0], [0], [0], [0], [1, 3, 6, 12, 12, 6, 3, 1]
  ];

  /* ---------------- op 表（与 符卡JSON设计文档.md §6 一致） ---------------- */
  // batch: 'spread' / 'spray' / null；data: 'shot' / 'laser' / null（动作类 op）
  const OP_INFO = {
    shot: { label: '单发弹', batch: null, data: 'shot', motion: true },
    wide: { label: '扇形弹', batch: 'spread', data: 'shot', motion: true, distance: true },
    circle: { label: '整圈弹', batch: 'spray', data: 'shot', motion: true, distance: true },
    ring: { label: '环形弹', batch: 'spray', data: 'shot', motion: true, distance: true, ringSpan: true },
    sphere: { label: '球面弹', batch: 'spray', data: 'shot', motion: true, distance: true, slope: true },
    random_ring: { label: '随机环弹', batch: null, data: 'shot', motion: true, distance: true, random: true },
    nuclear: { label: '核弹（单发自转）', batch: null, data: 'shot', motion: true, nuclear: true },
    laser_a: { label: '激光 A（直线）', batch: null, data: 'laser', motion: true },
    laser_b: { label: '激光 B（附着自转）', batch: 'spray', data: 'laser', attach: true },
    wide_laser: { label: '扇形激光', batch: 'spread', data: 'laser', distance: true },
    circle_laser: { label: '整圈激光', batch: 'spray', data: 'laser', distance: true },
    ring_laser: { label: '环形激光', batch: 'spray', data: 'laser', distance: true, ringSpan: true },
    sphere_laser: { label: '球面激光', batch: 'spray', data: 'laser', distance: true, slope: true },
    random_ring_laser: { label: '随机环激光', batch: null, data: 'laser', distance: true, random: true },
    remove_danmaku: { label: '清除弹幕', batch: null, data: null, action: true },
    explosion_effect: { label: '爆炸效果', batch: null, data: null, action: true },
    spawn: { label: '生成实体', batch: null, data: null, action: true },
    sound: { label: '播放音效', batch: null, data: null, action: true }
  };
  const OPS = Object.keys(OP_INFO);

  /* ---------------- 表达式（安全求值 + Java 反生成，不用 eval） ---------------- */
  const FUNCS = {
    sin: d => Math.sin(d * DEG), cos: d => Math.cos(d * DEG), tan: d => Math.tan(d * DEG),
    sqrt: Math.sqrt, abs: Math.abs, floor: Math.floor, ceil: Math.ceil, round: Math.round,
    min: Math.min, max: Math.max, pow: Math.pow,
    yawOf: (x, z) => Math.atan2(x, z) / DEG,
    pitchOf: (x, y, z) => Math.atan2(y, Math.sqrt(x * x + z * z)) / DEG
  };
  // 随机数：仿真里由 env.rand 提供（确定性），缺省给一个固定实现避免 NaN
  const DFLT_RAND = () => 0.5;

  // 常量别名：FORM_STAR → 9、LASER_GUNGNIR → 1、COLOR_RED → 0（设计文档 §5.4）
  const CONST_ALIASES = (function () {
    const m = {};
    for (const k in FORMS) m['FORM_' + k] = FORMS[k];
    for (const k in LASER_FORMS) { m['LASER_' + k] = LASER_FORMS[k]; m['LASERFORM_' + k] = LASER_FORMS[k]; }
    for (const k in COLORS) m['COLOR_' + k] = COLORS[k];
    for (const k in FORMS) m['F_' + k] = FORMS[k];
    return m;
  })();

  function parseExpr(src) {
    if (src === null || src === undefined || src === '') return { t: 'num', v: 0 };
    if (typeof src === 'number') return { t: 'num', v: src };
    if (typeof src === 'boolean') return { t: 'num', v: src ? 1 : 0 };
    const s = String(src).trim();
    if (/^[-+]?\d+(\.\d+)?$/.test(s)) return { t: 'num', v: parseFloat(s) };
    let i = 0;
    const skip = () => { while (i < s.length && /\s/.test(s[i])) i++; };
    const peek = () => { skip(); return s[i]; };
    // 比较 / 逻辑（供 guard 条件使用，例如 "time % 30 < 10 && time % 2 == 1"）
    function or() {
      let n = and();
      for (;;) { skip(); if (s.slice(i, i + 2) === '||') { i += 2; n = { t: 'bin', o: '||', a: n, b: and() }; } else return n; }
    }
    function and() {
      let n = cmp();
      for (;;) { skip(); if (s.slice(i, i + 2) === '&&') { i += 2; n = { t: 'bin', o: '&&', a: n, b: cmp() }; } else return n; }
    }
    function cmp() {
      const n = add();
      skip();
      for (const op of ['<=', '>=', '==', '!=', '<', '>']) {
        if (s.slice(i, i + op.length) === op) { i += op.length; return { t: 'bin', o: op, a: n, b: add() }; }
      }
      return n;
    }
    function add() {
      let n = mul();
      for (;;) {
        const c = peek();
        if (c === '+') { i++; n = { t: 'bin', o: '+', a: n, b: mul() }; }
        else if (c === '-') { i++; n = { t: 'bin', o: '-', a: n, b: mul() }; }
        else return n;
      }
    }
    function mul() {
      let n = pow();
      for (;;) {
        const c = peek();
        if (c === '*') { i++; n = { t: 'bin', o: '*', a: n, b: pow() }; }
        else if (c === '/') { i++; n = { t: 'bin', o: '/', a: n, b: pow() }; }
        else if (c === '%') { i++; n = { t: 'bin', o: '%', a: n, b: pow() }; }
        else return n;
      }
    }
    // 幂：右结合，优先级高于乘除、低于一元负号（-x^2 === -(x^2)）
    function pow() {
      const n = unary();
      skip();
      if (s[i] === '^') { i++; return { t: 'bin', o: '^', a: n, b: pow() }; }
      return n;
    }
    function unary() {
      const c = peek();
      if (c === '-') { i++; return { t: 'neg', a: unary() }; }
      if (c === '+') { i++; return unary(); }
      return atom();
    }
    function atom() {
      skip();
      if (peek() === '(') { i++; const n = or(); skip(); if (s[i] === ')') i++; return n; }
      // 标识符允许前导 @（表值引用写法 @SIZE[...]）与点分名（look.x）
      const id = /^@?[A-Za-z_][A-Za-z_0-9]*(?:\.[A-Za-z_][A-Za-z_0-9]*)*/.exec(s.slice(i));
      if (id) {
        i += id[0].length;
        const raw = id[0];
        const name = raw.charAt(0) === '@' ? raw.slice(1) : raw;
        if (peek() === '[') {
          // 表值引用：SIZE[9] / DAMAGE[FORM_RICE] / @SIZE[FORM_TINY]（设计文档 §5.4）
          i++;
          const inner = add();
          if (peek() === ']') i++;
          return { t: 'call', f: name, args: [inner] };
        }
        if (peek() === '(') {
          i++;
          const args = [];
          if (peek() !== ')') { args.push(add()); while (peek() === ',') { i++; args.push(add()); } }
          if (peek() === ')') i++;
          return { t: 'call', f: name, args };
        }
        if (CONST_ALIASES[name] !== undefined) return { t: 'num', v: CONST_ALIASES[name] };
        return { t: 'var', n: name };
      }
      const nu = /^\d+(\.\d+)?/.exec(s.slice(i));
      if (nu) { i += nu[0].length; return { t: 'num', v: parseFloat(nu[0]) }; }
      throw new Error('表达式无法解析：「' + s + '」位置 ' + i);
    }
    const out = or();
    skip();
    if (i < s.length) throw new Error('表达式有多余字符：「' + s.slice(i) + '」');
    return out;
  }

  function evalAst(n, env) {
    switch (n.t) {
      case 'num': return n.v;
      case 'var': {
        const nm = n.n;
        if (nm === 'pi') return Math.PI;
        if (nm === 'e') return Math.E;
        if (nm === 'tick' && 'time' in env) return env.time;
        if (nm in env) return env[nm];
        // card.vars 是「懒求值宏」：用到时才按当前 env 求（所以 a = time*6 + i*(360/7) 能拿到逐发的 i）
        if (env.$vars && Object.prototype.hasOwnProperty.call(env.$vars, nm)) {
          const depth = (env.$depth || 0) + 1;
          if (depth > 24) throw new Error('vars 循环引用：' + nm);
          const sub = Object.create(env);
          sub.$depth = depth;
          return evalAst((env.$vars[nm] instanceof Expr ? env.$vars[nm].ast : parseExpr(env.$vars[nm])), sub);
        }
        throw new Error('未知变量 ' + nm);
      }
      case 'neg': return -evalAst(n.a, env);
      case 'bin': {
        const a = evalAst(n.a, env), b = evalAst(n.b, env);
        switch (n.o) {
          case '+': return a + b;
          case '-': return a - b;
          case '*': return a * b;
          case '/': return a / b;
          case '%': return a % b;
          case '^': return Math.pow(a, b);
          case '<': return a < b;
          case '<=': return a <= b;
          case '>': return a > b;
          case '>=': return a >= b;
          case '==': return a === b;
          case '!=': return a !== b;
          case '&&': return !!a && !!b;
          case '||': return !!a || !!b;
        }
        throw new Error('未知运算符 ' + n.o);
      }
      case 'call': {
        const nm = n.f;
        if (nm === 'rand') return (env.rand || DFLT_RAND)();
        if (nm === 'randInt') {
          const mx = evalAst(n.args[0], env) | 0;
          return mx > 0 ? Math.min(mx - 1, ((env.rand || DFLT_RAND)() * mx) | 0) : 0;
        }
        const f = FUNCS[nm];
        if (f) return f.apply(null, n.args.map(x => evalAst(x, env)));
        const a0 = evalAst(n.args[0], env) | 0;
        if (nm === 'SIZE') return SIZE[a0 % 32];
        if (nm === 'DAMAGE') return DAMAGE[a0 % 32];
        throw new Error('未知函数 ' + nm + '()');
      }
    }
    throw new Error('非法表达式节点');
  }

  // Java 反生成时需要把 card.vars 展开成宏（vars 在 Java 里没有对应的局部变量）
  let javaVars = null;
  let javaVarDepth = 0;

  function toJava(n) {
    switch (n.t) {
      case 'num': return String(n.v);
      case 'var': {
        const nm = n.n;
        // 先看卡里的 vars 宏：即使宏名撞上内建变量（例如 vars.way = 9），也以宏为准
        if (javaVars && Object.prototype.hasOwnProperty.call(javaVars, nm)) {
          if (javaVarDepth > 24) throw new Error('vars 循环引用：' + nm);
          javaVarDepth++;
          try {
            const inner = javaVars[nm] instanceof Expr ? javaVars[nm].ast : parseExpr(javaVars[nm]);
            return '(' + toJava(inner) + ')';
          } finally { javaVarDepth--; }
        }
        if (nm === 'time' || nm === 'i' || nm === 'way' || nm === 'level') return nm;
        if (nm === 'tick') return 'time';
        if (nm === 'pi') return 'Math.PI';
        if (nm === 'e') return 'Math.E';
        if (nm === 'userYaw' || nm === 'cardYaw') return 'user.getYRot()';
        if (nm === 'userPitch' || nm === 'cardPitch') return 'user.getXRot()';
        if (nm === 'bbHeight') return 'user.getBbHeight()';
        if (nm === 'look.x' || nm === 'look.y' || nm === 'look.z') return 'user.getLookAngle().' + nm.slice(5);
        return nm;
      }
      case 'neg': return '(-' + toJava(n.a) + ')';
      case 'bin': return n.o === '^'
        ? 'Math.pow(' + toJava(n.a) + ', ' + toJava(n.b) + ')'
        : '(' + toJava(n.a) + ' ' + n.o + ' ' + toJava(n.b) + ')';
      case 'call': {
        const a = n.args.map(toJava);
        if (n.f === 'SIZE' || n.f === 'DAMAGE') return n.f + '[(int)(' + a[0] + ') % 32]';
        switch (n.f) {
          case 'sin': return 'Math.sin((' + a[0] + ') * Math.PI / 180.0)';
          case 'cos': return 'Math.cos((' + a[0] + ') * Math.PI / 180.0)';
          case 'tan': return 'Math.tan((' + a[0] + ') * Math.PI / 180.0)';
          case 'sqrt': return 'Math.sqrt(' + a[0] + ')';
          case 'abs': return 'Math.abs(' + a[0] + ')';
          case 'floor': return 'Math.floor(' + a[0] + ')';
          case 'ceil': return 'Math.ceil(' + a[0] + ')';
          case 'round': return 'Math.round(' + a[0] + ')';
          case 'min': return 'Math.min(' + a.join(', ') + ')';
          case 'max': return 'Math.max(' + a.join(', ') + ')';
          case 'pow': return 'Math.pow(' + a[0] + ', ' + a[1] + ')';
          case 'rand': return 'user.getRandom().nextFloat()';
          case 'randInt': return 'user.getRandom().nextInt(Math.max(1, (int)(' + a[0] + ')))';
          case 'yawOf': return '(Math.atan2(' + a[0] + ', ' + a[1] + ') * 180.0 / Math.PI)';
          case 'pitchOf':
            return '(Math.atan2(' + a[1] + ', Math.sqrt(' + a[0] + ' * ' + a[0] + ' + ' + a[2] + ' * ' + a[2] + ')) * 180.0 / Math.PI)';
          default: return a[0];
        }
      }
    }
    return '0';
  }

  // 表达式包装：源串 + 已解析 AST + 是否「常量」（导出 Java 时可直接折成数字）
  function E(src) {
    if (src instanceof Expr) return src;
    const ast = parseExpr(src);
    return new Expr(src, ast);
  }
  function Expr(src, ast) {
    this.src = src instanceof Expr ? src.src : src;
    this.ast = ast || parseExpr(this.src);
    this.isConst = isConstAst(this.ast);
  }
  Expr.prototype.eval = function (env) { return evalAst(this.ast, env || {}); };
  Expr.prototype.java = function () { return toJava(this.ast); };
  function isConstAst(n) {
    if (n.t === 'num') return true;
    if (n.t === 'var') return n.n === 'pi' || n.n === 'e';
    if (n.t === 'neg') return isConstAst(n.a);
    if (n.t === 'bin') return isConstAst(n.a) && isConstAst(n.b);
    if (n.t === 'call') {
      if (n.f === 'rand' || n.f === 'randInt') return false;
      return n.args.every(isConstAst);
    }
    return false;
  }

  // card.vars → { 名字: Expr }（懒求值，见 evalAst 的 $vars 分支）
  function compileVars(vars) {
    const out = {};
    if (!vars || typeof vars !== 'object') return out;
    for (const k in vars) out[k] = vars[k] instanceof Expr ? vars[k] : E(vars[k]);
    return out;
  }

  /* ---------------- 取值工具 ---------------- */
  // 数值取值：number 直取；Expr 求值；字符串当场解析（带缓存）。
  // 注意：早期版本只认 Expr/number，字符串会静默变成 0 —— 编辑器里所有输入都是字符串，
  // 这里必须做完整分派，否则 "time % 4" 这类表达式会悄悄失效。
  const exprCache = new Map();
  const n = (x, env) => {
    if (x instanceof Expr) return x.eval(env);
    if (typeof x === 'number') return x;
    if (typeof x === 'boolean') return x ? 1 : 0;
    if (x === undefined || x === null || x === '') return 0;
    const key = String(x);
    let e = exprCache.get(key);
    if (!e) { e = E(key); exprCache.set(key, e); }
    return e.eval(env);
  };
  const isBlank = x => x === undefined || x === null || x === '';
  // 取第一个有值的（用于「扁平写法 / 嵌套 spray / 嵌套 spread」三种来源）
  function firstOf() {
    for (let i = 0; i < arguments.length; i++) if (!isBlank(arguments[i])) return arguments[i];
    return undefined;
  }
  // 批量参数：优先 spray.xxx / spread.xxx，回退到扁平字段
  function batchVal(ev, key, flatKey, dflt) {
    const a = ev.spray ? ev.spray[key] : undefined;
    const b = ev.spread ? ev.spread[key] : undefined;
    const c = flatKey ? ev[flatKey] : undefined;
    const got = firstOf(a, b, c);
    return got === undefined ? dflt : got;
  }

  /* ---------------- 发射点（anchor，设计文档 §5.3） ---------------- */
  // 预览里把「玩家眼睛」放在原点；user_feet 往下 1.62；card 与 user_eye 同位置（原版符卡实体就在玩家身上）
  const EYE_HEIGHT = 1.62;
  function anchorPos(ev, env, ctx) {
    const a = ev.anchor || 'user_eye';
    if (a === 'user_feet') return v(ctx.userPos.x, ctx.userPos.y - EYE_HEIGHT, ctx.userPos.z);
    if (a === 'target') return v(ctx.targetPos.x, ctx.targetPos.y, ctx.targetPos.z);
    if (a === 'card') return v(ctx.cardPos.x, ctx.cardPos.y, ctx.cardPos.z);
    if (a === 'vec') {
      const o = ev.vec || {};
      return vadd(ctx.userPos, v(n(o.x, env), n(o.y, env), n(o.z, env)));
    }
    return v(ctx.userPos.x, ctx.userPos.y, ctx.userPos.z);
  }
  function anchorJava(ev) {
    const a = ev.anchor || 'user_eye';
    if (a === 'user_feet') return 'new Vec3(user.getX(), user.getY(), user.getZ())';
    if (a === 'target') return 'pos_Target()';
    if (a === 'card') return 'pos_Card()';
    if (a === 'vec') {
      const o = ev.vec || {};
      return 'pos_User().add(' + jstr(o.x) + ', ' + jstr(o.y) + ', ' + jstr(o.z) + ')';
    }
    return 'pos_User()';
  }

  /* ---------------- 事件 → 弹（THShotLib 的 create* 族，逐行复刻） ---------------- */
  function baseSpec(ev, env, pos, angle, rotate) {
    const yaw = yawOf(angle.x, angle.z);
    const pitch = pitchOf(angle.x, angle.y, angle.z);
    const info = OP_INFO[ev.op] || OP_INFO.shot;
    const isLaser = info.data === 'laser';
    const sh = isLaser ? (ev.laser || {}) : (ev.shot || {});
    // 名字与数字都接受：RED / FORM_STAR / 9 / "time % 4"
    const colorOf = x => {
      if (typeof x === 'string') {
        const t = x.trim().toUpperCase();
        if (t in COLORS) return COLORS[t];
        if (('COLOR_' + t) in COLORS) return COLORS[t.replace(/^COLOR_/, '')];
      }
      return n(x, env) | 0;
    };
    const formOf = x => {
      if (typeof x === 'string') {
        const t = x.trim().toUpperCase().replace(/^(FORM|LASER|LASERFORM)_/, '');
        if (t in FORMS && (LASER_FORMS[t] === undefined || (x.trim().toUpperCase().indexOf('LASER') !== 0))) return FORMS[t];
        if (t in LASER_FORMS) return LASER_FORMS[t];
        if (t in FORMS) return FORMS[t];
      }
      return n(x, env) | 0;
    };
    const form = formOf(isBlank(sh.form) ? 0 : sh.form);
    // 未指定 rotate 时 = fromAngle(yaw, pitch+90)（createShot 的 :583）
    let rot = rotate;
    if (!rot && ev.rotate && !isBlank(ev.rotate.x)) rot = vnorm(v(n(ev.rotate.x, env), n(ev.rotate.y, env), n(ev.rotate.z, env)));
    if (!rot) rot = fromAngle(yaw, pitch + 90, 1);

    const firstSpeed = n(ev.firstSpeed, env);
    const limitSpeed = isBlank(ev.limitSpeed) ? firstSpeed : n(ev.limitSpeed, env);
    const spin = firstOf(ev.spin, ev.rotSpeed, ev.rotationSpeed);
    const spinEnd = firstOf(ev.spinEnd, ev.rotEnd, ev.rotationEnd);

    return {
      kind: isLaser ? 'laser' : 'shot',
      op: ev.op,
      pos: pos,
      angle: angle,
      rotate: rot,
      rotSpeed: spin === undefined ? 0 : n(spin, env),
      rotEnd: spinEnd === undefined ? 9999 : n(spinEnd, env),
      firstSpeed: firstSpeed,
      limitSpeed: limitSpeed,
      accel: n(ev.accel, env),
      gravity: ev.gravity && typeof ev.gravity === 'object'
        ? v(n(ev.gravity.x, env), n(ev.gravity.y, env), n(ev.gravity.z, env))
        : v(n(ev.gx, env), n(ev.gy, env), n(ev.gz, env)),
      form: form,
      color: colorOf(isBlank(sh.color) ? 0 : sh.color),
      size: isBlank(sh.size) ? SIZE[Math.abs(form) % 32] : n(sh.size, env),
      damage: isBlank(sh.damage) ? DAMAGE[Math.abs(form) % 32] : n(sh.damage, env),
      delay: n(sh.delay, env) | 0,
      end: isBlank(sh.end) ? 80 : n(sh.end, env) | 0,
      special: isBlank(sh.special) ? 0 : (typeof sh.special === 'string' && !/^[-+]?\d+$/.test(sh.special.trim()) ? sh.special : n(sh.special, env) | 0),
      laserForm: isBlank((ev.laser || {}).form) ? 0 : (typeof ev.laser.form === 'string' && !/^[-+]?\d+$/.test(ev.laser.form.trim())
        ? (LASER_FORMS[ev.laser.form.trim().toUpperCase().replace(/^LASER_/, '')] | 0)
        : n(ev.laser.form, env) | 0),
      width: n((ev.laser || {}).width, env) || 0.1,
      length: isBlank((ev.laser || {}).length) ? 2.0 : n(ev.laser.length, env),
      slope: isBlank(ev.slope) ? 0 : n(ev.slope, env)
    };
  }

  // createWideShot 核心 :658-716
  function wideShots(ev, env, out, basePos, baseAngle) {
    const way = Math.max(2, n(batchVal(ev, 'way', 'way', 2), env) | 0);
    const wideAngle = n(firstOf(ev.spread && ev.spread.span, ev.wideAngle, ev.span, 60), env);
    const distance = n(ev.distance, env);
    const baseAngleOffset = n(batchVal(ev, 'base', 'baseAngle', 0), env);
    const yaw = yawOf(baseAngle.x, baseAngle.z);
    const pitch = pitchOf(baseAngle.x, baseAngle.y, baseAngle.z);
    const rotateVec = fromAngle(-yaw, -pitch + 90, 1);   // :668
    let a2 = -wideAngle / 2 + baseAngleOffset;           // :670-672
    const span = wideAngle / (way - 1);
    for (let i = 0; i < way; i++) {
      env.i = i; env.way = way;
      const ang = rotateAround(rotateVec, baseAngle, a2);
      const p = vadd(basePos, vscale(ang, distance));
      const slope = pitch * Math.sin(a2 * DEG);          // :705
      const spec = baseSpec(ev, env, p, ang, rotateVec);
      spec.slope = slope;
      out.push(spec);
      a2 += span;
    }
  }

  // createCircleShot :844-860 → 转发 wideShot(360 - 360/way)
  function circleShots(ev, env, out, basePos, baseAngle) {
    const way = Math.max(2, n(batchVal(ev, 'way', 'way', 2), env) | 0);
    let ba = n(batchVal(ev, 'base', 'baseAngle', 0), env);
    if (way % 2 === 0) ba += 360 / (way * 2);            // :850-853
    const sub = Object.assign({}, ev, {
      wideAngle: new Expr('' + (360 - 360 / way)),
      baseAngle: new Expr('' + ba)
    });
    wideShots(sub, env, out, basePos, baseAngle);
  }

  // createRingShot :968-1039
  function ringShots(ev, env, out, basePos, baseAngle) {
    const way = Math.max(2, n(batchVal(ev, 'way', 'way', 2), env) | 0);
    const distance = n(ev.distance, env);
    let ringSpan = n(firstOf(ev.spray && ev.spray.span, ev.ringSpan, ev.span, 0), env);
    const ringBaseAngle = n(batchVal(ev, 'base', 'baseAngle', 0), env);
    ringSpan /= 2;
    const yaw = yawOf(-baseAngle.x, baseAngle.z);         // :976
    const pitch = pitchOf(-baseAngle.x, -baseAngle.y, baseAngle.z);
    const baseVec = fromAngle(yaw, pitch + ringSpan, 1);
    const baseOverVec = fromAngle(yaw, pitch + ringSpan + 90, 1);
    const overVec = fromAngle(yaw, pitch + 90, 1);
    let a2 = ringBaseAngle;
    const span = 360 / way;
    for (let i = 0; i < way; i++) {
      env.i = i; env.way = way;
      const moving = rotateAround(baseAngle, baseVec, a2);       // :1022
      const extend = rotateAround(baseAngle, overVec, a2);       // :1023
      const over2 = rotateAround(baseAngle, baseOverVec, a2);    // :1024
      const p = vadd(basePos, vscale(extend, distance));
      const spec = baseSpec(ev, env, p, moving, over2);
      spec.slope = a2;
      out.push(spec);
      a2 += span;
    }
  }

  // createRandomRingShot :1082-1148
  function randomRingShots(ev, env, out, basePos, baseAngle, rand) {
    const num = Math.max(1, n(firstOf(ev.num, ev.way, 1), env) | 0);
    const distance = n(ev.distance, env);
    const ringSpan = n(firstOf(ev.ringSpan, ev.ring_span, ev.spray && ev.spray.span, 360), env);
    const ringMax = ringSpan / 2;
    const yaw = yawOf(-baseAngle.x, baseAngle.z);         // :1090
    const pitch = pitchOf(baseAngle.x, baseAngle.y, baseAngle.z);
    const overVec = fromAngle(-yaw, pitch + 90, 1);
    const axis = v(-baseAngle.x, -baseAngle.y, baseAngle.z);
    for (let i = 0; i < num; i++) {
      env.i = i; env.way = num;
      const a2 = rand() * 360;
      const baseVec = fromAngle(-yaw, pitch + rand() * ringMax, 1);
      const moving = rotateAround(axis, baseVec, a2);
      const extend = rotateAround(axis, overVec, a2);
      const p = v(basePos.x - extend.x * distance, basePos.y - extend.y * distance, basePos.z + extend.z * distance);
      const spec = baseSpec(ev, env, p, v(-moving.x, -moving.y, moving.z), baseAngle);
      spec.slope = a2;
      out.push(spec);
    }
  }

  // createSphereShot :1211-1277（含 ways 配比表；way ≥ 45 直接 return）
  function sphereShots(ev, env, out, basePos, baseAngle) {
    const way = n(batchVal(ev, 'way', 'way', 0), env) | 0;
    if (way >= WAYS.length || !WAYS[way] || WAYS[way].length === 0) {
      if (ev.op === 'sphere_laser' || ev.op === 'sphere') return;
      return;
    }
    const distance = n(ev.distance, env);
    const angleZ = n(ev.slope, env);
    const baseAngleOff = n(batchVal(ev, 'base', 'baseAngle', 0), env) + angleZ;
    const frontPos = vadd(basePos, vscale(baseAngle, distance));
    const backPos = vsub(basePos, vscale(baseAngle, distance));
    const row = WAYS[way];
    let angleBase = 0, angleSpan = 0;
    if (row.length >= 2) angleSpan = 360 / (row.length - 1);
    let flagFB = false, slopeAcc = 0;
    for (let i = 0; i < row.length; i++) {
      env.i = i; env.way = row.length;
      if (row[i] === 1) {
        if (!flagFB) {
          out.push(baseSpec(ev, env, frontPos, baseAngle, undefined));
          flagFB = true;
        } else {
          out.push(baseSpec(ev, env, backPos, v(-baseAngle.x, -baseAngle.y, -baseAngle.z), undefined));
        }
      } else {
        const sub = Object.assign({}, ev, {
          way: new Expr('' + row[i]),
          baseAngle: new Expr('' + (baseAngleOff + slopeAcc)),
          ringSpan: new Expr('' + angleBase),
          spray: undefined
        });
        ringShots(sub, env, out, basePos, baseAngle);
        slopeAcc += 180 / row.length;
      }
      angleBase += angleSpan;
    }
  }

  // createLaserB :1359 —— 附着在发射者身上的自转激光
  // 每束方向 = 绕视线轴均分（spray{way,span,base}），自转轴 = cross(视线, 该束方向)（getOuterProduct）
  function laserBShots(ev, env, out, basePos, baseAngle) {
    const way = Math.max(1, n(batchVal(ev, 'way', 'way', 1), env) | 0);
    const span = n(firstOf(ev.spray && ev.spray.span, ev.span, 360), env);
    const base = n(batchVal(ev, 'base', 'baseAngle', 0), env);
    const attach = ev.attach || {};
    const offDist = n(attach.length, env);
    const yOff = n(attach.yOffset === undefined ? attach.y_offset : attach.yOffset, env);
    const up = perpendicular(baseAngle);
    const origin = vadd(vadd(basePos, vscale(baseAngle, offDist)), v(0, yOff, 0));
    const step = way > 1 ? span / way : 0;
    for (let i = 0; i < way; i++) {
      env.i = i; env.way = way;
      const dir = way > 1 ? vnorm(rotateAround(baseAngle, up, base + i * step)) : baseAngle;
      const axis = vnorm(vcross(baseAngle, dir));
      const spec = baseSpec(ev, env, origin, dir, vlen(axis) < 0.5 ? up : axis);
      spec.kind = 'laser_attached';
      spec.attach = { to: attach.to || 'user', length: offDist, yOffset: yOff };
      out.push(spec);
    }
  }

  /* ---------------- 事件展开入口 ---------------- */
  function eventAnchorAngle(ev, env, ctx) {
    const aim = ev.aim || 'look';
    if (aim === 'look') return fromAngle(ctx.userYaw, ctx.userPitch, 1);
    if (aim === 'yawpitch') return fromAngle(n(ev.yaw, env), n(ev.pitch, env), 1);
    if (aim === 'target') return vnorm(vsub(ctx.targetPos, ctx.userPos));
    if (aim === 'dir') {
      const o = ev.dir || {};
      return vnorm(v(n(firstOf(o.x, ev.dx), env), n(firstOf(o.y, ev.dy), env), n(firstOf(o.z, ev.dz), env)));
    }
    return fromAngle(ctx.userYaw, ctx.userPitch, 1);
  }

  /**
   * 展开一个事件，把结果通过 emit 抛出。
   * emit 收到的对象 kind 可能是：
   *   'shot' / 'laser' / 'laser_attached'  —— 弹或激光
   *   'action'                             —— spawn / remove_danmaku / explosion_effect / sound
   */
  function expandEvent(ev, env, ctx, emit) {
    const op = ev.op || 'shot';
    const info = OP_INFO[op];
    if (!info) throw new Error('未知 op：' + op);
    const repeat = Math.max(1, n(isBlank(ev.repeat) ? 1 : ev.repeat, env) | 0);

    // 动作类 op：不产生弹
    if (info.action) {
      const act = { kind: 'action', op: op, tick: env.time };
      if (op === 'spawn') act.entity = ev.entity || '';
      if (op === 'remove_danmaku') { act.range = n(ev.range, env); act.target = ev.target || 'All'; act.dropBonus = ev.dropBonus === undefined ? (ev.drop_bonus !== undefined ? ev.drop_bonus : false) : ev.dropBonus; }
      if (op === 'explosion_effect') act.size = n(ev.size, env);
      for (let r = 0; r < repeat; r++) { env.r = r; env.i = r; emit(act); }
      return;
    }

    // 单发类 op：每次 repeat 都要重算（i = 重复序号，原卡里就是 for(i) 里的 i）
    if (op === 'shot' || op === 'laser_a' || op === 'nuclear') {
      for (let r = 0; r < repeat; r++) {
        env.r = r; env.i = r; env.way = 1;
        const pos = anchorPos(ev, env, ctx);
        const angle = eventAnchorAngle(ev, env, ctx);
        emit(baseSpec(ev, env, pos, angle, undefined));
      }
      return;
    }

    // 批量类 op：先展开成一批（i = 批内序号），再整体重复 repeat 次
    const pos = anchorPos(ev, env, ctx);
    const angle = eventAnchorAngle(ev, env, ctx);
    const out = [];
    switch (op) {
      case 'wide':
      case 'wide_laser':
        wideShots(ev, env, out, pos, angle);
        break;
      case 'circle':
      case 'circle_laser':
        circleShots(ev, env, out, pos, angle);
        break;
      case 'ring':
      case 'ring_laser':
        ringShots(ev, env, out, pos, angle);
        break;
      case 'random_ring':
      case 'random_ring_laser':
        randomRingShots(ev, env, out, pos, angle, ctx.rand);
        break;
      case 'sphere':
      case 'sphere_laser':
        sphereShots(ev, env, out, pos, angle);
        break;
      case 'laser_b':
        laserBShots(ev, env, out, pos, angle);
        break;
      default:
        throw new Error('未知 op：' + op);
    }
    for (let r = 0; r < repeat; r++) {
      env.r = r;
      for (let i = 0; i < out.length; i++) emit(out[i]);
    }
  }

  /* ---------------- 时间轴条件 ---------------- */
  function whenMatch(when, t) {
    if (!when) return t === 0;
    const k = when.kind || 'at';
    if (k === 'at') {
      const a = when.a;
      if (Array.isArray(a)) return a.some(x => (Number(x) | 0) === t);
      return t === (Number(a) | 0);
    }
    if (k === 'range') return t >= (Number(when.a) | 0) && t < (Number(when.b) | 0);
    if (k === 'every') {
      const every = Math.max(1, Number(when.a) | 0);
      return t % every === 0 && (isBlank(when.b) || t < (Number(when.b) | 0));
    }
    return false;
  }

  function whenJava(when) {
    if (!when || (when.kind || 'at') === 'at') {
      const a = when ? when.a : 0;
      if (Array.isArray(a)) return '(' + a.map(x => 'time == ' + (Number(x) | 0)).join(' || ') + ')';
      return 'time == ' + ((Number(a) | 0));
    }
    if (when.kind === 'range') return 'time >= ' + (Number(when.a) | 0) + ' && time < ' + (Number(when.b) | 0);
    if (when.kind === 'every') {
      let s = 'time % ' + Math.max(1, Number(when.a) | 0) + ' == 0';
      if (!isBlank(when.b)) s += ' && time < ' + (Number(when.b) | 0);
      return s;
    }
    return 'time == 0';
  }

  /* ---------------- 物理：EntityTHShot.tick() :282-370 ---------------- */
  function makeShot(spec, tick) {
    const motion = vscale(spec.angle, spec.firstSpeed);
    return {
      born: tick, alive: true,
      pos: v(spec.pos.x, spec.pos.y, spec.pos.z),
      angle: v(spec.angle.x, spec.angle.y, spec.angle.z),
      rotate: v(spec.rotate.x, spec.rotate.y, spec.rotate.z),
      rotSpeed: spec.rotSpeed, rotEnd: spec.rotEnd,
      limit: spec.limitSpeed, accel: spec.accel,
      gravity: v(spec.gravity.x, spec.gravity.y, spec.gravity.z),
      lm: v(motion.x, motion.y, motion.z),       // lastShotMotion
      lastTime: 0,
      anim: -spec.delay,                          // setAnimationCount(-delay)
      end: spec.end, form: spec.form, color: spec.color,
      size: spec.size, damage: spec.damage, special: spec.special,
      laserForm: spec.laserForm,
      slope: spec.slope === undefined ? 0 : spec.slope,
      delay: spec.delay,
      kind: spec.kind, width: spec.width, length: spec.length,
      op: spec.op, attach: spec.attach || null,
      px: [spec.pos.x], py: [spec.pos.y], pz: [spec.pos.z],
      ay: [spec.angle.x], ap: [spec.angle.y], az: [spec.angle.z]
    };
  }

  function stepShot(s, age) {
    if (!s.alive) return;
    // :304  消滅判定（anim 用上一 tick 的值，与原版顺序一致）
    if (s.anim > s.end) { s.alive = false; return; }
    // :311-322  延迟阶段只倒数不移动
    if (s.anim >= 0) s.anim = age;
    else { s.anim++; return; }

    // 附着型激光（laser_b）：位置固定在被附着物上，只有方向在转
    if (s.kind === 'laser_attached') {
      if (s.rotSpeed !== 0) s.angle = rotateAround(s.rotate, s.angle, s.rotSpeed);
      if (s.rotEnd < s.lastTime) s.rotSpeed = 0;
      if (age > s.lastTime) s.lastTime = age;
      return;
    }

    let motion = v(s.lm.x, s.lm.y, s.lm.z);                 // :334
    if (s.rotSpeed !== 0) s.angle = rotateAround(s.rotate, s.angle, s.rotSpeed);  // :337-340

    // shotAcceleration() :372-396
    const speed = vlen(motion);
    if (s.accel > 0 && speed < s.limit) {
      motion = vadd(motion, vscale(s.angle, s.accel));
      if (vlen(motion) > s.limit) motion = vscale(s.angle, s.limit);
    } else if (s.accel < 0 && speed > s.limit) {
      if (Math.abs(s.accel) > speed - s.limit) motion = vscale(s.angle, s.limit);
      else motion = vadd(motion, vscale(s.angle, s.accel));
    }

    // specialMotion：只复刻纯数学的一项（special 30 = 下坠），其余在预览里退化为直线
    if (s.special === 30) motion = vadd(motion, v(0, -0.02, 0));

    motion = vadd(motion, s.gravity);                       // :345-347
    if (s.rotEnd < s.lastTime) s.rotSpeed = 0;              // :352-355
    s.pos = vadd(s.pos, motion);                            // :357-361

    s.lm = motion;
    if (age > s.lastTime) s.lastTime = age;                 // :363-369
  }

  /* ---------------- 仿真 ---------------- */
  function mulberry32(a) {
    return function () {
      a |= 0; a = a + 0x6D2B79F5 | 0;
      let t = Math.imul(a ^ a >>> 15, 1 | a);
      t = t + Math.imul(t ^ t >>> 7, 61 | t) ^ t;
      return ((t ^ t >>> 14) >>> 0) / 4294967296;
    };
  }

  /**
   * card 的内部模型（camelCase）：
   * {
   *   name, nameEn, slug, id, needLevel, endTime, removeTime, originalUser, circleColor,
   *   maxTick, userYaw, userPitch, level, notes, vars, specials,
   *   events: [ {
   *     when:{kind:'at'|'range'|'every', a, b}, op, guard, repeat, sound,
   *     anchor:'user_eye'|'card'|'target'|'user_feet'|'vec'|'default', vec:{x,y,z},
   *     aim:'look'|'target'|'yawpitch'|'dir', yaw, pitch, dir:{x,y,z},
   *     spread:{way,span,base} / spray:{way,span,base},
   *     way, wideAngle, baseAngle, ringSpan, num, distance, slope,
   *     firstSpeed, limitSpeed, accel, gravity:{x,y,z},
   *     rotationSpeed, rotationEnd, spin, spinEnd, rotate:{x,y,z},
   *     attach:{to,length,yOffset},
   *     shot:{form,color,size,damage,delay,end,special},
   *     laser:{form,color,width,length,damage,delay,end},
   *     range, target, dropBonus, entity, size
   *   } ]
   * }
   */
  function simulate(card) {
    const maxTick = Math.max(1, card.maxTick | 0 || 120);
    const errors = [];
    const spawnLog = [];
    const shots = [];
    const actions = [];
    const ctx = {
      userPos: v(0, 0, 0),
      cardPos: v(0, 0, 0),
      targetPos: v(0, 0, 3),
      userYaw: card.userYaw || 0,
      userPitch: card.userPitch || 0,
      rand: mulberry32(20261021)
    };
    const level = card.level === undefined ? 2 : card.level | 0;
    let vars = {};
    try { vars = compileVars(card.vars); } catch (e) {
      errors.push({ tick: 0, op: 'vars', idx: -1, msg: e.message });
    }
    const look = fromAngle(ctx.userYaw, ctx.userPitch, 1);
    const events = Array.isArray(card.events) ? card.events : [];

    for (let t = 0; t <= maxTick; t++) {
      for (let ei = 0; ei < events.length; ei++) {
        const ev = events[ei];
        if (!whenMatch(ev.when, t)) continue;
        const env = {
          time: t, level: level, i: 0, way: 0, r: 0, pi: Math.PI, e: Math.E,
          userYaw: ctx.userYaw, userPitch: ctx.userPitch, bbHeight: 1.8,
          cardYaw: ctx.userYaw, cardPitch: ctx.userPitch,
          'look.x': look.x, 'look.y': look.y, 'look.z': look.z,
          rand: ctx.rand, $vars: vars, $depth: 0
        };
        // 复合守卫：对应原卡里的 "if(time % 30 < 10 && time % 2 == 1)"
        // 注意：不把 Expr 写回 ev.guard —— 那会污染调用方的卡对象（UI 会显示 [object Object]）
        if (!isBlank(ev.guard)) {
          try {
            const g = (ev.guard instanceof Expr) ? ev.guard : E(ev.guard);
            if (!g.eval(env)) continue;
          } catch (e) {
            errors.push({ tick: t, op: ev.op, idx: ei, msg: 'if: ' + e.message });
            continue;
          }
        }
        try {
          let spawned = 0;
          expandEvent(ev, env, ctx, spec => {
            if (spec.kind === 'action') {
              actions.push(Object.assign({ tick: t, idx: ei }, spec));
              if (ev.sound) actions.push({ kind: 'action', op: 'sound', tick: t, idx: ei });
              return;
            }
            // 原版 createShot :528-537：RANDOM 逐发取 0–7，RAINBOW 逐发取 0–6
            if (spec.color === 8) spec.color = (ctx.rand() * 8) | 0;
            else if (spec.color === 9) spec.color = (ctx.rand() * 7) | 0;
            const s = makeShot(spec, t);
            shots.push(s);
            spawned++;
          });
          if (spawned) spawnLog.push({ tick: t, op: ev.op, count: spawned, idx: ei });
          if (ev.sound && spawned) actions.push({ kind: 'action', op: 'sound', tick: t, idx: ei });
        } catch (e) {
          errors.push({ tick: t, op: ev.op, idx: ei, msg: e.message });
        }
      }
      // 2) 已存在的弹推进（born < t，与原版「实体本 tick 创建、下一 tick 才 tick」一致）
      for (const s of shots) {
        if (s.born >= t || !s.alive) continue;
        stepShot(s, t - s.born);
        s.px.push(s.pos.x); s.py.push(s.pos.y); s.pz.push(s.pos.z);
        s.ay.push(s.angle.x); s.ap.push(s.angle.y); s.az.push(s.angle.z);
      }
    }
    return { shots, actions, errors, spawnLog, maxTick, vars };
  }

  function stateAt(sim, tick) {
    const out = [];
    for (const s of sim.shots) {
      const idx = tick - s.born;
      if (idx < 0 || idx >= s.px.length) continue;
      out.push({
        x: s.px[idx], y: s.py[idx], z: s.pz[idx],
        ax: s.ay[idx], ay: s.ap[idx], az: s.az[idx],
        form: s.form, color: s.color, size: s.size, kind: s.kind,
        width: s.width, length: s.length, age: idx, born: s.born,
        vis: s.anim >= 0 || (idx - s.born) >= Math.abs(s.anim) ? 1 : 0,
        op: s.op
      });
    }
    return out;
  }

  /* ---------------- 代码生成：Java ---------------- */
  const fmt = x => {
    if (!isFinite(x)) return '0';
    const r = Math.round(x * 1e6) / 1e6;
    return String(r);
  };
  const jstr = e => {
    if (isBlank(e)) return '0';
    const E2 = e instanceof Expr ? e : E(e);
    return E2.isConst ? fmt(E2.eval({})) : '(' + E2.java() + ')';
  };
  // Java 是强类型的：double 常量不能隐式收窄成 float/int，表达式更不能直接贴 F 后缀。
  const jdouble = e => jstr(e);
  const jfloat = e => {
    if (isBlank(e)) return '0.0F';
    const E2 = e instanceof Expr ? e : E(e);
    return E2.isConst ? fmt(E2.eval({})) + 'F' : '(float)(' + E2.java() + ')';
  };
  const jint = e => {
    if (isBlank(e)) return '0';
    const E2 = e instanceof Expr ? e : E(e);
    return E2.isConst ? String(Math.round(E2.eval({}))) : '(int)(' + E2.java() + ')';
  };
  const jf = jfloat;

  function shotDataJava(sh, isLaser) {
    // 弹型/颜色写成名字时必须带 FORM_ 前缀：生成类 extends THSpellCard，
    // 而 THSpellCard 自己也有一批同名常量（STAR = 134 是角色，不是弹型 9）。
    const javaForm = expr => {
      if (isBlank(expr)) return '0';
      if (typeof expr === 'number') return String(Math.round(expr));
      const s = String(expr).trim();
      if (/^[-+]?\d+$/.test(s)) return String(Number(s));
      const up = s.toUpperCase().replace(/^FORM_/, '');
      if (Object.prototype.hasOwnProperty.call(FORMS, up)) return 'FORM_' + up;
      return jint(s);
    };
    const javaColor = expr => {
      if (isBlank(expr)) return '0';
      if (typeof expr === 'number') return String(Math.round(expr));
      const s = String(expr).trim();
      if (/^[-+]?\d+$/.test(s)) return String(Number(s));
      const up = s.toUpperCase();
      if (Object.prototype.hasOwnProperty.call(COLORS, up)) return up;
      return jint(s);
    };
    const form = javaForm(sh.form);
    const color = javaColor(sh.color);
    const delay = isBlank(sh.delay) ? '0' : jint(sh.delay);
    const end = isBlank(sh.end) ? '80' : jint(sh.end);
    if (isLaser) {
      const width = isBlank(sh.width) ? '0.1F' : jfloat(sh.width);
      const len = isBlank(sh.length) ? '2.0F' : jfloat(sh.length);
      const dmg = isBlank(sh.damage) ? '3.0F' : jfloat(sh.damage);
      let lf = 'LASER_NORMAL';
      if (!isBlank(sh.form)) {
        if (typeof sh.form === 'string' && !/^[-+]?\d+$/.test(sh.form.trim())) {
          const nm = sh.form.trim().toUpperCase().replace(/^LASER_/, '').replace(/^FORM_/, '');
          if (LASER_FORM_NAMES.indexOf(nm) >= 0) lf = 'LASER_' + nm;
        } else {
          const idx = Math.abs(Number(sh.form) | 0) % 3;
          lf = LASER_FORM_NAMES[idx];
        }
      }
      return 'LaserData.laser(' + lf + ', ' + color + ', ' + width + ', ' + len + ', ' + dmg + ', ' + delay + ', ' + end + ', 0)';
    }
    // SIZE[] / DAMAGE[] 本身就是 float，不能再贴 F 后缀
    const size = isBlank(sh.size) ? 'SIZE[' + form + ']' : jfloat(sh.size);
    const dmg = isBlank(sh.damage) ? 'DAMAGE[' + form + ']' : jfloat(sh.damage);
    const spVal = sh.special;
    let sp = '';
    if (!isBlank(spVal)) {
      if (typeof spVal === 'string' && !/^[-+]?\d+$/.test(spVal.trim())) {
        sp = ', 0 /* special: ' + spVal + ' 仅 JSON 解释器支持 */';
      } else {
        const num = Number(spVal);
        if (num) sp = ', ' + num;
      }
    }
    return 'shot(' + form + ', ' + color + ', ' + size + ', ' + dmg + ', ' + delay + ', ' + end + sp + ')';
  }

  function eventJava(ev, idx) {
    const op = ev.op || 'shot';
    const info = OP_INFO[op] || OP_INFO.shot;
    const isLaser = info.data === 'laser';
    const data = shotDataJava(isLaser ? (ev.laser || {}) : (ev.shot || {}), isLaser);
    const fu = s => (isBlank(s) ? '0.0D' : jstr(s));
    const first = fu(ev.firstSpeed);
    const limit = isBlank(ev.limitSpeed) ? first : fu(ev.limitSpeed);
    const accel = fu(ev.accel);
    const g = ev.gravity || {};
    const grav = 'THShotLib.gravity(' + fu(isBlank(g.x) ? ev.gx : g.x) + ', ' + fu(isBlank(g.y) ? ev.gy : g.y) + ', ' + fu(isBlank(g.z) ? ev.gz : g.z) + ')';
    const tag = 'dmk' + (idx === undefined ? 0 : idx);

    let aimVec;
    if ((ev.aim || 'look') === 'look') aimVec = 'user.getLookAngle()';
    else if (ev.aim === 'yawpitch') aimVec = 'THShotLib.getVecFromAngle(' + jf(ev.yaw) + ', ' + jf(ev.pitch) + ')';
    else if (ev.aim === 'target') aimVec = 'angle_UserToTarget()';
    else {
      const o = ev.dir || {};
      aimVec = 'THShotLib.getVectorNomalize(angle(' + fu(firstOf(o.x, ev.dx)) + ', ' + fu(firstOf(o.y, ev.dy)) + ', ' + fu(firstOf(o.z, ev.dz)) + '))';
    }

    const pos = anchorJava(ev);
    // 表达式里的 way = 「本批次数」。生成 Java 时先声明成局部变量，表达式里的 way 才有意义；
    // 若表达式里的 way 既没有 vars 宏也没有先声明，就会自引用，这里按 1 兜底。
    const waySrc = batchVal(ev, 'way', 'way', 1);
    const waySelfRef = /\bway\b/.test(String(waySrc)) && !(javaVars && Object.prototype.hasOwnProperty.call(javaVars, 'way'));
    const wayDecl = 'int way = ' + jint(waySelfRef ? 1 : waySrc) + ';';
    const way = 'way';
    const dist = jdouble(ev.distance);
    const base = jfloat(batchVal(ev, 'base', 'baseAngle', 0));
    const span = jfloat(firstOf(ev.spread && ev.spread.span, ev.spray && ev.spray.span, ev.wideAngle, ev.ringSpan, ev.span, 0));
    const spin = firstOf(ev.spin, ev.rotSpeed, ev.rotationSpeed);
    const spinEnd = firstOf(ev.spinEnd, ev.rotEnd, ev.rotationEnd);
    const rotS = jfloat(spin);
    const rotE = isBlank(spinEnd) ? '9999' : jint(spinEnd);

    const build = () => {
    switch (op) {
      case 'shot':
        return 'THShotLib.createShot(user, ' + pos + ', ' + aimVec + ', ' + first + ', ' + limit + ', ' + accel + ', ' + grav + ', ' + data + ');';
      case 'nuclear':
        return 'THShotLib.createNuclearShot(user, user, ' + pos + ', ' + aimVec + ', ' + jfloat(firstOf(ev.slope, 0)) + ', rotateAxis(' +
          jdouble(firstOf((ev.rotate || {}).x, 0)) + ', ' + jdouble(firstOf((ev.rotate || {}).y, 1)) + ', ' + jdouble(firstOf((ev.rotate || {}).z, 0)) + '), ' +
          rotS + ', ' + rotE + ', ' + first + ', ' + limit + ', ' + accel + ', ' + grav + ', ' + data + ');';
      case 'laser_a':
        return 'THShotLib.createLaserA(user, user, ' + pos + ', ' + aimVec + ', ' + first + ', ' + limit + ', ' + accel + ', ' + grav + ', ' + data + ');';
      case 'laser_b': {
        const at = ev.attach || {};
        const setLen = jdouble(firstOf(at.length, 1.0));
        const setYOff = jdouble(firstOf(at.yOffset, at.y_offset, 0));
        const wayNum = Number(exprOrNum(batchVal(ev, 'way', 'way', 1)));
        const spreadOk = Number.isFinite(wayNum) && wayNum > 1;
        const ang = spreadOk ? base + ' + ' + tag + 'I * (' + span + ' / ' + way + ')' : '0.0F';
        // createLaserB 没有 way 形参：原卡自己 for 循环逐条建（THSC_NonDirectionalLaser :34 同款）
        return [
          'Vec3 ' + tag + 'A = ' + aimVec + ';',
          // 原卡 THSC_MoonlightRay:27 就是用它当旋转轴（垂直于视线）
          'Vec3 ' + tag + 'U = THShotLib.getVecFromAngle(user.getYRot(), user.getXRot() + 90F);',
          'for(int ' + tag + 'I = 0; ' + tag + 'I < ' + way + '; ' + tag + 'I++)',
          '{',
          '    Vec3 ' + tag + 'D = THShotLib.getVectorFromRotation(' + tag + 'A, ' + tag + 'U, ' + ang + ');',
          '    THShotLib.createLaserB(user, user, ' + pos + ', ' + tag + 'D, THShotLib.getOuterProduct(' + tag + 'A, ' + tag + 'D), ' + rotS + ', ' + rotE + ', ' + data + ', user, ' + setLen + ', ' + setYOff + ');',
          '}'
        ].join('\n');
      }
      case 'wide':
        return 'THShotLib.createWideShot(user, user, ' + pos + ', ' + aimVec + ', ' + rotS + ', ' + rotE + ', ' + first + ', ' + limit + ', ' + accel + ', ' + grav + ', ' + data + ', ' + way + ', ' + span + ', ' + dist + ', ' + base + ');';
      case 'wide_laser':
        return 'THShotLib.createWideLaserA(user, user, ' + pos + ', ' + aimVec + ', ' + first + ', ' + limit + ', ' + accel + ', ' + grav + ', ' + data + ', ' + way + ', ' + span + ', ' + dist + ');';
      case 'circle':
        return 'THShotLib.createCircleShot(user, user, ' + pos + ', ' + aimVec + ', ' + rotS + ', ' + rotE + ', ' + first + ', ' + limit + ', ' + accel + ', ' + grav + ', ' + data + ', ' + way + ', ' + dist + ', ' + base + ');';
      case 'circle_laser':
        return 'THShotLib.createCircleLaserA(user, user, ' + pos + ', ' + aimVec + ', ' + first + ', ' + limit + ', ' + accel + ', ' + grav + ', ' + data + ', ' + way + ', ' + dist + ');';
      case 'ring':
        return 'THShotLib.createRingShot(user, user, ' + pos + ', ' + aimVec + ', ' + rotS + ', ' + rotE + ', ' + first + ', ' + limit + ', ' + accel + ', ' + grav + ', ' + data + ', ' + way + ', ' + dist + ', ' + span + ', ' + base + ');';
      case 'ring_laser':
        return 'THShotLib.createRingLaserA(user, user, ' + pos + ', ' + aimVec + ', ' + first + ', ' + limit + ', ' + accel + ', ' + grav + ', ' + data + ', ' + way + ', ' + dist + ', ' + span + ', ' + base + ');';
      case 'random_ring':
        return 'THShotLib.createRandomRingShot(user, user, ' + pos + ', ' + aimVec + ', ' + rotS + ', ' + rotE + ', ' + first + ', ' + limit + ', ' + accel + ', ' + grav + ', ' + data + ', ' + jint(firstOf(ev.num, 1)) + ', ' + dist + ', ' + jfloat(firstOf(ev.ringSpan, ev.ring_span, 360)) + ');';
      case 'random_ring_laser':
        return 'THShotLib.createRandomRingLaser(user, user, ' + pos + ', ' + aimVec + ', ' + first + ', ' + limit + ', ' + accel + ', ' + grav + ', ' + data + ', ' + jint(firstOf(ev.num, 1)) + ', ' + dist + ', ' + jfloat(firstOf(ev.ringSpan, ev.ring_span, 360)) + ');';
      case 'sphere':
        return 'THShotLib.createSphereShot(user, user, ' + pos + ', ' + aimVec + ', ' + jfloat(firstOf(ev.slope, 0)) + ', ' + first + ', ' + limit + ', ' + accel + ', ' + grav + ', ' + data + ', ' + way + ', ' + dist + ', ' + base + ');';
      case 'sphere_laser':
        return 'THShotLib.createSphereLaserA(user, user, ' + pos + ', ' + aimVec + ', ' + first + ', ' + limit + ', ' + accel + ', ' + grav + ', ' + data + ', ' + way + ', ' + dist + ', ' + base + ');';
      case 'remove_danmaku':
        return 'THShotLib.danmakuRemove(user, ' + jdouble(firstOf(ev.range, 4)) + ', "' + (ev.target || 'All') + '", ' + ((ev.dropBonus || ev.drop_bonus) ? 'true' : 'false') + ');';
      case 'explosion_effect':
        return [
          'Vec3 ' + tag + 'P = ' + pos + ';',
          'THShotLib.explosionEffect(world, ' + tag + 'P.x, ' + tag + 'P.y, ' + tag + 'P.z, ' + jfloat(firstOf(ev.size, 2)) + ');'
        ].join('\n');
      case 'spawn': {
        const e = String(ev.entity || '');
        return 'spawnEntity("' + e.replace(/["\\]/g, '\\$&') + '", ' + pos + ');';
      }
      case 'sound':
        return 'THShotLib.playShotSound(user);';
    }
    return '// 未知 op: ' + op;
    };
    let code = build();
    // 只要生成出来的调用里出现了 way（批量 op 都从它取发数），就先声明这个局部变量
    if (/\bway\b/.test(code) && !/\bint way\s*=/.test(code)) code = wayDecl + '\n' + code;
    return code;
  }

  function cardToJava(card) {
    const slug = String(card.slug || '').replace(/[^A-Za-z0-9_]/g, '_');
    const cls = 'THSC_' + (slug || 'Generated');
    const lines = [];
    const hasSpawn = (card.events || []).some(e => (e.op || '') === 'spawn');
    const usesRotateAxis = (card.events || []).some(e => (e.op || '') === 'nuclear');
    const pushCode = (indent, code) => {
      String(code).split('\n').forEach(ln => lines.push(' '.repeat(indent) + ln));
    };

    lines.push('package thKaguyaMod.entity.spellcard;');
    lines.push('');
    lines.push('import net.minecraft.world.phys.Vec3;');
    lines.push('import thKaguyaMod.LaserData;');
    lines.push('import thKaguyaMod.ShotData;');
    lines.push('import thKaguyaMod.THShotLib;');
    lines.push('');
    lines.push('import static thKaguyaMod.DanmakuConstants.*;');
    lines.push('');
    lines.push('/**');
    lines.push(' * ' + (card.name || '生成符卡') + (card.nameEn ? ' / ' + card.nameEn : '') + ' — 由可视化弹幕编辑器导出');
    lines.push(' * 事件数 ' + (card.events || []).length + '，时长 ' + (card.maxTick | 0) + ' tick');
    if (card.notes) {
      String(card.notes).split(/\r?\n/).forEach(t => lines.push(' * ' + t));
    }
    lines.push(' * 注意：导出的是 THShotLib 直接调用；specials 状态机只存在于 JSON 解释器，这里已省略。');
    lines.push(' */');
    lines.push('public class ' + cls + ' extends THSpellCard');
    lines.push('{');
    lines.push('    public ' + cls + '()');
    lines.push('    {');
    lines.push('        this.setNeedLevel(' + (card.needLevel | 0 || 1) + ');');
    lines.push('        this.setEndTime(' + (card.endTime | 0 || 120) + ');');
    lines.push('        this.setRemoveTime(' + (card.removeTime | 0 || 60) + ');');
    lines.push('        this.setOriginalUserName(' + (card.originalUser || 'OTHER') + ');');
    lines.push('    }');
    lines.push('');
    lines.push('    @Override');
    lines.push('    public void spellcard_main()');
    lines.push('    {');

    // card.vars 是「宏」：toJava 会就地展开
    javaVars = compileVars(card.vars);
    javaVarDepth = 0;
    try {
      const grouped = {};
      const order = [];
      (card.events || []).forEach((ev, idx) => {
        const cond = [whenJava(ev.when), isBlank(ev.guard) ? '' : jstr(ev.guard)].filter(Boolean).join(' && ');
        if (!grouped[cond]) { grouped[cond] = []; order.push(cond); }
        grouped[cond].push({ ev, idx });
      });
      order.forEach(cond => {
        lines.push('        if(' + cond + ')');
        lines.push('        {');
        grouped[cond].forEach(item => {
          const ev = item.ev;
          lines.push('            // #' + (item.idx + 1) + ' ' + (ev.op || 'shot') + (ev.note ? '  ' + ev.note : ''));
          const rep = isBlank(ev.repeat) ? 1 : Number(exprOrNum(ev.repeat));
          const code = eventJava(ev, item.idx);
          if (Number.isFinite(rep) && rep > 1) {
            lines.push('            for(int i = 0; i < ' + rep + '; i++)');
            lines.push('            {');
            pushCode(16, code);
            lines.push('            }');
          } else {
            pushCode(12, code);
          }
          if (ev.sound && (ev.op || 'shot') !== 'sound') lines.push('            THShotLib.playShotSound(user);');
        });
        lines.push('        }');
      });
    } finally {
      javaVars = null;
    }
    lines.push('    }');

    if (usesRotateAxis) {
      lines.push('');
      lines.push('    private Vec3 rotateAxis(double x, double y, double z)');
      lines.push('    {');
      lines.push('        Vec3 r = new Vec3(x, y, z);');
      lines.push('        if (r.lengthSqr() < 1.0E-6D) return rotate_Default();');
      lines.push('        return THShotLib.getVectorNomalize(r);');
      lines.push('    }');
    }
    if (hasSpawn) {
      lines.push('');
      lines.push('    private void spawnEntity(String id, Vec3 pos)');
      lines.push('    {');
      lines.push('        if (!(user.level() instanceof net.minecraft.server.level.ServerLevel server)) return;');
      lines.push('        net.minecraft.resources.ResourceLocation rl =');
      lines.push('            net.minecraft.resources.ResourceLocation.tryParse(id);');
      lines.push('        if (rl == null) return;');
      lines.push('        net.minecraft.world.entity.EntityType<?> type =');
      lines.push('            net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.get(rl);');
      lines.push('        if (type == null) return;');
      lines.push('        net.minecraft.world.entity.Entity e = type.create(server);');
      lines.push('        if (e == null) return;');
      lines.push('        e.moveTo(pos.x, pos.y, pos.z, user.getYRot(), user.getXRot());');
      lines.push('        server.addFreshEntity(e);');
      lines.push('    }');
    }
    lines.push('}');
    // 光有类文件还不会出现在游戏里：把注册那一行也写出来，省得每次翻 THKaguyaCore
    const id = card.id === undefined || card.id === null || card.id === '' ? 0 : (Number(card.id) | 0);
    const regName = slug || cls.replace(/^THSC_/, '');
    lines.push('');
    lines.push('/*');
    lines.push(' * 注册（贴进 THKaguyaCore.java 的 registerSpellCard 列表；id 必须唯一，重号会被 idCheck 静默丢掉）：');
    lines.push(' *   thKaguyaMod.registry.SpellCardRegistry.registerSpellCard(');
    lines.push(' *       ' + cls + '.class, "thkaguyamod", "' + regName + '", ' + id + ');');
    lines.push(' * 卡面贴图：assets/thkaguya/textures/item/spellcard/' + regName + '.png');
    lines.push(' * 名字/等级/时间已经在构造函数里设好，注册完给对应的符卡道具就能用。');
    lines.push(' */');
    return lines.join('\n');
  }

  // repeat 之类只想拿数值的地方用（不生成 Java）
  function exprOrNum(x) {
    if (x instanceof Expr) return x.isConst ? x.eval({}) : 1;
    if (typeof x === 'number') return x;
    if (isBlank(x)) return 1;
    try { const e = E(String(x)); return e.isConst ? e.eval({}) : 1; } catch (e) { return 1; }
  }

  /* ================================================================
   * 代码生成：JSON（符卡JSON设计文档.md 的 schema 1.0）
   * ================================================================ */
  const raw = e => (e instanceof Expr ? e.src : e);
  const KEEP = e => !isBlank(e) && !(typeof e === 'object' && e !== null && !Array.isArray(e) && Object.keys(e).length === 0);

  function whenToSchema(w) {
    if (!w) return { at: 0 };
    const k = w.kind || 'at';
    if (k === 'at') {
      const a = w.a;
      if (Array.isArray(a)) return { at: a.map(x => Number(x) | 0) };
      return { at: Number(a) | 0 };
    }
    if (k === 'range') return { from: Number(w.a) | 0, to: Number(w.b) | 0 };
    if (k === 'every') {
      const o = { every: Math.max(1, Number(w.a) | 0) };
      if (!isBlank(w.b)) o.until = Number(w.b) | 0;
      return o;
    }
    return { at: 0 };
  }

  function whenFromSchema(o) {
    if (o === undefined || o === null) return { kind: 'at', a: 0 };
    if (typeof o === 'number') return { kind: 'at', a: o };
    if (Array.isArray(o)) return { kind: 'at', a: o.map(x => Number(x) | 0) };
    if (o.at !== undefined) {
      if (Array.isArray(o.at)) return { kind: 'at', a: o.at.map(x => Number(x) | 0) };
      return { kind: 'at', a: Number(o.at) | 0 };
    }
    if (o.from !== undefined || o.to !== undefined) return { kind: 'range', a: Number(o.from) | 0, b: Number(o.to) | 0 };
    if (o.every !== undefined) return { kind: 'every', a: Math.max(1, Number(o.every) | 0), b: o.until === undefined ? undefined : (Number(o.until) | 0) };
    return { kind: 'at', a: 0 };
  }

  // 内部扁平字段 → schema 字段（只搬 op 表里声明过的）
  function eventToSchema(ev) {
    const op = ev.op || 'shot';
    const info = OP_INFO[op] || OP_INFO.shot;
    const o = {};
    o.when = whenToSchema(ev.when);
    o.op = op;
    if (!isBlank(ev.anchor) && ev.anchor !== 'user_eye') o.anchor = ev.anchor;
    if (ev.anchor === 'vec' && ev.vec) o.vec = { x: raw(ev.vec.x), y: raw(ev.vec.y), z: raw(ev.vec.z) };
    if (!isBlank(ev.aim) && ev.aim !== 'look') o.aim = ev.aim;
    if ((ev.aim || '') === 'yawpitch') {
      if (!isBlank(ev.yaw)) o.yaw = raw(ev.yaw);
      if (!isBlank(ev.pitch)) o.pitch = raw(ev.pitch);
    }
    if (ev.aim === 'dir') {
      const d = ev.dir || {};
      const dx = firstOf(d.x, ev.dx), dy = firstOf(d.y, ev.dy), dz = firstOf(d.z, ev.dz);
      if (!isBlank(dx) || !isBlank(dy) || !isBlank(dz)) {
        o.dir = { x: raw(dx === undefined ? 0 : dx), y: raw(dy === undefined ? 0 : dy), z: raw(dz === undefined ? 0 : dz) };
      }
    }
    // 批量
    if (info.batch) {
      const box = {};
      const w = firstOf(ev.spray && ev.spray.way, ev.spread && ev.spread.way, ev.way);
      const s = firstOf(ev.spray && ev.spray.span, ev.spread && ev.spread.span, ev.wideAngle, ev.ringSpan, ev.span);
      const b = firstOf(ev.spray && ev.spray.base, ev.spread && ev.spread.base, ev.baseAngle);
      if (!isBlank(w)) box.way = raw(w);
      if (!isBlank(s)) box.span = raw(s);
      if (!isBlank(b)) box.base = raw(b);
      if (Object.keys(box).length) o[info.batch] = box;
    }
    // 运动 / 批量标量
    if (info.motion) {
      if (!isBlank(ev.firstSpeed)) o.first_speed = raw(ev.firstSpeed);
      if (!isBlank(ev.limitSpeed)) o.limit_speed = raw(ev.limitSpeed);
      if (!isBlank(ev.accel)) o.acceleration = raw(ev.accel);
      const g = ev.gravity || {};
      const gx = firstOf(g.x, ev.gx), gy = firstOf(g.y, ev.gy), gz = firstOf(g.z, ev.gz);
      if (!isBlank(gx) || !isBlank(gy) || !isBlank(gz)) {
        o.gravity = { x: raw(gx === undefined ? 0 : gx), y: raw(gy === undefined ? 0 : gy), z: raw(gz === undefined ? 0 : gz) };
      }
    }
    if (info.distance && !isBlank(ev.distance)) o.distance = raw(ev.distance);
    if (info.slope && !isBlank(ev.slope)) o.slope = raw(ev.slope);
    if (info.random) {
      if (!isBlank(ev.num)) o.num = raw(ev.num);
      const rs = firstOf(ev.ringSpan, ev.ring_span);
      if (!isBlank(rs)) o.ring_span = raw(rs);
    }
    if (info.nuclear) {
      if (!isBlank(ev.slope)) o.slope = raw(ev.slope);
      if (ev.rotate && (!isBlank(ev.rotate.x) || !isBlank(ev.rotate.y) || !isBlank(ev.rotate.z))) {
        o.rotate = { x: raw(ev.rotate.x), y: raw(ev.rotate.y), z: raw(ev.rotate.z) };
      }
    }
    // 自转：laser_b / nuclear 用 spin+spin_end，其余批量 op 用 rotation_speed+rotation_end（设计文档 §6）
    {
      const spinV = firstOf(ev.spin, ev.rotSpeed, ev.rotationSpeed);
      const spinE = firstOf(ev.spinEnd, ev.rotEnd, ev.rotationEnd);
      if (!isBlank(spinV) || !isBlank(spinE)) {
        if (info.attach || info.nuclear) {
          if (!isBlank(spinV)) o.spin = raw(spinV);
          if (!isBlank(spinE)) o.spin_end = raw(spinE);
        } else {
          if (!isBlank(spinV)) o.rotation_speed = raw(spinV);
          if (!isBlank(spinE)) o.rotation_end = raw(spinE);
        }
      }
    }
    if (info.attach && ev.attach) {
      const at = {};
      if (!isBlank(ev.attach.to)) at.to = ev.attach.to;
      if (!isBlank(ev.attach.length)) at.length = raw(ev.attach.length);
      const yo = firstOf(ev.attach.yOffset, ev.attach.y_offset);
      if (!isBlank(yo)) at.y_offset = raw(yo);
      if (Object.keys(at).length) o.attach = at;
    }
    // 动作类字段
    if (op === 'spawn' && !isBlank(ev.entity)) o.entity = ev.entity;
    if (op === 'remove_danmaku') {
      if (!isBlank(ev.range)) o.range = raw(ev.range);
      o.target = ev.target || 'All';
      const db = firstOf(ev.dropBonus, ev.drop_bonus);
      if (!isBlank(db)) o.drop_bonus = typeof db === 'boolean' ? db : !!db;
    }
    if (op === 'explosion_effect' && !isBlank(ev.size)) o.size = raw(ev.size);
    // 弹 / 激光数据
    if (info.data === 'shot' && ev.shot && Object.keys(ev.shot).length) o.shot = shotToSchema(ev.shot);
    if (info.data === 'laser' && ev.laser && Object.keys(ev.laser).length) o.laser = laserToSchema(ev.laser);
    if (!isBlank(ev.repeat)) o.repeat = raw(ev.repeat);
    if (ev.sound) o.sound = true;
    if (!isBlank(ev.guard)) o.if = raw(ev.guard);
    return o;
  }

  function shotToSchema(sh) {
    const o = {};
    if (!isBlank(sh.form)) o.form = typeof sh.form === 'number' && FORM_NAMES[sh.form] ? FORM_NAMES[sh.form] : raw(sh.form);
    if (!isBlank(sh.color)) o.color = raw(sh.color);
    ['size', 'damage', 'delay', 'end', 'special'].forEach(k => { if (!isBlank(sh[k])) o[k] = raw(sh[k]); });
    return o;
  }
  function laserToSchema(sh) {
    const o = {};
    if (!isBlank(sh.form)) {
      o.form = typeof sh.form === 'number' ? (LASER_FORM_NAMES[Math.abs(sh.form) % 3] || 'LASER_NORMAL').replace(/^LASER_/, '') : raw(sh.form);
    }
    ['color', 'width', 'length', 'damage', 'delay', 'end'].forEach(k => { if (!isBlank(sh[k])) o[k] = raw(sh[k]); });
    return o;
  }

  function cardToObject(card) {
    const o = {};
    o.$schema = 1;
    o.id = card.id === undefined || card.id === '' ? 200 : Number(card.id) | 0;
    o.slug = card.slug || 'generated';
    o.name = { zh_cn: card.name || '生成符卡', en_us: card.nameEn || '' };
    o.need_level = card.needLevel | 0 || 1;
    o.end_time = card.endTime | 0 || 120;
    o.remove_time = card.removeTime | 0 || 60;
    o.original_user = card.originalUser || 'OTHER';
    if (!isBlank(card.circleColor)) o.circle_color = isBlank(card.circleColor) ? null : raw(card.circleColor);
    else o.circle_color = null;
    const vars = {};
    if (card.vars && typeof card.vars === 'object') {
      for (const k in card.vars) {
        const val = card.vars[k];
        vars[k] = val instanceof Expr ? val.src : (typeof val === 'string' ? val : String(val));
      }
    }
    o.vars = vars;
    o.events = (card.events || []).map(eventToSchema);
    o.specials = (card.specials && typeof card.specials === 'object') ? card.specials : {};
    if (!isBlank(card.notes)) o.notes = card.notes;
    return o;
  }

  function cardToJson(card) {
    return JSON.stringify(cardToObject(card), null, 2);
  }

  // schema → 内部扁平模型
  function shotFromSchema(sh) {
    const o = {};
    if (!sh || typeof sh !== 'object') return o;
    if (!isBlank(sh.form)) o.form = sh.form;
    ['color', 'size', 'damage', 'delay', 'end', 'special'].forEach(k => { if (!isBlank(sh[k])) o[k] = sh[k]; });
    return o;
  }
  function laserFromSchema(sh) {
    const o = {};
    if (!sh || typeof sh !== 'object') return o;
    if (!isBlank(sh.form)) o.form = sh.form;
    ['color', 'width', 'length', 'damage', 'delay', 'end'].forEach(k => { if (!isBlank(sh[k])) o[k] = sh[k]; });
    return o;
  }

  function eventFromSchema(o) {
    if (!o || typeof o !== 'object') return null;
    const ev = { when: whenFromSchema(o.when), op: o.op || 'shot' };
    const op = ev.op;
    const info = OP_INFO[op] || OP_INFO.shot;
    if (!isBlank(o.anchor)) ev.anchor = o.anchor;
    if (o.vec && typeof o.vec === 'object') ev.vec = { x: o.vec.x, y: o.vec.y, z: o.vec.z };
    if (!isBlank(o.aim)) ev.aim = o.aim;
    if (!isBlank(o.yaw)) ev.yaw = o.yaw;
    if (!isBlank(o.pitch)) ev.pitch = o.pitch;
    if (o.dir && typeof o.dir === 'object') ev.dir = { x: o.dir.x, y: o.dir.y, z: o.dir.z };
    if (o.spread && typeof o.spread === 'object') ev.spread = { way: o.spread.way, span: o.spread.span, base: o.spread.base };
    if (o.spray && typeof o.spray === 'object') ev.spray = { way: o.spray.way, span: o.spray.span, base: o.spray.base };
    ['first_speed:firstSpeed', 'limit_speed:limitSpeed', 'acceleration:accel',
      'distance:distance', 'slope:slope', 'spin:spin', 'spin_end:spinEnd',
      'rotation_speed:rotationSpeed', 'rotation_end:rotationEnd',
      'repeat:repeat', 'range:range', 'target:target', 'drop_bonus:dropBonus',
      'entity:entity', 'size:size', 'num:num', 'ring_span:ringSpan'].forEach(pair => {
        const [sk, ik] = pair.split(':');
        if (!isBlank(o[sk])) ev[ik] = o[sk];
      });
    if (o.gravity && typeof o.gravity === 'object') ev.gravity = { x: o.gravity.x, y: o.gravity.y, z: o.gravity.z };
    if (o.rotate && typeof o.rotate === 'object') ev.rotate = { x: o.rotate.x, y: o.rotate.y, z: o.rotate.z };
    if (o.attach && typeof o.attach === 'object') {
      ev.attach = { to: o.attach.to || 'user', length: o.attach.length, yOffset: o.attach.y_offset };
    }
    if (o.shot && typeof o.shot === 'object') ev.shot = shotFromSchema(o.shot);
    if (o.laser && typeof o.laser === 'object') ev.laser = laserFromSchema(o.laser);
    if (!isBlank(o.if)) ev.guard = o.if;
    if (o.sound) ev.sound = true;
    // 向后兼容：扁平写法（早期编辑器导出）
    ['way', 'wideAngle', 'baseAngle', 'ringSpan', 'gx', 'gy', 'gz', 'dx', 'dy', 'dz', 'firstSpeed', 'limitSpeed', 'accel', 'rotSpeed', 'rotEnd', 'num'].forEach(k => {
      if (!isBlank(o[k]) && isBlank(ev[k])) ev[k] = o[k];
    });
    // 保留未知字段，导出时原样带回
    const known = new Set(['when', 'op', 'anchor', 'vec', 'aim', 'yaw', 'pitch', 'dir', 'spread', 'spray',
      'shot', 'laser', 'if', 'sound', 'repeat', 'attach', 'gravity', 'rotate',
      'first_speed', 'limit_speed', 'acceleration', 'distance', 'slope', 'spin', 'spin_end',
      'rotation_speed', 'rotation_end', 'range', 'target', 'drop_bonus', 'entity', 'size', 'num', 'ring_span',
      'way', 'wideAngle', 'baseAngle', 'ringSpan', 'gx', 'gy', 'gz', 'dx', 'dy', 'dz',
      'firstSpeed', 'limitSpeed', 'accel', 'rotSpeed', 'rotEnd']);
    const extra = {};
    for (const k in o) if (!known.has(k)) extra[k] = o[k];
    if (Object.keys(extra).length) ev._extra = extra;
    return ev;
  }

  function cardFromJson(input) {
    let o = input;
    if (typeof o === 'string') o = JSON.parse(o);
    if (!o || typeof o !== 'object') throw new Error('不是合法的符卡对象');
    const name = o.name && typeof o.name === 'object' ? o.name : { zh_cn: o.name || '导入的符卡', en_us: '' };
    const card = {
      name: name.zh_cn || '导入的符卡',
      nameEn: name.en_us || '',
      slug: o.slug || 'imported',
      id: o.id === undefined ? 200 : Number(o.id) | 0,
      needLevel: o.need_level === undefined ? 1 : Number(o.need_level) | 0,
      endTime: o.end_time === undefined ? 120 : Number(o.end_time) | 0,
      removeTime: o.remove_time === undefined ? 60 : Number(o.remove_time) | 0,
      originalUser: o.original_user || 'OTHER',
      circleColor: o.circle_color === undefined ? null : o.circle_color,
      maxTick: Math.max(o.end_time === undefined ? 120 : Number(o.end_time) | 0, 40),
      userYaw: 0,
      userPitch: 0,
      level: 2,
      notes: o.notes || '',
      vars: (o.vars && typeof o.vars === 'object') ? Object.assign({}, o.vars) : {},
      specials: (o.specials && typeof o.specials === 'object') ? o.specials : {},
      events: []
    };
    card.events = (Array.isArray(o.events) ? o.events : []).map(eventFromSchema).filter(Boolean);
    return card;
  }

  /* ---------------- 校验（编辑器提示用，不是 JSON Schema） ---------------- */
  function validateCard(card, sim) {
    const out = [];
    const add = (level, where, msg) => out.push({ level, where, msg });
    if (!card || typeof card !== 'object') { add('error', 'card', '符卡对象为空'); return out; }
    const slug = String(card.slug || '');
    if (!slug) add('error', 'slug', 'slug 为空，导出的 Java 类名会变成 THSC_');
    else if (!/^[A-Za-z_][A-Za-z0-9_]*$/.test(slug)) add('warn', 'slug', 'slug 含非法字符，导出时会被替换成下划线：' + slug);
    if (!card.name) add('warn', 'name', '符卡名为空');
    if (!(card.maxTick | 0)) add('error', 'maxTick', '时长必须 ≥ 1 tick');
    const ou = String(card.originalUser || 'OTHER').trim().toUpperCase();
    if (USER_NAMES.indexOf(ou) < 0) add('error', 'original_user', 'THSpellCard 里没有这个原角色常量：' + card.originalUser);
    if (card.vars && typeof card.vars === 'object') {
      for (const k in card.vars) {
        try { E(card.vars[k]); } catch (e) { add('error', 'vars.' + k, e.message); }
      }
    }
    if (card.specials && typeof card.specials === 'object') {
      for (const k in card.specials) {
        const m = card.specials[k];
        if (!m || typeof m !== 'object') { add('warn', 'specials.' + k, '不是一个对象'); continue; }
        if (m.id === undefined) add('warn', 'specials.' + k, '缺 id');
        if (!Array.isArray(m.on_tick) && !Array.isArray(m.on_end)) add('warn', 'specials.' + k, 'on_tick / on_end 都没有');
      }
    }
    if (!Array.isArray(card.events) || !card.events.length) add('warn', 'events', '没有任何事件');
    (card.events || []).forEach((ev, i) => {
      const at = '#' + (i + 1) + ' ' + (ev.op || '?');
      if (!OP_INFO[ev.op]) { add('error', at, '未知 op：' + ev.op); return; }
      const info = OP_INFO[ev.op];
      if (!ev.when) add('warn', at, '没有 when，默认在第 0 tick 触发');
      if ((ev.aim || 'look') === 'yawpitch' && (isBlank(ev.yaw) || isBlank(ev.pitch))) add('warn', at, 'aim=yawpitch 但 yaw/pitch 没填');
      if (ev.aim === 'dir' && !(ev.dir || ev.dx !== undefined)) add('warn', at, 'aim=dir 但没有方向分量');
      if (ev.anchor === 'vec' && !ev.vec) add('warn', at, 'anchor=vec 但没有 vec 偏移');
      if (ev.op === 'spawn' && !ev.entity) add('error', at, 'spawn 需要 entity');
      if (ev.op === 'remove_danmaku' && !['Enemy', 'Other', 'All'].includes(ev.target || 'All')) add('warn', at, 'target 只能是 Enemy / Other / All');
      if (info.batch === 'spray' || info.batch === 'spread') {
        const w = firstOf(batchVal(ev, 'way', 'way', undefined));
        if (w === undefined) add('warn', at, '批量 op 建议填 way（默认 2）');
      }
      if (ev.op === 'sphere' || ev.op === 'sphere_laser') {
        const w = n(firstOf(batchVal(ev, 'way', 'way', 0)), { $vars: {}, time: 0, level: 2, i: 0, way: 0, r: 0, pi: Math.PI, e: Math.E, userYaw: 0, userPitch: 0, bbHeight: 1.8, rand: DFLT_RAND, $depth: 0 }) | 0;
        if (w >= WAYS.length) add('warn', at, 'sphere 的 way ≥ 45，原版会直接 return（不发射）');
      }
      if (ev.shot && !isBlank(ev.shot.form) && typeof ev.shot.form === 'string' && !/^[-+]?\d+$/.test(ev.shot.form)) {
        const t = ev.shot.form.trim().toUpperCase().replace(/^FORM_/, '');
        if (!(t in FORMS)) add('warn', at, '未知弹型名：' + ev.shot.form);
      }
      if (ev.op === 'wide' || ev.op === 'wide_laser') {
        const w = Number(exprOrNum(firstOf(batchVal(ev, 'way', 'way', 2))));
        if (Number.isFinite(w) && w < 2) add('warn', at, 'wide 系列的 way < 2 会让原版的 span=wideAngle/(way-1) 除零');
      }
      if (info.data === 'shot' && ev.shot && !isBlank(ev.shot.damage)) {
        const d = Number(exprOrNum(ev.shot.damage));
        if (Number.isFinite(d) && d <= 0) add('warn', at, 'damage ≤ 0 的弹体在游戏里第一个 tick 就会消失');
      }
      if (ev.op === 'circle_laser') add('info', at, '原版 createCircleLaserA 内部按 way+1 发，实际比填的多 1 束');
      try { E(isBlank(ev.guard) ? 0 : ev.guard); } catch (e) { add('error', at, 'if 条件：' + e.message); }
    });
    if (sim && sim.errors && sim.errors.length) {
      const seen = new Set();
      sim.errors.slice(0, 12).forEach(e => {
        const k = e.msg;
        if (seen.has(k)) return;
        seen.add(k);
        add('error', 't' + e.tick + ' ' + (e.op || '') + (e.idx >= 0 ? ' #' + (e.idx + 1) : ''), e.msg);
      });
    }
    return out;
  }

  /* ---------------- 导出 ---------------- */
  const API = {
    // 数学
    v, vadd, vsub, vscale, vlen, vdot, vcross, vnorm, perpendicular,
    fromAngle, yawOf, pitchOf, rotateAround, rotateDefault, gravityZero, GRAVITY_DEFAULT, EYE_HEIGHT,
    // 常量
    FORMS, FORM_NAMES, SIZE, DAMAGE, COLORS, PALETTE, PALETTE_BRIGHT, LASER_FORMS, LASER_FORM_NAMES, USER_NAMES, WAYS,
    OPS, OP_INFO,
    // 表达式
    E, Expr, parseExpr, evalAst, toJava, compileVars, CONST_ALIASES,
    // 展开 / 仿真
    expandEvent, whenMatch, simulate, stateAt, makeShot, stepShot, anchorPos, batchVal, firstOf,
    // 代码生成：Java
    cardToJava, eventJava, whenJava, shotDataJava,
    // 代码生成：JSON schema 1.0
    cardToJson, cardToObject, cardFromJson, eventToSchema, eventFromSchema, whenToSchema, whenFromSchema,
    validateCard
  };

  root.DMK = API;
  if (typeof module !== 'undefined' && module.exports) module.exports = API;
})(typeof window !== 'undefined' ? window : globalThis);
