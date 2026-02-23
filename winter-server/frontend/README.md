# Battle Frontend Tester

## 作用
- `battle-test.html` 是一个前端联调页，直接调用后端真实 `BattleEngine`。
- 支持技能效果：`MOD_DAMAGE`、`VAMPIRE`、`HEAL`、`ADD_BUFF`、`REDUCE_CD`。

## 使用
1. 启动后端测试服务：运行 `com.winter.modules.battle.core.BattleHttpTestServer`（默认端口 `18088`）。
2. 用浏览器打开 `battle-test.html`。
3. 确认接口地址为 `http://localhost:18088/api/battle/simulate`。
4. 调整双方属性和技能参数，点击“运行战斗”。

## 后端示例
- Java 侧可运行 `com.winter.modules.battle.core.BattleDemo`，用于验证后端技能链和战报输出。

## 接口说明
- `POST /api/battle/simulate`
- 请求体字段：
	- `seed`、`maxRounds`
	- `attacker` / `defender`：`id`、`type`、`hp`、`atk`、`def`
	- 技能字段：`skillModPercent`、`skillVampirePercent`、`skillHeal`、`skillBuffTypeId`、`skillBuffValue`、`skillReduceCd`
