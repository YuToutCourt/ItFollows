/*    */ package ESM;
/*    */ 
/*    */ import net.minecraft.world.entity.Entity;
/*    */ 
/*    */ 
/*    */ public class Clocker
/*    */ {
/*    */   public static int GetIndexFromTime(int IndexCount) {
/*  9 */     long Index_Long_Val = System.currentTimeMillis() % IndexCount;
/* 10 */     return (int)Index_Long_Val;
/*    */   }
/*    */ 
/*    */ 
/*    */ 
/*    */   
/*    */   public static boolean IsAtTimeInterval(int TimeIntervalMS) {
/* 17 */     return (System.currentTimeMillis() % TimeIntervalMS == 0L);
/*    */   }
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */   
/*    */   public static boolean IsAtTimeInterval(Entity Entity_Class, int TimeIntervalMS) {
/* 25 */     return (Entity_Class.f_19797_ % TimeIntervalMS == 0);
/*    */   }
/*    */ }


/* Location:              C:\Users\wapzi\Desktop\Plugins mC\mod\NightmareESM_1_20_4.jar!\ESM\Clocker.class
 * Java compiler version: 17 (61.0)
 * JD-Core Version:       1.1.3
 */