#version 120
#include "common.glsl"

// Draws a captured mask in one color, matching vanilla text: the shadow is the color at a quarter brightness.
void main() {
    float alpha = coverage(position());
    if (shadowPass) {
        if (alpha * opacity <= 0.001) discard;
        gl_FragColor = vec4(palette[0] * 0.25, clamp(alpha * opacity, 0.0, 1.0));
        return;
    }
    outputColor(palette[0], alpha);
}
