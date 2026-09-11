# 金钱系统

## 双钱包

- 大厅账户：永久保存，用于比赛外商店和后续武器背包解锁。
- 比赛资金：每场比赛开始时重置，用于购买枪械、配件、弹药和道具。
- 比赛结束后可以通过 matchEndPayoutFormula 把部分比赛资金转入大厅账户。

钱包保存在 config/fpsmod/economy_wallets.json，规则保存在 config/fpsmod/economy.json。

## GD656 兼容

GD656 未安装时，系统使用本 MOD 的击杀、死亡、伤害、回合和比赛事件结算。

GD656 安装后，通过其公开的 PlayerDataManager.getScore(UUID) 读取累计得分，
每秒保存一次快照。正 score_delta 会进入 gd656_score_formula 换算为比赛资金。
整个适配使用反射，不添加 GD656 编译期依赖，也不会篡改 GD656 自己的分数。

## 可用变量

- initial_match_balance：配置的初始比赛资金。
- match_balance：玩家当前比赛资金。
- global_balance：玩家当前大厅账户。
- loss_streak：连续失利回合数。
- damage：本次有效伤害值。
- score_delta：两次 GD656 快照之间的新增得分。
- count：购买数量。

## 公式语法

支持小数、括号、+ - * / %，以及 min、max、floor、ceil、abs。

示例：

- 300
- 1400 + min(loss_streak, 4) * 500
- 100 * count
- max(50, damage * 0.25)
- score_delta * 10

## 默认可配置项

- matchStartFormula：比赛开始资金。
- killFormula：击杀奖励。
- assistFormula：助攻奖励，供后续事件适配。
- deathFormula：阵亡补偿或扣款。
- damageFormula：按伤害获得资金。
- roundWinFormula / roundLossFormula / roundDrawFormula：回合结算。
- matchWinFormula：整场胜利奖励。
- matchEndPayoutFormula：比赛结束转入大厅账户的数量。
- gd656ScoreFormula：GD656 得分差值换算。
- categoryPriceFormulas：按武器分类设置价格公式。
- itemPriceOverrides：指定仓库条目的固定价格。

## 命令

- /fps money balance
- /fps money balance <玩家>
- /fps money add <玩家> <数量>
- /fps money set <玩家> <数量>
- /fps economy reload
- /fps shop list
- /fps shop addheld
- /fps shop price <仓库条目> <价格>
- /fps shop list
