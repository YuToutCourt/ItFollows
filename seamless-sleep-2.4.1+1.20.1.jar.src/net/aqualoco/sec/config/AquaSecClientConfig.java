/*    */ package net.aqualoco.sec.config;
/*    */ 
/*    */ public final class AquaSecClientConfig {
/*  4 */   public double sleepChatTextOpacityMultiplier = 0.5D;
/*  5 */   public double sleepChatBackgroundOpacityMultiplier = 0.4D;
/*    */   
/*    */   public void clamp() {
/*  8 */     this.sleepChatTextOpacityMultiplier = clamp01(this.sleepChatTextOpacityMultiplier, 0.5D);
/*  9 */     this.sleepChatBackgroundOpacityMultiplier = clamp01(this.sleepChatBackgroundOpacityMultiplier, 0.4D);
/*    */   }
/*    */   
/*    */   private static double clamp01(double value, double fallback) {
/* 13 */     if (Double.isNaN(value) || Double.isInfinite(value)) {
/* 14 */       return fallback;
/*    */     }
/* 16 */     if (value < 0.0D) {
/* 17 */       return 0.0D;
/*    */     }
/* 19 */     if (value > 1.0D) {
/* 20 */       return 1.0D;
/*    */     }
/* 22 */     return value;
/*    */   }
/*    */ }


/* Location:              C:\Users\wapzi\Desktop\Plugins mC\mod\seamless-sleep-2.4.1+1.20.1.jar!\net\aqualoco\sec\config\AquaSecClientConfig.class
 * Java compiler version: 17 (61.0)
 * JD-Core Version:       1.1.3
 */