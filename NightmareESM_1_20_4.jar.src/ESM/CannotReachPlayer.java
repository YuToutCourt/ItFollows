/*    */ package ESM;
/*    */ 
/*    */ import net.minecraft.world.entity.Entity;
/*    */ import net.minecraft.world.entity.ai.navigation.PathNavigation;
/*    */ import net.minecraft.world.entity.monster.Monster;
/*    */ import net.minecraft.world.level.pathfinder.Path;
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ public class CannotReachPlayer
/*    */ {
/*    */   boolean CannotReach(Entity Entity_Class) {
/* 15 */     PathNavigation Path_Nav = null;
/*    */     
/* 17 */     Monster M = (Monster)Entity_Class;
/* 18 */     Path_Nav = M.m_21573_();
/*    */     
/* 20 */     if (Path_Nav != null) {
/*    */ 
/*    */       
/* 23 */       Path Entity_Path = Path_Nav.m_26570_();
/*    */       
/* 25 */       if (Entity_Path != null)
/*    */       {
/* 27 */         return !Entity_Path.m_77403_();
/*    */       }
/* 29 */       return false;
/*    */     } 
/*    */ 
/*    */     
/* 33 */     return false;
/*    */   }
/*    */ }


/* Location:              C:\Users\wapzi\Desktop\Plugins mC\mod\NightmareESM_1_20_4.jar!\ESM\CannotReachPlayer.class
 * Java compiler version: 17 (61.0)
 * JD-Core Version:       1.1.3
 */