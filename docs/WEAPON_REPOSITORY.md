# 武器仓库系统设计

## 分层

1. 目录层：从已安装武器 MOD 的创造模式分类动态读取枪械、配件、弹药和其他装备，不复制武器数据。
2. 仓库层：把选中的完整 ItemStack 快照持久化到服务器，默认只允许管理员修改。
3. 发放层：比赛规则按模式请求仓库条目，并通过统一接口写入玩家背包。
4. 表现层：大厅的“武器仓库”界面负责搜索、筛选、添加、移除和领取。

## 数据模型

- WeaponSnapshot：物品注册名、完整 NBT 的 Base64、数量。枪械 ID、弹药、已装配件、皮肤等全部保留。
- WeaponCategory：目录 ID、显示名、GUN / ATTACHMENT / AMMO / EQUIPMENT 类型。
- WeaponCatalogItem：目录物品唯一 ID、分类和快照。
- WeaponRepositoryItem：仓库条目 ID、分类、快照、自定义名称、启用状态。

配置保存在服务器目录的 config/fpsmod/weapon_repository.json。

## 兼容策略

本 MOD 不添加对 TACZ 的编译期依赖。服务器启动后检查 tacz 是否安装，再扫描命名空间为
tacz 的创造分类。未来兼容其他枪械 MOD 时，只需把目录扫描范围扩展到对应命名空间，
仓库、网络包、界面和发放接口都不需要重写。

## 后续系统接口

武器背包：每个玩家保存自己的仓库条目 ID 集合，复用一个或多个
WeaponSnapshot，不要复制完整 NBT 到玩家数据。

武器商店：商店商品只引用仓库条目 ID，并额外保存价格、解锁条件、库存；
购买结果调用 WeaponRepositoryManager.giveLoadout 发放。

比赛模式：房间规则可以新增“武器方案 ID”。热身后的正式复活流程通过
WeaponRepositoryManager.entriesForMode 解析该方案，再写入玩家背包。
