package com.xyp.gtneverreset.client.gui;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.EnumChatFormatting;

import com.xyp.gtneverreset.client.profile.radialmenu.ProfileManager;
import com.xyp.gtneverreset.client.profile.radialmenu.ProfileStorage;
import com.xyp.gtneverreset.utils.gui.radialmenu.Profile;
import com.xyp.gtneverreset.utils.gui.radialmenu.ProfileBinding;
import com.xyp.gtnrlib.gui.widget.RadialUi;
import com.xyp.gtnrlib.gui.widget.RadialUiIconButton;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class RadialAutoBindRulesGui extends RadialUiScreen {

    private static final int ID_ADD = 1;
    private static final int ID_USE_CURRENT = 2;
    private static final int ID_TYPE_BASE = 100;
    private static final int ID_REMOVE_BASE = 200;

    private static final int TYPE_WIDTH = 86;
    private static final int REMOVE_WIDTH = 20;

    private final String profileName;

    private Profile profile;

    private final List<GuiTextField> valueFields = new ArrayList<>();

    /** Which rule each built field belongs to - only the visible rows have fields, so the two are not parallel. */
    private final List<Integer> fieldRuleIndex = new ArrayList<>();

    public RadialAutoBindRulesGui(String profileName) {
        this.profileName = profileName;
    }

    @Override
    protected String titleKey() {
        // #tr radialmenu.rules.title
        // # Auto-bind rules: %s
        // # zh_CN 自动绑定规则：%s
        return "radialmenu.rules.title";
    }

    @Override
    protected Object[] titleArgs() {
        return new Object[] { profileName };
    }

    @Override
    protected int panelWidth() {
        return 360;
    }

    @Override
    protected boolean framedViewport() {
        return true;
    }

    /** Row for the two add buttons, which stay put rather than scrolling away with the rules. */
    @Override
    protected int footerHeight() {
        return RadialUi.ROW + RadialUi.GAP;
    }

    @Override
    protected void buildControls() {
        if (profile == null) {
            profile = ProfileStorage.loadProfile(profileName);
            if (profile == null) {
                RadialGuiStack.pop();
                return;
            }
            if (profile.bindings == null) {
                profile.bindings = new ArrayList<>();
            }
        }

        valueFields.clear();
        fieldRuleIndex.clear();

        int left = contentLeft();
        int y = scrolledTop();

        for (int i = 0; i < profile.bindings.size(); i++) {
            ProfileBinding binding = profile.bindings.get(i);

            // Rows that do not fit are simply not built. Creating them and hiding them afterwards left buttons and
            // text fields disagreeing about where the edge was, so half a row could survive the cut.
            if (!isVisibleRow(y)) {
                y += RadialUi.STEP;
                continue;
            }

            this.buttonList.add(new GuiButton(ID_TYPE_BASE + i, left, y, TYPE_WIDTH, RadialUi.ROW, typeLabel(binding)));
            // #tr radialmenu.rules.type.tip
            // # Match a server address, a world folder or any singleplayer world
            // # zh_CN 匹配服务器地址、世界文件夹或任意单人世界
            tooltip(ID_TYPE_BASE + i, I18n.format("radialmenu.rules.type.tip"));

            int fieldLeft = left + TYPE_WIDTH + RadialUi.GAP;
            int fieldWidth = contentRight() - fieldLeft - REMOVE_WIDTH - RadialUi.GAP;

            GuiTextField field = new GuiTextField(this.fontRendererObj, fieldLeft + 1, y + 3, fieldWidth - 2, 14);
            field.setMaxStringLength(128);
            field.setText(binding.value == null ? "" : binding.value);
            field.setCursorPositionZero();
            // A singleplayer rule matches on its own, so there is nothing to type.
            field.setEnabled(binding.type != ProfileBinding.Type.Singleplayer);
            valueFields.add(field);
            fieldRuleIndex.add(Integer.valueOf(i));

            this.buttonList.add(
                new RadialUiIconButton(
                    ID_REMOVE_BASE + i,
                    contentRight() - REMOVE_WIDTH,
                    y,
                    REMOVE_WIDTH,
                    RadialUi.ROW,
                    RadialUiIconButton.Icon.Cross));
            y += RadialUi.STEP;
        }

        setContentHeight(profile.bindings.size() * RadialUi.STEP);

        int footerY = panelBottom - RadialUi.PAD - RadialUi.ROW;
        int half = (contentWidth() - RadialUi.GAP) / 2;
        // Marked as footer below, which is what exempts them from the scroll visibility check.
        // #tr radialmenu.rules.add
        // # Add rule
        // # zh_CN 添加规则
        GuiButton add = new GuiButton(ID_ADD, left, footerY, half, RadialUi.ROW, I18n.format("radialmenu.rules.add"));
        GuiButton useCurrent = new GuiButton(
            ID_USE_CURRENT,
            left + half + RadialUi.GAP,
            footerY,
            half,
            RadialUi.ROW,
            // #tr radialmenu.rules.useCurrent
            // # Use current world
            // # zh_CN 使用当前世界
            I18n.format("radialmenu.rules.useCurrent"));
        this.buttonList.add(add);
        this.buttonList.add(useCurrent);
        markFooter(ID_ADD);
        markFooter(ID_USE_CURRENT);
        // #tr radialmenu.rules.useCurrent.tip
        // # Add a rule for the current server or world
        // # zh_CN 为当前服务器或世界添加规则
        tooltip(ID_USE_CURRENT, I18n.format("radialmenu.rules.useCurrent.tip"));
        // #tr radialmenu.editor.save
        // # Save
        // # zh_CN 保存
        addBottomBar("radialmenu.editor.save", null, "gui.cancel");
    }

    private String typeLabel(ProfileBinding binding) {
        ProfileBinding.Type type = binding.type == null ? ProfileBinding.Type.Server : binding.type;
        switch (type) {
            case Server:
                // #tr radialmenu.rules.type.server
                // # Server
                // # zh_CN 服务器
                return I18n.format("radialmenu.rules.type.server");
            case World:
                // #tr radialmenu.rules.type.world
                // # World
                // # zh_CN 世界
                return I18n.format("radialmenu.rules.type.world");
            default:
                // #tr radialmenu.rules.type.singleplayer
                // # Singleplayer
                // # zh_CN 单人游戏
                return I18n.format("radialmenu.rules.type.singleplayer");
        }
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        if (button.id == ID_PRIMARY) {
            save();
            return;
        }
        if (button.id == ID_SECONDARY) {
            onCancel();
            return;
        }
        if (button.id == ID_ADD) {
            captureFields();
            profile.bindings.add(ProfileBinding.server(""));
            requestRebuild();
            return;
        }
        if (button.id == ID_USE_CURRENT) {
            captureFields();
            profile.bindings.add(currentWorldRule());
            requestRebuild();
            return;
        }

        int typeIndex = button.id - ID_TYPE_BASE;
        if (typeIndex >= 0 && typeIndex < profile.bindings.size()) {
            captureFields();
            cycleType(profile.bindings.get(typeIndex));
            requestRebuild();
            return;
        }

        int removeIndex = button.id - ID_REMOVE_BASE;
        if (removeIndex >= 0 && removeIndex < profile.bindings.size()) {
            captureFields();
            profile.bindings.remove(removeIndex);
            requestRebuild();
        }
    }

    private static void cycleType(ProfileBinding binding) {
        ProfileBinding.Type[] types = ProfileBinding.Type.values();
        int index = binding.type == null ? -1 : binding.type.ordinal();
        binding.type = types[(index + 1) % types.length];
    }

    private ProfileBinding currentWorldRule() {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.isSingleplayer()) {
            return mc.getIntegratedServer() == null ? ProfileBinding.world("")
                : ProfileBinding.world(
                    mc.getIntegratedServer()
                        .getFolderName());
        }
        ServerData data = mc.func_147104_D();
        return ProfileBinding.server(data == null ? "" : data.serverIP);
    }

    /** Text fields are the source of truth for values, so they are read back before any structural change. */
    private void captureFields() {
        for (int i = 0; i < valueFields.size(); i++) {
            int ruleIndex = fieldRuleIndex.get(i)
                .intValue();
            if (ruleIndex < profile.bindings.size()) {
                profile.bindings.get(ruleIndex).value = valueFields.get(i)
                    .getText()
                    .trim();
            }
        }
    }

    private void save() {
        captureFields();
        ProfileStorage.saveProfile(profile);

        // The edited profile may be the one loaded in memory; reload so the change takes effect immediately.
        if (profileName.equals(ProfileManager.activeName())) {
            ProfileManager.switchTo(profileName);
        }
        RadialGuiStack.pop();
    }

    @Override
    protected void beforeScroll() {
        captureFields();
    }

    @Override
    protected void drawContent(int mouseX, int mouseY, float partialTicks) {
        if (profile == null) {
            return;
        }
        if (profile.bindings.isEmpty()) {
            this.drawCenteredString(
                this.fontRendererObj,
                // #tr radialmenu.rules.empty
                // # No rules. This profile is only selected by hand.
                // # zh_CN 没有规则。此配置只能手动选择。
                EnumChatFormatting.GRAY + I18n.format("radialmenu.rules.empty"),
                this.width / 2,
                contentTop() + 6,
                RadialUi.TEXT);
        }
        // Only visible rows were built, so there is nothing left to clip here.
        for (GuiTextField field : valueFields) {
            field.drawTextBox();
        }
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int button) {
        super.mouseClicked(mouseX, mouseY, button);
        // A field clipped at the panel edge still answers to clicks on the part that was cut away.
        if (!isShowing() || !isInsideViewport(mouseY)) {
            return;
        }
        for (GuiTextField field : valueFields) {
            field.mouseClicked(mouseX, mouseY, button);
        }
    }

    @Override
    protected boolean handleKey(char typedChar, int keyCode) {
        for (GuiTextField field : valueFields) {
            if (field.textboxKeyTyped(typedChar, keyCode)) {
                return true;
            }
        }
        return false;
    }
}
