/**
 * main.js — 事件绑定与初始化
 */

// 聊天目标切换时显示/隐藏私聊ID输入框
document
  .getElementById("chat-target-type")
  .addEventListener("change", function () {
    document.getElementById("chat-target-id").style.display =
      this.value === "private" ? "block" : "none";
  });

// 快捷键
document.addEventListener("keydown", (e) => {
  if (e.key === "Escape") {
    closeBattleModal();
  }
  if (
    e.key === "Enter" &&
    document.activeElement === document.getElementById("chat-input")
  ) {
    sendChat();
  }
  if (
    e.key === "Enter" &&
    (document.activeElement === document.getElementById("login-user") ||
      document.activeElement === document.getElementById("login-pass"))
  ) {
    doLogin();
  }
});

// 登录页自动聚焦
document.getElementById("login-user").focus();
