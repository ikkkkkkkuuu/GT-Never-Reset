#version 120
#include "common.glsl"

void main() {
    float unit = textSize.y / 9.0;
    vec2 point = snapToHalfPixel(position());
    // A flux wave runs through the line every two and a half seconds and makes the strokes ripple.
    float waveX = fract(time / 2.5) * (textSize.x + 16.0 * unit) - 8.0 * unit;
    float wave = exp(-pow((point.x - waveX) / (3.0 * unit), 2.0));
    float ripple = floor(sin(point.x / unit * 1.4 - time * 9.0) * wave * 2.0 + 0.5) * 0.5 * unit;
    // Every five seconds a rift tears open somewhere along the line and pushes the strokes apart.
    // Each line seeds its own tears from its width and a few mask samples, which stay put while a tooltip moves.
    float lineSeed = fract(textSize.x * 0.1373) * 61.0 + coverage(vec2(textSize.x * 0.31, 4.5 * unit)) * 7.0
        + coverage(vec2(textSize.x * 0.67, 3.5 * unit)) * 13.0;
    float shifted = time / 5.0 + 0.3 * hash(vec2(lineSeed, 8.0));
    float epoch = floor(shifted);
    float tear = epoch + lineSeed * 17.0;
    float clock = fract(shifted) * 5.0;
    float open = smoothstep(0.0, 0.4, clock) * (1.0 - smoothstep(1.4, 2.2, clock));
    // Each tear has its own slant, span and zigzag, with one short crack branching off it.
    float riftX = (0.15 + 0.7 * hash(vec2(tear, 9.0))) * textSize.x;
    float slant = (hash(vec2(tear, 10.0)) - 0.5) * 6.0 * unit;
    float riftTop = (-1.5 + 3.0 * hash(vec2(tear, 11.0))) * unit;
    float riftBottom = textSize.y + (1.0 - 3.0 * hash(vec2(tear, 12.0))) * unit;
    float spacing = (1.0 + hash(vec2(tear, 13.0))) * unit;
    float node = floor((point.y - riftTop) / spacing);
    float between = fract((point.y - riftTop) / spacing);
    float zigA = (hash(vec2(node, tear + 20.0)) - 0.5) * 2.2 * unit;
    float zigB = (hash(vec2(node + 1.0, tear + 20.0)) - 0.5) * 2.2 * unit;
    float span = clamp((point.y - riftTop) / (riftBottom - riftTop), 0.0, 1.0);
    float crack = riftX + slant * (span - 0.5) + mix(zigA, zigB, between);
    float inside = step(riftTop, point.y) * step(point.y, riftBottom);
    float gape = open * (0.3 + 0.7 * hash(vec2(node, tear + 30.0))) * sin(3.14159 * span);
    float side = point.x < crack ? -1.0 : 1.0;
    float push = gape * unit * inside * (1.0 - smoothstep(2.0 * unit, 6.0 * unit, abs(point.x - crack)));
    vec2 source = point - vec2(side * push, ripple);
    if (drawShadow(source)) return;
    float ink = coverage(source);
    float pit = step(0.72, noise(vec2(source.x / unit * 1.3, source.y / unit * 1.3) + vec2(time * 0.15, 0.0)));
    float eaten = ink * pit;
    float branchAt = mix(riftTop, riftBottom, 0.3 + 0.4 * hash(vec2(tear, 14.0)));
    float branchSide = hash(vec2(tear, 15.0)) < 0.5 ? -1.0 : 1.0;
    vec2 branchRoot = vec2(riftX + slant * ((branchAt - riftTop) / (riftBottom - riftTop) - 0.5), branchAt);
    vec2 branchEnd = branchRoot + vec2(branchSide * (2.0 + 2.0 * hash(vec2(tear, 16.0))), (hash(vec2(tear, 17.0)) - 0.5) * 3.0) * unit * smoothstep(0.3, 0.8, open);
    float branch = open * step(segmentDistance(point, branchRoot, branchEnd), 0.3 * unit) * step(0.4, open);
    float rift = max(open * inside * step(abs(point.x - crack), 0.5 * gape * unit + 0.25 * unit), branch);
    float riftGlow = open * inside * (1.0 - smoothstep(0.0, 2.5 * unit, abs(point.x - crack))) * 0.5;
    // Tendrils root on a stroke, uncurl out of it, writhe with a hooked tip and pull back in.
    float tendril = 0.0;
    float rim = 0.0;
    float tip = 0.0;
    float count = min(10.0, max(2.0, textSize.x / (8.0 * unit)));
    for (int i = 0; i < 10; i++) {
        float k = float(i);
        if (k >= count) break;
        float up = hash(vec2(k, 4.0)) < 0.5 ? -1.0 : 1.0;
        float rootY = up < 0.0 ? 1.5 * unit : 6.5 * unit;
        vec2 root = vec2(-1.0);
        for (int t = 0; t < 4; t++) {
            float x = (k + 0.15 + 0.7 * hash(vec2(k, 1.0 + float(t)))) * textSize.x / count;
            if (root.x < 0.0 && coverage(vec2(x, rootY)) > 0.5) root = vec2(x, rootY);
        }
        if (root.x < 0.0) continue;
        float life = smoothstep(0.1, 0.9, 0.5 - 0.5 * cos(time * (0.8 + 0.4 * hash(vec2(k, 2.0))) + k * 1.7));
        if (life < 0.02) continue;
        float size = hash(vec2(k, 3.0));
        float reach = (1.5 + 5.5 * size * size) * unit * life;
        float lean = (hash(vec2(k, 5.0)) - 0.5) * 0.8;
        float curl = (hash(vec2(k, 6.0)) < 0.5 ? -1.0 : 1.0) * (1.2 + 1.3 * (1.0 - life));
        vec2 previous = root;
        float near = length(point - root) / ((1.0 + 0.4 * life) * unit);
        float along = 0.0;
        for (int s = 1; s <= 10; s++) {
            float f = float(s) / 10.0;
            float heading = lean + curl * f * f + sin(time * 2.2 + f * 4.0 + k * 1.3) * 0.45 * f;
            vec2 next = previous + vec2(sin(heading), up * cos(heading)) * reach / 10.0;
            float reachOut = segmentDistance(point, previous, next) / (mix(1.0, 0.3, f) * (0.7 + 0.3 * size) * unit);
            if (reachOut < near) {
                near = reachOut;
                along = f;
            }
            previous = next;
        }
        if (near < 1.0) {
            float throb = 1.0 - smoothstep(0.0, 0.12, abs(fract(time * 0.7 + 0.37 * k) - along));
            tendril = 1.0;
            rim = max(rim, step(0.55, near));
            tip = max(tip, max(smoothstep(0.6, 1.0, along), throb * 0.8));
        }
    }
    // Flux wisps seep out of the strokes and drift upward, swaying.
    float wisp = 0.0;
    float wisps = min(12.0, max(3.0, textSize.x / (5.0 * unit)));
    for (int i = 0; i < 12; i++) {
        float k = float(i);
        if (k >= wisps) break;
        float rate = 0.35 + 0.25 * hash(vec2(k, 6.0));
        float phase = fract(time * rate + hash(vec2(k, 7.0)));
        float cycleIndex = floor(time * rate + hash(vec2(k, 7.0)));
        float x = (k + hash(vec2(k, cycleIndex))) * textSize.x / wisps;
        vec2 at = vec2(x + sin(phase * 6.0 + k) * 1.2 * unit, mix(5.0 * unit, -3.5 * unit, phase));
        at = snapToHalfPixel(at);
        float size = mix(1.2, 0.5, phase) * unit;
        wisp = max(wisp, step(max(abs(point.x - at.x), abs(point.y - at.y)), size) * (1.0 - phase) * (1.0 - ink));
    }
    vec3 dark = palette[0];
    vec3 sick = paletteCount > 1 ? palette[1] : vec3(0.6, 0.2, 0.9);
    vec3 glow = paletteCount > 2 ? palette[2] : sick * 0.5;
    vec3 flux = paletteCount > 3 ? palette[3] : vec3(1.0, 0.4, 0.85);
    float pulse = 0.75 + 0.25 * sin(time * 1.5);
    float halo = (outline(source, unit) * 0.65 + outline(source, 2.0 * unit) * 0.25) * pulse * (1.0 - ink);
    float body = max(ink * (1.0 - eaten * 0.85), tendril);
    // Strokes keep a sick glow along their edges, so the darkened text still reads.
    float edge = ink * (1.0 - min(min(coverage(source + vec2(0.5 * unit, 0.0)), coverage(source - vec2(0.5 * unit, 0.0))),
        min(coverage(source + vec2(0.0, 0.5 * unit)), coverage(source - vec2(0.0, 0.5 * unit)))));
    vec3 core = mix(dark, sick, 0.25 + 0.15 * pulse);
    vec3 color = mix(glow, mix(core, sick, edge * 0.85), body);
    color = mix(color, dark * 0.6, eaten);
    vec3 flesh = mix(mix(core, sick, rim * 0.85), flux, tip * 0.75);
    color = mix(color, flesh, tendril * (1.0 - ink));
    color = mix(color, flux, wave * max(edge, 0.5 * ink));
    color = mix(color, flux, max(riftGlow, wisp));
    color = mix(color, mix(flux, vec3(1.0), 0.6), rift);
    outputColor(min(color, vec3(1.0)), max(max(body, halo), max(rift, max(riftGlow, wisp))));
}
