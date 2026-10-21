/**
 * verify-java.js — 把编辑器生成的 Java 拿去真的编译一遍。
 *
 * 单元测试只能保证字符串长得对，这个脚本才能保证「导出丢进 mod 就能编译」：
 * 它把所有预设 + 一张覆盖 18 个 op 的合成卡 + 一张表达式压力卡导出成 .java，
 * 然后用 mod 自己的编译 classpath（thkaguya-1.21.1/build/compile-cp.txt）调 javac。
 *
 *   node editor/verify-java.js            # 生成 + 编译
 *   node editor/verify-java.js --keep     # 保留生成的 .java 方便肉眼看
 *
 * 退出码：0 = 全部编译通过（或环境不具备而跳过），1 = 编译失败。
 */
'use strict';

const fs = require('fs');
const os = require('os');
const path = require('path');
const cp = require('child_process');

const ED = __dirname;
const ROOT = path.resolve(ED, '..');
const MOD = path.join(ROOT, 'thkaguya-1.21.1');
const D = require(path.join(ED, 'danmaku-core.js'));
const P = require(path.join(ED, 'presets.js'));

const keep = process.argv.indexOf('--keep') >= 0;
const outDir = path.join(os.tmpdir(), 'dmk-java-check', String(process.pid));
fs.mkdirSync(outDir, { recursive: true });

const files = [];
function emit(card, label) {
  const java = D.cardToJava(card);
  const m = /public class (\w+)/.exec(java);
  const cls = m ? m[1] : 'THSC_Unknown';
  const f = path.join(outDir, cls + '.java');
  fs.writeFileSync(f, java, 'utf8');
  files.push(f);
  return label + ' -> ' + cls + '.java';
}

/* ---------- 1. 所有预设 ---------- */
for (const k in P) emit(P[k].card, 'preset:' + k);

/* ---------- 2. 覆盖全部 18 个 op 的合成卡 ---------- */
function allOpsCard() {
  const evs = D.OPS.map(function (op, i) {
    const info = D.OP_INFO[op];
    const e = { when: { kind: 'at', a: i + 1 }, op: op };
    if (info.batch) e[info.batch] = { way: 3, span: 120, base: 10 };
    if (info.motion) { e.firstSpeed = 0.5; e.limitSpeed = 0.9; e.accel = 0.02; }
    if (info.data === 'shot') e.shot = { form: 'STAR', color: 'RED', size: 0.4, damage: 3, delay: 2, end: 40 };
    if (info.data === 'laser') e.laser = { form: 'NORMAL', color: 'BLUE', width: 0.3, length: 20, damage: 4, delay: 0, end: 40 };
    if (info.nuclear) { e.rotate = { x: 0, y: 1, z: 0 }; e.slope = 0.5; }
    if (info.attach) e.attach = { to: 'user', length: 1, yOffset: 'bbHeight * 0.5' };
    if (info.distance) e.distance = 0.5;
    if (info.slope) e.slope = 0.5;
    if (info.random) e.ringSpan = 360;
    if (op === 'spawn') e.entity = 'thkaguya:mini_hakkero';
    if (op === 'remove_danmaku') { e.range = 8; e.target = 'Enemy'; e.dropBonus = true; }
    if (op === 'explosion_effect') e.size = 3;
    if (op === 'sound') e.sound = true;
    return e;
  });
  return {
    slug: 'GenAllOps', name: '生成器全 op 自检', nameEn: 'Gen All Ops',
    needLevel: 1, endTime: 100, removeTime: 50, originalUser: 'REIMU',
    circleColor: 'RED', maxTick: 60, level: 2, vars: {}, events: evs
  };
}
emit(allOpsCard(), 'synth:all-ops');

/* ---------- 3. 表达式压力卡（每个数值位都塞表达式 / vars 宏） ---------- */
emit({
  slug: 'GenExpr', name: '表达式压力', needLevel: 3, endTime: 90, removeTime: 40,
  originalUser: 'MARISA', maxTick: 90, level: 3,
  vars: { a: 'time * 6 + i * (360 / 3)', g: '0.034 + 0.03 * sin(time * 6)' },
  events: [
    {
      when: { kind: 'range', a: 0, b: 90 }, op: 'ring', guard: 'time % 2 == 0',
      spray: { way: '3 + level % 2', span: 360, base: 'a' }, distance: '0.4 + 0.1 * sin(time)',
      aim: 'yawpitch', yaw: 'userYaw + a', pitch: 'userPitch - 10',
      repeat: 2, firstSpeed: '0.2 + 0.1 * level', limitSpeed: '0.6', accel: '-0.01',
      gravity: { x: 0, y: '-g', z: 0 },
      shot: { form: 'TINY', color: 'time % 4', size: '@SIZE[FORM_TINY]', damage: 'DAMAGE[FORM_TINY] * 2', delay: 0, end: '40 + level * 5' }
    },
    {
      when: { kind: 'every', a: 20, b: 80 }, op: 'laser_b', aim: 'look',
      spray: { way: 5, span: 360 }, spin: '-2 + level * 0.5', spinEnd: 9999,
      attach: { to: 'user', length: '1.0 + 0.1 * level', yOffset: 'bbHeight * 0.5' },
      laser: { form: 'NORMAL', color: 'BLUE', width: '0.2 + 0.1 * level', length: '30 + level', damage: 5, delay: 0, end: '30 + level * 2' }
    },
    {
      when: { kind: 'at', a: [1, 5, 9] }, op: 'nuclear', aim: 'target',
      rotate: { x: 0, y: 1, z: 0 }, slope: 'level * 0.5', spin: 3, spinEnd: 60,
      firstSpeed: 0.9, limitSpeed: 0.9, accel: 0,
      shot: { form: 'BIGLIGHT', color: 'ORANGE', size: 1.2, damage: 12, delay: 0, end: 50 }
    }
  ]
}, 'synth:expression');

console.log('生成 ' + files.length + ' 个 .java -> ' + outDir);

/* ---------- 4. javac ---------- */
const cpFile = path.join(MOD, 'build', 'compile-cp.txt');
if (!fs.existsSync(cpFile)) {
  console.log('\n跳过编译：找不到 ' + cpFile);
  console.log('先在 thkaguya-1.21.1 里跑一次 `g.bat build`，这个文件是 NeoForge 构建脚本产出的类路径缓存。');
  process.exit(0);
}
const classpath = fs.readFileSync(cpFile, 'utf8').trim() +
  ';' + path.join(MOD, 'build', 'classes', 'java', 'main');

const javacCandidates = [
  path.join(process.env.JAVA_HOME || '', 'bin', 'javac.exe'),
  'C:\\Java\\jdk-21\\bin\\javac.exe',
  'javac'
];
const javac = javacCandidates.find(function (p) {
  try { return p === 'javac' ? true : fs.existsSync(p); } catch (e) { return false; }
});

const clsDir = path.join(outDir, 'cls');
fs.mkdirSync(clsDir, { recursive: true });

console.log('\n用 ' + javac + ' 编译 …');
const r = cp.spawnSync(javac, ['-nowarn', '-encoding', 'UTF-8', '-cp', classpath, '-d', clsDir].concat(files),
  { stdio: 'inherit' });

if (!keep) {
  // 成功才清；失败时留着方便排查
}
if (r.status === 0) {
  console.log('\n编译通过：' + files.length + ' 个生成的符卡类都能编译。');
  if (!keep) fs.rmSync(outDir, { recursive: true, force: true });
  process.exit(0);
}
console.log('\n编译失败。生成的源文件保留在 ' + outDir);
process.exit(1);
