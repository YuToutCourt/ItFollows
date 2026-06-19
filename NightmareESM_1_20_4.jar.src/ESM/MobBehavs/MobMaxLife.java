/*    */ package ESM.MobBehavs;
/*    */ 
/*    */ import ESM.ESMConfig;
/*    */ import net.minecraft.world.entity.Entity;
/*    */ import org.apache.logging.log4j.LogManager;
/*    */ import org.apache.logging.log4j.Logger;
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ public class MobMaxLife
/*    */ {
/* 13 */   private static final Logger LOGGER = LogManager.getLogger();
/*    */ 
/*    */ 
/*    */   
/*    */   public MobMaxLife(Entity Entity_Class) {
/* 18 */     if (!Entity_Class.m_8077_())
/*    */     {
/*    */       
/* 21 */       if (Entity_Class.f_19797_ > ((Integer)ESMConfig.MobMaxLife.get()).intValue()) {
/*    */         
/* 23 */         KillMobRiding(Entity_Class);
/* 24 */         Entity_Class.m_146870_();
/*    */       } 
/*    */     }
/*    */   }
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */   
/*    */   void KillMobRiding(Entity Entity_Class) {
/* 34 */     Entity Veh = Entity_Class.m_20202_();
/* 35 */     if (Veh instanceof net.minecraft.world.entity.LivingEntity)
/*    */     {
/* 37 */       Veh.m_146870_();
/*    */     }
/*    */   }
/*    */ }


/* Location:              C:\Users\wapzi\Desktop\Plugins mC\mod\NightmareESM_1_20_4.jar!\ESM\MobBehavs\MobMaxLife.class
 * Java compiler version: 17 (61.0)
 * JD-Core Version:       1.1.3
 */