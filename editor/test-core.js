/* danmaku-core 自检：数学不变量 + 物理 + 仿真（node editor/test-core.js） */
const D = require('./danmaku-core.js');

let pass = 0, fail = 0;
function ok(name, cond, extra) {
  if (cond) { pass++; console.log('  ok   ' + name); }
  else { fail++; console.log('  FAIL ' + name + (extra ? '  → ' + extra : '')); }
}
function near(a, b, eps) { return Math.abs(a - b) <= (eps === undefined ? 1e-6 : eps); }
function ang(a, b) { // 两向量夹角（度）
  const c = D.vdot(a, b) / (D.vlen(a) * D.vlen(b));
  return Math.acos(Math.max(-1, Math.min(1, c))) / Math.PI * 180;
}

console.log('\n[1] THShotLib.getVecFromAngle / yawOf / pitchOf');
{
  const z = D.fromAngle(0, 0);
  ok('yaw=0,pitch=0 → +Z', near(z.x, 0) && near(z.y, 0) && near(z.z, 1), JSON.stringify(z));
  const s = D.fromAngle(90, 0);
  ok('yaw=90 → -X（与原版 -sin(yaw) 一致）', near(s.x, -1) && near(s.z, 0), JSON.stringify(s));
  const down = D.fromAngle(0, 90);
  ok('pitch=90 → -Y（原版 -sin(pitch)）', near(down.y, -1), JSON.stringify(down));
  ok('yawOf(+Z)=0', near(D.yawOf(0, 1), 0));
  // 重要：原版 getYawFromVector 与 getVecFromAngle 符号相反（atan2(x,z) vs -sin(yaw)），
  // 二者不是互逆函数。此处的期望值按原版实际返回值写，移植/编辑器都不能把它们当逆运算替换。
  ok('yawOf(-X)=-90（原版反号约定）', near(D.yawOf(-1, 0), -90), D.yawOf(-1, 0));
  ok('pitchOf(0,-1,0)=-90（同理反号）', near(D.pitchOf(0, -1, 0), -90), D.pitchOf(0, -1, 0));
}

console.log('\n[2] getVectorFromRotation（罗德里格斯，绕 +Y 转 90°）');
{
  const r = D.rotateAround(D.v(0, 1, 0), D.v(0, 0, 1), 90);
  ok('(0,0,1) 绕 +Y 转 90° → (+1,0,0)', near(r.x, 1, 1e-6) && near(r.z, 0, 1e-6), JSON.stringify(r));
  const r2 = D.rotateAround(D.v(0, 1, 0), D.v(0, 1, 0), 123);
  ok('平行于轴不变', near(r2.y, 1, 1e-6));
  ok('旋转保长度', near(D.vlen(D.rotateAround(D.v(0, 1, 0), D.v(0.3, 0.4, 0.5), 71)), D.vlen(D.v(0.3, 0.4, 0.5)), 1e-9));
}

console.log('\n[3] createWideShot（way=3, wideAngle=90）');
{
  const card = {
    maxTick: 1, level: 2,
    events: [{ op: 'wide', when: { kind: 'at', a: 0 }, aim: 'look', firstSpeed: 1, way: 3, wideAngle: 90, distance: 0, baseAngle: 0 }]
  };
  const sim = D.simulate(card);
  ok('发 3 发', sim.shots.length === 3, sim.shots.length);
  ok('无解析错误', sim.errors.length === 0, JSON.stringify(sim.errors));
  const ds = sim.shots.map(s => s.angle);
  const a01 = ang(ds[0], ds[1]), a12 = ang(ds[1], ds[2]);
  ok('相邻夹角 45°', near(a01, 45, 1e-6) && near(a12, 45, 1e-6), a01 + ' / ' + a12);
  ok('关于 +Z 对称', near(ds[0].x, -ds[2].x, 1e-6) && near(ds[1].x, 0, 1e-6));
}

console.log('\n[4] createCircleShot（way=8 → 每 45°）');
{
  const card = {
    maxTick: 1, level: 2,
    events: [{ op: 'circle', when: { kind: 'at', a: 0 }, aim: 'look', firstSpeed: 1, way: 8 }]
  };
  const sim = D.simulate(card);
  ok('发 8 发', sim.shots.length === 8, sim.shots.length);
  const ds = sim.shots.map(s => s.angle);
  let allHoriz = true, spans = [];
  for (const d of ds) if (Math.abs(d.y) > 1e-9) allHoriz = false;
  for (let i = 0; i < ds.length; i++) spans.push(ang(ds[i], ds[(i + 1) % ds.length]));
  ok('全部水平（y=0）', allHoriz);
  ok('相邻夹角 45°', spans.every(x => near(x, 45, 1e-6)), spans.map(x => x.toFixed(2)).join(','));
}

console.log('\n[5] createRingShot（ringSpan=0 与 180）');
{
  const flat = D.simulate({ maxTick: 1, level: 2, events: [{ op: 'ring', when: { kind: 'at', a: 0 }, aim: 'look', firstSpeed: 1, way: 8, distance: 1, ringSpan: 0 }] });
  ok('发 8 发', flat.shots.length === 8, flat.shots.length);
  const dirs = flat.shots.map(s => s.angle);
  const same = dirs.every(d => near(d.z, 1, 1e-6) && near(d.x, 0, 1e-6) && near(d.y, 0, 1e-6));
  ok('ringSpan=0：8 发同向 +Z（扁平环）', same, JSON.stringify(dirs[0]));
  // 出生位置用 px[0]（s.pos 是推进 1 tick 之后的坐标）
  const pos = flat.shots.map(s => D.v(s.px[0], s.py[0], s.pz[0]));
  const allR = pos.every(p => near(D.vlen(p), 1, 1e-6));
  ok('ringSpan=0：位置在半径 1 的圆上', allR, pos.map(p => D.vlen(p).toFixed(3)).join(','));
  const inPlane = pos.every(p => Math.abs(p.z) < 1e-6);   // 轴=+Z、起始向量 (0,-1,0) → 环面即 z=0 平面
  ok('ringSpan=0：环面垂直于飞行方向（z 恒 0）', inPlane, pos.map(p => p.z.toFixed(2)).join(','));

  const ball = D.simulate({ maxTick: 1, level: 2, events: [{ op: 'ring', when: { kind: 'at', a: 0 }, aim: 'look', firstSpeed: 1, way: 8, distance: 1, ringSpan: 180 }] });
  const d2 = ball.shots.map(s => s.angle);
  let span = [];
  for (let i = 0; i < d2.length; i++) span.push(ang(d2[i], d2[(i + 1) % d2.length]));
  ok('ringSpan=180：相邻 45°（竖直环）', span.every(x => near(x, 45, 1e-6)), span.map(x => x.toFixed(1)).join(','));
  ok('ringSpan=180：方向落在垂直圆上（z≈0）', d2.every(d => Math.abs(d.z) < 1e-6), d2.map(d => d.z.toFixed(2)).join(','));
}

console.log('\n[6] createSphereShot（ways 表 + 45 上限）');
{
  const sim = D.simulate({ maxTick: 1, level: 2, events: [{ op: 'sphere', when: { kind: 'at', a: 0 }, aim: 'look', firstSpeed: 1, way: 5, distance: 0.3, baseAngle: 0 }] });
  // way=5 → ways[5]={1,3,1} → 2 发单弹 + ring(3) = 2 + 3 = 5
  ok('way=5 展开成 5 发（2 单 + ring3）', sim.shots.length === 5, sim.shots.length);
  const over = D.simulate({ maxTick: 1, level: 2, events: [{ op: 'sphere', when: { kind: 'at', a: 0 }, aim: 'look', firstSpeed: 1, way: 50 }] });
  ok('way=50 ≥ 45 → 静默 return（0 发）', over.shots.length === 0, over.shots.length);
}

console.log('\n[7] 物理：等速 / 加速到限速 / 重力累加 / 延迟 / 寿命');
{
  const one = (extra) => D.simulate({
    maxTick: 40, level: 2,
    events: [Object.assign({ op: 'shot', when: { kind: 'at', a: 0 }, aim: 'look', firstSpeed: 1 }, extra)]
  });
  const s1 = one({});
  const shot = s1.shots[0];
  ok('等速 1 格/tick：第 10 tick 位移 10', near(shot.pz[10], 10, 1e-6), shot.pz[10]);
  ok('无重力时 y 恒 0', near(shot.py[10], 0));

  const s2 = one({ firstSpeed: 0.2, limitSpeed: 1.0, accel: 0.3 });
  const sh2 = s2.shots[0];
  ok('加速被 limitSpeed 钳制', near(D.vlen(D.v(0, 0, sh2.pz[39] - sh2.pz[38])), 1.0, 1e-6) || sh2.pz[39] - sh2.pz[38] <= 1.0001);
  ok('加速后总位移 > 等速 0.2 的 40 倍', sh2.pz[39] > 0.2 * 40, sh2.pz[39]);

  const s3 = one({ gy: -0.03 });
  const sh3 = s3.shots[0];
  ok('重力使 y 递减且加速（抛物线）', sh3.py[39] < sh3.py[30] && (sh3.py[39] - sh3.py[38]) < (sh3.py[20] - sh3.py[19]));

  const s4 = one({ shot: { form: 9, color: 0, size: 0.5, damage: 3, delay: 10, end: 80 } });
  const sh4 = s4.shots[0];
  ok('延迟 10 tick 内不移动', near(sh4.pz[10], 0, 1e-9), sh4.pz[10]);
  ok('延迟结束后开始移动', sh4.pz[12] > 0, sh4.pz[12]);

  const s5 = one({ shot: { form: 9, color: 0, size: 0.5, damage: 3, delay: 0, end: 10 } });
  const sh5 = s5.shots[0];
  ok('end=10 → 轨迹长度约 11~12', sh5.pz.length >= 10 && sh5.pz.length <= 13, sh5.pz.length);
}

console.log('\n[8] 时间轴 when');
{
  ok('at', D.whenMatch({ kind: 'at', a: 3 }, 3) && !D.whenMatch({ kind: 'at', a: 3 }, 4));
  ok('range 左闭右开', D.whenMatch({ kind: 'range', a: 30, b: 98 }, 30) && D.whenMatch({ kind: 'range', a: 30, b: 98 }, 97) && !D.whenMatch({ kind: 'range', a: 30, b: 98 }, 98));
  ok('every+until', D.whenMatch({ kind: 'every', a: 4, b: 10 }, 8) && !D.whenMatch({ kind: 'every', a: 4, b: 10 }, 12) && !D.whenMatch({ kind: 'every', a: 4, b: 10 }, 6));
}

console.log('\n[9] 表达式（求值 + Java 反生成）');
{
  const e = D.E('0.8 + level*0.2');
  ok('eval level=2 → 1.2', near(e.eval({ level: 2 }), 1.2));
  ok('常量折叠识别', D.E('2*3+1').isConst && !D.E('time*2').isConst);
  ok('sin 收度数', near(D.E('sin(90)').eval({}), 1, 1e-12));
  const j = D.E('sin(time*6)').java();
  ok('Java 反生成含 Math.sin 与度数换算', /Math\.sin\(.*Math\.PI \/ 180\.0\)/.test(j), j);
  ok('未知变量报错', (() => { try { D.E('foo').eval({}); return false; } catch (x) { return true; } })());
  ok('语法错误报错', (() => { try { D.E('1+'); return false; } catch (x) { return true; } })());
}

console.log('\n[10] 真实卡复刻：彩光乱舞（THSC_SaikouRanbu 第一段，含复合守卫）');
{
  // 原卡：if(time % 30 < 10 && time % 2 == 1) → createRingShot(..., 15, 0.7, 40+(time%30)*5, 0)
  const card = {
    maxTick: 89, level: 2,
    events: [
      {
        op: 'ring', when: { kind: 'range', a: 0, b: 90 },
        guard: 'time % 30 < 10 && time % 2 == 1',
        aim: 'yawpitch', yaw: 0, pitch: 0,
        firstSpeed: 0.7, limitSpeed: 0.7, accel: 0, gy: -0.03,
        way: 15, distance: 0.7, ringSpan: '40 + time % 30 * 5', baseAngle: 0,
        shot: { form: 11, color: 'time % 4', size: 0.2, damage: 2.5, delay: 0, end: 60 }
      }
    ]
  };
  const sim = D.simulate(card);
  ok('无解析错误', sim.errors.length === 0, JSON.stringify(sim.errors.slice(0, 2)));
  // t ∈ {1,3,5,7,9} × 3 个 30-tick 块 = 15 波 × 15 发 = 225
  ok('15 波 × 15 发 = 225 发', sim.shots.length === 225, sim.shots.length);
  ok('守卫真的在过滤（不是全发射）', sim.shots.length < 90 * 15);
  ok('颜色按 time%4 轮换（首波 time=1 → 1）', sim.shots[0].color === 1, sim.shots[0].color);
  ok('第二块（time=31）颜色 → 3', sim.shots[15].color === 3, sim.shots[15].color);
  // ringSpan = 40 + (time%30)*5：time=1 → 45 → 半角 22.5° → 首发的 y 分量约 -sin(22.5°)≈-0.38
  ok('ringSpan 表达式生效（方向有纵向分量）', Math.abs(sim.shots[0].angle.y) > 0.3, sim.shots[0].angle.y);
  const anyNaN = sim.shots.some(s => !isFinite(s.px[0]) || !isFinite(s.py[0]));
  ok('坐标无 NaN', !anyNaN);
}

console.log('\n[10b] 统一性：way=15 circle 与 ring(ringSpan=0) 的角分布应各自自洽');
{
  const c = D.simulate({ maxTick: 0, level: 2, events: [{ op: 'circle', when: { kind: 'at', a: 0 }, aim: 'look', firstSpeed: 1, way: 15 }] });
  const spans = [];
  for (let i = 0; i < 15; i++) spans.push(ang(c.shots[i].angle, c.shots[(i + 1) % 15].angle));
  ok('circle way=15 → 360/15=24° 均匀', spans.every(x => near(x, 24, 1e-6)), spans.map(x => x.toFixed(1)).join(','));
}

console.log('\n[11] 代码生成');
{
  const card = {
    slug: 'Demo', name: '测试', needLevel: 2, endTime: 80, removeTime: 60, originalUser: 'SAKUYA',
    maxTick: 40, level: 2,
    events: [
      { op: 'ring', when: { kind: 'range', a: 0, b: 40 }, aim: 'look', firstSpeed: 0.9, way: 12, distance: 0.5, ringSpan: 45, rotSpeed: '-3', rotEnd: 9999, shot: { form: 9, color: 3, size: 0.3, damage: 3, delay: 0, end: 50 } },
      { op: 'shot', when: { kind: 'at', a: 10 }, aim: 'dir', dx: 0, dy: 0, dz: 1, firstSpeed: 1.5, gy: -0.03 }
    ]
  };
  const java = D.cardToJava(card);
  ok('含类名 THSC_Demo', /public class THSC_Demo extends THSpellCard/.test(java));
  ok('含 createRingShot', /createRingShot\(/.test(java));
  ok('区间条件生成', /time >= 0 && time < 40/.test(java));
  ok('rotate 参数为负数正确', /-3F/.test(java));
  const json = D.cardToJson(card);
  const parsed = JSON.parse(json);
  ok('JSON 可解析且事件数正确', parsed.events.length === 2);
  ok('JSON 保留表达式字符串', parsed.events[0].rotation_speed === '-3', parsed.events[0].rotation_speed);
  console.log('\n----- 生成 Java 预览（前 28 行）-----');
  console.log(java.split('\n').slice(0, 28).join('\n'));
}

console.log('\n[12] Schema 1.0：字段形态与设计文档 §5 对齐');
{
  const card = {
    id: 46, slug: 'moonlight_ray', name: '月符', nameEn: 'Moonlight Ray',
    needLevel: 3, endTime: 109, removeTime: 99, originalUser: 'RUMIA', circleColor: 'BLUE',
    maxTick: 60, notes: '测试卡',
    vars: { a: 'time*6 + i*(360/7)' },
    events: [
      { op: 'laser_b', when: { kind: 'at', a: 3 }, spray: { way: 7, span: 360 }, spin: -2, spinEnd: 9999, attach: { to: 'user', length: 1, yOffset: 'bbHeight * 0.5' }, laser: { form: 'NORMAL', color: 'BLUE', width: 0.8, length: 40, damage: 6, delay: 10, end: 29 } },
      { op: 'wide', when: { kind: 'range', a: 30, b: 98 }, aim: 'yawpitch', yaw: 0, pitch: 12, firstSpeed: 1, limitSpeed: 0.5, accel: -0.1, gravity: { x: 0, y: -0.03, z: 0 }, spread: { way: 5, span: 60, base: 10 }, distance: 0.4, shot: { form: 9, color: 'time % 7', size: 0.4, damage: 8, delay: 0, end: 55 } },
      { op: 'sound', when: { kind: 'every', a: 4, b: 40 }, sound: true }
    ]
  };
  const o = D.cardToObject(card);
  ok('$schema 为 1', o.$schema === 1);
  ok('name 是 {zh_cn,en_us}', o.name.zh_cn === '月符' && o.name.en_us === 'Moonlight Ray');
  ok('snake_case 顶层字段', o.need_level === 3 && o.end_time === 109 && o.remove_time === 99);
  ok('circle_color 保留', o.circle_color === 'BLUE');
  ok('vars 原样透传', o.vars.a === 'time*6 + i*(360/7)');
  ok('notes 保留', o.notes === '测试卡');
  ok('when at → {at:3}', o.events[0].when.at === 3);
  ok('when range → {from,to}', o.events[1].when.from === 30 && o.events[1].when.to === 98);
  ok('when every → {every,until}', o.events[2].when.every === 4 && o.events[2].when.until === 40);
  ok('laser 嵌套对象存在', !!o.events[0].laser && o.events[0].laser.form === 'NORMAL');
  ok('spray 嵌套（laser_b）', o.events[0].spray.way === 7 && o.events[0].spray.span === 360);
  ok('spread 嵌套（shot）', o.events[1].spread.way === 5 && o.events[1].spread.base === 10);
  ok('attach.y_offset 用蛇形', o.events[0].attach.y_offset === 'bbHeight * 0.5');
  ok('gravity 嵌套对象', o.events[1].gravity.y === -0.03);
  ok('laser_b 自转写 spin/spin_end', o.events[0].spin === -2 && o.events[0].spin_end === 9999);
  ok('shot.form 数字 → 弹型名', o.events[1].shot.form === 'STAR');
  ok('shot 字段 snake_case', o.events[1].shot.damage === 8);
  ok('if 条件键名是 if', (() => {
    const c2 = D.cardToObject({ events: [{ op: 'shot', when: { kind: 'at', a: 0 }, guard: 'time % 2 == 0' }] });
    return c2.events[0].if === 'time % 2 == 0';
  })());
}

console.log('\n[13] Schema 1.0 往返：JSON → 内部模型 → JSON 不变');
{
  const card = {
    id: 47, slug: 'master_spark', name: '恋符', nameEn: 'Master Spark',
    needLevel: 5, endTime: 109, removeTime: 99, originalUser: 'MARISA',
    maxTick: 100, notes: '往返测试',
    vars: { lx: '-sin(cardYaw)', ly: '-sin(cardPitch+90)', lz: 'cos(cardYaw)', g: '0.034 + 0.03*sin(time*6)', a: 'time*6 + i*(360/7)' },
    specials: { bug_white: { id: 3400, on_tick: [{ when: { every: 3 }, set: { color: 'YELLOW' } }] } },
    events: [
      { op: 'spawn', when: { kind: 'at', a: 1 }, anchor: 'default', entity: 'thkaguya:mini_hakkero' },
      { op: 'shot', when: { kind: 'range', a: 30, b: 99 }, anchor: 'card', aim: 'dir', dir: { x: 'cos(a)*lx', y: '-cos(a)*sin(cardPitch+90)', z: 'cos(a)*lz' }, repeat: 7, firstSpeed: 0, limitSpeed: 0.2, accel: 0.3, gravity: { x: '-sin(cardYaw)*g', y: '-sin(cardPitch)*g', z: 'cos(cardYaw)*g' }, shot: { form: 'STAR', color: 'time%7', size: 0.4, damage: 8, delay: 0, end: 55 } }
    ]
  };
  const json1 = D.cardToJson(card);
  const back = D.cardFromJson(json1);
  ok('往返后事件数不变', back.events.length === 2);
  ok('往返后 vars 不变', Object.keys(back.vars).length === 5 && back.vars.a === 'time*6 + i*(360/7)', Object.keys(back.vars).join(','));
  ok('往返后 specials 保留', !!back.specials.bug_white && back.specials.bug_white.id === 3400);
  ok('往返后 dir 三分量保留', back.events[1].dir.x === 'cos(a)*lx');
  ok('往返后 gravity 保留', back.events[1].gravity.y === '-sin(cardPitch)*g');
  ok('往返后 anchor / aim 保留', back.events[1].anchor === 'card' && back.events[1].aim === 'dir');
  ok('往返后 repeat 保留', back.events[1].repeat === 7);
  const json2 = D.cardToJson(back);
  ok('两次导出的 JSON 完全一致', json1 === json2);
  const sim = D.simulate(back);
  ok('往返后的卡能仿真且无错', sim.errors.length === 0, JSON.stringify(sim.errors.slice(0, 2)));
  ok('往返后 7 次 repeat 生效', sim.shots.length === 7 * 69, sim.shots.length);
}

console.log('\n[14] 18 种 op 全量：都能仿真 + 都能生成合法 Java');
{
  const S = (form, color) => ({ form: form, color: color, size: 0.3, damage: 3, delay: 0, end: 40 });
  const L = () => ({ form: 'NORMAL', color: 1, width: 0.2, length: 20, damage: 4, delay: 0, end: 60 });
  const evs = [
    { op: 'shot', when: { kind: 'at', a: 5 }, firstSpeed: 1, limitSpeed: 1, accel: 0, shot: S(9, 0) },
    { op: 'wide', when: { kind: 'at', a: 5 }, spread: { way: 5, span: 60, base: 0 }, distance: 0.3, firstSpeed: 1, shot: S(9, 1) },
    { op: 'circle', when: { kind: 'at', a: 5 }, spray: { way: 8 }, distance: 0.3, firstSpeed: 1, shot: S(9, 2) },
    { op: 'ring', when: { kind: 'at', a: 5 }, spray: { way: 8, span: 360, base: 0 }, distance: 0.3, firstSpeed: 1, rotSpeed: -3, rotEnd: 40, shot: S(9, 3) },
    { op: 'sphere', when: { kind: 'at', a: 5 }, spray: { way: 5, base: 0 }, distance: 0.3, slope: 1, firstSpeed: 1, shot: S(9, 4) },
    { op: 'random_ring', when: { kind: 'at', a: 5 }, num: 8, ringSpan: 360, distance: 0.3, firstSpeed: 1, shot: S(9, 5) },
    { op: 'nuclear', when: { kind: 'at', a: 5 }, slope: 1, rotate: { x: 0, y: 1, z: 0 }, spin: 4, spinEnd: 70, firstSpeed: 1, shot: S(15, 6) },
    { op: 'laser_a', when: { kind: 'at', a: 5 }, firstSpeed: 0.5, laser: L() },
    { op: 'laser_b', when: { kind: 'at', a: 5 }, spray: { way: 7, span: 360 }, spin: -2, spinEnd: 9999, attach: { to: 'user', length: 1, yOffset: 0.5 }, laser: L() },
    { op: 'wide_laser', when: { kind: 'at', a: 5 }, spread: { way: 5, span: 60 }, distance: 0.3, laser: L() },
    { op: 'circle_laser', when: { kind: 'at', a: 5 }, spray: { way: 8 }, distance: 0.3, laser: L() },
    { op: 'ring_laser', when: { kind: 'at', a: 5 }, spray: { way: 8, span: 360 }, distance: 0.3, laser: L() },
    { op: 'sphere_laser', when: { kind: 'at', a: 5 }, spray: { way: 5 }, distance: 0.3, laser: L() },
    { op: 'random_ring_laser', when: { kind: 'at', a: 5 }, num: 8, ringSpan: 360, distance: 0.3, laser: L() },
    { op: 'remove_danmaku', when: { kind: 'at', a: 20 }, range: 8, target: 'Enemy', dropBonus: false },
    { op: 'explosion_effect', when: { kind: 'at', a: 20 }, size: 2 },
    { op: 'spawn', when: { kind: 'at', a: 1 }, entity: 'thkaguya:mini_hakkero' },
    { op: 'sound', when: { kind: 'at', a: 2 }, sound: true }
  ];
  ok('事件表覆盖全部 op', evs.length === D.OPS.length && D.OPS.every(o => evs.some(e => e.op === o)), evs.length + '/' + D.OPS.length);
  const card = { slug: 'AllOps', name: '全 op', needLevel: 1, endTime: 90, removeTime: 60, originalUser: 'REIMU', maxTick: 60, level: 2, events: evs };
  const sim = D.simulate(card);
  ok('18 op 仿真无错', sim.errors.length === 0, JSON.stringify(sim.errors.slice(0, 3)));
  ok('弹幕确实产生了', sim.shots.length > 70, sim.shots.length);
  ok('动作类事件被记录', sim.actions.length >= 4, sim.actions.length);
  const java = D.cardToJava(card);
  const want = ['createShot(', 'createWideShot(', 'createCircleShot(', 'createRingShot(', 'createSphereShot(',
    'createRandomRingShot(', 'createNuclearShot(', 'createLaserA(', 'createLaserB(', 'createWideLaserA(',
    'createCircleLaserA(', 'createRingLaserA(', 'createSphereLaserA(', 'createRandomRingLaser(',
    'danmakuRemove(', 'explosionEffect(', 'spawnEntity(', 'playShotSound('];
  const missing = want.filter(w => java.indexOf(w) < 0);
  ok('Java 覆盖全部生成方法', missing.length === 0, missing.join(','));
  ok('没有 ")F" 这种非法后缀', !/\)\s*F/.test(java), (java.match(/\)\s*F[^a-zA-Z]/) || [''])[0]);
  ok('SIZE[] 后面没乱加 F', !/SIZE\[[^\]]*\]F/.test(java));
  ok('float 参数用 (float) 强转或字面量 F', /(?:\(float\)\(|[\d.]F)/.test(java));
  ok('Java 花括号配平', (java.match(/{/g) || []).length === (java.match(/}/g) || []).length);
  const o = D.cardToObject(card);
  ok('全部 op 都能写成 Schema 1.0', o.events.length === evs.length);
  const back = D.cardFromJson(JSON.stringify(o));
  ok('全部 op 都能从 Schema 1.0 读回', back.events.length === evs.length && back.events.every(e => !!D.OP_INFO[e.op]));
}

console.log('\n[15] 表达式：常量别名 / @ 前缀 / 新函数');
{
  ok('SIZE[FORM_TINY] 可求值', near(D.E('SIZE[FORM_TINY]').eval({}), D.SIZE[1]), D.E('SIZE[FORM_TINY]').eval({}));
  ok('@SIZE[FORM_TINY] 同义', near(D.E('@SIZE[FORM_TINY]').eval({}), D.SIZE[1]));
  ok('DAMAGE[FORM_RICE] 可求值', near(D.E('DAMAGE[FORM_RICE]').eval({}), D.DAMAGE[10]));
  ok('^ 是幂（右结合）', near(D.E('2^3^2').eval({}), 512), D.E('2^3^2').eval({}));
  ok('rand() 走 env.rand', near(D.E('rand()').eval({ rand: () => 0.25 }), 0.25));
  ok('randInt(10) 走 env.rand', near(D.E('randInt(10)').eval({ rand: () => 0.42 }), 4), D.E('randInt(10)').eval({ rand: () => 0.42 }));
  ok('yawOf/pitchOf 可用', near(D.E('yawOf(0,1)').eval({}), 0) && near(D.E('pitchOf(0,1,0)').eval({}), 90));
  ok('tick 等价 time', near(D.E('tick').eval({ time: 7 }), 7));
  ok('e 是自然常数', near(D.E('e').eval({}), Math.E, 1e-9));
  ok('look.x 可求值', near(D.E('look.x').eval({ 'look.x': 0.5 }), 0.5));
  ok('变量宏可以引用 i', (() => {
    const env = { $vars: { a: D.E('i*10') }, i: 3, $depth: 0 };
    return near(D.E('a').eval(env), 30);
  })());
  ok('^ 的 Java 反生成是 Math.pow', /Math\.pow/.test(D.E('2^3').java()), D.E('2^3').java());
}

console.log('\n[16] 预设自检：8 张预设都能仿真 / 校验 / 导出');
{
  const P = require('./presets.js');
  const keys = Object.keys(P);
  ok('预设数量 ≥ 8', keys.length >= 8, keys.length);
  keys.forEach(k => {
    const card = JSON.parse(JSON.stringify(P[k].card));
    let sim = null, err = null;
    try { sim = D.simulate(card); } catch (e) { err = e; }
    ok('[' + k + '] 能仿真', !err && sim && sim.errors.length === 0, err ? err.message : JSON.stringify((sim.errors || []).slice(0, 2)));
    if (!sim) return;
    const issues = D.validateCard(card, sim);
    const bad = issues.filter(i => i.level === 'error');
    ok('[' + k + '] 校验无 error', bad.length === 0, bad.map(b => b.msg).join(' / '));
    const java = D.cardToJava(card);
    ok('[' + k + '] Java 有类名', /public class THSC_[A-Za-z0-9_]+ extends THSpellCard/.test(java));
    ok('[' + k + '] Java 花括号配平', (java.match(/{/g) || []).length === (java.match(/}/g) || []).length);
    const json = D.cardToJson(card);
    ok('[' + k + '] JSON 可解析', (() => { try { JSON.parse(json); return true; } catch (e) { return false; } })());
  });
}

console.log('\n结果：' + pass + ' 通过 / ' + fail + ' 失败\n');
process.exit(fail ? 1 : 0);
