# GTNRLib sources and adaptations

Extracted from the existing GT-Never-Reset text parser, text renderer, font mixins
and shader resources on 2026-10-31. The initial extraction retained the existing
code's ownership and licensing terms.

The historical `gtneverreset:exotic_rainbow` style identifier and invisible text
markers are retained for compatibility. Package names and the owning shader
resource namespace are now `com.xyp.gtnrlib` and `gtnrlib`.

The local extraction was extended on the same date to include existing wheel
geometry, style and animation helpers; shared GUI widgets and scrolling screen;
the pixel-font ModularUI screen; JSON adapters; mod identifiers; world block
listeners; saturating long arithmetic; and the MBS1 decoder. These originate
from the corresponding GT-Never-Reset client/gui, utils/gui/radialmenu,
utils/enums, common/world, utils/machine/wireless, utils/crafting/quantum/util
and utils/structure packages. Existing implementations and applicable terms
are retained.

Adaptations: namespaces and caller imports changed; world-listener registration
belongs to the library; the scrolling screen accepts host navigation by override;
the structure decoder accepts a caller-owned stream while host paths and caches
stay in GT-Never-Reset. Existing enum spellings and serialized field names remain.

## Upstream framework import (2026-10-31)

Source: https://github.com/ABKQPO/GT-Not-Leisure/tree/6dffa94077cd8dbe319f84ac72884a42c55ea1c0/gtnllib

Repository: ABKQPO/GT-Not-Leisure, branch refactor-trial, PR #663.
Imported Java frameworks, shader/skin resources, and contract tests from `gtnllib`.
Also imported `infernum_base_rarity_glow.png` and `infernum_gleam.png` from
`src/main/resources/assets/sciencenotleisure/textures/effects` at the same commit;
the upstream library references these textures but leaves them in its host module.
The upstream module's GPL version 3 license is reproduced in `GPL-3.0.txt`.
Its bundled AnvilLib MIT notice is reproduced in `ANVILLIB-MIT.txt`.
These notices accompany the binary and source distributions in `META-INF/licenses/gtnrlib`.

Modifications: renamed packages, resource namespaces, language keys, Mixin members
and commands for GTNRLib; separated configuration paths; retained the existing
GTNR text protocol, wheel APIs, mod IDs, singleton world listener and bounded MBS1
decoder; merged the new APIs and preserved compatibility with GTNL/GTNLLib font
patches. Added a HUD editor entry in the configuration screen and migrated the
upstream language resources into adjacent Java translation declarations. Tests
were adapted to the module paths and extended for local compatibility.

## Additional local extraction (2026-10-31)

Moved GT-Never-Reset's KeyBindingLookup, KeyBuffer and VanillaKeyEffects from
client/keybind to library client/input; PotionIcons from client/render/radialmenu
to client/render/icon; ChunkCoords, Facing and WorldTime from client/script/radialmenu
to world; and TooltipLineWrapper from utils/structure to text.

Moved the implementations of RadialColorPickerGui, RadialConfirmGui,
RadialTextPromptGui and RadialMultilineEditorGui to library client/gui as
ColorPickerScreen, ConfirmationScreen, TextPromptScreen and MultilineEditorScreen.
Host wrappers retain navigation; generic screens own their translations.
Tooltip wrapping accepts a configurable width while keeping the original default.
Wheel color blending and alpha fading now belong to Colors. Existing callers of
duplicate integer rounding and saturation use LongMath; direct teleportation uses
the library TeleporterUtils. Existing local ownership and licensing terms apply.

Also extracted RadialKeyBindPickerGui as client/gui/KeyBindingPickerScreen and
WirelessCollections as utils/ImmutableCollections. The latter retains Java 8
compatibility, null acceptance, encounter order and shallow snapshot semantics.

## Internal consolidation (2026-10-31)

Moved upstream ColorMath's pure color arithmetic into the common-side Colors
implementation, retaining ColorMath's public methods as client-side compatibility
delegates and preserving its GL/Tessellator helpers. Percentage brightness shares
the RGB scale/white interpolation kernels. Rounded alpha scaling is shared by
FadingNotice and WheelWidget while local truncated alpha scaling stays distinct.
Facing and RadialGeometry share degree normalization in MathUtils; RevealTiming
uses the existing unit clamp; the cyan text sparkle uses FxEasing.smoothStep.
HUD options and elements use ImmutableCollections snapshots. Original float
operation order, API names, client isolation and collection ownership are retained.

## Wildcard GUI support extraction (2026-10-31)

Extracted WildcardPatternGui's floating selector as gui/widget/ScrollingDropdown,
merged its item preview and copy/drop rendering into gui/widget/ItemStackSlot,
and parameterized its nine-slice skin as gui/theme/ModernGuiSkin. The host retains
the original sprite resources and their existing notices, wildcard model and
fluid display conversion, recipe import, synchronization and page layout.
Item copies retain NBT; existing GhostItemSlot and CycleDropdown semantics remain
unchanged. Existing local ownership and licensing terms apply.
