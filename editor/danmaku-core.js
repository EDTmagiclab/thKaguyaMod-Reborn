/*
 * danmaku-core.js — THShotLib 数学层 + 物理层 + 代码生成（无依赖，浏览器 / Node 双用）
 *
 * 逐行对照来源（1.20.1 移植版）：
 *   thKaguyaMod/THShotLib.java        createShot / createWideShot / createCircleShot /
 *                                     createRingShot / createRandomRingShot / createSphereShot /
 *                                     createLaserA / createWideLaserA / createCircleLaserA /
 *                                     createRingLaserA
 *   thKaguyaMod/entity/shot/EntityTHShot.java  tick() / shotAcceleration()
 *   thKaguyaMod/DanmakuConstants.java          FORM / SIZE / DAMAGE / 颜色
 *
 * 不做的事（原文明说）：
 *   - specialMotion() 的分发（ISpecialShot 需要世界实例）→ 只复刻 HOMING01 / FALL01 两个纯数学项
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

  /* ---------------- THShotLib 角度 / 旋转（逐行） ---------------- */
  // THShotLib.getVecFromAngle(yaw,pitch,force)  :376-390
  function fromAngle(yaw, pitch, force) {
    const f = force === undefined ? 1 : force;
    const yr = yaw * DEG, pr = pitch * DEG;
    return v(-Math.sin(yr) * Math.cos(pr) * f, -Math.sin(pr) * f, Math.cos(yr) * Math.cos(pr) * f);
  }
  const yawOf = (x, z) => Math.atan2(x, z) / DEG;                       // :393
  const pitchOf = (x, y, z) => Math.atan2(y, Math.sqrt(x * x + z * z)) / DEG; // :399

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

  const rotateDefault = () => v(0, 1, 0);                               // :170
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

  /* ---------------- 表达式（安全求值 + Java 反生成，不用 eval） ---------------- */
  const FUNCS = {
    sin: d => Math.sin(d * DEG), cos: d => Math.cos(d * DEG), tan: d => Math.tan(d * DEG),
    sqrt: Math.sqrt, abs: Math.abs, floor: Math.floor, ceil: Math.ceil, round: Math.round,
    min: Math.min, max: Math.max, pow: Math.pow
  };

  function parseExpr(src) {
    if (src === null || src === undefined || src === '') return { t: 'num', v: 0 };
    if (typeof src === 'number') return { t: 'num', v: src };
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
      let n = unary();
      for (;;) {
        const c = peek();
        if (c === '*') { i++; n = { t: 'bin', o: '*', a: n, b: unary() }; }
        else if (c === '/') { i++; n = { t: 'bin', o: '/', a: n, b: unary() }; }
        else if (c === '%') { i++; n = { t: 'bin', o: '%', a: n, b: unary() }; }
        else return n;
      }
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
      const id = /^[A-Za-z_][A-Za-z_0-9.]*/.exec(s.slice(i));
      if (id) {
        i += id[0].length;
        const name = id[0];
        if (peek() === '[') {
          // 表值引用：SIZE[9] / DAMAGE[FORM_RICE] / @SIZE[...]（对应设计文档 §5.4）
          i++;
          const inner = add();
          if (peek() === ']') i++;
          return { t: 'call', f: name.charAt(0) === '@' ? name.slice(1) : name, args: [inner] };
        }
        if (peek() === '(') {
          i++;
          const args = [];
          if (peek() !== ')') { args.push(add()); while (peek() === ',') { i++; args.push(add()); } }
          if (peek() === ')') i++;
          return { t: 'call', f: name, args };
        }
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
      case 'var':
        if (n.n === 'pi') return Math.PI;
        if (n.n in env) return env[n.n];
        throw new Error('未知变量 ' + n.n);
      case 'neg': return -evalAst(n.a, env);
      case 'bin': {
        const a = evalAst(n.a, env), b = evalAst(n.b, env);
        switch (n.o) {
          case '+': return a + b;
          case '-': return a - b;
          case '*': return a * b;
          case '/': return a / b;
          case '%': return a % b;
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
        const f = FUNCS[n.f];
        if (f) return f.apply(null, n.args.map(x => evalAst(x, env)));
        const a0 = evalAst(n.args[0], env) | 0;
        if (n.f === 'SIZE') return SIZE[a0 % 32];
        if (n.f === 'DAMAGE') return DAMAGE[a0 % 32];
        throw new Error('未知函数 ' + n.f + '()');
      }
    }
    throw new Error('非法表达式节点');
  }

  function toJava(n) {
    switch (n.t) {
      case 'num': return String(n.v);
      case 'var':
        if (n.n === 'time' || n.n === 'i' || n.n === 'way' || n.n === 'level') return n.n;
        if (n.n === 'pi') return 'Math.PI';
        if (n.n === 'userYaw') return 'user.getYRot()';
        if (n.n === 'userPitch') return 'user.getXRot()';
        if (n.n === 'bbHeight') return 'user.getBbHeight()';
        return n.n;
      case 'neg': return '(-' + toJava(n.a) + ')';
      case 'bin': return '(' + toJava(n.a) + ' ' + n.o + ' ' + toJava(n.b) + ')';
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
    this.src = src;
    this.ast = ast || parseExpr(src);
    this.isConst = isConstAst(this.ast);
  }
  Expr.prototype.eval = function (env) { return evalAst(this.ast, env || {}); };
  Expr.prototype.java = function () { return toJava(this.ast); };
  function isConstAst(n) {
    if (n.t === 'num') return true;
    if (n.t === 'var') return n.n === 'pi';
    if (n.t === 'neg') return isConstAst(n.a);
    if (n.t === 'bin') return isConstAst(n.a) && isConstAst(n.b);
    if (n.t === 'call') return n.args.every(isConstAst);
    return false;
  }

  /* ---------------- 事件 → 弹（THShotLib 的 create* 族，逐行复刻） ---------------- */
  // 数值取值：number 直取；Expr 求值；字符串当场解析（带缓存）。
  // 注意：早期版本只认 Expr/number，字符串会静默变成 0 —— 编辑器里所有输入都是字符串，
  // 这里必须做完整分派，否则 "time % 4" 这类表达式会悄悄失效。
  const exprCache = new Map();
  const n = (x, env) => {
    if (x instanceof Expr) return x.eval(env);
    if (typeof x === 'number') return x;
    if (x === undefined || x === null || x === '') return 0;
    const key = String(x);
    let e = exprCache.get(key);
    if (!e) { e = E(key); exprCache.set(key, e); }
    return e.eval(env);
  };

  function baseSpec(ev, env, pos, angle, rotate) {
    const yaw = yawOf(angle.x, angle.z);
    const pitch = pitchOf(angle.x, angle.y, angle.z);
    const isLaser = String(ev.op).indexOf('laser') >= 0;
    const sh = isLaser ? (ev.laser || {}) : (ev.shot || {});
    // 名字与数字都接受：RED / FORM_STAR / 9 / "time % 4"
    const colorOf = x => {
      if (typeof x === 'string') {
        const t = x.trim().toUpperCase();
        if (t in COLORS) return COLORS[t];
      }
      return n(x, env) | 0;
    };
    const formOf = x => {
      if (typeof x === 'string') {
        const t = x.trim().toUpperCase();
        if (t in FORMS) return FORMS[t];
        if (t in LASER_FORMS) return LASER_FORMS[t];
      }
      return n(x, env) | 0;
    };
    const form = formOf(sh.form === undefined ? 0 : sh.form);
    const sizeField = sh.size;
    const dmgField = sh.damage;
    return {
      kind: isLaser ? 'laser' : 'shot',
      pos: pos,
      angle: angle,
      // 未指定 rotate 时 = getVecFromAngle(yaw, pitch+90)（createShot 的 :583）
      rotate: rotate || fromAngle(yaw, pitch + 90, 1),
      rotSpeed: n(ev.rotSpeed, env),
      rotEnd: ev.rotEnd === undefined || ev.rotEnd === '' ? 9999 : n(ev.rotEnd, env),
      firstSpeed: n(ev.firstSpeed, env),
      limitSpeed: ev.limitSpeed === undefined || ev.limitSpeed === '' ? n(ev.firstSpeed, env) : n(ev.limitSpeed, env),
      accel: n(ev.accel, env),
      gravity: v(n(ev.gx, env), n(ev.gy, env), n(ev.gz, env)),
      form: form,
      color: colorOf(sh.color === undefined ? 0 : sh.color),
      size: sizeField === undefined || sizeField === '' ? SIZE[form % 32] : n(sizeField, env),
      damage: dmgField === undefined || dmgField === '' ? DAMAGE[form % 32] : n(dmgField, env),
      delay: n(sh.delay, env) | 0,
      end: sh.end === undefined || sh.end === '' ? 80 : n(sh.end, env) | 0,
      special: n(sh.special === undefined ? 0 : sh.special, env) | 0,
      width: n((ev.laser || {}).width, env) || 0.1,
      length: n((ev.laser || {}).length, env) || 2.0
    };
  }

  // createWideShot 核心 :658-716
  function wideShots(ev, env, out, basePos, baseAngle) {
    const way = Math.max(2, n(ev.way, env) | 0);
    const wideAngle = n(ev.wideAngle === undefined ? 60 : ev.wideAngle, env);
    const distance = n(ev.distance, env);
    const baseAngleOffset = n(ev.baseAngle, env);
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
    const way = Math.max(2, n(ev.way, env) | 0);
    let ba = n(ev.baseAngle, env);
    if (way % 2 === 0) ba += 360 / (way * 2);            // :850-853
    const sub = Object.assign({}, ev, { wideAngle: new Expr(360 - 360 / way), baseAngle: new Expr(ba) });
    wideShots(sub, env, out, basePos, baseAngle);
  }

  // createRingShot :968-1039
  function ringShots(ev, env, out, basePos, baseAngle) {
    const way = Math.max(2, n(ev.way, env) | 0);
    const distance = n(ev.distance, env);
    let ringSpan = n(ev.ringSpan === undefined ? 0 : ev.ringSpan, env);
    const ringBaseAngle = n(ev.baseAngle, env);
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
    const num = Math.max(1, n(ev.num === undefined ? ev.way : ev.num, env) | 0);
    const distance = n(ev.distance, env);
    const ringSpan = n(ev.ringSpan === undefined ? 360 : ev.ringSpan, env);
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
    const way = n(ev.way, env) | 0;
    if (way >= WAYS.length || WAYS[way].length === 0) return;
    const distance = n(ev.distance, env);
    const angleZ = n(ev.slope, env);
    const baseAngleOff = n(ev.baseAngle, env);
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
          way: new Expr(row[i]),
          baseAngle: new Expr(baseAngleOff + slopeAcc),
          ringSpan: new Expr(angleBase)
        });
        ringShots(sub, env, out, basePos, baseAngle);
        slopeAcc += 180 / row.length;
      }
      angleBase += angleSpan;
    }
  }

  /* ---------------- 事件展开入口 ---------------- */
  function eventAnchorAngle(ev, env, ctx) {
    const aim = ev.aim || 'look';
    if (aim === 'look') return fromAngle(ctx.userYaw, ctx.userPitch, 1);
    if (aim === 'yawpitch') return fromAngle(n(ev.yaw, env), n(ev.pitch, env), 1);
    if (aim === 'target') return vnorm(vsub(ctx.targetPos, ctx.userPos));
    if (aim === 'dir') return vnorm(v(n(ev.dx, env), n(ev.dy, env), n(ev.dz, env)));
    return fromAngle(ctx.userYaw, ctx.userPitch, 1);
  }

  function expandEvent(ev, env, ctx, emit) {
    const pos = ctx.userPos;   // anchor=user_eye（原点即眼睛）
    const angle = eventAnchorAngle(ev, env, ctx);
    const out = [];
    const op = ev.op || 'shot';
    switch (op) {
      case 'shot':
      case 'laser_a':
        out.push(baseSpec(ev, env, pos, angle, undefined));
        break;
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
      default:
        throw new Error('未知 op：' + op);
    }
    const repeat = Math.max(1, n(ev.repeat === undefined ? 1 : ev.repeat, env) | 0);
    for (let r = 0; r < repeat; r++) {
      env.r = r;
      for (let i = 0; i < out.length; i++) emit(out[i]);
    }
  }

  /* ---------------- 时间轴条件 ---------------- */
  function whenMatch(when, t) {
    if (!when) return t === 0;
    const k = when.kind || 'at';
    if (k === 'at') return t === (when.a | 0);
    if (k === 'range') return t >= (when.a | 0) && t < (when.b | 0);
    if (k === 'every') return t % Math.max(1, when.a | 0) === 0 && (when.b === undefined || when.b === '' || t < (when.b | 0));
    return false;
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
      slope: spec.slope === undefined ? 0 : spec.slope,
      kind: spec.kind, width: spec.width, length: spec.length,
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

    // specialMotion：只复刻纯数学的两项（HOMING01/FALL01），其余在预览里退化为直线
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
   * card = {
   *   name, level, maxTick, userYaw, userPitch,
   *   events: [ { op, when:{kind,a,b}, aim, yaw, pitch, dx, dy, dz,
   *               firstSpeed, limitSpeed, accel, gx, gy, gz, repeat,
   *               way, wideAngle, distance, baseAngle, ringSpan, num, slope,
   *               rotSpeed, rotEnd,
   *               shot:{form,color,size,damage,delay,end,special},
   *               laser:{form,color,width,length,damage,delay,end} } ]
   * }
   */
  function simulate(card) {
    const maxTick = Math.max(0, card.maxTick | 0 || 120);
    const errors = [];
    const spawnLog = [];
    const shots = [];
    const ctx = {
      userPos: v(0, 0, 0),
      targetPos: v(0, 0, 3),
      userYaw: card.userYaw || 0,
      userPitch: card.userPitch || 0,
      rand: mulberry32(20261021)
    };
    const level = card.level === undefined ? 2 : card.level | 0;

    for (let t = 0; t <= maxTick; t++) {
      // 1) 本 tick 的事件（对应 spellcard_main()）
      for (const ev of card.events) {
        if (!whenMatch(ev.when, t)) continue;
        const env = { time: t, level: level, i: 0, way: 0, r: 0, pi: Math.PI, userYaw: ctx.userYaw, userPitch: ctx.userPitch, bbHeight: 1.8 };
        // 复合守卫：对应原卡里的 "if(time % 30 < 10 && time % 2 == 1)"
        if (ev.guard) {
          try {
            if (!(ev.guard instanceof Expr)) ev.guard = E(ev.guard);
            if (!ev.guard.eval(env)) continue;
          } catch (e) {
            errors.push({ tick: t, op: ev.op, msg: 'guard: ' + e.message });
            continue;
          }
        }
        try {
          let spawned = 0;
          expandEvent(ev, env, ctx, spec => {
            // 原版 createShot :528-537：RANDOM 逐发取 0–7，RAINBOW 逐发取 0–6
            if (spec.color === 8) spec.color = (ctx.rand() * 8) | 0;
            else if (spec.color === 9) spec.color = (ctx.rand() * 7) | 0;
            const s = makeShot(spec, t);
            shots.push(s);
            spawned++;
          });
          if (spawned) spawnLog.push({ tick: t, op: ev.op, count: spawned });
        } catch (e) {
          errors.push({ tick: t, op: ev.op, msg: e.message });
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
    return { shots, errors, spawnLog, maxTick };
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
        width: s.width, length: s.length, age: idx, born: s.born
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
    const E2 = e instanceof Expr ? e : E(e);
    return E2.isConst ? fmt(E2.eval({})) : '(' + E2.java() + ')';
  };

  function shotDataJava(sh, isLaser) {
    const form = sh.form === undefined ? 0 : jstr(sh.form);
    const color = sh.color === undefined ? 0 : jstr(sh.color);
    if (isLaser) {
      const width = sh.width === undefined || sh.width === '' ? '0.1' : jstr(sh.width);
      const len = sh.length === undefined || sh.length === '' ? '2.0' : jstr(sh.length);
      const dmg = sh.damage === undefined || sh.damage === '' ? '3.0' : jstr(sh.damage);
      const delay = sh.delay === undefined || sh.delay === '' ? '0' : jstr(sh.delay);
      const end = sh.end === undefined || sh.end === '' ? '80' : jstr(sh.end);
      return 'LaserData.laser(LASER_NORMAL, ' + color + ', ' + width + ', ' + len + ', ' + dmg + ', ' + delay + ', ' + end + ', 0)';
    }
    const size = sh.size === undefined || sh.size === '' ? null : jstr(sh.size);
    const dmg = sh.damage === undefined || sh.damage === '' ? null : jstr(sh.damage);
    const delay = sh.delay === undefined || sh.delay === '' ? '0' : jstr(sh.delay);
    const end = sh.end === undefined || sh.end === '' ? '80' : jstr(sh.end);
    const sp = sh.special && String(sh.special) !== '0' ? ', ' + jstr(sh.special) : '';
    if (size === null && dmg === null) return 'shot(' + form + ', ' + color + ', ' + delay + ', ' + end + sp + ')';
    return 'shot(' + form + ', ' + color + ', ' + (size || 'SIZE[' + form + ']') + 'F, ' + (dmg || 'DAMAGE[' + form + ']') + 'F, ' + delay + ', ' + end + sp + ')';
  }

  function eventJava(ev) {
    const op = ev.op || 'shot';
    const isLaser = op.indexOf('laser') >= 0;
    const data = shotDataJava(isLaser ? (ev.laser || {}) : (ev.shot || {}), isLaser);
    const fu = s => (s === undefined || s === '' ? '0.0D' : jstr(s));
    const first = fu(ev.firstSpeed);
    const limit = ev.limitSpeed === undefined || ev.limitSpeed === '' ? first : fu(ev.limitSpeed);
    const accel = fu(ev.accel);
    const grav = 'gravity(' + fu(ev.gx) + ', ' + fu(ev.gy) + ', ' + fu(ev.gz) + ')';

    let aimVec;
    if ((ev.aim || 'look') === 'look') aimVec = 'user.getLookAngle()';
    else if (ev.aim === 'yawpitch') aimVec = 'THShotLib.getVecFromAngle(' + fu(ev.yaw) + 'F, ' + fu(ev.pitch) + 'F)';
    else if (ev.aim === 'target') aimVec = 'angle_UserToTarget()';
    else aimVec = 'THShotLib.getVectorNomalize(angle(' + fu(ev.dx) + ', ' + fu(ev.dy) + ', ' + fu(ev.dz) + '))';

    const way = ev.way === undefined || ev.way === '' ? '1' : jstr(ev.way);
    const dist = fu(ev.distance);
    const base = fu(ev.baseAngle);
    const rotS = fu(ev.rotSpeed);
    const rotE = ev.rotEnd === undefined || ev.rotEnd === '' ? '9999' : jstr(ev.rotEnd);

    switch (op) {
      case 'shot':
        return 'THShotLib.createShot(user, pos_User(), ' + aimVec + ', ' + first + ', ' + limit + ', ' + accel + ', ' + grav + ', ' + data + ');';
      case 'laser_a':
        return 'THShotLib.createLaserA(user, pos_User(), ' + aimVec + ', ' + first + ', ' + limit + ', ' + accel + ', ' + grav + ', ' + data + ');';
      case 'wide':
        return 'THShotLib.createWideShot(user, user, pos_User(), ' + aimVec + ', ' + rotS + 'F, ' + rotE + ', ' + first + ', ' + limit + ', ' + accel + ', ' + grav + ', ' + data + ', ' + way + ', ' + fu(ev.wideAngle) + 'F, ' + dist + ', ' + base + 'F);';
      case 'wide_laser':
        return 'THShotLib.createWideLaserA(user, user, pos_User(), ' + aimVec + ', ' + first + ', ' + limit + ', ' + accel + ', ' + grav + ', ' + data + ', ' + way + ', ' + fu(ev.wideAngle) + 'F, ' + dist + ');';
      case 'circle':
        return 'THShotLib.createCircleShot(user, user, pos_User(), ' + aimVec + ', ' + rotS + 'F, ' + rotE + ', ' + first + ', ' + limit + ', ' + accel + ', ' + grav + ', ' + data + ', ' + way + ', ' + dist + ', ' + base + 'F);';
      case 'circle_laser':
        return 'THShotLib.createCircleLaserA(user, user, pos_User(), ' + aimVec + ', ' + first + ', ' + limit + ', ' + accel + ', ' + grav + ', ' + data + ', ' + way + ', ' + dist + ');';
      case 'ring':
        return 'THShotLib.createRingShot(user, user, pos_User(), ' + aimVec + ', ' + rotS + 'F, ' + rotE + ', ' + first + ', ' + limit + ', ' + accel + ', ' + grav + ', ' + data + ', ' + way + ', ' + dist + ', ' + fu(ev.ringSpan) + 'F, ' + base + 'F);';
      case 'ring_laser':
        return 'THShotLib.createRingLaserA(user, user, pos_User(), ' + aimVec + ', ' + first + ', ' + limit + ', ' + accel + ', ' + grav + ', ' + data + ', ' + way + ', ' + dist + ', ' + fu(ev.ringSpan) + 'F, ' + base + 'F);';
      case 'random_ring':
        return 'THShotLib.createRandomRingShot(user, user, pos_User(), ' + aimVec + ', ' + rotS + 'F, ' + rotE + ', ' + first + ', ' + limit + ', ' + accel + ', ' + grav + ', ' + data + ', ' + jstr(ev.num) + ', ' + dist + ', ' + fu(ev.ringSpan) + 'F);';
      case 'random_ring_laser':
        return 'THShotLib.createRandomRingLaser(user, user, pos_User(), ' + aimVec + ', ' + first + ', ' + limit + ', ' + accel + ', ' + grav + ', ' + data + ', ' + jstr(ev.num) + ', ' + dist + ', ' + fu(ev.ringSpan) + 'F);';
      case 'sphere':
        return 'THShotLib.createSphereShot(user, user, pos_User(), ' + aimVec + ', ' + fu(ev.slope) + 'F, ' + first + ', ' + limit + ', ' + accel + ', ' + grav + ', ' + data + ', ' + way + ', ' + dist + ', ' + base + 'F);';
      case 'sphere_laser':
        return 'THShotLib.createSphereLaserA(user, user, pos_User(), ' + aimVec + ', ' + first + ', ' + limit + ', ' + accel + ', ' + grav + ', ' + data + ', ' + way + ', ' + dist + ', ' + base + 'F);';
    }
    return '// 未知 op: ' + op;
  }

  function whenJava(when) {
    if (!when || (when.kind || 'at') === 'at') return 'time == ' + ((when && when.a) | 0);
    if (when.kind === 'range') return 'time >= ' + (when.a | 0) + ' && time < ' + (when.b | 0);
    if (when.kind === 'every') {
      let s = 'time % ' + Math.max(1, when.a | 0) + ' == 0';
      if (when.b !== undefined && when.b !== '') s += ' && time < ' + (when.b | 0);
      return s;
    }
    return 'time == 0';
  }

  function cardToJava(card) {
    const cls = 'THSC_' + (card.slug || 'Generated').replace(/[^A-Za-z0-9_]/g, '_');
    const lines = [];
    lines.push('package thKaguyaMod.entity.spellcard;');
    lines.push('');
    lines.push('import net.minecraft.world.phys.Vec3;');
    lines.push('import thKaguyaMod.THShotLib;');
    lines.push('');
    lines.push('import static thKaguyaMod.DanmakuConstants.*;');
    lines.push('');
    lines.push('/**');
    lines.push(' * ' + (card.name || '生成符卡') + ' — 由可视化弹幕编辑器导出');
    lines.push(' * 事件数 ' + card.events.length + '，时长 ' + (card.maxTick | 0) + ' tick');
    lines.push(' * 注意：导出的是 THShotLib 直接调用，没有特殊弹状态机；需要 ISpecialShot 的卡请手写。');
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
    const grouped = {};
    const order = [];
    card.events.forEach((ev, idx) => {
      const cond = [whenJava(ev.when), ev.guard ? jstr(ev.guard) : ''].filter(Boolean).join(' && ');
      if (!grouped[cond]) { grouped[cond] = []; order.push(cond); }
      grouped[cond].push({ ev, idx });
    });
    order.forEach(cond => {
      lines.push('        if(' + cond + ')');
      lines.push('        {');
      grouped[cond].forEach(item => {
        const ev = item.ev;
        lines.push('            // #' + (item.idx + 1) + ' ' + (ev.op || 'shot') + (ev.note ? '  ' + ev.note : ''));
        const rep = ev.repeat === undefined || ev.repeat === '' ? 1 : Number(ev.repeat);
        if (Number.isFinite(rep) && rep > 1) {
          lines.push('            for(int i = 0; i < ' + rep + '; i++)');
          lines.push('            {');
          lines.push('                ' + eventJava(ev));
          lines.push('            }');
        } else {
          lines.push('            ' + eventJava(ev));
        }
      });
      lines.push('        }');
    });
    lines.push('    }');
    lines.push('}');
    return lines.join('\n');
  }

  /* ---------------- 代码生成：JSON（对齐 符卡JSON设计文档.md 的 schema 1.0） ---------------- */
  function cardToJson(card) {
    const raw = e => (e instanceof Expr ? e.src : e);
    const obj = {
      $schema: 1,
      id: card.id === undefined ? 200 : card.id,
      slug: card.slug || 'generated',
      name: { zh_cn: card.name || '生成符卡', en_us: card.nameEn || '' },
      need_level: card.needLevel | 0 || 1,
      end_time: card.endTime | 0 || 120,
      remove_time: card.removeTime | 0 || 60,
      original_user: card.originalUser || 'OTHER',
      vars: {},
      events: card.events.map(ev => {
        const o = {};
        if (ev.when) o.when = ev.when;
        o.op = ev.op || 'shot';
        if (ev.guard) o.if = raw(ev.guard);
        if (ev.aim && ev.aim !== 'look') o.aim = ev.aim;
        ['yaw', 'pitch', 'dx', 'dy', 'dz', 'firstSpeed', 'limitSpeed', 'accel', 'repeat',
          'way', 'wideAngle', 'distance', 'baseAngle', 'ringSpan', 'num', 'slope', 'rotSpeed', 'rotEnd']
          .forEach(k => { if (ev[k] !== undefined && ev[k] !== '') o[k] = raw(ev[k]); });
        if (ev.gx || ev.gy || ev.gz) o.gravity = { x: raw(ev.gx),
    y: raw(ev.gy), z: raw(ev.gz) };
        if (ev.op && ev.op.indexOf('laser') >= 0) o.laser = ev.laser || {};
        else o.shot = ev.shot || {};
        return o;
      })
    };
    return JSON.stringify(obj, null, 2);
  }

  /* ---------------- 导出 ---------------- */
  const API = {
    // 数学
    v, vadd, vsub, vscale, vlen, vdot, vcross, vnorm,
    fromAngle, yawOf, pitchOf, rotateAround, rotateDefault, gravityZero, GRAVITY_DEFAULT,
    // 常量
    FORMS, FORM_NAMES, SIZE, DAMAGE, COLORS, PALETTE, PALETTE_BRIGHT, LASER_FORMS, WAYS,
    // 表达式
    E, Expr, parseExpr, evalAst, toJava,
    // 展开 / 仿真
    expandEvent, whenMatch, simulate, stateAt, makeShot, stepShot,
    // 代码生成
    cardToJava, cardToJson, eventJava, whenJava
  };

  root.DMK = API;
  if (typeof module !== 'undefined' && module.exports) module.exports = API;
})(typeof window !== 'undefined' ? window : globalThis);
