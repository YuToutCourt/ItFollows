/*     */ package ESM.MobBehavs;
/*     */ 
/*     */ import ESM.ESMConfig;
/*     */ import ESM.GetNearestTarget;
/*     */ import net.minecraft.world.entity.Entity;
/*     */ import net.minecraft.world.entity.LivingEntity;
/*     */ import net.minecraft.world.entity.Mob;
/*     */ import org.apache.logging.log4j.LogManager;
/*     */ import org.apache.logging.log4j.Logger;
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ public class MobTargetPlayer
/*     */ {
/*  21 */   private static final Logger LOGGER = LogManager.getLogger();
/*     */   
/*  23 */   public static final String[] Whitelist = GetWhitelist();
/*     */   
/*     */   static String[] GetWhitelist() {
/*  26 */     String[] Val_List = ((String)ESMConfig.SiegeEntityWhitelist.get()).toLowerCase().split(",");
/*  27 */     for (int i = 0; i < Val_List.length; i++) {
/*     */       
/*  29 */       String Curr_Val = Val_List[i];
/*  30 */       Curr_Val = Curr_Val.trim();
/*  31 */       Val_List[i] = Curr_Val;
/*     */     } 
/*     */     
/*  34 */     return Val_List;
/*     */   }
/*     */ 
/*     */ 
/*     */ 
/*     */   
/*     */   public MobTargetPlayer(Entity Attacker) {
/*  41 */     if (isAllowedMobType(Attacker)) {
/*     */ 
/*     */ 
/*     */ 
/*     */       
/*  46 */       Mob Mob_Class = (Mob)Attacker;
/*     */ 
/*     */       
/*  49 */       LivingEntity livingEntity = Mob_Class.m_5448_();
/*     */       
/*  51 */       boolean isMonsterInfighting = ((Boolean)ESMConfig.AllowMonsterInfighting.get()).booleanValue();
/*     */       
/*  53 */       if (isMonsterInfighting && livingEntity != null) {
/*     */         return;
/*     */       }
/*     */       
/*  57 */       Entity Targeted_Entity = (new GetNearestTarget()).Get(Attacker);
/*     */       
/*  59 */       if (Targeted_Entity != null)
/*     */       {
/*  61 */         if (WithinRange(Targeted_Entity, Attacker))
/*     */         {
/*     */           
/*  64 */           Mob_Class.m_6710_((LivingEntity)Targeted_Entity);
/*     */         }
/*     */       }
/*     */       
/*  68 */       new MobMaxLife(Attacker);
/*     */     } 
/*     */   }
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   
/*     */   boolean isAllowedMobType(Entity Entity_Class) {
/*  78 */     boolean WasFound = false;
/*  79 */     for (int i = 0; i < Whitelist.length; i++) {
/*     */       
/*  81 */       String Val = Whitelist[i];
/*  82 */       String ChkName = Entity_Class.m_7755_().getString().toLowerCase();
/*  83 */       if (Val.compareTo(ChkName) == 0)
/*     */       {
/*  85 */         WasFound = true;
/*     */       }
/*     */     } 
/*     */     
/*  89 */     return WasFound;
/*     */   }
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   
/*     */   boolean WithinRange(Entity Targeted_Entity, Entity Entity_Class) {
/* 100 */     float Target_Dist_AboveGround = ((Integer)ESMConfig.MonsterAttackDistanceAboveGround.get()).intValue();
/* 101 */     float Target_Dist_UnderGround = ((Integer)ESMConfig.MonsterAttackDistanceUnderground.get()).intValue();
/*     */     
/* 103 */     double X_To = Math.abs(Math.abs(Targeted_Entity.m_20185_()) - Math.abs(Entity_Class.m_20185_()));
/* 104 */     double Z_To = Math.abs(Math.abs(Targeted_Entity.m_20189_()) - Math.abs(Entity_Class.m_20189_()));
/*     */ 
/*     */     
/* 107 */     float Y_Pos = Entity_Class.m_20183_().m_123342_();
/*     */ 
/*     */     
/* 110 */     if (Y_Pos < 50.0F) {
/* 111 */       return (Targeted_Entity.m_20270_(Entity_Class) < Target_Dist_UnderGround);
/*     */     }
/* 113 */     return (X_To < Target_Dist_AboveGround && Z_To < Target_Dist_AboveGround);
/*     */   }
/*     */ }


/* Location:              C:\Users\wapzi\Desktop\Plugins mC\mod\NightmareESM_1_20_4.jar!\ESM\MobBehavs\MobTargetPlayer.class
 * Java compiler version: 17 (61.0)
 * JD-Core Version:       1.1.3
 */