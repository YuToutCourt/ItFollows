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
/*    */ public class MobDigUp
/*    */ {
/* 15 */   float DigDist = 16.0F;
/*    */ 
/*    */ 
/*    */   
/*    */   public boolean isDoing;
/*    */ 
/*    */ 
/*    */   
/*    */   public MobDigUp(Mob M) {
/* 24 */     Mob mob = M;
/*    */ 
/*    */     
/* 27 */     if (!M.f_20911_) {
/*    */ 
/*    */       
/* 30 */       BlockPos Zombie_Pos = mob.m_20183_();
/*    */ 
/*    */       
/* 33 */       LivingEntity livingEntity = M.m_5448_();
/*    */ 
/*    */       
/* 36 */       if (livingEntity != null)
/*    */       {
/*    */         
/* 39 */         if (livingEntity instanceof net.minecraft.server.level.ServerPlayer)
/*    */         {
/*    */           
/* 42 */           if (isCloseEnough(M, (Entity)livingEntity))
/*    */           {
/*    */             
/* 45 */             if (isTargetAboveMonster(M, (Entity)livingEntity)) {
/*    */ 
/*    */               
/* 48 */               BlockPos OverBlockPosHead = Zombie_Pos.m_7494_();
/* 49 */               Level Lvl = MobInfo.GetMobLevel(M);
/* 50 */               Block Dmg_Block_Head = Lvl.m_8055_(OverBlockPosHead).m_60734_();
/* 51 */               new MobDamageBlock((Entity)mob, Dmg_Block_Head, OverBlockPosHead);
/*    */               
/* 53 */               BlockPos OverBlockPosHead_2 = Zombie_Pos.m_6630_(2);
/* 54 */               Block Dmg_Block = Lvl.m_8055_(OverBlockPosHead_2).m_60734_();
/* 55 */               new MobDamageBlock((Entity)mob, Dmg_Block, OverBlockPosHead_2);
/* 56 */               M.m_6674_(M.m_7655_());
/* 57 */               this.isDoing = true;
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
/*    */   
/*    */   boolean isCloseEnough(Mob M, Entity Tgt) {
/* 71 */     double M_X = M.m_20183_().m_123341_();
/* 72 */     double M_Z = M.m_20183_().m_123343_();
/* 73 */     double Tgt_X = Tgt.m_20183_().m_123341_();
/* 74 */     double Tgt_Z = Tgt.m_20183_().m_123343_();
/*    */     
/* 76 */     return (Math.abs(M_X - Tgt_X) < this.DigDist && Math.abs(M_Z - Tgt_Z) < this.DigDist);
/*    */   }
/*    */ 
/*    */ 
/*    */ 
/*    */   
/*    */   boolean isTargetAboveMonster(Mob M, Entity Tgt) {
/* 83 */     double M_Y = M.m_20183_().m_123342_();
/* 84 */     double TgtY = Tgt.m_20183_().m_123342_();
/*    */     
/* 86 */     return (TgtY > M_Y);
/*    */   }
/*    */ }


/* Location:              C:\Users\wapzi\Desktop\Plugins mC\mod\NightmareESM_1_20_4.jar!\ESM\MobBehavs\MobDigUp.class
 * Java compiler version: 17 (61.0)
 * JD-Core Version:       1.1.3
 */