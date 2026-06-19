/*    */ package ESM;
/*    */ 
/*    */ import net.minecraft.core.BlockPos;
/*    */ import net.minecraft.world.entity.Entity;
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ public class SnapToBlockCenter
/*    */ {
/*    */   public SnapToBlockCenter(Entity Entity_Class) {
/* 16 */     BlockPos Block_Pos = Entity_Class.m_20183_();
/*    */ 
/*    */     
/* 19 */     int X = Block_Pos.m_123341_();
/* 20 */     int Y = Block_Pos.m_123342_();
/* 21 */     int Z = Block_Pos.m_123343_();
/*    */ 
/*    */     
/* 24 */     double X_D = X;
/* 25 */     double Z_D = Z;
/*    */ 
/*    */     
/* 28 */     Entity_Class.m_6034_(X_D, Y, Z_D);
/*    */   }
/*    */ }


/* Location:              C:\Users\wapzi\Desktop\Plugins mC\mod\NightmareESM_1_20_4.jar!\ESM\SnapToBlockCenter.class
 * Java compiler version: 17 (61.0)
 * JD-Core Version:       1.1.3
 */