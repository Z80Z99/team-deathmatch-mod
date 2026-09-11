package cn.blockforge.generated.generatedmod.client.screen;

import cn.blockforge.generated.generatedmod.client.ClientMatchShopData;
import cn.blockforge.generated.generatedmod.client.ui.UiButton;
import cn.blockforge.generated.generatedmod.client.ui.UiCycleButton;
import cn.blockforge.generated.generatedmod.client.ui.UiListScreen;
import cn.blockforge.generated.generatedmod.client.ui.UiTheme;
import cn.blockforge.generated.generatedmod.network.FpsTdmNetwork;
import cn.blockforge.generated.generatedmod.network.packet.MatchShopActionPacket;
import cn.blockforge.generated.generatedmod.shop.MatchShopAction;
import cn.blockforge.generated.generatedmod.shop.MatchShopProduct;
import cn.blockforge.generated.generatedmod.shop.MatchShopView;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/** CS-style in-match buy menu. */
public final class MatchShopScreen extends UiListScreen<MatchShopScreen.Row> {
    private static final List<String> CATEGORIES = List.of("", "gun", "ammo", "attachment", "service");
    private final Screen parent;
    private UiCycleButton<String> categoryCycle;
    private UiButton buyButton;
    private String category = "";
    private int observedRevision = ClientMatchShopData.revision();
    private int ticks;
    private List<Row> cached = List.of();
    private int cacheRevision = -1;
    private String cacheCategory = "~";

    public MatchShopScreen(Screen parent) {
        super(Component.literal("对局商店"));
        this.parent = parent;
    }

    public static void open(Screen parent) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.getConnection() == null) return;
        minecraft.setScreen(new MatchShopScreen(parent));
        send(MatchShopAction.REQUEST, "");
    }

    @Override
    protected void init() {
        beginLayout(700, 0, BUTTON_HEIGHT);
        int y = flowRow(BUTTON_HEIGHT);
        categoryCycle = flowWidget(new UiCycleButton<>(innerLeft, y, innerWidth, BUTTON_HEIGHT,
                CATEGORIES, category, MatchShopScreen::categoryName,
                value -> { category = value; refreshEntries(); updateButtons(); },
                "切换武器、弹药、配件和维修服务。", UiButton.Kind.SECONDARY), y);
        addSearch("搜索商品");
        addList();
        buyButton = footerButton("购买", 0, 2, 0, this::buySelected,
                "使用本局比赛资金购买。", UiButton.Kind.PRIMARY);
        footerButton("返回", 1, 2, 0, this::onClose, null, UiButton.Kind.SECONDARY);
        updateButtons();
    }

    private void buySelected() {
        Row row = selectedEntry();
        if (row != null) send(MatchShopAction.BUY, row.id());
    }

    private static void send(MatchShopAction action, String productId) {
        if (Minecraft.getInstance().getConnection() != null) {
            FpsTdmNetwork.sendToServer(new MatchShopActionPacket(action, productId));
        }
    }

    @Override
    protected List<Row> entries() {
        int revision = ClientMatchShopData.revision();
        if (revision == cacheRevision && category.equals(cacheCategory)) return cached;
        MatchShopView view = ClientMatchShopData.view();
        cached = view.products().stream().filter(product -> category.isBlank()
                        || categoryFor(product).equals(category))
                .map(product -> new Row(product.id(), product.title(),
                        detail(product), "$" + product.price())).toList();
        cacheRevision = revision;
        cacheCategory = category;
        return cached;
    }

    private static String categoryFor(MatchShopProduct product) {
        String id = product.categoryId();
        if (id.startsWith("gun_")) return "gun";
        if (id.startsWith("attachment_")) return "attachment";
        if (id.equals("ammo")) return "ammo";
        if (id.startsWith("service_")) return "service";
        return "other";
    }

    private static String detail(MatchShopProduct product) {
        if (product.service() != null) return product.service().name();
        ItemStack stack = product.snapshot().stack();
        return stack.isEmpty() ? product.id() : stack.getHoverName().getString() + " · " + product.id();
    }

    @Override protected String entryId(Row entry) { return entry.id; }
    @Override protected String entryTitle(Row entry) { return entry.title; }
    @Override protected String entryDetail(Row entry) { return entry.detail; }
    @Override protected String entryBadge(Row entry) { return entry.badge; }
    @Override protected int entryColor(Row entry) { return UiTheme.INFO; }

    @Override
    public void tick() {
        super.tick();
        if (ClientMatchShopData.revision() != observedRevision) {
            observedRevision = ClientMatchShopData.revision();
            refreshEntries();
            updateButtons();
        }
        if (++ticks % 20 == 0) send(MatchShopAction.REQUEST, "");
        MatchShopView view = ClientMatchShopData.view();
        if (ClientMatchShopData.revision() > 0 && !view.open() && minecraft != null) {
            minecraft.setScreen(null);
        }
    }

    private void updateButtons() {
        MatchShopView view = ClientMatchShopData.view();
        if (buyButton != null) buyButton.active = view.open() && selectedEntry() != null;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        MatchShopView view = ClientMatchShopData.view();
        String timer = view.buySecondsRemaining() == Integer.MAX_VALUE
                ? "热身购买阶段" : "剩余 " + view.buySecondsRemaining() + "s";
        String durability = view.gundbLoaded()
                ? " · 枪械 " + view.gunDamage() + "/" + view.gunMaxDamage()
                + " · 配件损坏 " + view.attachmentDamage() : "";
        renderShell(graphics, "$" + view.matchBalance() + " · " + timer + durability);
        renderList(graphics, mouseX, mouseY, view.open() ? "暂无可购买商品" : "购买阶段已结束");
        renderStatus(graphics, view.message(), view.error() ? UiTheme.ERROR : UiTheme.INFO);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override public void onClose() { if (minecraft != null) minecraft.setScreen(parent); }
    @Override public boolean isPauseScreen() { return false; }

    private static String categoryName(String value) {
        return switch (value) {
            case "gun" -> "武器";
            case "ammo" -> "弹药";
            case "attachment" -> "配件";
            case "service" -> "维修服务";
            default -> "全部商品";
        };
    }

    record Row(String id, String title, String detail, String badge) { }
}
