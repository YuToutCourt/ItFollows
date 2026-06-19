/*    */ package ESM.MobBehavs;
/*    */ 
/*    */ import ESM.MobInfo;
/*    */ import net.minecraft.core.BlockPos;
/*    */ import net.minecraft.world.entity.Entity;
/*    */ import net.minecraft.world.entity.LivingEntity;
/*    */ import net.minecraft.world.entity.Mob;
/*    */ import net.minecraft.world.level.Level;
/*    */ import net.minecraft.world.level.block.Block;
/*    */ import net.minecraft.world.level.block.Blocks;
/*    */ 
/*    */ 
/*    */ 
/*    */ public class MobBuildBridge
/*    */ {
/* 16 */   Block Block_Type = Blocks.f_50652_;
/*    */ 
/*    */ 
/*    */   
/*    */   public boolean isDoing;
/*    */ 
/*    */ 
/*    */ 
/*    */   
/*    */   public MobBuildBridge(Mob M) {
/* 26 */     LivingEntity livingEntity = M.m_5448_();
/*    */ 
/*    */     
/* 29 */     if (livingEntity != null)
/*    */     {
/*    */       
/* 32 */       if (livingEntity instanceof net.minecraft.server.level.ServerPlayer)
/*    */       {
/*    */         
/* 35 */         if (isMonsterAtTargetLvl(M, (Entity)livingEntity)) {
/*    */ 
/*    */           
/* 38 */           BlockPos M_Pos = M.m_20183_();
/*    */ 
/*    */           
/* 41 */           M.m_21391_((Entity)livingEntity, 0.0F, 0.0F);
/*    */ 
/*    */           
/* 44 */           BlockPos Fwd_Block_Pos = M_Pos.m_121945_(M.m_6350_());
/*    */ 
/*    */           
/* 47 */           Level Curr_Level = MobInfo.GetMobLevel(M);
/*    */ 
/*    */           
/* 50 */           Block Current_Block = Curr_Level.m_8055_(Fwd_Block_Pos).m_60734_();
/*    */ 
/*    */           
/* 53 */           if (Current_Block.m_49966_().m_60795_()) {
/*    */             
/* 55 */             BlockPos Block_Below_Pos = Fwd_Block_Pos.m_7495_();
/* 56 */             BlockPos Block_Below_Pos2 = Block_Below_Pos.m_7495_();
/*    */ 
/*    */             
/* 59 */             Block Block_Below = Curr_Level.m_8055_(Block_Below_Pos).m_60734_();
/* 60 */             Block Block_Below2 = Curr_Level.m_8055_(Block_Below_Pos2).m_60734_();
/*    */             
/* 62 */             if (Block_Below.m_49966_().m_60795_() && Block_Below2.m_49966_().m_60795_()) {
/*    */ 
/*    */               
/* 65 */               Mob mob = M;
/*    */ 
/*    */               
/* 68 */               new MobPlaceBlock((Entity)mob, this.Block_Type, Fwd_Block_Pos.m_7495_());
/*    */               
/* 70 */               M.m_6674_(M.m_7655_());
/*    */               
/* 72 */               this.isDoing = true;
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
/*    */   boolean isMonsterAtTargetLvl(Mob M, Entity Tgt) {
/* 84 */     double M_Y = M.m_20183_().m_123342_();
/* 85 */     double TgtY = Tgt.m_20183_().m_123342_();
/*    */     
/* 87 */     return (Math.abs(TgtY - M_Y) < 2.0D);
/*    */   }
/*    */ }


/* Location:              C:\Users\wapzi\Desktop\Plugins mC\mod\NightmareESM_1_20_4.jar!\ESM\MobBehavs\MobBuildBridge.class
 * Java compiler version: 17 (61.0)
 * JD-Core Version:       1.1.3
 */