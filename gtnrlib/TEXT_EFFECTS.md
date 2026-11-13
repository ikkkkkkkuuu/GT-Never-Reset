# Text effects

Inline effects work through the shared font renderer, with optional Angelica batching integration. Both prefixes are accepted:

```text
&{burnished_auric;colors=#FFD700,#FFF2A0;speed=1.2}&oText&r
§{burnished_auric;colors=#FFD700,#FFF2A0;speed=1.2}§oText§r
```

`&r`/`§r` clears the effect and native formatting. Native colors such as `&c` replace the active effect palette while keeping the effect. Bold, italic and other native styles retain their ordinary meaning. Angelica's own `&q` and `&z` still require Angelica.

## Compact input

Sign lines in Minecraft 1.7.10 allow only 15 characters, including formatting declarations. Aliases reduce a declaration to six characters. Long identifiers remain supported.

| Alias | Renderer |
| --- | --- |
| `ir` | `infernum_red_rarity` |
| `gc` | `genesis_component_rarity_shader` |
| `pc` | `pulse_circle` |
| `nb` | `nameless_boss_bar_shader` |
| `pu` | `pulse_upwards` |
| `cr` | `calamity_red` |
| `er` | `exotic_rainbow` |
| `sb` | `superboss_rarity` |
| `is` | `infernum_spark_rarity` |
| `ba` | `burnished_auric` |
| `ec` | `evercold_cyan` |
| `ss` | `starsilver_rarity` |
| `nr` | `nebula_rift` |
| `ps` | `prismatic_scan` |
| `qg` | `quantum_glitch` |
| `mc` | `molten_core` |
| `nf` | `neon_flicker` |
| `ac` | `arcane_cipher` |
| `gl` | `gravity_lens` |
| `mt` | `melting` |
| `cn` | `constellation` |
| `tc` | `corruption` |

```text
&{ba}GOLD
&{pu}&oHello
&{ba;#fc0;2}Hi
&{nr}Nebula Rift
&{ps}Prismatic Scan
&{qg}Quantum Glitch
&{mc}Molten Core
```

The last example uses `#FFCC00` at twice the normal speed and occupies 14 characters. Positional colors start with `#`; a positional number sets speed. Named options accept `c` for `colors` and `s` for `speed`, for example `&{ba;c=fc0;s=2}Hi`. Colors accept three or six hexadecimal digits, with up to eight comma-separated entries. Speed must be finite and nonnegative; zero freezes animation. Duplicate and unknown options are rejected.

Unknown inline renderer names remain ordinary input when drawn. They do not flush deferred text belonging to other signs. Available renderer names are resolved during layout creation, so register renderers and aliases before rendering starts.

## Neon flicker

`&{nf}OPEN` draws glowing tubes: a pale core, a soft halo blurred from the strokes, and a whole-sign dip every four seconds, so a recording of four seconds or more always holds one. Every 2.2 seconds each glyph rolls again whether it sputters out for a while; `fault=` is that chance in whole percent (default 20, `fault=0` for none), as in `&{nf;#4ff,#08f;fault=40}`. The first color is the tube, the second the halo. Only the neon flicker accepts `fault=`; in code use `TextEffects.NEON_FLICKER.withFault(40)`.

Light reaching a pixel takes the brightness of the glyph it comes from, so a dark glyph's halo fades with it while its neighbors' glow still spreads over it. Glyphs are told apart by the mask's glyph columns, which follow where each glyph was drawn: each glyph is drawn alone with color writes off, and vanilla truncates every advance, so the sum matches the drawn text. Columns placed by measured width drifted after unicode text, because vanilla measures unicode glyphs wider than it draws them, and split later glyphs between two columns. Every effect that reads glyph columns (upward pulse, burnished auric, rune etching, starsilver rarity and the neon flicker) uses the drawn columns. The centers are 8-bit, so on a very long line two neighboring glyphs can share one and fail together.

## Effect options

An effect can register named numbers of its own with `TextEffectFormat.registerOptions(rendererId, TextEffectOption...)`. They are written `name=value` in the declaration, are checked against the option's range and kind (whole or up to three decimals), and are only accepted by the effect that registered them; anything else rejects the declaration. They live in `TextEffectStyle.options`, rounded to the millisecond, and an option left out keeps the effect's default. In code use `style.withOption(name, value)`; the effect reads them with `style.option(name, fallback)`. The neon flicker's `fault` is one of them (`withFault` remains as a shortcut).

## Arcane cipher

`&{ac}` writes its text in pixel runes that keep changing. A wave turns the line into plain text from left to right, the whole line stays readable, a second wave turns it back, and the whole line stays runes before the next round. Every pass takes the same time whatever the line's width, so lines of different lengths stay in step. Glyphs dissolve pixel by pixel with a glitter on the strokes caught mid-change, a rune shatters into motes as its glyph turns to text, and while the line reads a glyph now and then flicks back to its rune. Punctuation keeps its own shape in rune ink, and blanks draw nothing.

| Option | Meaning | Range | Default |
| --- | --- | --- | --- |
| `hold=` | seconds the whole line stays text; 0 keeps it runes for good | 0 to 30 | 4 |
| `hide=` | seconds the whole line stays runes | 0.5 to 30 | 3 |
| `sweep=` | seconds one pass of the wave takes | 0.2 to 10 | 1.5 |
| `flip=` | rune changes per second; 0 freezes them | 0 to 30 | 5 |
| `shards=` | motes a rune shatters into | 0 to 8 | 4 |
| `flick=` | percent chance per 0.6 s that a glyph flicks back | 0 to 100 | 12 |

The three colors are the runes, the glow and the revealed text, as in `&{ac;#7cf,#24a,#fff;hold=4}`. Runes are built from strokes on a 3 by 4 lattice and stretch to the glyph's drawn width, which the mask's second metadata row records together with whether the glyph is a blank, punctuation or a letter.

## Gravity lens

`&{gl}` sends an unseen black hole along the line: it starts past the left edge, crosses to the right and comes back every eight seconds (at speed 1), bobbing a little, so a recording of eight seconds loops seamlessly. Text near it is drawn from points pulled toward the hole, so glyphs bend and stretch around it; light near the hole dims and warms. A tilted accretion disk spins around the horizon, brighter on one side, its back half hidden behind the black disk and its front half crossing it; a thin photon ring hugs the horizon and a few motes spiral in. Everything is sampled on the font's half-pixel grid. The three colors are the text, the hot disk and ring, and the glow, as in `&{gl;#fff,#f80,#40f}`.

## Melting

`&{mt}` draws glowing molten metal. Each pixel's color comes from a temperature: white-hot in the middle of a stroke, cooler toward its edges and lower down, with dark dross drifting across the surface. The lower half of the line droops, stretches and sways. Beads swell under the strokes on a wide neck, pulling the stroke above them down; every 2.6 to 4.2 seconds (at speed 1) each one snaps, throws four sparks, and falls while cooling from orange to dark red with a crust. Faint heat ripples rise above the line. The beads hang below the text, so the effect pads its mask by 1.6 line heights. The three colors are the white heat, the orange heat and the ember, as in `&{mt;#ffe,#f81,#710}`.

## Constellation

`&{cn}` dims the text and, every six seconds (at speed 1), lights a new constellation: one star per six-pixel slice of the line, placed on a stroke, lit from left to right; brighter stars show a four-point cross that breathes. Lines grow from each star to the next, sometimes also to the one after, closing a triangle, with a bright point at their growing end. Text near a lit star brightens, faint dust stars twinkle around the line, and the stars go out from left to right before the next figure. Long lines light faster so the figure still finishes within the cycle. The three colors are the text, the lines and the glow, as in `&{cn;#bcf,#9df,#236}`.

## Corruption

`&{tc}` draws dark strokes with a sick glow along their edges, slowly eaten by pits. Tendrils root on the strokes, uncurl to between 1.5 and 7 pixels with a hooked tip and pull back in; a glowing pulse runs along each. A flux wave crosses the line every 2.5 seconds and ripples the strokes, and every five seconds a rift tears the line open with its own slant, span, zigzag and side crack, pushing the strokes apart before it closes. Each line seeds its rifts from its own width and glyphs, so lines tear differently and a moving tooltip does not change them. Flux wisps seep out and drift upward. A recording of five seconds loops. The four colors are the core, the edge glow, the halo and the flux, as in `&{tc;#213,#c6f,#518,#f5d}`.

## Typewriter

Parameters other than colors and an effect's own animation speed are always named, and every time is in seconds, down to the millisecond:

| Parameter | Meaning | Example |
| --- | --- | --- |
| `t=` (or `type=`) | typing rate in glyphs per second, 0 to 1000 | `&{ba;t=20}` |
| `r=` (or `reveal=`) | entrance of each typed glyph, optionally with its time (at most 2) and, for `slam`, its start size | `&{tw;t=24;r=decode:0.6}`, `&{tw;t=20;r=slam:0.3:4}` |
| `roll=` | numbers count up from zero over this time (at most 10), optionally with a mode, `slot` and `nowait` | `&{ba;roll=2.5:jump}`, `&{roll=2:slot:left:nowait}` |
| `blur=` | how strongly spinning slot wheels smear, in percent (0 to 200, 100 by default, 0 off); needs `roll=` | `&{roll=2:slot;blur=40}` |
| `shake=` | glyphs shake by up to this many pixels (1 to 3), optionally with `each` or `sync` and `smooth` or `jump` | `&{cr;shake=2:sync:jump}` |
| `&{p;s}` | a pause before the next glyph (at most 10; `&{p}` is 0.5) | `&{p;0.8}` |

Plain text types with `&{tw}` (20 glyphs per second) or `&{tw;t=40}`, rolls with `&{roll}` (1 s) or `&{roll=1.5:fade}`, and shakes with `&{shake}` (1 pixel) or `&{shake=2:sync:jump}`. An unnamed number after `tw`, `roll` or `shake` is rejected, since they have no animation speed.

Any effect types its text one glyph at a time with `t=`; zero shows the span at once. A pause marker changes no style, so it can sit inside any effect.

Entrances: `decode` flickers through stand-in glyphs before settling (0.18 s by default), `fade` fades the glyph in (0.25 s), `drop` drops it from four pixels above (0.2 s), `slam` falls from several times its size onto its place around its center (0.3 s), fading in fast and flashing a lighter shade of its own color as it lands. A slam's start size follows its time, from 1.5 to 6 times (3.5 by default), as in `r=slam:0.3:4`; only a slam takes a size, and writing the default time or size equals leaving it out. They apply to typed glyphs only.

Rolling numbers act as odometer wheels and grow from the left: wheels the value has not reached take no room. Separators follow the digit on their left. Modes:

- `cut` slides each digit like an odometer, cut at the line.
- `fade` slides it and fades it out over the last three pixels before the line.
- `jump` shows whole numbers that step every 0.05 s with a gentler ease.
- Without a mode, numbers in an effect jump, because an outlined effect would also trace a sliding digit's cut, and plain numbers cut.

`slot` spins the number like a slot machine instead: every wheel shows from the start and spins on its own, then brakes onto its digit with a small bounce, one wheel after another. The first stops about a third of the way through the roll and the last as it ends. They stop from the right, the ones digit first; `slot:left` stops them from the left, and `slot:right` is the default written out. `slot` combines with a mode in any order, as in `&{roll=2:fade:slot:left}`; with `jump` the spinning digits step instead of sliding. A spinning wheel smears into faint copies of its strip above and below it, fullest at speed and fading as it brakes; `blur=` scales the smear in percent, and the scoped form writes it as the roll word `blur=40`, which the inline form also accepts (`roll=2:slot:blur=40`). A jumping number never smears.

`nowait` lets the text after a number type on while the number rolls instead of waiting for it, as in `&{roll=1.5:slot:nowait}`; the text still counts as typing until the roll ends, while the next line starts once the typing is done.

Digits joined only by a separator (`, . _ '`) form one number; numbers split by anything else, such as `32 / 64` or `3x 128`, count up on their own at the same time. A number written with leading zeros (`007`) keeps all its wheels from the start, and a decimal keeps its decimals and the digit before the point, so `0.85` rolls as `0.xx`; a last separator with exactly three digits after it reads as a thousands separator unless an earlier separator is a different mark (`1,048.576` is a decimal, `1.048.576` and `12.345` are not). Wheels are computed exactly, so numbers of any length land on their digits. A sliding number is captured into one mask, cut to its line before the effect shades it, so glow and outlines still spread past the cut; plain sliding numbers use a one-color mask shader. Without framebuffers or GLSL a number jumps instead. Text in the rolling style after the number keeps the latest typing rate and entrance, so a unit after a rolled number types on.

Shaking glyphs pick a new random target twelve times a second. `smooth` (the default) glides there, `jump` snaps to whole pixels. `each` (the default) shakes every glyph on its own and moves it half as far sideways, so narrow punctuation stays clear of its neighbors; `sync` moves the run as one. An effect run shaking glyph by glyph is captured into one live mask, redrawn in place on the same framebuffer as the offsets change, so the effect's colors stay continuous across glyphs. Turning effects off also stops the shake.

- Text types when it appears and starts over after it has not been drawn for a frame, so a tooltip types each time it opens.
- Lines inside one string type one after another. So do strings drawn lower at the same x in the same frame, such as the lines of a tooltip; plain lines without typing still show at once.
- The layout keeps the width of the whole text, so a tooltip does not grow while typing.
- Each typed glyph, the first included, shows one interval after the one before it, so text starts with the caret alone.
- The caret `_` holds the slot of the next glyph, blinks during a pause, and disappears when typing ends. It is hidden when it would reach past the end of the text.
- Typed text is revealed by the font's drawn advance. Vanilla measures unicode glyphs wider than it draws them, so measuring would drift right on CJK text. A partly typed effect run is drawn from a mask of its typed glyphs alone at the full run size, so hidden glyphs cannot bleed outlines in.
- Text fields (chat input, NEI search) show their text in full while it is edited.
- The client setting `enableTypewriter` turns typing off; marked text then shows in full at once.

In code, use `TextEffects.TYPEWRITER`, `TextEffects.ROLL`, `style.withTyping(rate)`, `style.withEntrance(TextEntrance.DECODE, 0.6f)`, `style.withRoll(2.5f, RollMode.JUMP)`, `style.withRoll(2, RollMode.AUTO, SlotOrder.LEFT_FIRST)`, `style.withEntrance(TextEntrance.SLAM, 0.3f, 4)`, `style.withRollBlur(40)`, `style.withRollWait(false)`, `style.withShake(pixels, ShakeMode.SYNC, ShakeMotion.JUMP)` and `TextEffects.pause(0.8f)`. The Java API takes seconds too, and styles round every time to the millisecond so equal times compare equal. A style groups these settings into three records: `style.typewriter()` (`rate`, `entrance`, `entranceSeconds`, `entranceSize`), `style.roll()` (`seconds`, `mode`, `slot`, `blur`, `waits`) and `style.shake()` (`pixels`, `mode`, `motion`); each has a `NONE` value, and `withTypewriter`, `withRoll` and `withShake` replace one group whole.

## Code entry points

Use `TextEffects.format(style)` to open an inline effect and `TextEffects.apply(text, style)` for a closed, composable span. Existing `AnimatedTooltipHandler.renderedText` suppliers remain supported. Additional aliases can be registered during initialization with `TextEffectFormat.registerAlias(alias, namespacedRendererId)`.

All font-effect mixins are grouped under `mixins.early.texteffect` and `mixins.late.texteffect`, with optional Angelica hooks in `early.texteffect.angelica`. NEI keeps editable declarations intact and shares the vanilla input-field projection. The projection maps visible glyphs back to original source indices for scrolling, selection and cursor splits.

The font hooks cover the public draw and trim overloads as well as their vanilla delegates. This allows font replacements such as Qz-UILib to keep supplying glyphs without consuming effect declarations before GTNRLib parses them. No Qz-UILib dependency, reflection or class reference is introduced. UILib's separate scene/Markdown rendering API is outside these Minecraft font entry points.

The preview reads every registered effect when opened and displays its shortest registered alias before its name, for example `[ba] burnished_auric`. Custom registered renderers are included automatically; entries without aliases display their renderer names. Page count and the mixed-text example follow the collected entries.

Angelica defers sign text until its board geometry has rendered. Effect text follows that same deferred flush, using saved matrices, depth flags, Unicode mode and lightmap coordinates. It does not terminate model batching. Deferred storage and matrix buffers are pooled; layouts, draw runs, input slices and glyph masks are cached with existing bounds. No-Angelica rendering does not link Angelica classes: optional methods are erased and Angelica mixins are conditionally loaded.

## Verification

- `gradlew build -x test -x testClasses` passed, including formatting, compilation, checkstyle, packaging and JVM downgrading. No unit tests were added or run.
- Typewriter: `TypewriterFormatTest` covers the rate, entrance, roll, alias and pause syntax and both written forms; `TypewriterTimelineTest` covers line order, tooltip line chaining, pauses with the blinking caret, entrances and a slam's size, a rolling number holding the unit after it or, with `nowait`, letting it type on, and starting over after the text reappears; `NumberRollTest` covers odometer wheels, growth from the left, jump steps, separators, separate numbers in one run, leading zeros and decimals, exact long numbers, slot wheels stopping from either side, and the smear on spinning wheels only; `NeonFlickerFormatTest` covers the fault syntax and both written forms; `ArcaneCipherFormatTest` covers the cipher options and both written forms; `GravityLensFormatTest` covers the lens alias and colors; `MeltingConstellationCorruptionFormatTest` covers those three aliases and the corruption's fourth color; `TextShakeTest` covers whole-pixel jumps, smooth glides, the halved sideways move and glyphs shaking together; `TypewriterFormatTest` also covers the shake syntax, the slot words, the slam size, `blur` and `nowait`. Run them with `gradlew test`. The entrance and roll drawing (masks, scissor, fade pass, plain mask shader, the slam's scale and flash, the slot smear), the shake drawing, the neon flicker, the arcane cipher, the gravity lens, the melting, the constellation and the corruption have not been checked in game yet.
- Both client variants loaded the input/NEI mixins. The no-Angelica variant skipped the Angelica mixin.
- Both variants rendered adjacent signs containing `&{123}`, `PLAIN`, and `&{ba}GOLD`; all three texts remained visible. Captures are in `build/signcheck-angelica.png` and `build/signcheck-vanilla.png`.
- The user confirmed the NEI search field and chat input fixes. Their injection sites were also verified in transformed classes.
- Qz-UILib compatibility was traced against its local source. No Qz-UILib binary was available in the checkout for a runtime check.
- No numerical FPS benchmark or exhaustive check of third-party text renderers was performed.

## Public type and field audit

Ordinary public classes remain non-final. Existing public constructors noted below remain available. Unchanged fields are omitted except where a relocated mixin needs its complete shadow contract documented.

| Public type/signature | Added or changed fields/components |
| --- | --- |
| `class TextEffectFormat` | `String INLINE_OPEN`, `String AMP_INLINE_OPEN`, `int MAX_HEADER_LENGTH`; private `Map<String,String> ALIASES`, `int[] VANILLA_COLORS`. Static syntax, formatting, color, alias-registration and `String aliasFor(String)` methods. |
| `EffectTextParser.Cursor(String)`; `(String, UnaryOperator<String>, boolean)`; `(String, UnaryOperator<String>, boolean, Predicate<String>)` | `String source`, `UnaryOperator<String> preprocessor`, `boolean hexResetsStyles`, `Predicate<String> rendererAvailable`, `Deque<Scope> scopes`, `TextEffectStyle style`, `String formatting`, `int position`. |
| `record EffectTextParser.Token` | `String text`, `TextEffectStyle style`, `String formatting`, `int start`, `int end`, `boolean formattingCode`. |
| `record EffectTextLayout.Glyph` | Adds `int sourceStart`, `int sourceEnd` to `text`, `formatting`, `style`, `width`; old four-argument constructor retained. |
| `record EffectTextLayout.DrawRun` | `String text`, `TextEffectStyle style`, `float x`, `float y`, `float width`. |
| `record EffectTextLayout.Layout` | Adds `List<DrawRun> runs` to `glyphs`, `width`, `height`, `spacing`, `lines`; old five-argument constructor retained. |
| `record EffectTextRenderer.LayoutKey` | `FontRenderer font`, `String text`, `boolean unicode`, `int fontHeight`, `float scaleX`, `float scaleY`, `float spacing`, `float whitespace`, `String preprocessing`, `boolean hexResetsStyles`. |
| `class EffectTextFieldView(String, Layout)` | `String source`, `Layout layout`, `int[] sliceStarts`, `int[] sliceEnds`, `String[] slices`, `int nextSlice`. Public `matches`, `slice`, `width`, `trim`. |
| `class DeferredTextEffects` | Static `Deque<Draw> PENDING`, `Deque<Draw> POOL`, `boolean flushing`. Public static `isFlushing`, `enqueue`, `flush`, `clear`. |
| `static class DeferredTextEffects.Draw` | `FloatBuffer projection`, `FloatBuffer modelView`, `FontRenderer font`, `String text`, `float x/y`, `int color`, `boolean shadow/unicode/depthTest/depthMask`, `float lightX/lightY`. Private capture/replay methods; render-thread-owned storage. |
| `class EffectTextRenderer implements IResourceManagerReloadListener` | Adds private static `int nativeDepth` and public static `boolean isBypassingEffects()` to separate native fallback from coverage capture. |
| `class TextEffects` | Adds static `String format(TextEffectStyle)`; existing preset fields retain their signatures. |
| `class AngelicaTextAdapter` | Adds static `boolean shouldDeferEffects()` and erased implementation method; no added fields. |
| `class TextEffectPreview extends GuiScreen` | Adds private `boolean italic` and `List<Entry> entries`; removes the hardcoded `PRESETS` array. |
| `record TextEffectPreview.Entry` | `TextEffectStyle style`, `String label`. |
| `class TextEffectRegistry` | Changes the backing map to registration order and adds private `List<String> identifiers`; public static `List<String> identifiers()` returns its immutable snapshot. |
| `abstract class MixinGuiTextFieldTextEffects` | Shadow `FontRenderer field_146211_a`, `String text`, `int lineScrollOffset`, `int cursorPosition`; unique `EffectTextFieldView gtnr$effectView`. |
| `abstract class MixinNEIFormattedTextField extends GuiTextField` | Public `(FontRenderer,int,int,int,int)` constructor; no fields. |
| `abstract class MixinGuiNewChatTextEffects`; `abstract class MixinEnumChatFormattingTextEffects` | No fields. |
| Relocated `abstract class MixinFontRendererTextEffects` | No fields. |
| Relocated `abstract class MixinFontBatchTextEffects implements FontBatchBridge` | Shadows `FontRenderer underlying`, `int batchDepth`, static `BatchingFontRenderer arenaOwner`, `int fontShaderId`, `int AAMode`, `int fontAAModeLast`. Adds a deferred-flush callback; no queue state in the mixin. |
| `enum Mixins implements IMixins` | Separate `TEXT_EFFECTS_COMMON`, `TEXT_EFFECTS_CLIENT`, `TEXT_EFFECTS_ANGELICA`, `TEXT_EFFECTS_NEI` registrations; instance fields unchanged. |

`TextEffectStyle(String rendererId, List<Integer> colors, float speed)`, `AnimatedText` preset field signatures, and `TextMaskCache` field signatures remain unchanged.

The typewriter adds components with the old constructors retained: `TextEffectStyle.typewriter`, `roll`, `shake` and `options` (the nested records `TextEffectStyle.Typewriter`, `Roll` and `Shake`), `EffectTextParser.Token.pause`, `EffectTextLayout.Glyph.pause`, `EffectTextLayout.DrawRun.glyphs` and `TextMaskCache.Mask.advance`. The scoped form written by `TextEffects.opening` gains a fourth field for the rate, a fifth for the entrance, a sixth for the roll, a seventh for the shake and an eighth for the effect options, each only when needed, and three-field text still parses. New types: the enums `TextEntrance`, `RollMode`, `SlotOrder`, `ShakeMode` and `ShakeMotion`, `TypewriterTimeline` with its `Typing` and `GlyphEntrance` records, `NumberRoll`, `TextShake`, `TextEffectOption`, `NeonFlickerTextEffect` and `ArcaneCipherTextEffect`; the gravity lens, the melting, the constellation and the corruption are plain `ShaderTextEffect`s. `TextMaskCache` gains `getRolled` and `getShaken`, whose live masks keep a stable key and redraw in place when their glyph offsets or roll layers change.
