/*     */ package ESM;
/*     */ 
/*     */ import ESM.Base.NightmareCreeper;
/*     */ import ESM.Base.NightmareSkeleton;
/*     */ import ESM.Base.NightmareSpider;
/*     */ import ESM.Base.NightmareZombie;
/*     */ import ESM.MobBehavs.CreeperBreachWalls;
/*     */ import ESM.MobBehavs.MobBloodlust;
/*     */ import ESM.MobBehavs.MobBuildBridge;
/*     */ import ESM.MobBehavs.MobBuildUp;
/*     */ import ESM.MobBehavs.MobDig;
/*     */ import ESM.MobBehavs.MobDigDown;
/*     */ import ESM.MobBehavs.MobDigUp;
/*     */ import ESM.MobBehavs.MobPlaceTNT;
/*     */ import ESM.MobBehavs.MobTargetPlayer;
/*     */ import ESM.MobBehavs.SpawnRideableMob;
/*     */ import ESM.MobBehavs.SpiderShootWeb;
/*     */ import com.mojang.logging.LogUtils;
/*     */ import java.util.Optional;
/*     */ import net.minecraft.world.entity.Entity;
/*     */ import net.minecraft.world.entity.EntityType;
/*     */ import net.minecraft.world.entity.EquipmentSlot;
/*     */ import net.minecraft.world.entity.LivingEntity;
/*     */ import net.minecraft.world.entity.Mob;
/*     */ import net.minecraft.world.entity.MobSpawnType;
/*     */ import net.minecraft.world.entity.monster.Creeper;
/*     */ import net.minecraft.world.entity.player.Player;
/*     */ import net.minecraft.world.item.ItemStack;
/*     */ import net.minecraft.world.item.Items;
/*     */ import net.minecraft.world.level.Level;
/*     */ import net.minecraftforge.common.MinecraftForge;
/*     */ import net.minecraftforge.event.entity.living.LivingEvent;
/*     */ import net.minecraftforge.event.entity.living.MobSpawnEvent;
/*     */ import net.minecraftforge.event.entity.player.PlayerSleepInBedEvent;
/*     */ import net.minecraftforge.eventbus.api.EventPriority;
/*     */ import net.minecraftforge.eventbus.api.IEventBus;
/*     */ import net.minecraftforge.eventbus.api.SubscribeEvent;
/*     */ import net.minecraftforge.fml.ModLoadingContext;
/*     */ import net.minecraftforge.fml.common.Mod;
/*     */ import net.minecraftforge.fml.config.IConfigSpec;
/*     */ import net.minecraftforge.fml.config.ModConfig;
/*     */ import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
/*     */ import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
/*     */ import org.slf4j.Logger;
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
/*     */ @Mod("nightmareinminecraft")
/*     */ public class NightmareMain
/*     */ {
/*     */   public static final String MODID = "nightmareinminecraft";
/*  75 */   private static final Logger LOGGER = LogUtils.getLogger();
/*     */   
/*     */   public NightmareMain() {
/*  78 */     IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
/*     */ 
/*     */     
/*  81 */     modEventBus.addListener(this::commonSetup);
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */     
/*  91 */     MinecraftForge.EVENT_BUS.register(this);
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */     
/*  98 */     ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, (IConfigSpec)ESMConfig.SPEC, "esm-config.toml");
/*     */     
/* 100 */     LOGGER.info("Loaded Nightmare Epic Siege Mod.");
/*     */   }
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   
/*     */   @SubscribeEvent(priority = EventPriority.NORMAL)
/*     */   public void onEntitySpawn(MobSpawnEvent.FinalizeSpawn e) {
/* 109 */     if (((Boolean)ESMConfig.isSiegeModeEnabled.get()).booleanValue()) {
/*     */ 
/*     */       
/* 112 */       Mob mob = e.getEntity();
/*     */ 
/*     */       
/* 115 */       if (mob instanceof Mob) {
/*     */ 
/*     */         
/* 118 */         Mob M = mob;
/*     */ 
/*     */         
/* 121 */         if (isSiegeDay(M)) {
/*     */           
/* 123 */           Level Lvl = MobInfo.GetMobLevel(M);
/*     */ 
/*     */           
/* 126 */           if (Lvl != null && !Lvl.f_46443_) {
/*     */ 
/*     */             
/* 129 */             new MobTargetPlayer((Entity)mob);
/*     */ 
/*     */             
/* 132 */             if (mob.m_6095_() == EntityType.f_20501_ || mob
/* 133 */               .m_6095_() == EntityType.f_20530_ || mob
/* 134 */               .m_6095_() == EntityType.f_20458_)
/*     */             {
/*     */               
/* 137 */               new NightmareZombie((Entity)mob);
/*     */             }
/*     */ 
/*     */             
/* 141 */             if (mob.m_6095_() == EntityType.f_20558_)
/*     */             {
/* 143 */               if (((Boolean)ESMConfig.isChargedCreeperAllowed.get()).booleanValue())
/*     */               {
/* 145 */                 new NightmareCreeper((Entity)mob, ((Integer)ESMConfig.ChargedCreeperChance.get()).intValue());
/*     */               }
/*     */             }
/*     */ 
/*     */             
/* 150 */             if (mob.m_6095_() == EntityType.f_20524_) {
/* 151 */               new NightmareSkeleton((Entity)mob);
/*     */             
/*     */             }
/* 154 */             else if (mob.m_6095_() == EntityType.f_20479_ || mob
/* 155 */               .m_6095_() == EntityType.f_20554_) {
/* 156 */               new NightmareSpider((Entity)mob);
/*     */             } 
/*     */             
/* 159 */             MobSpawnType Spawn_Type = e.getSpawnType();
/*     */             
/* 161 */             if (((Boolean)ESMConfig.Entity_Duplication.get()).booleanValue()) {
/* 162 */               int MaxDupes = ((Integer)ESMConfig.MaxDuplicationClones.get()).intValue();
/* 163 */               int MinDupes = ((Integer)ESMConfig.MinDuplicationClones.get()).intValue();
/* 164 */               if (MinDupes > MaxDupes) {
/* 165 */                 MinDupes = MaxDupes;
/*     */               }
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */               
/* 173 */               if (Spawn_Type != MobSpawnType.MOB_SUMMONED && Spawn_Type != MobSpawnType.CHUNK_GENERATION) {
/*     */                 
/* 175 */                 int TotalDupes = (new RNG()).GetInt(MinDupes, MaxDupes);
/* 176 */                 if (ShouldDuplicate()) {
/* 177 */                   new DuplicateMob(M, TotalDupes);
/*     */                 }
/*     */               } 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */               
/* 187 */               if (Spawn_Type == MobSpawnType.SPAWNER) {
/*     */                 
/* 189 */                 int MaxDungeonDupes = ((Integer)ESMConfig.MaxDungeonDuplicationClones.get()).intValue();
/* 190 */                 int MinDungeonDupes = ((Integer)ESMConfig.MinDungeonDuplicationClones.get()).intValue();
/* 191 */                 if (MinDungeonDupes > MaxDungeonDupes) {
/* 192 */                   MinDungeonDupes = MaxDungeonDupes;
/*     */                 }
/*     */                 
/* 195 */                 int TotalDupes = (new RNG()).GetInt(MinDungeonDupes, MaxDungeonDupes);
/*     */ 
/*     */                 
/* 198 */                 new DuplicateMob(M, TotalDupes);
/*     */               } 
/*     */             } 
/*     */ 
/*     */             
/* 203 */             if (((Boolean)ESMConfig.isSpecialJockeyMobsAllowed.get()).booleanValue()) {
/*     */               
/* 205 */               double RNG_Val = (new RNG()).GetDouble(0.0D, 100.0D);
/* 206 */               double Jockey_Chance = ((Double)ESMConfig.SpecialJockeyGenerationChance.get()).doubleValue();
/* 207 */               if (RNG_Val < Jockey_Chance)
/*     */               {
/* 209 */                 if (Spawn_Type != MobSpawnType.MOB_SUMMONED)
/*     */                 {
/*     */                   
/* 212 */                   new SpawnRideableMob((Entity)mob);
/*     */                 }
/*     */               }
/*     */             } 
/*     */ 
/*     */ 
/*     */             
/* 219 */             if (((Boolean)ESMConfig.AngryEntities.get()).booleanValue()) {
/* 220 */               float Bloodlust_RNG = (new RNG()).GetInt(0, 100);
/* 221 */               if (Bloodlust_RNG < ((Integer)ESMConfig.AngryEntityChance.get()).intValue())
/*     */               {
/* 223 */                 new MobBloodlust((Entity)mob);
/*     */               }
/*     */             } 
/*     */           } 
/*     */         } 
/*     */       } 
/*     */     } 
/*     */   }
/*     */ 
/*     */ 
/*     */   
/*     */   boolean ShouldDuplicate() {
/* 235 */     int RNG_Val = (new RNG()).GetInt(0, 100);
/* 236 */     return (RNG_Val < ((Integer)ESMConfig.DuplicationChance.get()).intValue());
/*     */   }
/*     */ 
/*     */ 
/*     */   
/*     */   @SubscribeEvent
/*     */   public void EntityTick(LivingEvent.LivingTickEvent e) {
/* 243 */     if (((Boolean)ESMConfig.isSiegeModeEnabled.get()).booleanValue()) {
/*     */       
/* 245 */       LivingEntity livingEntity = e.getEntity();
/*     */ 
/*     */       
/* 248 */       if (livingEntity instanceof Mob) {
/*     */         
/* 250 */         Mob Mob_Class = (Mob)livingEntity;
/*     */         
/* 252 */         if (!Mob_Class.m_8077_()) {
/* 253 */           LivingMobTick(e, (Entity)livingEntity);
/*     */         }
/* 255 */         else if (livingEntity instanceof Creeper) {
/* 256 */           Creeper C = (Creeper)livingEntity;
/* 257 */           NightmareCreeper.CreeperTick(C);
/*     */         } 
/*     */       } 
/*     */     } 
/*     */   }
/*     */ 
/*     */ 
/*     */ 
/*     */   
/*     */   void LivingMobTick(LivingEvent.LivingTickEvent e, Entity Entity_Class) {
/* 267 */     if (Entity_Class instanceof Mob) {
/*     */ 
/*     */       
/* 270 */       long TimeStamp = System.currentTimeMillis();
/*     */ 
/*     */       
/* 273 */       Mob Entity_Mob = (Mob)Entity_Class;
/*     */       
/* 275 */       if (isTick(Entity_Mob) && 
/* 276 */         isSiegeDay(Entity_Mob)) {
/*     */         
/* 278 */         Level Lvl = MobInfo.GetMobLevel(Entity_Mob);
/* 279 */         if (Lvl != null && !Lvl.f_46443_) {
/*     */           
/* 281 */           new MobTargetPlayer(Entity_Class);
/*     */ 
/*     */           
/* 284 */           if (Entity_Class.m_6095_() == EntityType.f_20558_) {
/*     */             
/* 286 */             Creeper C = (Creeper)Entity_Class;
/* 287 */             if (((Boolean)ESMConfig.isCreeperBreachingAllowed.get()).booleanValue())
/*     */             {
/* 289 */               new CreeperBreachWalls((Creeper)Entity_Class);
/*     */             }
/*     */             
/* 292 */             NightmareCreeper.CreeperTick(C);
/*     */           } 
/*     */ 
/*     */           
/* 296 */           if (Entity_Class instanceof net.minecraft.world.entity.monster.Zombie) {
/*     */ 
/*     */             
/* 299 */             boolean isMobGriefingAllowed = ((Boolean)ESMConfig.AllowZombieGriefing.get()).booleanValue();
/*     */ 
/*     */             
/* 302 */             if (isMobGriefingAllowed) {
/*     */ 
/*     */               
/* 305 */               boolean isPickaxeRule = ((Boolean)ESMConfig.AllowZombieGriefingOnlyIfPickaxe.get()).booleanValue();
/*     */               
/* 307 */               boolean isBlockedByPickaxeRule = false;
/* 308 */               if (isPickaxeRule) {
/* 309 */                 isBlockedByPickaxeRule = true;
/* 310 */                 ItemStack I = Entity_Mob.m_6844_(EquipmentSlot.MAINHAND);
/* 311 */                 if (I.m_41720_() == Items.f_42427_ || I.m_41720_() == Items.f_42422_ || I
/* 312 */                   .m_41720_() == Items.f_42385_ || I
/* 313 */                   .m_41720_() == Items.f_42390_)
/*     */                 {
/*     */ 
/*     */                   
/* 317 */                   isBlockedByPickaxeRule = false;
/*     */                 }
/*     */               } 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */               
/* 328 */               if (!isBlockedByPickaxeRule) {
/*     */                 MobDig mobDig;
/*     */                 
/*     */                 MobDigUp mobDigUp;
/*     */                 
/*     */                 MobDigDown mobDigDown;
/* 334 */                 int Curr_Dig_Cycle = Clocker.GetIndexFromTime(3);
/*     */                 
/* 336 */                 switch (Curr_Dig_Cycle) {
/*     */                   case 0:
/* 338 */                     mobDig = new MobDig(Entity_Class);
/*     */                   
/*     */                   case 1:
/* 341 */                     mobDigUp = new MobDigUp(Entity_Mob);
/*     */                   
/*     */                   case 2:
/* 344 */                     mobDigDown = new MobDigDown(Entity_Mob);
/*     */                     break;
/*     */                 } 
/*     */ 
/*     */ 
/*     */               
/*     */               } 
/* 351 */               MobBuildUp Zombies_Build_Up = new MobBuildUp(Entity_Mob);
/* 352 */               MobBuildBridge Zombie_Build_Bridge = new MobBuildBridge(Entity_Mob);
/*     */ 
/*     */               
/* 355 */               if (((Boolean)ESMConfig.ZombiesLayTNT.get()).booleanValue())
/*     */               {
/* 357 */                 new MobPlaceTNT(Entity_Class);
/*     */               }
/*     */ 
/*     */               
/* 361 */               if (((Boolean)ESMConfig.ZombiesLightFires.get()).booleanValue());
/*     */             } 
/*     */           } 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */           
/* 369 */           if (Entity_Class.m_6095_() == EntityType.f_20479_ || Entity_Class
/* 370 */             .m_6095_() == EntityType.f_20554_)
/*     */           {
/* 372 */             if (((Boolean)ESMConfig.SpidersShootWebs.get()).booleanValue())
/*     */             {
/* 374 */               new SpiderShootWeb(Entity_Mob);
/*     */             }
/*     */           }
/*     */         } 
/*     */       } 
/*     */     } 
/*     */   }
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   
/*     */   boolean isSiegeDay(Mob Curr_Mob) {
/* 388 */     Level Lvl = MobInfo.GetMobLevel(Curr_Mob);
/* 389 */     if (Lvl != null && !Lvl.f_46443_) {
/* 390 */       long Uptime = Lvl.m_46468_();
/* 391 */       long GracePeriod = ((Integer)ESMConfig.StartInvasionAfterXTicks.get()).intValue();
/* 392 */       if (Uptime < GracePeriod)
/* 393 */         return false; 
/* 394 */       long DayCount = Uptime / 24000L;
/* 395 */       boolean RetVal = (DayCount % ((Integer)ESMConfig.InvadeEveryXDays.get()).intValue() == 0L);
/*     */ 
/*     */       
/* 398 */       return RetVal;
/*     */     } 
/* 400 */     return false;
/*     */   }
/*     */ 
/*     */ 
/*     */   
/*     */   boolean isTick(Mob Curr_Mob) {
/* 406 */     Level Lvl = MobInfo.GetMobLevel(Curr_Mob);
/* 407 */     if (Lvl != null && !Lvl.f_46443_) {
/* 408 */       long Uptime = Lvl.m_46468_();
/* 409 */       return (Uptime % 20L == 0L);
/*     */     } 
/* 411 */     return false;
/*     */   }
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   
/*     */   @SubscribeEvent
/*     */   public void cancelSleep(PlayerSleepInBedEvent e) {
/* 420 */     if (((Boolean)ESMConfig.isSiegeModeEnabled.get()).booleanValue() && 
/* 421 */       !((Boolean)ESMConfig.AllowSleeping.get()).booleanValue()) {
/*     */       
/* 423 */       Player Player_Entity = e.getEntity();
/* 424 */       new SetPlayerSpawn(Player_Entity, e.getPos());
/*     */ 
/*     */       
/* 427 */       e.setResult(Player.BedSleepingProblem.NOT_SAFE);
/*     */     } 
/*     */   }
/*     */ 
/*     */ 
/*     */ 
/*     */   
/*     */   public static EntityType[] GetEntitiesFromSerialized(String SerialList) {
/* 435 */     String[] Entity_String_List = SerialList.split(",");
/* 436 */     EntityType[] RetVal = new EntityType[Entity_String_List.length];
/* 437 */     for (int i = 0; i < Entity_String_List.length; i++) {
/* 438 */       String Curr_Entity = Entity_String_List[i];
/* 439 */       EntityType Entity_Type_Class = ToEntity(Curr_Entity);
/* 440 */       RetVal[i] = Entity_Type_Class;
/*     */     } 
/*     */     
/* 443 */     return RetVal;
/*     */   }
/*     */ 
/*     */   
/*     */   public static EntityType ToEntity(String Name) {
/* 448 */     Optional<EntityType<?>> Entity_Type = EntityType.m_20632_(Name);
/* 449 */     if (Entity_Type.isPresent()) {
/* 450 */       return Entity_Type.get();
/*     */     }
/* 452 */     return null;
/*     */   }
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
/*     */   public static String EntitiesToSerialList(EntityType[] Entity_List) {
/* 471 */     String Entity_List_Str = "";
/* 472 */     for (int i = 0; i < Entity_List.length; i++) {
/* 473 */       EntityType Curr_Entity = Entity_List[i];
/* 474 */       if (Curr_Entity == null) {
/* 475 */         Entity_List_Str = Entity_List_Str + "NULL,";
/*     */       } else {
/* 477 */         Entity_List_Str = Entity_List_Str + Entity_List_Str + ",";
/*     */       } 
/*     */     } 
/*     */ 
/*     */     
/* 482 */     return Entity_List_Str;
/*     */   }
/*     */   
/*     */   private void commonSetup(FMLCommonSetupEvent event) {}
/*     */ }


/* Location:              C:\Users\wapzi\Desktop\Plugins mC\mod\NightmareESM_1_20_4.jar!\ESM\NightmareMain.class
 * Java compiler version: 17 (61.0)
 * JD-Core Version:       1.1.3
 */