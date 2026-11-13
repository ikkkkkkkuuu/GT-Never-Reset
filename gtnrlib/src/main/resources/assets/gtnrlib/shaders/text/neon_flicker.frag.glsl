#version 120
#include "common.glsl"

uniform float faultShare;

// Only some windows hold a burst, so the flicker never settles into a beat. Strikes get denser toward the end of a
// burst, like a tube catching again.
float burst(float seed, float period, float chance, float share, float rate, float dim) {
    float shifted = time / period + hash(vec2(seed, 5.0));
    float phase = fract(shifted);
    if (phase > share || hash(vec2(floor(shifted), seed + 9.0)) > chance) return 1.0;
    float strike = hash(vec2(floor(time * rate), seed * 31.0 + 1.0));
    return strike < 0.3 + 0.6 * phase / share ? 1.0 : dim;
}

// Each glyph rolls again every few seconds, so a different faultShare of the glyphs drops out each time.
float glyphLight(float glyph) {
    float lit = 0.93 + 0.07 * noise(vec2(time * 3.0, glyph * 7.0));
    lit *= burst(glyph * 53.0 + 2.0, 2.2, faultShare, 0.35, 16.0, 0.05);
    return lit;
}

float glyphAt(vec2 point) {
    vec2 coord = (point + padding) / maskSize;
    return glyphMetadata(vec2(clamp(coord.x, 0.0, 1.0), 1.0 - clamp(coord.y, 0.0, 1.0))).b;
}

// Light reaching this pixel takes the brightness of the glyph it comes from, so a dim glyph's halo fades with it
// instead of being cut off where the next glyph's column begins. Rings average their probes rather than taking the
// brightest one, so the halo blurs smoothly and grows where strokes crowd together.
float ring(vec2 point, float radius, float turn) {
    float result = 0.0;
    for (int i = 0; i < 16; i++) {
        float angle = (float(i) + turn) * 0.3926990817;
        vec2 probe = point + vec2(cos(angle), sin(angle)) * radius;
        float ink = coverage(probe);
        if (ink <= 0.0) continue;
        float lit = glyphLight(glyphAt(probe));
        result += ink * lit * lit;
    }
    return result / 16.0;
}

void main() {
    if (drawShadow()) return;
    vec2 point = position();
    float unit = textSize.y / 9.0;
    // The whole sign dips once every four seconds, so a short loop or recording always catches it.
    float whole = burst(0.0, 4.0, 1.0, 0.08, 24.0, 0.4);
    vec3 tube = palette[0];
    vec3 halo = paletteCount > 1 ? palette[1] : palette[0];
    float ink = coverage(point);
    float near = ring(point, unit, 0.0);
    float light = near * 1.6 + ring(point, 2.0 * unit, 0.5) * 0.9 + ring(point, 3.2 * unit, 0.25) * 0.5
        + ring(point, 4.6 * unit, 0.75) * 0.28 + ring(point, 6.2 * unit, 0.0) * 0.14;
    float glow = (1.0 - exp(-light * 2.4)) * 0.85 * whole * whole * (1.0 - ink);
    vec3 haze = mix(halo, mix(halo, tube, 0.6), clamp(near * 2.5, 0.0, 1.0));
    float lit = whole * glyphLight(glyphAt(point));
    vec3 glass = mix(tube * 0.22, mix(tube, vec3(1.0), 0.55), lit);
    outputColor(mix(haze, glass, ink), max(ink, glow));
}
