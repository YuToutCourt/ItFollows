/*    */ package net.aqualoco.sec.mixin.client;
/*    */ 
/*    */ import net.minecraft.class_2960;
/*    */ import net.minecraft.class_310;
/*    */ import net.minecraft.class_329;
/*    */ import net.minecraft.class_332;
/*    */ import net.minecraft.class_746;
/*    */ import org.spongepowered.asm.mixin.Mixin;
/*    */ import org.spongepowered.asm.mixin.Unique;
/*    */ import org.spongepowered.asm.mixin.injection.At;
/*    */ import org.spongepowered.asm.mixin.injection.ModifyArgs;
/*    */ import org.spongepowered.asm.mixin.injection.invoke.arg.Args;
/*    */ 
/*    */ 
/*    */ @Mixin({class_329.class})
/*    */ public abstract class SleepOverlayOpacityMixin
/*    */ {
/*    */   @Unique
/*    */   private static final float SLEEP_DARKNESS_FACTOR = 0.2F;
/*    */   @Unique
/* 21 */   private static final class_2960 VIGNETTE = new class_2960("textures/misc/vignette.png");
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */   
/*    */   @ModifyArgs(method = {"render"}, at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/hud/InGameHud;renderOverlay(Lnet/minecraft/client/gui/DrawContext;Lnet/minecraft/util/Identifier;F)V"))
/*    */   private void aquasec$adjustSleepOverlayOpacity(Args args) {
/* 31 */     class_332 context = (class_332)args.get(0);
/* 32 */     class_2960 texture = (class_2960)args.get(1);
/* 33 */     float opacity = ((Float)args.get(2)).floatValue();
/*    */     
/* 35 */     class_310 client = class_310.method_1551();
/* 36 */     class_746 player = client.field_1724;
/*    */     
/* 38 */     if (player != null && VIGNETTE.equals(texture)) {
/* 39 */       float scaled = opacity * 0.2F;
/* 40 */       if (scaled < 0.0F) {
/* 41 */         scaled = 0.0F;
/* 42 */       } else if (scaled > 1.0F) {
/* 43 */         scaled = 1.0F;
/*    */       } 
/* 45 */       args.set(2, Float.valueOf(scaled));
/*    */     } 
/*    */   }
/*    */ }


/* Location:              C:\Users\wapzi\Desktop\Plugins mC\mod\seamless-sleep-2.4.1+1.20.1.jar!\net\aqualoco\sec\mixin\client\SleepOverlayOpacityMixin.class
 * Java compiler version: 17 (61.0)
 * JD-Core Version:       1.1.3
 */