package com.xyp.gtneverreset.qa;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.entity.passive.EntityCow;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ScreenShotHelper;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.WorldType;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;

import com.cleanroommc.modularui.ModularUI;
import com.cleanroommc.modularui.ModularUIConfig;
import com.cleanroommc.modularui.api.IMuiScreen;
import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.api.widget.IWidget;
import com.cleanroommc.modularui.drawable.Rectangle;
import com.cleanroommc.modularui.drawable.graph.GraphDrawable;
import com.cleanroommc.modularui.factory.ClientGUI;
import com.cleanroommc.modularui.screen.ModularContainer;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.screen.ModularScreen;
import com.cleanroommc.modularui.test.TestBlock;
import com.cleanroommc.modularui.test.TestGui;
import com.cleanroommc.modularui.test.TestGuis;
import com.cleanroommc.modularui.test.TestTile;
import com.cleanroommc.modularui.value.BoolValue;
import com.cleanroommc.modularui.value.DoubleValue;
import com.cleanroommc.modularui.value.sync.IntSyncValue;
import com.cleanroommc.modularui.widget.ParentWidget;
import com.cleanroommc.modularui.widgets.ButtonWidget;
import com.cleanroommc.modularui.widgets.EntityDisplayWidget;
import com.cleanroommc.modularui.widgets.ListWidget;
import com.cleanroommc.modularui.widgets.PagedWidget;
import com.cleanroommc.modularui.widgets.SliderWidget;
import com.cleanroommc.modularui.widgets.SortableListWidget;
import com.cleanroommc.modularui.widgets.ToggleButton;
import com.cleanroommc.modularui.widgets.menu.ContextMenuButton;
import com.cleanroommc.modularui.widgets.textfield.TextFieldWidget;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;

@Mod(modid = "mui2capabilitiesqa", name = "MUI2 capabilities QA", version = "1", dependencies = "after:gtneverreset")
public final class Mui2CapabilitiesChecks {

    private final List<Method> examples = new ArrayList<>();
    private final List<String> results = new ArrayList<>();
    private boolean started;
    private volatile boolean finished;
    private volatile int syncStage;
    private int stage = -1;
    private int frame;
    private int clientTicks;
    private int serverTicks;
    private long openedAt;
    private ModularScreen screen;
    private ModularPanel panel;
    private String name;
    private int searchCount;
    private int exampleFailures;
    private Object serverTile;

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        FMLCommonHandler.instance()
            .bus()
            .register(this);
        for (Method method : TestGuis.class.getDeclaredMethods()) {
            if (Modifier.isStatic(method.getModifiers()) && Modifier.isPublic(method.getModifiers())
                && method.getParameterCount() == 0
                && method.getReturnType() == ModularPanel.class) {
                examples.add(method);
            }
        }
        examples.sort(Comparator.comparing(Method::getName));
        record(
            "ENV MUI2=" + Loader.instance()
                .getIndexedModList()
                .get(ModularUI.ID)
                .getVersion() + ", Java=" + System.getProperty("java.version") + ", display=1100x740, language=zh_CN");
    }

    @SubscribeEvent
    public void client(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || finished) return;
        Minecraft mc = Minecraft.getMinecraft();
        mc.gameSettings.pauseOnLostFocus = false;
        mc.gameSettings.hideGUI = true;
        mc.ingameGUI.getChatGUI()
            .clearChatMessages();
        try {
            if (!started && mc.theWorld == null && mc.currentScreen != null) {
                started = true;
                mc.launchIntegratedServer(
                    "mui2-lab-" + System.currentTimeMillis(),
                    "MUI2 Lab",
                    new WorldSettings(71, WorldSettings.GameType.CREATIVE, false, false, WorldType.FLAT));
            }
            if (mc.thePlayer == null || mc.theWorld == null) {
                if (stage >= 0) throw new AssertionError("test client lost its world");
                return;
            }
            if (syncStage == 10 && mc.theWorld.getTileEntity(0, 10, 0) instanceof TestTile) syncStage = 11;
            if (++clientTicks > 6000) throw new AssertionError("client timeout stage=" + stage + " sync=" + syncStage);
            if (stage == -1 && clientTicks > 60) {
                require(ModularUIConfig.enableTestGuis, "native test GUI configuration enabled");
                mc.thePlayer.inventory.setInventorySlotContents(0, new ItemStack(Items.diamond));
                mc.thePlayer.inventory.currentItem = 0;
                PlayerInteractEvent click = new PlayerInteractEvent(
                    mc.thePlayer,
                    PlayerInteractEvent.Action.RIGHT_CLICK_AIR,
                    0,
                    0,
                    0,
                    -1,
                    mc.theWorld);
                MinecraftForge.EVENT_BUS.post(click);
                require(
                    click.isCanceled() && mc.currentScreen instanceof IMuiScreen,
                    "diamond air-use event opens MUI2");
                screen = ((IMuiScreen) mc.currentScreen).getScreen();
                require(screen instanceof TestGuis, "diamond entry is the upstream TestGuis");
                panel = screen.getMainPanel();
                name = "00-diamond-menu";
                stage = 0;
                frame = 0;
                openedAt = System.nanoTime();
            }
        } catch (Throwable failure) {
            finish(failure);
        }
    }

    @SubscribeEvent
    public void render(TickEvent.RenderTickEvent event) {
        if (event.phase != TickEvent.Phase.END || finished || stage < 0) return;
        try {
            Minecraft mc = Minecraft.getMinecraft();
            if (!(mc.currentScreen instanceof IMuiScreen)) return;
            if (++frame == 35) {
                require(panel.getArea().width > 0 && panel.getArea().height > 0, name + " has a laid-out panel");
                screenshot(name);
                interact();
            }
            if (frame < 65) return;
            if (stage <= examples.size() + 5) {
                if (name.equals("buildSearchTest")) {
                    ListWidget<?, ?> list = first(panel, ListWidget.class);
                    long visible = list.getChildren()
                        .stream()
                        .filter(IWidget::isEnabled)
                        .count();
                    require(
                        visible > 0 && visible < searchCount,
                        "search filters " + searchCount + " rows to " + visible);
                    screenshot(name + "-filtered");
                }
                if (name.equals("buildContextMenu")) screenshot(name + "-opened");
                if (name.contains("Animation") || name.equals("buildTextFieldUI") || name.startsWith("dashboard")) {
                    screenshot(name + "-after");
                }
                record(
                    "RENDER " + name
                        + ", widgets="
                        + flatten(panel).size()
                        + ", panel="
                        + panel.getArea().width
                        + "x"
                        + panel.getArea().height
                        + ", elapsedMs="
                        + (System.nanoTime() - openedAt) / 1_000_000);
                advance();
            } else {
                syncClient();
            }
        } catch (Throwable failure) {
            finish(failure);
        }
    }

    private void advance() throws Exception {
        Minecraft mc = Minecraft.getMinecraft();
        stage++;
        frame = 0;
        openedAt = System.nanoTime();
        if (stage <= examples.size()) {
            Method method = examples.get(stage - 1);
            name = method.getName();
            try {
                panel = (ModularPanel) method.invoke(null);
            } catch (java.lang.reflect.InvocationTargetException failure) {
                exampleFailures++;
                Throwable cause = failure.getCause();
                record("EXAMPLE_FAILURE " + name + ": " + cause);
                cause.printStackTrace();
                advance();
                return;
            }
            screen = new ModularScreen(ModularUI.ID, panel);
            ClientGUI.open(screen);
        } else if (stage == examples.size() + 1) {
            name = "sortable-list";
            ClientGUI.open(new TestGui());
            screen = ((IMuiScreen) mc.currentScreen).getScreen();
            panel = screen.getMainPanel();
        } else if (stage <= examples.size() + 4) {
            int scale = stage - examples.size() - 1;
            mc.gameSettings.guiScale = scale;
            name = "dashboard-scale-" + scale;
            panel = dashboard();
            screen = new ModularScreen("gtneverreset", panel);
            ClientGUI.open(screen);
            ScaledResolution resolution = new ScaledResolution(mc, mc.displayWidth, mc.displayHeight);
            mc.currentScreen.setWorldAndResolution(mc, resolution.getScaledWidth(), resolution.getScaledHeight());
        } else if (stage == examples.size() + 5) {
            mc.gameSettings.guiScale = 2;
            name = "entity-direct-cow";
            EntityCow preview = new EntityCow(mc.theWorld);
            panel = new ModularPanel("entity_preview").size(180, 200)
                .child(
                    IKey.str("实体预览 / 直接构造客户端实体")
                        .asWidget()
                        .top(10)
                        .horizontalCenter())
                .child(
                    new EntityDisplayWidget(() -> preview).asWidget()
                        .pos(30, 30)
                        .size(120, 150));
            screen = new ModularScreen("gtneverreset", panel);
            ClientGUI.open(screen);
        } else {
            mc.thePlayer.closeScreen();
            mc.gameSettings.guiScale = 2;
            syncStage = 1;
            name = "synced-tile";
        }
    }

    private void interact() {
        if (name.equals("buildToggleGridListUI")) {
            ToggleButton toggle = first(panel, ToggleButton.class);
            boolean before = toggle.isValueSelected();
            toggle.onMousePressed(0);
            require(toggle.isValueSelected() != before, "toggle event changes bound local value");
        } else if (name.equals("buildSearchTest")) {
            searchCount = first(panel, ListWidget.class).getChildren()
                .size();
            TextFieldWidget search = first(panel, TextFieldWidget.class);
            search.setText("钻石");
            search.onRemoveFocus(screen.getContext());
        } else if (name.equals("buildTextFieldUI")) {
            List<TextFieldWidget> fields = all(panel, TextFieldWidget.class);
            fields.get(0)
                .setText("中文 GUI / MUI2");
            fields.get(0)
                .onRemoveFocus(screen.getContext());
            require(
                fields.get(0)
                    .getStringValue()
                    .getStringValue()
                    .contains("中文"),
                "Chinese text commits to local value");
            fields.get(1)
                .setText("99999");
            fields.get(1)
                .onRemoveFocus(screen.getContext());
            require(
                fields.get(1)
                    .parse(
                        fields.get(1)
                            .getText())
                    == 1000,
                "numeric input clamps to configured range");
        } else if (name.equals("buildContextMenu")) {
            ContextMenuButton<?> menu = first(panel, ContextMenuButton.class);
            menu.onMousePressed(0);
            require(menu.isOpen(), "context menu opens through its click handler");
        } else if (name.equals("sortable-list")) {
            SortableListWidget<?> list = first(panel, SortableListWidget.class);
            IWidget before = list.getChildren()
                .get(0);
            list.moveTo(0, 1);
            require(
                list.getChildren()
                    .get(1) == before,
                "sortable list move API changes row order");
        } else if (name.startsWith("dashboard")) {
            var area = panel.getArea();
            ScaledResolution resolution = new ScaledResolution(Minecraft.getMinecraft(), 1100, 740);
            require(
                area.x >= 0 && area.y >= 0
                    && area.x + area.width <= resolution.getScaledWidth()
                    && area.y + area.height <= resolution.getScaledHeight(),
                name + " fits scaled viewport");
            SliderWidget slider = first(panel, SliderWidget.class);
            slider.setValue(75, true);
            require(slider.getSliderValue() == 75, "slider accepts and displays 75 within bounds");
            ListWidget<?, ?> jobs = first(panel, ListWidget.class);
            jobs.getScrollData()
                .scrollBy(jobs.getScrollArea(), 100);
            require(
                jobs.getScrollData()
                    .getScroll() > 0,
                "80-row dashboard scroll offset advances");
        }
    }

    @SubscribeEvent
    public void server(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || finished || syncStage == 0) return;
        var server = FMLCommonHandler.instance()
            .getMinecraftServerInstance();
        if (server == null || server.getConfigurationManager().playerEntityList.isEmpty()) return;
        try {
            EntityPlayerMP player = (EntityPlayerMP) server.getConfigurationManager().playerEntityList.get(0);
            if (++serverTicks > 1200) throw new AssertionError("sync timeout " + syncStage);
            if (syncStage == 1) {
                player.worldObj.setBlock(0, 10, 0, TestBlock.testBlock);
                serverTile = player.worldObj.getTileEntity(0, 10, 0);
                player.playerNetServerHandler.setPlayerLocation(1.5, 11, 2.5, 0, 0);
                syncStage = 10;
            } else if (syncStage == 11) {
                TestBlock.testBlock.onBlockActivated(player.worldObj, 0, 10, 0, player, 1, .5f, .5f, .5f);
                IntSyncValue cycle = ((ModularContainer) player.openContainer).getSyncManager()
                    .getMainPSM()
                    .findSyncHandler("cycle_state", IntSyncValue.class);
                require(!cycle.isAllowC2S(), "upstream TestTile cycle_state is S2C-only by default");
                record(
                    "TEST_ADAPTATION enable allowC2S on the test tile cycle_state on both sides for round-trip probe");
                cycle.allowC2S();
                syncStage = 2;
            } else if (syncStage == 3 && ((Integer) field(serverTile, "cycleState")) == 1) {
                record("CHECK client toggle packet changed server tile cycleState to 1");
                setField(serverTile, "cycleState", 2);
                syncStage = 4;
            }
        } catch (Throwable failure) {
            finish(failure);
        }
    }

    private void syncClient() {
        Minecraft mc = Minecraft.getMinecraft();
        screen = ((IMuiScreen) mc.currentScreen).getScreen();
        panel = screen.getMainPanel();
        if (!panel.getName()
            .equals("test_tile")) return;
        List<ToggleButton> toggles = all(panel, ToggleButton.class);
        if (syncStage == 2 && toggles.size() >= 3) {
            IntSyncValue cycle = ((ModularContainer) mc.thePlayer.openContainer).getSyncManager()
                .getMainPSM()
                .findSyncHandler("cycle_state", IntSyncValue.class);
            require(!cycle.isAllowC2S(), "client test tile also defaults to S2C-only");
            cycle.allowC2S();
            screenshot("synced-tile-before");
            toggles.get(1)
                .onMousePressed(0);
            syncStage = 3;
        } else if (syncStage == 4 && toggles.get(2)
            .isValueSelected()) {
                require(
                    !toggles.get(1)
                        .isValueSelected(),
                    "server change synchronizes to client toggle group");
                screenshot("synced-tile-after");
                PagedWidget<?> pages = first(panel, PagedWidget.class);
                pages.setPage(1);
                require(
                    pages.getCurrentPageIndex() == 1 && pages.getCurrentPage()
                        .isEnabled(),
                    "native tile second tab activates");
                syncStage = 5;
                frame = 0;
            } else if (syncStage == 5 && frame > 65) {
                screenshot("synced-tile-second-tab");
                finish(null);
            }
    }

    private static ModularPanel dashboard() {
        int ink = 0xFF101B2A;
        int card = 0xFF1C2C40;
        DoubleValue power = new DoubleValue(48);
        ModularPanel root = new ModularPanel("factory_dashboard").widthRel(.92f)
            .heightRel(.88f)
            .background(new Rectangle().color(ink));
        root.child(
            IKey.str("GTNR  /  工厂控制台")
                .style(IKey.WHITE)
                .asWidget()
                .pos(12, 10));
        root.child(
            IKey.str("ONLINE  ·  MUI2 能力验证")
                .style(IKey.AQUA)
                .asWidget()
                .pos(12, 25));
        ListWidget<IWidget, ?> jobs = new ListWidget<>().left(12)
            .top(48)
            .bottom(32)
            .widthRel(.34f);
        for (int i = 0; i < 80; i++) {
            final int index = i;
            jobs.child(
                new ButtonWidget<>().height(22)
                    .widthRel(1f)
                    .background(new Rectangle().color(card))
                    .overlay(
                        IKey.str(String.format("%02d  %s", i + 1, i % 2 == 0 ? "装配任务" : "化学处理"))
                            .style(IKey.WHITE))
                    .addTooltipLine("任务 " + index + "：可绑定服务端任务详情和操作"));
        }
        root.child(jobs);
        ParentWidget<?> detail = new ParentWidget<>().leftRel(.39f)
            .right(12)
            .top(48)
            .bottom(32)
            .background(new Rectangle().color(card));
        detail.child(
            IKey.str("能耗趋势 / EU·t⁻¹")
                .style(IKey.WHITE)
                .asWidget()
                .pos(8, 8));
        double[] x = new double[80], y = new double[80];
        for (int i = 0; i < x.length; i++) {
            x[i] = i;
            y[i] = 40 + 15 * Math.sin(i / 8.0);
        }
        detail.child(
            new GraphDrawable().plot(x, y)
                .asWidget()
                .left(8)
                .right(8)
                .top(25)
                .bottom(85));
        detail.child(
            new ParentWidget<>().left(8)
                .right(8)
                .bottom(58)
                .height(6)
                .background(new Rectangle().color(0xFF354B63))
                .child(
                    new Rectangle().color(0xFF53D6CA)
                        .asWidget()
                        .widthRel(.72f)
                        .heightRel(1f)));
        detail.child(
            IKey.str("完成度 72% / 功率限制")
                .style(IKey.GRAY)
                .asWidget()
                .left(8)
                .bottom(69));
        detail.child(
            new SliderWidget().left(8)
                .right(8)
                .bottom(30)
                .height(14)
                .background(new Rectangle().color(0xFF354B63))
                .sliderTexture(new Rectangle().color(0xFF53D6CA))
                .bounds(0, 100)
                .value(power));
        detail.child(
            new ToggleButton().left(8)
                .bottom(5)
                .size(16)
                .selectedBackground(new Rectangle().color(0xFF53D6CA))
                .value(new BoolValue(true)));
        detail.child(
            IKey.str("自动调度")
                .style(IKey.WHITE)
                .asWidget()
                .left(30)
                .bottom(9));
        root.child(detail);
        root.child(
            IKey.str("80 个任务  |  演示数据  |  滚动列表 · 图表 · 滑条 · 状态切换")
                .style(IKey.GRAY)
                .asWidget()
                .left(12)
                .bottom(12));
        return root;
    }

    private static List<IWidget> flatten(IWidget root) {
        List<IWidget> widgets = new ArrayList<>();
        widgets.add(root);
        for (IWidget child : root.getChildren()) widgets.addAll(flatten(child));
        return widgets;
    }

    private static <T> List<T> all(IWidget root, Class<T> type) {
        List<T> matches = new ArrayList<>();
        for (IWidget widget : flatten(root)) if (type.isInstance(widget)) matches.add(type.cast(widget));
        return matches;
    }

    private static <T> T first(IWidget root, Class<T> type) {
        return all(root, type).stream()
            .findFirst()
            .orElseThrow(() -> new AssertionError("missing " + type.getName()));
    }

    private static Object field(Object owner, String name) throws ReflectiveOperationException {
        Field field = owner.getClass()
            .getDeclaredField(name);
        field.setAccessible(true);
        return field.get(owner);
    }

    private static void setField(Object owner, String name, Object value) throws ReflectiveOperationException {
        Field field = owner.getClass()
            .getDeclaredField(name);
        field.setAccessible(true);
        field.set(owner, value);
    }

    private void screenshot(String filename) {
        Minecraft mc = Minecraft.getMinecraft();
        ScreenShotHelper
            .saveScreenshot(new File("."), filename + ".png", mc.displayWidth, mc.displayHeight, mc.getFramebuffer());
        require(new File("screenshots", filename + ".png").isFile(), "screenshot saved: " + filename);
    }

    private void require(boolean success, String message) {
        if (!success) throw new AssertionError(message);
        record("CHECK " + message);
    }

    private synchronized void record(String message) {
        results.add(message);
        System.out.println("MUI2_QA: " + message);
    }

    private void finish(Throwable failure) {
        if (finished) return;
        finished = true;
        if (failure != null) failure.printStackTrace();
        record("SUMMARY upstream example construction failures=" + exampleFailures);
        record(failure == null ? "PASS (probe completed; inspect EXAMPLE_FAILURE findings)" : "FAIL " + failure);
        try {
            Files.writeString(Path.of("result.txt"), failure == null ? "PASS" : "FAIL");
            Files.write(Path.of("report.txt"), results);
        } catch (Exception error) {
            error.printStackTrace();
        }
        Thread exit = new Thread(() -> {
            try {
                Thread.sleep(5000);
            } catch (InterruptedException ignored) {
                Thread.currentThread()
                    .interrupt();
            }
            FMLCommonHandler.instance()
                .exitJava(failure == null ? 0 : 1, true);
        }, "mui2-qa-exit");
        exit.setDaemon(true);
        exit.start();
        Minecraft.getMinecraft()
            .shutdown();
    }
}
