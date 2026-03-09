/**
 * friends.js — 好友系统
 */

function addFriend() {
  const idInput = document.getElementById("add-friend-id");
  const targetId = parseInt(idInput.value);
  if (!targetId || targetId === State.playerId) {
    toast("无效的玩家ID", "error");
    return;
  }

  if (State.friends.find((f) => f.id === targetId)) {
    toast("已经是好友了", "warning");
    return;
  }

  toast(`已向玩家 ${targetId} 发送好友申请`, "success");
  addSystemMsg(`向玩家 ${targetId} 发送了好友申请`);
  idInput.value = "";

  // 模拟对方自动同意
  setTimeout(() => {
    const name = fakePlayerNames[targetId % fakePlayerNames.length];
    State.friends.push({
      id: targetId,
      name,
      level: 1 + (targetId % 15),
      online: Math.random() > 0.3,
      status: 1,
    });
    renderFriendList();
    toast(`${name} 同意了好友申请！`, "success");
    addSystemMsg(`${name} 已成为你的好友`);
  }, 1500);
}

function renderFriendList() {
  const container = document.getElementById("friend-list");
  if (State.friends.length === 0) {
    container.innerHTML =
      '<div style="color:#555;font-size:11px;text-align:center;padding:20px">暂无好友</div>';
    return;
  }
  container.innerHTML = State.friends
    .map(
      (f) => `
  <div class="friend-item">
    <div class="status-dot ${f.online ? "online" : "offline"}"></div>
    <span class="fname">${f.name}</span>
    <span class="flevel">Lv.${f.level}</span>
    <div class="friend-actions">
      <button class="btn-chat-friend" onclick="startPrivateChat(${f.id},'${f.name.replace(/'/g, "\\'")}')">💬</button>
      <button class="btn-attack-friend" onclick="startPKWithFriend(${f.id},'${f.name.replace(/'/g, "\\'")}',${f.level})">⚔️</button>
    </div>
  </div>`,
    )
    .join("");
}

function startPrivateChat(targetId, targetName) {
  document.getElementById("chat-target-type").value = "private";
  document.getElementById("chat-target-id").style.display = "block";
  document.getElementById("chat-target-id").value = targetId;
  switchChatTab("private", document.querySelectorAll(".chat-tab")[1]);
  document.getElementById("chat-input").focus();
  toast(`开始与 ${targetName} 的私聊`, "info");
}
