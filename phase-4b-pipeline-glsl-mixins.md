# Phase 4b — Pipeline GLSL & mixins FOV/caméra (effets visuels lourds)

> Suite de la **Phase 4a** (présence : audio + overlays HUD). Ici on remplace les *approximations*
> HUD (voile gris, jitter, flashs 1 frame) par les **vrais** effets de rendu : désaturation, flou,
> distorsion *wave*, *ghosting*, bruit blanc — via un **pipeline de post-processing GLSL** — plus la
> **vision tunnel (FOV)**, la **respiration visuelle (zoom)** et le **screen shake caméra** — via
> **mixins**.
>
> La piste **audio** de la 4a est validée et ne bouge pas. Ce document ne concerne que le **visuel**.

---

## 1. Objectif & effets visés

Rappel du cahier (piste « présence de l'entité », par bandes de distance, **cible traquée seulement**) :

| Bande | Effets visuels visés | Technique 4b |
|---|---|---|
| **30 b** | vignette légère, **micro-désaturation**, **bruit blanc** rare | shader (passe désat + noise) |
| **15 b** | **flou** intermittent, **distorsion wave**, ombres qui tremblent | shader (blur + wave) |
| **10 b** | silhouettes 1 frame, **screen shake** léger, **ghosting** | mixin caméra + shader (persistence) |
| **5 b** | **vision tunnel (FOV)**, **respiration visuelle (zoom)**, micro-freeze | mixin `getFov` |
| **< 3 b** | (audio cut, déjà géré 4a) | — |

La **piste fatigue** (20/10/5 %) peut, en option, réutiliser une partie de ces effets à faible
intensité (léger flou/désat quand on est à bout). On garde ça pour la fin (tuning).

---

## 2. Comment marche le post-processing dans Minecraft 1.20.1

Minecraft a **déjà** un système de post-processing complet (celui des vues « creeper / araignée /
enderman » en spectateur). On le réutilise plutôt que de réinventer un framebuffer.

### Vocabulaire (mappings Mojang — ceux du projet)
- **`PostChain`** (`net.minecraft.client.renderer.PostChain`) : une *chaîne* de passes plein écran.
  - Construction : `new PostChain(textureManager, resourceManager, mainRenderTarget, location)`.
  - `resize(w, h)` quand la fenêtre change ; `process(float partialTicks)` chaque frame ; `close()`.
  - Champ privé `List<PostPass> passes` (on y accède via un **accessor mixin** pour piloter les uniforms).
- **`PostPass`** : une passe = un programme GLSL + une cible de sortie. `getEffect()` → `EffectInstance`.
- **`EffectInstance`** : le programme compilé. `safeGetUniform("Nom")` → **`Uniform`**.
- **`Uniform`** : `set(float)`, `set(float,float)`, `set(float,float,float,float)`, etc.
- **`RenderTarget`** : un framebuffer. `minecraft.getMainRenderTarget()` = l'écran rendu.

### Les fichiers d'une chaîne (dans `resources`)
Trois niveaux, tous sous le namespace `itfollows` :

```
assets/itfollows/shaders/post/presence.json                 ← la CHAÎNE (chargée explicitement, ns itfollows OK)
assets/minecraft/shaders/program/itfollows_presence.json    ← le PROGRAMME (réfs vsh/fsh + uniforms + samplers)
assets/minecraft/shaders/program/itfollows_presence.vsh     ← vertex shader plein écran (générique)
assets/minecraft/shaders/program/itfollows_presence.fsh     ← fragment shader (le cœur des effets)
```
> ⚠️ **Piège majeur 1.20.1** : la CHAÎNE peut vivre dans notre namespace (on la charge nous-mêmes via
> `new ResourceLocation("itfollows","shaders/post/presence.json")`). Mais le **PROGRAMME** d'une passe
> est résolu par `EffectInstance` qui construit littéralement `shaders/program/<name>.json` → toujours
> dans le namespace **minecraft**, et un `:` dans `<name>` est illégal. Donc le programme + ses `.vsh/.fsh`
> doivent aller dans `assets/minecraft/shaders/program/`, avec un **nom unique** (`itfollows_presence`)
> pour éviter toute collision. (`shaders/core/` = *core shaders* `ShaderInstance`, autre système.)

### Uniforms « gratuits » vs « custom »
- **Auto-fournis** par le moteur à chaque passe : `ProjMat`, `InSize`, `OutSize`, `ScreenSize`,
  et surtout **`Time`** (incrémenté par `PostChain.process`). On s'en sert pour animer wave/noise.
- **Custom** (`PresenceIntensity`, `DesatAmount`, `WaveAmount`, `BlurAmount`, `GhostAmount`,
  `NoiseAmount`, `VignetteAmount`) : déclarés dans le JSON programme **et** dans le `.fsh`, et qu'il
  faut **réécrire nous-mêmes chaque frame** avant `process(...)`.

### Principe clé : « passthrough à 0 »
Une `PostChain` exécute **toujours** toutes ses passes. Pour « désactiver » un effet, on met son
uniform à **0** et le shader fait `mix(original, effet, amount)`. À `amount = 0`, l'image ressort
**intacte**. Donc on peut laisser la chaîne tourner et tout piloter par uniforms — ou, pour économiser,
ne pas appeler `process` quand toutes les intensités sont nulles.

---

## 3. Architecture retenue

On gère **nous-mêmes** la `PostChain` (on ne passe **pas** par `gameRenderer.loadEffect`, qui est
volé/écrasé par les vues d'entités en spectateur et se réinitialise au respawn).

```
ClientPresenceState (4a, distance entité)         FatigueState client (4a)
            │                                              │
            ▼                                              ▼
   PresenceVisuals  ──── calcule les niveaux par effet (desat, wave, blur, ghost, fov, shake)
            │  (mapping distance/bandes → [0,1])
   ┌────────┴───────────────────────────────┐
   ▼                                         ▼
PresencePostProcessor (PostChain)     PresenceFovState / PresenceShakeState
   • set uniforms chaque frame          (lus par les mixins getFov / Camera.setup)
   • process() après le monde
```

- **`PresenceVisuals`** (client) : une seule source qui transforme la distance (et la fatigue) en
  niveaux `[0,1]` par effet, **lissés** (réutilise `advanceCloseness` de la 4a). Tout le monde lit ça.
- **`PresencePostProcessor`** (client) : possède la `PostChain`, gère création/resize/reload, écrit
  les uniforms depuis `PresenceVisuals`, appelle `process()` au bon moment.
- **`PresenceFovState`** / **`PresenceShakeState`** : petits porteurs de valeurs lues par les mixins.

---

## 4. Fichiers à créer

### Resources (shaders)
```
src/main/resources/assets/itfollows/shaders/post/presence.json                 (chaîne)
src/main/resources/assets/minecraft/shaders/program/itfollows_presence.json     (programme)
src/main/resources/assets/minecraft/shaders/program/itfollows_presence.vsh
src/main/resources/assets/minecraft/shaders/program/itfollows_presence.fsh
```
> Le nom de passe dans la chaîne = `"itfollows_presence"` (sans `:`), et `"vertex"/"fragment"` du
> programme = `"itfollows_presence"`. Tout est résolu en namespace `minecraft`.
*(passes avancées « ghosting » : ajouter un programme `presence_ghost.json` + `.fsh` et une cible
persistante — voir §7.)*

### Java client
```
src/client/java/.../client/render/PresencePostProcessor.java   // gère la PostChain
src/client/java/.../client/PresenceVisuals.java                // mapping distance/fatigue → niveaux
src/client/java/.../client/PresenceFovState.java               // valeur FOV (tunnel + respiration)
src/client/java/.../client/PresenceShakeState.java             // amplitude de shake
```

### Mixins client (package `io.github.yutoutcourt.itfollows.mixin.client`)
```
src/client/java/.../mixin/client/PostChainAccessor.java        // @Accessor pour List<PostPass> passes
src/client/java/.../mixin/client/GameRendererFovMixin.java     // getFov → tunnel + respiration
src/client/java/.../mixin/client/CameraShakeMixin.java         // setup → screen shake
```
À déclarer dans `src/client/resources/itfollows.client.mixins.json` (liste `"client"`, déjà prête,
package déjà `...mixin.client`).

---

## 5. Le shader (cœur des effets)

### 5.1 `shaders/core/presence.vsh` (vertex plein écran, générique)
Identique aux post-shaders vanilla (un quad plein écran) :
```glsl
#version 150
in vec4 Position;
uniform mat4 ProjMat;
uniform vec2 OutSize;
out vec2 texCoord;
void main() {
    vec4 outPos = ProjMat * vec4(Position.xy, 0.0, 1.0);
    gl_Position = vec4(outPos.xy, 0.2, 1.0);
    texCoord = Position.xy / OutSize;
}
```

### 5.2 `shaders/core/presence.fsh` (fragment — effets gated par uniforms)
```glsl
#version 150
uniform sampler2D DiffuseSampler;
uniform vec2 InSize;
uniform float Time;            // auto
uniform float DesatAmount;     // 0..1
uniform float WaveAmount;      // 0..1
uniform float BlurAmount;      // 0..1  (rayon en px = BlurAmount * BLUR_MAX)
uniform float NoiseAmount;     // 0..1
uniform float VignetteAmount;  // 0..1
in vec2 texCoord;
out vec4 fragColor;

float hash(vec2 p){ return fract(sin(dot(p, vec2(127.1,311.7))) * 43758.5453); }

void main() {
    vec2 uv = texCoord;

    // --- Distorsion wave : décale l'UV par des sinus animés ---
    if (WaveAmount > 0.0) {
        float a = 0.004 * WaveAmount;
        uv.x += sin(uv.y * 30.0 + Time * 8.0) * a;
        uv.y += cos(uv.x * 28.0 + Time * 7.0) * a;
    }

    // --- Flou : petit box-blur séparable léger (gated) ---
    vec3 col;
    if (BlurAmount > 0.0) {
        vec2 px = (1.0 / InSize) * (BlurAmount * 2.5);
        col = texture(DiffuseSampler, uv).rgb * 0.4;
        col += texture(DiffuseSampler, uv + vec2( px.x, 0.0)).rgb * 0.15;
        col += texture(DiffuseSampler, uv + vec2(-px.x, 0.0)).rgb * 0.15;
        col += texture(DiffuseSampler, uv + vec2(0.0,  px.y)).rgb * 0.15;
        col += texture(DiffuseSampler, uv + vec2(0.0, -px.y)).rgb * 0.15;
    } else {
        col = texture(DiffuseSampler, uv).rgb;
    }

    // --- Désaturation : vers le luminance ---
    if (DesatAmount > 0.0) {
        float lum = dot(col, vec3(0.299, 0.587, 0.114));
        col = mix(col, vec3(lum), DesatAmount);
    }

    // --- Bruit blanc : pixels aléatoires animés ---
    if (NoiseAmount > 0.0) {
        float n = hash(floor(texCoord * InSize) + Time * 60.0);
        if (n > 1.0 - 0.06 * NoiseAmount) {
            col = mix(col, vec3(step(0.5, hash(uv + Time))), 0.8);
        }
    }

    // --- Vignette (optionnel : on peut la garder en HUD) ---
    if (VignetteAmount > 0.0) {
        float d = distance(texCoord, vec2(0.5));
        col *= 1.0 - smoothstep(0.35, 0.75, d) * VignetteAmount;
    }

    fragColor = vec4(col, 1.0);
}
```

### 5.3 `shaders/program/presence.json` (déclare le programme + uniforms)
```json
{
  "blend": { "func": "add", "srcrgb": "one", "dstrgb": "zero" },
  "vertex": "itfollows:presence",
  "fragment": "itfollows:presence",
  "attributes": [ "Position" ],
  "samplers": [ { "name": "DiffuseSampler" } ],
  "uniforms": [
    { "name": "ProjMat",        "type": "matrix4x4", "count": 16, "values": [ 1,0,0,0, 0,1,0,0, 0,0,1,0, 0,0,0,1 ] },
    { "name": "InSize",         "type": "float", "count": 2, "values": [ 1.0, 1.0 ] },
    { "name": "OutSize",        "type": "float", "count": 2, "values": [ 1.0, 1.0 ] },
    { "name": "Time",           "type": "float", "count": 1, "values": [ 0.0 ] },
    { "name": "DesatAmount",    "type": "float", "count": 1, "values": [ 0.0 ] },
    { "name": "WaveAmount",     "type": "float", "count": 1, "values": [ 0.0 ] },
    { "name": "BlurAmount",     "type": "float", "count": 1, "values": [ 0.0 ] },
    { "name": "NoiseAmount",    "type": "float", "count": 1, "values": [ 0.0 ] },
    { "name": "VignetteAmount", "type": "float", "count": 1, "values": [ 0.0 ] }
  ]
}
```

### 5.4 `shaders/post/presence.json` (la chaîne)
Une passe de l'écran (`minecraft:main`) vers une cible tampon `swap`, puis recopie vers l'écran :
```json
{
  "targets": [ "swap" ],
  "passes": [
    {
      "name": "itfollows_presence",
      "intarget": "minecraft:main",
      "outtarget": "swap",
      "uniforms": []
    },
    {
      "name": "blit",
      "intarget": "swap",
      "outtarget": "minecraft:main",
      "uniforms": []
    }
  ]
}
```
> ⚠️ **Piège** : le `"name"` d'une passe est résolu en `minecraft:shaders/program/<name>.json` et un `:`
> dans le nom est illégal. D'où le nom **unique sans namespace** `"itfollows_presence"` + fichiers dans
> `assets/minecraft/shaders/program/`. `blit` = programme vanilla de recopie.

---

## 6. Piloter les uniforms chaque frame (Java)

### 6.1 Accessor mixin (lire les passes)
```java
@Mixin(PostChain.class)
public interface PostChainAccessor {
    @Accessor("passes") List<PostPass> itfollows$getPasses();
}
```

### 6.2 `PresencePostProcessor` (squelette)
```java
public final class PresencePostProcessor {
    private static final ResourceLocation CHAIN =
            new ResourceLocation("itfollows", "shaders/post/presence.json");
    private PostChain chain;
    private int lastW = -1, lastH = -1;

    public void renderAfterWorld(float tickDelta) {
        Minecraft mc = Minecraft.getInstance();
        PresenceVisuals.tick(tickDelta);                 // met à jour les niveaux lissés
        if (!PresenceVisuals.anyActive()) {              // rien à faire → on n'allume pas le pipeline
            return;
        }
        ensureLoaded(mc);
        ensureSize(mc);
        setUniforms();
        chain.process(tickDelta);
        mc.getMainRenderTarget().bindWrite(false);       // re-bind écran pour le HUD ensuite
    }

    private void ensureLoaded(Minecraft mc) {
        if (chain == null) {
            try {
                chain = new PostChain(mc.getTextureManager(), mc.getResourceManager(),
                        mc.getMainRenderTarget(), CHAIN);
                lastW = lastH = -1;
            } catch (IOException e) { /* log + désactiver proprement */ }
        }
    }

    private void ensureSize(Minecraft mc) {
        int w = mc.getWindow().getWidth(), h = mc.getWindow().getHeight();
        if (w != lastW || h != lastH) { chain.resize(w, h); lastW = w; lastH = h; }
    }

    private void setUniform(String name, float value) {
        for (PostPass pass : ((PostChainAccessor) chain).itfollows$getPasses()) {
            Uniform u = pass.getEffect().safeGetUniform(name);
            u.set(value);                                 // safeGetUniform renvoie un no-op si absent
        }
    }

    private void setUniforms() {
        setUniform("DesatAmount",    PresenceVisuals.desat());
        setUniform("WaveAmount",     PresenceVisuals.wave());
        setUniform("BlurAmount",     PresenceVisuals.blur());
        setUniform("NoiseAmount",    PresenceVisuals.noise());
        setUniform("VignetteAmount", PresenceVisuals.vignette());
    }

    public void reload() { if (chain != null) { chain.close(); chain = null; } }
}
```

### 6.3 Quand appeler `process()` ?
Au point « après le monde, avant le HUD ». Le plus simple sans mixin :
```java
WorldRenderEvents.LAST.register(ctx -> PRESENCE_PP.renderAfterWorld(ctx.tickDelta()));
```
> Alternative plus « propre » : un mixin dans `GameRenderer.render` juste avant le rendu du GUI.
> `WorldRenderEvents.LAST` suffit pour commencer ; surveiller la compat (voir §9).

### 6.4 Recharger au reload de resources
Une `PostChain` doit être recréée quand on recharge les packs :
```java
ResourceManagerHelper.get(PackType.CLIENT_RESOURCES)
    .registerReloadListener(id("presence_pp"), (SimpleSynchronousResourceReloadListener) rm -> PRESENCE_PP.reload());
```

---

## 7. Ghosting (image rémanente) — passe avancée

Le *ghosting* = mélanger la frame courante avec les **frames précédentes**. Technique vanilla
(`phosphor.json`) : une **cible persistante** (non effacée entre frames) sur laquelle on accumule.

Plan :
1. Ajouter une cible `ghost` qui **n'est pas vidée** chaque frame.
2. Programme `presence_ghost.fsh` : `out = mix(current, previousGhost, GhostAmount)` puis on réécrit
   `ghost` avec ce résultat (persistance).
3. `GhostAmount` piloté par la bande 10 b.

> C'est la partie la plus délicate (gestion de la persistance de cible). À faire **en dernier**,
> une fois desat/wave/blur validés. Si trop coûteux/instable, on peut s'en passer (effet « bonus »).

---

## 8. Mixins FOV & caméra

### 8.1 Vision tunnel + respiration (FOV) — `GameRendererFovMixin`
```java
@Mixin(GameRenderer.class)
public class GameRendererFovMixin {
    @Inject(method = "getFov(Lnet/minecraft/client/Camera;FZ)D", at = @At("RETURN"), cancellable = true)
    private void itfollows$presenceFov(Camera camera, float tickDelta, boolean useFovSetting,
                                       CallbackInfoReturnable<Double> cir) {
        float factor = PresenceFovState.factor(tickDelta);   // 1.0 = neutre
        if (factor != 1.0f) {
            cir.setReturnValue(cir.getReturnValue() * factor);
        }
    }
}
```
- **Tunnel** : bande 5 b → `factor` descend vers ~0.75 (FOV réduit = on « rétrécit » la vision).
- **Respiration** : ajoute une oscillation lente `1 + sin(t)*0.02` (zoom léger in/out), amplitude ∝ proximité.
- `PresenceFovState.factor` combine les deux, lissé.

### 8.2 Screen shake — `CameraShakeMixin`
```java
@Mixin(Camera.class)
public abstract class CameraShakeMixin {
    @Shadow protected abstract void setRotation(float yaw, float pitch);
    @Shadow public abstract float getYRot();
    @Shadow public abstract float getXRot();

    @Inject(method = "setup", at = @At("TAIL"))
    private void itfollows$shake(BlockGetter level, Entity entity, boolean detached,
                                boolean thirdPersonReverse, float partialTicks, CallbackInfo ci) {
        float amp = PresenceShakeState.amplitude();          // degrés, 0 = aucun
        if (amp > 0.0f) {
            RandomSource r = entity.level().getRandom();
            float dYaw   = (r.nextFloat() * 2.0f - 1.0f) * amp;
            float dPitch = (r.nextFloat() * 2.0f - 1.0f) * amp;
            setRotation(getYRot() + dYaw, getXRot() + dPitch);
        }
    }
}
```
- Bande 10 b → `amplitude` léger (≈ 0.2–0.6°), monte un peu en se rapprochant.
- ⚠️ `setRotation` est `private` côté vanilla : `@Shadow` fonctionne (Mixin shadow les privés). Si
  l'obfuscation pose souci, fallback : `@Invoker` sur `setRotation`/`move`.

### 8.3 Le « micro-freeze » (0,1 s)
Pas d'API de gel propre. On garde l'approximation 4a (flash 1 frame), **ou** on simule une fraction
de seconde de rendu figé en court-circuitant le rendu du monde (risqué) → **on reste sur le flash**.
À documenter comme « volontairement approximé ».

---

## 9. Risques & pièges

| Risque | Détail / parade |
|---|---|
| **Iris / OptiFine** | Un shaderpack remplace le pipeline → notre `PostChain` peut ne pas s'appliquer. **Détecter** Iris (FabricLoader `isModLoaded("iris")`) et désactiver le pipeline GLSL (garder les mixins FOV/caméra qui, eux, marchent). |
| **Sodium** | OK en général (ne touche pas au post). À tester quand même. |
| **Resize / plein écran** | Toujours `resize()` quand `window.getWidth/Height` change, sinon image étirée/crash. |
| **Reload resources (F3+T)** | Recréer la `PostChain` (listener §6.4), sinon textures/targets invalides. |
| **Re-bind du framebuffer** | Après `process()`, re-`bindWrite` le main target pour que le HUD se dessine au bon endroit. |
| **Uniforms non réécrits** | Les customs gardent leur dernière valeur ; bien **tout** remettre chaque frame (y compris à 0 pour éteindre). |
| **Coût GPU** | Passes plein écran chaque frame. Léger, mais **ne pas** lancer `process()` si tout est à 0 (cf. `anyActive()`). |
| **`getFov` signature** | Vérifier le descripteur exact au build (`(Lnet/minecraft/client/Camera;FZ)D`). |
| **Mal des transports** | FOV + shake = nausée réelle pour les joueurs. Amplitudes **faibles** + tout exposé en config. |

---

## 10. Étapes d'implémentation (ordre conseillé)

> Incrémental : à chaque étape, on **build + test en jeu** avant de passer à la suivante.

1. **Fondation** : `PresenceVisuals` (mapping distance→niveaux, pour l'instant juste `DesatAmount`),
   les 4 fichiers shader, `PostChainAccessor`, `PresencePostProcessor`, branchement `WorldRenderEvents.LAST`.
   → *Test* : forcer la traque, vérifier que l'écran se **désature** en s'approchant et redevient normal en s'éloignant.
2. **Wave** : ajouter `WaveAmount` (bande 15 b). *Test* : ondulation visible vers 15 b.
3. **Blur** : ajouter `BlurAmount` (bande 15 b). *Test* : flou léger intermittent.
4. **Noise + vignette shader** (optionnel) : migrer le bruit blanc / la vignette du HUD vers le shader.
5. **FOV mixin** : tunnel (5 b) + respiration. *Test* : rétrécissement + léger zoom in/out très proche.
6. **Camera shake mixin** : (10 b). *Test* : tremblement discret, pas vomitif.
7. **Ghosting** (avancé) : cible persistante + passe de mélange (10 b).
8. **Nettoyage 4a** ✅ : voile gris désat + jitter déjà migrés au shader aux étapes précédentes.
   Assombrissement de **proximité** retiré du HUD (couvert par la vignette radiale du shader) — on
   ne garde que l'assombrissement de **fatigue**. **Gardés** : silhouette 1 frame, pulsation cardiaque,
   et flash micro-freeze (cf. §8.3, approximation volontaire).
9. **Compat & config** ✅ : détection Iris/Oculus dans `PresencePostProcessor`
   (`SHADER_MOD_PRESENT` → court-circuite le GLSL, garde les mixins FOV/caméra, log une fois).
   `presenceNoiseMax` ajouté en config (le grain n'était plus en dur). Amplitudes shader/FOV/shake
   déjà toutes en config.
10. **Tuning multijoueur** : régler les bandes et amplitudes au ressenti (cf. §11).

---

## 11. Config à ajouter (Phase 4b)

Dans `ItFollowsConfig` (mêmes conventions que la 4a) :
```java
// Phase 4b : effets de rendu (GLSL + FOV/caméra)
public boolean presenceGlslEnabled = true;        // coupé auto si Iris détecté
public boolean presenceFovEnabled = true;
public boolean presenceShakeEnabled = true;

public float presenceDesatMax = 0.7f;             // désat max au contact
public float presenceWaveMax = 1.0f;
public float presenceBlurMax = 1.0f;
public float presenceGhostMax = 0.6f;

public float presenceFovTunnelFactor = 0.78f;     // FOV min (≈ 22 % de réduction)
public float presenceFovBreathAmplitude = 0.02f;  // zoom respiration
public float presenceShakeMaxDegrees = 0.5f;      // amplitude de shake max
```
Le mapping bande→niveau vit dans `PresenceVisuals` (lissé), pas en dur dans les shaders.

---

## 12. Tests (récap)

- **Pipeline GLSL** : `/itfollows stalker spawn <cible>` puis la cible avance/recule → desat (30 b) →
  wave+blur (15 b) → ghosting (10 b). Vérifier le **passthrough propre** quand l'entité s'éloigne
  (image 100 % normale, aucun résidu).
- **FOV** : vision tunnel + respiration à < 5 b, retour fluide au FOV normal en s'éloignant.
- **Shake** : tremblement discret à ~10 b (non nauséeux).
- **Non-cibles** : aucun effet de rendu (la distance envoyée est `-1`).
- **Compat** : tester **avec Sodium** ; tester **avec Iris** → le GLSL se coupe, FOV/shake restent.
- **Robustesse** : F3+T (reload), changement de résolution / plein écran, mort/respawn de la cible.

---

### Notes
- La **piste audio** (4a) reste la source principale de direction (panning L/R). Le visuel 4b
  ajoute le « ressenti » mais ne doit jamais **révéler** la position de l'entité aux non-cibles.
- Toutes les amplitudes sont volontairement **basses** par défaut : on monte au tuning, jamais l'inverse.
