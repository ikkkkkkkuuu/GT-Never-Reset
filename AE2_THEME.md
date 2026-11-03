# AE2 theme

GTNLLib ships a ModularUI 2 theme that copies the look of Applied Energistics 2 for Minecraft 1.21, in a light and a dark variant. Everything below is client-only: `AE2Theme`, `HandleScrollData` and `NumberEntry` are `@SideOnly(Side.CLIENT)`, so build them only where client classes may load (client screens, or code that never runs on a dedicated server).

## Themes and variants

| Theme id | Used for |
| --- | --- |
| `gtnllib:ae2_light` / `gtnllib:ae2_dark` | screens |
| `gtnllib:ae2_light_menu` / `gtnllib:ae2_dark_menu` | dropdown menus opened from those screens |

`AE2Theme.register()` and `AE2Theme.Reloader` are installed by `GTNLLibClient`; mods only pick a theme.

- `ScreenTheme` (`DEFAULT`, `AE2_LIGHT`, `AE2_DARK`) is the enum to put in a config option; `themeId` is `null` for the MUI2 default theme.
- `AE2Theme.variant(themeId)` turns a theme id (screen or menu) into a `Variant`, or `null` when it is not an AE2 theme. `AE2Theme.active(context)` does the same for the theme a widget is drawn with.
- `AE2Theme.useMenuTheme(themeId, owner, screen, menuPanelNames...)` maps the dropdown panels of one screen to the matching menu theme; without it MUI2 always uses `modularui.context_menu`.
- `Variant.colors` (`AE2Theme.Colors`) holds the palette: `text`, `muted`, `secondary`, `buttonText`, `hoverText`, `disabledText`, `fieldText`, `placeholder`, `line`, `edge`, `panel`, `well`, `outline`, `raised`, `good`, `bad`, `info`, `warning`, `error`. `AE2Theme.HIGHLIGHT` / `HIGHLIGHT_SHADOW` are the selection blues, the same in both variants, so text drawn on them should use the light palette.

## Styled automatically

Setting the panel theme is enough for: panel backgrounds, text fields, item and fluid slots, MUI2 sliders (handles via `AE2Theme.sliderHandles(root, themeId)`), `SlideSwitch`, and dropdown menus (with `useMenuTheme`). MUI2's own scroll bars only get the small scroller stretched to the bar length; use `HandleScrollData` for the AE2 behaviour.

## Drawables for widgets

These return an `IDrawable` that checks the theme at draw time: AE2 look under an AE2 theme, the current theme's look otherwise.

| Method | AE2 1.21 original | Notes |
| --- | --- | --- |
| `button(widget, available, selected)` | `AE2Button` | background; pair it with `label(...)` as overlay |
| `label(widget, text, available, selected)` | button text | centred, AE2 text colours and sink on hover |
| `buttonText(context, state, height)` | | colour, y offset and shadow for custom-drawn button text |
| `iconButton(widget, icon, available)` | `IconButton` | 18x20 toolbar button, `AE2Theme.Icon` picks the sprite |
| `tabButton(widget, icon, available)` | `TabButton` | 20x20, 22x22 when hovered |
| `toggleSwitch(widget, on, label, available)` | `AECheckbox` | 22x12 switch with label at +26; `switchWidth(label)` |
| `radio(widget, selected, label, available)` | `AECheckbox` radio | 14x14 with label at +16; `radioWidth(label)` |
| `listRow(selected)` | `CPUSelectionList` row | nine-slice row, light blue when `selected` is true; pass `widget::isHovering` for hover rows |

Example:

```java
ButtonWidget<?> button = new ButtonWidget<>();
IDrawable background = AE2Theme.button(button, () -> true, () -> false);
button.background(background)
    .hoverBackground(background)
    .overlay(AE2Theme.label(button, () -> "Apply", () -> true, () -> false));
```

`AE2Theme.Icon` lists the shared icons (`COPY`, `MORE`, `REFRESH`, `VISIBLE`, `PLAY`, `PAUSE`, `MOVE_TO`, `SEARCH`, `CHECKMARK`, `GEAR`, `ADD`, `REMOVE`, `CLOSE`, `EDIT`, `FILTER`); `icon.texture(variant)` falls back to the MUI2 icon when `variant` is `null`.

## Direct drawing

For custom `draw` methods. Coordinates are relative to the current widget. The methods that take a non-null `Variant` have no MUI2 fallback; check `variant != null` first.

| Method | AE2 1.21 original | Size |
| --- | --- | --- |
| `drawScrollbar(variant, x, y, height, handleY, enabled)` | `Scrollbar.BIG` | groove `x..x+scrollbarWidth(variant)`, `y..y+height`; handle 12x15 travels `height - 17`; `variant == null` draws the vanilla creative tab scroller (14 wide) |
| `drawSmallScrollbar(variant, x, y, height, handleY, enabled)` | `Scrollbar.SMALL` | 7 wide, handle 7x15 over a 5 px groove |
| `drawButtonBar(variant, x, y, width, height)` | `VerticalButtonBar` | AE2 draws it at buttons' box `(-2, -1, +1, +4)`; buttons are `BUTTON_BAR_MARGIN` from the edge, `BUTTON_BAR_SPACING` apart |
| `drawUpgradePanel(variant, x, y, slots)` | `UpgradesPanel` | slots in columns of `UPGRADE_ROWS` (8), first slot at `+UPGRADE_PADDING`; `upgradePanelWidth/Height(slots)` |
| `drawTable(variant, x, y, columns, rows, cellWidth, cellHeight)` | crafting CPU table | well of `tableSize(columns, cellWidth)` x `tableSize(rows, cellHeight)`; cell `(c, r)` starts at `x + 1 + c * (cellWidth + 1)`, `y + 1 + r * (cellHeight + 1)` |
| `drawLegacyState(variant, index, x, y, size)` | AE2 rv3 `states.png` | cell `index` of the old 16x16 icon sheet |
| `text(text, x, y, color, shadow)` | | font draw that restores the GL colour |

`Variant` also exposes the raw textures: `panel`, `inset` (list well), `slotItem`, `slotFluid`, `scroller`, `scrollerDisabled`, `bigScroller`, `bigScrollerDisabled`, `textField`, `iconButton(Hover)`, `tabButton(Hover)`, `switchSprite(on, hovered)`, `radioSprite(on, hovered)`, `listRow`, `listRowSelected`, `buttonBar`, `progressEmpty`, `progressFull`, `invalid`, and `legacyStates` / `legacyTerminal` (AE2 rv3 sheets recoloured for the variant).

Progress bar (`ProgressBar` in AE2, the inscriber bar):

```java
new ProgressWidget().texture(variant.progressEmpty, variant.progressFull, AE2Theme.PROGRESS_HEIGHT)
    .direction(ProgressWidget.Direction.UP)
    .size(AE2Theme.PROGRESS_WIDTH, AE2Theme.PROGRESS_HEIGHT)
    .progress(() -> fraction);
```

## Widgets

### `HandleScrollData`

Vertical scroll data with AE2's fixed-size handle: `new HandleScrollData(variant)` (big, 12 px) or `new HandleScrollData(variant, true)` (small, 7 px; big vanilla when `variant` is `null`). `getThickness()` is the width to reserve on the right of the rows.

```java
ListWidget<IWidget, ?> list = new GTNLListWidget<>().scrollDirection(new HandleScrollData(variant))
    .size(width, height);
```

MUI2 skips the scroll bar when nothing scrolls; `GTNLListWidget` then draws the disabled handle itself, so use it (not a plain `ListWidget`) to get AE2's greyed handle. A custom scroll widget calls `drawTrack(area)` every frame instead. The groove is drawn one pixel above and below the scroll area.

Like AE2 1.21 lists, it draws no fade shadow at the top and bottom edges. For a list of equal rows call `.rows(rowHeight)`: the wheel moves one row, dragging snaps to whole rows and scrolling is not animated, so no row is ever cut in half. Make the list height a multiple of the row height for this to line up.

### `NumberEntry`

AE2's amount entry (`NumberEntryWidget`): four `+` buttons, a text field and four `-` buttons, 138x62 (`NumberEntry.WIDTH/HEIGHT`). Steps are 1/10/100/1000, or 1/16/32/64 while Shift or Ctrl is held. The field accepts math expressions and SI suffixes (`2*64`, `1.5k`); a valid expression shows its value in the tooltip. Invalid input turns the text red and shows the warning icon with the reason (`gtnllib.gui.number_entry.*`).

```java
NumberEntry amount = new NumberEntry(variant).bounds(1, 1_000_000)
    .value(64)
    .onChange(value -> requested = value);
amount.value(); // OptionalLong, empty while the text is invalid
```

### Others

- `SlideSwitch`: a `ButtonWidget` switch that takes its sprites and colours from the theme (`value`, `label`, `availableWhen`); `SlideSwitch.widthFor(style, textWidth)`.
- `AE2ButtonDrawable`: the serializable button background used by the theme json (`gtnllib:ae2_button`); `AE2ButtonDrawable.draw(folder, state, ...)` draws it directly.

## Sizes

`BUTTON_HEIGHT` 20, `TEXT_FIELD_HEIGHT` 12, `ICON_SIZE` 16, `ICON_BUTTON_WIDTH/HEIGHT` 18x20, `TAB_BUTTON_SIZE` 20, `SWITCH_WIDTH/HEIGHT` 22x12, `RADIO_SIZE` 14, `LIST_ROW_HEIGHT` 22, `SCROLLBAR_WIDTH` 7, `BIG_SCROLLER_WIDTH` 12, `SCROLLER_HEIGHT` 15, `PROGRESS_WIDTH/HEIGHT` 6x18, `VALIDATION_ICON_SIZE` 8, `INSET_TOP` 3 and `INSET_EDGE` 1 (list well borders). `AE2Theme.STYLE` is the matching `ThemeStyle`.

## Adding another AE2 sprite

1. Copy the file from AE2 at the revision in `META-INF/licenses/AE2-gui-theme-NOTICE.txt` into `textures/gui/skin/ae2_light/` (crop only when a sheet holds unrelated sprites).
2. Make the `ae2_dark` copy by replacing each colour with its pair from the existing light/dark files; colours with no pair are interpolated and listed in the NOTICE.
3. List both the local and the original file in the NOTICE.
4. Add a `UITexture` field to `Variant` and, when it needs layout logic, a helper in `AE2Theme`.
