/**
 * config.js — 全局常量和状态
 */

const API_BATTLE = "http://localhost:18088";

const State = {
  loggedIn: false,
  playerId: 0,
  playerName: "",
  level: 1,
  wood: 100,
  coal: 50,
  food: 80,
  x: 400,
  y: 300,
  buildings: {},
  friends: [],
  friendRequests: [],
  nearbyPlayers: [],
  chatMessages: { world: [], private: [], system: [] },
  currentChatTab: "world",
};

// 建筑类型映射
const BUILDING_TYPES = {
  1: { name: "熔炉", icon: "🏭", desc: "冶炼矿石，提高煤炭采集效率" },
  2: { name: "兵营", icon: "⚔️", desc: "训练士兵，提高战斗力" },
  3: { name: "伐木场", icon: "🪓", desc: "砍伐树木，提高木材采集效率" },
};

// 技能配置（从后端配置表复制）
const SKILL_CONFIG = [
  {
    id: 1001,
    name: "战前鼓舞",
    desc: "战斗开始时，全军伤害增加20%",
    trigger: 0,
  },
  {
    id: 1002,
    name: "先发制人",
    desc: "战斗开始时，若自身血量高于80%，暴击率提升30%",
    trigger: 0,
  },
  {
    id: 1003,
    name: "开战祝福",
    desc: "战斗开始时，给自身施加攻击强化Buff，持续3回合",
    trigger: 0,
  },
  {
    id: 1101,
    name: "战场急救",
    desc: "回合开始时，若自身血量低于30%，恢复20%最大生命值",
    trigger: 1,
  },
  {
    id: 1102,
    name: "坚韧意志",
    desc: "回合开始时，如果自身Buff数量>=1，降低所有技能1回合冷却",
    trigger: 1,
  },
  {
    id: 1103,
    name: "铁壁防御",
    desc: "回合开始时，50%概率给自身施加护盾Buff，持续2回合",
    trigger: 1,
  },
  {
    id: 1104,
    name: "持久作战",
    desc: "第5回合起，回合开始时恢复10%最大生命值并增加15%伤害",
    trigger: 1,
  },
  {
    id: 1105,
    name: "命运转盘",
    desc: "回合开始时30%概率随机施加一个Buff给自身或敌方",
    trigger: 1,
  },
  {
    id: 1201,
    name: "蓄力打击",
    desc: "攻击前，若连续2回合未暴击，下次攻击必定暴击",
    trigger: 2,
  },
  {
    id: 1202,
    name: "破甲攻击",
    desc: "攻击前，降低目标20%防御，持续2回合",
    trigger: 2,
  },
  {
    id: 1203,
    name: "吸血攻击",
    desc: "攻击前，本次攻击附带30%吸血效果",
    trigger: 2,
  },
  {
    id: 1204,
    name: "连击准备",
    desc: "攻击前，20%概率本回合攻击两次",
    trigger: 2,
  },
  {
    id: 1301,
    name: "追击打击",
    desc: "攻击后，若目标血量低于50%，额外造成20%伤害",
    trigger: 3,
  },
  {
    id: 1302,
    name: "嗜血本能",
    desc: "攻击后，本次攻击造成的伤害10%转化为自身生命值",
    trigger: 3,
  },
  {
    id: 1303,
    name: "致残打击",
    desc: "攻击后，30%概率使目标眩晕1回合",
    trigger: 3,
  },
  {
    id: 1304,
    name: "毒刃",
    desc: "攻击后，给目标施加中毒Buff，持续2回合",
    trigger: 3,
  },
  {
    id: 1401,
    name: "反击风暴",
    desc: "被攻击时，30%概率反弹30%伤害",
    trigger: 4,
  },
  {
    id: 1402,
    name: "坚守意志",
    desc: "被攻击时，如果伤害超过最大生命值15%，减少50%伤害",
    trigger: 4,
  },
  {
    id: 1403,
    name: "强化护盾",
    desc: "被攻击时，若血量高于50%，50%概率获得护盾1回合",
    trigger: 4,
  },
  {
    id: 1404,
    name: "以牙还牙",
    desc: "被攻击时，给攻击者施加虚弱2回合",
    trigger: 4,
  },
  { id: 1501, name: "同归于尽", desc: "死亡时对敌方造成100%伤害", trigger: 5 },
  {
    id: 1502,
    name: "亡者诅咒",
    desc: "死亡时给击杀者施加中毒和虚弱各3回合",
    trigger: 5,
  },
  {
    id: 1503,
    name: "英灵庇护",
    desc: "死亡时恢复友军25%最大生命值",
    trigger: 5,
  },
  {
    id: 1601,
    name: "回合恢复",
    desc: "回合结束时，若血量低于50%，恢复10%最大生命值",
    trigger: 6,
  },
  {
    id: 1602,
    name: "蓄势待发",
    desc: "回合结束时，降低冷却1回合并增加10%暴击率",
    trigger: 6,
  },
  {
    id: 1603,
    name: "持续毒雾",
    desc: "第3回合起，回合结束时给目标施加中毒2回合",
    trigger: 6,
  },
  {
    id: 1701,
    name: "逆境爆发",
    desc: "自身血量低于30%且第4回合起，伤害+120%并吸血50%",
    trigger: 2,
  },
  {
    id: 1702,
    name: "精准处刑",
    desc: "目标血量低于25%且有虚弱Buff时，伤害+150%，暴击+80%",
    trigger: 2,
  },
  {
    id: 1703,
    name: "全面反击",
    desc: "被攻击时有护盾且血量>40%，反弹50%伤害并眩晕1回合",
    trigger: 4,
  },
];

const TRIGGER_NAMES = [
  "⚡战斗开始",
  "🔄回合开始",
  "⚔️攻击前",
  "💥攻击后",
  "🛡️被攻击",
  "💀死亡时",
  "🔚回合结束",
  "🎯高级",
];

// 已选技能集合
const selectedSkills = { atk: new Set(), def: new Set() };

// 玩家名称列表
const fakePlayerNames = [
  "冰霜骑士",
  "暗夜刺客",
  "圣光法师",
  "风暴射手",
  "铁血战士",
  "幽灵猎人",
  "雷电法王",
  "破晓勇者",
  "寒冰公主",
  "烈焰龙骑",
  "影舞者",
  "碎星弓手",
  "钢铁卫士",
  "毒蛇刺客",
  "圣殿守卫",
];
