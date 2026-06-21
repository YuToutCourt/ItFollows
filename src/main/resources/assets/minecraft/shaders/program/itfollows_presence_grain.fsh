#version 150

uniform sampler2D DiffuseSampler;  // image déjà composée (scène + ghosting)
uniform vec2 InSize;
uniform float Time;                // auto (PostPass) — anime la neige
uniform float NoiseAmount;         // 0..1 : densité du grain « neige TV »

in vec2 texCoord;
out vec4 fragColor;

float hash(vec2 p) {
    return fract(sin(dot(p, vec2(127.1, 311.7))) * 43758.5453);
}

void main() {
    vec3 col = texture(DiffuseSampler, texCoord).rgb;

    // Grain plein écran, uniforme et FRAIS chaque frame (pas de rémanence : appliqué après le ghosting).
    if (NoiseAmount > 0.0) {
        vec2 cell = floor(texCoord * InSize);
        float n = hash(cell + Time * 60.0);
        if (n > 1.0 - 0.12 * NoiseAmount) {
            col = mix(col, vec3(step(0.5, hash(cell * 1.7 + Time * 97.0))), 0.9);
        }
    }

    fragColor = vec4(col, 1.0);
}
