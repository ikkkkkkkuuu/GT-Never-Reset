#version 120
#include "common.glsl"

float smoothUnion(float a, float b, float k) {
    float h = clamp(0.5 + 0.5 * (b - a) / k, 0.0, 1.0);
    return mix(b, a, h) - k * h * (1.0 - h);
}

// Distance to a hanging strand that narrows from topRadius at its root to bottomRadius at its end.
float strand(vec2 point, vec2 top, float reach, float topRadius, float bottomRadius) {
    float t = clamp((point.y - top.y) / max(reach, 0.001), 0.0, 1.0);
    return length(point - vec2(top.x, top.y + t * reach)) - mix(topRadius, bottomRadius, t);
}

// Glow of metal by temperature: black-red when cool, through red and orange, to white-hot.
vec3 incandescence(float temperature, vec3 white, vec3 orange, vec3 ember) {
    vec3 cold = ember * 0.25;
    if (temperature < 0.4) return mix(cold, ember, temperature / 0.4);
    if (temperature < 0.7) return mix(ember, orange, (temperature - 0.4) / 0.3);
    return mix(orange, white, (temperature - 0.7) / 0.3);
}

void main() {
    float unit = textSize.y / 9.0;
    vec2 point = snapToHalfPixel(position());
    float base = 7.5 * unit;
    // Beads of molten metal swell under the strokes, snap off with a spray of sparks, and cool as they fall.
    float count = min(14.0, max(4.0, textSize.x / (5.0 * unit)));
    float slice = textSize.x / count;
    float sag = 0.0;
    float drip = 0.0;
    float dripTemperature = 0.0;
    float sparks = 0.0;
    float warmth = 0.0;
    for (int i = 0; i < 14; i++) {
        float k = float(i);
        if (k >= count) break;
        float x = -1.0;
        float rootY = base;
        for (int t = 0; t < 3; t++) {
            float candidate = (floor((k + 0.1 + 0.8 * hash(vec2(k, 1.0 + float(t)))) * slice / unit) + 0.5) * unit;
            if (x < 0.0 && coverage(vec2(candidate, base - 0.5 * unit)) > 0.5) x = candidate;
            if (x < 0.0 && coverage(vec2(candidate, base - 1.5 * unit)) > 0.5) {
                x = candidate;
                rootY = base - unit;
            }
        }
        if (x < 0.0) continue;
        float cycleLength = 2.6 + 1.6 * hash(vec2(k, 2.0));
        float phase = fract(time / cycleLength + hash(vec2(k, 3.0)));
        float grow = smoothstep(0.0, 0.6, phase);
        float fall = clamp((phase - 0.62) / 0.22, 0.0, 1.0);
        float gone = step(0.84, phase);
        float reach = grow * (1.5 + 2.0 * hash(vec2(k, 4.0))) * unit;
        float bulbRadius = mix(0.6, 1.2, grow) * unit;
        float neckRadius = mix(0.9, 0.4, smoothstep(0.45, 0.62, phase)) * unit;
        float pull = (1.0 - fall) * grow * exp(-pow((point.x - x) / (2.0 * unit), 2.0));
        sag = max(sag, pull * 1.4 * unit);
        // The bead hangs from where the bent stroke bottom now is.
        float rootDepth = rootY / textSize.y;
        rootY += noise(vec2(x / unit * 0.4, time * 0.5)) * rootDepth * rootDepth * 1.4 * unit
            + (rootY - 4.0 * unit) * 0.15 * (0.5 + 0.5 * noise(vec2(x / unit * 0.3 + 5.0, time * 0.35)))
            + (1.0 - fall) * grow * 1.4 * unit * rootDepth;
        rootY = floor(rootY / (0.5 * unit) + 0.5) * 0.5 * unit;
        vec2 root = vec2(x, rootY - 0.5 * unit);
        vec2 snapAt = vec2(x, rootY + reach + bulbRadius * 0.6);
        vec2 bulb = snapAt + vec2(0.0, fall * fall * 6.0 * unit);
        bool attached = phase < 0.62;
        float strandReach = attached ? bulb.y - root.y : reach * (1.0 - smoothstep(0.62, 0.7, phase));
        float strandDistance = strand(point, root, strandReach, 1.4 * unit, attached ? neckRadius : 0.35 * unit);
        vec2 offset = point - bulb;
        float dropDistance = length(vec2(offset.x, offset.y * (offset.y < 0.0 ? 0.75 : 1.0))) - bulbRadius * (1.0 - 0.25 * fall) + gone * 99.0;
        float shape = attached ? smoothUnion(strandDistance, dropDistance, 0.9 * unit) : min(strandDistance, dropDistance);
        if (shape < 0.0) {
            drip = 1.0;
            bool falling = !attached && dropDistance < strandDistance;
            float rim = smoothstep(-0.6 * unit, 0.0, shape);
            // Attached metal stays near the strokes' heat; a falling bead cools fast and grows a dark crust.
            float heat = falling ? 0.78 - fall * 0.62 : 0.85 - clamp((point.y - rootY) / (5.0 * unit), 0.0, 1.0) * 0.15;
            float crust = falling ? step(0.55, noise(offset / unit * 1.6 + k * 3.0)) * smoothstep(0.2, 0.6, fall) : 0.0;
            dripTemperature = heat - rim * 0.15 - crust * 0.3;
        }
        // Sparks spray out when the bead snaps off.
        float since = (phase - 0.62) * cycleLength;
        if (since > 0.0 && since < 0.7) {
            for (int s = 0; s < 4; s++) {
                float angle = 3.14159 * (0.15 + 0.7 * hash(vec2(k * 7.0 + float(s), floor(time / cycleLength))));
                float speed = (5.0 + 5.0 * hash(vec2(float(s), k + 9.0))) * unit;
                vec2 spark = snapAt + vec2(cos(angle), -sin(angle) * 0.8) * speed * since + vec2(0.0, 14.0 * unit * since * since);
                spark = snapToHalfPixel(spark);
                sparks = max(sparks, step(max(abs(point.x - spark.x), abs(point.y - spark.y)), 0.35 * unit) * step(since, 0.6 - 0.15 * float(s)));
            }
        }
        warmth = max(warmth, (1.0 - smoothstep(bulbRadius, bulbRadius + 1.5 * unit, length(offset))) * 0.3 * (1.0 - fall) * grow);
    }
    // The strokes run and droop like liquid metal.
    float depth = clamp(point.y / textSize.y, 0.0, 1.0);
    float droop = noise(vec2(point.x / unit * 0.4, time * 0.5)) * depth * depth * 1.4 * unit;
    float stretch = max(point.y - 4.0 * unit, 0.0) * 0.15 * (0.5 + 0.5 * noise(vec2(point.x / unit * 0.3 + 5.0, time * 0.35)));
    float wobble = sin(point.y / unit * 0.9 + point.x / unit * 0.2 + time * 1.2) * 0.35 * unit * depth;
    vec2 bend = vec2(wobble, droop + stretch + sag * depth);
    vec2 source = point - floor(bend / (0.5 * unit) + 0.5) * 0.5 * unit;
    if (drawShadow(source)) return;
    float ink = coverage(source);
    vec3 white = palette[0];
    vec3 orange = paletteCount > 1 ? palette[1] : palette[0];
    vec3 ember = paletteCount > 2 ? palette[2] : orange * 0.4;
    // Strokes glow hottest in their middle, cooler at their edges; dark dross floats across the surface.
    float flow = noise(vec2(point.x / unit * 0.8, point.y / unit * 0.5 - time * 1.0));
    float edge = ink * (1.0 - min(min(coverage(source + vec2(0.5 * unit, 0.0)), coverage(source - vec2(0.5 * unit, 0.0))),
        min(coverage(source + vec2(0.0, 0.5 * unit)), coverage(source - vec2(0.0, 0.5 * unit)))));
    float dross = step(0.76, noise(vec2(point.x / unit * 0.45 + time * 0.25, point.y / unit * 0.45)));
    float temperature = 0.95 + 0.1 * flow - 0.1 * depth - 0.12 * edge - 0.3 * dross;
    vec3 body = incandescence(clamp(temperature, 0.0, 1.0), white, orange, ember);
    vec3 dripColor = incandescence(clamp(dripTemperature, 0.0, 1.0), white, orange, ember);
    // Hot air shimmers just above the line as faint rising ripples.
    float rising = point.y < 0.0 ? (1.0 - clamp(-point.y / (3.0 * unit), 0.0, 1.0)) * 0.3
        * step(0.55, noise(vec2(point.x / unit * 0.6 + sin(point.y / unit * 1.5 + time * 5.0) * 0.6, point.y / unit * 0.8 + time * 3.0)))
        * step(0.0, point.x) * step(point.x, textSize.x) : 0.0;
    float halo = max((outline(source, unit) * 0.3 + outline(source, 2.5 * unit) * 0.1) * (1.0 - ink), rising);
    vec3 color = mix(ember, orange, clamp(max(warmth, rising * 3.0), 0.0, 1.0));
    color = mix(color, dripColor, drip);
    color = mix(color, body, ink);
    color = mix(color, vec3(1.0, 0.93, 0.55), sparks);
    outputColor(color, max(max(ink, drip), max(max(halo, warmth), sparks)));
}
