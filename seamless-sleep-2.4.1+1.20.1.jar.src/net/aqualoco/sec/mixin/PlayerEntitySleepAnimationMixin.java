/*    */ package net.aqualoco.sec.mixin;
/*    */ 
/*    */ import net.minecraft.class_1657;
/*    */ import org.spongepowered.asm.mixin.Mixin;
/*    */ import org.spongepowered.asm.mixin.injection.At;
/*    */ import org.spongepowered.asm.mixin.injection.Redirect;
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ @Mixin({class_1657.class})
/*    */ public abstract class PlayerEntitySleepAnimationMixin
/*    */ {
/*    */   @Redirect(method = {"tick"}, at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/player/PlayerEntity;wakeUp(ZZ)V"))
/*    */   private void aquasec$forwardWakeUp(class_1657 self, boolean skipSleepTimer, boolean updateSleepingPlayers) {
/* 22 */     self.method_7358(skipSleepTimer, updateSleepingPlayers);
/*    */   }
/*    */ }


/* Location:              C:\Users\wapzi\Desktop\Plugins mC\mod\seamless-sleep-2.4.1+1.20.1.jar!\net\aqualoco\sec\mixin\PlayerEntitySleepAnimationMixin.class
 * Java compiler version: 17 (61.0)
 * JD-Core Version:       1.1.3
 */