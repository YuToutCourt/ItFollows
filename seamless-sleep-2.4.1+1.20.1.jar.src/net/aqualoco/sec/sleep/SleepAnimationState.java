/*     */ package net.aqualoco.sec.sleep;
/*     */ 
/*     */ import net.minecraft.class_3218;
/*     */ 
/*     */ public final class SleepAnimationState
/*     */ {
/*     */   private static final long FULL_NIGHT_TICKS = 12000L;
/*     */   private static final int MIN_DURATION_TICKS = 40;
/*     */   private static final int MAX_DURATION_TICKS = 180;
/*     */   private boolean active;
/*     */   private long startTimeOfDay;
/*     */   private long endTimeOfDay;
/*     */   private int durationTicks;
/*     */   private long startMillis;
/*     */   
/*     */   public boolean isActive() {
/*  17 */     return this.active;
/*     */   }
/*     */   
/*     */   public void start(long currentTime, long targetTime) {
/*  21 */     if (targetTime <= currentTime) {
/*  22 */       this.active = false;
/*     */       
/*     */       return;
/*     */     } 
/*  26 */     long delta = targetTime - currentTime;
/*  27 */     this.durationTicks = computeDurationTicks(delta);
/*     */     
/*  29 */     this.active = true;
/*  30 */     this.startTimeOfDay = currentTime;
/*  31 */     this.endTimeOfDay = targetTime;
/*  32 */     this.startMillis = System.currentTimeMillis();
/*     */   }
/*     */   
/*     */   public void cancel() {
/*  36 */     this.active = false;
/*     */   }
/*     */   
/*     */   public void tick(class_3218 world) {
/*  40 */     if (!this.active) {
/*     */       return;
/*     */     }
/*     */     
/*  44 */     long now = System.currentTimeMillis();
/*  45 */     double elapsedMs = (now - this.startMillis);
/*  46 */     if (elapsedMs <= 0.0D) {
/*  47 */       world.method_29199(this.startTimeOfDay);
/*     */       
/*     */       return;
/*     */     } 
/*  51 */     double totalMs = this.durationTicks * 50.0D;
/*  52 */     double x = elapsedMs / totalMs;
/*  53 */     if (x >= 1.0D) {
/*  54 */       this.active = false;
/*  55 */       world.method_29199(this.endTimeOfDay);
/*     */       
/*     */       return;
/*     */     } 
/*     */     
/*  60 */     if (x < 0.0D) {
/*  61 */       x = 0.0D;
/*  62 */     } else if (x > 1.0D) {
/*  63 */       x = 1.0D;
/*     */     } 
/*     */     
/*  66 */     double eased = integralEase(x);
/*  67 */     long delta = this.endTimeOfDay - this.startTimeOfDay;
/*  68 */     long interpolated = this.startTimeOfDay + (long)(delta * eased);
/*  69 */     world.method_29199(interpolated);
/*     */   }
/*     */   
/*     */   public long getStartTimeOfDay() {
/*  73 */     return this.startTimeOfDay;
/*     */   }
/*     */   
/*     */   public long getEndTimeOfDay() {
/*  77 */     return this.endTimeOfDay;
/*     */   }
/*     */   
/*     */   public int getDurationTicks() {
/*  81 */     return this.durationTicks;
/*     */   }
/*     */   
/*     */   public long getStartMillis() {
/*  85 */     return this.startMillis;
/*     */   }
/*     */   
/*     */   private static int computeDurationTicks(long delta) {
/*  89 */     double fraction = delta / 12000.0D;
/*  90 */     if (fraction < 0.0D) {
/*  91 */       fraction = 0.0D;
/*  92 */     } else if (fraction > 1.0D) {
/*  93 */       fraction = 1.0D;
/*     */     } 
/*     */     
/*  96 */     int durationRange = 140;
/*  97 */     int duration = 40 + (int)Math.round(durationRange * fraction);
/*     */     
/*  99 */     if (duration < 40) {
/* 100 */       duration = 40;
/* 101 */     } else if (duration > 180) {
/* 102 */       duration = 180;
/*     */     } 
/*     */     
/* 105 */     return duration;
/*     */   }
/*     */ 
/*     */ 
/*     */   
/*     */   public static double integralEase(double x) {
/* 111 */     if (x <= 0.0D) {
/* 112 */       return 0.0D;
/*     */     }
/* 114 */     if (x >= 1.0D) {
/* 115 */       return 1.0D;
/*     */     }
/*     */     
/* 118 */     double x2 = x * x;
/* 119 */     double base = (x2 - 1.0D) * Math.sqrt(1.0D - x2) + 1.0D;
/* 120 */     double oneMinus = 1.0D - base;
/* 121 */     return 1.0D - Math.pow(oneMinus, 3.0D);
/*     */   }
/*     */ }


/* Location:              C:\Users\wapzi\Desktop\Plugins mC\mod\seamless-sleep-2.4.1+1.20.1.jar!\net\aqualoco\sec\sleep\SleepAnimationState.class
 * Java compiler version: 17 (61.0)
 * JD-Core Version:       1.1.3
 */