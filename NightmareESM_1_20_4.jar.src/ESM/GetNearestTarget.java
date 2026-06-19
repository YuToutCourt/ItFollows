/*     */ package ESM;
/*     */ 
/*     */ import java.util.ArrayList;
/*     */ import java.util.List;
/*     */ import net.minecraft.server.MinecraftServer;
/*     */ import net.minecraft.server.level.ServerPlayer;
/*     */ import net.minecraft.server.players.PlayerList;
/*     */ import net.minecraft.world.entity.Entity;
/*     */ import net.minecraft.world.entity.LivingEntity;
/*     */ import net.minecraft.world.entity.Mob;
/*     */ import net.minecraft.world.entity.ai.targeting.TargetingConditions;
/*     */ import net.minecraft.world.entity.npc.Villager;
/*     */ import net.minecraft.world.level.Level;
/*     */ import net.minecraftforge.server.ServerLifecycleHooks;
/*     */ import org.apache.logging.log4j.LogManager;
/*     */ import org.apache.logging.log4j.Logger;
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ public class GetNearestTarget
/*     */ {
/*  26 */   private static final Logger LOGGER = LogManager.getLogger();
/*     */ 
/*     */ 
/*     */   
/*     */   public Entity Get(Entity Attacker) {
/*  31 */     MinecraftServer Curr_Server = ServerLifecycleHooks.getCurrentServer();
/*     */     
/*  33 */     PlayerList Player_List = Curr_Server.m_6846_();
/*     */     
/*  35 */     if (Curr_Server != null) {
/*     */ 
/*     */       
/*  38 */       int PlayerCount = Player_List.m_11309_();
/*     */ 
/*     */       
/*  41 */       if (PlayerCount > 0) {
/*     */         
/*  43 */         Entity Targeted_Entity = GetNearestEntity(Player_List, Attacker);
/*     */         
/*  45 */         return Targeted_Entity;
/*     */       } 
/*  47 */       return null;
/*     */     } 
/*     */     
/*  50 */     return null;
/*     */   }
/*     */ 
/*     */ 
/*     */   
/*     */   Entity GetNearestEntity(PlayerList Player_List, Entity Attacker) {
/*  56 */     List<Entity> Nearby_Entities = GetNearbyEntities(Player_List, Attacker);
/*  57 */     int Entity_Count = Nearby_Entities.size();
/*     */     
/*  59 */     if (Entity_Count > 0) {
/*     */       
/*  61 */       float Winning_Dist = 999.0F;
/*  62 */       Entity Winning_Target = Nearby_Entities.get(0);
/*     */ 
/*     */       
/*  65 */       for (int i = 0; i < Entity_Count; i++) {
/*     */ 
/*     */         
/*  68 */         Entity PotentialTgt = Nearby_Entities.get(i);
/*     */         
/*  70 */         if (Winning_Target != PotentialTgt)
/*     */         {
/*     */ 
/*     */ 
/*     */           
/*  75 */           if (isEntitySurvivalModeOrMob(PotentialTgt)) {
/*     */ 
/*     */ 
/*     */             
/*  79 */             float Dist = Attacker.m_20270_(PotentialTgt);
/*     */ 
/*     */             
/*  82 */             if (Dist < Winning_Dist) {
/*     */               
/*  84 */               Winning_Dist = Dist;
/*  85 */               Winning_Target = PotentialTgt;
/*     */             } 
/*     */           } 
/*     */         }
/*     */       } 
/*     */ 
/*     */       
/*  92 */       return Winning_Target;
/*     */     } 
/*  94 */     return null;
/*     */   }
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   
/*     */   List<Entity> GetNearbyEntities(PlayerList Player_List, Entity Attacker) {
/* 102 */     List<Entity> Compiled_Entities = new ArrayList<>();
/* 103 */     List<ServerPlayer> All_Players = Player_List.m_11314_();
/* 104 */     int PlayerCount = Player_List.m_11309_();
/*     */ 
/*     */     
/* 107 */     for (int i = 0; i < PlayerCount; i++) {
/* 108 */       ServerPlayer Plyr = All_Players.get(i);
/* 109 */       if (!Plyr.m_7500_() && Plyr.m_6084_()) {
/* 110 */         Compiled_Entities.add(Plyr);
/*     */       }
/*     */     } 
/*     */ 
/*     */     
/* 115 */     if (((Boolean)ESMConfig.isMonstersTargetVillagers.get()).booleanValue()) {
/*     */ 
/*     */ 
/*     */       
/* 119 */       TargetingConditions Tgt = TargetingConditions.m_148352_().m_26883_(64.0D);
/* 120 */       Mob Mob_Attacker = (Mob)Attacker;
/* 121 */       Level Lvl = MobInfo.GetMobLevel(Mob_Attacker);
/* 122 */       List<Villager> All_Nearby_Entities = Lvl.m_45971_(Villager.class, Tgt, (LivingEntity)Attacker, Attacker
/*     */           
/* 124 */           .m_20191_().m_82377_(16.0D, 4.0D, 16.0D));
/*     */ 
/*     */ 
/*     */       
/* 128 */       for (int j = 0; j < All_Nearby_Entities.size(); j++) {
/* 129 */         Villager Curr_Villager = All_Nearby_Entities.get(j);
/* 130 */         Villager villager1 = Curr_Villager;
/* 131 */         Compiled_Entities.add(villager1);
/*     */       } 
/*     */     } 
/*     */ 
/*     */ 
/*     */     
/* 137 */     return Compiled_Entities;
/*     */   }
/*     */ 
/*     */ 
/*     */ 
/*     */   
/*     */   boolean isEntitySurvivalModeOrMob(Entity Entity_Class) {
/* 144 */     if (Entity_Class instanceof ServerPlayer) {
/* 145 */       ServerPlayer Server_Player = (ServerPlayer)Entity_Class;
/* 146 */       return Server_Player.m_7500_();
/*     */     } 
/* 148 */     return true;
/*     */   }
/*     */ }


/* Location:              C:\Users\wapzi\Desktop\Plugins mC\mod\NightmareESM_1_20_4.jar!\ESM\GetNearestTarget.class
 * Java compiler version: 17 (61.0)
 * JD-Core Version:       1.1.3
 */