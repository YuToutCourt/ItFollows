/*     */ package ESM.MobBehavs;
/*     */ 
/*     */ import ESM.Clocker;
/*     */ import ESM.ESMConfig;
/*     */ import ESM.MobInfo;
/*     */ import ESM.NightmareMain;
/*     */ import net.minecraft.core.BlockPos;
/*     */ import net.minecraft.server.level.ServerLevel;
/*     */ import net.minecraft.world.entity.Entity;
/*     */ import net.minecraft.world.entity.EntityType;
/*     */ import net.minecraft.world.entity.Mob;
/*     */ import net.minecraft.world.entity.MobSpawnType;
/*     */ import net.minecraft.world.entity.player.Player;
/*     */ import net.minecraft.world.item.ItemStack;
/*     */ import net.minecraft.world.item.Items;
/*     */ import net.minecraft.world.level.ItemLike;
/*     */ import org.apache.logging.log4j.LogManager;
/*     */ import org.apache.logging.log4j.Logger;
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ public class SpawnRideableMob
/*     */ {
/*  25 */   private static final Logger LOGGER = LogManager.getLogger();
/*     */ 
/*     */   
/*     */   static EntityType[] RideableList;
/*     */   
/*     */   static EntityType[] JockeyList;
/*     */ 
/*     */   
/*     */   public SpawnRideableMob(Entity Jockey) {
/*  34 */     RefreshEntityList();
/*     */     
/*  36 */     Mob Mob_J = (Mob)Jockey;
/*     */     
/*  38 */     ServerLevel Lvl = MobInfo.GetMobServerLevel(Mob_J);
/*     */     
/*  40 */     BlockPos Above = Jockey.m_20183_().m_7494_();
/*  41 */     boolean isAboveOK = Lvl.m_8055_(Above).m_60795_();
/*     */     
/*  43 */     BlockPos Above_2 = Above.m_7494_();
/*  44 */     boolean isAboveOK_2 = Lvl.m_8055_(Above_2).m_60795_();
/*     */     
/*  46 */     if (isAboveOK && isAboveOK_2)
/*     */     {
/*  48 */       if (isJockeyMob(Jockey))
/*     */       {
/*  50 */         RideAMob(Jockey);
/*     */       }
/*     */     }
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
/*     */   void RefreshEntityList() {
/*  64 */     if (RideableList == null) {
/*  65 */       RideableList = NightmareMain.GetEntitiesFromSerialized(((String)ESMConfig.RideableMobs.get()).toString());
/*     */     }
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
/*     */   boolean isJockeyMob(Entity Jockey) {
/*  80 */     return ((String)ESMConfig.JockeyMobs.get()).contains(Jockey.m_7755_().getString().toLowerCase());
/*     */   }
/*     */ 
/*     */   
/*     */   void RideAMob(Entity Jockey) {
/*  85 */     Entity MobToRide = GetRandomRideableMobAndSpawn(Jockey);
/*  86 */     if (MobToRide != null)
/*     */     {
/*  88 */       Jockey.m_20329_(MobToRide);
/*     */     }
/*     */   }
/*     */ 
/*     */ 
/*     */   
/*     */   Entity GetRandomRideableMobAndSpawn(Entity Jockey) {
/*  95 */     Mob Entity_Mob = (Mob)Jockey;
/*  96 */     ServerLevel Lvl = MobInfo.GetMobServerLevel(Entity_Mob);
/*  97 */     if (Lvl != null && !Lvl.f_46443_) {
/*     */ 
/*     */       
/* 100 */       int MobNum = Clocker.GetIndexFromTime(RideableList.length);
/* 101 */       EntityType Entity_Type_To_Spawn = RideableList[MobNum];
/* 102 */       if (Entity_Type_To_Spawn != null) {
/*     */         
/* 104 */         Entity Entity_Class = Entity_Type_To_Spawn.m_20592_(Lvl, new ItemStack((ItemLike)Items.f_42329_), (Player)null, Jockey.m_20183_(), MobSpawnType.MOB_SUMMONED, true, false);
/* 105 */         return Entity_Class;
/*     */       } 
/*     */       
/* 108 */       return null;
/*     */     } 
/*     */     
/* 111 */     return null;
/*     */   }
/*     */ }


/* Location:              C:\Users\wapzi\Desktop\Plugins mC\mod\NightmareESM_1_20_4.jar!\ESM\MobBehavs\SpawnRideableMob.class
 * Java compiler version: 17 (61.0)
 * JD-Core Version:       1.1.3
 */