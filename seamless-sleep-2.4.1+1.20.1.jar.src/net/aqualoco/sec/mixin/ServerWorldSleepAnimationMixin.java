/*     */ package net.aqualoco.sec.mixin;
/*     */ 
/*     */ import java.util.function.BooleanSupplier;
/*     */ import net.aqualoco.sec.AquaSec;
/*     */ import net.aqualoco.sec.network.SleepAnimationNetworking;
/*     */ import net.aqualoco.sec.sleep.SleepAnimationState;
/*     */ import net.minecraft.class_1928;
/*     */ import net.minecraft.class_1937;
/*     */ import net.minecraft.class_3218;
/*     */ import net.minecraft.class_3222;
/*     */ import org.spongepowered.asm.mixin.Mixin;
/*     */ import org.spongepowered.asm.mixin.Unique;
/*     */ import org.spongepowered.asm.mixin.gen.Invoker;
/*     */ import org.spongepowered.asm.mixin.injection.At;
/*     */ import org.spongepowered.asm.mixin.injection.Inject;
/*     */ import org.spongepowered.asm.mixin.injection.Redirect;
/*     */ import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ @Mixin({class_3218.class})
/*     */ public abstract class ServerWorldSleepAnimationMixin
/*     */ {
/*     */   @Unique
/*     */   private boolean aquasec$sleepAnimationWakePlayers;
/*     */   @Unique
/*     */   private boolean aquasec$sleepAnimationResetWeather;
/*     */   @Unique
/*     */   private int aquasec$sleepSubtitleTicks;
/*     */   
/*     */   @Invoker("wakeSleepingPlayers")
/*     */   abstract void aquasec$invokeWakeSleepingPlayers();
/*     */   
/*     */   @Invoker("resetWeather")
/*     */   abstract void aquasec$invokeResetWeather();
/*     */   
/*     */   @Redirect(method = {"tick"}, at = @At(value = "INVOKE", target = "Lnet/minecraft/server/world/ServerWorld;setTimeOfDay(J)V"))
/*     */   private void aquasec$redirectSetTimeOfDay(class_3218 world, long newTime) {
/*  47 */     if (!world.method_27983().equals(class_1937.field_25179)) {
/*  48 */       world.method_29199(newTime);
/*     */       
/*     */       return;
/*     */     } 
/*  52 */     SleepAnimationState state = AquaSec.OVERWORLD_SLEEP_ANIMATION;
/*  53 */     if (state.isActive()) {
/*     */       return;
/*     */     }
/*     */     
/*  57 */     long currentTime = world.method_8532();
/*  58 */     if (newTime <= currentTime) {
/*  59 */       world.method_29199(newTime);
/*     */       
/*     */       return;
/*     */     } 
/*  63 */     state.start(currentTime, newTime);
/*  64 */     this.aquasec$sleepAnimationWakePlayers = true;
/*  65 */     this
/*  66 */       .aquasec$sleepAnimationResetWeather = world.method_8450().method_8355(class_1928.field_19406);
/*  67 */     this.aquasec$sleepSubtitleTicks = 0;
/*     */     
/*  69 */     SleepAnimationNetworking.sendStart(world, state);
/*     */     
/*  71 */     AquaSec.LOGGER.debug("Iniciando animacao de sono: {} -> {}", Long.valueOf(currentTime), Long.valueOf(newTime));
/*     */   }
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   
/*     */   @Redirect(method = {"tick"}, at = @At(value = "INVOKE", target = "Lnet/minecraft/server/world/ServerWorld;wakeSleepingPlayers()V"))
/*     */   private void aquasec$redirectWakeSleepingPlayers(class_3218 world) {
/*  82 */     if (world.method_27983().equals(class_1937.field_25179) && this.aquasec$sleepAnimationWakePlayers) {
/*     */       return;
/*     */     }
/*     */ 
/*     */     
/*  87 */     aquasec$invokeWakeSleepingPlayers();
/*     */   }
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   
/*     */   @Redirect(method = {"tick"}, at = @At(value = "INVOKE", target = "Lnet/minecraft/server/world/ServerWorld;resetWeather()V"))
/*     */   private void aquasec$redirectResetWeather(class_3218 world) {
/*  98 */     if (world.method_27983().equals(class_1937.field_25179) && this.aquasec$sleepAnimationWakePlayers && this.aquasec$sleepAnimationResetWeather) {
/*     */       return;
/*     */     }
/*     */ 
/*     */ 
/*     */     
/* 104 */     aquasec$invokeResetWeather();
/*     */   }
/*     */   
/*     */   @Inject(method = {"tick"}, at = {@At("TAIL")})
/*     */   private void aquasec$tickSleepAnimation(BooleanSupplier shouldKeepTicking, CallbackInfo ci) {
/* 109 */     class_3218 self = (class_3218)this;
/*     */     
/* 111 */     if (!self.method_27983().equals(class_1937.field_25179)) {
/*     */       return;
/*     */     }
/*     */     
/* 115 */     SleepAnimationState state = AquaSec.OVERWORLD_SLEEP_ANIMATION;
/* 116 */     if (!state.isActive()) {
/*     */       return;
/*     */     }
/*     */     
/* 120 */     state.tick(self);
/*     */     
/* 122 */     if (!aquasec$hasEnoughSleeping(self)) {
/* 123 */       state.cancel();
/* 124 */       this.aquasec$sleepAnimationWakePlayers = false;
/* 125 */       this.aquasec$sleepAnimationResetWeather = false;
/* 126 */       SleepAnimationNetworking.sendStop(self);
/* 127 */       AquaSec.LOGGER.debug("Animacao de sono cancelada: jogadores dormindo insuficientes.");
/*     */       
/*     */       return;
/*     */     } 
/* 131 */     if (!state.isActive() && this.aquasec$sleepAnimationWakePlayers) {
/* 132 */       aquasec$invokeWakeSleepingPlayers();
/* 133 */       if (this.aquasec$sleepAnimationResetWeather) {
/* 134 */         aquasec$invokeResetWeather();
/*     */       }
/* 136 */       this.aquasec$sleepAnimationWakePlayers = false;
/* 137 */       this.aquasec$sleepAnimationResetWeather = false;
/*     */       
/* 139 */       AquaSec.LOGGER.debug("Animacao de sono concluida, jogadores acordados.");
/*     */     } 
/*     */   }
/*     */   
/*     */   @Unique
/*     */   private boolean aquasec$hasEnoughSleeping(class_3218 world) {
/* 145 */     int percentage = world.method_8450().method_8356(class_1928.field_28357);
/* 146 */     if (percentage <= 0) {
/* 147 */       return false;
/*     */     }
/*     */     
/* 150 */     int total = 0;
/* 151 */     int sleeping = 0;
/* 152 */     for (class_3222 player : world.method_18456()) {
/* 153 */       if (player.method_7325()) {
/*     */         continue;
/*     */       }
/* 156 */       total++;
/* 157 */       if (player.method_6113()) {
/* 158 */         sleeping++;
/*     */       }
/*     */     } 
/*     */     
/* 162 */     if (total == 0) {
/* 163 */       return false;
/*     */     }
/*     */     
/* 166 */     int required = Math.max(1, total * percentage / 100);
/* 167 */     return (sleeping >= required);
/*     */   }
/*     */ }


/* Location:              C:\Users\wapzi\Desktop\Plugins mC\mod\seamless-sleep-2.4.1+1.20.1.jar!\net\aqualoco\sec\mixin\ServerWorldSleepAnimationMixin.class
 * Java compiler version: 17 (61.0)
 * JD-Core Version:       1.1.3
 */