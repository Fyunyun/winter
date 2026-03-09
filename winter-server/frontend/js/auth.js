/**
 * auth.js — 登录/注册/进入游戏
 */

async function doLogin() {
  const user = document.getElementById("login-user").value.trim();
  const pass = document.getElementById("login-pass").value.trim();
  const msgEl = document.getElementById("login-msg");
  if (!user || !pass) {
    msgEl.className = "login-msg err";
    msgEl.textContent = "请输入用户名和密码";
    return;
  }

  msgEl.className = "login-msg ok";
  msgEl.textContent = "正在连接服务器...";
  try {
    const resp = await fetch(`${API_BATTLE}/api/login`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ username: user, password: pass }),
    });
    const data = await resp.json();
    if (data.code !== 0) {
      msgEl.className = "login-msg err";
      msgEl.textContent = data.msg || "登录失败";
      return;
    }
    State.playerId = data.playerId;
    State.playerName = data.name || user;
    State.loggedIn = true;
    State.level = data.level || 1;
    State.x = data.x || 400;
    State.y = data.y || 300;
    enterGame();
  } catch (e) {
    msgEl.className = "login-msg err";
    msgEl.textContent = "连接服务器失败，请确保游戏服务器已启动(端口18088)";
  }
}

async function doRegister() {
  await doLogin();
}

function enterGame() {
  document.getElementById("login-screen").classList.add("hidden");
  document.getElementById("game-screen").classList.add("active");
  updateUI();
  initGameCanvas();
  initSkillChips();
  addSystemMsg(`欢迎回来，${State.playerName}！你的ID是 ${State.playerId}`);
  addSystemMsg("在地图上点击鼠标移动角色，靠近其他玩家可以发起PK！");
  renderChatMessages();
  startPolling();
  toast(`登录成功！欢迎 ${State.playerName}`, "success");
}
