/*    */ package net.aqualoco.sec.client;
/*    */ 
/*    */ import net.aqualoco.sec.sleep.ClientSleepAnimationState;
/*    */ import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
/*    */ import net.fabricmc.loader.api.FabricLoader;
/*    */ import net.minecraft.class_1937;
/*    */ import net.minecraft.class_2561;
/*    */ import net.minecraft.class_310;
/*    */ import net.minecraft.class_332;
/*    */ import net.minecraft.class_5250;
/*    */ import net.minecraft.class_5348;
/*    */ 
/*    */ public final class SleepStatusOverlay {
/*    */   public static void register(ClientSleepAnimationState state) {
/* 15 */     HudRenderCallback.EVENT.register((drawContext, tickDelta) -> {
/*    */           int x;
/*    */           
/*    */           int y;
/*    */           
/*    */           class_310 client = class_310.method_1551();
/*    */           if (client.field_1724 == null || client.field_1687 == null) {
/*    */             return;
/*    */           }
/*    */           if (!client.field_1687.method_27983().equals(class_1937.field_25179)) {
/*    */             return;
/*    */           }
/*    */           if (!state.isActive()) {
/*    */             return;
/*    */           }
/*    */           class_5250 class_5250 = class_2561.method_43471("seamlesssleep.text.sleeping");
/* 31 */           boolean hasXaero = (FabricLoader.getInstance().isModLoaded("xaerominimap") || FabricLoader.getInstance().isModLoaded("xaerominimapfair"));
/*    */           long now = System.currentTimeMillis();
/*    */           double pulse = 0.6D + 0.4D * Math.sin(now / 400.0D);
/*    */           double clamped = Math.max(0.2D, Math.min(1.0D, pulse));
/*    */           int alpha = (int)(clamped * 255.0D);
/*    */           int color = alpha << 24 | 0xFFFFFF;
/*    */           if (hasXaero) {
/*    */             int sw = drawContext.method_51421();
/*    */             int sh = drawContext.method_51443();
/*    */             int textWidth = client.field_1772.method_27525((class_5348)class_5250);
/*    */             x = (sw - textWidth) / 2;
/*    */             y = sh - 68;
/*    */           } else {
/*    */             x = 6;
/*    */             y = 6;
/*    */           } 
/*    */           drawContext.method_27535(client.field_1772, (class_2561)class_5250, x, y, color);
/*    */         });
/*    */   }
/*    */ }


/* Location:              C:\Users\wapzi\Desktop\Plugins mC\mod\seamless-sleep-2.4.1+1.20.1.jar!\net\aqualoco\sec\client\SleepStatusOverlay.class
 * Java compiler version: 17 (61.0)
 * JD-Core Version:       1.1.3
 */