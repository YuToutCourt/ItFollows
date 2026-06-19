# Graph Report - .  (2026-06-17)

## Corpus Check
- Corpus is ~6,876 words - fits in a single context window. You may not need a graph.

## Summary
- 197 nodes · 306 edges · 19 communities (17 shown, 2 thin omitted)
- Extraction: 94% EXTRACTED · 6% INFERRED · 0% AMBIGUOUS · INFERRED: 18 edges (avg confidence: 0.8)
- Token cost: 34,758 input · 0 output

## Community Hubs (Navigation)
- [[_COMMUNITY_Curse & Stalker Design|Curse & Stalker Design]]
- [[_COMMUNITY_NapSieste Interaction|Nap/Sieste Interaction]]
- [[_COMMUNITY_Fatigue Manager|Fatigue Manager]]
- [[_COMMUNITY_Fatigue & Perception Design|Fatigue & Perception Design]]
- [[_COMMUNITY_Fatigue Rules (Client)|Fatigue Rules (Client)]]
- [[_COMMUNITY_Fatigue HUD Rendering|Fatigue HUD Rendering]]
- [[_COMMUNITY_Nap Mixins|Nap Mixins]]
- [[_COMMUNITY_Fatigue Persistence|Fatigue Persistence]]
- [[_COMMUNITY_Debug Command|Debug Command]]
- [[_COMMUNITY_Mod Init & Networking|Mod Init & Networking]]
- [[_COMMUNITY_Config Loading|Config Loading]]
- [[_COMMUNITY_Fatigue Effects|Fatigue Effects]]
- [[_COMMUNITY_Sleep Timer Mixin|Sleep Timer Mixin]]
- [[_COMMUNITY_Data Generation|Data Generation]]
- [[_COMMUNITY_Jump Mixin|Jump Mixin]]
- [[_COMMUNITY_License|License]]

## God Nodes (most connected - your core abstractions)
1. `FatigueManager` - 14 edges
2. `ServerPlayer` - 10 edges
3. `Systeme de fatigue (barre 0-100 par joueur)` - 10 edges
4. `FatigueState` - 7 edges
5. `SiesteManager` - 7 edges
6. `StalkerEntity (entite traqueuse invincible/lente/ghost)` - 7 edges
7. `FatigueHudOverlay` - 6 edges
8. `ItFollowsCommand` - 6 edges
9. `FatigueRules` - 6 edges
10. `Player` - 6 edges

## Surprising Connections (you probably didn't know these)
- `ServerPlayerNapMixin` --implements--> `NapStarter`  [EXTRACTED]
  src/main/java/io/github/yutoutcourt/itfollows/mixin/ServerPlayerNapMixin.java → src/main/java/io/github/yutoutcourt/itfollows/sieste/NapStarter.java
- `ServerPlayerNapMixin` --inherits--> `Player`  [EXTRACTED]
  src/main/java/io/github/yutoutcourt/itfollows/mixin/ServerPlayerNapMixin.java → src/main/java/io/github/yutoutcourt/itfollows/sieste/SiesteManager.java

## Import Cycles
- None detected.

## Hyperedges (group relationships)
- **Boucle traque: ciblage par fatigue, etat persistant, escalade** — cahier_target_selector, cahier_stalk_tracker_state, cahier_haunt_phase_controller, cahier_stalker_entity [EXTRACTED 0.90]
- **Flux malediction: action, detection, transfert, re-ciblage entite** — cahier_curse_action, cahier_curse_manager, cahier_curse_component, cahier_stalk_tracker_state [EXTRACTED 0.85]
- **Pattern sync: logique serveur, packets, effets client** — cahier_decoupage_client_serveur, cahier_packets_custom, cahier_presence_manager, cahier_hallucination_manager [INFERRED 0.80]

## Communities (19 total, 2 thin omitted)

### Community 0 - "Curse & Stalker Design"
Cohesion: 0.07
Nodes (28): Cooldown anti-ping-pong de malediction, BreakBlockGoal (casser blocs sur le chemin), Config (durees, intensites, toggles), CurseAction (interface action de malediction), CurseActionRegistry (banque d'actions), Actions unilaterales bizarres (sneak/fixer/pousser...), CurseComponent (statut maudit + objectif), CurseManager (assignation, detection, transfert) (+20 more)

### Community 1 - "Nap/Sieste Interaction"
Cohesion: 0.19
Nodes (9): BlockHitResult, InteractionHand, InteractionResult, TestEvents, SiesteManager, ItFollowsConfig, Level, MinecraftServer (+1 more)

### Community 2 - "Fatigue Manager"
Cohesion: 0.33
Nodes (4): FatigueManager, ItFollowsConfig, MinecraftServer, ServerPlayer

### Community 3 - "Fatigue & Perception Design"
Cohesion: 0.13
Nodes (17): Commande debug (lire/forcer fatigue), Decoupage client/serveur strict, FatigueComponent (etat par joueur), FatigueEffects (mapping fatigue -> effets), Calcul fatigue evenementiel + echantillonnage, FatigueManager (tick, couts, regen), Paliers de fatigue (Legere/Moyenne/Elevee/Critique), Systeme de fatigue (barre 0-100 par joueur) (+9 more)

### Community 4 - "Fatigue Rules (Client)"
Cohesion: 0.17
Nodes (5): ClientFatigueState, FatigueRules, FatigueTier, FatigueTier, ItFollowsConfig

### Community 5 - "Fatigue HUD Rendering"
Cohesion: 0.22
Nodes (9): ItfollowsClient, ClientModInitializer, GuiGraphics, FatigueHudOverlay, HudRenderCallback, Minecraft, FatigueTier, Override (+1 more)

### Community 6 - "Nap Mixins"
Cohesion: 0.24
Nodes (8): GameProfile, ServerPlayerNapMixin, NapStarter, BlockPos, Level, Override, BlockPos, Player

### Community 7 - "Fatigue Persistence"
Cohesion: 0.26
Nodes (6): CompoundTag, FatigueState, SavedData, MinecraftServer, Override, UUID

### Community 8 - "Debug Command"
Cohesion: 0.45
Nodes (5): ItFollowsCommand, CommandContext, CommandDispatcher, CommandSourceStack, ServerPlayer

### Community 9 - "Mod Init & Networking"
Cohesion: 0.22
Nodes (5): Itfollows, ModInitializer, ItFollowsNetworking, Override, ServerPlayer

### Community 11 - "Fatigue Effects"
Cohesion: 0.43
Nodes (3): FatigueEffects, FatigueTier, ServerPlayer

### Community 12 - "Sleep Timer Mixin"
Cohesion: 0.47
Nodes (4): Boolean, CallbackInfoReturnable, PlayerSleepTimerMixin, Inject

### Community 13 - "Data Generation"
Cohesion: 0.47
Nodes (4): ItfollowsDataGenerator, DataGeneratorEntrypoint, FabricDataGenerator, Override

### Community 14 - "Jump Mixin"
Cohesion: 0.60
Nodes (3): CallbackInfo, LivingEntityJumpMixin, Inject

## Knowledge Gaps
- **22 isolated node(s):** `Override`, `Override`, `Override`, `Override`, `FatigueTier` (+17 more)
  These have ≤1 connection - possible missing edges or undocumented components.
- **2 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `Player` connect `Nap Mixins` to `Debug Command`, `Nap/Sieste Interaction`, `Sleep Timer Mixin`?**
  _High betweenness centrality (0.142) - this node is a cross-community bridge._
- **What connects `Override`, `Override`, `Override` to the rest of the system?**
  _29 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `Curse & Stalker Design` be split into smaller, more focused modules?**
  _Cohesion score 0.07407407407407407 - nodes in this community are weakly interconnected._
- **Should `Fatigue & Perception Design` be split into smaller, more focused modules?**
  _Cohesion score 0.1323529411764706 - nodes in this community are weakly interconnected._