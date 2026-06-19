# Graph Report - .  (2026-06-19)

## Corpus Check
- 88 files · ~105,176 words
- Verdict: corpus is large enough that graph structure adds value.

## Summary
- 755 nodes · 1376 edges · 53 communities (41 shown, 12 thin omitted)
- Extraction: 88% EXTRACTED · 11% INFERRED · 1% AMBIGUOUS · INFERRED: 156 edges (avg confidence: 0.8)
- Token cost: 0 input · 177,758 output

## Community Hubs (Navigation)
- [[_COMMUNITY_Entity Tracking & Visibility Mixins|Entity Tracking & Visibility Mixins]]
- [[_COMMUNITY_Nightmare Creeper & Mob Variants|Nightmare Creeper & Mob Variants]]
- [[_COMMUNITY_Stalker Entity Core|Stalker Entity Core]]
- [[_COMMUNITY_Curse  Malediction System|Curse / Malediction System]]
- [[_COMMUNITY_Mod Entity Registration & Faint|Mod Entity Registration & Faint]]
- [[_COMMUNITY_Fatigue Effects|Fatigue Effects]]
- [[_COMMUNITY_Player Interaction & Sleep Mixins|Player Interaction & Sleep Mixins]]
- [[_COMMUNITY_Stalker Chase Goal (AI Pursuit)|Stalker Chase Goal (AI Pursuit)]]
- [[_COMMUNITY_Client Fatigue State & HUD|Client Fatigue State & HUD]]
- [[_COMMUNITY_Time Clock & Mob TNT|Time Clock & Mob TNT]]
- [[_COMMUNITY_Mob Duplication|Mob Duplication]]
- [[_COMMUNITY_Fatigue Rules & State|Fatigue Rules & State]]
- [[_COMMUNITY_Stalker Pathfinding & Hybrid Flight|Stalker Pathfinding & Hybrid Flight]]
- [[_COMMUNITY_ItFollows Debug Command|ItFollows Debug Command]]
- [[_COMMUNITY_Mob Dig DownUp|Mob Dig Down/Up]]
- [[_COMMUNITY_Creeper Breach Walls|Creeper Breach Walls]]
- [[_COMMUNITY_Texture Generation Script|Texture Generation Script]]
- [[_COMMUNITY_Mob Start Fires|Mob Start Fires]]
- [[_COMMUNITY_Le Traqueur Concept Design Sheet|Le Traqueur Concept Design Sheet]]
- [[_COMMUNITY_Random Armor & Pickaxe Equip|Random Armor & Pickaxe Equip]]
- [[_COMMUNITY_Stalker Move Control (3D Steering)|Stalker Move Control (3D Steering)]]
- [[_COMMUNITY_Blocking Block Detection|Blocking Block Detection]]
- [[_COMMUNITY_Nearest Target Selection|Nearest Target Selection]]
- [[_COMMUNITY_Fallen Hunter Texture (AI Test)|Fallen Hunter Texture (AI Test)]]
- [[_COMMUNITY_Mob Damage Block|Mob Damage Block]]
- [[_COMMUNITY_Mob Target Player|Mob Target Player]]
- [[_COMMUNITY_Target Selector|Target Selector]]
- [[_COMMUNITY_Wraith Texture (AI Test)|Wraith Texture (AI Test)]]
- [[_COMMUNITY_Stalker Glowmask  Emissive|Stalker Glowmask / Emissive]]
- [[_COMMUNITY_ItFollows Config|ItFollows Config]]
- [[_COMMUNITY_Stalker Entity Texture|Stalker Entity Texture]]
- [[_COMMUNITY_GROCK Stalker Emissive Texture|GROCK Stalker Emissive Texture]]
- [[_COMMUNITY_GROCK Stalker Texture|GROCK Stalker Texture]]
- [[_COMMUNITY_Nightmare Zombie|Nightmare Zombie]]
- [[_COMMUNITY_Fabric Data Generator|Fabric Data Generator]]
- [[_COMMUNITY_Mob Bloodlust|Mob Bloodlust]]
- [[_COMMUNITY_Mob Build Bridge|Mob Build Bridge]]
- [[_COMMUNITY_Mob Dig|Mob Dig]]
- [[_COMMUNITY_Stalker Texture Template & Model|Stalker Texture Template & Model]]
- [[_COMMUNITY_Mob Max Life|Mob Max Life]]
- [[_COMMUNITY_General Entity Base|General Entity Base]]
- [[_COMMUNITY_Nightmare Spider|Nightmare Spider]]
- [[_COMMUNITY_Super Skeleton|Super Skeleton]]
- [[_COMMUNITY_Stalker Renderer|Stalker Renderer]]
- [[_COMMUNITY_Snap To Block Center|Snap To Block Center]]
- [[_COMMUNITY_Creeper Explode|Creeper Explode]]
- [[_COMMUNITY_Item Checker|Item Checker]]
- [[_COMMUNITY_Emissive Generation Script|Emissive Generation Script]]
- [[_COMMUNITY_ESM Config|ESM Config]]
- [[_COMMUNITY_Community 51|Community 51]]

## God Nodes (most connected - your core abstractions)
1. `StalkerEntity` - 29 edges
2. `HauntController` - 26 edges
3. `StalkerChaseGoal` - 24 edges
4. `MinecraftServer` - 20 edges
5. `StalkTrackerState` - 17 edges
6. `Override` - 16 edges
7. `FatigueManager` - 15 edges
8. `NightmareMain` - 13 edges
9. `ItFollowsConfig` - 12 edges
10. `ServerPlayer` - 11 edges

## Surprising Connections (you probably didn't know these)
- `ServerPlayerNapMixin` --implements--> `NapStarter`  [EXTRACTED]
  src/main/java/io/github/yutoutcourt/itfollows/mixin/ServerPlayerNapMixin.java → src/main/java/io/github/yutoutcourt/itfollows/sieste/NapStarter.java
- `ServerPlayerNapMixin` --inherits--> `Player`  [EXTRACTED]
  src/main/java/io/github/yutoutcourt/itfollows/mixin/ServerPlayerNapMixin.java → src/main/java/io/github/yutoutcourt/itfollows/sieste/SiesteManager.java

## Import Cycles
- None detected.

## Hyperedges (group relationships)
- **Pipeline pathfinding intelligent (Étape 1 livrée)** — ia_stalker_path_planner, ia_break_obstacle_goal, ia_stalker_chase_goal [EXTRACTED 1.00]
- **Chaîne pathfinding vanilla** — ia_ground_path_navigation, ia_path_finder, ia_walk_node_evaluator, ia_block_path_types [EXTRACTED 1.00]
- **Système de vol hybride** — ia_flying_path_navigation, ia_stalker_move_control, ia_stalker_model, ia_hybrid_flight [EXTRACTED 1.00]

## Communities (53 total, 12 thin omitted)

### Community 0 - "Entity Tracking & Visibility Mixins"
Cohesion: 0.09
Nodes (20): ChunkMapTrackedEntityMixin, ModifyVariable, ResourceKey, ServerPlayer, ItFollowsConfig, MinecraftServer, ServerLevel, ServerPlayer (+12 more)

### Community 1 - "Nightmare Creeper & Mob Variants"
Cohesion: 0.07
Nodes (24): NightmareCreeper, NightmareSkeleton, MobInfo, NightmareMain, RNG, FinalizeSpawn, FMLCommonSetupEvent, LivingTickEvent (+16 more)

### Community 2 - "Stalker Entity Core"
Cohesion: 0.07
Nodes (22): AnimatableInstanceCache, AnimationState, ControllerRegistrar, DamageSource, StalkerEntity, CannotReachPlayer, GeoEntity, MobPlaceBlock (+14 more)

### Community 3 - "Curse / Malediction System"
Cohesion: 0.05
Nodes (45): Cooldown anti-ping-pong de malediction, BreakBlockGoal (casser blocs sur le chemin), Commande debug (lire/forcer fatigue), Config (durees, intensites, toggles), CurseAction (interface action de malediction), CurseActionRegistry (banque d'actions), Actions unilaterales bizarres (sneak/fixer/pousser...), CurseComponent (statut maudit + objectif) (+37 more)

### Community 4 - "Mod Entity Registration & Faint"
Cohesion: 0.08
Nodes (19): Builder, ModEntities, Faint, FaintManager, Itfollows, ModInitializer, ItFollowsNetworking, StalkerGeoModel (+11 more)

### Community 5 - "Fatigue Effects"
Cohesion: 0.13
Nodes (14): CallbackInfo, FatigueEffects, NauseaWave, FatigueManager, LivingEntityJumpMixin, FatigueTier, ItFollowsConfig, MobEffect (+6 more)

### Community 6 - "Player Interaction & Sleep Mixins"
Cohesion: 0.09
Nodes (21): BlockHitResult, Boolean, CallbackInfoReturnable, GameProfile, InteractionHand, InteractionResult, TestEvents, PlayerSleepTimerMixin (+13 more)

### Community 7 - "Stalker Chase Goal (AI Pursuit)"
Cohesion: 0.17
Nodes (10): StalkerChaseGoal, Direction, Goal, BlockPos, BlockState, ItFollowsConfig, Override, ServerPlayer (+2 more)

### Community 8 - "Client Fatigue State & HUD"
Cohesion: 0.14
Nodes (11): ClientFatigueState, ItfollowsClient, ClientModInitializer, GuiGraphics, FatigueHudOverlay, HudRenderCallback, Minecraft, FatigueTier (+3 more)

### Community 9 - "Time Clock & Mob TNT"
Cohesion: 0.14
Nodes (9): Clocker, SetPlayerSpawn, MobPlaceTNT, SpawnRideableMob, Entity, Entity, Entity, BlockPos (+1 more)

### Community 10 - "Mob Duplication"
Cohesion: 0.18
Nodes (10): DuplicateMob, PositionInfo, MobBuildUp, BlockPos, Mob, ServerLevel, Entity, Mob (+2 more)

### Community 11 - "Fatigue Rules & State"
Cohesion: 0.13
Nodes (10): CompoundTag, FatigueRules, FatigueState, SavedData, FatigueTier, ItFollowsConfig, CompoundTag, MinecraftServer (+2 more)

### Community 12 - "Stalker Pathfinding & Hybrid Flight"
Cohesion: 0.10
Nodes (22): Baritone, BlockPathTypes / malus, BreakObstacleGoal, Casser-pour-traverser (coût ∝ dureté), FlyingPathNavigation, Piège géométrique (getFloorLevel), GroundPathNavigation (vanilla), HauntController (+14 more)

### Community 13 - "ItFollows Debug Command"
Cohesion: 0.35
Nodes (5): ItFollowsCommand, CommandContext, CommandDispatcher, CommandSourceStack, ServerPlayer

### Community 14 - "Mob Dig Down/Up"
Cohesion: 0.29
Nodes (6): MobDigDown, MobDigUp, Entity, Mob, Entity, Mob

### Community 15 - "Creeper Breach Walls"
Cohesion: 0.40
Nodes (4): CreeperBreachWalls, Creeper, Entity, ServerPlayer

### Community 16 - "Texture Generation Script"
Cohesion: 0.24
Nodes (6): clamp(), faces(), fill_rect(), paint(), Remplit un rectangle pixel avec couleur ombre + bruit + taches optionnelles., shade()

### Community 17 - "Mob Start Fires"
Cohesion: 0.32
Nodes (7): MobStartFires, Block, BlockPos, BlockState, Level, Mob, ServerPlayer

### Community 18 - "Le Traqueur Concept Design Sheet"
Cohesion: 0.22
Nodes (11): Le Traqueur - Concept Design Sheet, Animation / Behavior Variants (Comportements), Dark Horror Atmosphere, Deployable Black Wings (Ailes Deployees), Distorted Face with Wide Toothy Grin, Glowing Red Eyes, Tall Gaunt Humanoid Silhouette, Le Traqueur (The Stalker) (+3 more)

### Community 19 - "Random Armor & Pickaxe Equip"
Cohesion: 0.29
Nodes (5): GiveRandomArmor, GiveRandomPickaxe, Item, Entity, Monster

### Community 20 - "Stalker Move Control (3D Steering)"
Cohesion: 0.31
Nodes (4): StalkerMoveControl, MoveControl, Mob, Override

### Community 21 - "Blocking Block Detection"
Cohesion: 0.33
Nodes (4): BlockInfo, GetBlockingBlock, Block, Entity

### Community 22 - "Nearest Target Selection"
Cohesion: 0.50
Nodes (4): GetNearestTarget, List, Entity, PlayerList

### Community 23 - "Fallen Hunter Texture (AI Test)"
Cohesion: 0.31
Nodes (9): Fallen Hunter Texture (AI Test), Eerie Pale Stalker Mood, Fallen Hunter Creature Variant, Desaturated Gray Palette, Very Low Resolution Pixel Art, Dark Red Maroon Mouth, Minimal Pixel Face Motif, Blocky UV Atlas Layout (+1 more)

### Community 24 - "Mob Damage Block"
Cohesion: 0.39
Nodes (5): MobDamageBlock, Block, BlockPos, Entity, String

### Community 25 - "Mob Target Player"
Cohesion: 0.43
Nodes (3): MobTargetPlayer, Entity, String

### Community 26 - "Target Selector"
Cohesion: 0.43
Nodes (4): MinecraftServer, ServerPlayer, UUID, TargetSelector

### Community 27 - "Wraith Texture (AI Test)"
Cohesion: 0.38
Nodes (7): Wraith Texture (AI Test Variant), Dark Brown and Near-Black Color Palette, Shadowy Grim Spectral Mood, Wraith Humanoid Stalker Creature, Muddy Dark Design Direction (Non-Ethereal), Muted Reddish-Brown Accents, Biped UV Atlas Texture Layout

### Community 28 - "Stalker Glowmask / Emissive"
Cohesion: 0.38
Nodes (7): Cyan Emissive Accent, Emissive Glow Mask, Glowing Eyes in the Dark, Red Emissive Region, Sparse Emissive Pixels on Transparent Sheet, Stalker Entity, Stalker Glowmask Texture

### Community 30 - "Stalker Entity Texture"
Cohesion: 0.38
Nodes (7): Dark Red and Black Color Palette, Head Texture Region, Horror Stalker Theme, Humanoid Creature Form, Minecraft 64x64 Player-Model Texture Atlas Layout, Pale Skin Limb Regions, Stalker Entity Texture

### Community 31 - "GROCK Stalker Emissive Texture"
Cohesion: 0.38
Nodes (7): Stalker Emissive Texture (GROCK test), AI-Generated Test Variant, Cyan/Teal Glow Region (AMBIGUOUS), Isolated Accent Glow Design Direction, Red/Orange Glow Region, Sparse Emissive Glow Map, Mostly Transparent/Black Base

### Community 32 - "GROCK Stalker Texture"
Cohesion: 0.38
Nodes (7): Grock Stalker Texture (Candidate), AI-Generated Test Design Direction (GROCK), Bloodied Red and Dark Navy Palette, Blotchy Gore/Wound Pattern, Horror / Unsettling Mood, Humanoid Stalker Creature, Minecraft Skin UV Layout (64x64)

### Community 33 - "Nightmare Zombie"
Cohesion: 0.47
Nodes (3): NightmareZombie, Entity, Monster

### Community 34 - "Fabric Data Generator"
Cohesion: 0.47
Nodes (4): ItfollowsDataGenerator, DataGeneratorEntrypoint, FabricDataGenerator, Override

### Community 35 - "Mob Bloodlust"
Cohesion: 0.53
Nodes (3): MobBloodlust, Entity, EntityType

### Community 36 - "Mob Build Bridge"
Cohesion: 0.60
Nodes (3): MobBuildBridge, Entity, Mob

### Community 37 - "Mob Dig"
Cohesion: 0.53
Nodes (3): MobDig, Entity, Monster

### Community 38 - "Stalker Texture Template & Model"
Cohesion: 0.40
Nodes (6): Stalker Blockbench Model, Stalker Texture Template, Dark Base Background, Facial Eye Markings Region, Head Texture Region (AMBIGUOUS), Stalker UV Layout

## Ambiguous Edges - Review These
- `Minecraft 64x64 Player-Model Texture Atlas Layout` → `Pale Skin Limb Regions`  [AMBIGUOUS]
  src/main/resources/assets/itfollows/textures/entity/stalker.png · relation: references
- `Cyan Emissive Accent` → `Stalker Entity`  [AMBIGUOUS]
  src/main/resources/assets/itfollows/textures/entity/stalker_glowmask.png · relation: conceptually_related_to
- `Wraith Texture (AI Test Variant)` → `Muted Reddish-Brown Accents`  [AMBIGUOUS]
  stalker_blockbench/TEST/CLAUDE/wraith_texture.jpg · relation: references
- `Dark Red Maroon Mouth` → `Fallen Hunter Creature Variant`  [AMBIGUOUS]
  stalker_blockbench/TEST/GPT/fallen_hunter_texture.png · relation: conceptually_related_to
- `Stalker Emissive Texture (GROCK test)` → `Cyan/Teal Glow Region (AMBIGUOUS)`  [AMBIGUOUS]
  stalker_blockbench/TEST/GROCK/stalker_emissive.png · relation: references
- `Red/Orange Glow Region` → `Cyan/Teal Glow Region (AMBIGUOUS)`  [AMBIGUOUS]
  stalker_blockbench/TEST/GROCK/stalker_emissive.png · relation: conceptually_related_to
- `Facial Eye Markings Region` → `Head Texture Region (AMBIGUOUS)`  [AMBIGUOUS]
  stalker_blockbench/stalker_texture_template.png · relation: conceptually_related_to

## Knowledge Gaps
- **58 isolated node(s):** `Override`, `Override`, `Override`, `Override`, `FatigueTier` (+53 more)
  These have ≤1 connection - possible missing edges or undocumented components.
- **12 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **What is the exact relationship between `Minecraft 64x64 Player-Model Texture Atlas Layout` and `Pale Skin Limb Regions`?**
  _Edge tagged AMBIGUOUS (relation: references) - confidence is low._
- **What is the exact relationship between `Cyan Emissive Accent` and `Stalker Entity`?**
  _Edge tagged AMBIGUOUS (relation: conceptually_related_to) - confidence is low._
- **What is the exact relationship between `Wraith Texture (AI Test Variant)` and `Muted Reddish-Brown Accents`?**
  _Edge tagged AMBIGUOUS (relation: references) - confidence is low._
- **What is the exact relationship between `Dark Red Maroon Mouth` and `Fallen Hunter Creature Variant`?**
  _Edge tagged AMBIGUOUS (relation: conceptually_related_to) - confidence is low._
- **What is the exact relationship between `Stalker Emissive Texture (GROCK test)` and `Cyan/Teal Glow Region (AMBIGUOUS)`?**
  _Edge tagged AMBIGUOUS (relation: references) - confidence is low._
- **What is the exact relationship between `Red/Orange Glow Region` and `Cyan/Teal Glow Region (AMBIGUOUS)`?**
  _Edge tagged AMBIGUOUS (relation: conceptually_related_to) - confidence is low._
- **What is the exact relationship between `Facial Eye Markings Region` and `Head Texture Region (AMBIGUOUS)`?**
  _Edge tagged AMBIGUOUS (relation: conceptually_related_to) - confidence is low._