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

## Icon, input and syntax extraction (2026-10-31)

Extracted the existing host SpriteAtlas, UserIconLoader and PlayerHeadIcons into
client/render/icon as instance-owned SpriteAtlas, FileIconCache and PlayerHeadCache.
Host resource identifiers, file locations and logging are supplied by the caller.
File icon refresh also releases textures owned by that cache.

Moved key injection, held-key tracking and modifier handling into client/input;
held-key resynchronization is part of KeyInjector. The library exclusively owns
the client KeyBindingAccessor mixin. Host action settings and lifecycle dispatch
remain in GT-Never-Reset. Mode spellings and serialized action fields are retained.

Moved LuaSyntax lexical analysis into text/syntax, with caller-supplied immutable
API vocabularies. The host keeps its script vocabulary and runtime. These changes
retain the existing code ownership and licensing terms. See the radial-menu port
source notice for its original source and attribution.

## AE2 screen themes

Ported the reusable light and dark AE2 themes from GTNLLib PR 2 at babc4ac208aa51b19d176444a19de4b4aad004d8.
https://github.com/GTNotLeisure/GTNLLib/pull/2
Adapted namespaces, resource paths and registration to the GTNRLib client lifecycle. See GPL-3.0.txt for the code and the AE2 and Modernity notices in this directory for the assets.

The additional AE2 widgets, scrollbar and theme guide come from [GTNLLib PR 3](https://github.com/GTNotLeisure/GTNLLib/pull/3) at d5c43b46380948c0b043b483823c99b7b3751288. The guide is reproduced verbatim. The library uses its own namespace and generates the quantity validation messages from Java translation declarations.

Updated on 2026-11-11 to GTNLLib commit c1e431dbcabf33dfbba7955a28267827725c4574:
[PR 4](https://github.com/GTNotLeisure/GTNLLib/pull/4) anchors scrollbar dragging,
[PR 5](https://github.com/GTNotLeisure/GTNLLib/pull/5) adds the dark purple theme,
and [PR 6](https://github.com/GTNotLeisure/GTNLLib/pull/6) fixes flat dropdown options
and menu padding. Also includes the persistent scrollbar, empty-list layout and
number-entry corrections in 4f6ee31aeff8d92e24b179e6786c5a2dca51e5a6.
The theme guide is adapted to local package paths and runtime screenshots; purple skin assets follow that revision. GTNRLib retains its
local radio drawing, adjacent translation declarations and isValueValid() API;
isInputValid() is an alias for compatibility with upstream examples. Radio
highlights now follow the selected variant. Code and asset licenses are unchanged.

## First-person hand motion

The example uses 1.7.10 action keyframes, converted neutral held-item poses and a client motion sampler. The releases and the separate Forge source port below were inspected for behavior. Upstream code, scripts and assets are not bundled.

- The upstream release’s `hand_pose.lua`, `hand_relative_pose.lua` and `item_pose.lua` separate hand motion, wrist placement and item transforms. The ground gait uses traveled distance and eases in and out. For the empty hand, the published hand scripts were also sampled at 60 Hz with fixed view, stationary player and vanilla swing progress to locate the inward strike and outward recovery. The example uses independently authored keyframes for the 1.7.10 arm mesh. SHA-1 `cb8c8301fcd51ed255a3d77bfcb85a0426d1a222`. [Hold My Items 5.1.1 for Fabric](https://modrinth.com/mod/hold-my-items/version/KZm1qTcU)
- `de.bene2212.holdmyitems.mixin.HeldItemsMixin` in the published Forge JAR applies a vertical-velocity response, idle breathing and a tilt during the equip transition. SHA-1 `a7d8354adb258be8d57c344a40dd1873647868ea`. [Hold My Items – Reforged 2.0](https://modrinth.com/mod/hold-my-items-reforged/version/TBHht5Nt)
- The separate 1.19.2 Forge backport of 5.1.1 runs the shared pose before pushing separate arm and item matrices. It was used to verify transform order. [Forge source at 4302ab2, HeldItemsMixin](https://github.com/zadei/hold-my-items-1.19.2-port/blob/4302ab25e233893e97fb6131fc09e9c14a724e40/src/main/java/de/bene2212/holdmyitems/mixin/HeldItemsMixin.java#L250-L368)

The offline grip converter samples the 5.1.1 release above with the LuaJ runtime and easing class inside the user's original archive. It composes the Java arm and item mounts with the 1.21.11 handheld/generated model display transform and retargets the result to the 1.7.10 renderer. The JAR declares CC0-1.0 in `fabric.mod.json` and includes `LICENSE_holdmyitems`. The Modrinth project metadata lists All Rights Reserved. This repository distributes its own sampler, conversion math and pose parameters. [Original project metadata](https://api.modrinth.com/v2/project/hold-my-items)

The 5.1.1 playback adapter reads the four pose/model scripts and selected model and texture data from the player's verified local archive (SHA-256 `dc90bce5ff8d4a9932ec2226d8d5a2644a190fbe6681b7a26bb1438679f3f9f8`). No HMI script, texture, model or binary is included in GTNRLib. The bridge, OpenGL adapters and regression fixtures are original implementations. LuaJ 3.0.1 is bundled under a private package. Its license is in `LUAJ-MIT.txt`. [LuaJ 3.0.1 sources](https://repo.maven.apache.org/maven2/org/luaj/luaj-jse/3.0.1/luaj-jse-3.0.1-sources.jar)

GTNRLib 0.1.23 includes numerical local pose and mount observations obtained from the same verified 5.1.1 release. The offline capture tool and the in-game spring, gait, blending and playback code were written for this repository. The 35 default first-person meshes are independently authored and use the installed Minecraft block atlas. Running the default player requires only the distributed mod.

For 0.1.24 the held-model display context was checked against the release's `HeldItemRendererMixin`, the Minecraft 1.21.11 client models and Mojang's client mappings. The right-hand submission uses intermediary field `field_4320`. The grip check separately measures the legacy skin and extruded sprite at draw time. [Fabric 1.21.11 display-context mapping](https://maven.fabricmc.net/docs/yarn-1.21.11%2Bbuild.4/net/minecraft/item/ItemDisplayContext.html#THIRD_PERSON_RIGHT_HAND)
