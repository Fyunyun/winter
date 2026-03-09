/**
 * network.js — 服务器通信（轮询、心跳、位置同步）
 */

let chatSinceTimestamp = 0;
let pollingTimers = [];

function startPolling() {
  pollingTimers.push(setInterval(pollWorldChat, 1000));
  pollingTimers.push(setInterval(pollNearbyPlayers, 2000));
  pollingTimers.push(setInterval(sendHeartbeat, 10000));
  pollWorldChat();
  pollNearbyPlayers();
}

async function sendHeartbeat() {
  try {
    await fetch(`${API_BATTLE}/api/heartbeat`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ playerId: State.playerId }),
    });
  } catch (e) {
    /* 忽略 */
  }
}

async function pollWorldChat() {
  try {
    const resp = await fetch(
      `${API_BATTLE}/api/chat/world?since=${chatSinceTimestamp}`,
    );
    const data = await resp.json();
    if (data.messages && data.messages.length > 0) {
      for (const msg of data.messages) {
        State.chatMessages.world.push({
          sender: msg.sender,
          text: msg.content,
          time: formatTs(msg.timestamp),
          system: msg.playerId === 0,
        });
        chatSinceTimestamp = Math.max(chatSinceTimestamp, msg.timestamp);
      }
      if (State.currentChatTab === "world") renderChatMessages();
    }
  } catch (e) {
    /* 忽略 */
  }
}

async function pollNearbyPlayers() {
  try {
    const resp = await fetch(
      `${API_BATTLE}/api/players/nearby?playerId=${State.playerId}`,
    );
    const data = await resp.json();
    State.nearbyPlayers = (data.players || []).map((p) => ({
      id: p.playerId,
      name: p.name,
      level: p.level || 1,
      x: p.x,
      y: p.y,
      hp: 800 + (p.level || 1) * 100,
      atk: 150 + (p.level || 1) * 15,
      def: 40 + (p.level || 1) * 5,
    }));
    renderNearbyList();
    document.getElementById("nearby-count").textContent =
      State.nearbyPlayers.length;
  } catch (e) {
    /* 忽略 */
  }
}

async function syncPosition() {
  try {
    await fetch(`${API_BATTLE}/api/move`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({
        playerId: State.playerId,
        x: State.x,
        y: State.y,
      }),
    });
  } catch (e) {
    /* 忽略 */
  }
}
