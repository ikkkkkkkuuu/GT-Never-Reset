#version 120

uniform float age;
varying vec3 surfacePoint;
varying float facing;

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

void main() {
    float rotation = age * 0.037;
    float sine = sin(rotation);
    float cosine = cos(rotation);
    vec3 point = normalize(surfacePoint);
    point.xz = mat2(cosine, sine, -sine, cosine) * point.xz;
    vec3 field = point * 5.8;
    vec3 drift = vec3(0.0, age * 0.027, age * -0.019);
    vec3 warp = vec3(
        noise(field * 0.72 + drift + vec3(4.7, 0.0, 5.3)),
        noise(field * 0.72 - drift + vec3(0.0, 6.8, 2.7)),
        noise(field * 0.72 + drift.yzx + vec3(3.4, 5.9, 0.0)));
    vec3 plasma = field + (warp - 0.5) * 1.8;

    float sheet = noise(plasma + drift) * 0.69
        + noise(plasma * 2.23 - drift) * 0.23
        + noise(plasma * 5.12 + drift.zxy) * 0.08;
    float distanceToVein = abs(sheet - 0.50);
    float glow = 1.0 - smoothstep(0.045, 0.17, distanceToVein);
    float vein = 1.0 - smoothstep(0.018, 0.063, distanceToVein);
    float violet = smoothstep(0.35, 0.67, noise(plasma * 0.63 + vec3(7.2, 3.4, 11.6)));
    float flecks = smoothstep(0.78, 0.93, noise(plasma * 9.6 + drift));

    vec3 deep = mix(vec3(0.016, 0.042, 0.52), vec3(0.24, 0.015, 0.57), violet);
    vec3 emissive = mix(vec3(0.05, 0.70, 1.0), vec3(0.80, 0.11, 1.0), violet);
    vec3 hot = mix(vec3(0.71, 1.0, 1.0), vec3(1.0, 0.72, 1.0), violet);
    vec3 color = deep * (0.84 + 0.27 * sheet) + emissive * (0.37 * glow + 0.34 * flecks);
    color = mix(color, hot, vein * 0.96);
    float limb = pow(1.0 - clamp(facing, 0.0, 1.0), 3.0);
    color += vec3(0.17, 0.48, 0.61) * limb * (0.58 + 0.42 * glow);
    gl_FragColor = vec4(min(color, vec3(1.0)), 1.0);
}
