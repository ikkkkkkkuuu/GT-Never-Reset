#version 120
#include "common.glsl"

// Rune changes per second, seconds the whole line stays text once the wave has turned it (zero keeps it runes),
// seconds the whole line stays runes once the wave has turned it back, and seconds one pass of the wave takes.
uniform float flipRate;
uniform float holdSeconds;
uniform float hideSeconds;
uniform float sweepSeconds;
// Motes a rune shatters into as it turns to text, and the chance that a glyph flicks back while the line reads.
uniform float shardCount;
uniform float flickChance;
const float FADE_IN_SECONDS = 0.25;
const float FADE_OUT_SECONDS = 0.6;

// Distance to a rune built from strokes between a 3 by 4 lattice of nodes; the seed picks which strokes exist.
float rune(vec2 local, float seed) {
    float best = 99.0;
    int strokes = 0;
    for (int row = 0; row < 4; row++) {
        for (int column = 0; column < 3; column++) {
            vec2 node = vec2(float(column) * 2.0 - 2.0, float(row) * 2.0 + 1.0);
            float id = float(row * 3 + column);
            if (column < 2 && hash(vec2(seed, id * 4.0)) < 0.3) {
                best = min(best, segmentDistance(local, node, node + vec2(2.0, 0.0)));
                strokes++;
            }
            if (row < 3 && hash(vec2(seed, id * 4.0 + 1.0)) < 0.38) {
                best = min(best, segmentDistance(local, node, node + vec2(0.0, 2.0)));
                strokes++;
            }
            if (row < 3 && column < 2 && hash(vec2(seed, id * 4.0 + 2.0)) < 0.14) {
                best = min(best, segmentDistance(local, node, node + vec2(2.0, 2.0)));
                strokes++;
            }
            if (row < 3 && column > 0 && hash(vec2(seed, id * 4.0 + 3.0)) < 0.14) {
                best = min(best, segmentDistance(local, node, node + vec2(-2.0, 2.0)));
                strokes++;
            }
        }
    }
    // A rune always keeps its spine, so no glyph turns into a lone dot.
    if (strokes < 3) best = min(best, segmentDistance(local, vec2(0.0, 1.0), vec2(0.0, 7.0)));
    return best;
}

// The mask's second row holds each glyph's drawn width over 16 units (red) and its kind (blue): 0 for a blank, a half
// for punctuation and 1 for a letter.
vec2 glyphShape(float x) {
    vec3 row = glyphMetadata(vec2(clamp((x + padding) / maskSize.x, 0.0, 1.0), 1.0 - 1.5 / (maskSize.y * maskResolution)));
    return vec2(row.r * 16.0, row.b);
}

float motes(vec2 point, float unit) {
    float light = 0.0;
    float count = min(24.0, max(3.0, textSize.x / unit / 7.0));
    for (int i = 0; i < 24; i++) {
        float seed = float(i);
        if (seed >= count) break;
        float age = fract(time * 0.35 + hash(vec2(seed, 7.0)));
        vec2 center = vec2(hash(vec2(seed, 8.0)) * textSize.x + sin(time * 1.7 + seed * 2.3) * unit,
            textSize.y * (0.9 - age * 1.4));
        float spark = 1.0 - smoothstep(0.0, 0.9 * unit, length(point - center));
        light = max(light, spark * sin(age * 3.141592654));
    }
    return light;
}

void main() {
    vec2 point = position();
    float unit = textSize.y / 9.0;
    vec2 coord = (point + padding) / maskSize;
    float center = glyphMetadata(vec2(clamp(coord.x, 0.0, 1.0), 1.0 - clamp(coord.y, 0.0, 1.0))).b * textSize.x;
    vec2 shape = glyphShape(point.x);
    float written = step(0.75, shape.y);
    // Punctuation keeps its own shape in rune ink, so the sentence still shows its pauses.
    float kept = step(0.25, shape.y) * (1.0 - written);
    // Runes stretch to the glyph's drawn width, so wide CJK glyphs fill out and narrow ones do not collide.
    float stretch = clamp(shape.x / 6.0, 0.5, 1.5);
    // Each glyph swaps runes a few times a second, out of step with its neighbors; a new rune flares as it forms.
    float clock = time * flipRate + hash(vec2(center, 1.0)) * 5.0;
    float flare = flipRate > 0.0 ? 1.0 - fract(clock) : 0.0;
    // Runes are sampled at the centers of the font's half-pixel grid and cut hard, so they read as pixel glyphs.
    vec2 snapped = snapToHalfPixel(point);
    float runeDistance = rune((snapped - vec2(center, 0.0)) / unit / vec2(stretch, 1.0), center * 13.0 + floor(clock) * 7.0);
    float runeInk = step(runeDistance, 0.5) * written;
    // A wave turns the line into text from left to right, the whole line holds for holdSeconds, a second wave turns it
    // back into runes, and the whole line stays runes for hideSeconds. Every pass takes sweepSeconds whatever the
    // line's width, so lines of different lengths keep the same cycle and stay in step.
    float sweep = sweepSeconds;
    float lead = clamp(center / max(textSize.x, unit), 0.0, 1.0) * sweep;
    float cycle = sweep + FADE_IN_SECONDS + holdSeconds + sweep + FADE_OUT_SECONDS + hideSeconds;
    float passed = mod(time, cycle);
    float leave = sweep + FADE_IN_SECONDS + holdSeconds + lead;
    float reach = holdSeconds <= 0.0 ? 0.0
        : min(smoothstep(lead, lead + FADE_IN_SECONDS, passed), 1.0 - smoothstep(leave, leave + FADE_OUT_SECONDS, passed));
    // Pixels cross over one by one, so a glyph dissolves from rune into text instead of the two overlapping.
    float grain = hash(floor((point + padding) / unit * 2.0) + vec2(center, 3.0));
    float shown = step(grain, reach);
    // While the line reads as text, a glyph now and then flicks back to its rune, as if the knowledge resists.
    float window = time / 0.6 + hash(vec2(center, 60.0));
    shown *= 1.0 - step(0.999, reach) * step(hash(vec2(center * 7.0, floor(window))), flickChance) * step(fract(window), 0.25);
    float text = coverage(point);
    // Only strokes caught mid-change glitter, so the dissolve stays on the glyph.
    float spark = (1.0 - smoothstep(0.0, 0.1, abs(reach - grain))) * max(text, runeInk) * step(0.02, reach)
        * step(reach, 0.98);
    float ink = mix(max(runeInk, text * kept), text, shown);
    if (shadowPass) {
        float alpha = ink * opacity * 0.65;
        if (alpha <= 0.001) discard;
        gl_FragColor = vec4(vec3(0.035), alpha);
        return;
    }
    vec3 light = paletteCount > 2 ? palette[2] : vec3(1.0);
    vec3 runeColor = mix(palette[0], palette[1], 0.5 + 0.5 * sin(time * 2.0 + center * 0.15));
    runeColor = mix(runeColor, light, 0.4 * flare * flare * flare);
    vec3 inkColor = mix(runeColor, light, shown);
    float halo = (1.0 - smoothstep(0.6, 2.4, runeDistance)) * written * (1.0 - shown) * (0.35 + 0.2 * flare);
    halo = max(halo, outline(point, unit) * kept * (1.0 - shown) * 0.35);
    halo = max(halo, outline(point, unit) * reach * 0.35);
    halo = max(halo, motes(point, unit) * 0.8);
    halo *= 1.0 - ink;
    vec3 color = mix(palette[1], inkColor, ink);
    // As a glyph turns to text, its rune shatters into a few motes that drift up and fade.
    float age = passed - lead;
    float shards = 0.0;
    if (holdSeconds > 0.0 && age > 0.0 && age < 0.9) {
        for (int i = 0; i < 8; i++) {
            float k = float(i);
            if (k >= shardCount) break;
            vec2 from = vec2(center + (hash(vec2(center, k + 20.0)) - 0.5) * 6.0 * unit,
                textSize.y * (0.3 + 0.5 * hash(vec2(center, k + 30.0))));
            vec2 at = from + vec2((hash(vec2(center, k + 40.0)) - 0.5) * 4.0 * unit,
                -age * (8.0 + 6.0 * hash(vec2(center, k + 50.0))) * unit);
            shards = max(shards, (1.0 - smoothstep(0.0, 0.9 * unit, length(point - at))) * (1.0 - age / 0.9));
        }
    }
    color = mix(color, light, shards);
    outputColor(mix(color, light, spark), max(max(max(ink, halo), spark), shards));
}
