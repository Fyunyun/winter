/**
 * map.js — 游戏地图Canvas渲染与玩家移动
 */

let canvasCtx, canvasEl;
let mapWidth = 1000,
  mapHeight = 800;
let cameraX = 0,
  cameraY = 0;
let targetX = null,
  targetY = null;
let animFrame;
let moveParticles = [];

function initGameCanvas() {
  canvasEl = document.getElementById("gameCanvas");
  const rect = canvasEl.parentElement.getBoundingClientRect();
  canvasEl.width = rect.width;
  canvasEl.height = rect.height;
  canvasCtx = canvasEl.getContext("2d");

  cameraX = State.x - canvasEl.width / 2;
  cameraY = State.y - canvasEl.height / 2;

  canvasEl.addEventListener("click", onCanvasClick);
  window.addEventListener("resize", () => {
    const r = canvasEl.parentElement.getBoundingClientRect();
    canvasEl.width = r.width;
    canvasEl.height = r.height;
  });

  gameLoop();
}

function onCanvasClick(e) {
  const rect = canvasEl.getBoundingClientRect();
  const cx = e.clientX - rect.left;
  const cy = e.clientY - rect.top;
  const worldX = cx + cameraX;
  const worldY = cy + cameraY;

  // 检查是否点击了附近玩家
  for (const p of State.nearbyPlayers) {
    const dx = worldX - p.x;
    const dy = worldY - p.y;
    if (Math.sqrt(dx * dx + dy * dy) < 20) {
      startPKWith(p);
      return;
    }
  }

  // 否则移动
  targetX = Math.max(0, Math.min(mapWidth, worldX));
  targetY = Math.max(0, Math.min(mapHeight, worldY));

  // 点击特效
  const ring = document.createElement("div");
  ring.className = "click-ring";
  ring.style.left = e.clientX - rect.left + "px";
  ring.style.top = e.clientY - rect.top + "px";
  canvasEl.parentElement.appendChild(ring);
  setTimeout(() => ring.remove(), 600);
}

function gameLoop() {
  updatePlayer();
  drawMap();
  animFrame = requestAnimationFrame(gameLoop);
}

function updatePlayer() {
  if (targetX === null) return;
  const dx = targetX - State.x;
  const dy = targetY - State.y;
  const dist = Math.sqrt(dx * dx + dy * dy);
  if (dist < 2) {
    State.x = targetX;
    State.y = targetY;
    targetX = null;
    targetY = null;
    document.getElementById("coords-display").textContent =
      `坐标: (${Math.floor(State.x)}, ${Math.floor(State.y)})`;
    syncPosition();
    return;
  }
  const speed = 3;
  const vx = (dx / dist) * speed;
  const vy = (dy / dist) * speed;
  State.x += vx;
  State.y += vy;

  // 移动粒子
  if (Math.random() > 0.6) {
    moveParticles.push({
      x: State.x,
      y: State.y,
      life: 20,
      color: `hsl(${170 + Math.random() * 30}, 80%, 60%)`,
    });
  }

  cameraX = State.x - canvasEl.width / 2;
  cameraY = State.y - canvasEl.height / 2;
  document.getElementById("coords-display").textContent =
    `坐标: (${Math.floor(State.x)}, ${Math.floor(State.y)})`;
}

function drawMap() {
  const c = canvasCtx;
  const w = canvasEl.width,
    h = canvasEl.height;
  c.clearRect(0, 0, w, h);

  // 背景渐变
  const grad = c.createRadialGradient(w / 2, h / 2, 100, w / 2, h / 2, w);
  grad.addColorStop(0, "#0f1923");
  grad.addColorStop(1, "#070a0f");
  c.fillStyle = grad;
  c.fillRect(0, 0, w, h);

  // 网格
  c.strokeStyle = "rgba(69,162,158,0.08)";
  c.lineWidth = 1;
  const gridSize = 60;
  const offX = -(cameraX % gridSize);
  const offY = -(cameraY % gridSize);
  for (let x = offX; x < w; x += gridSize) {
    c.beginPath();
    c.moveTo(x, 0);
    c.lineTo(x, h);
    c.stroke();
  }
  for (let y = offY; y < h; y += gridSize) {
    c.beginPath();
    c.moveTo(0, y);
    c.lineTo(w, y);
    c.stroke();
  }

  // 地图装饰元素
  drawMapObjects(c);

  // 移动粒子
  for (let i = moveParticles.length - 1; i >= 0; i--) {
    const p = moveParticles[i];
    p.life--;
    if (p.life <= 0) {
      moveParticles.splice(i, 1);
      continue;
    }
    const sx = p.x - cameraX,
      sy = p.y - cameraY;
    c.globalAlpha = p.life / 20;
    c.fillStyle = p.color;
    c.beginPath();
    c.arc(sx, sy, 2, 0, Math.PI * 2);
    c.fill();
    c.globalAlpha = 1;
  }

  // 目标指示器
  if (targetX !== null) {
    const tx = targetX - cameraX,
      ty = targetY - cameraY;
    c.strokeStyle = "rgba(102,252,241,0.5)";
    c.lineWidth = 1.5;
    c.setLineDash([4, 4]);
    c.beginPath();
    c.arc(tx, ty, 12, 0, Math.PI * 2);
    c.stroke();
    c.setLineDash([]);

    c.strokeStyle = "rgba(102,252,241,0.15)";
    c.beginPath();
    c.setLineDash([3, 6]);
    c.moveTo(State.x - cameraX, State.y - cameraY);
    c.lineTo(tx, ty);
    c.stroke();
    c.setLineDash([]);
  }

  // 附近玩家
  for (const p of State.nearbyPlayers) {
    const px = p.x - cameraX,
      py = p.y - cameraY;
    if (px < -40 || px > w + 40 || py < -40 || py > h + 40) continue;

    c.beginPath();
    c.arc(px, py, 16, 0, Math.PI * 2);
    c.fillStyle = "rgba(233,69,96,0.15)";
    c.fill();
    c.strokeStyle = "#e94560";
    c.lineWidth = 1.5;
    c.stroke();

    c.fillStyle = "#e94560";
    c.beginPath();
    c.arc(px, py, 8, 0, Math.PI * 2);
    c.fill();

    c.fillStyle = "#ff8a9b";
    c.font = '10px "Segoe UI"';
    c.textAlign = "center";
    c.fillText(p.name, px, py - 22);
    c.fillStyle = "#888";
    c.font = '9px "Segoe UI"';
    c.fillText(`Lv.${p.level}`, px, py - 12);

    c.fillStyle = "#e94560";
    c.font = '12px "Segoe UI"';
    c.fillText("⚔️", px + 16, py - 8);
  }

  // 我方玩家
  const mx = State.x - cameraX,
    my = State.y - cameraY;

  const pulse = Math.sin(Date.now() / 500) * 4 + 18;
  c.beginPath();
  c.arc(mx, my, pulse, 0, Math.PI * 2);
  c.fillStyle = "rgba(102,252,241,0.08)";
  c.fill();

  c.beginPath();
  c.arc(mx, my, 14, 0, Math.PI * 2);
  c.fillStyle = "rgba(102,252,241,0.2)";
  c.fill();
  c.strokeStyle = "#66fcf1";
  c.lineWidth = 2;
  c.stroke();

  c.beginPath();
  c.arc(mx, my, 8, 0, Math.PI * 2);
  c.fillStyle = "#66fcf1";
  c.fill();

  c.beginPath();
  c.arc(mx - 2, my - 2, 3, 0, Math.PI * 2);
  c.fillStyle = "rgba(255,255,255,0.6)";
  c.fill();

  c.fillStyle = "#66fcf1";
  c.font = 'bold 11px "Segoe UI"';
  c.textAlign = "center";
  c.fillText(State.playerName, mx, my - 22);
  c.fillStyle = "#aaa";
  c.font = '9px "Segoe UI"';
  c.fillText(`Lv.${State.level}`, mx, my - 12);
}

// 地图装饰物 — 随机但固定位置
const mapObjects = [];
(function genMapObjects() {
  const rng = (s) => {
    s = Math.sin(s) * 43758.5453;
    return s - Math.floor(s);
  };
  for (let i = 0; i < 80; i++) {
    mapObjects.push({
      x: rng(i * 1.1 + 0.3) * mapWidth,
      y: rng(i * 1.7 + 0.9) * mapHeight,
      type: i % 5 === 0 ? "rock" : "tree",
      size: 4 + rng(i * 2.3) * 8,
    });
  }
})();

function drawMapObjects(c) {
  for (const obj of mapObjects) {
    const sx = obj.x - cameraX,
      sy = obj.y - cameraY;
    if (
      sx < -30 ||
      sx > canvasEl.width + 30 ||
      sy < -30 ||
      sy > canvasEl.height + 30
    )
      continue;
    if (obj.type === "tree") {
      c.fillStyle = "rgba(34,100,60,0.4)";
      c.beginPath();
      c.arc(sx, sy - obj.size, obj.size, 0, Math.PI * 2);
      c.fill();
      c.fillStyle = "rgba(90,60,30,0.5)";
      c.fillRect(sx - 1.5, sy - obj.size * 0.5, 3, obj.size);
    } else {
      c.fillStyle = "rgba(100,100,100,0.3)";
      c.beginPath();
      c.ellipse(sx, sy, obj.size * 1.2, obj.size * 0.8, 0, 0, Math.PI * 2);
      c.fill();
    }
  }
}
