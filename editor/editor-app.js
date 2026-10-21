/*
 * editor-app.js — THKaguya 可视化弹幕编辑器的界面层。
 *
 * 依赖（都是普通 <script>，没有构建步骤）：
 *   danmaku-core.js  → window.DMK      仿真 / 表达式 / Java 与 Schema 1.0 双向转换
 *   presets.js       → window.DMK_PRESETS
 *
 * 设计原则：
 *   · 内部模型保持扁平的 camelCase（UI 直接读写它）；
 *   · 只有在「导出/导入 JSON」时才经过 danmaku-core 的 Schema 1.0 适配层；
 *   · 所有会改变符卡的操作都先 pushUndo()，所以 Ctrl+Z 永远安全。
 */
(function () {
  'use strict';

  const D = window.DMK;
  const PRESETS = window.DMK_PRESETS || {};
  const LS_KEY = 'thkaguya.dmk.v1';
  const TICK_MS = 50;            // 20 tick/s，与原版服务器的节拍感知一致
  const EYE = 1.62;

  const $ = sel => document.querySelector(sel);
  const clone = o => JSON.parse(JSON.stringify(o));
  const esc = s => String(s).replace(/[&<>"]/g, c => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;' }[c]));
  const isBlank = v => v === undefined || v === null || v === '';

  function el(tag, cls, txt) {
    const e = document.createElement(tag);
    if (cls) e.className = cls;
    if (txt !== undefined) e.textContent = txt;
    return e;
  }

  /* ============================ 状态 ============================ */

  const S = {
    card: null,
    sim: { shots: [], actions: [], errors: [], spawnLog: [], maxTick: 120 },
    issues: [],
    sel: -1,
    tick: 0,
    playing: false,
    speed: 1,
    cam: { yaw: 0, pitch: 12, fov: 62 },
    acc: 0,
    last: 0,
    curTab: 'java',
    presetKey: '',
    undo: [],
    redo: [],
    lastCommit: 0,
    lastSnap: null,     // 上一次「已落定」的卡快照，pushUndo 拍的是它（不能拍改完之后的 S.card）
    snapDirty: false,
    refreshTimer: 0
  };

  function blankCard() {
    return {
      id: 200, name: '新符卡', nameEn: 'New Spell Card', slug: 'new_spell',
      needLevel: 1, endTime: 120, removeTime: 60, originalUser: 'OTHER',
      circleColor: '', maxTick: 120, userYaw: 0, level: 2, notes: '',
      vars: {}, specials: {}, events: []
    };
  }

  function normalizeCard(c) {
    const d = blankCard();
    if (!c || typeof c !== 'object') return d;
    for (const k in d) if (c[k] === undefined) c[k] = d[k];
    if (!c.vars || typeof c.vars !== 'object') c.vars = {};
    if (!c.specials || typeof c.specials !== 'object') c.specials = {};
    if (!Array.isArray(c.events)) c.events = [];
    c.events.forEach(ev => { if (!ev.when) ev.when = { kind: 'at', a: 0 }; });
    if (!c.maxTick) c.maxTick = 120;
    return c;
  }

  /* ============================ 持久化 ============================ */

  function store() {
    try { return window.localStorage || null; } catch (e) { return null; }
  }
  function save() {
    // 每次「编辑动作收尾」都会走到这里：把 lastSnap 刷成当前状态，
    // 下一次 pushUndo 拍到的才是「这次编辑之前」的卡。
    if (S.snapDirty) { S.lastSnap = JSON.stringify(S.card); S.snapDirty = false; }
    const ls = store();
    if (!ls) return;
    try {
      ls.setItem(LS_KEY, JSON.stringify({ card: S.card, cam: S.cam, tick: S.tick, presetKey: S.presetKey }));
    } catch (e) { /* 隐私模式 / 配额满：忽略 */ }
  }
  function restore() {
    const ls = store();
    if (!ls) return null;
    try {
      const raw = ls.getItem(LS_KEY);
      return raw ? JSON.parse(raw) : null;
    } catch (e) { return null; }
  }

  /* ============================ 撤销 / 重做 ============================ */

  function syncUndoBtns() {
    $('#btnUndo').disabled = !S.undo.length;
    $('#btnRedo').disabled = !S.redo.length;
  }

  /**
   * 700ms 内的连续编辑合并成一次撤销点（打字不会一个字符一个撤销点）。
   *
   * 关键点：所有编辑路径都是「先改 S.card，再 commit()」，所以这里不能直接拍 S.card ——
   * 那拍到的已经是改完的状态，第一次 Ctrl+Z 什么都不会发生。改成拍 S.lastSnap
   * （上一次「已落定」的快照），并在 save() 里刷新它。
   */
  function pushUndo(force) {
    const now = Date.now();
    const snap = S.lastSnap === null ? JSON.stringify(S.card) : S.lastSnap;
    if (!force && now - S.lastCommit < 700 && S.undo.length) {
      S.lastCommit = now;
      S.redo.length = 0;      // 撤销后继续输入，旧的重做链必须作废
      S.snapDirty = true;
      syncUndoBtns();
      save();
      return;
    }
    S.undo.push(snap);
    if (S.undo.length > 120) S.undo.shift();
    S.redo.length = 0;
    S.lastCommit = now;
    S.snapDirty = true;       // 本次编辑结束（save()）后再把 lastSnap 刷成新状态
    syncUndoBtns();
    save();
  }

  function undo() {
    if (!S.undo.length) { toast('没有可撤销的操作'); return; }
    S.redo.push(JSON.stringify(S.card));
    S.card = normalizeCard(JSON.parse(S.undo.pop()));
    S.sel = Math.min(S.sel, S.card.events.length - 1);
    afterCardSwap();
    toast('已撤销');
  }

  function redo() {
    if (!S.redo.length) { toast('没有可重做的操作'); return; }
    S.undo.push(JSON.stringify(S.card));
    S.card = normalizeCard(JSON.parse(S.redo.pop()));
    S.sel = Math.min(S.sel, S.card.events.length - 1);
    afterCardSwap();
    toast('已重做');
  }

  function afterCardSwap() {
    S.lastSnap = JSON.stringify(S.card);
    S.snapDirty = false;
    // 换卡后不要延续「700ms 合并窗口」——否则紧接着的第一次编辑会被并进
    // 换卡之前那个撤销点里，按 Ctrl+Z 会越过它
    S.lastCommit = 0;
    // 撤销/重做/导入后的卡不再对应任何预设，下拉框回落（否则显示的是另一张卡的名字）
    S.presetKey = '';
    const sel = $('#preset');
    if (sel) sel.value = '';
    syncUndoBtns();
    syncMeta();
    S.tick = Math.min(S.tick, S.card.maxTick | 0 || 120);
    refresh();
    renderVars();       // 变量面板也得重画，否则删掉的变量会「复活」写回 S.card.vars
    rebuildForm();
    jumpToFirstAlive();
    save();
  }

  /** 换卡后如果当前帧是空的，跳到第一个「画面上有东西」的 tick —— 否则新用户会以为预览坏了。 */
  function firstAliveTick() {
    const max = Math.max(1, S.sim.maxTick | 0 || 120);
    for (let t = 0; t <= max; t++) {
      if (D.stateAt(S.sim, t).some(s => s.vis !== false)) return t;
    }
    return 0;
  }
  function jumpToFirstAlive() {
    if (D.stateAt(S.sim, S.tick).some(s => s.vis !== false)) return;
    const t = firstAliveTick();
    S.tick = t;
    const scrub = $('#scrub');
    if (scrub) scrub.value = String(t);
    updateHud();
    drawFrame();
  }

  /* ============================ 事件模型 ============================ */

  function newEvent(op) {
    op = op || 'ring';
    const info = D.OP_INFO[op] || D.OP_INFO.shot;
    const ev = { when: { kind: 'at', a: 0 }, op: op, aim: 'look' };
    if (info.data === 'shot') {
      ev.shot = {
        form: 'FORM_CRYSTAL', color: 'RED',
        size: 'SIZE[FORM_CRYSTAL]', damage: 'DAMAGE[FORM_CRYSTAL]',
        delay: 0, end: 80
      };
    }
    if (info.data === 'laser') {
      ev.laser = { form: 'NORMAL', color: 'BLUE', width: 0.2, length: 20, damage: 4, delay: 0, end: 60 };
    }
    if (info.data) {
      ev.firstSpeed = 0.8;
      ev.limitSpeed = 0.8;
      ev.accel = 0;
    }
    if (info.batch === 'spread') ev.spread = { way: 5, span: 60, base: 0 };
    if (info.batch === 'spray') ev.spray = { way: 8, span: 360, base: 0 };
    if (info.distance) ev.distance = 0.7;
    if (info.slope && (op === 'sphere' || op === 'sphere_laser')) ev.slope = 1;
    if (info.ringSpan) ev.ringSpan = 360;
    if (info.random) { ev.num = 8; ev.ringSpan = 360; }
    if (op === 'sphere_laser') ev.slope = 0;
    if (op === 'nuclear') { ev.slope = 1; ev.rotate = { x: 0, y: 1, z: 0 }; ev.spin = 4; ev.spinEnd = 70; }
    if (op === 'ring' || op === 'circle' || op === 'wide' || op === 'sphere' ||
        op === 'random_ring' || op === 'ring_laser' || op === 'circle_laser' ||
        op === 'wide_laser' || op === 'sphere_laser' || op === 'random_ring_laser' || op === 'laser_b') {
      ev.spin = 0;
      ev.spinEnd = 9999;
    }
    if (op === 'laser_b') ev.attach = { to: 'user', length: 1.0, yOffset: 'bbHeight * 0.5' };
    if (op === 'spawn') ev.entity = 'thkaguya:mini_hakkero';
    if (op === 'remove_danmaku') { ev.range = 4; ev.target = 'All'; ev.dropBonus = false; }
    if (op === 'explosion_effect') ev.size = 2;
    return ev;
  }

  /** 换 op 时保留触发条件与朝向，只重建数据字段。 */
  function changeOp(ev, op) {
    const keep = {};
    ['when', 'guard', 'note', 'anchor', 'aim', 'yaw', 'pitch', 'dir', 'vec', 'repeat', 'sound'].forEach(k => {
      if (ev[k] !== undefined) keep[k] = clone(ev[k]);
    });
    Object.keys(ev).forEach(k => delete ev[k]);
    const fresh = newEvent(op);
    Object.keys(fresh).forEach(k => { ev[k] = fresh[k]; });
    Object.keys(keep).forEach(k => { if (keep[k] !== undefined) ev[k] = keep[k]; });
    ev.op = op;
  }

  /* ============================ 仿真 / 刷新 ============================ */

  function recompute() {
    try {
      S.sim = D.simulate(S.card);
    } catch (e) {
      S.sim = {
        shots: [], actions: [],
        errors: [{ tick: 0, op: '-', msg: e.message, idx: -1 }],
        spawnLog: [], maxTick: S.card.maxTick | 0 || 120
      };
    }
    try { S.issues = D.validateCard(S.card, S.sim) || []; }
    catch (e) { S.issues = [{ level: 'error', where: 'validate', msg: e.message }]; }
  }

  function refresh() {
    recompute();
    const max = Math.max(1, S.sim.maxTick | 0 || 120);
    const scrub = $('#scrub');
    scrub.max = String(max);
    if (S.tick > max) S.tick = max;
    renderStats();
    renderMarks();
    renderIssues();
    renderList();
    updateHud();
    drawFrame();
  }

  function scheduleRefresh() {
    if (S.refreshTimer) return;
    S.refreshTimer = setTimeout(() => {
      S.refreshTimer = 0;
      refresh();
    }, 70);
  }

  /** 表单里的普通输入：不进表单重建（避免抢焦点），只更新仿真与列表。 */
  function commit() {
    pushUndo();
    recompute();
    renderStats();
    renderMarks();
    renderIssues();
    renderList();
    updateHud();
    drawFrame();
    save();
  }

  function rebuildForm() {
    renderForm();
  }

  function computePeak() {
    const max = Math.max(1, S.sim.maxTick | 0 || 120);
    const diff = new Int32Array(max + 2);
    let total = 0;
    (S.sim.shots || []).forEach(s => {
      const life = (s.delay | 0) + (s.end | 0) + 1;
      const a = Math.max(0, s.born | 0);
      const b = Math.min(max, (s.born | 0) + life);
      if (b >= a) {
        diff[a]++;
        if (b + 1 <= max + 1) diff[b + 1]--;
      }
      total++;
    });
    let cur = 0, peak = 0;
    for (let t = 0; t <= max; t++) {
      cur += diff[t];
      if (cur > peak) peak = cur;
    }
    return { peak: peak, total: total };
  }

  function renderStats() {
    const st = computePeak();
    $('#statPeak').textContent = String(st.peak);
    $('#statTotal').textContent = String(st.total);
    $('#statMax').textContent = String(S.sim.maxTick | 0);
    const errCount = (S.sim.errors || []).length;
    const bad = S.issues.filter(i => i.level === 'error').length;
    const chip = $('#statErr');
    chip.innerHTML = '问题 <b>' + S.issues.length + '</b>' + (errCount ? ' · 运行 ' + errCount : '');
    chip.classList.toggle('hot', bad > 0);
  }

  /* ============================ 问题面板 ============================ */

  function renderIssues() {
    const list = $('#issueList');
    list.innerHTML = '';
    if (!S.issues.length) {
      list.appendChild(el('div', 'empty', '没有问题，可以导出。'));
      return;
    }
    S.issues.forEach(it => {
      const row = el('div', 'issue ' + (it.level || 'info'));
      const lv = el('div', 'lv', it.level === 'error' ? 'ERR' : it.level === 'warn' ? 'WARN' : 'INFO');
      const msg = el('div', '', (it.where ? it.where + '  ' : '') + it.msg);
      row.appendChild(lv);
      row.appendChild(msg);
      row.onclick = () => {
        const m = /^#(\d+)/.exec(String(it.where || ''));
        if (m) select(Number(m[1]) - 1);
      };
      list.appendChild(row);
    });
  }

  function toggleIssues(force) {
    const box = $('#issues');
    const on = force === undefined ? !box.classList.contains('on') : !!force;
    box.classList.toggle('on', on);
    $('#issueTitle').textContent = '问题 · ' + S.issues.length;
  }

  /* ============================ 事件列表 ============================ */

  function whenText(w) {
    if (!w) return 't=0';
    const k = w.kind || 'at';
    if (k === 'at') {
      if (Array.isArray(w.a)) return 't∈{' + w.a.join(',') + '}';
      return 't=' + (isBlank(w.a) ? 0 : w.a);
    }
    if (k === 'range') return 't∈[' + (isBlank(w.a) ? 0 : w.a) + ',' + (isBlank(w.b) ? '∞' : w.b) + ')';
    if (k === 'every') return '每 ' + (isBlank(w.a) ? 1 : w.a) + (isBlank(w.b) ? '' : ' 到 ' + w.b);
    return 't=0';
  }

  function opBadgeClass(info) {
    if (!info || !info.data) return 'act';
    return info.data === 'laser' ? 'laser' : 'shot';
  }

  function renderList() {
    const box = $('#evlist');
    box.innerHTML = '';
    const evs = S.card.events || [];
    $('#evCount').textContent = String(evs.length);
    if (!evs.length) {
      box.appendChild(el('div', 'empty', '还没有事件，点「+ 事件」'));
      return;
    }
    evs.forEach((ev, i) => {
      const info = D.OP_INFO[ev.op] || {};
      const row = el('div', 'ev' + (i === S.sel ? ' on' : ''));
      row.appendChild(el('div', 'idx', String(i + 1)));
      const body = el('div', 'body');
      const op = el('div', 'op');
      op.appendChild(el('span', 'badge ' + opBadgeClass(info), ev.op || 'shot'));
      op.appendChild(el('span', '', info.label || ''));
      body.appendChild(op);
      let metaTxt = whenText(ev.when);
      if (ev.guard) metaTxt += ' · if ' + ev.guard;
      if (ev.repeat) metaTxt += ' · ×' + ev.repeat;
      body.appendChild(el('div', 'meta', metaTxt));
      row.appendChild(body);

      const tools = el('div', 'tools');
      const dup = el('button', '', '⧉');
      dup.title = '复制这个事件';
      dup.onclick = e => { if (e && e.stopPropagation) e.stopPropagation(); pushUndo(true); S.card.events.splice(i + 1, 0, clone(ev)); S.sel = i + 1; refresh(); rebuildForm(); save(); };
      const del = el('button', 'x', '✕');
      del.title = '删除这个事件';
      del.onclick = e => {
        if (e && e.stopPropagation) e.stopPropagation();
        pushUndo(true);
        S.card.events.splice(i, 1);
        // 删掉当前项之前的一行时选中项要跟着左移，否则高亮和右栏会指到「另一个事件」上
        if (i < S.sel) S.sel--;
        else S.sel = Math.min(S.sel, S.card.events.length - 1);
        refresh(); rebuildForm(); save();
      };
      tools.appendChild(dup);
      tools.appendChild(del);
      row.appendChild(tools);

      row.onclick = () => select(i);
      box.appendChild(row);
    });
  }

  function select(i) {
    if (i < -1 || i >= S.card.events.length) return;
    S.sel = i;
    renderList();
    rebuildForm();
  }

  /* ============================ 表单 ============================ */

  function section(parent, title) {
    const s = el('div', 'sec');
    const t = el('div', 'sec-t');
    t.appendChild(el('span', '', title));
    s.appendChild(t);
    const g = el('div', 'grid2');
    s.appendChild(g);
    parent.appendChild(s);
    return g;
  }

  function hint(parent, text) {
    parent.appendChild(el('p', 'hint', text));
  }

  function field(parent, label, obj, key, opts) {
    opts = opts || {};
    const wrap = el('div', 'fld' + (opts.wide ? ' wide' : ''));
    wrap.appendChild(el('span', 'fld-l', label));
    const cur = obj[key];
    let input;

    if (opts.type === 'select') {
      input = el('select', 'fld-i');
      const list = opts.options || [];
      let html = '';
      let found = false;
      list.forEach(o => {
        if (String(o.v) === String(cur)) found = true;
        html += '<option value="' + esc(o.v) + '">' + esc(o.t) + '</option>';
      });
      if (!found) {
        const shown = isBlank(cur) ? '（未设置）' : String(cur);
        html = '<option value="' + esc(isBlank(cur) ? '' : cur) + '">' + esc(shown) + '</option>' + html;
      }
      input.innerHTML = html;
      input.value = isBlank(cur) ? '' : String(cur);
      input.onchange = () => {
        const raw = input.value;
        if (raw === '') delete obj[key];
        else obj[key] = (opts.text || isNaN(raw)) ? raw : Number(raw);
        if (opts.onChange) opts.onChange(raw);
        commit();
        if (opts.rebuild) rebuildForm();
      };
    } else if (opts.type === 'check') {
      input = el('input', 'fld-c');
      input.type = 'checkbox';
      input.checked = !!cur;
      input.onchange = () => { obj[key] = input.checked; commit(); };
    } else {
      input = el('input', 'fld-i');
      input.type = 'text';
      if (opts.list) input.setAttribute('list', opts.list);
      input.value = isBlank(cur) ? '' : String(cur);
      if (opts.placeholder) input.placeholder = opts.placeholder;
      input.oninput = () => {
        const v = input.value;
        let val = v;
        if (v === '') { delete obj[key]; }
        else {
          if (opts.array && /^\s*[-\d.]+(\s*,\s*[-\d.]+)+\s*$/.test(v)) {
            val = v.split(',').map(x => Number(x.trim())).filter(x => isFinite(x));
          } else if (opts.num) {
            val = isNaN(Number(v)) ? v : Number(v);
          }
          obj[key] = val;
        }
        if (opts.expr) {
          try { D.E(v === '' ? 0 : v); input.classList.remove('bad'); }
          catch (e) { input.classList.add('bad'); }
        }
        commit();
      };
    }

    wrap.appendChild(input);
    parent.appendChild(wrap);
    return input;
  }

  function formList() {
    const arr = [];
    D.OPS.forEach(op => arr.push({ v: op, t: op + ' · ' + (D.OP_INFO[op].label || '') }));
    return arr;
  }

  function formSelect() {
    const arr = [];
    Object.keys(D.FORMS).forEach(k => arr.push({ v: String(D.FORMS[k]), t: k + ' (' + D.FORMS[k] + ')' }));
    return arr;
  }

  function renderForm() {
    const box = $('#form');
    box.innerHTML = '';
    const ev = S.card.events[S.sel];
    const label = $('#evLabel');
    const tools = $('#evTools');
    tools.innerHTML = '';

    if (!ev) {
      label.textContent = '未选中事件';
      box.appendChild(el('div', 'empty', '在左侧选一个事件，或点「+ 事件」新建'));
      return;
    }
    const info = D.OP_INFO[ev.op] || D.OP_INFO.shot;
    label.textContent = '事件 #' + (S.sel + 1) + ' · ' + (info.label || ev.op);

    /* ---------- 触发 ---------- */
    const g1 = section(box, '触发');
    field(g1, 'op', ev, 'op', {
      type: 'select', wide: true, text: true, rebuild: true,
      options: formList(),
      onChange: raw => {
        changeOp(ev, raw);
      }
    });
    if (!ev.when) ev.when = { kind: 'at', a: 0 };
    field(g1, 'when', ev.when, 'kind', {
      type: 'select', rebuild: true,
      options: [
        { v: 'at', t: 'at（某一 tick）' },
        { v: 'range', t: 'range（区间，左闭右开）' },
        { v: 'every', t: 'every（每 N tick）' }
      ],
      onChange: () => {
        if (ev.when.kind === 'range' && isBlank(ev.when.b)) ev.when.b = S.card.maxTick | 0 || 120;
        if (ev.when.kind === 'every' && isBlank(ev.when.a)) ev.when.a = 4;
      }
    });
    if (ev.when.kind === 'every') {
      field(g1, 'every（每 N tick）', ev.when, 'a', { expr: true });
      field(g1, 'until（到这里停，可空）', ev.when, 'b', { expr: true });
    } else if (ev.when.kind === 'range') {
      field(g1, 'from（含）', ev.when, 'a', { expr: true });
      field(g1, 'to（不含）', ev.when, 'b', { expr: true });
    } else {
      field(g1, 'at（tick，可写 3,10,20）', ev.when, 'a', { array: true });
    }
    field(g1, 'repeat', ev, 'repeat', { expr: true });
    field(g1, 'if 条件（可选）', ev, 'guard', { expr: true, wide: true, placeholder: '留空 = 不过滤；例如 time % 2 == 0' });
    field(g1, 'note（备注）', ev, 'note', { wide: true });
    field(g1, '播放音效 sound', ev, 'sound', { type: 'check', wide: true });

    /* ---------- 发射点与朝向 ---------- */
    if (info.data || ev.op === 'spawn') {
      const g2 = section(box, '发射点与朝向');
      field(g2, 'anchor', ev, 'anchor', {
        type: 'select', text: true, rebuild: true,
        options: [
          { v: 'user_eye', t: 'user_eye（玩家眼睛）' },
          { v: 'card', t: 'card（符卡位置）' },
          { v: 'target', t: 'target（目标）' },
          { v: 'user_feet', t: 'user_feet（玩家脚下）' },
          { v: 'vec', t: 'vec（相对眼睛的偏移）' },
          { v: 'default', t: 'default（同 user_eye）' }
        ]
      });
      if (ev.anchor === 'vec') {
        if (!ev.vec) ev.vec = { x: 0, y: 0, z: 0 };
        field(g2, '偏移 x', ev.vec, 'x', { expr: true });
        field(g2, '偏移 y', ev.vec, 'y', { expr: true });
        field(g2, '偏移 z', ev.vec, 'z', { expr: true });
      }
      if (info.data) {
        field(g2, 'aim', ev, 'aim', {
          type: 'select', text: true, rebuild: true,
          options: [
            { v: 'look', t: 'look（视线）' },
            { v: 'target', t: 'target（指向目标）' },
            { v: 'yawpitch', t: 'yawpitch（角度）' },
            { v: 'dir', t: 'dir（方向向量）' }
          ]
        });
        if (ev.aim === 'yawpitch') {
          field(g2, 'yaw（度）', ev, 'yaw', { expr: true });
          field(g2, 'pitch（度）', ev, 'pitch', { expr: true });
        }
        if (ev.aim === 'dir') {
          if (!ev.dir) ev.dir = { x: 0, y: 0, z: 1 };
          field(g2, 'dir.x', ev.dir, 'x', { expr: true });
          field(g2, 'dir.y', ev.dir, 'y', { expr: true });
          field(g2, 'dir.z', ev.dir, 'z', { expr: true });
        }
      }
    }

    /* ---------- 批量 ---------- */
    if (info.batch) {
      const g3 = section(box, info.batch === 'spread' ? '扇形 spread' : '整圈/环形 spray');
      if (!ev[info.batch] || typeof ev[info.batch] !== 'object') ev[info.batch] = {};
      const b = ev[info.batch];
      // 把扁平写法（presets / 老卡用的 way / wideAngle / ringSpan / baseAngle）迁进嵌套对象，
      // 这样表单读到的就是真值，不会显示成空框。
      const alias = info.batch === 'spread'
        ? { way: ['way'], span: ['wideAngle', 'span'], base: ['baseAngle'] }
        : { way: ['way'], span: ['ringSpan', 'span'], base: ['baseAngle'] };
      Object.keys(alias).forEach(k => {
        if (b[k] !== undefined) return;
        for (const fk of alias[k]) {
          if (ev[fk] !== undefined) { b[k] = ev[fk]; delete ev[fk]; break; }
        }
      });
      field(g3, 'way（发数）', b, 'way', { expr: true, num: true });
      // circle / sphere 的 spray 表里只有 way（+base），span 只有 wide / ring / laser_b 用得上
      const usesSpan = info.batch === 'spread' || ev.op === 'ring' || ev.op === 'ring_laser' || ev.op === 'laser_b';
      if (usesSpan) field(g3, info.batch === 'spread' ? 'span（张角，度）' : 'span（环跨，度）', b, 'span', { expr: true });
      field(g3, 'base（起始角，度）', b, 'base', { expr: true });
      if (info.slope) field(g3, 'slope（angleZ，度）', ev, 'slope', { expr: true });
      if (info.distance) field(g3, 'distance（出生半径）', ev, 'distance', { expr: true });
      if (info.random) {
        field(g3, 'num（发数）', ev, 'num', { expr: true });
      }
    } else {
      const extra = [];
      if (info.distance) extra.push(['distance（出生半径）', 'distance']);
      if (info.slope) extra.push(['slope（angleZ，度）', 'slope']);
      if (info.random) extra.push(['num（发数）', 'num'], ['ringSpan（环跨，度）', 'ringSpan']);
      if (extra.length) {
        const g4 = section(box, '批量参数');
        extra.forEach(p => field(g4, p[0], ev, p[1], { expr: true }));
      }
    }

    /* ---------- 运动 ---------- */
    if (info.motion) {
      const g5 = section(box, '运动');
      field(g5, 'first_speed', ev, 'firstSpeed', { expr: true });
      field(g5, 'limit_speed', ev, 'limitSpeed', { expr: true });
      field(g5, 'acceleration', ev, 'accel', { expr: true });
      if (!ev.gravity) ev.gravity = { x: 0, y: 0, z: 0 };
      field(g5, 'gravity.x', ev.gravity, 'x', { expr: true });
      field(g5, 'gravity.y', ev.gravity, 'y', { expr: true });
      field(g5, 'gravity.z', ev.gravity, 'z', { expr: true });
      hint(box, 'gravity 是每 tick 直接加到速度上的增量，默认 (0,-0.03,0)。想匀速就让 first_speed == limit_speed 且 acceleration = 0。');
    }

    /* ---------- 自转 ---------- */
    const spinOps = ['ring', 'circle', 'wide', 'sphere', 'random_ring', 'ring_laser', 'circle_laser',
      'wide_laser', 'sphere_laser', 'random_ring_laser', 'laser_b', 'nuclear'];
    if (spinOps.indexOf(ev.op) >= 0) {
      const g6 = section(box, '自转');
      field(g6, 'spin（度/tick）', ev, 'spin', { expr: true });
      field(g6, 'spinEnd（tick）', ev, 'spinEnd', { expr: true });
      if (ev.op === 'nuclear') {
        if (!ev.rotate) ev.rotate = { x: 0, y: 1, z: 0 };
        field(g6, 'rotate.x', ev.rotate, 'x', { expr: true });
        field(g6, 'rotate.y', ev.rotate, 'y', { expr: true });
        field(g6, 'rotate.z', ev.rotate, 'z', { expr: true });
      }
      hint(box, 'rotationEnd / spinEnd 是 tick 序号：超过它就不再自转（9999 表示不停）。');
    }

    /* ---------- 附着 ---------- */
    if (info.attach) {
      const g7 = section(box, '附着 attach');
      if (!ev.attach) ev.attach = { to: 'user', length: 1.0, yOffset: 0 };
      field(g7, 'to', ev.attach, 'to', {
        type: 'select', text: true,
        options: [{ v: 'user', t: 'user（跟随玩家）' }]
      });
      field(g7, 'length（离玩家多远）', ev.attach, 'length', { expr: true });
      field(g7, 'y_offset', ev.attach, 'yOffset', { expr: true, wide: true });
      hint(box, '附着激光的坐标由「附着目标 + 长度×方向 + y_offset」每 tick 重算，所以 pos 实际被覆盖。');
    }

    /* ---------- 弹 / 激光数据 ---------- */
    if (info.data === 'shot') {
      if (!ev.shot) ev.shot = { form: 'FORM_CRYSTAL', color: 'RED' };
      const s = ev.shot;
      const g8 = section(box, '弹数据');
      field(g8, 'form', s, 'form', { type: 'select', text: true, options: formSelect() });
      field(g8, 'color', s, 'color', { list: 'dlColor', placeholder: 'RED / 0..9 / time % 7' });
      field(g8, 'size', s, 'size', { expr: true, placeholder: 'SIZE[FORM_STAR]' });
      field(g8, 'damage', s, 'damage', { expr: true, placeholder: 'DAMAGE[FORM_STAR]' });
      field(g8, 'delay', s, 'delay', { expr: true });
      field(g8, 'end', s, 'end', { expr: true });
      field(g8, 'special（数字或名字）', s, 'special', { wide: true, placeholder: '0 / FALL01' });
      hint(box, 'damage ≤ 0 的弹体在游戏里第一个 tick 就会消失；size 是碰撞半径基准。');
    }
    if (info.data === 'laser') {
      if (!ev.laser) ev.laser = { form: 'NORMAL', color: 'BLUE' };
      const l = ev.laser;
      const g9 = section(box, '激光数据');
      field(g9, 'form', l, 'form', {
        type: 'select', text: true,
        options: [
          { v: 'NORMAL', t: 'NORMAL（普通）' },
          { v: 'GUNGNIR', t: 'GUNGNIR（冈格尼尔）' },
          { v: 'LAEVATAIN', t: 'LAEVATAIN（莱瓦汀）' }
        ]
      });
      field(g9, 'color', l, 'color', { list: 'dlColor' });
      field(g9, 'width（半宽）', l, 'width', { expr: true });
      field(g9, 'length', l, 'length', { expr: true });
      field(g9, 'damage', l, 'damage', { expr: true });
      field(g9, 'delay', l, 'delay', { expr: true });
      field(g9, 'end', l, 'end', { expr: true });
    }

    /* ---------- 动作类 ---------- */
    if (ev.op === 'spawn') {
      const g10 = section(box, '生成实体');
      field(g10, 'entity（注册名）', ev, 'entity', { wide: true, placeholder: 'thkaguya:mini_hakkero' });
    }
    if (ev.op === 'remove_danmaku') {
      const g11 = section(box, '清除弹幕');
      field(g11, 'range（半径）', ev, 'range', { expr: true });
      field(g11, 'target', ev, 'target', {
        type: 'select', text: true,
        options: [{ v: 'Enemy', t: 'Enemy' }, { v: 'Other', t: 'Other' }, { v: 'All', t: 'All' }]
      });
      field(g11, 'drop_bonus', ev, 'dropBonus', { type: 'check', wide: true });
    }
    if (ev.op === 'explosion_effect') {
      const g12 = section(box, '爆炸效果');
      field(g12, 'size（威力）', ev, 'size', { expr: true, wide: true });
    }

    /* ---------- 表达式速查 ---------- */
    const g13 = section(box, '表达式速查');
    hint(box, '变量：time / i / way / level / pi / e / cardYaw / cardPitch / userYaw / userPitch / bbHeight / look.x,y,z / tick。');
    hint(box, '函数：sin cos tan（度）、sqrt abs floor ceil round min max pow、rand()、randInt(n)、yawOf(x,z)、pitchOf(x,y,z)。');
    hint(box, '常量：FORM_STAR、COLOR_RED、LASER_GUNGNIR；表值写成 SIZE[FORM_STAR] / DAMAGE[FORM_RICE]。');
    void g13;
  }

  /* ============================ 元数据 ============================ */

  const META = [
    ['#mName', 'name'],
    ['#mNameEn', 'nameEn'],
    ['#mSlug', 'slug'],
    ['#mId', 'id'],
    ['#mNeed', 'needLevel'],
    ['#mMax', 'maxTick'],
    ['#mEnd', 'endTime'],
    ['#mRemove', 'removeTime'],
    ['#mUser', 'originalUser'],
    ['#mCircle', 'circleColor'],
    ['#mNotes', 'notes']
  ];

  function syncMeta() {
    META.forEach(p => {
      const v = S.card[p[1]];
      $(p[0]).value = isBlank(v) ? '' : String(v);
    });
    $('#level').value = String(S.card.level === undefined ? 2 : S.card.level);
    $('#userYaw').value = String(S.card.userYaw || 0);
    $('#userYawV').textContent = (S.card.userYaw || 0) + '°';
  }

  function bindMeta() {
    META.forEach(p => {
      const node = $(p[0]);
      node.oninput = () => {
        const v = node.value;
        const numKeys = ['id', 'needLevel', 'maxTick', 'endTime', 'removeTime'];
        S.card[p[1]] = (numKeys.indexOf(p[1]) >= 0 && v !== '' && !isNaN(Number(v))) ? Number(v) : v;
        if (p[1] === 'maxTick') {
          // 只更新滑杆上限，不要在这里夹 S.tick —— 把 120 改成 200 的过程中会路过 1，
          // 那样播放头会被永久拖到第 1 tick
          const max = Math.max(1, S.card.maxTick | 0 || 1);
          $('#scrub').max = String(max);
        }
        commit();
      };
    });
    $('#level').onchange = () => { S.card.level = Number($('#level').value) || 2; commit(); };
    $('#userYaw').oninput = () => {
      S.card.userYaw = Number($('#userYaw').value) || 0;
      $('#userYawV').textContent = S.card.userYaw + '°';
      commit();
    };
  }

  /* ============================ vars ============================ */

  function renderVars() {
    const box = $('#vars');
    box.innerHTML = '';
    const keys = Object.keys(S.card.vars || {});
    if (!keys.length) {
      box.appendChild(el('div', 'empty', '还没有变量'));
      renderSpecialsHint();
      return;
    }
    keys.forEach(k => {
      const row = el('div', 'varrow');
      const kIn = el('input', 'fld-i');
      kIn.value = k;
      kIn.onchange = () => {
        const nk = kIn.value.trim();
        if (!nk || nk === k) return;
        pushUndo(true);
        S.card.vars[nk] = S.card.vars[k];
        delete S.card.vars[k];
        renderVars();
        commit();
      };
      const vIn = el('input', 'fld-i');
      vIn.value = S.card.vars[k];
      vIn.oninput = () => {
        S.card.vars[k] = vIn.value;
        try { D.E(vIn.value); vIn.classList.remove('bad'); } catch (e) { vIn.classList.add('bad'); }
        commit();
      };
      const del = el('button', 'del', '✕');
      del.title = '删除变量';
      del.onclick = () => {
        pushUndo(true);
        delete S.card.vars[k];
        renderVars();
        commit();
      };
      row.appendChild(kIn);
      row.appendChild(vIn);
      row.appendChild(del);
      box.appendChild(row);
    });
    renderSpecialsHint();
  }

  /** specials 状态机只在 JSON 里原样透传（可视化编辑会失真），这里给出明确提示。 */
  function renderSpecialsHint() {
    const box = $('#vars');
    if (!box) return;
    const n = Object.keys(S.card.specials || {}).length;
    if (!n) return;
    box.appendChild(el('div', 'hint',
      'specials：' + n + ' 台状态机 —— 编辑器和 Java 导出都不处理，只原样写进 JSON。'));
  }

  /* ============================ 画布 ============================ */

  const cvs = $('#view');
  const ctx = cvs.getContext('2d');
  let W = 960, H = 540;

  function resize() {
    const stage = $('#stage');
    const r = stage.getBoundingClientRect ? stage.getBoundingClientRect() : { width: 960, height: 540 };
    const dpr = Math.min(2, window.devicePixelRatio || 1);
    W = Math.max(360, r.width || 960);
    H = Math.max(240, r.height || 540);
    cvs.width = Math.round(W * dpr);
    cvs.height = Math.round(H * dpr);
    cvs.style.width = W + 'px';
    cvs.style.height = H + 'px';
    ctx.setTransform(dpr, 0, 0, dpr, 0, 0);
  }

  // 预览相机沿「视线反方向」后撤这么多格：玩家自己发射的环形弹出生在身体四周（z≈0），
  // 相机正好贴着眼睛时它们会全部落在近裁剪面后面，预览看起来像空的。
  const CAM_BACK = 3.4;

  function camTransform(p) {
    const yaw = S.cam.yaw * Math.PI / 180;
    const pitch = S.cam.pitch * Math.PI / 180;
    const x0 = p.x, y0 = p.y - EYE, z0 = p.z;
    const cy = Math.cos(yaw), sy = Math.sin(yaw);
    const x1 = x0 * cy - z0 * sy;
    const z1 = x0 * sy + z0 * cy;
    const cp = Math.cos(pitch), sp = Math.sin(pitch);
    return { x: x1, y: y0 * cp - z1 * sp, z: y0 * sp + z1 * cp + CAM_BACK };
  }

  function focal() {
    return (H / 2) / Math.tan(S.cam.fov * Math.PI / 360);
  }

  function project(p) {
    const c = camTransform(p);
    if (c.z <= 0.08) return null;
    const f = focal();
    return { x: W / 2 + f * c.x / c.z, y: H / 2 - f * c.y / c.z, z: c.z };
  }

  function colorOf(c) {
    const n = Number(c);
    if (!isFinite(n)) return '#ff4d63';
    if (n === 8) return '#b06bff';
    if (n === 9) return '#ff9b3d';
    return D.PALETTE[Math.abs(n) % 8] || '#ffffff';
  }

  function drawGrid() {
    const g = ctx.createLinearGradient(0, 0, 0, H);
    g.addColorStop(0, '#141c2e');
    g.addColorStop(1, '#0a0e18');
    ctx.fillStyle = g;
    ctx.fillRect(0, 0, W, H);

    const GY = -EYE;
    ctx.lineWidth = 1;
    ctx.strokeStyle = 'rgba(96,126,196,.20)';
    for (let z = 2; z <= 60; z += 2) {
      const a = project({ x: -24, y: GY, z: z });
      const b = project({ x: 24, y: GY, z: z });
      if (!a || !b) continue;
      ctx.beginPath(); ctx.moveTo(a.x, a.y); ctx.lineTo(b.x, b.y); ctx.stroke();
    }
    ctx.strokeStyle = 'rgba(96,126,196,.14)';
    for (let x = -24; x <= 24; x += 4) {
      const a = project({ x: x, y: GY, z: 2 });
      const b = project({ x: x, y: GY, z: 60 });
      if (!a || !b) continue;
      ctx.beginPath(); ctx.moveTo(a.x, a.y); ctx.lineTo(b.x, b.y); ctx.stroke();
    }

    // 正前方 12 格处一块靶面，方便判断弹幕是否往镜头方向飞
    const t0 = project({ x: -3, y: GY, z: 12 });
    const t1 = project({ x: 3, y: GY + 1.8, z: 12 });
    if (t0 && t1) {
      ctx.strokeStyle = 'rgba(150,170,220,.28)';
      ctx.setLineDash([4, 4]);
      ctx.strokeRect(Math.min(t0.x, t1.x), Math.min(t0.y, t1.y), Math.abs(t1.x - t0.x), Math.abs(t1.y - t0.y));
      ctx.setLineDash([]);
    }

    // 玩家脚下的十字
    const c0 = project({ x: 0, y: GY, z: 0 });
    if (c0) {
      ctx.strokeStyle = 'rgba(120,200,255,.5)';
      ctx.beginPath();
      ctx.moveTo(c0.x - 8, c0.y); ctx.lineTo(c0.x + 8, c0.y);
      ctx.moveTo(c0.x, c0.y - 8); ctx.lineTo(c0.x, c0.y + 8);
      ctx.stroke();
    }
  }

  function starPath(x, y, r) {
    for (let i = 0; i < 10; i++) {
      const a = -Math.PI / 2 + i * Math.PI / 5;
      const rr = i % 2 ? r * 0.44 : r;
      const px = x + Math.cos(a) * rr, py = y + Math.sin(a) * rr;
      if (i === 0) ctx.moveTo(px, py); else ctx.lineTo(px, py);
    }
  }

  function heartPath(x, y, r) {
    ctx.moveTo(x, y + r * 0.75);
    ctx.bezierCurveTo(x - r * 1.6, y - r * 0.35, x - r * 0.5, y - r * 1.25, x, y - r * 0.35);
    ctx.bezierCurveTo(x + r * 0.5, y - r * 1.25, x + r * 1.6, y - r * 0.35, x, y + r * 0.75);
  }

  function drawSprite(x, y, r, col, form, alpha) {
    ctx.globalAlpha = alpha;
    ctx.fillStyle = col;
    ctx.beginPath();
    ctx.arc(x, y, r * 2.0, 0, Math.PI * 2);
    ctx.globalAlpha = alpha * 0.14;
    ctx.fill();
    ctx.globalAlpha = alpha;

    ctx.beginPath();
    if (form === 9 || form === 8) starPath(x, y, r * (form === 9 ? 1.2 : 0.95));
    else if (form === 11) { ctx.moveTo(x, y - r * 1.2); ctx.lineTo(x + r * 0.75, y); ctx.lineTo(x, y + r * 1.2); ctx.lineTo(x - r * 0.75, y); }
    else if (form === 12) heartPath(x, y, r);
    else if (form === 10 || form === 13 || form === 18 || form === 28) ctx.ellipse(x, y, r * 0.5, r * 1.25, 0, 0, Math.PI * 2);
    else ctx.arc(x, y, r, 0, Math.PI * 2);
    ctx.fill();

    ctx.beginPath();
    ctx.fillStyle = 'rgba(255,255,255,.88)';
    ctx.arc(x, y, Math.max(0.6, r * 0.3), 0, Math.PI * 2);
    ctx.globalAlpha = alpha * 0.9;
    ctx.fill();
    ctx.globalAlpha = 1;
  }

  function drawLaser(p, s) {
    const len = Number(s.length) || 2;
    const a = project({ x: s.x, y: s.y, z: s.z });
    const b = project({ x: s.x + s.ax * len, y: s.y + s.ay * len, z: s.z + s.az * len });
    if (!a || !b) return;
    const col = colorOf(s.color);
    const lw = Math.min(60, Math.max(1.2, focal() * (Number(s.width) || 0.1) * 2 / Math.max(0.5, p.z)));
    ctx.globalAlpha = s.vis ? 0.85 : 0.25;
    ctx.strokeStyle = col;
    ctx.lineWidth = lw;
    ctx.lineCap = 'round';
    ctx.beginPath(); ctx.moveTo(a.x, a.y); ctx.lineTo(b.x, b.y); ctx.stroke();
    ctx.globalAlpha = (s.vis ? 0.5 : 0.15);
    ctx.strokeStyle = 'rgba(255,255,255,.85)';
    ctx.lineWidth = Math.max(0.8, lw * 0.4);
    ctx.beginPath(); ctx.moveTo(a.x, a.y); ctx.lineTo(b.x, b.y); ctx.stroke();
    ctx.globalAlpha = 1;
  }

  function drawFrame() {
    if (!ctx) return;
    drawGrid();
    let list = [];
    try { list = D.stateAt(S.sim, S.tick) || []; } catch (e) { list = []; }
    const items = [];
    const f = focal();
    for (let i = 0; i < list.length; i++) {
      const s = list[i];
      const pr = project({ x: s.x, y: s.y, z: s.z });
      if (!pr) continue;
      const r = Math.min(40, Math.max(0.8, f * (Number(s.size) || 0.3) * 0.5 / Math.max(0.4, pr.z)));
      items.push({ s: s, pr: pr, r: r });
    }
    items.sort((a, b) => b.pr.z - a.pr.z);

    let drawn = 0;
    for (let i = 0; i < items.length; i++) {
      const it = items[i];
      if (it.pr.z < 0.35 || it.r < 0.3 || it.pr.z > 160) continue;
      if (it.s.kind === 'laser' || it.s.kind === 'laser_attached') drawLaser(it.pr, it.s);
      else drawSprite(it.pr.x, it.pr.y, it.r, colorOf(it.s.color), it.s.form, it.s.vis ? 1 : 0.25);
      drawn++;
    }
    ctx.fillStyle = 'rgba(190,206,236,.55)';
    ctx.font = '11px ui-monospace, monospace';
    ctx.fillText('绘制 ' + drawn + ' / 场上 ' + list.length, 10, H - 10);
  }

  /* ============================ 播放 ============================ */

  function updateHud() {
    const max = Math.max(1, S.sim.maxTick | 0 || 120);
    $('#tNow').textContent = String(S.tick);
    $('#tMax').textContent = String(max);
    $('#statT').textContent = String(S.tick);
    const scrub = $('#scrub');
    scrub.max = String(max);
    scrub.value = String(S.tick);
  }

  function renderMarks() {
    const box = $('#marks');
    box.innerHTML = '';
    const max = Math.max(1, S.sim.maxTick | 0 || 120);
    const seen = {};
    // spawnLog 的元素是 {tick, op, count, idx}，不是数字
    (S.sim.spawnLog || []).forEach(s => { seen[s.tick | 0] = 1; });
    Object.keys(seen).forEach(t => {
      const d = el('div', '');
      const left = (Number(t) / max) * 100;
      if (d.style) {
        d.style.position = 'absolute';
        d.style.left = left + '%';
        d.style.top = '0';
        d.style.bottom = '0';
        d.style.width = '2px';
        d.style.background = 'rgba(91,91,214,.5)';
      }
      box.appendChild(d);
    });
  }

  function setPlayIcon() {
    $('#playIcon').textContent = S.playing ? '❚❚' : '▶';
  }

  function loop(ts) {
    requestAnimationFrame(loop);
    if (!S.last) S.last = ts;
    const dt = Math.min(250, ts - S.last);
    S.last = ts;
    if (!S.playing) return;
    S.acc += dt * S.speed;
    const step = TICK_MS;
    let moved = false;
    while (S.acc >= step) {
      S.acc -= step;
      S.tick++;
      const max = Math.max(1, S.sim.maxTick | 0 || 120);
      if (S.tick > max) S.tick = 0;
      moved = true;
    }
    if (moved) {
      updateHud();
      drawFrame();
    }
  }

  function play() {
    S.playing = !S.playing;
    S.acc = 0;
    setPlayIcon();
  }

  /* ============================ 报告 / 导出 ============================ */

  function reportMd() {
    const st = computePeak();
    const lines = [];
    lines.push('# ' + (S.card.name || '未命名符卡') + ' 校验报告');
    lines.push('');
    lines.push('| 项 | 值 |');
    lines.push('| --- | --- |');
    lines.push('| slug | `' + (S.card.slug || '') + '` |');
    lines.push('| 类名 | `THSC_' + String(S.card.slug || 'generated').replace(/[^A-Za-z0-9_]/g, '_') + '` |');
    lines.push('| 难度 / end_time / remove_time | ' + (S.card.needLevel | 0) + ' / ' + (S.card.endTime | 0) + ' / ' + (S.card.removeTime | 0) + ' |');
    lines.push('| 时长 | ' + (S.sim.maxTick | 0) + ' tick |');
    lines.push('| 累计弹幕 | ' + st.total + ' |');
    lines.push('| 同屏峰值 | ' + st.peak + ' |');
    lines.push('| 事件数 | ' + (S.card.events || []).length + ' |');
    lines.push('');
    lines.push('## 事件');
    lines.push('');
    lines.push('| # | op | 触发 | if | 备注 |');
    lines.push('| --- | --- | --- | --- | --- |');
    (S.card.events || []).forEach((ev, i) => {
      lines.push('| ' + (i + 1) + ' | `' + (ev.op || '') + '` | ' + whenText(ev.when) +
        ' | ' + (ev.guard ? '`' + ev.guard + '`' : '') +
        ' | ' + (ev.note || '') + ' |');
    });
    lines.push('');
    lines.push('## 问题');
    lines.push('');
    if (!S.issues.length) lines.push('无。');
    else S.issues.forEach(it => lines.push('- **' + (it.level || 'info') + '** ' + (it.where || '') + '：' + it.msg));
    return lines.join('\n');
  }

  function exportText(kind) {
    if (kind === 'java') return D.cardToJava(S.card);
    if (kind === 'json') return D.cardToJson(S.card);
    return reportMd();
  }

  function openExport(tab) {
    S.curTab = tab || S.curTab || 'java';
    const modal = $('#modal');
    modal.classList.add('on');
    setTab(S.curTab);
  }

  function setTab(kind) {
    S.curTab = kind;
    const tabs = document.querySelectorAll('.tab');
    for (let i = 0; i < tabs.length; i++) {
      const t = tabs[i];
      t.classList.toggle('on', t.getAttribute && t.getAttribute('data-tab') === kind);
    }
    const imp = $('#importBox');
    const ex = $('#export');
    const isImport = kind === 'import';
    imp.classList.toggle('on', isImport);
    // 导入面板是绝对定位盖在文本框上的，透明时会把 Java 文本透出来 —— 直接隐藏更干净
    ex.style.display = isImport ? 'none' : 'block';
    // 导入页签没有可复制的产物，留着按钮会复制上一个页签的旧文本
    ['#btnCopy', '#btnDownload'].forEach(sel => {
      const b = $(sel);
      if (b) b.style.display = isImport ? 'none' : '';
    });
    if (!isImport) {
      let txt = '';
      try { txt = exportText(kind); } catch (e) { txt = '导出出错：' + e.message; }
      ex.value = txt;
      $('#exportInfo').textContent = txt.length + ' 字符';
    } else {
      $('#exportInfo').textContent = '';
    }
  }

  function download() {
    const txt = $('#export').value;
    const slug = String(S.card.slug || 'spellcard');
    const ext = S.curTab === 'json' ? '.json' : S.curTab === 'report' ? '.md' : '.java';
    const name = S.curTab === 'java' ? 'THSC_' + slug.replace(/[^A-Za-z0-9_]/g, '_') + ext : slug + ext;
    try {
      const blob = new Blob([txt], { type: 'text/plain;charset=utf-8' });
      const url = URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = name;
      a.click();
      URL.revokeObjectURL(url);
      toast('已下载 ' + name);
    } catch (e) {
      toast('下载失败：' + e.message);
    }
  }

  function copyOut() {
    const txt = $('#export').value;
    const nav = window.navigator || {};
    if (nav.clipboard && nav.clipboard.writeText) {
      nav.clipboard.writeText(txt).then(() => toast('已复制'), () => fallbackCopy(txt));
    } else {
      fallbackCopy(txt);
    }
  }

  function fallbackCopy(txt) {
    const ta = $('#export');
    try {
      ta.select();
      document.execCommand('copy');
      toast('已复制');
    } catch (e) {
      toast('复制失败，请手动选择文本');
    }
  }

  function doImport() {
    const txt = $('#importText').value || '';
    if (!txt.trim()) { toast('先粘贴 JSON'); return; }
    // cardFromJson 永远返回带 events 的对象，所以「像不像一张卡」只能在这里判断 ——
    // 否则粘一个 `{}` 会把当前编辑内容静默换成空白卡
    let probe = null;
    try { probe = JSON.parse(txt.replace(/^\uFEFF/, '')); } catch (e) { probe = null; }
    if (!probe || typeof probe !== 'object' || (!Array.isArray(probe.events) && probe.$schema === undefined && probe.slug === undefined)) {
      toast('这不像符卡 JSON（至少要有个 events 数组或 $schema / slug 字段）');
      return;
    }
    let card;
    try {
      card = D.cardFromJson(txt);
    } catch (e) {
      toast('JSON 解析失败：' + e.message);
      return;
    }
    if (!card || !Array.isArray(card.events)) { toast('这不是一张符卡 JSON'); return; }
    pushUndo(true);
    S.card = normalizeCard(card);
    S.sel = S.card.events.length ? 0 : -1;
    S.tick = 0;
    S.presetKey = '';
    $('#preset').value = '';
    syncMeta();
    renderVars();
    refresh();
    rebuildForm();
    save();
    $('#modal').classList.remove('on');
    toast('已导入 ' + (S.card.name || '符卡') + '（' + S.card.events.length + ' 个事件）');
  }

  /** 「选文件…」：把 .json 读进文本框，再由 doImport 走同一条路径。 */
  function bindImportFile() {
    const input = $('#importFile');
    if (!input) return;
    input.onchange = () => {
      const f = input.files && input.files[0];
      if (!f) return;
      try {
        const rd = new FileReader();
        rd.onload = () => { $('#importText').value = String(rd.result || ''); };
        rd.onerror = () => toast('读文件失败');
        rd.readAsText(f);
      } catch (e) {
        toast('读文件失败：' + e.message);
      }
      // 清空 value：否则第二次选「同一个文件」不会触发 onchange
      try { input.value = ''; } catch (e) { /* 桩实现可能不允许 */ }
    };
  }

  /* ============================ 预设 ============================ */

  function renderPresets() {
    const sel = $('#preset');
    let html = '<option value="">— 选择预设 —</option>';
    Object.keys(PRESETS).forEach(k => {
      html += '<option value="' + esc(k) + '">' + esc(PRESETS[k].label || k) + '</option>';
    });
    sel.innerHTML = html;
    sel.value = S.presetKey || '';
    sel.onchange = () => {
      if (!sel.value) return;
      loadPreset(sel.value);
    };
  }

  function loadPreset(key) {
    const p = PRESETS[key];
    if (!p) return;
    pushUndo(true);
    S.card = normalizeCard(clone(p.card));
    S.presetKey = key;
    S.sel = S.card.events.length ? 0 : -1;
    S.tick = 0;
    S.playing = false;
    setPlayIcon();
    syncMeta();
    renderVars();
    refresh();
    rebuildForm();
    save();
    // 下拉框回落到「— 选择预设 —」：让它像命令菜单一样可以重复选同一项来重载
    const sel = $('#preset');
    if (sel) sel.value = '';
    toast('已载入预设「' + (p.label || key) + '」');
  }

  /* ============================ toast ============================ */

  let toastTimer = 0;
  let saveTimer = 0;
  function toast(msg) {
    const t = $('#toast');
    t.textContent = msg;
    t.classList.add('on');
    if (toastTimer) clearTimeout(toastTimer);
    toastTimer = setTimeout(() => { t.classList.remove('on'); toastTimer = 0; }, 2200);
  }

  /* ============================ 交互绑定 ============================ */

  function bindUi() {
    $('#btnAdd').onclick = () => {
      pushUndo(true);
      const ev = newEvent('ring');
      const def = S.card.events.length ? S.card.events[S.card.events.length - 1].when : null;
      ev.when = def ? clone(def) : { kind: 'at', a: 0 };
      S.card.events.push(ev);
      S.sel = S.card.events.length - 1;
      refresh();
      rebuildForm();
      save();
    };

    $('#btnAddVar').onclick = () => {
      pushUndo(true);
      let i = 1;
      while (S.card.vars['v' + i] !== undefined) i++;
      S.card.vars['v' + i] = '0';
      renderVars();
      commit();
    };

    $('#btnNew').onclick = () => {
      if (window.confirm && !window.confirm('新建会清空当前编辑内容（可 Ctrl+Z 撤销），继续？')) return;
      pushUndo(true);
      S.card = blankCard();
      S.card.events.push(newEvent('ring'));
      S.sel = 0;
      S.tick = 0;
      S.presetKey = '';
      $('#preset').value = '';
      syncMeta();
      renderVars();
      refresh();
      rebuildForm();
      save();
      toast('已新建空白符卡');
    };

    $('#btnUndo').onclick = undo;
    $('#btnRedo').onclick = redo;

    $('#btnImport').onclick = () => {
      openExport('import');
      $('#importText').value = '';
      $('#importText').focus();
    };
    $('#btnDoImport').onclick = doImport;
    bindImportFile();
    $('#btnExport').onclick = () => openExport(S.curTab === 'import' ? 'java' : S.curTab);
    $('#btnClose').onclick = () => $('#modal').classList.remove('on');
    $('#btnCopy').onclick = copyOut;
    $('#btnDownload').onclick = download;
    $('#modal').onclick = e => { if (e && e.target === e.currentTarget) $('#modal').classList.remove('on'); };

    const tabs = document.querySelectorAll('.tab');
    for (let i = 0; i < tabs.length; i++) {
      const t = tabs[i];
      t.onclick = () => setTab(t.getAttribute('data-tab'));
    }

    $('#statErr').onclick = () => toggleIssues();
    $('#btnIssuesClose').onclick = () => toggleIssues(false);

    $('#btnPlay').onclick = play;
    $('#btnRewind').onclick = () => { S.tick = 0; S.acc = 0; updateHud(); drawFrame(); };
    $('#btnPrev').onclick = () => { S.tick = Math.max(0, S.tick - 1); updateHud(); drawFrame(); };
    $('#btnNext').onclick = () => { S.tick = Math.min(S.sim.maxTick | 0, S.tick + 1); updateHud(); drawFrame(); };
    $('#scrub').oninput = () => {
      S.playing = false;
      setPlayIcon();
      S.tick = Number($('#scrub').value) || 0;
      updateHud();
      drawFrame();
      saveLater();
    };
    $('#speed').onchange = () => { S.speed = Number($('#speed').value) || 1; };

    // 视角与播放头也要落盘，否则调好的一刷新就回默认
    $('#camYaw').oninput = () => { S.cam.yaw = Number($('#camYaw').value) || 0; drawFrame(); saveLater(); };
    $('#camPitch').oninput = () => { S.cam.pitch = Number($('#camPitch').value) || 0; drawFrame(); saveLater(); };
    $('#camFov').oninput = () => { S.cam.fov = Number($('#camFov').value) || 62; drawFrame(); saveLater(); };

    dragCamera();
    bindKeys();
  }

  function saveLater() {
    if (saveTimer) clearTimeout(saveTimer);
    saveTimer = setTimeout(() => { saveTimer = 0; save(); }, 400);
  }

  function dragCamera() {
    let down = false, lx = 0, ly = 0;
    cvs.addEventListener('mousedown', e => { down = true; lx = e.clientX; ly = e.clientY; });
    window.addEventListener && window.addEventListener('mouseup', () => { down = false; });
    cvs.addEventListener('mousemove', e => {
      if (!down) return;
      S.cam.yaw = Math.max(-180, Math.min(180, S.cam.yaw - (e.clientX - lx) * 0.4));
      S.cam.pitch = Math.max(-80, Math.min(80, S.cam.pitch + (e.clientY - ly) * 0.4));
      lx = e.clientX; ly = e.clientY;
      $('#camYaw').value = String(Math.round(S.cam.yaw));
      $('#camPitch').value = String(Math.round(S.cam.pitch));
      drawFrame();
    });
    cvs.addEventListener('wheel', e => {
      S.cam.fov = Math.max(25, Math.min(110, S.cam.fov + (e.deltaY > 0 ? 3 : -3)));
      $('#camFov').value = String(Math.round(S.cam.fov));
      drawFrame();
      e.preventDefault && e.preventDefault();
    });
  }

  function bindKeys() {
    window.addEventListener && window.addEventListener('keydown', e => {
      const tag = (e.target && e.target.tagName) || '';
      const typing = tag === 'INPUT' || tag === 'TEXTAREA' || tag === 'SELECT';
      const mod = e.ctrlKey || e.metaKey;
      if (mod && e.key === 's') { e.preventDefault(); save(); toast('已保存到浏览器'); return; }
      if (mod && e.key === 'e') { e.preventDefault(); openExport(); return; }
      // 光标在输入框里时把撤销交给浏览器（它只回退文本框内容，并照常触发 input 事件写回卡对象）
      if (typing) return;
      if (mod && e.key === 'z' && !e.shiftKey) { e.preventDefault(); undo(); return; }
      if (mod && (e.key === 'y' || (e.key === 'z' && e.shiftKey))) { e.preventDefault(); redo(); return; }
      if (e.key === ' ') { e.preventDefault(); play(); }
      else if (e.key === 'ArrowLeft') { S.playing = false; setPlayIcon(); S.tick = Math.max(0, S.tick - 1); updateHud(); drawFrame(); }
      else if (e.key === 'ArrowRight') { S.playing = false; setPlayIcon(); S.tick = Math.min(S.sim.maxTick | 0, S.tick + 1); updateHud(); drawFrame(); }
      else if (e.key === 'Home') { S.tick = 0; updateHud(); drawFrame(); }
      else if (e.key === 'End') { S.tick = S.sim.maxTick | 0; updateHud(); drawFrame(); }
    });
  }

  /* ============================ 启动 ============================ */

  /** 原角色下拉的候选值直接从核心层来，避免和 THSpellCard 的常量表走偏。 */
  function fillUserDatalist() {
    const dl = $('#dlUser');
    if (!dl || !Array.isArray(D.USER_NAMES)) return;
    dl.innerHTML = '';
    D.USER_NAMES.forEach(k => {
      const o = el('option');
      o.value = k;
      dl.appendChild(o);
    });
  }

  function boot() {
    // 支持 danmaku-editor.html#moon 这种「直接打开某个预设」的链接（调试 / 分享用）
    const hashKey = String((typeof location !== 'undefined' && location.hash) || '').replace(/^#/, '');
    const saved = hashKey && PRESETS[hashKey] ? null : restore();
    let loaded = false;
    if (saved && saved.card && Array.isArray(saved.card.events)) {
      // 存档可能是旧版本 / 手改坏的：解析失败就退回预设，别让页面白屏
      try {
        S.card = normalizeCard(saved.card);
        S.presetKey = saved.presetKey || '';
        if (saved.cam) S.cam = saved.cam;
        S.tick = saved.tick | 0;
        loaded = true;
      } catch (e) { loaded = false; }
    }
    if (!loaded) {
      const first = (hashKey && PRESETS[hashKey]) ? hashKey : (PRESETS.saikou ? 'saikou' : Object.keys(PRESETS)[0]);
      S.card = first ? normalizeCard(clone(PRESETS[first].card)) : blankCard();
      S.presetKey = first || '';
      if (!S.card.events.length) S.card.events.push(newEvent('ring'));
    }
    S.sel = S.card.events.length ? 0 : -1;

    // 属性面板用到的两个输入范围
    $('#camYaw').value = String(S.cam.yaw);
    $('#camPitch').value = String(S.cam.pitch);
    $('#camFov').value = String(S.cam.fov);
    $('#speed').value = '1';

    bindMeta();
    bindUi();
    syncMeta();
    fillUserDatalist();
    renderPresets();
    renderVars();
    refresh();
    rebuildForm();
    syncUndoBtns();
    setPlayIcon();
    resize();
    drawFrame();
    save();                       // 首次启动也落一次存档，刷新后能原样恢复
    jumpToFirstAlive();
    requestAnimationFrame(loop);
  }

  boot();
})();
