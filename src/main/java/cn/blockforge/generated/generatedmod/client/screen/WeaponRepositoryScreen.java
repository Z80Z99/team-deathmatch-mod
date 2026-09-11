package cn.blockforge.generated.generatedmod.client.screen;

import cn.blockforge.generated.generatedmod.client.ClientWeaponRepositoryData;
import cn.blockforge.generated.generatedmod.client.ui.UiButton;
import cn.blockforge.generated.generatedmod.client.ui.UiCycleButton;
import cn.blockforge.generated.generatedmod.client.ui.UiListScreen;
import cn.blockforge.generated.generatedmod.client.ui.UiTheme;
import cn.blockforge.generated.generatedmod.network.FpsTdmNetwork;
import cn.blockforge.generated.generatedmod.network.packet.WeaponRepositoryActionPacket;
import cn.blockforge.generated.generatedmod.weapon.WeaponCatalogItem;
import cn.blockforge.generated.generatedmod.weapon.WeaponCategory;
import cn.blockforge.generated.generatedmod.weapon.WeaponRepositoryAction;
import cn.blockforge.generated.generatedmod.weapon.WeaponRepositoryItem;
import cn.blockforge.generated.generatedmod.weapon.WeaponRepositoryView;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/** Arsenal editor: browse weapon-mod creative categories and maintain the shared repository. */
public final class WeaponRepositoryScreen extends UiListScreen<WeaponRepositoryScreen.Row> {
    private static final List<String> CATEGORY_OPTIONS = List.of(
            "", "gun_pistol", "gun_rifle", "gun_sniper", "gun_shotgun", "gun_smg",
            "gun_rpg", "gun_mg", "attachment_scope", "attachment_muzzle", "attachment_stock",
            "attachment_grip", "attachment_extended_mag", "attachment_laser", "ammo", "other");

    private final Screen parent;
    private UiCycleButton<Boolean> modeCycle;
    private UiCycleButton<String> categoryCycle;
    private UiButton addButton;
    private UiButton removeButton;
    private UiButton giveButton;
    private boolean showingRepository;
    private String selectedCategory = "";
    private int observedRevision = ClientWeaponRepositoryData.revision();
    private List<Row> cachedRows = List.of();
    private int rowsRevision = -1;
    private boolean rowsMode;
    private String rowsCategory = "~unset~";
    private int ticks;

    public WeaponRepositoryScreen(Screen parent) {
        super(Component.literal("武器仓库"));
        this.parent = parent;
    }

    public static void open(Screen parent) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player != null && minecraft.getConnection() != null) {
            minecraft.setScreen(new WeaponRepositoryScreen(parent));
            send(WeaponRepositoryAction.REQUEST, "", "");
        }
    }

    @Override
    protected void init() {
        beginLayout(760, 0, BUTTON_HEIGHT);
        int filterY = flowRow(BUTTON_HEIGHT);
        int gap = 6;
        modeCycle = flowWidget(new UiCycleButton<>(columnX(0, 2, gap), filterY,
                columnWidth(2, gap), BUTTON_HEIGHT, List.of(false, true), false,
                flag -> flag ? "仓库条目" : "武器目录",
                flag -> { showingRepository = flag; refreshEntries(); updateButtons(); },
                "切换浏览武器目录或已保存的仓库条目。", UiButton.Kind.SECONDARY), filterY);
        categoryCycle = flowWidget(new UiCycleButton<>(columnX(1, 2, gap), filterY,
                columnWidth(2, gap), BUTTON_HEIGHT, CATEGORY_OPTIONS, selectedCategory,
                WeaponRepositoryScreen::categoryLabel,
                value -> { selectedCategory = value; refreshEntries(); updateButtons(); },
                "按创造模式分类筛选。", UiButton.Kind.SECONDARY), filterY);
        addSearch("搜索名称、ID 或分类");
        addList();
        addButton = footerButton("加入仓库", 0, 4, 0, this::addSelected,
                "把当前目录物品保存为仓库条目。", UiButton.Kind.PRIMARY);
        removeButton = footerButton("移除条目", 1, 4, 0, this::removeSelected,
                "从仓库移除当前条目。", UiButton.Kind.DANGER);
        giveButton = footerButton("领取物品", 2, 4, 0, this::giveSelected,
                "把仓库物品发给自己。", UiButton.Kind.PRIMARY);
        footerButton("返回", 3, 4, 0, this::onClose, null, UiButton.Kind.SECONDARY);
        updateButtons();
    }

    private void addSelected() {
        Row row = selectedEntry();
        if (row != null && !showingRepository) send(WeaponRepositoryAction.ADD, row.id(), "");
    }

    private void removeSelected() {
        Row row = selectedEntry();
        if (row != null && showingRepository) send(WeaponRepositoryAction.REMOVE, "", row.id());
    }

    private void giveSelected() {
        Row row = selectedEntry();
        if (row != null && showingRepository) send(WeaponRepositoryAction.GIVE, "", row.id());
    }

    private static void send(WeaponRepositoryAction action, String catalogId, String entryId) {
        if (Minecraft.getInstance().getConnection() != null) {
            FpsTdmNetwork.sendToServer(new WeaponRepositoryActionPacket(action, catalogId, entryId));
        }
    }

    @Override
    protected List<Row> entries() {
        int revision = ClientWeaponRepositoryData.revision();
        if (revision == rowsRevision && showingRepository == rowsMode
                && selectedCategory.equals(rowsCategory)) {
            return cachedRows;
        }
        WeaponRepositoryView view = ClientWeaponRepositoryData.view();
        if (showingRepository) {
            cachedRows = view.repository().stream()
                    .filter(item -> selectedCategory.isBlank()
                            || selectedCategory.equals(item.categoryId()))
                    .map(Row::repository).toList();
        } else {
            cachedRows = view.catalog().stream()
                    .filter(item -> selectedCategory.isBlank()
                            || selectedCategory.equals(item.categoryId()))
                    .map(Row::catalog).toList();
        }
        rowsRevision = revision;
        rowsMode = showingRepository;
        rowsCategory = selectedCategory;
        return cachedRows;
    }

    @Override
    protected String entryId(Row entry) {
        return entry.id();
    }

    @Override
    protected String entryTitle(Row entry) {
        return entry.title();
    }

    @Override
    protected String entryDetail(Row entry) {
        return entry.detail();
    }

    @Override
    protected String entryBadge(Row entry) {
        return entry.badge();
    }

    @Override
    protected int entryColor(Row entry) {
        return entry.repository() ? UiTheme.INFO : UiTheme.ACCENT;
    }

    @Override
    public void tick() {
        super.tick();
        if (ClientWeaponRepositoryData.revision() != observedRevision) {
            observedRevision = ClientWeaponRepositoryData.revision();
            WeaponRepositoryView view = ClientWeaponRepositoryData.view();
            if (!CATEGORY_OPTIONS.contains(selectedCategory)) selectedCategory = "";
            if (categoryCycle != null) categoryCycle.setValue(selectedCategory);
            refreshEntries();
            updateButtons();
        }
        if (++ticks % 40 == 0) send(WeaponRepositoryAction.REQUEST, "", "");
    }

    private void updateButtons() {
        Row row = selectedEntry();
        if (addButton != null) addButton.active = row != null && !showingRepository;
        if (removeButton != null) removeButton.active = row != null && showingRepository;
        if (giveButton != null) giveButton.active = row != null && showingRepository;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        WeaponRepositoryView view = ClientWeaponRepositoryData.view();
        String subtitle = showingRepository ? resultCount() + " 个仓库条目"
                : resultCount() + " 个目录物品" + (view.taczLoaded() ? " · TACZ 已检测" : "");
        renderShell(graphics, subtitle);
        renderList(graphics, mouseX, mouseY, showingRepository ? "仓库暂无条目" : "未检测到武器目录");
        renderStatus(graphics, view.message(), view.error() ? UiTheme.ERROR : UiTheme.INFO);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public void onClose() {
        if (minecraft != null) minecraft.setScreen(parent);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private static String categoryLabel(String id) {
        if (id == null || id.isBlank()) return "全部分类";
        return switch (id) {
            case "gun_pistol" -> "手枪";
            case "gun_rifle" -> "步枪";
            case "gun_sniper" -> "狙击枪";
            case "gun_shotgun" -> "霰弹枪";
            case "gun_smg" -> "冲锋枪";
            case "gun_rpg" -> "火箭筒";
            case "gun_mg" -> "机枪";
            case "attachment_scope" -> "瞄具";
            case "attachment_muzzle" -> "枪口";
            case "attachment_stock" -> "枪托";
            case "attachment_grip" -> "握把";
            case "attachment_extended_mag" -> "弹匣";
            case "attachment_laser" -> "镭射/战术灯";
            case "ammo" -> "弹药";
            case "other" -> "其他装备";
            default -> id;
        };
    }

    private static String categoryName(WeaponRepositoryView view, String categoryId) {
        return view.categories().stream().filter(value -> value.id().equals(categoryId))
                .findFirst().map(WeaponCategory::name).orElse(categoryLabel(categoryId));
    }

    record Row(String id, String title, String detail, String badge, boolean repository) {
        static Row catalog(WeaponCatalogItem item) {
            ItemStack stack = item.snapshot().stack();
            String title = stack.isEmpty() ? item.snapshot().itemId() : stack.getHoverName().getString();
            String detail = categoryName(ClientWeaponRepositoryData.view(), item.categoryId())
                    + " · " + item.snapshot().itemId();
            return new Row(item.catalogId(), title, detail, "目录", false);
        }

        static Row repository(WeaponRepositoryItem item) {
            ItemStack stack = item.snapshot().stack();
            String title = item.title();
            String detail = categoryName(ClientWeaponRepositoryData.view(), item.categoryId())
                    + " · " + item.snapshot().itemId();
            String badge = item.enabled() ? "x" + stack.getCount() : "停用";
            return new Row(item.id(), title, detail, badge, true);
        }
    }
}
