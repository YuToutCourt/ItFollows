/*    */ package net.aqualoco.sec.sleep;
/*    */ 
/*    */ import net.minecraft.class_638;
/*    */ 
/*    */ public final class ClientSleepAnimationState
/*    */ {
/*    */   private boolean active;
/*    */   private long startTimeOfDay;
/*    */   private long endTimeOfDay;
/*    */   private int durationTicks;
/*    */   private long startMillis;
/*    */   
/*    */   public boolean isActive() {
/* 14 */     return this.active;
/*    */   }
/*    */   
/*    */   public void reset() {
/* 18 */     this.active = false;
/*    */   }
/*    */   
/*    */   public void start(long startTimeOfDay, long endTimeOfDay, int durationTicks, long serverStartMillis) {
/* 22 */     long now = System.currentTimeMillis();
/*    */     
/* 24 */     long elapsedSinceServerStart = Math.max(0L, now - serverStartMillis);
/* 25 */     int adjustedDuration = (int)Math.max(1L, durationTicks - elapsedSinceServerStart / 50L);
/*    */     
/* 27 */     this.startTimeOfDay = startTimeOfDay;
/* 28 */     this.endTimeOfDay = endTimeOfDay;
/* 29 */     this.durationTicks = adjustedDuration;
/* 30 */     this.startMillis = now;
/* 31 */     this.active = true;
/*    */   }
/*    */   
/*    */   public void tick(class_638 world) {
/* 35 */     if (!this.active) {
/*    */       return;
/*    */     }
/*    */     
/* 39 */     long now = System.currentTimeMillis();
/* 40 */     long elapsedMs = now - this.startMillis;
/* 41 */     double totalMs = this.durationTicks * 50.0D;
/*    */     
/* 43 */     double x = (totalMs <= 0.0D) ? 1.0D : Math.min(1.0D, elapsedMs / totalMs);
/* 44 */     double eased = SleepAnimationState.integralEase(x);
/*    */     
/* 46 */     long delta = this.endTimeOfDay - this.startTimeOfDay;
/* 47 */     long newTimeOfDay = this.startTimeOfDay + (long)(delta * eased);
/*    */     
/* 49 */     world.method_28104().method_165(newTimeOfDay);
/*    */     
/* 51 */     if (x >= 1.0D)
/* 52 */       this.active = false; 
/*    */   }
/*    */ }


/* Location:              C:\Users\wapzi\Desktop\Plugins mC\mod\seamless-sleep-2.4.1+1.20.1.jar!\net\aqualoco\sec\sleep\ClientSleepAnimationState.class
 * Java compiler version: 17 (61.0)
 * JD-Core Version:       1.1.3
 */