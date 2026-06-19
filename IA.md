# Plan IA — Rendre la traqueuse « super intelligente »

> Plan technique pour l'IA de `StalkerEntity`. Complète `cahier.md` (Phase 2).
> Cible : **Fabric 1.20.1**, mappings **Mojang officiels** (Mojmap) → les noms de classes ci-dessous
> sont ceux du code source de production (pas Yarn).
> Objectif : passer d'une poursuite **réactive** (« je bute, donc je casse ») à une poursuite
> **délibérée** (« je planifie le chemin le moins cher vers toi, mur ou pas »).
>
> **Astuce outillage** : `./gradlew genSources` décompile le client Mojmap. Garde-le ouvert pour
> vérifier les signatures exactes des méthodes vanilla citées ici (elles bougent entre versions).

---

## 0. État actuel (rappel)

L'IA de déplacement est correcte mais **réactive** :

- [`StalkerChaseGoal`](src/main/java/io/github/yutoutcourt/itfollows/entity/ai/StalkerChaseGoal.java) :
  `getNavigation().moveTo(target)` toutes les 10 ticks + assistance escalade quand `isDone()`.
- [`BreakObstacleGoal`](src/main/java/io/github/yutoutcourt/itfollows/entity/ai/BreakObstacleGoal.java) :
  ne s'arme **que** quand l'entité est physiquement bloquée (`isBlocked()`), et choisit le bloc à
  casser **gloutonnement** (`findObstacle`, lignes 135‑163), pas en lisant le chemin.
- [`GroundPathNavigation`](src/main/java/io/github/yutoutcourt/itfollows/entity/StalkerEntity.java#L106)
  vanilla : pathfinde **uniquement dans l'espace déjà navigable**. Devant un refuge scellé, aucun
  chemin n'existe → l'entité avance « au pif », bute, et la casse réactive prend le relais.

### Les 3 défauts qui empêchent la « super intelligence »
1. **Aucun plan à travers les murs** → choix glouton local qui rate les géométries coudées (contourner
   *puis* casser) et perce parfois le mauvais bloc.
2. **Aucune anticipation** : on vise la position *actuelle* de la cible, jamais où elle va.
3. **Escalade hackée** (`setWantedPosition` pour presser le mur quand `isDone()`) au lieu d'un
   mouvement planifié.

---

## 1. Décision : Baritone vs custom (tranchée)

### ❌ Rejeté : intégrer Baritone (« Boritone/Mobitone »)
Client-side & lié au `LocalPlayer` (inputs clavier simulés) ; **GPL‑3.0** (contamine tout le mod) ;
version-lock 1.12 (forks 1.20.1 fragiles) ; **surdimensionné** (A* longue distance + minage/build)
alors qu'on n'opère qu'à **courte portée** (bornée par la simulation via `HauntController`) ;
sémantique fantôme/invincible/dégâts-ciblés **absente** de Baritone.
→ **On garde l'idée, pas le code** : A* voxel où un bloc cassable est *traversable à un coût ∝ temps
de minage*.

### ✅ Étape 1 (recommandée) — `NodeEvaluator` custom + exécuteur lisant le `Path`
**Minecraft est déjà un A*.** On étend l'évaluateur de nœuds vanilla pour que les blocs cassables
soient **franchissables avec malus** au lieu de `BLOCKED`. Le A* vanilla route alors *à travers* les
murs quand c'est le moins cher, et l'exécuteur casse le bloc du nœud courant. ~80 % du résultat pour
~20 % du code. **Mais** : il y a un **piège géométrique** réel (cf. §2.2) — ce n'est pas un override
trivial de 10 lignes, et c'est le point qui décide du succès.

### 🔶 Étape 2 (repli conditionnel) — A* maison `StalkerPathfinder`
Si l'évaluateur vanilla plafonne (malus **binaire**, pas le **temps réel** de minage ; chutes
multi-blocs, parkour, tunnels coudés profonds mal modélisés), on écrit un A* dédié à coût par
primitive. **On ne le fait que si un scénario réel le force** (cf. §5). Ne pas sur-construire.

---

## 2. Étape 1 — détail technique

### 2.1 Surface d'API vanilla 1.20.1 (Mojmap)

Chaîne d'appel du pathfinding au sol :

```
GroundPathNavigation.createPathFinder(int maxVisitedNodes)
   └─ new PathFinder(NodeEvaluator nodeEvaluator, int maxVisitedNodes)
PathNavigation (ctor): maxVisitedNodes = Mth.floor(getAttributeValue(FOLLOW_RANGE) * 16)
PathFinder.findPath(region, mob, targets, maxRange, accuracy, searchDepthMultiplier)
   └─ NodeEvaluator.getNeighbors(Node[] out, Node node)         // voisins d'un nœud
        └─ WalkNodeEvaluator.findAcceptedNode(x,y,z, stepUp, floorLevel, dir, pathType)
             ├─ getFloorLevel(BlockPos)                          // hauteur du sol au nœud
             ├─ getCachedBlockType(Mob, x, y, z) → getBlockPathType(level, x,y,z, mob)
             └─ mob.getPathfindingMalus(BlockPathTypes)          // coût ; < 0 ⇒ nœud rejeté
```

Points clés sur les **malus** :
- `BlockPathTypes.BLOCKED` a un malus **−1.0** → le nœud est **rejeté** (jamais exploré).
- `WALKABLE` = 0.0, `OPEN` = 0.0, `WATER` = 8.0 (réglé à 0 chez nous), `DOOR_OPEN` = 0.0…
- Le mob peut **surcharger** un malus via `setPathfindingMalus(type, valeur)` (déjà utilisé pour
  `WATER = 0` dans [`StalkerEntity`](src/main/java/io/github/yutoutcourt/itfollows/entity/StalkerEntity.java#L83)).

### 2.2 ⚠️ Le piège géométrique (à comprendre *avant* de coder)

Réflexe naïf : « je renvoie `WALKABLE` pour un mur cassable ». **Ça ne fait pas ce qu'on croit.**
`findAcceptedNode` calcule `getFloorLevel()` = **surface supérieure** du solide ; avec la limite de
marche (`d - floorLevel > 1.125` → rejet), marquer un solide `WALKABLE` fait que l'A* veut **monter
dessus** (router *par-dessus* le mur), pas le **traverser**.

**Solution** : pour un bloc cassable, renvoyer **`OPEN`** (malus ≥ 0), pas `WALKABLE`. `OPEN` = « je
peux occuper cette case ». L'A* place alors un nœud *à l'intérieur* du mur ; le bloc **en dessous**
(solide) sert de sol → nœud debout valide → on traverse *à travers*. Conséquences à gérer :
- **Dégagement tête** : le nœud feet=`OPEN` ne vérifie pas toujours le bloc `y+1`. L'exécuteur devra
  casser **feet ET tête** du nœud cible (cf. 2.4), sinon collision physique au-dessus.
- **Diagonales** : `getNeighbors` rejette une diagonale si un des deux blocs de coin est `BLOCKED`.
  Avec nos coins en `OPEN`, l'A* peut couper en diagonale dans un mur → on bornera ça (préférer les
  pas cardinaux pour le tunnel, via malus diagonal plus cher).
- **Sécurité** : un bloc **incassable** (dureté < 0, ou > `stalkerBreakMaxHardness`) doit **rester
  `BLOCKED`** (bedrock, barrière) — sinon l'entité planifie un tunnel impossible et gèle.

### 2.3 `StalkerNodeEvaluator extends WalkNodeEvaluator`

Override le point d'évaluation par bloc. **Vérifier la signature exacte** sur les sources décompilées
(`genSources`) — en 1.20.1 c'est de l'ordre de `getBlockPathType(BlockGetter, int, int, int, Mob)` ;
sinon surcharger `getCachedBlockType(Mob, int, int, int)` qui l'enveloppe.

```java
public final class StalkerNodeEvaluator extends WalkNodeEvaluator {

    @Override
    public BlockPathTypes getBlockPathType(BlockGetter level, int x, int y, int z, Mob mob) {
        BlockPathTypes vanilla = super.getBlockPathType(level, x, y, z, mob);
        if (vanilla != BlockPathTypes.BLOCKED) {
            return vanilla; // air, eau, porte… : on ne touche pas
        }
        // Bloc plein et bloquant : est-il cassable dans nos règles ?
        BlockPos pos = new BlockPos(x, y, z);
        BlockState state = level.getBlockState(pos);
        if (isOpenableWood(state)) {
            return BlockPathTypes.DOOR_WOOD_CLOSED; // route à travers, ouverture quasi gratuite
        }
        float hardness = state.getDestroySpeed(/* niveau réel */ , pos); // BlockGetter ⇒ voir note
        float cap = ItFollowsConfig.get().stalkerBreakMaxHardness;
        boolean breakable = hardness >= 0.0f && (cap < 0.0f || hardness <= cap);
        return breakable ? BlockPathTypes.OPEN : BlockPathTypes.BLOCKED; // sinon vraiment infranchissable
    }
}
```

> Note `getDestroySpeed` : il prend un `BlockGetter`/`Level` + `BlockPos`. Dans l'évaluateur le monde
> est un `PathNavigationRegion` (`this.level`) — utilisable comme `BlockGetter`. Si une API exige le
> `Level`, passer par `this.mob.level()`.

**Malus = coût ∝ temps de casse.** On ne met pas le coût *dans* le type mais via le mob, par type.
Comme `OPEN` sert aussi à l'air normal, on ne peut pas lui coller un malus de casse global. Deux
options :
- **Simple (recommandé d'abord)** : malus `OPEN` **modéré et constant** (ex. `setPathfindingMalus(
  OPEN, 0)` reste pour l'air ; impossible de différencier). → on accepte un coût de tunnel *uniforme*
  et on règle la « réticence à creuser » via le **biais de l'exécuteur** (l'entité ne creuse que si la
  nav ne trouve pas de route en surface plus courte). En pratique le A* préfère déjà l'air car un
  tunnel allonge le chemin en nombre de nœuds.
- **Fin (si besoin)** : introduire un type custom n'est pas possible (enum vanilla figé) → c'est
  exactement la limite qui justifie l'**Étape 2** (A* maison où le coût d'arête = temps de casse réel).

Branchement de l'évaluateur (anonyme dans la nav, pour garder la double-navigation actuelle) :

```java
@Override
protected PathNavigation createNavigation(Level level) {
    GroundPathNavigation nav = new GroundPathNavigation(this, level) {
        @Override
        protected PathFinder createPathFinder(int maxVisitedNodes) {
            this.nodeEvaluator = new StalkerNodeEvaluator();
            this.nodeEvaluator.setCanPassDoors(true);
            this.nodeEvaluator.setCanOpenDoors(true);
            return new PathFinder(this.nodeEvaluator, maxVisitedNodes);
        }
    };
    nav.setCanFloat(true);
    nav.setCanOpenDoors(true);
    return nav;
}
```

### 2.4 Exécuteur conscient du chemin — `BreakObstacleGoal` v2

Aujourd'hui le goal **devine** le bloc. Demain il le **lit dans le `Path`** :

```java
Path path = stalker.getNavigation().getPath();
if (path != null && !path.isDone()) {
    BlockPos next = path.getNextNodePos();          // BlockPos du nœud courant à atteindre
    BlockPos head = next.above();
    // Casse exactement les blocs que le planificateur a décidé de traverser (feet + tête).
    BlockPos toBreak = pickSolidBreakable(next, head);
    if (toBreak != null) {
        breakProgressively(toBreak);                 // réutilise la casse progressive existante
        return;
    }
}
// repli : ancienne heuristique findObstacle() quand il n'y a pas de Path (cas dégénéré)
```

Bénéfices :
- Plus de choix glouton : on casse le bon bloc, **dans l'ordre du chemin** (escalier, tunnel coudé).
- On casse **feet + tête** → règle le défaut de dégagement vertical de `OPEN` (§2.2).
- L'ouverture bois reste prioritaire (porte/trappe/portillon) via `tryOpen`, déjà écrit.
- `markActivity()` continue d'alimenter l'anti-blocage existant.

> Garder le déclenchement « seulement si nav bloquée ou nœud solide » : si la nav avance en espace
> libre, le goal ne fait rien — la casse n'arrive qu'aux nœuds réellement pleins du chemin planifié.

### 2.5 Garde-fous perf (le vrai risque opérationnel) — chiffré

- **`FOLLOW_RANGE` actuel = 256** → `maxVisitedNodes = floor(256 × 16) = 4096` nœuds explorés **par
  recompute**, toutes les 10 ticks. C'est énorme et inutile : le ciblage est custom, `FOLLOW_RANGE`
  ne sert ici **qu'au pathfinding**. → **Le baisser à 48–64** (`maxVisitedNodes ≈ 768–1024`). La
  longue distance est déjà couverte par la **poursuite virtuelle** de `HauntController`.
- **`setMaxVisitedNodesMultiplier(float)`** sur la nav : knob fin (défaut 1.0) pour plafonner encore.
- **Throttle** : repath toutes ~10 ticks (déjà le cas) — ne pas descendre sous ça.
- **Cap de casse / seconde** : ajouter un compteur (config) pour borner les blocs cassés/s
  (anti-lag + anti-griefing runaway). Aujourd'hui implicite (1 bloc à la fois) ; le rendre explicite.
- **Régén optionnelle** des blocs cassés après X temps (déjà prévue cahier §3.5).
- **Pas de multi-thread.** L'A* vanilla tourne sur le thread serveur ; à courte portée + nœuds
  plafonnés, le budget tient. (Baritone est off-thread sur un *cache* de chunks — complexité qu'on
  ne veut pas.)

### 2.6 Nouveaux champs de config (à ajouter dans `ItFollowsConfig`)

```java
/** Active le pathfinding « intelligent » (NodeEvaluator custom) ; sinon nav vanilla pure. */
public boolean stalkerSmartPathfinding = true;
/** Portée de pathfinding (blocs) → maxVisitedNodes = ⌊range×16⌋. Bas = moins de CPU. */
public int stalkerPathRange = 48;
/** Multiplicateur de nœuds visités (knob fin, défaut 1.0). */
public float stalkerPathNodesMultiplier = 1.0f;
/** Cap dur de blocs cassés par seconde (anti-lag). 0 = pas de cap. */
public int stalkerMaxBreaksPerSecond = 6;
/** Ticks d'anticipation pour l'interception (cf. §4.1). 0 = vise la position actuelle. */
public int stalkerLeadTicks = 6;
```

> `FOLLOW_RANGE` reste à 256 dans `createAttributes()` **seulement** si une autre logique en dépend ;
> sinon l'aligner sur `stalkerPathRange`. Le plus propre : laisser l'attribut bas et ne pas s'en
> servir ailleurs.

### 2.7 Critère go/no-go vers l'Étape 2

On reste en Étape 1 si les scénarios §5 passent. On bascule en A* maison **seulement si** : tunnels
coudés ratés, refuge multi-couches non résolu, ou besoin de **coût = temps de casse réel** (le malus
binaire fait creuser un mur d'obsidienne aussi « volontiers » qu'un mur de terre, ce qui est non
désiré).

---

## 3. Étape 2 (conditionnelle) — A* maison `StalkerPathfinder`

Seulement si l'Étape 1 plafonne. On garde la structure mais le **coût d'arête = primitive typée**.

### 3.1 Modèle de coût (par arête)

| Primitive | Coût (base = 1.0) | Pré-conditions | Notes |
|---|---|---|---|
| Marche | 1.0 | case + sol libres | |
| Diagonale | 1.41 | 2 coins libres | évite de couper les murs |
| Monter d'1 | 1.0 + 0.5 | tête libre | |
| Descendre 1–3 | 1.0 + 0.2·n | — | **invincible ⇒ pas de fall damage** |
| Chute > 3 | 1.0 + 0.1·n (borné) | — | exploit assumé : « elle te tombe dessus » |
| Nager | 1.0 × `swimFactor` | eau | physique d'eau déjà gérée |
| Grimper (mur/échelle) | 1.0 + 1.0 | mur adjacent | pattern araignée existant |
| **Casser-pour-traverser** | 1.0 + `dureté × k` | cassable | **cœur Baritone-like** |
| Ouvrir porte bois | 0.2 | porte/trappe bois | |
| Incassable | ∞ | — | bedrock, barrière |

`dureté × k` rend enfin le coût **proportionnel au vrai temps de minage** — ce que l'enum vanilla ne
permet pas. C'est l'unique raison valable de passer à l'Étape 2.

### 3.2 Exécution & perf
- Chaque nœud porte sa **primitive** (`WALK/DIAG/STEP/FALL/SWIM/CLIMB/BREAK/OPEN_DOOR`) → l'exécuteur
  sait quoi faire **sans deviner** (fini `findObstacle`).
- A* **synchrone borné** : cap de nœuds explorés **1500–2500**, **budget temps/tick**, fenêtre
  verticale bornée (±12), réutilisation du dernier chemin tant que la cible n'a pas bougé de > N blocs.
- File de priorité (binary heap), `HashMap<Long, NodeRecord>` indexée par `BlockPos.asLong()`.
- Amorti sur K ticks (calcul incrémental possible : N expansions/tick jusqu'à trouver).

---

## 4. Couche « intelligence d'horreur » (par-dessus la nav, Étape 1 ou 2)

L'optimalité pure n'est pas le but : « It Follows » = **lent mais inéluctable**. Tout configurable.

### 4.1 Interception / anticipation *(fort impact, faible coût)*
⚠️ `target.getDeltaMovement()` est **peu fiable pour un joueur** (mouvement piloté par packets,
souvent ~0 côté serveur). → **Mesurer la vélocité nous-mêmes** sur l'intervalle d'échantillonnage :

```java
Vec3 vel = target.position().subtract(lastTargetPos).scale(1.0 / dtTicks); // bloc/tick
Vec3 aim = target.position().add(vel.scale(config.stalkerLeadTicks));        // point d'interception
// pathfinder vers 'aim' au lieu de target.position()
lastTargetPos = target.position();
```

Doser `stalkerLeadTicks` : trop = injuste/incohérent, trop peu = bête. ~6 ticks est un bon départ.

### 4.2 Raccourci par destruction / chute
Émerge gratuitement du modèle de coût (chute bon marché car invincible) → ligne droite plongeante,
sentiment d'inéluctabilité. Ajouter un léger **biais « ligne droite »** (tie-break en faveur du nœud
le plus aligné sur la cible) pour renforcer l'effet.

### 4.3 Théâtralité (optionnel, fort sur l'ambiance)
Biais d'approche selon le regard de la cible : approche **silencieuse** quand elle regarde ailleurs,
**fige** quand elle te fixe (ange pleureur). Derrière un flag config — change radicalement le ressenti,
à valider en multi.

### 4.4 Dernière position connue *(optionnel, à trancher)*
En tension avec le ciblage omniscient par fatigue. **Compromis retenu** : garder l'omniscience du
*ciblage*, rendre l'*approche* faillible/lente (latence d'interception, replan périodique) — meilleur
ratio horreur/lisibilité que de simuler une vraie perte de piste.

---

## 5. Vérification (scénarios de test multi local, 2 clients)

Seul le traqué voit l'entité (cf. cahier §10). Tests décisifs :

- **Refuge scellé** (mur plein 3×3×3) → l'entité **planifie un tunnel** (le moins cher), ne tourne pas
  en rond, casse les bons blocs dans le bon ordre (feet + tête).
- **Labyrinthe / coude** (contourner *puis* casser) → **test décisif Étape 1 vs 2** : le glouton actuel
  échoue, le `NodeEvaluator` doit réussir.
- **Tour de minage** (cible perchée) → escalade **ou** casse de colonne selon le moins cher, sans gel.
- **Murs hétérogènes** (terre + obsidienne) → l'Étape 1 perce indifféremment (malus binaire) ; si on
  veut qu'elle **contourne l'obsidienne**, c'est le signal de passer à l'Étape 2.
- **Fuite rapide** (sprint/cheval/élytre) → la poursuite virtuelle reprend la main, re-matérialisation
  propre, pas de gel (non-régression `HauntController`).
- **Eau / plongée** → nage, plonge, ressort (non-régression `StalkerMoveControl`).
- **Perf** → profiler (`/forge tps` non dispo en Fabric ; utiliser un profiler ou logs custom) avec 1
  entité + casse soutenue : pas de spike, pas de destruction runaway, `maxVisitedNodes` jamais saturé.

---

## 6. Roadmap d'implémentation

> **Pivot d'implémentation (assumé).** À l'écriture, le `StalkerNodeEvaluator` envisagé en §2 s'est
> heurté au **piège géométrique** §2.2 : overrider `getBlockPathType` ne suffit pas — `getFloorLevel`
> lit la collision réelle et traite un mur « franchissable » comme une surface où *monter*, pas à
> *traverser*. Le faire proprement = réécrire les internes de `WalkNodeEvaluator` (fragile, bugs
> visibles qu'en playtest). **On a donc livré l'hybride** : la nav vanilla marche/nage en espace
> libre, et un **planificateur A\* borné** ([`StalkerPathPlanner`](src/main/java/io/github/yutoutcourt/itfollows/entity/ai/StalkerPathPlanner.java))
> désigne le **prochain bloc à casser** sur la route la moins chère (coût ∝ dureté). On obtient le
> tunnel **délibéré** promis, sans combattre le moteur — et avec le **coût ∝ dureté réelle** que
> l'enum vanilla interdisait (donc contourne l'obsidienne si un passage plus tendre existe).

- **Phase A — Planificateur + interception** ✅ *(cœur du gain, livré)* :
  [`StalkerPathPlanner`](src/main/java/io/github/yutoutcourt/itfollows/entity/ai/StalkerPathPlanner.java)
  (A* 6-voisins, air|cassable, coût ∝ dureté, borné `MAX_EXPANSIONS`/`stalkerPathRange`) ;
  [`BreakObstacleGoal`](src/main/java/io/github/yutoutcourt/itfollows/entity/ai/BreakObstacleGoal.java)
  consulte le planificateur (throttlé `PLAN_INTERVAL`) au lieu du choix glouton ;
  [`StalkerChaseGoal`](src/main/java/io/github/yutoutcourt/itfollows/entity/ai/StalkerChaseGoal.java)
  vise un **point d'interception** (vélocité mesurée × `stalkerLeadTicks`). Flag `stalkerSmartPathfinding`.
  - **Correctifs locomotion** (suite playtest) :
    - `setMaxUpStep(1.1f)` → monte un bloc plein sans sauter (+ rebords / sortie d'eau).
    - **Garde de proximité** (`isWithinReach`) dans `BreakObstacleGoal` → ne casse que le bloc
      voisin, plus de casse « à distance » d'un mur lointain désigné par le planificateur.
    - `setMaxUpStep(1.1f)` conservé pour le franchissement d'1 bloc au sol.
  - **Vol hybride** (remplace l'escalade araignée, suite re-challenge Baritone) :
    L'escalade « pattern araignée » (flag `CLIMBING`, `onClimbable`, `hasAdjacentWall`) est
    **supprimée** — trop fragile sur plateformes/surplombs. Remplacée par un **mode vol** :
    - **3ᵉ navigation** [`FlyingPathNavigation`](src/main/java/io/github/yutoutcourt/itfollows/entity/StalkerEntity.java)
      (A* 3D dans l'air), arbitrée avec eau/sol dans `updateSwimming` : **eau > vol > sol**.
    - **Décollage** (`customServerAiStep`) : cible nettement au-dessus (`stalkerFlyTriggerHeight`),
      hors de portée, **et marche bloquée** (`isBlocked`) → on laisse d'abord le sol tenter, sinon
      on décolle. **Atterrissage** avec hystérésis (`stalkerMinFlightTicks`) : hauteur de la cible
      rejointe ou portée d'engagement → la gravité repose l'entité.
    - **Physique** : `StalkerMoveControl` branche vol (pilotage 3D façon nage, `stalkerFlySpeedFactor`)
      + `travel` (poussée `yya`, sans gravité, friction air) ; flag synchronisé `isFlying()` →
      **animation d'ailes** côté client (`StalkerModel`, pose placeholder à enrichir).
    - **Casse conservée** : une pièce scellée n'a aucun chemin aérien → `BreakObstacleGoal` perce
      l'enceinte (le vol presse le surplomb, la casse l'ouvre). Le vol règle les bugs n°3/4/5
      (montée 1 bloc, sortie d'eau, plateforme en hauteur) **par construction**.
    - Flags : `stalkerCanFly`, `stalkerFlySpeedFactor`, `stalkerFlyTriggerHeight`,
      `stalkerMinFlightTicks`. `stalkerCanClimb` est désormais **obsolète** (inutilisé).
- **Phase B — Garde-fous perf** : `stalkerPathRange`, multiplicateur, cap casse/s, profilage (§2.5).
- **Phase C — Anticipation** : interception sur vélocité mesurée (§4.1), biais ligne droite (§4.2).
- **Phase D — Théâtralité (optionnel)** : biais regard / figement (§4.3), flags config.
- **Phase E — A\* maison** *(seulement si A–D plafonnent)* : `StalkerPathfinder` + primitives typées,
  coût = temps de casse réel (§3).

> **Principe directeur** : maximiser l'intelligence *perçue* (inéluctabilité, anticipation, tunnels
> délibérés) au coût minimal. Réutiliser le A* vanilla tant qu'il tient ; ne construire l'A* maison
> que quand un scénario réel le force.
>
> **Sur Baritone (verdict révisé)** : contrairement à un premier jugement, un *fork* de Baritone
> **peut** piloter une entité serveur (cf. `enhanced-mobs` 1.21.10 : `createBaritone(server, entity)`).
> Mais leur propre code montre qu'il sert de **planificateur**, la nav vanilla exécutant le
> déplacement (≡ notre archi `StalkerPathPlanner` + nav). Le seul gain net — la **pose de blocs**
> pour la poursuite verticale — est ici résolu autrement, par le **vol hybride**, sans le coût de
> vendoriser/porter un fork LGPL 1.21.10→1.20.1 + ~2000 lignes de pont. **Décision : pas de Baritone.**
