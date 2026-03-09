/**
 * players.js — 附近玩家列表
 */

function generateNearbyPlayers() {
  pollNearbyPlayers();
}

function renderNearbyList() {
  const container = document.getElementById("nearby-list");
  if (State.nearbyPlayers.length === 0) {
    container.innerHTML =
      '<div style="color:#555;font-size:11px;text-align:center;padding:20px">附近没有玩家</div>';
    return;
  }
  container.innerHTML = State.nearbyPlayers
    .map(
      (p) => `
  <div class="nearby-item">
    <span class="nname">${p.name}</span>
    <span class="npos">Lv.${p.level}</span>
    <button class="btn-pk" onclick="startPKWith(getNearby(${p.id}))">⚔️ PK</button>
  </div>`,
    )
    .join("");
}

function getNearby(id) {
  return State.nearbyPlayers.find((p) => p.id === id);
}
