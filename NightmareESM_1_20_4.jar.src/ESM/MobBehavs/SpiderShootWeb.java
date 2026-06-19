/*    */ package ESM.MobBehavs;
/*    */ 
/*    */ import ESM.ESMConfig;
/*    */ import ESM.MobInfo;
/*    */ import ESM.RNG;
/*    */ import net.minecraft.server.level.ServerPlayer;
/*    */ import net.minecraft.world.entity.Entity;
/*    */ import net.minecraft.world.entity.LivingEntity;
/*    */ import net.minecraft.world.entity.Mob;
/*    */ import net.minecraft.world.level.Level;
/*    */ import net.minecraft.world.level.block.Blocks;
/*    */ import net.minecraft.world.level.block.state.BlockState;
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ public class SpiderShootWeb
/*    */ {
/*    */   public SpiderShootWeb(Mob M) {
/* 22 */     LivingEntity Tgt = M.m_5448_();
/*    */ 
/*    */     
/* 25 */     if (Tgt != null)
/*    */     {
/*    */       
/* 28 */       if (Tgt instanceof ServerPlayer) {
/*    */         
/* 30 */         ServerPlayer Targeted_Player = (ServerPlayer)Tgt;
/*    */ 
/*    */         
/* 33 */         if (SpiderIsInShootDistance(M, Targeted_Player)) {
/*    */           
/* 35 */           double New_RNG = (new RNG()).GetDouble(0.0D, 100.0D);
/* 36 */           if (New_RNG < ((Double)ESMConfig.SpiderShootWebChance.get()).doubleValue()) {
/* 37 */             ShootWebAtPlayer(M, Targeted_Player);
/*    */           }
/*    */         } 
/*    */       } 
/*    */     }
/*    */   }
/*    */ 
/*    */ 
/*    */   
/*    */   boolean SpiderIsInShootDistance(Mob Spider_Entity, ServerPlayer Targeted_Player) {
/* 47 */     float Dist = Spider_Entity.m_20270_((Entity)Targeted_Player);
/* 48 */     return (Dist < ((Integer)ESMConfig.SpiderShootWebDist.get()).intValue());
/*    */   }
/*    */ 
/*    */ 
/*    */   
/*    */   void ShootWebAtPlayer(Mob Spider_Entity, ServerPlayer Targeted_Player) {
/* 54 */     Level Lvl = MobInfo.GetMobLevel(Spider_Entity);
/*    */ 
/*    */     
/* 57 */     BlockState Player_Block_State = Lvl.m_8055_(Targeted_Player.m_20183_());
/*    */ 
/*    */     
/* 60 */     if (Player_Block_State.m_60795_())
/*    */     {
/* 62 */       Lvl.m_7731_(Targeted_Player.m_20183_(), Blocks.f_50033_.m_49966_(), 1);
/*    */     }
/*    */   }
/*    */ }


/* Location:              C:\Users\wapzi\Desktop\Plugins mC\mod\NightmareESM_1_20_4.jar!\ESM\MobBehavs\SpiderShootWeb.class
 * Java compiler version: 17 (61.0)
 * JD-Core Version:       1.1.3
 */