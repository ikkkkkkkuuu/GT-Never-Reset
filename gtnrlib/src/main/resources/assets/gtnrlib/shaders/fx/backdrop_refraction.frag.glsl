#version 120

uniform sampler2D sceneColor;
uniform sampler2D sceneDepth;
uniform vec2 centerPixel;
uniform vec2 copyOrigin;
uniform vec2 copySize;
uniform vec2 textureSize;
uniform float strength;

varying vec2 uv;

void main() {
    float radius = length(uv);
    if (radius >= 1.0) discard;

    float pull = strength * smoothstep(0.25, 0.42, radius) * (1.0 - smoothstep(0.42, 1.0, radius));
    vec2 warpedPixel = gl_FragCoord.xy - (gl_FragCoord.xy - centerPixel) * pull;
    vec2 warpedUv = clamp(warpedPixel - copyOrigin, vec2(0.5), copySize - vec2(0.5)) / textureSize;
    // Keep the source background behind the lens, even when the warped ray crosses a foreground block.
    if (texture2D(sceneDepth, warpedUv).r + 0.000005 < gl_FragCoord.z) discard;

    vec2 originalUv = (gl_FragCoord.xy - copyOrigin) / textureSize;
    gl_FragColor = vec4(texture2D(sceneColor, warpedUv).rgb, texture2D(sceneColor, originalUv).a);
}
