# AE2 Theme API

GTNRLib provides light, dark and purple-highlighted dark AE2 themes for ModularUI 2. Their source Javadoc defines parameter units, return values, null handling and lifecycle requirements.

Every AE2 widget decides its look while drawing, from the theme of the panel it is in (`AE2Look.of(widget)`), so a dialog that uses a theme of its own is drawn in that theme's look, not the screen's. A theme names its AE2 skin in its `gtnrlibAE2Look` theme data entry, which the AE2 themes write and themes based on them inherit; a theme without the entry is recognised by its identifier. Nothing has to be passed to a widget when it is created.

`AE2Look` is the one way to say which look to draw: one of the three AE2 skins (light, dark, purple dark) or the plain look of every other theme. Whatever exists in every look is asked of the look itself: `listText()`, `panelText(available)`, `buttonText(state, height)`, `error()`, `drawWell`, `drawSelectedRow`, `drawButton`, `drawCheckbox`, `drawScrollbar`, `scrollbarWidth()` and so on. Parts only AE2 has (the toolbar strip, tables, the upgrade panel) take the skin, `look.variant()`, which is `null` in the plain look. Drawables made for a widget, such as `AE2ButtonPainter.button(widget, ...)`, resolve their widget's look themselves. `AE2Look.of(context)` gives the look of the whole screen and is only for drawing code that has no widget at hand.

The plain look's colors and drawing rules are all in `PlainLook`: Minecraft's sunken slot box and creative-inventory scroll bar, MUI2's button, check box and icons, and the theme's own text style. The painters of the `ae2` package only draw the AE2 skins.

ModularUI 2 builds a GUI that the server opens (a machine, a held item) on both sides. `AE2Theme`, the drawing classes of the `ae2` package and every widget below load on both sides, so they can be used in such GUIs; they draw only on the client. `ServerSideLoadingTest` builds them in a class loader that behaves like a dedicated server. Theme registration (`AE2Theme.register`) is client-only and is done by GTNRLib.

The public skin and icon types are `AE2Theme.Variant` and `AE2Theme.Icon`.

## Quick Start

1. **Apply the theme to the screen** with `useTheme`: `AE2Theme.LIGHT_ID`, `DARK_ID` or `DARK_PURPLE_ID`, or the identifier a `ScreenTheme` setting stores (`ScreenTheme.themeId`). Every GTNRLib widget in the screen then takes that look; nothing is passed to the widgets.

   ```java
   new ModularScreen(MODID, panel).useTheme(AE2Theme.DARK_ID); // a client-only screen
   ```

   In a GUI the server opens (`IGuiHolder`), build the panel in `buildUI`, which runs on both sides, and apply the theme in `createScreen`, which runs on the client:

   ```java
   public ModularPanel buildUI(PosGuiData data, PanelSyncManager sync, UISettings settings) { ... }

   public ModularScreen createScreen(PosGuiData data, ModularPanel panel) {
       return new ModularScreen(MODID, panel).useTheme(AE2Theme.DARK_ID);
   }
   ```

   Do not use `IThemeApi.registerThemeForScreen` for this: MUI2 lets a registered screen theme win over every panel's own theme, so a dialog that sets a theme of its own would lose it.

2. **A dialog with a theme of its own** calls `themeOverride(id)` on its panel. Widgets inside it follow the dialog's theme, not the screen's, and dropdowns in it open their menus in the matching menu theme by themselves.

3. **Sync values in a GUI the server opens.** Widgets that take a MUI2 value (`SettingToggleButton.value`, `AECheckbox`, `ProgressBar.value`, `NumberEntryWidget.value(ILongValue)`) take a sync value directly. **Register it with the panel's sync manager first**: a widget does not do it for you, and an unregistered value only changes on the client, with no warning. Widgets that take a supplier and an action (`VerticalButtonBar.toggle`, list rows, `PageTabs`, `ValueStepper`) read the sync value's getter and write through its setter, which sends the change to the server. Widgets check bounds on the client only, so a server-side setter clamps the value again.

   ```java
   LongSyncValue speed = new LongSyncValue(() -> machine.speed, value -> machine.speed = Math.max(1, Math.min(64, value)));
   BooleanSyncValue auto = new BooleanSyncValue(() -> machine.auto, value -> machine.auto = value);
   sync.syncValue("speed", speed);
   sync.syncValue("auto", auto);
   panel.child(new NumberEntryWidget().bounds(1, 64).value(speed).size(64, 16))
       .child(new VerticalButtonBar()
           .add(new SettingToggleButton().value(auto)
               .title(IKey.str("Auto export"))
               .state(true, AE2SettingIcon.AUTO_EXPORT_ON, IKey.str("On"))
               .state(false, AE2SettingIcon.AUTO_EXPORT_OFF, IKey.str("Off")))
           .pos(-VerticalButtonBar.WIDTH + VerticalButtonBar.OVERLAP, 4));
   ```

4. **Sizes that depend on text**, such as `AE2CheckboxPainter.checkboxWidth(label)`, measure with the client's font, which a dedicated server does not have. In `buildUI` give them as a size supplier, which MUI2 only evaluates on the client: `.width(() -> AE2CheckboxPainter.checkboxWidth(label), Measure.PIXEL)`.

5. **Icons**: `AE2SettingIcon` has AE2 1.21's own setting icons (redstone, fuzzy, access, power units and more) under AE2's names; `Icon` has general actions (add, refresh, close, ...). `IconSource.of(texture)` and `IconSource.of(itemStack)` take anything else.

6. **See every widget at work**: type `/gtnrlib_widgets` in game. It opens a screen with all of these widgets, working, and buttons that switch it between the three AE2 skins and the default theme, plus a dialog in another theme.

### A Complete Machine Screen

A machine screen the server opens, with a toolbar setting, a radio group, a number entry and an upgrade panel. It looks like AE2 1.21 under the AE2 themes and like a vanilla screen in others, with no theme checks in the code.

```java
public class SorterGui implements IGuiHolder<PosGuiData> {

    @Override
    public ModularPanel buildUI(PosGuiData data, PanelSyncManager sync, UISettings settings) {
        SorterTile tile = (SorterTile) data.getTileEntity();
        BooleanSyncValue auto = new BooleanSyncValue(() -> tile.auto, value -> tile.auto = value);
        IntSyncValue mode = new IntSyncValue(() -> tile.mode, value -> tile.mode = value);
        LongSyncValue limit = new LongSyncValue(() -> tile.limit, value -> tile.limit = value);
        sync.syncValue("auto", auto);
        sync.syncValue("mode", mode);
        sync.syncValue("limit", limit);

        ModularPanel panel = ModularPanel.defaultPanel("sorter", 176, 166);
        return panel.child(AECheckbox.radio(() -> "Round robin", mode, 0).pos(8, 8))
            .child(AECheckbox.radio(() -> "Random", mode, 1).pos(8, 24))
            .child(new NumberEntryWidget().bounds(1, 64).value(limit).pos(8, 44).size(64, 16))
            .child(SlotGroupWidget.playerInventory(true))
            .child(new VerticalButtonBar()
                .add(new SettingToggleButton().value(auto)
                    .title(IKey.str("Auto export"))
                    .state(true, AE2SettingIcon.AUTO_EXPORT_ON, IKey.str("On"))
                    .state(false, AE2SettingIcon.AUTO_EXPORT_OFF, IKey.str("Off")))
                .pos(-VerticalButtonBar.WIDTH + VerticalButtonBar.OVERLAP, 4))
            .child(new UpgradesPanel()
                .slot(new ItemSlot().slot(new ModularSlot(tile.upgrades, 0)))
                .slot(new ItemSlot().slot(new ModularSlot(tile.upgrades, 1)))
                .pos(176 - AE2Theme.UPGRADE_OVERLAP, 4));
    }

    @Override
    public ModularScreen createScreen(PosGuiData data, ModularPanel panel) {
        return new ModularScreen(MODID, panel).useTheme(AE2Theme.LIGHT_ID);
    }
}
```

The toolbar and the upgrade panel are added after the panel's other children, so they draw over its borders. In a real screen the labels come from language keys (`IKey.lang`, `StatCollector.translateToLocal`), and the theme from a setting the player chooses (`ScreenTheme`).

## API Reference

| Type | Responsibility |
| --- | --- |
| [AE2Theme](gtnrlib/src/main/java/com/xyp/gtnrlib/gui/theme/AE2Theme.java) | Theme identifiers, AE2's widget dimensions, the skin texture folders, theme registration and lookup. Its drawing methods are deprecated forwarders to the classes below. |
| [Variant](gtnrlib/src/main/java/com/xyp/gtnrlib/gui/theme/AE2Theme.java) | The three AE2 skins (light, dark, purple dark) as data: their palette (`Colors`, built by name with `Colors.builder()`), selection colors and textures. The purple dark skin ships only the textures it recolours (`AE2Theme.PURPLE_TEXTURES`) and takes the rest from the dark skin. |
| [AE2ThemeDefinitions](gtnrlib/src/main/java/com/xyp/gtnrlib/gui/theme/ae2/AE2ThemeDefinitions.java) | The MUI2 panel and menu theme of each skin (`panelTheme(variant)`, `menuTheme(variant)`). Client only; `AE2Theme.register` uses it, and other code selects a theme by its identifier. |
| [Icon](gtnrlib/src/main/java/com/xyp/gtnrlib/gui/theme/AE2Theme.java) | The built-in toolbar icons, drawn from the AE2 skin's sheet or MUI2's icons outside AE2 themes; the three MUI2 has no equivalent for (`FLIP_VERTICAL`, `MOVE_TO_UP`, `MOVE_TO_DOWN`) use the light skin's sprite there, which is white like MUI2's. |
| [AE2Look](gtnrlib/src/main/java/com/xyp/gtnrlib/gui/theme/ae2/AE2Look.java) | The look a panel gives GTNRLib's AE2 widgets: one of the three AE2 skins or the plain look. `AE2Look.of(widget)` reads the theme of the widget's panel; every AE2 widget asks it while drawing. It answers colors and draws wells, selected rows, buttons, check boxes and scroll bars in either look. |
| [PlainLook](gtnrlib/src/main/java/com/xyp/gtnrlib/gui/theme/ae2/PlainLook.java) | Every color and drawing rule of the look outside AE2 themes, in one place. `AE2Look.plainLook()` hands it out (`null` under an AE2 skin). |
| [ae2 package](gtnrlib/src/main/java/com/xyp/gtnrlib/gui/theme/ae2) | The painters, one class per part: `AE2ButtonPainter` (text buttons, toolbar buttons, tabs and page tabs as drawables, button labels), `AE2CheckboxPainter` (check boxes and radio buttons as drawables, check-box labels, `CheckboxHover`), `AE2ScrollbarPainter` (AE2's two scroll bars), `AE2PanelPainter` (panel backgrounds, list wells and rows, the toolbar strip, tables, the dropdown caret), `AE2ListRowPainter` (what `IconRowList` rows and `ListRow` share: text position, check box, section header), `AE2UpgradePainter`, `AE2Legacy` (GTNH AE2's rv3 sheets) and `AE2Draw` (text and icon helpers that do not depend on the look). `IconSource` is what a toolbar button or tab shows. |
| [ScreenTheme](gtnrlib/src/main/java/com/xyp/gtnrlib/gui/theme/ScreenTheme.java) | Persistable selection of the default, light, dark or purple dark panel theme. |
| [HandleScrollData](gtnrlib/src/main/java/com/xyp/gtnrlib/gui/widget/HandleScrollData.java) | Vertical scrolling with a fixed-size handle and optional row snapping; a press on the track beside the handle moves the handle's centre to the pointer and keeps dragging, or pages like AE2 with `pageOnTrackPress(true)`. |
| [GTNRListWidget](gtnrlib/src/main/java/com/xyp/gtnrlib/gui/widget/GTNRListWidget.java) | List layout, initial scroll position and persistent scroll-bar integration; with paging turned on, keeps paging while a track press is held. |
| [Scrollbar](gtnrlib/src/main/java/com/xyp/gtnrlib/gui/widget/Scrollbar.java) | Stand-alone AE2 scroll bar over whole positions (usually rows) for widgets that draw their own content, like AE2's `Scrollbar`: the owner keeps the position, and the bar drags, jumps to a track press (or pages like AE2) and takes the wheel. |
| [IPersistentScrollbar](gtnrlib/src/main/java/com/xyp/gtnrlib/gui/widget/IPersistentScrollbar.java) | Extension contract for scroll data that retains an inactive track. |
| [NumberEntryWidget](gtnrlib/src/main/java/com/xyp/gtnrlib/gui/widget/NumberEntryWidget.java) | Bounded integer input of any size, expression parsing, step buttons or `<` `>` arrows, optional wheel stepping (`wheel(true)`), right-click reset and validation feedback; `available` gives it AE2's disabled look, and `textAlignment(Alignment.Center)` centres the number in its field. |
| [VerticalButtonBar](gtnrlib/src/main/java/com/xyp/gtnrlib/gui/widget/VerticalButtonBar.java) | AE2's vertical toolbar left of a panel: 18×20 icon buttons 22 pixels apart (18×18 squares 20 apart outside AE2 themes, where buttons have no shadow below them) on a strip that covers the panel's left border; on/off buttons show AE2's selected frame while on, `available` disables the button added last, and `buttonSize(width, height)` changes every button's size. |
| [PageTabs](gtnrlib/src/main/java/com/xyp/gtnrlib/gui/widget/PageTabs.java) | AE2's page tabs down a panel's right edge as one widget: the owner keeps the open page, `tab(icon, tooltip)` adds a tab, `page(index, widget)` shows a page only while its tab is open, `available` disables the tab added last, and `tabSize(width, height)` changes every tab's size. |
| [IconRowList](gtnrlib/src/main/java/com/xyp/gtnrlib/gui/widget/IconRowList.java) | Fast drawn list rows with icons, details (optionally in their own color), dropdowns, check boxes and `NumberEntryWidget` fields; AE2 inset, highlights and scroll bar under AE2 themes. Rows are made with the `Row` factories (`header`, `item`, `colored`, `checkbox`, `dropdown`, `number`) and changed with `withAvailable`, `withDetailColor`, `onRightClick` and `withKey`, which return copies. The list knows which dropdown is open by its text and order, or by the row's `withKey` key: give dropdown rows a key when their texts or order can change while one is open. `Row.withAvailable(false)` draws a row muted and ignores its clicks. Its scroll bar is a `Scrollbar`: one row per wheel notch, and a track press moves the handle to the pointer. `rowHeight` and `numberWidth` change the row height (18 by default) and the number fields' width (64), and `numberWheel(true)` lets the wheel step the number fields. Put a `ValueStepper` in a `ListRow` for a stepper row. |
| [ListRow](gtnrlib/src/main/java/com/xyp/gtnrlib/gui/widget/ListRow.java) | A widget row drawn like an `IconRowList` row: label, then a control widget (`control`) or check box (`checkbox`) against the right end; clickable rows highlight, and `available` mutes the row. It is 18 pixels high unless its height is set; the label, check box and control stay centred in any height. For lists that mix MUI2 controls with rows. A control that implements `IListRowControl`, as `ValueStepper` does, is told that it sits in a row and takes the list's text colors. |
| [AE2Button](gtnrlib/src/main/java/com/xyp/gtnrlib/gui/widget/AE2Button.java), [IconButton](gtnrlib/src/main/java/com/xyp/gtnrlib/gui/widget/IconButton.java), [TabButton](gtnrlib/src/main/java/com/xyp/gtnrlib/gui/widget/TabButton.java) | AE2's text button, 18×20 toolbar icon button (an 18×18 square outside AE2 themes) and 20×20 framed tab button, with a left-click action and an optional available state; `AE2Button` and `IconButton` also show a selected state, while `TabButton`, like AE2's, has none. They share [ThemedButton](gtnrlib/src/main/java/com/xyp/gtnrlib/gui/widget/ThemedButton.java), which on its own (`new ThemedButton<>()`) is an invisible click area; an unavailable button swallows every mouse button. `ThemedButton` is typed by its subclass, so `available`, `selected` and `action` return the button's own type and a subclass can add chained methods. |
| [SettingToggleButton](gtnrlib/src/main/java/com/xyp/gtnrlib/gui/widget/SettingToggleButton.java) | AE2's setting button on MUI2's cycle button: a toolbar button (18×20, or an 18×18 square outside AE2 themes) with one icon and tooltip description per value; left click steps forward, right click back. With a boolean value it is AE2's two-icon toggle. Takes an integer, enum, boolean or synced value; `available` disables it. |
| [ProgressBar](gtnrlib/src/main/java/com/xyp/gtnrlib/gui/widget/ProgressBar.java) | AE2's 6×18 inscriber progress bar on MUI2's `ProgressWidget`, filling upwards in whole pixels; outside AE2 themes a slot-style well that fills with green. Takes a 0–1 or synced value. |
| [ValueStepper](gtnrlib/src/main/java/com/xyp/gtnrlib/gui/widget/ValueStepper.java) | `<` value `>` stepper over named values such as voltage tiers; `wheel(true)` lets the mouse wheel step it; `available` greys it out. |
| [CycleDropdown](gtnrlib/src/main/java/com/xyp/gtnrlib/gui/widget/CycleDropdown.java) | Dropdown that keeps its menu inside the panel (`menuWidth`); `iconOption` builds an item-icon option row with a scrolling name, placed by an optional `Alignment` (left by default) and keeping formatting codes such as GT's tier colors. Under AE2 themes the shown value and its caret use the button text colors and sink on hover like a button label. `available` draws it as a disabled button and closes its menu; `wheel(true)` cycles the choices with the mouse wheel. |
| [SlideSwitch](gtnrlib/src/main/java/com/xyp/gtnrlib/gui/widget/SlideSwitch.java) | Theme-driven on/off switch with a label; AE2 themes give it AE2's check box sprites, centred on its height, with the label wrapped and placed as in AE2's check boxes and AE2's hover rule after a click. `available` fades it; `glowWhen` only chooses whether an on switch glows. |
| [StepSlider](gtnrlib/src/main/java/com/xyp/gtnrlib/gui/widget/StepSlider.java) | Slider over a fixed number of steps with ticks, milestones and a value tag in a small built-in font (digits, letters shown as capitals, space and `% . - + × / : ( ) = < > ! ?`). Under AE2 themes it uses the slider skins, also when vertical; `available` greys it out, and under AE2 themes darkens its skins; `wheel(true)` lets the mouse wheel move it. |
| [AECheckbox](gtnrlib/src/main/java/com/xyp/gtnrlib/gui/widget/AECheckbox.java) | AE2's check box with its label, or with `AECheckbox.radio(label, value, option)` one radio button of a group: radio buttons sharing one integer value select one option at a time. Takes MUI2 values, synced ones included; `available` disables it. |
| [UpgradesPanel](gtnrlib/src/main/java/com/xyp/gtnrlib/gui/widget/UpgradesPanel.java) | AE2's upgrade panel as a widget: give it the slots, place it at `panelRight - AE2Theme.UPGRADE_OVERLAP`, and it lays them out in columns of eight (`perColumn`), sizes itself and draws AE2's panel, or the theme's panel background in other themes. |
| [GhostItemSlot](gtnrlib/src/main/java/com/xyp/gtnrlib/gui/widget/GhostItemSlot.java) | Item slot for client-only screens that takes an item dragged from NEI, with an optional filter and change callback; it lights up under the pointer, and with `clearable(true)` a right-click empties it and runs `onClear`. |

Every widget that can be unavailable takes `available(BooleanSupplier)`: `AE2Button`, `IconButton`, `TabButton`, `AECheckbox`, `SettingToggleButton`, `NumberEntryWidget`, `ListRow`, `ValueStepper`, `CycleDropdown`, `SlideSwitch`, `StepSlider`, and the last button or tab added to a `VerticalButtonBar` or `PageTabs`; an `IconRowList` row uses `Row.withAvailable(false)`. An unavailable widget looks disabled (AE2's disabled sprites, a darkened skin, a faded icon or muted text), shows no hover, swallows clicks so they never reach what lies below (a `ListRow` does when it has a click action), and leaves the mouse wheel to the widgets around it. The click rule is written once, in `Availability`, which each of these widgets keeps: while unavailable a press ends there and runs nothing. A widget of your own gets the same behaviour by routing `onMousePressed` through `Availability.press`. `SlideSwitch.availableWhen`, `SlideSwitch.liveWhen` and `StepSlider.activeWhen` are the older names and still work.

Two mechanisms decide how a control looks, and each control uses one of them:

| Mechanism | Controls | How a theme changes them |
| --- | --- | --- |
| The look of the panel (`AE2Look`): an AE2 skin or the plain look | `AE2Button`, `IconButton`, `TabButton`, `AECheckbox`, `UpgradesPanel`, `SettingToggleButton`, `PageTabs`, `VerticalButtonBar`, `NumberEntryWidget`, `IconRowList`, `ListRow`, `ValueStepper`, `CycleDropdown`, `Scrollbar`, `HandleScrollData`, `ProgressBar`, and the drawables of the painters | A theme based on an AE2 theme, or one that names a skin in its `gtnrlibAE2Look` entry, gets that skin; every other theme gets the plain look in its own text and button styles. |
| Theme data of the control itself | `CloseButton` (the theme's close-button style; its height and cross follow the look), `SlideSwitch` (`SlideSwitch.Theme`), `StepSlider` (`StepSlider.Theme`), `FillBar` (`FillBar.Theme`), `Divider` (`Divider.Theme`), sizes and gaps (`LayoutTheme`), and MUI2's own widgets | Any theme can set their colors, skins and sprites; the AE2 themes fill them with AE2's. |

The two are separate on purpose and do not read each other. One consequence: outside AE2 themes an on/off value is a sliding switch in `SlideSwitch` and MUI2's check box in list rows and `AE2CheckboxPainter.checkbox`; under AE2 themes both show AE2's check box sprite.

Toolbar buttons, tabs and page tabs show an `IconSource`: one of the built-in `Icon`s or `AE2SettingIcon`s, which follow the AE2 skin, or `IconSource.of(texture)` for a mod's own 16×16 texture and `IconSource.of(itemStack)` for an item. Built-in icons and textures fade on an unavailable button; items do not.

Every widget takes a custom size, and AE2's sizes are the defaults. Frames and fills stretch while their borders keep their width: buttons, icon buttons, tabs (`AE2ButtonPainter.tabButton` and `pageTab` draw at the widget's size), panels, insets, list rows and the upgrade panel. Sprites that AE2 never stretches keep their size and stay centred or anchored: check boxes, radio buttons, scroll-bar handles and 16×16 icons. Widgets made of several parts take their part sizes through their own methods: `VerticalButtonBar.buttonSize`, `PageTabs.tabSize`, `IconRowList.rowHeight` and `numberWidth`, and the `perColumn` overloads of `AE2UpgradePainter.drawUpgradePanel`, `upgradePanelWidth`, `upgradePanelHeight`, `upgradeSlotX` and `upgradeSlotY` (AE2 fills columns of `UPGRADE_SLOTS_PER_COLUMN`, 8).

AE2 draws its toolbar strip after the panel, anchored three pixels inside the panel's left edge, so the strip's right two pixels hide the panel border and the two read as one piece. Place `VerticalButtonBar` at `panelLeft - WIDTH + OVERLAP` and add it after the panel's other children. A panel that also holds the bar starts its background `WIDTH - OVERLAP` pixels right of its own left edge; `AE2PanelPainter.panelBackground(owner, left, right)` draws the panel texture of the owner's panel theme between such insets.

Pages are switched with AE2's page tabs (`PageTabs`, or `AE2ButtonPainter.pageTab` to draw one on a button of your own), the mode tabs of the pattern encoding terminal: 22×22 tabs stacked `PAGE_TAB_PITCH` apart on the panel's right edge, starting `PAGE_TAB_INSET` pixels inside it and added after the panel. The selected tab covers the panel border and joins the panel; the others stand out. Toolbar buttons (`AE2ButtonPainter.iconButton`) are actions, not pages; their `selected` overload marks a toggled action.

AE2 lists keep their scroll bar beside the list inset, on the panel background, with eight pixels from the inset edge to the groove (as in AE2's network status and crafting CPU screens); the groove is never drawn inside the inset. `IconRowList` reserves `AE2_SCROLL_COLUMN` for it, so align controls above a list to `width - AE2_SCROLL_COLUMN` under AE2 themes. `AE2PanelPainter.well(owner, width)` draws such an inset for a page of your own widgets (`AE2Look.drawWell` for code that draws directly). The drawables `AE2PanelPainter.well`, `AE2PanelPainter.listRow` and `AE2PanelPainter.panelBackground` take the widget they belong to and follow the theme of its panel, like every other AE2 drawable. Check boxes in list rows and `AE2CheckboxPainter.checkbox` share `AE2Look.drawCheckbox`: AE2's check box sprite under AE2 themes, MUI2's check box otherwise. `ListRow`s as wide as the inset put their text and controls where `IconRowList`'s own rows put them, so pages built from either line up. A scrolling list of `ListRow`s placed one pixel inside its inset, top and bottom, clips its rows inside the inset border while `HandleScrollData` still draws the groove over the inset's full height.

`NumberEntryWidget`, `CycleDropdown`, `ValueStepper` and `StepSlider` ignore the mouse wheel by default, so the wheel scrolls a list around them even while the pointer is over one, and scrolling a page never changes a value by accident. `wheel(true)` lets one step its value while the pointer is over it (for `NumberEntryWidget`, over its text field), also inside a scrolling list; anywhere else in the list the wheel still scrolls the list. `IconRowList.numberWheel(true)` does the same for the number fields of its rows. Lists and scroll bars always take the wheel. MUI2 offers the wheel to a text field with keyboard focus first, wherever the pointer is; `NumberEntryWidget` passes it on unless the pointer is over the field, so clicking into a number field and moving away never turns the wheel into a hidden value change.

AE2 buttons are 20 pixels high, and at that height `AE2ButtonDrawable` matches AE2 row for row. Lower buttons, such as the 16-pixel arrows of a compact `NumberEntryWidget` or a `ValueStepper`, follow a GTNRLib rule rather than AE2's, on purpose. The normal shadow is `height - 13` pixels, clamped to 1–3: 3 from 16 pixels up, 2 at 15 and 1 at 14 and below. A hovered button's shadow is one pixel thinner and a disabled button's two, never below zero. The face sinks by the pixels the shadow lost, and the label stays centred on the face. AE2 1.21 itself nine-slices its button sprite with a 3-pixel border. Below 20 pixels that leaves a 2-pixel shadow, and from 18 pixels down the face also loses its bottom highlight row, so a 16-pixel GTNRLib button has a thicker shadow than AE2's. Above 20 pixels GTNRLib stretches the face, while AE2 repeats a light and dark stripe across the middle. `AE2ButtonDrawable.shadow(state, height)` and `textY(state, height)` return the shadow and the label offset for custom drawing.

`AE2ButtonPainter.label` places button text as AE2 does: normal, hovered and disabled labels move with the face, and under AE2 themes a label wider than the button scrolls back and forth inside it. A scrolling label does not move with the face: as in AE2, it stays at the hovered label's height in every state, so on a normal button it sits one pixel lower than a centred label, and on a disabled one one pixel higher. MUI2's own buttons (close buttons, toggle buttons, dialog buttons) get the AE2 button background from the theme, but MUI2 places their text itself: centred, one pixel lower than AE2's normal label and without sinking on hover. Use `AE2Button` or `AE2ButtonPainter.button` with `AE2ButtonPainter.label` where the text should behave like AE2's.

The on/off box is called a check box throughout, after AE2's `AECheckbox`; AE2 draws it as a 22×12 sprite. "Switch" is kept for the `SlideSwitch` widget, and "toggle" for buttons that switch a setting (`SettingToggleButton`, `VerticalButtonBar.toggle`). Check box and radio labels wrap at the widget's width as in AE2's `AECheckbox` (`AE2CheckboxPainter.checkboxLabel`): one line starts four pixels below the sprite's top, several lines start one pixel below it. After a check box flips under the pointer it shows its plain sprite until the pointer leaves, as AE2's does (`AE2CheckboxPainter.CheckboxHover`); radio buttons keep their hover look. AE2 wraps at the widget width minus 22, which lets a check box label run four pixels past the widget; GTNRLib wraps inside the widget.

AE2's toolbar icon buttons are 16×16 widgets inside a drawn 18×20 frame. `IconButton` and the `VerticalButtonBar` buttons take the mouse on the whole 18×20 frame, on purpose. AE2's text fields draw their text two pixels from the background's edge; GTNRLib's text fields, `NumberEntryWidget` included, keep MUI2's four pixels.

AE2 attaches the upgrade panel to the right of a screen's panel, `UPGRADE_OVERLAP` pixels inside its right edge: the upgrade panel has no left border, so it covers the main panel's border and the two read as one piece. `UpgradesPanel` is that panel as a widget: add the slots with `slot(...)`, place it at `panelRight - UPGRADE_OVERLAP` after the panel's other children, and it lays the slots out, sizes itself and draws its background. As in AE2, a slot turned off with `setEnabled(false)` or `setEnabledIf` leaves no gap, and with no slot shown the panel takes no space and NEI is not asked to keep clear. When the last column is not full, AE2's panel ends below its last slot there, so it steps in at the bottom right. Other themes draw the theme's panel background without its left border, joined the same way, as a full rectangle with the slots' own frames. For drawing of your own, `AE2UpgradePainter.drawUpgradePanel` draws AE2's panel, with a light frame around a block of 16×16 slot wells 18 pixels apart; put 18×18 item slots at `upgradeSlotX(i)`, `upgradeSlotY(i)` with `disableThemeBackground(true)` and `disableHoverThemeBackground(true)`, so their theme frame does not cover the wells, also under the mouse.

`VerticalButtonBar`, `PageTabs` and `UpgradesPanel` stand outside their panel and ask NEI to keep clear of them, so they belong in ordinary screens: MUI2 has no NEI settings for overlay screens and throws when such a widget opens in one.

MUI2's `ListWidget` and `ScrollWidget` darken their top and bottom edges while there is more to scroll; AE2's lists do not. `GTNRListWidget` and the menus of `CycleDropdown` turn this off; for a plain MUI2 list in an AE2-style screen, call `showScrollShadows(false)`.

AE2's scroll bars also take the mouse wheel anywhere on the screen. MUI2 offers the wheel to screen-level listeners before any widget and lets none of them keep it, so GTNRLib does not do this by default; a screen that wants it calls `Scrollbar.scroll(direction)` from its panel's `onMouseScroll` when `super.onMouseScroll` returns false.

The small scroll bar (`HandleScrollData(true)`) draws the groove AE2 uses for its small scroll bar in the processing pattern panel: a one-pixel line-colored outline around a raised fill. Scroll-bar handles never stretch in AE2; MUI2 scroll widgets that do not use `HandleScrollData` stretch the theme's small handle to their own length.

A background set with `background(...)` on a MUI2 `ItemSlot` disappears while the slot holds an item (`ItemSlot.getCurrentBackground` in MUI2 2.3.94). The theme's slot frame is drawn separately and stays, so leave slot frames to the theme or draw them in the panel background.

## Widget Catalog

What each widget is and how it looks at a glance; the sections above give the details. "AE2 counterpart" names the AE2 1.21 widget a widget copies, or says that GTNRLib built it. "Other themes" is the plain look of every theme that is not an AE2 theme. "Wheel" says whether the mouse wheel over the widget changes something: always, only after `wheel(true)`, or never.

| Widget | AE2 counterpart | Under AE2 themes | Other themes | Default size | `available` | Wheel |
| --- | --- | --- | --- | --- | --- | --- |
| `AE2Button` | `AE2Button` | AE2's button; the label sinks with the face and scrolls when too long | The theme's MUI2 button, label centred and trimmed | Set by the caller (AE2's are 20 high) | Yes | Never |
| `IconButton` | `IconButton` | AE2's 18×20 toolbar frame, sinking on hover; selected frame | The theme's MUI2 button with the icon | 18×20; 18×18 in other themes | Yes | Never |
| `TabButton` | `TabButton` (BOX style) | AE2's framed tab | The theme's MUI2 button with the icon | 20×20 | Yes | Never |
| `SettingToggleButton` | `SettingToggleButton`, `ToggleButton` | As `IconButton`, one icon per value; tooltip with title and value | As `IconButton` | 18×20; 18×18 in other themes | Yes | Never |
| `PageTabs` | `TabButton` (HORIZONTAL style) | AE2's page tabs; the open one joins the panel | The theme's MUI2 buttons with the icons | 22×22 per tab | Last tab added | Never |
| `VerticalButtonBar` | `VerticalButtonBar` | AE2's toolbar strip with `IconButton`s | The panel background with plain icon buttons | 18×20 per button, 22 apart; 18×18, 20 apart in other themes | Last button added | Never |
| `ProgressBar` | `ProgressBar` (inscriber) | AE2's empty and full sprites, filling upwards | A sunken slot well filling with green | 6×18 | No | Never |
| `NumberEntryWidget` | `NumberEntryWidget` | AE2's step buttons, field, error color and warning icon | The theme's buttons and text field, red error text, the light skin's warning icon | 138×62; below 62 high one row of `<` field `>` | Yes | After `wheel(true)` |
| `Scrollbar` | `Scrollbar` | AE2's groove and fixed 12×15 handle, or the 7-wide small one | Minecraft's creative-inventory scroll bar, 14 wide; with `small(true)` a 7-wide bar with Minecraft's handle narrowed to 5; or a thin bar after `plainBar` | Height set by the caller; width follows the look unless set | No | Always |
| `HandleScrollData` | `Scrollbar` | As `Scrollbar`, for MUI2 lists | As `Scrollbar`: 14 wide, or 7 wide for `HandleScrollData(true)` | Thickness follows the look | No | Always |
| `IconRowList` | GTNRLib | AE2's inset, rows, highlights and scroll bar | A sunken slot well, white text with shadow, a thin grey scroll bar | Set by the caller; rows 18 high | Per row (`withAvailable`) | Always (scrolls); number fields after `numberWheel(true)` |
| `ListRow` | GTNRLib | Drawn like an `IconRowList` row | Drawn like a plain `IconRowList` row | 18 high | Yes | Never |
| `ValueStepper` | GTNRLib | AE2 arrow buttons around the value | The theme's arrow buttons | Set by the caller | Yes | After `wheel(true)` |
| `CycleDropdown` | GTNRLib (on MUI2's dropdown) | AE2 button with a caret; label sinks on hover | The theme's MUI2 button with a caret | Set by the caller | Yes | After `wheel(true)` |
| Check boxes (`AE2CheckboxPainter.checkbox`, list rows) | `AECheckbox` | AE2's 22×12 check box sprite | MUI2's check box | 22×12 sprite, 12×12 plain | Yes | Never |
| `SlideSwitch` | `AECheckbox` | AE2's check box sprites from its theme data | A sliding switch in the theme's colors | Set by the caller | Yes | Never |
| `StepSlider` | GTNRLib | AE2's slider skins from its theme data | The slider in the theme's colors | Set by the caller | Yes | After `wheel(true)` |
| `AECheckbox` | `AECheckbox` | AE2's 22×12 check box or 14×14 radio sprite with its label | MUI2's check box with the theme's text | Box height, one line of label wide | Yes | Never |
| `UpgradesPanel` | `UpgradesPanel` | AE2's upgrade panel joined to the panel, with its slot wells | The theme's panel background without its left border, joined the same way, with the slots' frames | Follows the slots | No | Never |
| `FillBar`, `Divider` | GTNRLib | AE2 colors from their theme data | The theme's colors | Set by the caller; `Divider` 2 high | No | Never |

## Differences From AE2

These differ from AE2 1.21 on purpose; the paragraphs above give the details.

- AE2 1.21 has no close button: its sub-screens and dialogs return with a tab button showing the back arrow, five pixels above the panel's top edge and four in from its right edge (`TabButton.back(action)`, `TabButton.BACK_TOP`, `BACK_RIGHT`). GTNRLib also keeps MUI2's `CloseButton` for panels that close in place; it shows AE2's 4×4 cross in the theme's close-button style, 12×13 pixels under AE2 themes so the cross sits in the middle of the face above the button's shadow, and 12×12 in others.
- AE2 lights a tab button only while it has keyboard focus; 1.7.10 buttons get no focus, so `TabButton` lights up under the mouse instead, at the same size.
- Buttons lower than 20 pixels use GTNRLib's shadow rule, and taller ones stretch the face.
- A press on a scroll bar's track moves the handle's centre to the pointer and keeps dragging; AE2 pages. `pageOnTrackPress(true)` restores AE2's paging.
- Toolbar icon buttons take the mouse on their whole 18×20 frame; AE2's only on the 16×16 icon.
- Text fields keep MUI2's four-pixel text inset; AE2's is two pixels.
- Value controls ignore the wheel unless `wheel(true)` is set; with it they step while the pointer is over them, also in a scrolling list, where AE2 has no such controls.
- The wheel does not scroll a list from anywhere on the screen by default.
- Check box and radio labels wrap inside the widget; AE2's check box labels can run four pixels past it.
- Unavailable toolbar buttons, tabs and page tabs fade their built-in or texture icon (item icons do not fade), and outside AE2 themes also take MUI2's disabled button; AE2's full-size icon buttons keep their icon and are usually hidden instead.
- The built-in `Icon`s are MUI2's icon set redrawn for each skin; AE2's own setting icons are `AE2SettingIcon`.
- `NumberEntryWidget` has no confirm action on Enter; AE2's amount screens confirm with it.
- `VerticalButtonBar.toggle` shows the selected frame while on and keeps its icon; AE2's `ToggleButton` switches between an on and an off icon.
- `NumberEntryWidget` arrows and wheel step by 10 while Shift is held, and a step from invalid input starts at zero; AE2's wheel steps by one and only on valid input.
- `IconRowList` and clickable `ListRow` rows highlight under the pointer; AE2's crafting CPU rows do not.
- `UpgradesPanel` shows no tooltip of the upgrades a machine accepts, since GTNRLib does not know them; AE2's panel lists them. Give it one with MUI2's `tooltipBuilder`.
- When the upgrade panel's last column is not full, NEI keeps the panel's whole rectangle clear; AE2 frees the corner below the short column.

AE2 1.21 has no dropdown, stepper, slider, fill bar, divider, close button, breadcrumb or toast. `CycleDropdown`, `ValueStepper`, the single-row `NumberEntryWidget`, `StepSlider`, `FillBar`, `Divider`, `CloseButton`, `Breadcrumb`, `FadingNotice`, `IconRowList`, `ListRow` and `GhostItemSlot` are GTNRLib's own controls, built from AE2's parts and colors where the AE2 themes apply.

Two things cannot match AE2: Minecraft 1.7.10 has no keyboard focus for buttons, so AE2's focus look has no counterpart, and MUI2's own buttons place their text themselves.

## Theme Selection

| Selection | Panel theme | Menu theme |
| --- | --- | --- |
| `ScreenTheme.DEFAULT` | Screen default (`themeId == null`) | `modularui.context_menu` |
| `ScreenTheme.AE2_LIGHT` | `gtnrlib:ae2_light` | `gtnrlib:ae2_light_menu` |
| `ScreenTheme.AE2_DARK` | `gtnrlib:ae2_dark` | `gtnrlib:ae2_dark_menu` |
| `ScreenTheme.AE2_DARK_PURPLE` | `gtnrlib:ae2_dark_purple` | `gtnrlib:ae2_dark_purple_menu` |

GTNRLib registers these themes during client initialization. Apply the selected identifier with `ModularScreen.useTheme` (see Quick Start), not with `IThemeApi.registerThemeForScreen`, which would override the theme of every dialog in the screen. `CycleDropdown` menus pick the menu theme of their panel's look by themselves; `AE2Theme.useMenuTheme` maps other named menu panels, such as MUI2's own context menus, to the matching menu theme.

### Themes Of Your Own

There are three AE2 skins and no way yet to add a fourth: the skins are the `Variant` enum, and `AE2Look` only knows those three. What a theme of your own can change depends on the mechanism a widget uses (see the table above):

- A MUI2 theme JSON whose parent is an AE2 theme keeps that skin for every widget that follows `AE2Look` (buttons, icon buttons, lists, check boxes, scroll bars, number entries, dropdowns). Colors set in the JSON do not reach them; they read the skin's palette (`Variant.colors`).
- The same JSON can recolor MUI2's own widgets and the theme-data widgets (`SlideSwitch`, `StepSlider`, `FillBar`, `Divider`, `LayoutTheme`). `PaletteThemes.register` builds such theme data from one palette.
- A theme can name a skin in its `gtnrlibAE2Look` entry, `{"gtnrlibAE2Look": {"skin": "ae2_dark"}}`, with one of the folder names `ae2_light`, `ae2_dark` or `ae2_dark_purple`. An unknown name gives the plain look and logs a warning.

The purple dark theme shares the dark palette and replaces AE2's light-blue selection with fluix purple. Custom selection drawing should read `Variant.highlight`, `highlightTop`, `highlightShadow` and `highlightText` instead of the `AE2Theme.HIGHLIGHT` constants, so it matches every variant.

## Usage Examples

### Row-Based List

```java
import com.cleanroommc.modularui.api.widget.IWidget;
import com.cleanroommc.modularui.widgets.ListWidget;
import com.xyp.gtnrlib.gui.widget.GTNRListWidget;
import com.xyp.gtnrlib.gui.widget.HandleScrollData;

HandleScrollData scrolling = new HandleScrollData().rows(22);
ListWidget<IWidget, ?> list = new GTNRListWidget<>().scrollDirection(scrolling)
    .size(180, 110);
```

`GTNRListWidget` hands `HandleScrollData` the look of its panel before every layout, so the handle and the track width (12 pixels for AE2, 14 for vanilla) follow the panel's theme; another scroll widget calls `look(AE2Look.of(widget))` itself. The older constructors that take an `Variant` are deprecated and still work: that skin is used until a `GTNRListWidget` hands over its panel's look. `GTNRListWidget` reserves the scroll-bar width and draws its disabled handle when the content fits. Row snapping includes both scroll endpoints. See `HandleScrollData.rows(int)` for layout alignment requirements. Dragging the handle is anchored to the offset at the press, so holding the handle still never moves long lists. A press on the track beside the handle moves the handle's centre to the pointer at once, and the same press keeps dragging from there. This is a deliberate GTNRLib choice: AE2 pages instead, and `pageOnTrackPress(true)` gives AE2's behaviour, moving one wheel step towards the pointer (one row after `rows(int)`) and, while the button is held, again after 250 ms and then every 150 ms. AE2's scroll bars move by a page size each screen chooses, for the wheel and the track alike: one row in the network status and crafting screens, a sixth of the rows in terminals and a third in the crafting CPU list; for a larger step call `setScrollSpeed(rows * rowHeight)` after `rows(int)`. Only the left button scrolls a `GTNRListWidget`'s scroll bar.

### Stand-alone Scroll Bar

```java
import com.xyp.gtnrlib.gui.widget.Scrollbar;

Scrollbar bar = new Scrollbar(() -> firstRow, () -> rowCount - visibleRows, row -> firstRow = row)
    .height(visibleRows * 18 + 2); // the width follows the panel's look: 12 under AE2 themes, 14 in others
```

Layout can depend on the look too, since AE2's and the plain look's parts differ in size. Widgets that size themselves (`Scrollbar` without a width, `IconButton`, `SettingToggleButton`, `VerticalButtonBar`, `HandleScrollData`) read their panel's look while the panel is laid out. For sizes of your own, do the same with a size supplier, for example `.width(() -> AE2Look.of(widget).scrollbarWidth(), Measure.PIXEL)`, instead of deciding from the theme identifier while the GUI is built.

`Scrollbar` is AE2's scroll bar for content a widget draws or places itself, such as a grid, a table or a list of drawn rows. It keeps no position of its own: it reads the first visible row and the last reachable one from the owner and passes every new row back. The wheel over the bar moves `step(int)` rows (default 1). A press beside the handle moves the handle's centre to the pointer and keeps dragging; `pageOnTrackPress(true)` makes it page `step(int)` rows towards the pointer and repeat while held, like AE2. Forward the wheel over the content with `bar.scroll(direction)`. Under AE2 themes it draws AE2's groove and 12×15 handle (`small(true)` for the 7×15 one, `AE2Theme.SMALL_SCROLLER_WIDTH` wide); other themes get the vanilla scroll bar, 14 pixels wide (7 with `small(true)`), or a thin bar with `plainBar(color, visibleRows)`, whose width the caller sets. The bar needs `AE2Theme.SCROLLER_HEIGHT + 2` pixels of height at least.

### Integer Input

```java
import java.util.OptionalLong;

import com.xyp.gtnrlib.gui.widget.NumberEntryWidget;

NumberEntryWidget amount = new NumberEntryWidget().bounds(1, 1_000_000)
    .value(64)
    .available(() -> canEdit);
OptionalLong requested = amount.value();
```

The entry follows the theme it is drawn with, validation feedback included: invalid text is AE2's input error color under AE2 themes and a plain red otherwise. While `available` reports false the entry looks like AE2's disabled amount entry: disabled buttons, the dark uneditable field background and grey text; input is ignored and the field gives up keyboard focus. Invalid input turns the text red and lists the problems in the field's tooltip; the tall layout also shows AE2's 16×16 warning icon to the right of the field (its tooltip area is the top-left 8×8 pixels, as in AE2). `value()` returns an empty result for invalid input. `follow(LongSupplier)` keeps the field showing a value that other controls can change too, except while the player is typing in it. `isInputValid()` checks the input; inherited `isValid()` reports the MUI2 widget lifecycle. Callback semantics are defined by `NumberEntryWidget.onChange(LongConsumer)`.

The layout follows the size: from 62 pixels high it shows the AE2 step buttons stretched to the width, and lower entries are one row of `<`, the text field and `>`, such as `.size(64, 16)`. The arrows step by 1, or 10 with Shift, and so does the mouse wheel over the field after `wheel(true)`. The wheel steps only while the pointer is over the text field, even after the player has clicked into it, as in AE2. Right-clicking the field applies `resetValue(long)` (default 0) clamped to the bounds.

## Texture Attribution

Texture origins, transformations and licensing are specified in [AE2-gui-theme-NOTICE.txt](licenses/gtnrlib/AE2-gui-theme-NOTICE.txt).

## GTNR integration and verification

GTNR's client screen-theme configuration accepts `AE2_DARK_PURPLE` for the wireless frequency card, wireless power card, wildcard pattern, advanced IO bus and Useless Tools. Existing defaults remain unchanged.

Run `./gradlew.bat :gtnrlib:checkLibraryContracts` for drag anchoring, endpoint, row snapping and theme JSON reload regression checks. Run `./gradlew.bat -I gtnrlib/scripts/ae2-theme-qa.init.gradle :gtnrlib:runClient25 --no-configuration-cache` for isolated client checks. The fixture uses a transparent, click-through, silent background window and verifies all four screen themes, thousand-row dragging, empty lists, long-number bounds, callbacks and dropdown hover/selection. Screenshots and results are written to `gtnrlib/build/ae2-theme-qa/results`. It drives in-game widget APIs rather than physical input.
