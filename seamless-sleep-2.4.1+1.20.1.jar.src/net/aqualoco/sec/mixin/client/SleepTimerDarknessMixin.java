/*    */ package net.aqualoco.sec.mixin.client;
/*    */ 
/*    */ import net.minecraft.class_1657;
/*    */ import org.spongepowered.asm.mixin.Mixin;
/*    */ import org.spongepowered.asm.mixin.Unique;
/*    */ import org.spongepowered.asm.mixin.injection.At;
/*    */ import org.spongepowered.asm.mixin.injection.Inject;
/*    */ import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
/*    */ 
/*    */ @Mixin({class_1657.class})
/*    */ public abstract class SleepTimerDarknessMixin
/*    */ {
/*    */   @Unique
/*    */   private static final float SLEEP_DARKNESS_FACTOR = 0.4F;
/*    */   
/*    */   @Inject(method = {"getSleepTimer"}, at = {@At("RETURN")}, cancellable = true)
/*    */   private void aquasec$scaleSleepTimer(CallbackInfoReturnable<Integer> cir) {
/* 18 */     int original = cir.getReturnValueI();
/* 19 */     if (original <= 0) {
/*    */       return;
/*    */     }
/*    */     
/* 23 */     int scaled = (int)(original * 0.4F);
/* 24 */     if (scaled < 1) {
/* 25 */       scaled = 1;
/*    */     }
/*    */     
/* 28 */     cir.setReturnValue(Integer.valueOf(scaled));
/*    */   }
/*    */ }


/* Location:              C:\Users\wapzi\Desktop\Plugins mC\mod\seamless-sleep-2.4.1+1.20.1.jar!\net\aqualoco\sec\mixin\client\SleepTimerDarknessMixin.class
 * Java compiler version: 17 (61.0)
 * JD-Core Version:       1.1.3
 */