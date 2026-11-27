#version 120

varying vec3 surfacePoint;
varying float facing;

void main() {
    surfacePoint = normalize(gl_Vertex.xyz);
    vec3 eyePoint = (gl_ModelViewMatrix * gl_Vertex).xyz;
    vec3 eyeNormal = normalize((gl_ModelViewMatrix * vec4(surfacePoint, 0.0)).xyz);
    facing = dot(eyeNormal, normalize(-eyePoint));
    gl_Position = gl_ModelViewProjectionMatrix * gl_Vertex;
}
