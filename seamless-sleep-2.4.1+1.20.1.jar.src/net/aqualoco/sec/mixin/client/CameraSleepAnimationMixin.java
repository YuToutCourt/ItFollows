/*    */ package net.aqualoco.sec.mixin.client;
/*    */ 
/*    */ import net.minecraft.class_1297;
/*    */ import net.minecraft.class_1657;
/*    */ import net.minecraft.class_1922;
/*    */ import net.minecraft.class_4184;
/*    */ import org.spongepowered.asm.mixin.Mixin;
/*    */ import org.spongepowered.asm.mixin.Shadow;
/*    */ import org.spongepowered.asm.mixin.injection.At;
/*    */ import org.spongepowered.asm.mixin.injection.Inject;
/*    */ import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ @Mixin({class_4184.class})
/*    */ public abstract class CameraSleepAnimationMixin
/*    */ {
/*    */   @Shadow
/*    */   protected abstract void method_19325(float paramFloat1, float paramFloat2);
/*    */   
/*    */   @Inject(method = {"update"}, at = {@At("TAIL")})
/*    */   private void aquasec$tiltSleepCamera(class_1922 area, class_1297 focusedEntity, boolean thirdPerson, boolean inverseView, float tickDelta, CallbackInfo ci) {
/*    */     class_1657 player;
/* 27 */     if (focusedEntity instanceof class_1657) { player = (class_1657)focusedEntity; }
/*    */     else
/*    */     { return; }
/*    */     
/* 31 */     if (thirdPerson || !player.method_6113()) {
/*    */       return;
/*    */     }
/*    */     
/* 35 */     class_4184 self = (class_4184)this;
/* 36 */     float yaw = self.method_19330();
/* 37 */     float pitch = self.method_19329();
/*    */     
/* 39 */     float tilt = -10.0F;
/*    */     
/* 41 */     method_19325(yaw, pitch + tilt);
/*    */   }
/*    */ }


/* Location:              C:\Users\wapzi\Desktop\Plugins mC\mod\seamless-sleep-2.4.1+1.20.1.jar!\net\aqualoco\sec\mixin\client\CameraSleepAnimationMixin.class
 * Java compiler version: 17 (61.0)
 * JD-Core Version:       1.1.3
 */