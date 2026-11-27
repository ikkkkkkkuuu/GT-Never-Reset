#version 120

uniform float age;
uniform float coronaPass;
varying vec3 surfacePoint;
varying float facing;
varying vec2 coronaPoint;

float hash(vec3 point) {
    point = fract(point * vec3(0.1031, 0.11369, 0.13787));
    point += dot(point, point.yzx + 19.19);
    return fract((point.x + point.y) * point.z);
}

float noise(vec3 point) {
    vec3 cell = floor(point);
    vec3 blend = fract(point);
    blend = blend * blend * (3.0 - 2.0 * blend);
    vec2 lower = vec2(
        mix(hash(cell), hash(cell + vec3(1.0, 0.0, 0.0)), blend.x),
        mix(hash(cell + vec3(0.0, 1.0, 0.0)), hash(cell + vec3(1.0, 1.0, 0.0)), blend.x));
    vec2 upper = vec2(
        mix(hash(cell + vec3(0.0, 0.0, 1.0)), hash(cell + vec3(1.0, 0.0, 1.0)), blend.x),
        mix(hash(cell + vec3(0.0, 1.0, 1.0)), hash(cell + vec3(1.0, 1.0, 1.0)), blend.x));
    return mix(mix(lower.x, lower.y, blend.y), mix(upper.x, upper.y, blend.y), blend.z);
}

vec3 surfaceColor() {
    float rotation = age * 0.029;
    float sine = sin(rotation);
    float cosine = cos(rotation);
    vec3 point = normalize(surfacePoint);
    point.xz = mat2(cosine, sine, -sine, cosine) * point.xz;
    vec3 field = point * 6.5;
    vec3 drift = vec3(0.0, age * 0.024, age * -0.017);
    vec3 warp = vec3(
        noise(field * 0.72 + drift + vec3(4.7, 0.0, 5.3)),
        noise(field * 0.72 - drift + vec3(0.0, 6.8, 2.7)),
        noise(field * 0.72 + drift.yzx + vec3(3.4, 5.9, 0.0)));
    vec3 plasma = field + (warp - 0.5) * 2.0;
    float plate = noise(plasma + drift);
    float branch = noise(plasma * 2.36 - drift);
    float fragments = noise(plasma * 5.9 + drift.zxy);
    float grain = noise(plasma * 15.0 + drift);
    float grainVisibility = 1.0 - smoothstep(1.0, 3.6, length(fwidth(plasma * 15.0)));

    float sheet = 0.58 * plate + 0.25 * branch + 0.17 * fragments;
    float width = mix(0.038, 0.145, smoothstep(0.29, 0.78, branch));
    float groove = abs(sheet - 0.50) + (grain - 0.47) * 0.035 * grainVisibility;
    float glow = 1.0 - smoothstep(width * 0.55, width + 0.10, groove);
    float fissure = 1.0 - smoothstep(width * 0.59, width, groove);
    float holes = smoothstep(0.51, 0.78, fragments) * (0.45 + 0.55 * grain * grainVisibility);
    float whiteHeat = fissure * (1.0 - holes * 0.75);
    float violet = smoothstep(0.49, 0.76, noise(plasma * 0.61 + vec3(7.2, 3.4, 11.6)));
    float flecks = smoothstep(0.76, 0.90, grain) * grainVisibility;

    vec3 deep = mix(vec3(0.012, 0.027, 0.56), vec3(0.16, 0.018, 0.69), violet);
    vec3 emissive = mix(vec3(0.045, 0.53, 1.0), vec3(0.65, 0.085, 1.0), violet);
    vec3 hot = mix(vec3(0.90, 0.98, 1.0), vec3(1.0, 0.84, 1.0), violet);
    vec3 color = deep * (0.76 + 0.34 * branch) + emissive * (0.26 * glow + 0.36 * flecks);
    color = mix(color, hot, whiteHeat);
    float limb = pow(1.0 - clamp(facing, 0.0, 1.0), 3.0);
    color = mix(color, vec3(0.70, 0.94, 1.0), limb * (0.29 + 0.54 * whiteHeat));
    return min(color, vec3(1.0));
}

vec4 coronaColor() {
    float radius = length(coronaPoint);
    float distanceToLimb = radius - 1.0;
    if (distanceToLimb < -0.024 || distanceToLimb > 0.54) discard;

    vec2 direction = coronaPoint / radius;
    vec2 tangent = vec2(-direction.y, direction.x);
    float time = age * 0.058;
    float plume = noise(vec3(direction * 11.2, time * 0.68));
    float fork = noise(vec3(direction * 26.0 + 9.3, time * 0.55));
    float jet = smoothstep(0.32, 0.77, 0.75 * plume + 0.25 * fork);
    float extent = 0.075 + 0.07 * fork + 0.34 * jet * jet;
    float envelope = 1.0 - smoothstep(extent * 0.29, extent, distanceToLimb);

    vec2 ribbon = coronaPoint + tangent * max(distanceToLimb, 0.0) * (0.42 - fork);
    float strand = noise(vec3(ribbon * 29.0, time + radius * 5.2));
    float grain = noise(vec3(ribbon * 69.0 + 7.0, time * 1.36));
    float shreds = smoothstep(0.29, 0.75, 0.71 * strand + 0.29 * grain);
    float ridge = 1.0 - smoothstep(0.038, 0.15, abs(strand - 0.48) + 0.065 * (1.0 - grain));
    float limb = 1.0 - smoothstep(0.0, 0.085, max(distanceToLimb, 0.0));
    float light = envelope * (0.16 + 0.57 * shreds + 0.27 * ridge);
    light *= smoothstep(-0.024, 0.006, distanceToLimb);
    vec3 blue = mix(vec3(0.04, 0.30, 1.0), vec3(0.31, 0.88, 1.0), shreds);
    vec3 color = mix(blue, vec3(0.91, 0.99, 1.0), limb * (0.50 + 0.50 * ridge));
    return vec4(color, light * (0.67 + 0.28 * limb));
}

void main() {
    gl_FragColor = coronaPass > 0.5 ? coronaColor() : vec4(surfaceColor(), 1.0);
}
