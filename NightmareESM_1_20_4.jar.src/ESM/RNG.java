/*    */ package ESM;
/*    */ 
/*    */ import java.util.Random;
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ public class RNG
/*    */ {
/*    */   public int GetInt(int Min, int Max) {
/* 16 */     Random New_RNG = new Random();
/* 17 */     return New_RNG.nextInt(Min, Max);
/*    */   }
/*    */ 
/*    */ 
/*    */   
/*    */   public double GetDouble(double Min, double Max) {
/* 23 */     Random New_RNG = new Random();
/* 24 */     return New_RNG.nextDouble(Min, Max);
/*    */   }
/*    */ }


/* Location:              C:\Users\wapzi\Desktop\Plugins mC\mod\NightmareESM_1_20_4.jar!\ESM\RNG.class
 * Java compiler version: 17 (61.0)
 * JD-Core Version:       1.1.3
 */