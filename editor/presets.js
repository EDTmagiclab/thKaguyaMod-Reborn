/*
 * presets.js — 示例符卡库（编辑器 UI 与 render-preview.js 的单一来源）
 *
 * 这里存的是编辑器的「内部扁平模型」（camelCase），不是 schema 1.0。
 * 导出 JSON 时由 danmaku-core.js 的 cardToJson() 负责转成 符卡JSON设计文档.md 的 schema。
 *
 * 字段约定见 danmaku-core.js 顶部注释；每个事件的字段名与 THShotLib 的 create* 参数一一对应。
 */
(function (root) {
  'use strict';

  const PRESETS = {

    /* ============================================================
     * 1. 彩符「彩光乱舞」——环形弹 + 表达式调制 + 守卫条件
     * ============================================================ */
    saikou: {
      label: '彩符「彩光乱舞」（环弹张开 + 守卫）',
      card: {
        name: '彩符「彩光乱舞」', nameEn: 'Saikou Ranbu', slug: 'SaikouRanbuDemo', id: 201,
        needLevel: 2, endTime: 80, removeTime: 60, originalUser: 'MEIRIN',
        maxTick: 90, userYaw: 0, level: 2,
        notes: '环弹的纵向张角 ringSpan 随 time 线性张开；第二段用确定性三角函数代替原版 rand 散弹。',
        vars: {}, specials: {},
        events: [
          {
            when: { kind: 'range', a: 0, b: 90 }, op: 'ring',
            guard: 'time % 30 < 10 && time % 2 == 1',
            aim: 'look', anchor: 'user_eye',
            firstSpeed: 0.7, limitSpeed: 0.7, accel: 0, gravity: { x: 0, y: -0.03, z: 0 },
            way: 15, distance: 0.7, ringSpan: '40 + time % 30 * 5', baseAngle: 0,
            shot: { form: 11, color: 'time % 4', size: 0.2, damage: 2.5, delay: 0, end: 60 },
            note: '晶体弹环，ringSpan 随 time 张开'
          },
          {
            when: { kind: 'range', a: 10, b: 20 }, op: 'shot', repeat: 6,
            aim: 'dir', dir: { x: 'sin(i * 137)', y: 'cos(i * 97)', z: 'sin(i * 61)' },
            firstSpeed: 1.5, limitSpeed: 0.4, accel: -0.4,
            shot: { form: 11, color: '7 - i % 3', size: 0.2, damage: 3.5, delay: 0, end: 50 },
            note: '减速散弹（原版用 rand，这里用确定性三角函数代替）'
          }
        ]
      }
    },

    /* ============================================================
     * 2. 恋符「Master Spark」——整圈旋转 + 正弦重力调制
     * ============================================================ */
    spark: {
      label: '恋符「Master Spark」（整圈旋转 + 重力调制）',
      card: {
        name: '恋符「マスタースパーク」', nameEn: 'Master Spark', slug: 'MasterSparkDemo', id: 202,
        needLevel: 2, endTime: 110, removeTime: 70, originalUser: 'MARISA',
        maxTick: 110, userYaw: 0, level: 2,
        notes: 'circle 弹整圈均匀分布、整体以 6°/tick 旋转；重力按 sin(time*6) 调制形成马鞍形。',
        vars: {}, specials: {},
        events: [{
          when: { kind: 'range', a: 30, b: 99 }, op: 'circle',
          aim: 'look', anchor: 'user_eye',
          firstSpeed: 0.0, limitSpeed: 0.2, accel: 0.3,
          gravity: { x: 0, y: '-(0.034 + 0.03 * sin(time * 6))', z: 0 },
          way: 7, baseAngle: 'time * 6',
          shot: { form: 9, color: 'time % 7', size: 0.4, damage: 8, delay: 0, end: 55 },
          note: '整圈旋转 + 逐发换色 + 重力正弦调制'
        }]
      }
    },

    /* ============================================================
     * 3. 月符「月光曲」——laser_b 附着自转激光（真实还原 46_moonlight_ray.json）
     * ============================================================ */
    moon: {
      label: '月符「月光曲」（laser_b 附着自转 × 7）',
      card: {
        name: '月符「ムーンライトレイ」', nameEn: 'Moonlight Ray', slug: 'MoonlightRayDemo', id: 46,
        needLevel: 1, endTime: 60, removeTime: 30, originalUser: 'RUMIA',
        maxTick: 80, userYaw: 0, level: 2,
        notes: '第 3 tick 发 7 束激光，绕视线轴均分 360°，贴在玩家身上每 tick 转 -2° 永不停止。',
        vars: {}, specials: {},
        events: [{
          when: { kind: 'at', a: 3 }, op: 'laser_b',
          aim: 'look', anchor: 'user_eye',
          spray: { way: 7, span: 360 },
          laser: {
            form: 0, color: 'BLUE', width: '0.8 + level * 0.2', length: 40,
            damage: 6, delay: 10, end: '29 + level * 5'
          },
          spin: -2.0, spinEnd: 9999,
          attach: { to: 'user', length: 1.0, yOffset: 'bbHeight * 0.5' },
          note: 'rotate 轴由解释器按 cross(look, dir_i) 自动算'
        }]
      }
    },

    /* ============================================================
     * 4. 球面弹——ways 配比表
     * ============================================================ */
    sphere: {
      label: '球面弹（sphere way=20，20 向均匀分布）',
      card: {
        name: '球面弹演示', nameEn: 'Sphere Demo', slug: 'SphereDemo', id: 203,
        needLevel: 1, endTime: 100, removeTime: 60, originalUser: 'OTHER',
        maxTick: 100, userYaw: 0, level: 2,
        notes: 'ways[20]={1,3,6,6,3,1} → 20 个均匀方向；RAINBOW 逐发取 0–6。',
        vars: {}, specials: {},
        events: [{
          when: { kind: 'every', a: 5, b: 80 }, op: 'sphere',
          aim: 'look', anchor: 'user_eye',
          firstSpeed: 0.85, limitSpeed: 0.15, accel: -0.008,
          way: 20, distance: 0.6, baseAngle: 0, slope: 1,
          shot: { form: 9, color: 9, size: 'SIZE[9]', damage: 3, delay: 0, end: 72 },
          note: 'RAINBOW 逐发取色'
        }]
      }
    },

    /* ============================================================
     * 5. 迷你八卦炉——vars + spawn + anchor=card + aim=dir（还原 47_master_spark.json）
     * ============================================================ */
    hakkero: {
      label: '恋符「Master Spark」原卡（vars + spawn + dir）',
      card: {
        name: '恋符「マスタースパーク」（原卡结构）', nameEn: 'Master Spark (raw)', slug: 'MasterSparkRaw', id: 47,
        needLevel: 5, endTime: 109, removeTime: 99, originalUser: 'MARISA',
        maxTick: 110, userYaw: 0, level: 5,
        notes: 'vars 是懒求值的「宏」：a 里引用 i，所以 7 发弹各自算出自己的方向。',
        vars: {
          lx: '-sin(cardYaw)*cos(cardPitch+90)',
          ly: '-sin(cardPitch+90)',
          lz: 'cos(cardYaw)*cos(cardPitch+90)',
          g: '0.034 + 0.03*sin(time*6)',
          a: 'time*6 + i*(360/7)'
        },
        specials: {},
        events: [
          {
            when: { kind: 'at', a: 1 }, op: 'spawn',
            entity: 'thkaguya:mini_hakkero', anchor: 'default',
            note: '第 1 tick 放迷你八卦炉实体'
          },
          {
            when: { kind: 'range', a: 30, b: 99 }, op: 'shot',
            anchor: 'card', aim: 'dir',
            dir: {
              x: 'cos(a)*lx + sin(a)*cos(cardYaw)',
              y: '-cos(a)*sin(cardPitch+90)',
              z: 'cos(a)*lz + sin(a)*sin(cardYaw)'
            },
            repeat: 7, firstSpeed: 0.0, limitSpeed: 0.2, accel: 0.3,
            gravity: { x: '-sin(cardYaw)*cos(cardPitch)*g', y: '-sin(cardPitch)*g', z: 'cos(cardYaw)*cos(cardPitch)*g' },
            shot: { form: 'STAR', color: 'time%7', size: 0.4, damage: 8.0, delay: 0, end: 55 },
            note: '整圈均匀分布 + 6°/tick 旋转 + sin 调制重力'
          }
        ]
      }
    },

    /* ============================================================
     * 6. 核弹——nuclear op 演示
     * ============================================================ */
    nuclear: {
      label: '核弹（nuclear：单发 + spin 旋转）',
      card: {
        name: '核弹演示', nameEn: 'Nuclear Demo', slug: 'NuclearDemo', id: 204,
        needLevel: 3, endTime: 90, removeTime: 50, originalUser: 'OTHER',
        maxTick: 90, userYaw: 0, level: 2,
        notes: 'nuclear 是「一发大弹 + 绕 rotate 轴自转」，rotate 轴取默认值 getVecFromAngle(yaw, pitch+90)。',
        vars: {}, specials: {},
        events: [{
          when: { kind: 'at', a: 5 }, op: 'nuclear',
          aim: 'look', anchor: 'user_eye',
          firstSpeed: 0.9, limitSpeed: 0.9, accel: 0,
          slope: 1, rotate: { x: 0, y: 1, z: 0 }, spin: 4, spinEnd: 70,
          shot: { form: 15, color: 'ORANGE', size: 'SIZE[FORM_BIGLIGHT]*1.6', damage: 12, delay: 0, end: 70 },
          note: '自转 4°/tick，到第 70 tick 停'
        }]
      }
    },

    /* ============================================================
     * 7. 小虫风暴——vars 驱动的 yawpitch + specials 状态机透传（还原 48_little_bug_storm.json）
     * ============================================================ */
    bug: {
      label: '小虫风暴（vars + yawpitch + specials 透传）',
      card: {
        name: '风符「小さな虫の嵐」', nameEn: 'Little Bug Storm', slug: 'LittleBugStormDemo', id: 48,
        needLevel: 4, endTime: 100, removeTime: 40, originalUser: 'WAKASAGIHIME',
        maxTick: 100, userYaw: 0, level: 2,
        notes: 'specials 是 §7 的状态机 DSL，编辑器只做「原样透传」：在右侧 specials 面板改 JSON，导出时原样带出，Java 导出不含状态机。',
        vars: { way: '9', yaw: 'time * 7', pitch: '20 * sin(time * 3)' },
        specials: {
          bug_white: {
            id: 3400,
            on_end: [{
              actions: [
                { set: { color: 'WHITE', size: '@SIZE[FORM_SMALL]', end_time: 20 } },
                { rotate: 90 },
                { set: { speed: 3.0, limit_speed: 0.0, acceleration: -1.0 } },
                { respawn: true },
                { next: 'bug_light' }
              ]
            }]
          },
          bug_light: {
            id: 3401,
            on_end: [{
              actions: [
                { set: { form: 'LIGHT', color: 'YELLOW', damage: 4.0, size: '@SIZE[FORM_TINY]' } },
                { respawn: true },
                { special_end: true }
              ]
            }]
          }
        },
        events: [
          {
            when: { kind: 'range', a: 0, b: 15 }, op: 'sphere',
            aim: 'yawpitch', yaw: 'yaw', pitch: 'pitch',
            spray: { way: 'way', base: 1.4 },
            slope: 1.0, firstSpeed: 1.0, limitSpeed: 0.0, accel: '-0.05 - time*0.001',
            distance: 0.3,
            shot: { form: 'TINY', color: 'WHITE', size: '@SIZE[FORM_TINY]', damage: 2.0, delay: 0, end: 50, special: 'bug_white' },
            sound: true,
            note: 'way/yaw/pitch 都是 vars 宏'
          },
          {
            when: { kind: 'range', a: 15, b: 40 }, op: 'sphere',
            aim: 'yawpitch', yaw: '-yaw', pitch: 'pitch',
            spray: { way: 'way', base: 1.4 },
            slope: 1.0, firstSpeed: 1.0, limitSpeed: 0.0, accel: '-0.05 - time*0.001',
            distance: 0.3,
            shot: { form: 'TINY', color: 'GREEN', size: '@SIZE[FORM_TINY]', damage: 2.0, delay: 0, end: 50, special: 'bug_white' },
            note: '反向旋转'
          }
        ]
      }
    },

    /* ============================================================
     * 8. 空白
     * ============================================================ */
    blank: {
      label: '空白（从零开始）',
      card: {
        name: '新符卡', nameEn: 'New Card', slug: 'NewCard', id: 200,
        needLevel: 1, endTime: 100, removeTime: 60, originalUser: 'OTHER',
        maxTick: 100, userYaw: 0, level: 2,
        notes: '', vars: {}, specials: {},
        events: [{
          when: { kind: 'range', a: 0, b: 100 }, op: 'ring',
          aim: 'look', anchor: 'user_eye',
          firstSpeed: 0.8, limitSpeed: 0.8, accel: 0,
          way: 12, distance: 0.5, ringSpan: 0, baseAngle: 0,
          shot: { form: 9, color: 3, size: 0.3, damage: 3, delay: 0, end: 60 }
        }]
      }
    }
  };

  root.DMK_PRESETS = PRESETS;
  if (typeof module !== 'undefined' && module.exports) module.exports = PRESETS;
})(typeof window !== 'undefined' ? window : globalThis);
