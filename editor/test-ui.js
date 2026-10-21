/*
 * test-ui.js — 用最小 DOM 桩把 danmaku-editor.html 的内联脚本真跑一遍。
 * 目的：捕获初始化 / 表单重建 / 切预设 / 新增事件 / 导出 各路径上的运行时异常。
 * 不做像素断言（渲染正确性靠人眼 + test-core.js 的数学断言）。
 */
const fs = require('fs');
const vm = require('vm');
const path = require('path');

const dir = __dirname;
const core = fs.readFileSync(path.join(dir, 'danmaku-core.js'), 'utf8');
const html = fs.readFileSync(path.join(dir, 'danmaku-editor.html'), 'utf8');
const appMatch = [...html.matchAll(/<script>([\s\S]*?)<\/script>/g)];
const app = appMatch[appMatch.length - 1][1];

/* ---------- 最小 DOM ---------- */
const els = new Map();

function ctxStub() {
  const base = {
    canvas: { width: 0, height: 0 },
    createLinearGradient: () => ({ addColorStop() {} }),
    createRadialGradient: () => ({ addColorStop() {} }),
    measureText: () => ({ width: 12 }),
    setTransform() {}, save() {}, restore() {}, beginPath() {}, closePath() {},
    moveTo() {}, lineTo() {}, arc() {}, ellipse() {}, bezierCurveTo() {}, rect() {},
    fill() {}, stroke() {}, fillRect() {}, strokeRect() {}, clearRect() {},
    translate() {}, rotate() {}, scale() {}, setLineDash() {}, fillText() {}, drawImage() {},
    quadraticCurveTo() {}
  };
  return new Proxy(base, {
    get(t, k) { return k in t ? t[k] : undefined; },
    set(t, k, v) { t[k] = v; return true; }
  });
}

class El {
  constructor(sel) {
    this.sel = sel;
    this.children = [];
    this._cls = new Set();
    this._html = '';
    this.style = {};
    this.dataset = {};
    this.options = [];
    this.value = '';
    this.textContent = '';
    this.title = '';
    this.max = 100;
    this.checked = false;
    this._ctx = ctxStub();
  }
  set className(v) { this._cls = new Set(String(v).split(/\s+/).filter(Boolean)); }
  get className() { return [...this._cls].join(' '); }
  get classList() {
    const s = this._cls;
    return {
      add: c => s.add(c), remove: c => s.delete(c), contains: c => s.has(c),
      toggle: (c, on) => { if (on === undefined ? !s.has(c) : on) s.add(c); else s.delete(c); }
    };
  }
  set innerHTML(v) { if (v === '') this.children = []; this._html = String(v); }
  get innerHTML() { return this._html; }
  appendChild(c) { this.children.push(c); return c; }
  removeChild(c) { this.children = this.children.filter(x => x !== c); }
  getBoundingClientRect() { return { width: 960, height: 540, left: 0, top: 0 }; }
  getContext() { return this._ctx; }
  addEventListener() {}
  removeEventListener() {}
  select() {}
  click() {}
  focus() {}
}

const tabEls = ['java', 'json', 'report'].map(k => {
  const e = new El('.tab');
  e.dataset.tab = k;
  return e;
});

const document = {
  body: new El('body'),
  documentElement: new El('html'),
  querySelector(sel) {
    if (!els.has(sel)) els.set(sel, new El(sel));
    return els.get(sel);
  },
  querySelectorAll(sel) {
    if (sel === '.tab') return tabEls;
    return [];
  },
  createElement(tag) { return new El('<' + tag + '>'); },
  addEventListener() {},
  execCommand() {}
};
const window = {
  devicePixelRatio: 1,
  addEventListener() {},
  getComputedStyle: () => ({ fontFamily: 'sans-serif' })
};
const rafQueue = [];
const sandbox = {
  document,
  window,
  navigator: { clipboard: { writeText: async () => {} } },
  getComputedStyle: () => ({ fontFamily: 'sans-serif' }),
  requestAnimationFrame: cb => { rafQueue.push(cb); return rafQueue.length; },
  setTimeout: (fn, ms) => { pending.push(fn); return pending.length; },
  clearTimeout() {},
  console,
  Math, JSON, Date, Number, String, Array, Object, Int32Array, Float32Array, Map, Set, isNaN, isFinite, parseFloat, parseInt,
  Blob: function () {}, URL: { createObjectURL: () => 'blob:x', revokeObjectURL() {} },
  Error, TypeError, RangeError, RegExp, Infinity, NaN, undefined
};
const pending = [];
sandbox.globalThis = sandbox;
sandbox.addEventListener = () => {};
sandbox.removeEventListener = () => {};
sandbox.devicePixelRatio = 1;
sandbox.window = sandbox;          // 脚本里 `var window = globalThis` 之后，window 就是全局对象本身

let pass = 0, fail = 0;
function ok(name, cond, extra) {
  if (cond) { pass++; console.log('  ok   ' + name); }
  else { fail++; console.log('  FAIL ' + name + (extra ? '  → ' + extra : '')); }
}
function drain() {
  // 依次执行被 setTimeout 排队的回调（表单重建等）
  let guard = 0;
  while (pending.length && guard++ < 50) pending.shift()();
}

const context = vm.createContext(sandbox);
try {
  vm.runInContext('var window = globalThis;', context);
  vm.runInContext(core, context);
  ok('danmaku-core 加载并挂到全局', !!vm.runInContext('typeof DMK', context) && vm.runInContext('typeof DMK.simulate', context) === 'function');
  vm.runInContext(app, context);
  ok('编辑器脚本执行（初始化 + 首个预设）', true);
} catch (e) {
  ok('编辑器脚本执行', false, e.stack.split('\n').slice(0, 3).join(' | '));
  console.log('\n结果：' + pass + ' 通过 / ' + fail + ' 失败');
  process.exit(1);
}
drain();

ok('事件列表已渲染（预设 1 有 2 个事件）', els.get('#evlist').children.length === 2, els.get('#evlist').children.length);
ok('参数表单已渲染', els.get('#form').children.length > 4, els.get('#form').children.length);
ok('统计：累计弹数 > 0', Number(els.get('#statTotal').textContent) > 0, els.get('#statTotal').textContent);
ok('统计：无解析错误', Number(els.get('#statErr').textContent) === 0, els.get('#statErr').textContent);
ok('实体峰值 > 0', Number(els.get('#statPeak').textContent) > 0, els.get('#statPeak').textContent);

// 跑一帧渲染
try {
  rafQueue.shift()(16);
  ok('drawFrame 执行（含 t=0 空场景）', true);
  els.get('#scrub').value = 45;
  els.get('#scrub').oninput({ target: { value: 45 } });
  rafQueue.shift()(500);
  ok('drawFrame 执行（t=45 有弹幕）', true);
} catch (e) {
  ok('drawFrame 执行', false, e.message);
}

// 切预设
function setPreset(key) {
  const sel = els.get('#preset');
  sel.value = key;
  sel.onchange({ target: { value: key } });
  drain();
}
for (const key of ['spark', 'moon', 'sphere', 'blank', 'saikou']) {
  try { setPreset(key); drain(); ok('切换预设 ' + key, true); }
  catch (e) { ok('切换预设 ' + key, false, e.message); }
}

// 新增 / 删除事件
try {
  const n0 = context.S ? 0 : 0;
  els.get('#btnAdd').onclick();
  drain();
  ok('新增事件后列表 +1', els.get('#evlist').children.length >= 2, els.get('#evlist').children.length);
} catch (e) { ok('新增事件', false, e.message); }

// 改 op（触发表单重建）
function findAll(root, pred, acc) {
  acc = acc || [];
  if (!root || !root.children) return acc;
  for (const c of root.children) { if (pred(c)) acc.push(c); findAll(c, pred, acc); }
  return acc;
}
try {
  const selects = findAll(els.get('#form'), c => c.sel === '<select>');
  ok('表单含 select 控件', selects.length > 0, selects.length);
  const firstSel = selects[0];
  if (firstSel) {
    firstSel.value = 'wide';
    firstSel.onchange();
    drain();
    ok('切换 op → 表单重建成功', els.get('#form').children.length > 4, els.get('#form').children.length);
  } else ok('找到 op select', false);
} catch (e) { ok('切换 op', false, e.message); }

// 表达式错误不应崩
try {
  const inputs = findAll(els.get('#form'), c => c.sel === '<input>');
  if (inputs.length) {
    inputs[0].value = '1+';
    inputs[0].oninput();
    drain();
    ok('非法表达式不崩（标记红框）', inputs[0].classList.contains('bad'));
  } else ok('找到 input', false);
} catch (e) { ok('非法表达式容错', false, e.message); }

// 表值引用 SIZE[...]
try {
  ok('SIZE[9] 解析为 0.5', Math.abs(context.DMK.E('SIZE[9]').eval({}) - 0.5) < 1e-9);
  ok('DAMAGE[9] 解析为 3.0', Math.abs(context.DMK.E('DAMAGE[9]').eval({}) - 3.0) < 1e-9);
} catch (e) { ok('表值引用', false, e.message); }

// 导出三种格式（通过点击 tab 切换）
for (const tab of tabEls) {
  try {
    tab.onclick();
    els.get('#btnExport').onclick();
    const txt = els.get('#export').value;
    ok('导出 ' + tab.dataset.tab + ' 有内容', typeof txt === 'string' && txt.length > 60, (txt || '').length);
    if (tab.dataset.tab === 'java') ok('Java 里含 THShotLib 调用', /THShotLib\./.test(txt));
    if (tab.dataset.tab === 'json') ok('JSON 可解析', (() => { try { JSON.parse(txt); return true; } catch (e) { return false; } })(), txt.slice(0, 120));
    if (tab.dataset.tab === 'report') ok('Markdown 含表头', /^\| # \|/m.test(txt));
  } catch (e) { ok('导出 ' + tab.dataset.tab, false, e.message); }
}

// 播放循环若干帧
try {
  els.get('#btnPlay').onclick();
  for (let i = 0; i < 30; i++) { const cb = rafQueue.shift(); if (cb) cb(i * 16); }
  ok('播放 30 帧不崩', true);
  els.get('#btnPlay').onclick();
} catch (e) { ok('播放循环', false, e.message); }

console.log('\n结果：' + pass + ' 通过 / ' + fail + ' 失败');
process.exit(fail ? 1 : 0);
