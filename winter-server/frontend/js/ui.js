/**
 * ui.js — UI更新、建筑系统、资源采集
 */

function updateUI() {
  document.getElementById("res-wood").textContent = State.wood;
  document.getElementById("res-coal").textContent = State.coal;
  document.getElementById("res-food").textContent = State.food;
  document.getElementById("player-name").textContent = State.playerName;
  document.getElementById("player-level").textContent = State.level;
  document.getElementById("player-id").textContent = State.playerId;
  renderBuildings();
}

// ═══════════ 建筑系统 ═══════════

function renderBuildings() {
  const container = document.getElementById("building-list");
  const types = Object.keys(State.buildings);
  if (types.length === 0) {
    container.innerHTML =
      '<div style="color:#555;font-size:11px;text-align:center;padding:12px">还没有建筑，快去建造吧！</div>';
    return;
  }
  container.innerHTML = types
    .map((t) => {
      const b = State.buildings[t];
      const info = BUILDING_TYPES[t];
      return `<div class="building-card">
  <div class="bname">${info.icon} ${info.name}</div>
  <div class="blevel">等级 ${b.level} · ${info.desc}</div>
  <div class="bactions">
    <button class="btn-sm btn-upgrade" onclick="upgradeBuilding(${t})">⬆ 升级</button>
  </div>
</div>`;
    })
    .join("");
}

function createBuilding() {
  const type = parseInt(document.getElementById("new-building-type").value);
  if (State.buildings[type]) {
    toast("该建筑已存在，请直接升级", "warning");
    return;
  }
  const info = BUILDING_TYPES[type];
  const cost = 50;
  if (State.wood < cost) {
    toast("木材不足！需要 " + cost, "error");
    return;
  }
  State.wood -= cost;
  State.buildings[type] = { type, level: 1, name: info.name };
  updateUI();
  toast(`${info.icon} ${info.name} 建造成功！`, "success");
  addSystemMsg(`建造了 ${info.name}（等级1）`);
}

function upgradeBuilding(type) {
  const b = State.buildings[type];
  if (!b) return;
  const cost = b.level * 80;
  if (State.wood < cost) {
    toast(`升级需要 ${cost} 木材`, "error");
    return;
  }
  State.wood -= cost;
  b.level++;
  updateUI();
  const info = BUILDING_TYPES[type];
  toast(`${info.icon} ${info.name} 升级到 ${b.level} 级！`, "success");
  addSystemMsg(`${info.name} 升级到了 等级${b.level}`);
}

// ═══════════ 资源采集 ═══════════

function collectResource(resType) {
  const amounts = { wood: 30, coal: 20, food: 25 };
  const names = { wood: "木材", coal: "煤炭", food: "食物" };
  const icons = { wood: "🪵", coal: "⛏️", food: "🌾" };

  let bonus = 1;
  if (resType === "wood" && State.buildings[3])
    bonus += State.buildings[3].level * 0.2;
  if (resType === "coal" && State.buildings[1])
    bonus += State.buildings[1].level * 0.2;
  if (resType === "food") bonus += 0.1;

  const amount = Math.floor(amounts[resType] * bonus);
  State[resType] += amount;
  updateUI();
  toast(
    `${icons[resType]} 采集了 ${amount} ${names[resType]}${bonus > 1 ? ` (建筑加成 x${bonus.toFixed(1)})` : ""}`,
    "success",
  );
}

// ═══════════ Tab 切换 ═══════════

function switchTab(id, el) {
  document
    .querySelectorAll(".right-panel .tab-btn")
    .forEach((t) => t.classList.remove("active"));
  document
    .querySelectorAll(".right-panel .tab-content")
    .forEach((t) => t.classList.remove("active"));
  el.classList.add("active");
  document.getElementById("tab-" + id).classList.add("active");
}
