/*    */ package net.aqualoco.sec.mixin.client;
/*    */ 
/*    */ import net.aqualoco.sec.config.AquaSecClientConfig;
/*    */ import net.aqualoco.sec.config.AquaSecClientConfigManager;
/*    */ import net.minecraft.class_310;
/*    */ import net.minecraft.class_338;
/*    */ import net.minecraft.class_7172;
/*    */ import org.spongepowered.asm.mixin.Final;
/*    */ import org.spongepowered.asm.mixin.Mixin;
/*    */ import org.spongepowered.asm.mixin.Shadow;
/*    */ import org.spongepowered.asm.mixin.injection.At;
/*    */ import org.spongepowered.asm.mixin.injection.Inject;
/*    */ import org.spongepowered.asm.mixin.injection.Redirect;
/*    */ import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
/*    */ 
/*    */ @Mixin({class_338.class})
/*    */ public abstract class ChatHudSleepMixin {
/*    */   @Shadow
/*    */   @Final
/*    */   private class_310 field_2062;
/*    */   
/*    */   @Inject(method = {"getHeight"}, at = {@At("HEAD")}, cancellable = true)
/*    */   private void aquasec$limitHeightWhenSleeping(CallbackInfoReturnable<Integer> cir) {
/* 24 */     if (!aquasec$isSleepingChat()) {
/*    */       return;
/*    */     }
/*    */     
/* 28 */     double chatHeightSetting = ((Double)this.field_2062.field_1690.method_41803().method_41753()).doubleValue();
/* 29 */     int vanillaHeight = class_338.method_1818(chatHeightSetting);
/*    */     
/* 31 */     double spacing = ((Double)this.field_2062.field_1690.method_42546().method_41753()).doubleValue();
/* 32 */     int lineHeight = (int)(9.0D * (1.0D + spacing));
/* 33 */     int maxLines = 4;
/* 34 */     int limitedHeight = Math.min(vanillaHeight, lineHeight * maxLines);
/*    */     
/* 36 */     cir.setReturnValue(Integer.valueOf(limitedHeight));
/*    */   }
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */   
/*    */   @Redirect(method = {"render"}, at = @At(value = "INVOKE", target = "Lnet/minecraft/client/option/SimpleOption;getValue()Ljava/lang/Object;", ordinal = 1))
/*    */   private Object aquasec$dimBackgroundWhileSleeping(class_7172<Double> option) {
/* 48 */     double value = ((Double)option.method_41753()).doubleValue();
/* 49 */     if (!aquasec$isSleepingChat()) {
/* 50 */       return Double.valueOf(value);
/*    */     }
/*    */     
/* 53 */     double factor = (aquasec$getConfig()).sleepChatBackgroundOpacityMultiplier;
/* 54 */     return Double.valueOf(value * factor);
/*    */   }
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */   
/*    */   @Redirect(method = {"render"}, at = @At(value = "INVOKE", target = "Lnet/minecraft/client/option/SimpleOption;getValue()Ljava/lang/Object;", ordinal = 0))
/*    */   private Object aquasec$dimTextWhileSleeping(class_7172<Double> option) {
/* 66 */     double value = ((Double)option.method_41753()).doubleValue();
/* 67 */     if (!aquasec$isSleepingChat()) {
/* 68 */       return Double.valueOf(value);
/*    */     }
/*    */     
/* 71 */     double factor = (aquasec$getConfig()).sleepChatTextOpacityMultiplier;
/* 72 */     return Double.valueOf(value * factor);
/*    */   }
/*    */   
/*    */   private boolean aquasec$isSleepingChat() {
/* 76 */     return (this.field_2062 != null && this.field_2062.field_1724 != null && this.field_2062.field_1724
/*    */       
/* 78 */       .method_6113() && this.field_2062.field_1755 instanceof net.minecraft.class_423);
/*    */   }
/*    */ 
/*    */   
/*    */   private AquaSecClientConfig aquasec$getConfig() {
/* 83 */     return AquaSecClientConfigManager.get();
/*    */   }
/*    */ }


/* Location:              C:\Users\wapzi\Desktop\Plugins mC\mod\seamless-sleep-2.4.1+1.20.1.jar!\net\aqualoco\sec\mixin\client\ChatHudSleepMixin.class
 * Java compiler version: 17 (61.0)
 * JD-Core Version:       1.1.3
 */