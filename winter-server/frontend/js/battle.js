/**
 * battle.js — PvP战斗系统与战斗动画
 */

let battleResult = null;
let battleSpeed = 1;
let battleAnimTimer = null;

// ═══════════ 技能面板 ═══════════

function initSkillChips() {
  ["atk", "def"].forEach((side) => {
    const filterEl = document.getElementById(`${side}-skill-filter`);
    const chipsEl = document.getElementById(`${side}-skills`);

    filterEl.innerHTML =
      '<button class="active" onclick="filterSkills(\'' +
      side +
      "',-1,this)\">全部</button>" +
      TRIGGER_NAMES.map(
        (n, i) =>
          `<button onclick="filterSkills('${side}',${i},this)">${n}</button>`,
      ).join("");

    chipsEl.innerHTML = SKILL_CONFIG.map(
      (s) =>
        `<div class="skill-chip" data-trigger="${s.trigger}" data-id="${s.id}" 
     title="${s.desc}" onclick="toggleSkill('${side}',${s.id},this)">
  ${s.name}
</div>`,
    ).join("");
  });
}

function filterSkills(side, trigger, btn) {
  const container = document.getElementById(`${side}-skill-filter`);
  container
    .querySelectorAll("button")
    .forEach((b) => b.classList.remove("active"));
  btn.classList.add("active");

  const chips = document
    .getElementById(`${side}-skills`)
    .querySelectorAll(".skill-chip");
  chips.forEach((chip) => {
    if (trigger === -1 || parseInt(chip.dataset.trigger) === trigger) {
      chip.style.display = "";
    } else {
      chip.style.display = "none";
    }
  });
}

function toggleSkill(side, skillId, el) {
  if (selectedSkills[side].has(skillId)) {
    selectedSkills[side].delete(skillId);
    el.classList.remove("selected");
  } else {
    selectedSkills[side].add(skillId);
    el.classList.add("selected");
  }
}

// ═══════════ 战斗模态窗 ═══════════

function openBattleModal() {
  document.getElementById("battle-modal").classList.add("show");
  document.getElementById("battle-setup").style.display = "block";
  document.getElementById("battle-arena").classList.remove("active");
}

function closeBattleModal() {
  document.getElementById("battle-modal").classList.remove("show");
  if (battleAnimTimer) {
    clearTimeout(battleAnimTimer);
    battleAnimTimer = null;
  }
}

function backToSetup() {
  document.getElementById("battle-setup").style.display = "block";
  document.getElementById("battle-arena").classList.remove("active");
  if (battleAnimTimer) {
    clearTimeout(battleAnimTimer);
    battleAnimTimer = null;
  }
}

// ═══════════ PK入口 ═══════════

function startPKWith(player) {
  if (!player) return;
  openBattleModal();
  document.getElementById("atk-id").value = State.playerId;
  document.getElementById("atk-hp").value = 800 + State.level * 100;
  document.getElementById("atk-atk").value = 150 + State.level * 15;
  document.getElementById("atk-def").value = 40 + State.level * 5;
  document.getElementById("def-id").value = player.id;
  document.getElementById("def-hp").value = player.hp;
  document.getElementById("def-atk").value = player.atk;
  document.getElementById("def-def").value = player.def;
  toast(`准备与 ${player.name}(Lv.${player.level}) 进行PK！`, "warning");
}

function startPKWithFriend(fId, fName, fLevel) {
  openBattleModal();
  document.getElementById("atk-id").value = State.playerId;
  document.getElementById("atk-hp").value = 800 + State.level * 100;
  document.getElementById("atk-atk").value = 150 + State.level * 15;
  document.getElementById("atk-def").value = 40 + State.level * 5;
  document.getElementById("def-id").value = fId;
  document.getElementById("def-hp").value = 800 + fLevel * 100;
  document.getElementById("def-atk").value = 150 + fLevel * 15;
  document.getElementById("def-def").value = 40 + fLevel * 5;
  toast(`准备与好友 ${fName}(Lv.${fLevel}) 进行PK！`, "warning");
}

// ═══════════ 发起战斗 ═══════════

async function startBattle() {
  const btn = document.getElementById("btn-start-battle");
  btn.disabled = true;
  btn.textContent = "⏳ 战斗中...";

  const payload = {
    seed: parseInt(document.getElementById("battle-seed").value) || 0,
    maxRounds:
      parseInt(document.getElementById("battle-max-rounds").value) || 100,
    attacker: {
      id: parseInt(document.getElementById("atk-id").value) || 101,
      type: document.getElementById("atk-type").value,
      hp: parseInt(document.getElementById("atk-hp").value) || 1500,
      atk: parseInt(document.getElementById("atk-atk").value) || 220,
      def: parseInt(document.getElementById("atk-def").value) || 60,
      skillIds: [...selectedSkills.atk],
    },
    defender: {
      id: parseInt(document.getElementById("def-id").value) || 201,
      type: document.getElementById("def-type").value,
      hp: parseInt(document.getElementById("def-hp").value) || 1600,
      atk: parseInt(document.getElementById("def-atk").value) || 200,
      def: parseInt(document.getElementById("def-def").value) || 70,
      skillIds: [...selectedSkills.def],
    },
  };

  try {
    const resp = await fetch(`${API_BATTLE}/api/battle/simulate`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(payload),
    });
    if (!resp.ok) throw new Error(`HTTP ${resp.status}`);
    battleResult = await resp.json();

    document.getElementById("battle-setup").style.display = "none";
    document.getElementById("battle-arena").classList.add("active");
    renderBattleResult();
    playBattleAnimation();
  } catch (e) {
    toast(
      "战斗请求失败: " + e.message + " (请确保游戏服务器已启动)",
      "error",
      5000,
    );
    console.error("Battle error:", e);
  } finally {
    btn.disabled = false;
    btn.textContent = "⚔️ 开 始 战 斗";
  }
}

// ═══════════ 战斗结果 ═══════════

function renderBattleResult() {
  if (!battleResult) return;

  const resultBox = document.getElementById("battle-result-box");
  const win = battleResult.win;
  resultBox.innerHTML = `<div class="battle-result ${win ? "win" : "lose"}">
  ${win ? "🎉 进攻方胜利！" : "💀 防守方胜利！"} · ${battleResult.rounds} 回合
  <div style="font-size:12px;font-weight:400;margin-top:4px;opacity:0.8">
    进攻方剩余HP: ${battleResult.attackerHp} | 防守方剩余HP: ${battleResult.defenderHp}
  </div>
</div>`;

  const logBox = document.getElementById("battle-log");
  const logs = battleResult.logs || [];
  logBox.innerHTML = logs
    .map((log) => {
      let cls = "";
      if (log.includes("回合")) cls = "round-header";
      if (log.includes("暴击")) cls = "crit";
      if (log.includes("恢复") || log.includes("治疗")) cls = "heal-log";
      if (log.includes("技能") || log.includes("Buff")) cls = "skill-log";
      return `<div class="${cls}">${log}</div>`;
    })
    .join("");
  logBox.scrollTop = 0;

  if (win) {
    const woodReward = 20 + Math.floor(Math.random() * 30);
    const coalReward = 10 + Math.floor(Math.random() * 20);
    State.wood += woodReward;
    State.coal += coalReward;
    updateUI();
    addSystemMsg(`PK胜利！获得 ${woodReward} 木材, ${coalReward} 煤炭`);
    toast(`胜利！获得 ${woodReward} 木材, ${coalReward} 煤炭`, "success");
  } else {
    addSystemMsg("PK失败...");
    toast("战斗失败...", "error");
  }
}

// ═══════════ 战斗动画 ═══════════

function playBattleAnimation() {
  if (!battleResult || !battleResult.actions) return;
  const canvas = document.getElementById("battleCanvas");
  const wrap = canvas.parentElement;
  canvas.width = wrap.clientWidth;
  canvas.height = wrap.clientHeight;
  const ctx = canvas.getContext("2d");

  const atkMaxHp = parseInt(document.getElementById("atk-hp").value) || 1500;
  const defMaxHp = parseInt(document.getElementById("def-hp").value) || 1600;
  let atkHp = atkMaxHp;
  let defHp = defMaxHp;
  const atkId = parseInt(document.getElementById("atk-id").value) || 101;
  const atkType = document.getElementById("atk-type").value;
  const defType = document.getElementById("def-type").value;
  const typeEmoji = { INFANTRY: "🛡️", CAVALRY: "🐴", ARCHER: "🏹" };

  const actions = battleResult.actions || [];
  let actionIdx = 0;
  let effects = [];
  let shakeTimer = 0;

  function drawFrame() {
    const w = canvas.width,
      h = canvas.height;
    ctx.clearRect(0, 0, w, h);

    const bg = ctx.createLinearGradient(0, 0, w, h);
    bg.addColorStop(0, "#0a0d12");
    bg.addColorStop(1, "#131820");
    ctx.fillStyle = bg;
    ctx.fillRect(0, 0, w, h);

    ctx.fillStyle = "rgba(69,162,158,0.05)";
    ctx.fillRect(0, h * 0.7, w, h * 0.3);
    ctx.strokeStyle = "rgba(69,162,158,0.1)";
    ctx.lineWidth = 1;
    for (let i = 0; i < 20; i++) {
      ctx.beginPath();
      ctx.moveTo(i * (w / 20), h * 0.7);
      ctx.lineTo(i * (w / 20) - 30, h);
      ctx.stroke();
    }

    const shake = shakeTimer > 0 ? (Math.random() - 0.5) * 6 : 0;
    if (shakeTimer > 0) shakeTimer--;

    const ax = w * 0.25 + shake,
      ay = h * 0.55;
    drawUnit(
      ctx,
      ax,
      ay,
      atkHp,
      atkMaxHp,
      typeEmoji[atkType] || "⚔️",
      "进攻方",
      "#e94560",
      atkHp <= 0,
    );

    const dx = w * 0.75 + shake,
      dy = h * 0.55;
    drawUnit(
      ctx,
      dx,
      dy,
      defHp,
      defMaxHp,
      typeEmoji[defType] || "🛡️",
      "防守方",
      "#0ea5e9",
      defHp <= 0,
    );

    ctx.fillStyle = "rgba(245,197,24,0.3)";
    ctx.font = 'bold 28px "Segoe UI"';
    ctx.textAlign = "center";
    ctx.fillText("⚔️", w / 2, h * 0.45);

    for (let i = effects.length - 1; i >= 0; i--) {
      const eff = effects[i];
      eff.life--;
      if (eff.life <= 0) {
        effects.splice(i, 1);
        continue;
      }
      ctx.globalAlpha = eff.life / eff.maxLife;
      ctx.font = `bold ${eff.size}px "Segoe UI"`;
      ctx.textAlign = "center";
      ctx.fillStyle = eff.color;
      ctx.fillText(eff.text, eff.x, eff.y - (eff.maxLife - eff.life) * 1.5);
      ctx.globalAlpha = 1;
    }
  }

  function drawUnit(ctx, x, y, hp, maxHp, emoji, label, color, dead) {
    ctx.fillStyle = "rgba(0,0,0,0.3)";
    ctx.beginPath();
    ctx.ellipse(x, y + 40, 30, 8, 0, 0, Math.PI * 2);
    ctx.fill();

    if (!dead) {
      ctx.font = '48px "Segoe UI"';
      ctx.textAlign = "center";
      ctx.fillText(emoji, x, y + 15);
    } else {
      ctx.globalAlpha = 0.3;
      ctx.font = '48px "Segoe UI"';
      ctx.textAlign = "center";
      ctx.fillText("💀", x, y + 15);
      ctx.globalAlpha = 1;
    }

    ctx.fillStyle = color;
    ctx.font = 'bold 13px "Segoe UI"';
    ctx.fillText(label, x, y - 50);

    const barW = 80,
      barH = 8;
    const barX = x - barW / 2,
      barY = y - 40;
    ctx.fillStyle = "#1a1a2e";
    ctx.fillRect(barX, barY, barW, barH);
    const ratio = Math.max(0, hp / maxHp);
    const hpColor =
      ratio > 0.5 ? "#22c55e" : ratio > 0.2 ? "#f59e0b" : "#e94560";
    ctx.fillStyle = hpColor;
    ctx.fillRect(barX, barY, barW * ratio, barH);
    ctx.strokeStyle = "#333";
    ctx.lineWidth = 1;
    ctx.strokeRect(barX, barY, barW, barH);

    ctx.fillStyle = "#fff";
    ctx.font = '10px "Segoe UI"';
    ctx.fillText(`${Math.max(0, hp)} / ${maxHp}`, x, barY - 4);
  }

  function nextAction() {
    if (actionIdx >= actions.length) {
      drawFrame();
      return;
    }
    const a = actions[actionIdx];
    actionIdx++;

    const isAtkActing = a.actorId === atkId;
    if (isAtkActing) {
      defHp -= a.damage;
      shakeTimer = 8;
      effects.push({
        x: canvas.width * 0.75,
        y: canvas.height * 0.35,
        text: (a.isCrit ? "💥暴击 " : "") + `-${a.damage}`,
        color: a.isCrit ? "#f5c518" : "#e94560",
        size: a.isCrit ? 20 : 16,
        life: 40,
        maxLife: 40,
      });
    } else {
      atkHp -= a.damage;
      shakeTimer = 8;
      effects.push({
        x: canvas.width * 0.25,
        y: canvas.height * 0.35,
        text: (a.isCrit ? "💥暴击 " : "") + `-${a.damage}`,
        color: a.isCrit ? "#f5c518" : "#0ea5e9",
        size: a.isCrit ? 20 : 16,
        life: 40,
        maxLife: 40,
      });
    }

    if (a.skillId) {
      const skill = SKILL_CONFIG.find((s) => s.id === a.skillId);
      if (skill) {
        effects.push({
          x: canvas.width / 2,
          y: canvas.height * 0.15,
          text: `✨ ${skill.name}`,
          color: "#a855f7",
          size: 14,
          life: 50,
          maxLife: 50,
        });
      }
    }

    const logBox = document.getElementById("battle-log");
    const logItems = logBox.children;
    if (actionIdx - 1 < logItems.length) {
      logItems[actionIdx - 1].style.background = "rgba(102,252,241,0.1)";
      logItems[actionIdx - 1].scrollIntoView({
        behavior: "smooth",
        block: "nearest",
      });
    }

    drawFrame();
    battleAnimTimer = setTimeout(nextAction, 400 / battleSpeed);
  }

  drawFrame();
  battleAnimTimer = setTimeout(nextAction, 800 / battleSpeed);
}

function replayBattle() {
  if (battleAnimTimer) {
    clearTimeout(battleAnimTimer);
    battleAnimTimer = null;
  }
  playBattleAnimation();
}

function setSpeed(speed, btn) {
  battleSpeed = speed;
  document
    .querySelectorAll(".speed-btn")
    .forEach((b) => b.classList.remove("active"));
  btn.classList.add("active");
}
