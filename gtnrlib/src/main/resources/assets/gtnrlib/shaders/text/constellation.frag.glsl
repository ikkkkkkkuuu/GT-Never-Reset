#version 120
#include "common.glsl"

// Star k of this epoch sits on a stroke inside its slice of the line; a slice with no stroke gets no star.
vec3 star(float k, float slice, float epoch, float unit) {
    for (int t = 0; t < 4; t++) {
        vec2 at = vec2((k + 0.2 + 0.6 * hash(vec2(k, epoch + float(t) * 7.0))) * slice,
            (1.0 + 6.0 * hash(vec2(k + 11.0, epoch + float(t) * 3.0))) * unit);
        at = snapToHalfPixel(at);
        if (coverage(at) > 0.5) return vec3(at, 1.0);
    }
    return vec3(0.0);
}

// A pixel star: a bright core, and for the brighter ones a four-point cross whose arms breathe.
float sparkle(vec2 offset, float size, float twinkle, float unit) {
    vec2 d = abs(offset);
    float core = step(max(d.x, d.y), 0.5 * unit);
    float arm = (1.5 + 1.5 * size) * (0.6 + 0.4 * twinkle) * unit;
    float rays = max(step(d.y, 0.25 * unit) * step(d.x, arm), step(d.x, 0.25 * unit) * step(d.y, arm));
    float falloff = 1.0 - 0.4 * max(d.x, d.y) / max(arm, 0.001);
    return max(core, rays * falloff * step(0.35, size));
}

void main() {
    float unit = textSize.y / 9.0;
    vec2 point = snapToHalfPixel(position());
    if (drawShadow(point)) return;
    float ink = coverage(point);
    // Every six seconds a new constellation draws itself from left to right, holds, then fades out star by star.
    float cycle = time / 6.0;
    float epoch = floor(cycle);
    float clock = fract(cycle) * 6.0;
    float slice = 6.0 * unit;
    float count = max(1.0, ceil(textSize.x / slice));
    float pace = min(0.18, 3.0 / count);
    float stars = 0.0;
    float lines = 0.0;
    float head = 0.0;
    float light = 0.0;
    float k0 = floor(point.x / slice) - 2.0;
    for (int j = 0; j < 5; j++) {
        float k = k0 + float(j);
        if (k < 0.0 || k >= count) continue;
        vec3 a = star(k, slice, epoch, unit);
        if (a.z < 0.5) continue;
        float fade = 1.0 - smoothstep(4.6 + k / count * 0.8, 5.2 + k / count * 0.8, clock);
        float lit = smoothstep(k * pace, k * pace + 0.2, clock) * fade;
        float twinkle = 0.5 + 0.5 * sin(time * (3.0 + 2.0 * hash(vec2(k, epoch + 40.0))) + k * 2.3);
        float size = hash(vec2(k, epoch + 50.0));
        stars = max(stars, sparkle(point - a.xy, size, twinkle, unit) * lit * (0.7 + 0.3 * twinkle));
        stars = max(stars, (1.0 - smoothstep(0.0, 1.8 * unit, length(point - a.xy))) * 0.3 * lit);
        light = max(light, (1.0 - smoothstep(0.0, 5.0 * unit, length(point - a.xy))) * lit);
        // Each star links to the next one, and now and then also to the one after, closing a triangle.
        for (int b = 1; b <= 2; b++) {
            float step2 = float(b);
            if (k + step2 >= count) break;
            if (b == 2 && hash(vec2(k, epoch + 60.0)) > 0.35) break;
            vec3 c = star(k + step2, slice, epoch, unit);
            if (c.z < 0.5) continue;
            float grow = clamp((clock - k * pace - 0.1 - (step2 - 1.0) * 0.3) / (0.2 * step2), 0.0, 1.0);
            if (grow <= 0.0) continue;
            vec2 end = mix(a.xy, c.xy, grow);
            float shimmer = 0.85 + 0.15 * sin(time * 3.0 - point.x / unit * 0.3);
            lines = max(lines, step(segmentDistance(point, a.xy, end), 0.4 * unit) * shimmer * fade);
            if (grow < 1.0) head = max(head, step(length(point - end), 0.6 * unit) * fade);
        }
    }
    // Faint dust stars twinkle in the gaps around the text.
    vec2 cell = floor(point / (3.0 * unit));
    vec2 spot = (cell + 0.2 + 0.6 * vec2(hash(cell + 3.1), hash(cell + 7.7))) * 3.0 * unit;
    spot = snapToHalfPixel(spot);
    float dust = step(hash(cell + 1.3), 0.3) * step(max(abs(point.x - spot.x), abs(point.y - spot.y)), 0.25 * unit)
        * (0.5 + 0.5 * sin(time * 2.0 + hash(cell) * 6.28)) * (1.0 - ink) * 0.7
        * step(-unit, point.y) * step(point.y, textSize.y + unit) * step(-unit, point.x) * step(point.x, textSize.x + unit);
    vec3 text = palette[0];
    vec3 lineColor = paletteCount > 1 ? palette[1] : vec3(0.6, 0.8, 1.0);
    vec3 glow = paletteCount > 2 ? palette[2] : palette[0];
    float halo = outline(point, unit) * 0.25 * (1.0 - ink);
    vec3 color = mix(glow, mix(text * 0.7, min(text * 1.2, vec3(1.0)), light), ink);
    color = mix(color, lineColor, max(lines, dust));
    color = mix(color, vec3(1.0), max(stars, head));
    outputColor(color, max(max(ink * 0.85, halo), max(max(lines, dust), max(stars, head))));
}
