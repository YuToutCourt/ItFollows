/*    */ package ESM.MobBehavs;
/*    */ 
/*    */ import ESM.MobInfo;
/*    */ import net.minecraft.core.BlockPos;
/*    */ import net.minecraft.world.entity.Entity;
/*    */ import net.minecraft.world.entity.LivingEntity;
/*    */ import net.minecraft.world.entity.Mob;
/*    */ import net.minecraft.world.level.Level;
/*    */ import net.minecraft.world.level.block.Block;
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ public class MobDigDown
/*    */ {
/*    */   public boolean isDoing;
/* 20 */   float DigDist = 16.0F;
/*    */ 
/*    */ 
/*    */   
/*    */   public MobDigDown(Mob M) {
/* 25 */     Mob mob = M;
/*    */ 
/*    */     
/* 28 */     if (!M.f_20911_) {
/*    */ 
/*    */       
/* 31 */       BlockPos Zombie_Pos = mob.m_20183_();
/*    */ 
/*    */       
/* 34 */       LivingEntity livingEntity = M.m_5448_();
/*    */ 
/*    */       
/* 37 */       if (livingEntity != null)
/*    */       {
/*    */ 
/*    */         
/* 41 */         if (livingEntity instanceof net.minecraft.server.level.ServerPlayer)
/*    */         {
/*    */           
/* 44 */           if (isCloseEnough(M, (Entity)livingEntity))
/*    */           {
/*    */ 
/*    */             
/* 48 */             if (isTargetUnderMonster(M, (Entity)livingEntity)) {
/*    */ 
/*    */ 
/*    */               
/* 52 */               BlockPos Under_BlockPos = Zombie_Pos.m_7495_();
/*    */               
/* 54 */               Level Lvl = MobInfo.GetMobLevel(M);
/*    */               
/* 56 */               Block Dmg_Block = Lvl.m_8055_(Under_BlockPos).m_60734_();
/*    */               
/* 58 */               new MobDamageBlock((Entity)mob, Dmg_Block, Under_BlockPos);
/*    */               
/* 60 */               this.isDoing = true;
/*    */             } 
/*    */           }
/*    */         }
/*    */       }
/*    */     } 
/*    */   }
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */   
/*    */   boolean isCloseEnough(Mob M, Entity Tgt) {
/* 73 */     double M_X = M.m_20183_().m_123341_();
/* 74 */     double M_Z = M.m_20183_().m_123343_();
/* 75 */     double Tgt_X = Tgt.m_20183_().m_123341_();
/* 76 */     double Tgt_Z = Tgt.m_20183_().m_123343_();
/*    */     
/* 78 */     return (Math.abs(M_X - Tgt_X) < this.DigDist && Math.abs(M_Z - Tgt_Z) < this.DigDist);
/*    */   }
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */   
/*    */   boolean isTargetUnderMonster(Mob M, Entity Tgt) {
/* 87 */     double M_Y = M.m_20183_().m_123342_();
/* 88 */     double TgtY = Tgt.m_20183_().m_123342_();
/*    */     
/* 90 */     return (TgtY < M_Y);
/*    */   }
/*    */ }


/* Location:              C:\Users\wapzi\Desktop\Plugins mC\mod\NightmareESM_1_20_4.jar!\ESM\MobBehavs\MobDigDown.class
 * Java compiler version: 17 (61.0)
 * JD-Core Version:       1.1.3
 */