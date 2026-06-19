# Cahier des charges — Mod Minecraft Horreur « It Follow »

## Context

Projet **greenfield** (le dossier ne contient que `idea.md`, pas de code, pas de git).
Objectif : un mod **Fabric 1.20.1** d'horreur psychologique multijoueur dont le pilier est
« **je suis le seul à la voir** ». Une entité invisible traque le joueur le plus fatigué ;
pour s'en débarrasser il faut **transférer une malédiction** à un autre joueur via une action
discrète et bizarre, ce qui instille la **paranoïa** dans le groupe.

Ce document fige les décisions de design, identifie les pièges techniques et propose une
roadmap incrémentale. Il sert de base au plan d'implémentation et au démarrage du code.

### Décisions validées avec le porteur du projet
- **Perception de l'entité = Hybride** : l'entité modifie *réellement* le monde (blocs, portes)
  côté serveur, mais son **corps/rendu est invisible aux joueurs non-traqués**. Les autres
  voient les *conséquences* (blocs cassés, portes ouvertes) sans voir le monstre.
- **Cible = Multijoueur** (jeu entre amis). **MVP = système de fatigue d'abord**, puis l'entité.
- **Destruction = Permanente** (par défaut). Régénération possible en option config (anti-grief).
- **Malédiction = actions solo, unilatérales, discrètes et « bizarres »** réalisées par le maudit
  *sur/à proximité* d'une cible (donner un objet rare, sneak X fois devant lui, le pousser dans
  l'eau…). Besoin d'une **grande banque** d'actions pour éviter la répétition. Le but n'est PAS
  la coopération mais le **doute** : « pourquoi il fait ça ? c'est lui le maudit ? ».

---

## 1. Architecture technique (vue d'ensemble)

- **Loader** : Fabric 1.20.1, `fabric-api`. Build : Gradle + Loom. Java 17.
- **Découpage client/serveur strict** :
  - Logique de vérité (fatigue, ciblage, entité, malédiction, monde) = **serveur**.
  - Effets sensoriels (hallucinations, overlays, sons locaux, rendu sélectif) = **client**.
  - Synchronisation via **packets custom** (`fabric-networking-api-v1`) — un payload par système.
- **État persistant** : `PersistentState` attaché au `ServerWorld`/overworld pour le système de
  traque (cible courante, historique des maudits, timers). Données par-joueur via
  composant/attachement (fatigue, statut maudit).
- **Config** : fichier (ex. via `fabric` + une lib simple ou JSON maison) pour les durées,
  intensités, toggle régénération des blocs, durée de grâce avant traque, etc.

### Structure de projet proposée (fichiers à créer)
```
src/main/java/<group>/itfollow/
  ItFollowMod.java                 // entrypoint commun (register)
  ItFollowClient.java              // entrypoint client (render, overlays, sons)
  fatigue/
    FatigueComponent.java          // état par joueur (0-100)
    FatigueManager.java            // tick, coûts d'actions, régen
    FatigueEffects.java            // mapping fatigue -> effets serveur/client
  entity/
    StalkerEntity.java             // MobEntity custom (invincible, ghost vs autres)
    StalkerNavigation.java         // navigation custom
    goal/BreakBlockGoal.java       // casser blocs sur le chemin
    goal/OpenDoorGoal.java         // portes / trappes
    goal/FollowTargetGoal.java     // poursuite cible
    render/StalkerRenderer.java    // rendu CLIENT, visible uniquement pour la cible
  tracking/
    StalkTrackerState.java         // PersistentState : cible, historique, phase
    TargetSelector.java            // choix de la cible (fatigue max)
    HauntPhaseController.java      // escalade : bruits -> portes -> silhouette -> traque
  curse/
    CurseComponent.java            // statut maudit + objectif courant
    CurseActionRegistry.java       // banque d'actions
    CurseAction.java               // interface (condition, progression, validation)
    CurseManager.java              // assignation, détection accomplissement, transfert
  presence/
    PresenceManager.java           // effets de présence (cible 100% / autres faibles)
  hallucination/
    HallucinationManager.java      // CLIENT-only, pool limité, ghost entities
  sleep/
    SleepManager.java              // sieste / sommeil profond / nuit globale (phase tardive)
  net/
    payloads...                    // packets S2C/C2S
```

---

## 2. Système de fatigue (MVP — Phase 1)

Barre 0→100 par joueur (100 = en forme, 0 = épuisé).

### Coûts (à équilibrer via config)
| Action | Coût |
|---|---|
| Sprint | élevé |
| Sauts répétés | moyen |
| Combat | moyen→élevé |
| Minage | faible |
| Marche | très faible |
| Nage | élevé (effort) |
| Bateau (en mouvement) | faible (le bateau soulage) |
| Inactivité / repos | **régén passive lente et faible** : comme dans la vraie vie, ne rien faire récupère un peu — mais volontairement très lent, ce n'est pas une vraie voie de récup (le vrai repos = sieste/sommeil, Phases 1.5 & 5). |

### Effets par palier (serveur applique effets, client renforce le ressenti)
| Palier | Effets |
|---|---|
| Légère 75-50 | régén réduite, respiration plus forte (client) |
| Moyenne 50-25 | légère lenteur, FOV réduit, vision plus sombre la nuit |
| Élevée 25-10 | lenteur ++, bruitages de stress, hallucinations rares, réactions ralenties |
| Critique 10-0 | hallucinations fréquentes, vision réduite, nausée légère, sprint impossible, micro-endormissements |

### Notes techniques
- **Ne pas tout calculer chaque tick** : système événementiel (hooks sur sprint/jump/attack/mine)
  + échantillonnage périodique (toutes N ticks) pour la décroissance.
- Effets de mouvement via `StatusEffect`/attributs (slowness) côté serveur ; FOV / vision /
  respiration / nausée = overlays et sons **client** pilotés par un packet de niveau de fatigue.
- La fatigue est la **métrique de ciblage** de l'entité (lien Phase 2). C'est pourquoi on la fait
  en premier.

---

## 3. Entité traqueuse (Phase 2)

### 3.1 Ciblage & cycle de vie
- **Délai de grâce** : 10–15 min après la connexion du *premier* joueur (config), puis sélection
  de la cible = **joueur avec le plus de fatigue**.
- **À la mort de la cible** : on remonte l'**historique des maudits** (si A a maudit B et que B
  meurt, A redevient maudit) ; sinon nouvelle cible = fatigue max à l'instant T.
- **Persistance / hors-chunks** : l'entité **n'existe pas toujours physiquement**. Un
  `StalkTrackerState` (logique) connaît la cible et la phase ; l'entité est **spawn/despawn**
  dynamiquement près de la cible (et suit les changements de dimension).

### 3.2 Escalade de la traque (avertissement progressif)
`HauntPhaseController` enchaîne, sur plusieurs minutes :
1. Bruits lointains.
2. Portes qui s'ouvrent / blocs cassés *à proximité* (réels, donc perçus aussi par les autres).
3. Silhouette aperçue brièvement.
4. **Traque active** : l'entité spawn et poursuit.

### 3.3 Comportement
- **Invincible**, **lente**, casse les blocs (vitesse selon dureté du bloc), ouvre portes/trappes,
  monte échelles/blocs.
- Tue la cible au contact (dégâts).

### 3.4 Rendu hybride (le point délicat)
- Entité serveur réelle pour **tout le monde** (IA, modifs de monde).
- **Invisibilité sélective au rendu** : la cible courante est synchronisée (UUID dans les
  `TrackedData` de l'entité). `StalkerRenderer` (client) **ne dessine l'entité que si le joueur
  local == la cible**. Pour les autres clients → non rendue.
- **Collisions** : l'entité **traverse les joueurs** (pas de push) pour ne pas trahir sa position
  à un non-traqué qui « buterait » dessus. Elle interagit avec **blocs/portes** et inflige des
  dégâts **uniquement à la cible**.
- Les autres joueurs perçoivent donc seulement : blocs cassés, portes ouvertes, sons → cohérent
  avec « je suis le seul à la voir ».

### 3.5 Pathfinding & risques
- `PathNavigation` vanilla insuffisant (casser pour avancer). → Navigation custom + `BreakBlockGoal`
  qui détecte un obstacle sur la trajectoire et le mine progressivement.
- **Risques** : entité coincée dans les murs, destruction excessive de chunks, lag serveur.
- **Mitigations** : limiter la portée de simulation (n'agir que près de la cible) ; cap sur le
  nombre de blocs cassés par seconde ; **toggle config régénération** des blocs après X temps
  (anti-grief) même si destruction permanente est le défaut.

---

## 4. Malédiction & transfert (Phase 3) — cœur du doute

### Principe
Le maudit reçoit un **objectif secret** : accomplir une **action unilatérale** ciblant un autre
joueur précis (ou « n'importe quel autre joueur »). Une fois accomplie, la **malédiction est
transférée** à cette cible. L'action doit **paraître bizarre** vue de l'extérieur → la victime
potentielle et les témoins se demandent « pourquoi il fait ça ? » → **paranoïa**.

### Règles
- Action **solo** (pas besoin de coopération/consentement de la cible).
- **Pas trop facile** : nécessite répétition, proximité, ou un objet rare → donc *observable*.
- **Variée** : tirage aléatoire dans une grande banque ; éviter de redonner la même récemment.
- **Cooldown / anti-ping-pong** : court délai avant de pouvoir re-maudire la personne qui vient
  de vous maudire (configurable) pour éviter l'aller-retour instantané.
- Le maudit **connaît** son action (UI discrète / message privé) ; la cible **ne sait pas**
  qu'elle est visée (sinon la tension tombe).

### Banque d'actions proposée (à étendre — but : éviter la répétition)
> Catégorisées ; chaque entrée = `CurseAction` avec condition de détection + progression.

**Don / échange**
- Donner un **objet rare précis** (diamant, tête, lingot d'or, fleur rare…) à la cible.
- Faire en sorte que la cible **ramasse** un objet précis que vous laissez tomber à ses pieds.

**Gestuelle / proximité (les plus « creepy »)**
- **Sneak X fois** à moins de 3 blocs de la cible, dans son champ de vision.
- **Fixer** la cible (la regarder) pendant X secondes cumulées à courte distance.
- **Tourner autour** de la cible (faire le tour complet) sans la perdre de vue.
- S'**accroupir/relever** rapidement Y fois près d'elle.

**Contact / physique**
- **Pousser** la cible dans l'**eau**.
- La **toucher** (attaque à 0 dégât) X fois.
- La faire **tomber** d'une petite hauteur (déclencher une chute non létale).

**Environnement**
- **Casser un bloc** juste sous/à côté de la cible.
- **Placer un bloc spécifique** (ex : tête, citrouille, torche) à proximité immédiate d'elle.
- **Ouvrir un coffre** devant la cible / lui faire ouvrir un coffre que vous indiquez.
- **Allumer/éteindre** une source de lumière près d'elle.

**Monde / mobs**
- **Tuer un mob précis** (mouton, poulet…) dans le champ de vision de la cible.
- **Amener** la cible (ou se trouver avec elle) à un type de lieu (sous l'eau, en hauteur, dans le noir).

**Consommation**
- Faire **manger** à la cible un aliment précis (lui donner puis qu'elle le consomme).

### Technique
- `CurseAction` = interface : `id`, `description`, `assignDifficulty`, `onTick/onEvent(progress)`,
  `isComplete(curser, target, world)`.
- Détection via **events Fabric** (attaque, pickup, use item, sneak toggle, move) + checks de
  proximité/regard (raycast/angle).
- `CurseManager` : assigne (tirage pondéré, exclut les dernières utilisées), suit la progression,
  exécute le transfert, met à jour `StalkTrackerState` (cible de l'entité = nouveau maudit).

---

## 5. Effets de présence (Phase 2–3, transverse)

| Public | Effets quand l'entité est proche |
|---|---|
| **Cible (100%)** | battements de cœur, respiration ++, bruits aléatoires, vision troublée, hallucinations, faux joueurs, ombres, nausée légère, écran qui s'assombrit. Doute permanent « elle est là ou c'est mon cerveau ? » |
| **Autres (faibles, indirects)** | moyenne distance : bruits étranges, portes qui grincent, torches qui vacillent, son sourd. Très proche : légère baisse de luminosité, souffle/murmures, léger tremblement d'écran, animaux silencieux. **Pas de grosses hallucinations** (sinon le concept s'effondre). |

Pilotage : packets S2C ciblés (au bon joueur uniquement) → overlays/sons **client**.

---

## 6. Hallucinations & faux stimuli (Phase 4)

Déclenchées par **fatigue élevée** et/ou **présence de l'entité**, **uniquement pour la cible**.
- Catalogue : entité aperçue 1 s, bruit de creeper, pas derrière soi, faux joueur, son propre skin
  qui regarde au loin, coffres qui semblent ouverts, lit occupé alors que vide, faux messages chat.
- **Client-only**, **pool limité** (nb max simultané), réutilisation d'**entités fantômes**
  (copies de rendu, pas d'entités serveur) → pas de désync, pas de surcharge.

---

## 7. Sommeil & nuit globale (split en deux : Phase 1.5 et Phase 5)

> **Phase 1.5 (tôt, peu risqué)** = la sieste seule, sans toucher au temps du monde.
> **Phase 5 (tard, risqué)** = sommeil profond + nuit globale + overlay de temps.

- **Sieste** *(Phase 1.5)* (à tout moment) : récup accélérée vs régén passive, proportionnelle à la
  durée, joueur vulnérable, **temps du monde inchangé** (aucun overlay de temps requis).
- **Sommeil profond** (nuit only) : récup rapide, fait passer la nuit, plus risqué.
- **Endormissement** : 3 s allongé avant de récupérer (anti micro-siestes).
- **Réveil progressif** : 5 s sans sprint, mouvements ralentis, désorientation.
- **Nuit globale** : tous couchés 5 s → accélération ; si un seul se lève → arrêt immédiat.
- **Qualité du sommeil** : modulée par l'environnement (monstres, pluie, entité = mauvais ;
  refuge sûr, feu de camp = bon).
- **Audio en dormant** : yeux fermés mais sons du monde audibles (porte, pas, respiration près du lit).
- **NE PAS modifier le time vanilla directement** → « overlay de temps » : freeze world time,
  animation de ciel **client**, puis `setTime` officiel à la fin. Évite conflits autres mods,
  désync, bugs si un joueur quitte en transition.

---

## 8. Risques techniques majeurs & parades (récap)

| Risque | Parade |
|---|---|
| Rendu invisible sélectif | TrackedData cible + render client conditionnel ; collisions joueur désactivées |
| Pathfinding destructeur (coincé, chunks détruits, lag) | navigation custom bornée à la proximité cible, cap de casse/s, toggle régén |
| Entité hors-chunks | tracking logique abstrait + spawn/despawn dynamique |
| Fatigue tick-heavy | événementiel + échantillonnage, effets client via packets |
| Hallucinations lourdes | client-only, pool limité, ghost copies |
| Sommeil/nuit casse l'écosystème serveur | overlay de temps, jamais toucher le time vanilla en direct |
| Joueur quitte pendant traque/transition | état serveur persistant, reprise propre au reconnect |

---

## 9. Roadmap (phases incrémentales)

- **Phase 0 — Squelette** ✅ : projet Fabric 1.20.1 + Loom, entrypoints, enregistrement de base,
  commande debug, config.
- **Phase 1 — Fatigue (MVP)** ✅ : composant par joueur, coûts d'actions (sprint/marche/nage/bateau/
  saut/attaque/minage), paliers + effets serveur, overlay client minimal, packet de sync.
  *(Système choisi en premier.)*
- **Phase 1.5 — Sieste simple** : s'allonger → régén accélérée **sans toucher au temps du monde**.
  Petit ajout pour fermer la boucle dépenser/récupérer sans prendre le risque du sommeil global.
  Le sommeil profond + nuit globale (le morceau risqué) reste en **Phase 5**.
- **Phase 2 — Entité & traque** *(cœur du jeu)* : StalkerEntity (invincible, lente, ghost vs autres),
  rendu hybride, navigation + casse de blocs/portes, ciblage par fatigue, escalade des
  avertissements, persistance/tracking.
- **Phase 3 — Malédiction** *(boucle sociale)* : banque d'actions, assignation, détection, transfert,
  gestion mort de la cible (remontée d'historique).
- **Phase 4 — Présence & hallucinations** : effets cible vs autres, hallucinations client-only.
- **Phase 5 — Sommeil profond & nuit globale** *(le plus risqué, en dernier)* : overlay de temps,
  qualité du sommeil, audio en dormant. **Ne jamais toucher au time vanilla en direct.**
- **Phase 6 — Équilibrage & polish** : valeurs config, tests multijoueur, perf.

> **Note de séquencement** : on traite le sous-système le plus risqué (nuit globale / overlay de
> temps) en dernier, une fois les patterns réseau/persistance/test multi rodés sur l'entité. La
> sieste simple (1.5) donne la récupération « confort » sans ce risque.

---

## 10. Vérification (comment tester)

- **Build** : `./gradlew build` puis lancement `runClient` / `runServer` (env Loom).
- **Phase 1** : commande debug pour lire/forcer la fatigue ; vérifier coûts par action et
  apparition des effets aux bons paliers (overlay + slowness).
- **Phase 2** : test **multijoueur local** (2 clients) → confirmer que SEUL le traqué voit
  l'entité, que les autres voient blocs cassés/portes ouvertes, et que l'entité ne pousse pas les
  non-traqués. Vérifier pathfinding (pas de blocage, pas de destruction runaway) et persistance
  inter-chunks/dimensions.
- **Phase 3** : assigner une malédiction (debug), accomplir l'action, vérifier le transfert et le
  re-ciblage de l'entité ; tester la remontée d'historique à la mort.
- **Phases 4–5** : vérifier que hallucinations/présence n'apparaissent QUE pour les bons joueurs ;
  vérifier que la nuit globale ne désync pas le temps et survit à un déco/reco.

---

## 11. Points encore ouverts (à trancher pendant l'implémentation)

- **Banque d'actions de malédiction** : étendre la liste, pondérer la difficulté, définir les
  seuils (X sneaks ? Y secondes de regard ?) — c'est le levier principal anti-répétition.
- **Récupération à l'inactivité** : choix de design retenu = ne rien faire régénère la fatigue
  **lentement et faiblement** (analogie « repos réel »), sans pénalité anti-AFK. La régén passive
  est calibrée très basse pour qu'elle ne remplace pas la vraie récupération (sieste/sommeil) ni ne
  rende l'immobilité « optimale » pour fuir l'entité. À ajuster à l'équilibrage.
- Détails d'équilibrage : durée de grâce, vitesse de l'entité, vitesse de casse par bloc, taux de
  régén de fatigue par type de sommeil.
- `group`/package name définitif et nom de mod final (« It Follow » ?).
