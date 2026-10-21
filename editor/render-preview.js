/*
 * render-preview.js — 不依赖浏览器，把 DMK 的仿真结果光栅化成 PNG（2×2 四帧）。
 * 用途：亲眼确认 create* 族生成的 3D 几何是否合理（有没有反向飞、有没有聚成一团）。
 * 用法：node render-preview.js [preset] > 见 out-preview.png
 */
const fs = require('fs');
const zlib = require('zlib');
const D = require('./danmaku-core.js');

/* ---------- 极小 PNG 编码器 ---------- */
const CRC = (() => {
  const t = new Int32Array(256);
  for (let n = 0; n < 256; n++) {
    let c = n;
    for (let k = 0; k < 8; k++) c = c & 1 ? 0xEDB88320 ^ (c >>> 1) : c >>> 1;
    t[n] = c;
  }
  return t;
})();
function crc32(buf) {
  let c = -1;
  for (let i = 0; i < buf.length; i++) c = CRC[(c ^ buf[i]) & 0xFF] ^ (c >>> 8);
  return (c ^ -1) >>> 0;
}
function chunk(type, data) {
  const len = Buffer.alloc(4);
  len.writeUInt32BE(data.length);
  const td = Buffer.concat([Buffer.from(type, 'ascii'), data]);
  const crc = Buffer.alloc(4);
  crc.writeUInt32BE(crc32(td));
  return Buffer.concat([len, td, crc]);
}
function png(w, h, rgb) {
  const ihdr = Buffer.alloc(13);
  ihdr.writeUInt32BE(w, 0); ihdr.writeUInt32BE(h, 4);
  ihdr[8] = 8; ihdr[9] = 2; ihdr[10] = 0; ihdr[11] = 0; ihdr[12] = 0;
  const raw = Buffer.alloc((w * 3 + 1) * h);
  for (let y = 0; y < h; y++) {
    raw[y * (w * 3 + 1)] = 0;
    rgb.copy(raw, y * (w * 3 + 1) + 1, y * w * 3, (y + 1) * w * 3);
  }
  return Buffer.concat([
    Buffer.from([0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A]),
    chunk('IHDR', ihdr), chunk('IDAT', zlib.deflateSync(raw, { level: 9 })), chunk('IEND', Buffer.alloc(0))
  ]);
}

/* ---------- 光栅 ---------- */
const W = 520, H = 330;
const rgb = Buffer.alloc(W * H * 3);
function px(x, y, r, g, b, a) {
  x |= 0; y |= 0;
  if (x < 0 || y < 0 || x >= W || y >= H) return;
  const i = (y * W + x) * 3;
  const al = a === undefined ? 1 : a;
  rgb[i] = rgb[i] * (1 - al) + r * al;
  rgb[i + 1] = rgb[i + 1] * (1 - al) + g * al;
  rgb[i + 2] = rgb[i + 2] * (1 - al) + b * al;
}
function disc(cx, cy, r, col, a) {
  const ri = Math.ceil(r) + 1;
  for (let y = -ri; y <= ri; y++) {
    for (let x = -ri; x <= ri; x++) {
      const d = Math.sqrt(x * x + y * y);
      if (d <= r) px(cx + x, cy + y, col[0], col[1], col[2], a * Math.min(1, (r - d + 1)));
    }
  }
}
function line(x0, y0, x1, y1, col, a, w) {
  const n = Math.max(Math.abs(x1 - x0), Math.abs(y1 - y0)) | 0;
  for (let i = 0; i <= n; i++) {
    const t = n ? i / n : 0;
    disc(x0 + (x1 - x0) * t, y0 + (y1 - y0) * t, w, col, a);
  }
}
const hex2rgb = h => [parseInt(h.slice(1, 3), 16), parseInt(h.slice(3, 5), 16), parseInt(h.slice(5, 7), 16)];

function frame(sim, tick, camYaw, camPitch, fov) {
  // 背景
  for (let y = 0; y < H; y++) {
    const t = y / H;
    const r = 247 - 20 * t, g = 249 - 22 * t, b = 254 - 16 * t;
    for (let x = 0; x < W; x++) {
      const i = (y * W + x) * 3;
      rgb[i] = r; rgb[i + 1] = g; rgb[i + 2] = b;
    }
  }
  const f = (H / 2) / Math.tan(fov * Math.PI / 360);
  const cyaw = Math.cos(camYaw * Math.PI / 180), syaw = Math.sin(camYaw * Math.PI / 180);
  const cp = Math.cos(camPitch * Math.PI / 180), sp = Math.sin(camPitch * Math.PI / 180);
  function proj(p) {
    const x1 = p.x * cyaw - p.z * syaw;
    const z1 = p.x * syaw + p.z * cyaw;
    const y2 = p.y * cp + z1 * sp;
    const z2 = -p.y * sp + z1 * cp;
    if (z2 <= 0.1) return null;
    return { x: W / 2 + f * x1 / z2, y: H / 2 - f * y2 / z2, z: z2 };
  }
  // 地面网格
  const gc = [150, 165, 195];
  for (let z = 2; z <= 60; z += 4) {
    const a = proj({ x: -24, y: -1.62, z }), b = proj({ x: 24, y: -1.62, z });
    if (a && b) line(a.x, a.y, b.x, b.y, gc, 0.25, 0.45);
  }
  for (let x = -24; x <= 24; x += 4) {
    const a = proj({ x, y: -1.62, z: 0.4 }), b = proj({ x, y: -1.62, z: 60 });
    if (a && b) line(a.x, a.y, b.x, b.y, gc, 0.25, 0.45);
  }

  const list = D.stateAt(sim, tick).map(s => {
    const p = proj(s);
    return p ? { s, p } : null;
  }).filter(Boolean).sort((a, b) => b.p.z - a.p.z);

  // 激光
  for (const it of list) {
    if (it.s.kind !== 'laser') continue;
    const q = proj({ x: it.s.x + it.s.ax * it.s.length, y: it.s.y + it.s.ay * it.s.length, z: it.s.z + it.s.az * it.s.length });
    if (!q) continue;
    const col = hex2rgb(D.PALETTE[it.s.color] || '#ffffff');
    const w = Math.max(0.6, f * it.s.width / it.p.z * 0.5);
    line(it.p.x, it.p.y, q.x, q.y, col, 0.22, w * 2);
    line(it.p.x, it.p.y, q.x, q.y, [255, 255, 255], 0.55, w * 0.6);
  }
  for (const it of list) {
    if (it.s.kind === 'laser') continue;
    const r = f * it.s.size * 0.5 / it.p.z;
    if (r < 0.3) continue;
    const col = hex2rgb(D.PALETTE[it.s.color] || '#ffffff');
    disc(it.p.x, it.p.y, Math.min(r, 14) * 1.7, col, 0.18);
    disc(it.p.x, it.p.y, Math.min(r, 12), col, 0.95);
    if (r > 2.2) disc(it.p.x, it.p.y - r * 0.15, Math.max(0.6, r * 0.32), [255, 255, 255], 0.8);
  }
  // 标注
  return list.length;
}

/* ---------- 组合 2×2 ---------- */
const presets = {
  saikou: {
    name: '彩光乱舞', maxTick: 90, ticks: [0, 20, 40, 60], yaw: 0, pitch: 12,
    events: [
      { op: 'ring', when: { kind: 'range', a: 0, b: 90 }, guard: 'time % 30 < 10 && time % 2 == 1', aim: 'look', firstSpeed: 0.7, limitSpeed: 0.7, accel: 0, gy: -0.03, way: 15, distance: 0.7, ringSpan: '40 + time % 30 * 5', baseAngle: 0, shot: { form: 11, color: 'time % 4', size: 0.2, damage: 2.5, delay: 0, end: 60 } },
      { op: 'shot', when: { kind: 'range', a: 10, b: 20 }, repeat: 6, aim: 'dir', dx: 'sin(i * 137)', dy: 'cos(i * 97)', dz: 'sin(i * 61)', firstSpeed: 1.5, limitSpeed: 0.4, accel: -0.4, shot: { form: 11, color: '7 - i % 3', size: 0.2, damage: 3.5, delay: 0, end: 50 } }
    ]
  },
  spark: {
    name: 'Master Spark', maxTick: 110, ticks: [30, 45, 60, 80], yaw: 0, pitch: 6,
    events: [{ op: 'circle', when: { kind: 'range', a: 30, b: 99 }, aim: 'look', firstSpeed: 0, limitSpeed: 0.2, accel: 0.3, gy: '-(0.034 + 0.03 * sin(time * 6))', way: 7, baseAngle: 'time * 6', shot: { form: 9, color: 'time % 7', size: 0.4, damage: 8, delay: 0, end: 55 } }]
  },
  sphere: {
    name: 'sphere way=20', maxTick: 90, ticks: [8, 20, 34, 50], yaw: 35, pitch: 10,
    events: [{ op: 'sphere', when: { kind: 'every', a: 6, b: 80 }, aim: 'look', firstSpeed: 1.1, limitSpeed: 0.15, accel: -0.01, way: 20, distance: 0.6, baseAngle: 0, slope: 1, shot: { form: 9, color: 9, size: 'SIZE[9]', damage: 3, delay: 0, end: 70 } }]
  },
  ring0: {
    name: 'ring way=16 ringSpan=0', maxTick: 60, ticks: [1, 8, 16, 26], yaw: 40, pitch: 18,
    events: [{ op: 'ring', when: { kind: 'at', a: 0 }, aim: 'look', firstSpeed: 0.9, way: 16, distance: 1.2, ringSpan: 0, baseAngle: 0, shot: { form: 4, color: 5, size: 0.3, damage: 3, delay: 0, end: 60 } }]
  }
};

const which = process.argv[2] || 'saikou';
const p = presets[which];
const sim = D.simulate({ maxTick: p.maxTick, level: 2, events: p.events });
console.log(which + ': shots=' + sim.shots.length + ' errors=' + sim.errors.length);
if (sim.errors.length) console.log(sim.errors.slice(0, 3));

const GW = W * 2, GH = H * 2;
const out = Buffer.alloc(GW * GH * 3);
p.ticks.forEach((t, i) => {
  frame(sim, t, p.yaw, p.pitch, 74);
  const ox = (i % 2) * W, oy = Math.floor(i / 2) * H;
  for (let y = 0; y < H; y++) {
    rgb.copy(out, ((oy + y) * GW + ox) * 3, y * W * 3, (y + 1) * W * 3);
  }
  // 分隔线
  console.log('  frame t=' + t + ' shots on screen=' + D.stateAt(sim, t).length);
});
fs.writeFileSync('out-' + which + '.png', png(GW, GH, out));
console.log('已写出 out-' + which + '.png');
