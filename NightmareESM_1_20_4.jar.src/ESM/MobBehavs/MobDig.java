/*    */ package ESM.MobBehavs;
/*    */ 
/*    */ import ESM.GetBlockingBlock;
/*    */ import net.minecraft.server.level.ServerPlayer;
/*    */ import net.minecraft.world.entity.Entity;
/*    */ import net.minecraft.world.entity.LivingEntity;
/*    */ import net.minecraft.world.entity.monster.Monster;
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ public class MobDig
/*    */ {
/*    */   public boolean isDoing;
/* 15 */   float DigDist = 2.0F;
/*    */ 
/*    */ 
/*    */ 
/*    */   
/*    */   public MobDig(Entity Entity_Class) {
/* 21 */     Monster M = (Monster)Entity_Class;
/*    */ 
/*    */     
/* 24 */     if (!M.f_20911_) {
/*    */ 
/*    */       
/* 27 */       LivingEntity Tgt = M.m_5448_();
/* 28 */       if (Tgt != null)
/*    */       {
/*    */         
/* 31 */         if (Tgt instanceof ServerPlayer) {
/*    */ 
/*    */           
/* 34 */           ServerPlayer Server_Player = (ServerPlayer)Tgt;
/*    */ 
/*    */           
/* 37 */           if (!Server_Player.m_7500_()) {
/*    */ 
/*    */             
/* 40 */             GetBlockingBlock Blocking_Block = new GetBlockingBlock(Entity_Class);
/*    */ 
/*    */             
/* 43 */             if (!Blocking_Block.Current_Block.m_49966_().m_60795_() || 
/* 44 */               !Blocking_Block.Current_Block.m_49966_().m_60795_()) {
/*    */               
/* 46 */               M.m_6674_(M.m_7655_());
/*    */               
/* 48 */               new MobDamageBlock(Entity_Class, Blocking_Block.Current_Block, Blocking_Block.Current_Position);
/*    */ 
/*    */               
/* 51 */               new MobDamageBlock(Entity_Class, Blocking_Block.Feet_Block, Blocking_Block.Feet_Position);
/*    */               
/* 53 */               this.isDoing = true;
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
/*    */   boolean isAtLevel(Monster M, Entity Tgt) {
/* 67 */     double M_Y = M.m_20183_().m_123342_();
/* 68 */     double Tgt_Y = Tgt.m_20183_().m_123342_();
/*    */     
/* 70 */     double Dist = M_Y - Tgt_Y;
/* 71 */     Dist = Math.abs(Dist);
/*    */ 
/*    */     
/* 74 */     return (Dist < this.DigDist);
/*    */   }
/*    */ }


/* Location:              C:\Users\wapzi\Desktop\Plugins mC\mod\NightmareESM_1_20_4.jar!\ESM\MobBehavs\MobDig.class
 * Java compiler version: 17 (61.0)
 * JD-Core Version:       1.1.3
 */