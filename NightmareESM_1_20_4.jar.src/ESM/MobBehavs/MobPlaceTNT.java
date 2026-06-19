/*    */ package ESM.MobBehavs;
/*    */ 
/*    */ import ESM.ESMConfig;
/*    */ import ESM.GetNearestTarget;
/*    */ import ESM.MobInfo;
/*    */ import ESM.RNG;
/*    */ import net.minecraft.core.BlockPos;
/*    */ import net.minecraft.server.level.ServerLevel;
/*    */ import net.minecraft.world.entity.Entity;
/*    */ import net.minecraft.world.entity.EntityType;
/*    */ import net.minecraft.world.entity.Mob;
/*    */ import net.minecraft.world.entity.MobSpawnType;
/*    */ import net.minecraft.world.entity.player.Player;
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ public class MobPlaceTNT
/*    */ {
/*    */   public MobPlaceTNT(Entity Entity_Class) {
/* 24 */     Entity Nearest_Entity = (new GetNearestTarget()).Get(Entity_Class);
/*    */ 
/*    */     
/* 27 */     if (Nearest_Entity != null)
/*    */     {
/* 29 */       if (CanPlaceThisTick() && isCloseEnough(Nearest_Entity, Entity_Class)) {
/*    */         
/* 31 */         BlockPos Entity_Block_Pos = Entity_Class.m_20183_();
/*    */ 
/*    */         
/* 34 */         Mob Mob_Entity = (Mob)Entity_Class;
/* 35 */         ServerLevel Lvl = MobInfo.GetMobServerLevel(Mob_Entity);
/*    */         
/* 37 */         if (Lvl != null)
/*    */         {
/*    */           
/* 40 */           EntityType.f_20515_.m_20592_(Lvl, null, (Player)null, Entity_Block_Pos, MobSpawnType.MOB_SUMMONED, true, false);
/*    */         }
/*    */       } 
/*    */     }
/*    */   }
/*    */ 
/*    */ 
/*    */ 
/*    */   
/*    */   boolean isCloseEnough(Entity Target_Entity, Entity Entity_Class) {
/* 50 */     return (Target_Entity.m_20270_(Entity_Class) < ((Integer)ESMConfig.ZombieTNTDistance.get()).intValue());
/*    */   }
/*    */ 
/*    */   
/*    */   boolean CanPlaceThisTick() {
/* 55 */     double Rand_Num = (new RNG()).GetDouble(0.0D, 100.0D);
/* 56 */     return (Rand_Num < ((Double)ESMConfig.ZombieTNTChance.get()).doubleValue());
/*    */   }
/*    */ }


/* Location:              C:\Users\wapzi\Desktop\Plugins mC\mod\NightmareESM_1_20_4.jar!\ESM\MobBehavs\MobPlaceTNT.class
 * Java compiler version: 17 (61.0)
 * JD-Core Version:       1.1.3
 */