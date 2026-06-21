#version 150

uniform sampler2D DiffuseSampler;
uniform vec2 InSize;
uniform float Time;            // auto (PostPass), 0..1

uniform float DesatAmount;     // 0..1 : désaturation vers le luminance
uniform float WaveAmount;      // 0..1 : distorsion ondulée des UV
uniform float BlurAmount;      // 0..1 : flou box léger
uniform float VignetteAmount;  // 0..1 : assombrissement des bords
// NB : le grain « neige TV » (NoiseAmount) est appliqué dans une passe SÉPARÉE après le ghosting
// (itfollows_presence_grain) pour qu'il ne s'accumule pas dans la boucle de rémanence.

in vec2 texCoord;
out vec4 fragColor;

void main() {
    vec2 uv = texCoord;

    // --- Distorsion wave : décale l'UV par des sinus animés (bien visible) ---
    if (WaveAmount > 0.0) {
        float a = 0.001 * WaveAmount;
        uv.x += sin(uv.y * 26.0 + Time * 10.0) * a;
        uv.y += cos(uv.x * 22.0 + Time * 9.0) * a;
        // On évite que la distorsion échantillonne hors de l'écran (sinon liserés noirs aux bords).
        uv = clamp(uv, 0.0, 1.0);
    }

    // --- Flou : box-blur (gated, rayon marqué) ---
    vec3 col;
    if (BlurAmount > 0.0) {
        vec2 px = (1.0 / InSize) * (BlurAmount * 5.0);
        col  = texture(DiffuseSampler, uv).rgb * 0.30;
        col += texture(DiffuseSampler, uv + vec2( px.x, 0.0)).rgb * 0.14;
        col += texture(DiffuseSampler, uv + vec2(-px.x, 0.0)).rgb * 0.14;
        col += texture(DiffuseSampler, uv + vec2(0.0,  px.y)).rgb * 0.14;
        col += texture(DiffuseSampler, uv + vec2(0.0, -px.y)).rgb * 0.14;
        col += texture(DiffuseSampler, uv + vec2( px.x,  px.y)).rgb * 0.035;
        col += texture(DiffuseSampler, uv + vec2(-px.x,  px.y)).rgb * 0.035;
        col += texture(DiffuseSampler, uv + vec2( px.x, -px.y)).rgb * 0.035;
        col += texture(DiffuseSampler, uv + vec2(-px.x, -px.y)).rgb * 0.035;
    } else {
        col = texture(DiffuseSampler, uv).rgb;
    }

    // --- Désaturation : vers le luminance ---
    if (DesatAmount > 0.0) {
        float lum = dot(col, vec3(0.299, 0.587, 0.114));
        col = mix(col, vec3(lum), DesatAmount);
    }

    // --- Vignette : radial centré, corrigé de l'aspect (couvre tout l'écran, look CRT) ---
    // Sans correction d'aspect, le « cercle » UV devient une ellipse étirée sur écran large
    // et l'assombrissement paraît décentré / sur une moitié seulement.
    if (VignetteAmount > 0.0) {
        vec2 d = texCoord - vec2(0.5);
        d.x *= InSize.x / InSize.y;                          // aspect → vrai cercle centré
        float maxR = 0.5 * length(vec2(InSize.x / InSize.y, 1.0)); // distance centre → coin
        float dd = length(d) / maxR;                        // 0 au centre, 1 aux coins
        col *= 1.0 - smoothstep(0.45, 1.0, dd) * VignetteAmount;
    }

    fragColor = vec4(col, 1.0);
}
