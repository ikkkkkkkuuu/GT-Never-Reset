#version 120
#include "common.glsl"

// One round trip of the black hole, left edge to right edge and back.
const float CYCLE_SECONDS = 8.0;

void main() {
    float unit = textSize.y / 9.0;
    // Pixel centers of the font's half-pixel grid, so the whole scene reads as pixel art.
    vec2 point = snapToHalfPixel(position());
    // An unseen black hole starts at the left edge, crosses the line and comes back, bobbing a little.
    float phase = time * 6.283185307 / CYCLE_SECONDS;
    vec2 hole = vec2(textSize.x * 0.5 - (textSize.x * 0.5 + 4.0 * unit) * cos(phase),
        textSize.y * 0.45 + sin(phase * 3.0) * 0.8 * unit);
    vec2 offset = point - hole;
    float radius = length(offset);
    float horizon = 2.4 * unit;
    vec2 away = offset / max(radius, 0.001);
    // Light passing close is pulled in, so text near the hole is drawn from points nearer to it.
    vec2 source = point - away * (16.0 * unit * unit / max(radius, horizon));
    float inside = 1.0 - smoothstep(horizon - 0.4 * unit, horizon, radius);
    if (drawShadow(source)) return;
    float ink = coverage(source) * (1.0 - inside);
    // Light climbing out of the well loses energy, so text near the hole dims and warms.
    float pull = 1.0 - smoothstep(horizon, horizon * 4.0, radius);

    // A tilted, flattened accretion disk spins around the hole, brighter on the side turning toward the viewer.
    float tilt = 0.25;
    vec2 turned = vec2(offset.x * cos(tilt) + offset.y * sin(tilt), offset.y * cos(tilt) - offset.x * sin(tilt));
    vec2 squashed = vec2(turned.x, turned.y * 3.2);
    float spread = length(squashed);
    float band = smoothstep(horizon * 1.15, horizon * 1.5, spread) * (1.0 - smoothstep(horizon * 2.4, horizon * 3.6, spread));
    float angle = atan(squashed.y, squashed.x);
    float streak = 0.5 + 0.5 * noise(vec2(angle * 3.0 - time * 4.0, spread / unit * 0.9));
    float facing = 0.45 + 0.55 * (0.5 + 0.5 * cos(angle));
    float disk = band * streak * facing;
    float front = step(0.0, turned.y);

    // A thin photon ring hugs the horizon.
    float ring = (1.0 - smoothstep(0.0, 0.55 * unit, abs(radius - horizon * 1.2))) * (1.0 - inside);

    // A few motes spiral in and vanish at the horizon.
    float motes = 0.0;
    for (int i = 0; i < 5; i++) {
        float k = float(i);
        float fall = fract(time * 0.3 + hash(vec2(k, 1.0)));
        float orbit = mix(horizon * 5.0, horizon, fall);
        float spin = hash(vec2(k, 2.0)) * 6.283185307 + fall * 9.0;
        vec2 at = hole + vec2(cos(spin), sin(spin) / 2.2) * orbit;
        motes = max(motes, (1.0 - smoothstep(0.0, 0.6 * unit, length(point - at))) * (1.0 - fall * 0.7));
    }

    vec3 textColor = palette[0];
    vec3 hot = paletteCount > 1 ? palette[1] : vec3(1.0, 0.7, 0.3);
    vec3 glow = paletteCount > 2 ? palette[2] : palette[0];
    vec3 diskColor = mix(hot, vec3(1.0, 0.95, 0.85), streak * facing * 0.6);
    float halo = outline(source, unit) * 0.3 * (1.0 - inside);
    vec3 color = mix(glow, mix(textColor, hot * 0.75, pull * 0.6) * (1.0 - pull * 0.35), ink);
    float alpha = max(ink, halo);
    // The back of the disk sits behind the hole, the front crosses over it.
    color = mix(color, diskColor, disk * (1.0 - front) * (1.0 - inside));
    alpha = max(alpha, disk * (1.0 - front) * (1.0 - inside));
    color = mix(color, vec3(0.0), inside);
    alpha = max(alpha, inside * 0.95);
    color = mix(color, diskColor, disk * front);
    alpha = max(alpha, disk * front);
    color = mix(color, mix(hot, vec3(1.0), 0.6), ring);
    alpha = max(alpha, ring);
    color = mix(color, vec3(1.0, 0.92, 0.8), motes * (1.0 - inside));
    alpha = max(alpha, motes * (1.0 - inside));
    outputColor(color, alpha);
}
