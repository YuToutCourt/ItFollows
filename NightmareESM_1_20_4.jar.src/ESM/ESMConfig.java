/*     */ package ESM;
/*     */ 
/*     */ import net.minecraftforge.common.ForgeConfigSpec;
/*     */ 
/*     */ public final class ESMConfig
/*     */ {
/*   7 */   public static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   
/*     */   public static final ForgeConfigSpec SPEC;
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   
/*     */   static {
/*  75 */     BUILDER.push("Nightmare: Epic Siege Configuration");
/*     */   }
/*     */   
/*  78 */   public static final ForgeConfigSpec.ConfigValue<Boolean> isSiegeModeEnabled = (ForgeConfigSpec.ConfigValue<Boolean>)BUILDER.comment("Enable Enemies Swarming You").define("enablesiege", true);
/*  79 */   public static final ForgeConfigSpec.ConfigValue<Boolean> Entity_Duplication = (ForgeConfigSpec.ConfigValue<Boolean>)BUILDER.comment("Enable Increased Enemy Presence").define("incenemies", true);
/*  80 */   public static final ForgeConfigSpec.ConfigValue<Boolean> isSpecialJockeyMobsAllowed = (ForgeConfigSpec.ConfigValue<Boolean>)BUILDER.comment("Should special mobs spawn riding other mobs?").define("isjockies", true);
/*  81 */   public static final ForgeConfigSpec.ConfigValue<Boolean> isChargedCreeperAllowed = (ForgeConfigSpec.ConfigValue<Boolean>)BUILDER.comment("Charged Creepers Spawn Naturally").define("ischargedcreepers", false);
/*  82 */   public static final ForgeConfigSpec.ConfigValue<Boolean> AngryEntities = (ForgeConfigSpec.ConfigValue<Boolean>)BUILDER.comment("Allow Bloodlusting Enemies").define("isbloodlusting", true);
/*  83 */   public static final ForgeConfigSpec.ConfigValue<Boolean> GiveZombiesPickaxe = (ForgeConfigSpec.ConfigValue<Boolean>)BUILDER.comment("Give Zombies Pickaxes and Weapons?").define("iszombiepickaxes", true);
/*  84 */   public static final ForgeConfigSpec.ConfigValue<Boolean> GiveZombiesArmor = (ForgeConfigSpec.ConfigValue<Boolean>)BUILDER.comment("Give Zombies Armor?").define("iszombiearmor", true);
/*  85 */   public static final ForgeConfigSpec.ConfigValue<Boolean> GiveSkeletonsArmor = (ForgeConfigSpec.ConfigValue<Boolean>)BUILDER.comment("Give Skeletons Armor?").define("isskeletonarmor", true);
/*  86 */   public static final ForgeConfigSpec.ConfigValue<Boolean> ZombiesLayTNT = (ForgeConfigSpec.ConfigValue<Boolean>)BUILDER.comment("Allow Zombies to Lay TNT?").define("iszombietnt", true);
/*  87 */   public static final ForgeConfigSpec.ConfigValue<Double> ZombieTNTChance = BUILDER.comment("Chance of zombies placing TNT each tick if in distance.").define("zombietntchance", Double.valueOf(0.25D));
/*  88 */   public static final ForgeConfigSpec.ConfigValue<Integer> ZombieTNTDistance = BUILDER.comment("How close zombies need to be to player to allow setting TNT.").define("zombietntdistance", Integer.valueOf(2));
/*  89 */   public static final ForgeConfigSpec.ConfigValue<Boolean> AllowZombieGriefing = (ForgeConfigSpec.ConfigValue<Boolean>)BUILDER.comment("Allow Zombies To Destroy Blocks?").define("iszombiegriefing", true);
/*  90 */   public static final ForgeConfigSpec.ConfigValue<Boolean> AllowZombieGriefingOnlyIfPickaxe = (ForgeConfigSpec.ConfigValue<Boolean>)BUILDER.comment("If true, zombies can only destroy blocks if they have a pickaxe.").define("iszombiedigifpickaxeonly", false);
/*  91 */   public static final ForgeConfigSpec.ConfigValue<Boolean> AllowZombieBuilding = (ForgeConfigSpec.ConfigValue<Boolean>)BUILDER.comment("Allow Zombies To Place Blocks?").define("iszombiebuilding", true);
/*  92 */   public static final ForgeConfigSpec.ConfigValue<Boolean> SpidersShootWebs = (ForgeConfigSpec.ConfigValue<Boolean>)BUILDER.comment("Allow Spiders to Shoot Webs?").define("isspidershootwebs", true);
/*  93 */   public static final ForgeConfigSpec.ConfigValue<Boolean> isCreeperBreachingAllowed = (ForgeConfigSpec.ConfigValue<Boolean>)BUILDER.comment("Can Creepers Breach Walls?").define("iscreeperbreaching", true);
/*  94 */   public static final ForgeConfigSpec.ConfigValue<Boolean> AllowSleeping = (ForgeConfigSpec.ConfigValue<Boolean>)BUILDER.comment("Allow Sleeping?").define("allowsleep", false);
/*  95 */   public static final ForgeConfigSpec.ConfigValue<Boolean> AllowMonsterInfighting = (ForgeConfigSpec.ConfigValue<Boolean>)BUILDER.comment("Allow Monster Infighting?").define("allowmonsterinfighting", false);
/*  96 */   public static final ForgeConfigSpec.ConfigValue<Boolean> ZombiesLightFires = (ForgeConfigSpec.ConfigValue<Boolean>)BUILDER.comment("Allow Zombies and Piglins to light flammable things on fire?").define("zombieslightfires", true);
/*  97 */   public static final ForgeConfigSpec.ConfigValue<Boolean> AllowSuperSkeletons = (ForgeConfigSpec.ConfigValue<Boolean>)BUILDER.comment("Allow Super Skeletons to Spawn in the Game?").define("allowsuperskeletons", true);
/*  98 */   public static final ForgeConfigSpec.ConfigValue<Boolean> isMonstersTargetVillagers = (ForgeConfigSpec.ConfigValue<Boolean>)BUILDER.comment("Should monsters target villagers?").define("isMonstersTargetVillagers", true);
/*     */   
/* 100 */   public static final ForgeConfigSpec.ConfigValue<Integer> InvadeEveryXDays = BUILDER.comment("What days should sieges occur? 1 = default (everyday, constantly). 2 = every other day, 7 every week, and so on.").define("siegerecurrence", Integer.valueOf(1));
/* 101 */   public static final ForgeConfigSpec.ConfigValue<Integer> StartInvasionAfterXTicks = BUILDER.comment("If number of in-game Ticks (Day is 24000 ticks) is greater or equal to this amount, start invasion.").define("startinvasionafterxticks", Integer.valueOf(0));
/* 102 */   public static final ForgeConfigSpec.ConfigValue<Double> SpecialJockeyGenerationChance = BUILDER.comment("Chance of special mob spawning and riding another entity out of 100.").define("jockeychance", Double.valueOf(5.0D));
/* 103 */   public static final ForgeConfigSpec.ConfigValue<Integer> DuplicationChance = BUILDER.comment("Chance Entity Duplicate on Spawn (Out of 100)").define("dupechance", Integer.valueOf(10));
/* 104 */   public static final ForgeConfigSpec.ConfigValue<Integer> ChargedCreeperChance = BUILDER.comment("Chance of Creepers Spawning Charged Out of 100").define("chargedchance", Integer.valueOf(1));
/* 105 */   public static final ForgeConfigSpec.ConfigValue<Integer> AngryEntityChance = BUILDER.comment("Chance of Entities Spawning Bloodlusted Out of 100").define("angryentitychance", Integer.valueOf(10));
/* 106 */   public static final ForgeConfigSpec.ConfigValue<Integer> CreeperBreachingDistance = BUILDER.comment("Distance at Which Creepers Will Breach if Blocked (in Blocks)").define("breachingdist", Integer.valueOf(64));
/* 107 */   public static final ForgeConfigSpec.ConfigValue<Integer> CreeperStrikingDistance = BUILDER.comment("Creepers Will Explode if This Close to Player (in Blocks)").define("creeperexplodedist", Integer.valueOf(6));
/* 108 */   public static final ForgeConfigSpec.ConfigValue<Integer> CreeperAboveExplodeDistance = BUILDER.comment("Distance At Which Creepers Breech the Ground (in Blocks)").define("creepersbreechgrounddist", Integer.valueOf(6));
/* 109 */   public static final ForgeConfigSpec.ConfigValue<Integer> CreeperObstructedExplodeTicks = BUILDER.comment("Ticks Until Blocked Creeper Explodes (in Game Ticks)").define("obstructedcreeperexpticks", Integer.valueOf(60));
/* 110 */   public static final ForgeConfigSpec.ConfigValue<Integer> MobMaxLife = BUILDER.comment("Maximum ticks in an entity's life until they despawn (if their target is the player). 20 Ticks = 1 second.").define("mobmaxlife", Integer.valueOf(4800)); public static final ForgeConfigSpec.ConfigValue<Double> ChanceOfNuclearCreeper;
/* 111 */   public static final ForgeConfigSpec.ConfigValue<Integer> NuclearCreeperExplosionRadius = BUILDER.comment("Nuclear Creeper Explosion Radius (Default vanilla is 3").define("nuclearcreeperexplosionradius", Integer.valueOf(10));
/* 112 */   public static final ForgeConfigSpec.ConfigValue<Integer> NuclearCreeperFuse = BUILDER.comment("How long (in ticks) Nuclear Creeper is lit before it blows up").define("nuclearcreeperfuse", Integer.valueOf(120)); static {
/* 113 */     ChanceOfNuclearCreeper = BUILDER.comment("Chance of Nuclear Creeper out of 100").define("chancenuclearcreeper", Double.valueOf(0.0D));
/* 114 */     ChanceOfPickaxe = BUILDER.comment("Chance of Zombies Recieving Special Item or Pickaxe Out of 100").define("pickaxechance", Integer.valueOf(20));
/* 115 */     ChanceOfZombieLightFire = BUILDER.comment("Chance of Zombies Lighting Flammable Items on Fire Per Tick Out of 100").define("zombiefirechance", Double.valueOf(1.0D));
/* 116 */     ChanceOfSuperSkeleton = BUILDER.comment("Chance of More Powerful Skeleton Spawning").define("superskeletonchance", Double.valueOf(1.0D));
/* 117 */     EntityDigDelay = BUILDER.comment("Cooloff time for entities breaking blocks in ticks").define("entitydigdelay", Integer.valueOf(5));
/* 118 */     EntityBuildDelay = BUILDER.comment("Cooloff time for entities placing blocks in ticks").define("entitybuilddelay", Integer.valueOf(5));
/*     */     
/* 120 */     MonsterAttackDistanceAboveGround = BUILDER.comment("Above Ground Monster Attack Distance (in Blocks)").define("grounddist", Integer.valueOf(96));
/* 121 */     MonsterAttackDistanceUnderground = BUILDER.comment("Below Ground Monster Attack Distance (in Blocks)").define("cavedist", Integer.valueOf(48));
/*     */     
/* 123 */     SpiderShootWebChance = BUILDER.comment("Chance out of 100 for spider shooting webs each tick. Default = 0.5").define("spiderwebchance", Double.valueOf(0.5D));
/* 124 */     SpiderShootWebDist = BUILDER.comment("Distance at which spider can shoot a web at player. Default = 2 blocks").define("spiderwebdist", Integer.valueOf(2));
/*     */     
/* 126 */     MaxDuplicationClones = BUILDER.comment("Maximum Duplicated Enemies. Recommend Min. 1, Max. 4. Must be greater than min duplication clones and greater than 0.").define("maxdupes", Integer.valueOf(3));
/* 127 */     MinDuplicationClones = BUILDER.comment("Maximum Duplicated Enemies. Recommend Min. 0, Max. 4. Must be less than max duplication clones, or, will use max dupes value.").define("mindupes", Integer.valueOf(0));
/*     */     
/* 129 */     MaxDungeonDuplicationClones = BUILDER.comment("Maximum Duplicated Enemies FROM A DUNGEON. Recommend Min. 1, Max. 6. Must be greater than min duplication clones and greater than 0.").define("maxdungeondupes", Integer.valueOf(6));
/* 130 */     MinDungeonDuplicationClones = BUILDER.comment("Maximum Duplicated Enemies FROM A DUNGEON. Recommend Min. 0, Max. 3. Must be less than max duplication clones, or, will use max dupes value.").define("mindungeondupes", Integer.valueOf(1));
/*     */ 
/*     */ 
/*     */     
/* 134 */     SiegeEntityWhitelist = BUILDER.comment("Entities that will besiege players.").define("siegewhitelist", "spider,cave spider,creeper,skeleton,witch,ghast,giant,husk,hoglin,pillager,vindicator,illusioner,evoker,ravager,shulker,silverfish,stray,slime,witch,zoglin,zombie,zombie villager,zombified piglin,zoglin,drowned,zombie villager,Wither Skeleton,pillager");
/*     */ 
/*     */ 
/*     */     
/* 138 */     BlocksEntitiesCannotDigThrough = BUILDER.comment("Any part of a block name, which defines which blocks that siege monsters CANNOT break to try and get to your positon.").define("siegebreakableblockblacklist", "_ore,_sign,air,water,lava,_bed,rail,_block,obsidian,torch,fire,spawner,ladder,furnace,level,iron_door,pressure_plate,_button,sugar_cane,juke,soul_,portal,cake,repeater,trapdoor,iron_bars,chain,vine,lily_pad,enchanting,brewing,cauldron,end_,dragon,potted_,skull,_head,anvil,chest,dropper,comparator,hopper,detector,Pillar,terracotta,light,prismarine,book,grass,snow,flower,sapling,bush,stem,dragon,carrots,potatoes,detector,powered,concrete,barrier,carpet,banner,beet,void,shulker,structure,compost,piston,dispenser,fern,pickle,chorus,purpur,beacon,conduit,frame,lectern,smith,ancient,armor,wither,barrel,cutter,hive,lode,respawn,sculk,brick,copper");
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */     
/* 146 */     RideableMobs = BUILDER.comment("Defines all mobs that can spawn as rideable by spawning monsters. E.g. 'chicken' will cause enemies to spawn riding chickens.").define("rideablemobs", "creeper,spider,skeleton,chicken,pig,skeleton horse,zombie horse");
/*     */ 
/*     */     
/* 149 */     JockeyMobs = BUILDER.comment("Mobs that can spawn riding other mobs.").define("jockeymobs", "creeper,skeleton,zombie,pillager,zombified piglin");
/*     */ 
/*     */     
/* 152 */     BUILDER.pop();
/* 153 */     SPEC = BUILDER.build();
/*     */   }
/*     */   
/*     */   public static final ForgeConfigSpec.ConfigValue<Integer> ChanceOfPickaxe;
/*     */   public static final ForgeConfigSpec.ConfigValue<Double> ChanceOfZombieLightFire;
/*     */   public static final ForgeConfigSpec.ConfigValue<Double> ChanceOfSuperSkeleton;
/*     */   public static final ForgeConfigSpec.ConfigValue<Integer> EntityDigDelay;
/*     */   public static final ForgeConfigSpec.ConfigValue<Integer> EntityBuildDelay;
/*     */   public static final ForgeConfigSpec.ConfigValue<Integer> MonsterAttackDistanceAboveGround;
/*     */   public static final ForgeConfigSpec.ConfigValue<Integer> MonsterAttackDistanceUnderground;
/*     */   public static final ForgeConfigSpec.ConfigValue<Double> SpiderShootWebChance;
/*     */   public static final ForgeConfigSpec.ConfigValue<Integer> SpiderShootWebDist;
/*     */   public static final ForgeConfigSpec.ConfigValue<Integer> MaxDuplicationClones;
/*     */   public static final ForgeConfigSpec.ConfigValue<Integer> MinDuplicationClones;
/*     */   public static final ForgeConfigSpec.ConfigValue<Integer> MaxDungeonDuplicationClones;
/*     */   public static final ForgeConfigSpec.ConfigValue<Integer> MinDungeonDuplicationClones;
/*     */   public static final ForgeConfigSpec.ConfigValue<String> SiegeEntityWhitelist;
/*     */   public static final ForgeConfigSpec.ConfigValue<String> BlocksEntitiesCannotDigThrough;
/*     */   public static final ForgeConfigSpec.ConfigValue<String> RideableMobs;
/*     */   public static final ForgeConfigSpec.ConfigValue<String> JockeyMobs;
/*     */ }


/* Location:              C:\Users\wapzi\Desktop\Plugins mC\mod\NightmareESM_1_20_4.jar!\ESM\ESMConfig.class
 * Java compiler version: 17 (61.0)
 * JD-Core Version:       1.1.3
 */