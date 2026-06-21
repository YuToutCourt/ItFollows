#version 150

// Passe de rémanence (ghosting, Phase 4b §7). Mélange la frame courante (post desat/wave/blur)
// avec l'accumulation de la frame précédente via un filtre exponentiel (EMA) :
//   final = mix(current, previous, GhostAmount)   puis   previous <- final   (blit de feedback)
// Propriétés : à GhostAmount = 0 la sortie vaut exactement la frame courante (passthrough propre,
// aucun résidu) ; à GhostAmount > 0 les objets en mouvement laissent une traînée qui s'estompe
// d'elle-même (pas de saturation, contrairement à un max() sans décroissance).

uniform sampler2D DiffuseSampler;  // frame courante (sortie de la passe d'effets)
uniform sampler2D PrevSampler;     // accumulation de la frame précédente (cible "ghost" persistante)

uniform float GhostAmount;         // 0..1 : force de la rémanence

in vec2 texCoord;
out vec4 fragColor;

void main() {
    vec3 curr = texture(DiffuseSampler, texCoord).rgb;
    if (GhostAmount > 0.0) {
        vec3 prev = texture(PrevSampler, texCoord).rgb;
        curr = mix(curr, prev, GhostAmount);
    }
    fragColor = vec4(curr, 1.0);
}
