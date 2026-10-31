package com.xyp.gtneverreset.client.gui;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.StatCollector;
import net.minecraftforge.client.event.RenderGameOverlayEvent;

import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;

import com.xyp.gtneverreset.common.packet.NetWorkHandler;
import com.xyp.gtneverreset.common.packet.WirelessMonitorRequestPacket;
import com.xyp.gtneverreset.common.packet.WirelessMonitorSnapshotPacket;
import com.xyp.gtneverreset.config.MainConfig;
import com.xyp.gtneverreset.loader.ConfigLoader;
import com.xyp.gtneverreset.utils.world.WirelessEnergyHistory;
import com.xyp.gtnrlib.client.text.EffectTextRenderer;
import com.xyp.gtnrlib.text.TextEffectStyle;
import com.xyp.gtnrlib.text.TextEffects;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.InputEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import gregtech.api.enums.GTValues;

@SideOnly(Side.CLIENT)
public final class WirelessMonitorHud extends Gui {

    public static final WirelessMonitorHud INSTANCE = new WirelessMonitorHud();
    // #tr key.gtneverreset.wireless_monitor.toggle
    // # Toggle Wireless Network HUD
    // # zh_CN 开关无线电网 HUD
    public static final KeyBinding TOGGLE = new KeyBinding(
        "key.gtneverreset.wireless_monitor.toggle",
        Keyboard.KEY_P,
        "key.categories.gtneverreset");
    // #tr key.gtneverreset.wireless_monitor.configure
    // # Configure Wireless Network HUD
    // # zh_CN 设置无线电网 HUD
    public static final KeyBinding CONFIGURE = new KeyBinding(
        "key.gtneverreset.wireless_monitor.configure",
        Keyboard.KEY_NONE,
        "key.categories.gtneverreset");
    // #tr key.gtneverreset.wireless_monitor.move
    // # Move Wireless Network HUD
    // # zh_CN 调整无线电网 HUD 位置
    public static final KeyBinding MOVE = new KeyBinding(
        "key.gtneverreset.wireless_monitor.move",
        Keyboard.KEY_HOME,
        "key.categories.gtneverreset");
    private final WirelessEnergyHistory history = new WirelessEnergyHistory();
    private NetHandlerPlayClient connection;
    private long requestId;
    private long pendingRequest = -1;
    private int ticksUntilRequest;
    private int ticksSinceSnapshot;
    private boolean wasEnabled;
    private String cachedPalette;
    private float cachedSpeed;
    private boolean cachedCustomPalette;
    private TextEffectStyle cachedStyle;

    private WirelessMonitorHud() {}

    @SubscribeEvent
    public void onKeyInput(InputEvent event) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer == null || mc.currentScreen != null) return;
        while (TOGGLE.isPressed()) setEnabled(!MainConfig.wirelessMonitor.enabled);
        if (CONFIGURE.isPressed()) mc.displayGuiScreen(new WirelessMonitorSettingsGui());
        if (MOVE.isPressed()) mc.displayGuiScreen(new WirelessMonitorPositionGui());
    }

    public void setEnabled(boolean enabled) {
        MainConfig.wirelessMonitor.enabled = enabled;
        wasEnabled = enabled;
        reset();
        ConfigLoader.save();
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getMinecraft();
        NetHandlerPlayClient current = mc.getNetHandler();
        boolean enabled = MainConfig.wirelessMonitor.enabled;
        if (connection != current || mc.thePlayer == null || mc.theWorld == null || wasEnabled != enabled) {
            reset();
            connection = current;
            wasEnabled = enabled;
        }
        if (!enabled || mc.thePlayer == null || mc.theWorld == null || mc.isGamePaused()) return;
        int interval = Math.max(20, Math.min(200, MainConfig.wirelessMonitor.refreshTicks));
        if (++ticksSinceSnapshot > Math.max(300, interval * 3)) history.clear();
        ticksUntilRequest = Math.min(ticksUntilRequest, interval);
        if (ticksUntilRequest-- > 0) return;
        ticksUntilRequest = interval - 1;
        pendingRequest = ++requestId;
        NetWorkHandler.sendToServer(new WirelessMonitorRequestPacket(pendingRequest));
    }

    public void receive(WirelessMonitorSnapshotPacket message, NetHandlerPlayClient source) {
        if (!MainConfig.wirelessMonitor.enabled || source != connection || pendingRequest != message.requestId) return;
        pendingRequest = -1;
        ticksSinceSnapshot = 0;
        history.add(message.owner, message.tick, message.energy);
    }

    private void reset() {
        history.clear();
        pendingRequest = -1;
        ticksUntilRequest = 0;
        ticksSinceSnapshot = 0;
    }

    @SubscribeEvent
    public void onRenderOverlay(RenderGameOverlayEvent.Post event) {
        Minecraft mc = Minecraft.getMinecraft();
        if (event.type != RenderGameOverlayEvent.ElementType.ALL || !MainConfig.wirelessMonitor.enabled
            || mc.thePlayer == null
            || mc.theWorld == null
            || mc.gameSettings.hideGUI
            || mc.gameSettings.showDebugInfo
            || mc.currentScreen instanceof WirelessMonitorPositionGui
            || mc.currentScreen instanceof WirelessMonitorSettingsGui) return;
        draw(layout(event.resolution.getScaledWidth(), event.resolution.getScaledHeight()));
    }

    Layout layout(int screenWidth, int screenHeight) {
        var config = MainConfig.wirelessMonitor;
        String[] lines = lines();
        int textWidth = 0;
        for (String line : lines)
            textWidth = Math.max(textWidth, Minecraft.getMinecraft().fontRenderer.getStringWidth(line));
        int width = config.style == 0 ? textWidth + 4 : config.width;
        int height = Math.max(12, lines.length * 12) + (config.style == 2 ? 46 : 0);
        float scale = Math
            .max(0.05F, Math.min(config.scale, Math.min((screenWidth - 8F) / width, (screenHeight - 8F) / height)));
        int x = Math.max(4, Math.min(4 + config.xOffset, screenWidth - (int) Math.ceil(width * scale) - 4));
        int y = Math.max(
            4,
            Math.min(
                screenHeight - 42 - (int) Math.ceil(height * scale) - config.yOffset,
                screenHeight - (int) Math.ceil(height * scale) - 4));
        return new Layout(lines, x, y, width, height, scale);
    }

    void draw(Layout layout) {
        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
        GL11.glPushMatrix();
        try {
            GL11.glTranslatef(layout.x, layout.y, 0);
            GL11.glScalef(layout.scale, layout.scale, 1);
            GL11.glDisable(GL11.GL_DEPTH_TEST);
            GL11.glDisable(GL11.GL_LIGHTING);
            int style = MainConfig.wirelessMonitor.style;
            if (style != 0) {
                drawRect(-2, -2, layout.contentWidth - 2, layout.contentHeight - 2, 0xC018202C);
                drawRect(-2, -2, 0, layout.contentHeight - 2, 0xFF55DDEE);
            }
            for (int i = 0; i < layout.lines.length; i++) {
                String line = layout.lines[i];
                if (style == 0) drawRect(
                    -2,
                    i * 12 - 2,
                    Minecraft.getMinecraft().fontRenderer.getStringWidth(line) + 2,
                    i * 12 + 10,
                    0x80000000);
                float lineScale = style == 0 ? 1
                    : Math.min(
                        1,
                        (layout.contentWidth - 8F)
                            / Math.max(1, Minecraft.getMinecraft().fontRenderer.getStringWidth(line)));
                GL11.glPushMatrix();
                try {
                    GL11.glTranslatef(style == 0 ? 0 : 3, i * 12, 0);
                    GL11.glScalef(lineScale, lineScale, 1);
                    drawLine(line, 0, 0);
                } finally {
                    GL11.glPopMatrix();
                }
            }
            if (style == 2) drawGraph(3, Math.max(12, layout.lines.length * 12) + 2, layout.contentWidth - 10, 36);
        } finally {
            GL11.glPopMatrix();
            GL11.glPopAttrib();
        }
    }

    void drawLine(String line, int x, int y) {
        var config = MainConfig.wirelessMonitor;
        var font = Minecraft.getMinecraft().fontRenderer;
        if (!config.animatedColors) {
            font.drawStringWithShadow(line, x, y, 0xFFFFFF);
            return;
        }
        String plain = EnumChatFormatting.getTextWithoutFormattingCodes(line);
        int end = config.animateValues ? plain.length() : Math.max(plain.indexOf(':'), plain.indexOf('：')) + 1;
        if (end <= 0) end = plain.length();
        String animated = formatting() + plain.substring(0, end);
        EffectTextRenderer.INSTANCE.drawStyledLine(font, animated, effectStyle(), x, y);
        if (end < plain.length()) {
            int split = Math.max(line.indexOf(':'), line.indexOf('：')) + 1;
            font.drawStringWithShadow(
                formatting() + line.substring(split),
                x + font.getStringWidth(animated),
                y,
                0xFFFFFF);
        }
    }

    private TextEffectStyle effectStyle() {
        var config = MainConfig.wirelessMonitor;
        if (cachedStyle == null || !config.palette.equals(cachedPalette)
            || cachedSpeed != config.speed
            || cachedCustomPalette != config.customPalette) {
            List<Integer> colors = new ArrayList<>();
            if (config.customPalette) {
                try {
                    for (String color : config.palette.split(",")) {
                        if (colors.size() == 8 || !color.matches("#[0-9a-fA-F]{6}"))
                            throw new IllegalArgumentException();
                        colors.add(Integer.parseInt(color.substring(1), 16));
                    }
                } catch (IllegalArgumentException exception) {
                    colors = List.of(0x33CCFF, 0xFFAA33, 0xDD77FF);
                }
            }
            cachedStyle = new TextEffectStyle(
                TextEffects.EXOTIC_RAINBOW.rendererId(),
                colors,
                Float.isFinite(config.speed) ? Math.max(0, Math.min(10, config.speed)) : 1);
            cachedPalette = config.palette;
            cachedSpeed = config.speed;
            cachedCustomPalette = config.customPalette;
        }
        return cachedStyle;
    }

    private void drawGraph(int x, int y, int width, int height) {
        var samples = history.samples();
        drawRect(x, y, x + width, y + height, 0x60000000);
        if (samples.size() < 2) return;
        BigInteger min = samples.get(0).energy;
        BigInteger max = min;
        for (var sample : samples) {
            min = min.min(sample.energy);
            max = max.max(sample.energy);
        }
        BigInteger range = max.subtract(min);
        long lastTick = samples.get(samples.size() - 1).tick;
        long startTick = Math.max(samples.get(0).tick, lastTick - 6000);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glLineWidth(1F);
        GL11.glColor4f(0.3F, 0.85F, 1F, 1F);
        GL11.glBegin(GL11.GL_LINE_STRIP);
        for (var sample : samples) {
            // Normalize exact differences before converting to pixels, even for balances above double range.
            float fraction = range.signum() == 0 ? 0.5F
                : new BigDecimal(sample.energy.subtract(min)).divide(new BigDecimal(range), 8, RoundingMode.HALF_UP)
                    .floatValue();
            GL11.glVertex2f(
                x + (width - 1F) * (sample.tick - startTick) / Math.max(1, lastTick - startTick),
                y + height - 1 - fraction * (height - 2));
        }
        GL11.glEnd();
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glColor4f(1, 1, 1, 1);
    }

    private static String formatting() {
        return (MainConfig.wirelessMonitor.bold ? "\u00a7l" : "")
            + (MainConfig.wirelessMonitor.italic ? "\u00a7o" : "");
    }

    private String[] lines() {
        var config = MainConfig.wirelessMonitor;
        // #tr hud.gtneverreset.wireless.realtime
        // # Instant:
        // # zh_CN 瞬时：
        String realtime = StatCollector.translateToLocal("hud.gtneverreset.wireless.realtime");
        // #tr hud.gtneverreset.wireless.average
        // # Average (5m):
        // # zh_CN 平均（5分钟）：
        String average = StatCollector.translateToLocal("hud.gtneverreset.wireless.average");
        // #tr hud.gtneverreset.wireless.energy
        // # Wireless network:
        // # zh_CN 无线电网：
        String energy = StatCollector.translateToLocal("hud.gtneverreset.wireless.energy");
        // #tr hud.gtneverreset.wireless.syncing
        // # Synchronizing...
        // # zh_CN 同步中…
        String syncing = EnumChatFormatting.GRAY + StatCollector.translateToLocal("hud.gtneverreset.wireless.syncing");
        // #tr hud.gtneverreset.wireless.measuring
        // # Measuring...
        // # zh_CN 采样中…
        String measuring = EnumChatFormatting.GRAY
            + StatCollector.translateToLocal("hud.gtneverreset.wireless.measuring");
        List<String> result = new ArrayList<>();
        if (config.showRealtime) result.add(
            EnumChatFormatting.AQUA + realtime
                + " "
                + (history.realtime() == null ? measuring
                    : Format.rate(history.realtime(), config.scientific, config.showTier)));
        if (config.showAverage) result.add(
            EnumChatFormatting.AQUA + average
                + " "
                + (history.average() == null ? measuring
                    : Format.rate(history.average(), config.scientific, config.showTier)));
        if (config.showEnergy) result.add(
            EnumChatFormatting.AQUA + energy
                + " "
                + (history.energy() == null ? syncing
                    : EnumChatFormatting.WHITE
                        + Format.number(new BigDecimal(history.energy()), config.scientific, false)
                        + EnumChatFormatting.AQUA
                        + " EU"));
        if (result.isEmpty() && config.style != 2) result.add(EnumChatFormatting.AQUA + energy);
        return result.stream()
            .map(line -> line.replaceAll("(§[0-9a-fr])", "$1" + formatting()))
            .toArray(String[]::new);
    }

    static final class Layout {

        final String[] lines;
        final int x, y, width, height, contentWidth, contentHeight;
        final float scale;

        private Layout(String[] lines, int x, int y, int width, int height, float scale) {
            this.lines = lines;
            this.x = x;
            this.y = y;
            this.contentWidth = width;
            this.contentHeight = height;
            this.width = (int) Math.ceil(width * scale);
            this.height = (int) Math.ceil(height * scale);
            this.scale = scale;
        }

        boolean contains(int mouseX, int mouseY) {
            return mouseX >= x - 2 * scale && mouseX <= x + width && mouseY >= y - 2 * scale && mouseY <= y + height;
        }
    }

    private static final class Format {

        public static String number(BigDecimal value, boolean scientific, boolean fractional) {
            BigDecimal absolute = value.abs();
            if ((scientific && absolute.compareTo(BigDecimal.valueOf(1000)) >= 0)
                || absolute.precision() - absolute.scale() > 18) {
                BigDecimal rounded = absolute.round(new MathContext(3, RoundingMode.HALF_UP));
                int exponent = rounded.precision() - rounded.scale() - 1;
                return rounded.movePointLeft(exponent)
                    .stripTrailingZeros()
                    .toPlainString() + "×10^"
                    + exponent;
            }
            if (fractional && absolute.signum() > 0 && absolute.compareTo(new BigDecimal("0.01")) < 0) return "<0.01";
            DecimalFormat format = new DecimalFormat(
                fractional ? "#,##0.00" : "#,##0",
                DecimalFormatSymbols.getInstance(Locale.US));
            format.setRoundingMode(RoundingMode.HALF_UP);
            return format.format(absolute);
        }

        public static String rate(BigDecimal value, boolean scientific, boolean showTier) {
            EnumChatFormatting color = value.signum() > 0 ? EnumChatFormatting.GREEN
                : value.signum() < 0 ? EnumChatFormatting.RED : EnumChatFormatting.GRAY;
            String sign = value.signum() > 0 ? "+" : value.signum() < 0 ? "-" : "";
            if (!showTier) return color + sign + number(value, scientific, true) + EnumChatFormatting.AQUA + " EU/t";
            BigDecimal absolute = value.abs();
            int tier = 0;
            // GregTech's final voltage entry is an error sentinel, not a displayable tier.
            while (tier + 2 < GTValues.V.length && absolute.compareTo(BigDecimal.valueOf(GTValues.V[tier + 1])) >= 0) {
                tier++;
            }
            BigDecimal amps = absolute.divide(BigDecimal.valueOf(GTValues.V[tier]), 6, RoundingMode.HALF_UP);
            DecimalFormat ampFormat = new DecimalFormat("0.0", DecimalFormatSymbols.getInstance(Locale.US));
            String amperage = amps.precision() - amps.scale() > 6 ? number(amps, true, true) : ampFormat.format(amps);
            return color + sign
                + number(value, scientific, true)
                + EnumChatFormatting.AQUA
                + " EU/t ("
                + GTValues.TIER_COLORS[tier]
                + amperage
                + "A "
                + GTValues.VN[tier]
                + EnumChatFormatting.AQUA
                + ")";
        }
    }
}
