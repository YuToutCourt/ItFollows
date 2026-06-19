/*     */ package ESM.MobBehavs;
/*     */ 
/*     */ import ESM.ESMConfig;
/*     */ import ESM.GetBlockingBlock;
/*     */ import net.minecraft.core.BlockPos;
/*     */ import net.minecraft.server.level.ServerPlayer;
/*     */ import net.minecraft.world.entity.Entity;
/*     */ import net.minecraft.world.entity.LivingEntity;
/*     */ import net.minecraft.world.entity.monster.Creeper;
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ public class CreeperBreachWalls
/*     */ {
/*     */   public boolean Debug_ExplodeImmediately = false;
/*     */   
/*     */   public CreeperBreachWalls(Creeper C) {
/*  22 */     if (this.Debug_ExplodeImmediately) {
/*  23 */       new CreeperExplode(C);
/*     */     }
/*     */     else {
/*     */       
/*  27 */       LivingEntity livingEntity = C.m_5448_();
/*     */ 
/*     */       
/*  30 */       if (livingEntity != null) {
/*     */ 
/*     */         
/*  33 */         Creeper creeper = C;
/*     */         
/*  35 */         if (livingEntity instanceof ServerPlayer) {
/*     */           
/*  37 */           ServerPlayer Plyr = (ServerPlayer)livingEntity;
/*     */           
/*  39 */           if (!Plyr.m_7500_()) {
/*     */ 
/*     */ 
/*     */             
/*  43 */             float Dist_To_Tgt = C.m_20270_((Entity)livingEntity);
/*     */ 
/*     */             
/*  46 */             boolean isCloseEnough = (Dist_To_Tgt < ((Integer)ESMConfig.CreeperBreachingDistance.get()).intValue());
/*     */ 
/*     */             
/*  49 */             int Ticks = GetTicks(C);
/*     */ 
/*     */             
/*  52 */             if (isAtWall((Entity)creeper) || isWithinStrikingDistance(C, Plyr)) {
/*     */ 
/*     */               
/*  55 */               if (isCloseEnough) {
/*     */ 
/*     */                 
/*  58 */                 C.m_21391_((Entity)C, 1.0F, 1.0F);
/*     */ 
/*     */                 
/*  61 */                 IncTicks(C);
/*     */ 
/*     */                 
/*  64 */                 if (Ticks > ((Integer)ESMConfig.CreeperObstructedExplodeTicks.get()).intValue())
/*     */                 {
/*     */                   
/*  67 */                   new CreeperExplode(C);
/*     */                 }
/*     */               } else {
/*  70 */                 ResetTicks(C);
/*     */               } 
/*     */             } else {
/*  73 */               ResetTicks(C);
/*     */             } 
/*     */ 
/*     */             
/*  77 */             if (isHoveringAbove(C, Plyr))
/*     */             {
/*     */               
/*  80 */               new CreeperExplode(C);
/*     */             }
/*     */           }
/*     */           else {
/*     */             
/*  85 */             ResetTicks(C);
/*     */           } 
/*     */         } 
/*     */       } 
/*     */     } 
/*     */   }
/*     */ 
/*     */ 
/*     */   
/*     */   void IncTicks(Creeper C) {
/*  95 */     int CurrTicks = GetTicks(C);
/*  96 */     CurrTicks++;
/*  97 */     SetTicks(C, CurrTicks);
/*     */   }
/*     */ 
/*     */   
/*     */   void SetTicks(Creeper C, int Amt) {
/* 102 */     C.getPersistentData().m_128405_("nightmare_ticks", Amt);
/*     */   }
/*     */ 
/*     */   
/*     */   void ResetTicks(Creeper C) {
/* 107 */     SetTicks(C, 0);
/*     */   }
/*     */ 
/*     */   
/*     */   int GetTicks(Creeper C) {
/* 112 */     int RetVal = C.getPersistentData().m_128451_("nightmare_ticks");
/* 113 */     return RetVal;
/*     */   }
/*     */ 
/*     */   
/*     */   boolean isAtWall(Entity Entity_Class) {
/* 118 */     GetBlockingBlock Blocking_Block = new GetBlockingBlock(Entity_Class);
/* 119 */     return (Blocking_Block.isFootBlocked && Blocking_Block.isBlocking);
/*     */   }
/*     */ 
/*     */ 
/*     */   
/*     */   boolean isHoveringAbove(Creeper C, ServerPlayer Player_Target) {
/* 125 */     BlockPos Creeper_Pos = C.m_20183_();
/* 126 */     BlockPos Player_Pos = Player_Target.m_20183_();
/*     */ 
/*     */     
/* 129 */     float x_diff = Math.abs(Creeper_Pos.m_123341_() - Player_Pos.m_123341_());
/* 130 */     float z_diff = Math.abs(Creeper_Pos.m_123343_() - Player_Pos.m_123343_());
/*     */ 
/*     */     
/* 133 */     return (x_diff < ((Integer)ESMConfig.CreeperAboveExplodeDistance.get()).intValue() && z_diff < ((Integer)ESMConfig.CreeperAboveExplodeDistance.get()).intValue() && Player_Pos.m_123342_() < Creeper_Pos.m_123342_());
/*     */   }
/*     */ 
/*     */ 
/*     */ 
/*     */   
/*     */   boolean isWithinStrikingDistance(Creeper C, ServerPlayer Player_Target) {
/* 140 */     float Creeper_Dist = C.m_20270_((Entity)Player_Target);
/* 141 */     return (Creeper_Dist < ((Integer)ESMConfig.CreeperStrikingDistance.get()).intValue());
/*     */   }
/*     */ }


/* Location:              C:\Users\wapzi\Desktop\Plugins mC\mod\NightmareESM_1_20_4.jar!\ESM\MobBehavs\CreeperBreachWalls.class
 * Java compiler version: 17 (61.0)
 * JD-Core Version:       1.1.3
 */