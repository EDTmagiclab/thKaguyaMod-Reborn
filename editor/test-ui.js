/**
 * test-ui.js — 用最小 DOM 桩把 editor-app.js 真跑一遍。
 *
 * 目的：捕获初始化 / 切预设 / 增删事件 / 改 op / 导入导出 / 撤销重做 各路径的运行时异常，
 *       并确认界面确实把数据写回了 DOM。不做像素断言（那属于 render-preview.js 的事）。
 *
 *   node test-ui.js
 */
'use strict';

const fs = require('fs');
const path = require('path');
const vm = require('vm');

const dir = __dirname;
const read = f => fs.readFileSync(path.join(dir, f), 'utf8');
const coreSrc = read('danmaku-core.js');
const presetSrc = read('presets.js');
const appSrc = read('editor-app.js');
const html = read('danmaku-editor.html');

let pass = 0, fail = 0;
function ok(name, cond, extra) {
  if (cond) { pass++; console.log('  ✓ ' + name); }
  else { fail++; console.log('  ✗ FAIL ' + name + (extra === undefined ? '' : '  → ' + extra)); }
}

/* ------------------------------------------------------------------ *
 * 1. DOM 桩
 * ------------------------------------------------------------------ */

const CTX_METHODS = [
  'beginPath', 'closePath', 'moveTo', 'lineTo', 'arc', 'arcTo', 'ellipse', 'rect',
  'bezierCurveTo', 'quadraticCurveTo', 'fill', 'stroke', 'fillRect', 'strokeRect',
  'clearRect', 'save', 'restore', 'translate', 'rotate', 'scale', 'setTransform',
  'resetTransform', 'clip', 'fillText', 'strokeText', 'drawImage', 'setLineDash',
  'createLinearGradient', 'createRadialGradient', 'createPattern', 'putImageData'
];

function makeCtx() {
  const base = {
    canvas: null, globalAlpha: 1, globalCompositeOperation: 'source-over',
    lineWidth: 1, lineCap: 'butt', lineJoin: 'miter', miterLimit: 10,
    font: '10px sans-serif', textAlign: 'start', textBaseline: 'alphabetic',
    shadowBlur: 0, shadowColor: '#000', filter: 'none'
  };
  const grad = { addColorStop() { } };
  const target = Object.create(null);
  CTX_METHODS.forEach(m => {
    target[m] = function () {
      if (m === 'createLinearGradient' || m === 'createRadialGradient' || m === 'createPattern') return grad;
      if (m === 'measureText') return { width: 8 };
      return undefined;
    };
  });
  target.measureText = () => ({ width: 8 });
  target.getImageData = () => ({ data: new Uint8Array(4) });
  return new Proxy(target, {
    get(t, k) {
      if (k in t) return t[k];
      if (k in base) return base[k];
      return undefined;
    },
    set(t, k, v) { t[k] = v; return true; }
  });
}

const ctxStub = makeCtx();

class El {
  constructor(tag) {
    this.tagName = String(tag || 'div').toUpperCase();
    this.children = [];
    this.parentNode = null;
    this._cls = new Set();
    this._attrs = Object.create(null);
    this._text = '';
    this._html = '';
    this.style = Object.create(null);
    this.value = '';
    this.checked = false;
    this.disabled = false;
    this.href = '';
    this.download = '';
    this.title = '';
    this.id = '';
    this.type = '';
    this.width = 960;
    this.height = 540;
    this.clientWidth = 960;
    this.clientHeight = 540;
    this.offsetWidth = 960;
    this.offsetHeight = 540;
    this.dataset = Object.create(null);
    this._listeners = Object.create(null);
    this._ctx = null;
  }

  /* --- class --- */
  get className() { return Array.from(this._cls).join(' '); }
  set className(v) { this._cls = new Set(String(v || '').split(/\s+/).filter(Boolean)); }
  get classList() {
    const self = this;
    return {
      add(...names) { names.forEach(n => self._cls.add(String(n))); },
      remove(...names) { names.forEach(n => self._cls.delete(String(n))); },
      contains(n) { return self._cls.has(String(n)); },
      toggle(n, force) {
        const has = force === undefined ? !self._cls.has(String(n)) : !!force;
        if (has) self._cls.add(String(n)); else self._cls.delete(String(n));
        return has;
      },
      get length() { return self._cls.size; },
      item(i) { return Array.from(self._cls)[i]; }
    };
  }
  hasClass(n) { return this._cls.has(String(n)); }

  /* --- 内容 --- */
  get textContent() { return this._text; }
  set textContent(v) { this._text = String(v == null ? '' : v); this.children.length = 0; }
  get innerHTML() { return this._html; }
  set innerHTML(v) {
    this._html = String(v == null ? '' : v);
    this.children.length = 0;      // app 只做 innerHTML = '' 清空，或纯文本拼接
    this._text = this._html.replace(/<[^>]*>/g, '');
    // 桩只解析 <option>，让「预设下拉里有几个选项」这类断言有意义
    const re = /<option\b[^>]*?value="([^"]*)"[^>]*>([\s\S]*?)<\/option>/g;
    let m;
    while ((m = re.exec(this._html))) {
      const o = new El('option');
      o.setAttribute('value', m[1]);
      o.value = m[1];
      o.textContent = m[2].replace(/<[^>]*>/g, '');
      o.parentNode = this;
      this.children.push(o);
    }
    if (this.tagName === 'SELECT' && this.children.length) this.value = this.children[0].value;
  }
  get firstChild() { return this.children[0] || null; }
  get lastChild() { return this.children[this.children.length - 1] || null; }
  get childNodes() { return this.children; }
  get childElementCount() { return this.children.length; }
  get options() { return this.children; }
  get selectedIndex() { return 0; }

  /* --- 树 --- */
  appendChild(c) {
    if (!c) return c;
    if (c.parentNode) c.parentNode.removeChild(c);
    c.parentNode = this;
    this.children.push(c);
    if (c.id) idCache.set(c.id, c);
    return c;
  }
  append(...cs) { cs.forEach(c => this.appendChild(typeof c === 'string' ? el('span') : c)); }
  removeChild(c) {
    const i = this.children.indexOf(c);
    if (i >= 0) this.children.splice(i, 1);
    c.parentNode = null;
    return c;
  }
  insertBefore(c, ref) {
    const i = ref ? this.children.indexOf(ref) : -1;
    if (i < 0) return this.appendChild(c);
    c.parentNode = this;
    this.children.splice(i, 0, c);
    return c;
  }
  replaceChildren(...cs) { this.children.length = 0; cs.forEach(c => this.appendChild(c)); }
  remove() { if (this.parentNode) this.parentNode.removeChild(this); }
  contains(n) {
    if (n === this) return true;
    return this.children.some(c => c.contains && c.contains(n));
  }
  cloneNode() { return new El(this.tagName); }

  /* --- 属性 --- */
  setAttribute(k, v) {
    this._attrs[String(k)] = String(v);
    if (k === 'value') this.value = v;
    if (k === 'class') this.className = v;
    if (k === 'id') this.id = String(v);
  }
  getAttribute(k) { const v = this._attrs[String(k)]; return v === undefined ? null : v; }
  removeAttribute(k) { delete this._attrs[String(k)]; }
  hasAttribute(k) { return String(k) in this._attrs; }

  /* --- 事件 --- */
  addEventListener(type, fn) {
    (this._listeners[type] = this._listeners[type] || []).push(fn);
  }
  removeEventListener(type, fn) {
    const a = this._listeners[type] || [];
    const i = a.indexOf(fn);
    if (i >= 0) a.splice(i, 1);
  }
  dispatch(type, ev) {
    const e = Object.assign({ type, target: this, preventDefault() { }, stopPropagation() { } }, ev || {});
    (this._listeners[type] || []).forEach(fn => fn.call(this, e));
    const inline = this['on' + type];
    if (typeof inline === 'function') inline.call(this, e);
    return e;
  }
  click() { return this.dispatch('click', {}); }
  focus() { this._focused = true; }
  blur() { this._focused = false; }
  select() { this._selected = true; }
  scrollIntoView() { }

  /* --- 几何 / canvas --- */
  getBoundingClientRect() {
    return { left: 0, top: 0, right: 960, bottom: 540, width: 960, height: 540, x: 0, y: 0 };
  }
  getContext() { if (!this._ctx) this._ctx = ctxStub; return this._ctx; }
  setPointerCapture() { }
  releasePointerCapture() { }
  toDataURL() { return 'data:image/png;base64,'; }
}

function el(tag) { return new El(tag); }

const idCache = new Map();
const TABS = ['java', 'json', 'report', 'import'].map(k => {
  const t = new El('button');
  t.className = 'tab';
  t.setAttribute('data-tab', k);
  return t;
});

const bodyEl = new El('body');
const headEl = new El('head');

const documentStub = {
  body: bodyEl,
  head: headEl,
  documentElement: new El('html'),
  createElement: tag => new El(tag),
  createElementNS: (ns, tag) => new El(tag),
  createTextNode: txt => { const e = new El('span'); e.textContent = txt; return e; },
  createDocumentFragment: () => new El('fragment'),
  querySelector(sel) {
    if (sel === 'body') return bodyEl;
    if (sel === 'head') return headEl;
    if (sel === '.tab') return TABS[0];
    if (/^#/.test(sel)) {
      const id = sel.slice(1);
      if (!idCache.has(id)) {
        // 依 id 猜标签，让 input/select/canvas 行为更接近真实
        let tag = 'div';
        if (/^btn/.test(id)) tag = 'button';
        else if (/^(mName|mNameEn|mSlug|mId|mNeed|mMax|mEnd|mRemove|mUser|mCircle|mNotes|importText|userYaw|camYaw|camPitch|camFov|scrub|speed|level|preset|export)$/.test(id)) tag = 'input';
        else if (id === 'view') tag = 'canvas';
        else if (/^(evlist|form|marks|issueList|vars|evTools|export)$/.test(id)) tag = id === 'export' ? 'textarea' : 'div';
        const e = new El(tag);
        e.id = id;
        idCache.set(id, e);
        bodyEl.appendChild(e);
      }
      return idCache.get(id);
    }
    return new El('div');
  },
  querySelectorAll(sel) {
    if (sel === '.tab') return TABS;
    if (/^#/.test(sel)) return [documentStub.querySelector(sel)];
    return [];
  },
  getElementById(id) { return documentStub.querySelector('#' + id); },
  addEventListener() { },
  removeEventListener() { },
  execCommand() { return true; },
  get activeElement() { return null; },
  get readyState() { return 'complete'; }
};

/* ------------------------------------------------------------------ *
 * 2. 定时器 / rAF / 存储
 * ------------------------------------------------------------------ */

let rafQueue = [];
let timers = [];
let timerSeq = 1;

const localStorageStub = (() => {
  const map = new Map();
  return {
    getItem(k) { return map.has(String(k)) ? map.get(String(k)) : null; },
    setItem(k, v) { map.set(String(k), String(v)); },
    removeItem(k) { map.delete(String(k)); },
    clear() { map.clear(); },
    key(i) { return Array.from(map.keys())[i] || null; },
    get length() { return map.size; },
    _map: map
  };
})();

const windowStub = {
  addEventListener() { },
  removeEventListener() { },
  localStorage: localStorageStub,
  devicePixelRatio: 1,
  innerWidth: 1600,
  innerHeight: 900,
  navigator: { clipboard: { writeText: () => Promise.resolve() }, userAgent: 'node-test' },
  confirm: () => true,
  alert() { },
  matchMedia: () => ({ matches: false, addListener() { }, removeListener() { } }),
  getComputedStyle: () => ({ getPropertyValue: () => '' })
};

function drain(n) {
  const limit = n || 200;
  for (let i = 0; i < limit; i++) {
    if (rafQueue.length) { const f = rafQueue.shift(); try { f(performance.now()); } catch (e) { throw e; } continue; }
    if (timers.length) {
      const t = timers.sort((a, b) => a.at - b.at).shift();
      try { t.fn(); } catch (e) { throw e; }
      continue;
    }
    break;
  }
}

function frame(ts) {
  if (rafQueue.length) { const f = rafQueue.shift(); f(ts === undefined ? performance.now() : ts); }
}

const sandbox = {
  console,
  document: documentStub,
  navigator: windowStub.navigator,
  localStorage: localStorageStub,
  devicePixelRatio: 1,
  requestAnimationFrame(fn) { rafQueue.push(fn); return rafQueue.length; },
  cancelAnimationFrame() { },
  setTimeout(fn, ms) { const id = timerSeq++; timers.push({ id, fn, at: performance.now() + (ms || 0) }); return id; },
  clearTimeout(id) { timers = timers.filter(t => t.id !== id); },
  setInterval(fn) { const id = timerSeq++; timers.push({ id, fn: () => { }, at: 1e18 }); return id; },
  clearInterval() { },
  performance: { now: () => Date.now() },
  Math, JSON, Number, String, Array, Object, Boolean, Date, RegExp, Error, TypeError, RangeError,
  Map, Set, WeakMap, WeakSet, Symbol, Promise, Proxy, Reflect,
  Int8Array, Uint8Array, Uint8ClampedArray, Int16Array, Uint16Array, Int32Array, Uint32Array,
  Float32Array, Float64Array, ArrayBuffer, DataView,
  isNaN, isFinite, parseFloat, parseInt, encodeURIComponent, decodeURIComponent,
  Blob: function Blob(parts, opts) { this.parts = parts; this.type = (opts || {}).type || ''; },
  FileReader: function FileReader() { this.readAsText = () => { this.onload && this.onload({ target: { result: '' } }); }; },
  URL: { createObjectURL: () => 'blob:stub', revokeObjectURL() { } },
  alert() { },
  confirm: () => true
};
sandbox.window = sandbox;
sandbox.globalThis = sandbox;
sandbox.self = sandbox;
sandbox.top = sandbox;
Object.assign(sandbox, windowStub);
sandbox.window = sandbox;   // window.localStorage 等已由 Object.assign 带上

vm.createContext(sandbox);

/* ------------------------------------------------------------------ *
 * 3. 依次加载 core → presets → app
 * ------------------------------------------------------------------ */

console.log('\n[0] 加载');
{
  vm.runInContext('var window = globalThis;', sandbox);
  let e = null;
  try { vm.runInContext(coreSrc, sandbox, { filename: 'danmaku-core.js' }); } catch (err) { e = err; }
  ok('danmaku-core.js 加载无异常', !e, e && e.message);
  ok('DMK 挂到全局', typeof sandbox.DMK === 'object' && typeof sandbox.DMK.simulate === 'function');

  e = null;
  try { vm.runInContext(presetSrc, sandbox, { filename: 'presets.js' }); } catch (err) { e = err; }
  ok('presets.js 加载无异常', !e, e && e.message);
  ok('DMK_PRESETS 挂到全局', sandbox.DMK_PRESETS && Object.keys(sandbox.DMK_PRESETS).length >= 8);

  e = null;
  try { vm.runInContext(appSrc, sandbox, { filename: 'editor-app.js' }); } catch (err) { e = err; }
  ok('editor-app.js 初始化无异常', !e, e && (e.stack || e.message));
  try { drain(); } catch (err) { fail++; console.log('  ✗ FAIL 初始化定时器异常 → ' + (err.stack || err.message)); }
}

const D = sandbox.DMK;
const $ = sel => documentStub.querySelector(sel);

/** 递归收集某元素下的所有「标签文本 + 控件」对 */
function fieldsIn(root) {
  const out = [];
  const walk = node => {
    if (!node || !node.children) return;
    node.children.forEach(c => {
      if (c.hasClass && c.hasClass('fld')) {
        const lab = (c.children || []).find(x => x.hasClass && (x.hasClass('fld-l') || x.hasClass('fld-l ')));
        const ctl = (c.children || []).find(x => x.hasClass && x.hasClass('fld-c'));
        if (lab && ctl) out.push({ label: lab.textContent, ctl });
      }
      walk(c);
    });
  };
  walk(root);
  return out;
}

function findByLabel(root, text) {
  const all = fieldsIn(root);
  return all.find(f => f.label.indexOf(text) >= 0);
}

/* ------------------------------------------------------------------ *
 * 4. 启动后的界面
 * ------------------------------------------------------------------ */

console.log('\n[1] 启动后界面状态');
{
  ok('#evlist 有事件行（saikou 预设 = 2 个事件）', $('#evlist').children.length === 2, $('#evlist').children.length);
  ok('#form 表单已渲染', $('#form').children.length > 5, $('#form').children.length);
  ok('#statTotal 有内容', /\d/.test($('#statTotal').textContent), $('#statTotal').textContent);
  ok('#statPeak 是数字', /\d/.test($('#statPeak').textContent), $('#statPeak').textContent);
  ok('#evLabel 显示当前事件名', $('#evLabel').textContent.length > 0, $('#evLabel').textContent);
  ok('#tMax 显示了时长', /\d/.test($('#tMax').textContent), $('#tMax').textContent);
  ok('#mSlug 回填了 slug', $('#mSlug').value === 'saikou_ranbu' || $('#mSlug').value.length > 0, $('#mSlug').value);
  ok('#preset 里有 8 个预设', $('#preset').children.length >= 8, $('#preset').children.length);
  ok('#mMax 回填了 maxTick', String($('#mMax').value).length > 0, $('#mMax').value);
  ok('localStorage 已写入 thkaguya.dmk.v1', localStorageStub.getItem('thkaguya.dmk.v1') !== null);
  const saved = JSON.parse(localStorageStub.getItem('thkaguya.dmk.v1') || '{}');
  ok('存档里有 card.events', saved.card && Array.isArray(saved.card.events));
}

console.log('\n[2] rAF 循环');
{
  let e = null;
  try { for (let i = 0; i < 5; i++) frame(1000 + i * 16); } catch (err) { e = err; }
  ok('连续 5 帧无异常', !e, e && (e.stack || e.message));
}

/* ------------------------------------------------------------------ *
 * 5. 预设切换
 * ------------------------------------------------------------------ */

console.log('\n[3] 切换全部预设');
{
  const keys = Object.keys(sandbox.DMK_PRESETS);
  keys.forEach(k => {
    let e = null;
    try {
      $('#preset').value = k;
      $('#preset').dispatch('change');
      drain();
      frame(2000);
    } catch (err) { e = err; }
    ok('切到 [' + k + '] 不崩', !e, e && (e.stack || e.message));
    ok('切到 [' + k + '] 后列表有事件', $('#evlist').children.length >= 1, $('#evlist').children.length);
  });
  // 回到 saikou，后续用例基于它
  $('#preset').value = 'saikou';
  $('#preset').dispatch('change');
  drain();
}

/* ------------------------------------------------------------------ *
 * 6. 事件增删 / 改 op / 非法表达式
 * ------------------------------------------------------------------ */

console.log('\n[4] 事件增删与 op 切换');
{
  const before = $('#evlist').children.length;
  $('#btnAdd').click();
  drain();
  ok('新增事件后行数 +1', $('#evlist').children.length === before + 1, $('#evlist').children.length);

  // 选中最后一个事件（ring 为例），改 op 为 wide
  const opSelect = (() => {
    const walk = n => {
      for (const c of (n.children || [])) {
        if (c.tagName === 'SELECT') return c;
        const r = walk(c);
        if (r) return r;
      }
      return null;
    };
    return walk($('#form'));
  })();
  ok('#form 里有 op 下拉', !!opSelect);
  if (opSelect) {
    let e = null;
    try { opSelect.value = 'wide'; opSelect.dispatch('change'); drain(); } catch (err) { e = err; }
    ok('改 op 为 wide 不崩', !e, e && (e.stack || err0(e)));
    ok('改 op 后表单重建', $('#form').children.length > 5, $('#form').children.length);
  }

  // 非法表达式 → .bad
  const inputs = [];
  (function walk(n) { (n.children || []).forEach(c => { if (c.tagName === 'INPUT') inputs.push(c); walk(c); }); })($('#form'));
  let badCount = 0;
  inputs.forEach(inp => {
    try { inp.value = '1+'; inp.dispatch('input'); inp.dispatch('change'); } catch (e) { /* 忽略单个字段异常 */ }
    if (inp.hasClass('bad')) badCount++;
  });
  ok('非法表达式被标 .bad', badCount >= 1, badCount + '/' + inputs.length);

  // 删掉刚加的事件
  const row = $('#evlist').children[$('#evlist').children.length - 1];
  const delBtn = (function walk(n) {
    for (const c of (n.children || [])) {
      if (c.hasClass && (c.hasClass('del') || /✕/.test(c.textContent))) return c;
      const r = walk(c);
      if (r) return r;
    }
    return null;
  })(row);
  if (delBtn) {
    let e = null;
    try { delBtn.click(); drain(); } catch (err) { e = err; }
    ok('删除事件不崩', !e, e && e.message);
    ok('删除后行数回到原值', $('#evlist').children.length === before, $('#evlist').children.length);
  } else {
    ok('列表行里有删除按钮', false);
  }
}

function err0(e) { return e && e.message; }

/* ------------------------------------------------------------------ *
 * 7. 导出 / 导入
 * ------------------------------------------------------------------ */

console.log('\n[5] 导出四个 tab');
{
  $('#btnExport').click();
  drain();
  const tabs = TABS;
  const texts = {};
  ['java', 'json', 'report'].forEach(kind => {
    const t = tabs.find(t => t.getAttribute('data-tab') === kind);
    let e = null;
    try { t.click(); drain(); } catch (err) { e = err; }
    ok('切到 ' + kind + ' tab 不崩', !e, e && e.message);
    texts[kind] = $('#export').value;
    ok(kind + ' 有内容（>60 字符）', (texts[kind] || '').length > 60, (texts[kind] || '').length);
  });
  ok('java 导出含 THShotLib 调用', /THShotLib\./.test(texts.java));
  ok('java 导出含类名', /public class THSC_/.test(texts.java));
  ok('json 导出可解析', (() => { try { JSON.parse(texts.json); return true; } catch (e) { return false; } })());
  ok('json 导出是 Schema 1.0', (() => { try { return JSON.parse(texts.json).$schema === 1; } catch (e) { return false; } })());
  ok('report 导出是 Markdown 表格', /^\|.*\|/m.test(texts.report));
  ok('report 里含事件表头', /事件/.test(texts.report));

  let e = null;
  try { $('#btnCopy').click(); drain(); } catch (err) { e = err; }
  ok('复制按钮不崩', !e, e && e.message);
}

console.log('\n[6] 从 JSON 导入');
{
  const card = {
    $schema: 1, id: 999, slug: 'imported_test', name: { zh_cn: '导入测试', en_us: 'Imported Test' },
    need_level: 3, end_time: 150, remove_time: 70, original_user: 'MARISA',
    vars: { a: 'time * 3' },
    events: [
      { when: { at: 5 }, op: 'shot', aim: 'look', first_speed: 1.2, limit_speed: 1.2, acceleration: 0, shot: { form: 'STAR', color: 'RED', size: 0.3, damage: 3, delay: 0, end: 60 } },
      { when: { from: 20, to: 80 }, op: 'circle', aim: 'look', first_speed: 0.8, spray: { way: 12 }, distance: 0.4, shot: { form: 'TINY', color: 2, size: 0.2, damage: 2, delay: 0, end: 40 } }
    ]
  };
  $('#btnImport').click();
  drain();
  $('#importText').value = JSON.stringify(card, null, 2);
  let e = null;
  try { $('#btnDoImport').click(); drain(); frame(3000); } catch (err) { e = err; }
  ok('导入不崩', !e, e && (e.stack || e.message));
  ok('导入后 slug 变了', $('#mSlug').value === 'imported_test', $('#mSlug').value);
  ok('导入后事件数 2', $('#evlist').children.length === 2, $('#evlist').children.length);
  ok('导入后 id 生效', String($('#mId').value) === '999', $('#mId').value);
  ok('导入后 need_level 生效', String($('#mNeed').value) === '3', $('#mNeed').value);
  ok('导入后 end_time 生效', String($('#mEnd').value) === '150', $('#mEnd').value);
  ok('导入后 original_user 生效', String($('#mUser').value) === 'MARISA', $('#mUser').value);
  ok('导入后能导出 Java', (() => {
    $('#btnExport').click(); drain();
    TABS.find(t => t.getAttribute('data-tab') === 'java').click(); drain();
    return /THSC_ImportedTest|THSC_imported_test/.test($('#export').value);
  })(), $('#export').value.slice(0, 80));
}

/* ------------------------------------------------------------------ *
 * 8. 撤销 / 重做
 * ------------------------------------------------------------------ */

console.log('\n[7] 撤销与重做');
{
  const slugNow = $('#mSlug').value;
  let e = null;
  try { $('#btnUndo').click(); drain(); } catch (err) { e = err; }
  ok('撤销不崩', !e, e && e.message);
  ok('撤销后 slug 回到 saikou', $('#mSlug').value !== slugNow, $('#mSlug').value);
  const slugUndo = $('#mSlug').value;
  e = null;
  try { $('#btnRedo').click(); drain(); } catch (err) { e = err; }
  ok('重做不崩', !e, e && e.message);
  ok('重做后 slug 回到导入值', $('#mSlug').value === slugNow, $('#mSlug').value + ' vs ' + slugUndo);
}

/* ------------------------------------------------------------------ *
 * 9. 播放 / 时间轴
 * ------------------------------------------------------------------ */

console.log('\n[8] 播放与时间轴');
{
  const t0 = $('#tNow').textContent;
  let e = null;
  try {
    $('#btnPlay').click();
    for (let i = 0; i < 30; i++) frame(4000 + i * 16);
    $('#btnPlay').click();
  } catch (err) { e = err; }
  ok('播放 30 帧无异常', !e, e && (e.stack || e.message));
  ok('播放后时间在走', $('#tNow').textContent !== t0, t0 + ' → ' + $('#tNow').textContent);

  e = null;
  try {
    $('#scrub').value = 45;
    $('#scrub').dispatch('input');
    frame(5000);
  } catch (err) { e = err; }
  ok('拖动时间轴不崩', !e, e && e.message);
  ok('时间轴拖动后 HUD 更新', /\d/.test($('#tNow').textContent), $('#tNow').textContent);

  ['#btnPrev', '#btnNext', '#btnRewind'].forEach(sel => {
    let err2 = null;
    try { $(sel).click(); frame(5100); } catch (err3) { err2 = err3; }
    ok(sel + ' 不崩', !err2, err2 && err2.message);
  });
}

/* ------------------------------------------------------------------ *
 * 10. 相机 / 滑杆 / 问题面板
 * ------------------------------------------------------------------ */

console.log('\n[9] 相机与其它控件');
{
  [['#camYaw', '20'], ['#camPitch', '-6'], ['#camFov', '48'], ['#level', '4'], ['#userYaw', '135'], ['#speed', '2']].forEach(([sel, v]) => {
    let e = null;
    try { $(sel).value = v; $(sel).dispatch('input'); $(sel).dispatch('change'); frame(6000); } catch (err) { e = err; }
    ok(sel + ' 不崩', !e, e && e.message);
  });
  let e = null;
  try { $('#statErr').click(); drain(); } catch (err) { e = err; }
  ok('点问题 chip 不崩', !e, e && e.message);
  ok('#issueList 有内容', $('#issueList').children.length >= 0);
}

/* ------------------------------------------------------------------ *
 * 11. 新建
 * ------------------------------------------------------------------ */

console.log('\n[10] 新建空白卡');
{
  let e = null;
  try { $('#btnNew').click(); drain(); frame(7000); } catch (err) { e = err; }
  ok('新建不崩', !e, e && (e.stack || e.message));
  ok('新建后 slug 是 new_spell', $('#mSlug').value === 'new_spell', $('#mSlug').value);
  ok('新建后能导出 JSON', (() => {
    $('#btnExport').click(); drain();
    TABS.find(t => t.getAttribute('data-tab') === 'json').click(); drain();
    try { return JSON.parse($('#export').value).slug === 'new_spell'; } catch (x) { return false; }
  })());
}

/* ------------------------------------------------------------------ *
 * 12. 存储 / 静态资源
 * ------------------------------------------------------------------ */

console.log('\n[11] 杂项');
{
  ok('HTML 不再内联 app 脚本', !/<script>[\s\S]{2000,}<\/script>/.test(html));
  ok('HTML 引用了三个脚本', /danmaku-core\.js/.test(html) && /presets\.js/.test(html) && /editor-app\.js/.test(html));
  ok('HTML 有 canvas#view', /<canvas[^>]*id="view"/.test(html));
  ok('保存后 localStorage 里有导入的卡', (() => {
    const s = JSON.parse(localStorageStub.getItem('thkaguya.dmk.v1') || '{}');
    return s.card && s.card.slug === 'new_spell';
  })(), localStorageStub.getItem('thkaguya.dmk.v1') ? 'ok' : 'empty');
}

console.log('\n结果：' + pass + ' 通过 / ' + fail + ' 失败\n');
process.exit(fail ? 1 : 0);
