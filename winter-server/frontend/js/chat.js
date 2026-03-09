/**
 * chat.js — 聊天系统
 */

function switchChatTab(tab, el) {
  document
    .querySelectorAll(".chat-tab")
    .forEach((t) => t.classList.remove("active"));
  el.classList.add("active");
  State.currentChatTab = tab;
  renderChatMessages();
}

function renderChatMessages() {
  const container = document.getElementById("chat-messages");
  const msgs = State.chatMessages[State.currentChatTab] || [];
  if (msgs.length === 0) {
    container.innerHTML =
      '<div style="color:#555;font-size:11px;text-align:center;padding:20px">暂无消息</div>';
    return;
  }
  container.innerHTML = msgs
    .map((m) => {
      if (m.system) return `<div class="chat-msg system">📢 ${m.text}</div>`;
      const cls = m.isPrivate ? "private" : "";
      const prefix = m.isPrivate ? "🔒 " : "";
      return `<div class="chat-msg ${cls}"><span class="sender">${prefix}${m.sender}</span>: ${m.text}<span class="ts">${m.time}</span></div>`;
    })
    .join("");
  container.scrollTop = container.scrollHeight;
}

function addSystemMsg(text) {
  State.chatMessages.system.push({ system: true, text, time: now() });
  if (State.currentChatTab === "system") renderChatMessages();
}

async function sendChat() {
  const input = document.getElementById("chat-input");
  const text = input.value.trim();
  if (!text) return;
  input.value = "";

  const type = document.getElementById("chat-target-type").value;
  const time = now();

  if (type === "private") {
    const targetId = parseInt(document.getElementById("chat-target-id").value);
    if (!targetId) {
      toast("请输入私聊对象ID", "error");
      return;
    }
    const msg = { sender: State.playerName, text, time, isPrivate: true };
    State.chatMessages.private.push(msg);
    if (State.currentChatTab === "private") renderChatMessages();
  } else {
    try {
      await fetch(`${API_BATTLE}/api/chat/world`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ playerId: State.playerId, content: text }),
      });
    } catch (e) {
      toast("发送失败，服务器连接异常", "error");
    }
  }
}
