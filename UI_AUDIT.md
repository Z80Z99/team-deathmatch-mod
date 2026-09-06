# 客户端 UI/UX 审计

## 范围与约束

本次审计覆盖 `src/main/java/cn/blockforge/generated/generatedmod/client` 及其 `screen` 子包、客户端同步缓存和可选 Mod 集成入口。服务器比赛、队伍、大厅、地图、网络和战斗逻辑不在本次 UI 改造范围内。

项目目标版本为 Minecraft 1.20.1 Forge。HP、Armor、Ammo、Weapon 由原版和 TACZ HUD 提供，本 Mod 没有对应的客户端同步字段，因此只优化本 Mod 自己拥有的数据，不伪造战斗数值。

## UI 清单

| 当前 UI | 所属模块 | 数据来源 | 显示条件 | 当前问题 | 优化方案 |
| --- | --- | --- | --- | --- | --- |
| `MatchHudOverlay` 记分板 | 战斗 HUD / Match | `ClientMatchData`、`ClientHudLayout` | 玩家处于非 `WAITING` 比赛状态 | 单色面板、比分和计时层级接近、底部提示可能靠近原版快捷栏、状态变化没有视觉反馈 | 使用统一面板、队伍色块、独立计时区、安全边距和短促状态过渡；保留百分比布局设置 |
| `MatchHudOverlay` 队伍/状态提示 | 战斗 HUD / Team / Respawn | `ClientMatchData`、`Team` | HUD 提示开启且玩家在比赛中 | 只有一行文本，队伍人数和重生状态识别速度有限 | 统一状态条、队伍色与文字双重提示、对重生和比赛结束状态使用高优先级样式 |
| 原版 Crosshair | 战斗 HUD | Minecraft 客户端 | 游戏内 | 本 Mod 不拥有该数据；记分板注册在 Crosshair 上方时需避免覆盖中心战斗视线 | 保持原版 Crosshair，不增加第二个准星；记分板固定在上方安全区域 |
| 原版/TACZ HP、Armor、Ammo、Weapon | 战斗 HUD | Minecraft、TACZ | 游戏内 | 本 Mod 没有同步数据，不能在本 Mod HUD 重复显示 | 保持第三方和原版职责，调整本 Mod 底部提示的安全边距 |
| GD656Killicon 击杀提示 | 第三方集成 / Kill Feed | `GD656Killicon` 自有系统 | 安装可选 Mod 时 | 未确认其私有接口；本 Mod 不能安全接管或复制 | 保持现有第三方 Kill Feed 功能，不新增第二套 Kill Feed；本 Mod 只显示比分变化 |
| `FpsTdmHubScreen` | 菜单 / 功能入口 | 无状态数据 | 通过按键、暂停菜单或父界面打开 | 面板和按钮使用各自颜色常量，信息分组弱，窗口高度较小时边界固定 | 使用统一主题、标题层级、分组说明和可键盘聚焦按钮 |
| `RoomScreen` | Lobby / 房间 | `ClientLobbyData`、`RoomView`、`LobbyConfigValues` | 房间大厅打开 | 房间列表是单行长文本，选中态和状态颜色弱，小窗口空间紧张，底部操作密集 | 使用列表行分区、状态标签、队伍/成员详情区和响应式行数；按钮使用动作优先级样式 |
| `MatchmakingScreen` | Lobby / 快速匹配 | `ClientLobbyData.matchmaking()` | 快速匹配打开 | 队列信息为三行居中文本，排队状态变化不突出，按钮状态反馈基础 | 使用队列状态面板、序位和等待时间分组，统一成功/错误状态条和轮询缓存 |
| `LobbyConfigScreen` | Lobby 管理配置 | `ClientLobbyData` | 管理员或只读用户打开 | 表单标签与控件空间关系固定，配置锁定和权限状态不够醒目 | 使用统一表单行、只读提示条、状态色和键盘保存反馈 |
| `FpsTdmConfigScreen` | Match / Server 配置 | `ClientConfigData`、`FpsTdmConfigValues` | 配置界面打开 | 页签、表单和状态提示视觉不统一，窄屏时表单密度较高 | 保留分页和服务器权威模型，统一页签、表单控件、状态栏和响应式面板 |
| `MapEditorScreen` | Map UI / 管理编辑 | `ClientMapEditorData`、`MapEditorView` | 地图编辑器打开 | 区域信息挤在两列文本中，编辑按钮没有分组，快照状态不够突出 | 增加地图摘要、区域分组、危险操作样式和快照状态条；保持所有请求与权限判断不变 |
| `MapCreateScreen` | Map UI / 创建地图 | `ClientMapEditorData`、服务器响应 | 从地图编辑器打开 | 创建流程只有单面板文本，等待、成功和失败状态层级较弱 | 使用步骤化摘要、输入框状态、不可重复提交反馈和统一结果条 |
| `HudLayoutScreen` | Settings / HUD 设置 | `ClientHudLayout` | HUD 设置打开 | 配置项与预览之间层级有限，默认预览固定，说明文字密集 | 保留百分比布局模型，统一表单、预览框和保存反馈；渲染使用安全边距 |
| 暂无独立 Team Selection | Team UI | 现有大厅/服务器队伍同步 | 当前未发现独立 Screen | 无单独队伍选择界面可优化 | 不新增客户端队伍决策逻辑；继续显示 `ClientMatchData.myTeam` 和队伍人数 |
| 暂无独立 Countdown/Toast 组件 | 通知 / 倒计时 | 现有同步字段和 Screen 消息 | 当前以文本状态显示 | 同类状态颜色和面板样式重复实现 | 由统一主题状态条承载；HUD 状态变化使用客户端短过渡，不新增网络协议 |
| 暂无独立 Debug/Spectator 菜单 | 辅助 UI | 未发现客户端入口 | 当前不存在 | 无实现可审计 | 不凭空新增功能，避免改变现有游戏流程 |

## 数据与兼容性结论

- 客户端 UI 只读取 `ClientMatchData`、`ClientLobbyData`、`ClientMapEditorData`、`ClientConfigData` 和 `ClientHudLayout`。
- `ClientPacketHandler` 只负责把既有同步包交给客户端缓存；本次不改变包结构和协议版本。
- `IntegrationManager` 继续只检测 `TACZ` 与 `GD656Killicon`。不修改第三方 Mod 核心，也不复制 GD656Killicon 的 Kill Feed。
- UI 动画只使用客户端缓存中的状态变化计时，不参与比赛胜负、队伍、击杀或重生决策。

## 统一 Design System 基线

- 背景：深炭灰半透明面板，使用细边框和单像素顶部强调线。
- 文字：主文本、次文本、成功、警告、错误、A 队、B 队和观战状态分离，颜色之外同时保留文字标签。
- 间距：面板内边距 10，表单列间距 10，按钮间距 5，交互控件高度至少 20。
- 控件：统一悬停、聚焦、禁用和危险操作样式；所有按钮保留原有鼠标回调并支持 Minecraft 默认 Tab/Enter 键盘路径。
- 布局：面板宽度随窗口收敛，列表和文本使用字体宽度裁剪，HUD 使用屏幕安全边距。
- 动画：状态变化最多约半秒的强调，不在每帧创建组件、纹理或复杂对象。

## 实施结果（历史轮次）

- Common UI：新增并统一使用 `UiTheme`、`UiScreen`、`UiButton`、`UiCycleButton`、`UiEditBox`；颜色、卡片、状态条、按钮动作层级、焦点态和禁用态已统一。
- HUD：比赛记分板与状态提示使用安全边距、队伍色、阶段层级和短促状态强调；`HudLayoutScreen` 支持实时比例预览、`Ctrl+S` 保存及完整默认值同步。
- 功能中心与配置：`FpsTdmHubScreen`、`FpsTdmConfigScreen`、`LobbyConfigScreen` 已使用响应式面板、统一页签/表单、权限锁定反馈和服务器确认状态。
- 房间与匹配：`RoomScreen`、`MatchmakingScreen` 已使用分区列表、选中态、队列统计、动作优先级和窄窗口适配。
- 地图：`MapEditorScreen` 已重排为地图摘要及“区域与快照 / 出生点”分页操作；`MapCreateScreen` 已增加步骤摘要、输入校验、等待/成功/失败反馈和防重复提交状态。
- 边界：未新增或修改服务器比赛、队伍、大厅、地图、战斗决策与网络包协议；所有 UI 继续只消费既有客户端同步数据。

## 本轮：重叠 / 错位根治（流式布局引擎）

上一轮虽然统一了视觉，但坐标仍是各屏幕手算的“固定带”，在小窗口下暴露出多处互相压叠。本轮逐屏审计出的根因：

1. 标题画在面板上沿之外（`panelTop - 20/-24`），副标题（14px 行距）会落到面板强调线或状态条上。
2. `section()` 自带下划线与相邻 `divider()` 重叠成双线（功能中心）。
3. 匹配页提示文字（progressTop+27）与状态条（actionTop-24）在常见高度下 y 区间相交。
4. 房间页列表按“最多 8 行”预留 50px，但详情卡实际需要 36px + 行尾，5 行时详情卡压住第一排动作按钮；状态条与详情卡也交叠。
5. 房间/大厅配置页窄窗口下 24px 高的行卡按 rowHeight 20 排布，卡片上下沿互叠 4px。
6. 大厅配置页状态条（panelTop+4）与 section 标题（panelTop+16）同行带互压。
7. 服务器配置页副标题与状态条同区间（7..29 vs 19..39）；底部三按钮画在面板矩形之外。
8. 房间页列表标题与右侧提示在小宽度下可能水平相撞。

### 结构保证（新 `UiScreen` 布局引擎）

- **四区带模型**：面板顶部固定标题区（36px）、内容视口、固定状态条（22px）、固定底部操作区，各区带 y 区间由 `beginLayout` 一次划死，互不相交；标题不再画到面板外。
- **流式横带**：所有行经 `flowRow(height)` 领取独占 y 区间，游标单调推进——任何两行（文字、输入框、按钮、卡片）在纵向上不可能重叠。
- **溢出整行隐藏 + 滚轮滚动**：内容总高超出现口时出现右侧滚动条，滚轮按 24px/格滚动；不完整可见的带连同其控件整体 `visible=false`，不存在“半截元素压在别处”的错位。
- **底部按钮等分列**：`footerButton(column, columns, row)` 由 `innerWidth` 测量等分，行高 20 + 4 间距，全部固定且落在面板内。
- **行内标签/控件精确切分**：`UiTheme.field()` 保证 labelX+labelWidth+gap+controlWidth 严格等于列宽；所有文字绘制前按所在单元格 `fit()` 裁剪，横向不可能相撞。
- **窄文本相撞守卫**：成对的左标题/右提示先量宽、放不下就只画标题（房间列表头）。
- **房间列表独立视口**：列表区固定在“创建区之下、状态条之上”，自带按行滚动与滚动条；窗口过小放不下至少一行时整区隐藏并在状态条提示，而不是叠到按钮上。选中详情卡同理“空间不足即让位”。
- **HUD**：击杀公告现在把自身底边传回给底部提示的安全边界，三者（记分板 / 击杀公告 / 状态提示）纵向串联不互相覆盖。

### 静态核对

- 对 427×240（最小窗口，缩放 1）与 142px 宽（极端 GUI 缩放）逐屏推演：所有区带、按钮列、输入列总宽 = 内容区宽，无越界、无叠带。
- 所有交互与网络行为（发包时机、按钮 enable 条件、服务器同步、Ctrl+S、页签切换）保持原逻辑，仅坐标来源改为布局引擎。

## 本轮：大厅/匹配/地图编辑功能解耦 + 规则与地图所有权模型

功能结构调整（布局仍走上面的流式引擎，不手算坐标）：

1. **房间规则（仅房主可改）**：新增 `RoomRules`（击杀目标、回合时长、胜利回合、热身、复活延迟、自动复活、友军伤害），
   随 `RoomView`/`RoomSyncPacket` 下发。`RoomScreen` 底部按钮改为 11 个声明式定义流入网格（新增按钮不再改布局代码），
   详情卡升级为三行（房主 / 规则摘要 / 成员）。新界面 `RoomRulesScreen`：房主可编辑、成员只读，
   保存走独立包 `RoomRulesPacket`，服务器端在 `RoomManager.saveRoomRules` 二次校验“只有房主、房间 OPEN、非匹配房”；
   开赛时 `MatchManager.applyRoomRules` 整体覆盖服务器默认，比赛 `resetToWaiting` 时恢复。
2. **地图编辑独立化**：地图编辑器对所有玩家开放，`MapOwnershipStore`（`config/fpsmod/maps/.index.json`）登记
   每张地图的拥有者 UUID 与邀请码；非拥有者只能编辑自己的地图（管理员可全量编辑作运维兜底），
   服务器旧图（无登记）只有管理员可编辑。新界面 `MapLibraryScreen`（地图工作台）负责
   新建（服务器自动生成 `玩家名-名称` 唯一 id）、删除、选择编辑目标、生成/撤销 6 位邀请码、按邀请码导入
   **独立副本**（导入后归导入者所有，之后编辑互不影响）；`MapEditorScreen` 只管当前目标的区域/出生点编辑。
   区域与出生点直接写回对应地图定义：目标恰为比赛当前地图时才同步运行时快照，否则只作废旧快照文件、
   载入比赛时重新捕获。所有写操作在“比赛进行中 / 房间开赛流程 / 快照任务进行”时统一锁定。
3. **协议**：`FpsTdmNetwork` 协议版本升到 7（房间视图新增规则与匹配房标记、地图编辑器同步包改为按玩家的
   专属视图、新增房主规则保存包）；`RoomSyncPacket` 消息长度放宽到 128 以容纳规则摘要。

## 本轮：游戏大厅独立化 + 快速匹配合并 + 匹配参数固定 + 匹配房中途加入

功能结构（布局仍走流式引擎，不手算坐标）：

1. **大厅成为独立界面种类**：新增 `LobbyScreen`（游戏大厅），暂停菜单新增独立“游戏大厅”按钮、
   新增快捷键 J（`ClientEvents.OPEN_LOBBY_KEY`，语言文件已补）；控制中心只保留一个
   “游戏大厅”入口，不再内嵌“快速匹配”“房间大厅”“匹配参数”入口。
2. **大厅只有三个按钮**：`LobbyScreen` 底部固定 加入房间 / 快速匹配（排队中变“取消匹配”、
   成局变“已匹配”）/ 返回；其余全是大厅内容本身（状态徽标、队列三统计、匹配进度条、
   进行中的匹配比赛摘要、固定规则说明、状态条）。
3. **快速匹配合并进大厅**：删除 `MatchmakingScreen`，排队/取消/实时序位全部在 `LobbyScreen` 内完成；
   原界面逻辑（统计卡、进度条、状态条）迁移到大厅内容区。
4. **匹配参数固定、要求准备删除**：`LobbyConfigValues` 移除 `requireReady` 与全部 `matchmaking*`
   字段（旧 `lobby.json` 中的多余键在读取时自然忽略），`LobbyConfigManager` 同步瘦身；
   `MatchmakingManager` 固定为 `MIN_PLAYERS_TO_FORM=6`、无人数上限（整个队列一起成局）、
   等待时长无限、成局后 `READY_SECONDS=10` 秒准备开赛。`LobbyConfigScreen` 只剩“房间参数”，
   `RoomAction.TOGGLE_READY`、`Room` 的准备集合、`RoomView`/`RoomSyncPacket` 的 `[已准备]`
   标记与 `ownReady` 字段全部删除，`canStart` 只看人数。
5. **匹配比赛 = 房间 + 中途加入 + 赛终自动解散**：`createMatchmakingRoom` 创建
   `maxPlayers = 0`（无上限，`Room.unlimitedCapacity()`）的匹配房并进入 10 秒倒计时；
   `RoomManager.join` 对匹配房放开到准备倒计时与比赛进行中（`RoomState.RUNNING` +
   `activeRoomId` 校验），中途加入走新增 `MatchManager.joinRoomMember` →
   `TeamManager.forceJoinDuringMatch`（平衡分队、优先选偏好队，失衡放不下时挂下一回合、先观战），
   传送本队出生点并广播；大厅“加入房间 → 房间列表”即可看到进行中匹配比赛并加入。
   `RoomManager.tick` 检测到匹配房比赛结束时不再重开，而是直接移除房间并公告“自动解散”。
   `MatchmakingStatus` 的 `enabled` 字段替换为 `readySeconds`（成局准备倒计时，
   `MatchmakingManager.readySecondsLeft()` 由房间 COUNTDOWN 剩余 tick 折算），
   `MatchmakingSyncPacket` 编解码与 `ClientLobbyData` 默认值同步更新。
6. **界面分工**：`RoomBrowserScreen`（加入房间：创建 + 列表 + 进入我的房间，创建/加入成功后自动跳
   `RoomScreen`）；`RoomScreen`（我的房间：信息卡 / 成员折行名单 / 地图输入 + 7 按钮网格，
   检测到被踢、解散或匹配房赛终自动退回上一级）；`ClientForgeEvents.isLobbyScreen`
   覆盖全部大厅系界面，比赛开始瞬间统一关闭并交还视角。
7. **协议**：版本 7 → 8（房间同步删除 ownReady、匹配状态 enabled→readySeconds、
   大厅配置字段收缩、RoomAction 枚举收缩）。

## 本轮：大厅/匹配彻底分离 + 底部按钮统一标准尺寸 + 房间列表去底部文本框 + HUD 模块化闭环

1. **大厅与匹配彻底分开**：`LobbyMenuScreen` 重写为纯入口菜单（只回答“往哪儿走”，不再排队/取消、
   不再显示队列详情卡）；`LobbyScreen` 语义为“房间大厅”（只做创建、浏览、加入房间）；
   `MatchmakingScreen` 是唯一匹配界面；`ClientForgeEvents.isLobbyScreen` 补上 `MatchmakingScreen`，
   开赛瞬间统一关闭并交还视角。已在房间时入口按钮自动变为“我的房间”直达 `RoomScreen`。
2. **所有 UI 底部按钮统一大小**：`UiScreen` 新增 `FOOTER_BUTTON_WIDTH=150` 标准宽，底部按钮
   一律 150×22、行内水平居中（窗口太窄放不下才退化为等分）；`RoomScreen` 底栏列数收敛到 ≤4、
   `RoomRulesScreen` 面板加宽到 640，全部界面（房间大厅 / 快速匹配 / 我的房间 / 规则 / 选图 /
   地图工作台 / 地图编辑 / 建图）同尺寸；HUD 配置窗内所有按钮、开关、输入框也统一为 22 高。
3. **房间列表只显示房间名 / 模式 / 人数，底部文本框删除**：`UiScreen.beginLayout` 支持关闭状态条
   区带；`LobbyScreen` 底部不再常驻任何文本框，列表区下探到按钮区之上；服务器反馈改为
   列表顶部约 4.5 秒的临时提示浮层（错误红 / 成功绿），操作提示移入按钮 tooltip。
4. **HUD 模块化闭环**：新增 `HudCustomRenderer`，文字 / 色块 / 进度条 / 图片四类独立模块在
   实战覆盖层与配置窗预览共用同一画法（此前配置窗看不到模块、色块不填充、图片不渲染）；
   `HudBackground` 升级为多图 LRU 缓存 + 任意矩形绘制（`drawImage`），图片模块缺图画虚线占位；
   配置窗新增“+图片”、显示/隐藏开关、文字内容编辑框（实时生效）、颜色循环（9 色调色板）、
   图片文件选择，支持在预览中直接点选与拖动模块（最上层先命中，松手落盘并刷新侧栏）；
   模块 id 生成改为查重，删后再加不会撞车。
5. **外部背景图**（延续落实）：`config/fpsmod/hud_images` 目录 PNG → 背景图循环选择 + “刷新图片”；
   “正在匹配 / 房间中”HUD 计时（延续落实）：`ClientLobbyData.dynamicWaitedTicks` 以本地 tick 平滑推进。
6. 交付版本 1.0.0-r54；本轮未改服务器逻辑与网络协议。
