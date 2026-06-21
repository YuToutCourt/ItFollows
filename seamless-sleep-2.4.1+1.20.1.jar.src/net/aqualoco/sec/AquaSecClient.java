/*    */ package net.aqualoco.sec;
/*    */ 
/*    */ import net.aqualoco.sec.client.SleepStatusOverlay;
/*    */ import net.aqualoco.sec.config.AquaSecClientConfigManager;
/*    */ import net.aqualoco.sec.network.SleepAnimationNetworking;
/*    */ import net.aqualoco.sec.sleep.ClientSleepAnimationState;
/*    */ import net.fabricmc.api.ClientModInitializer;
/*    */ import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
/*    */ import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
/*    */ import net.minecraft.class_1937;
/*    */ import net.minecraft.class_638;
/*    */ 
/*    */ public class AquaSecClient implements ClientModInitializer {
/* 14 */   public static final ClientSleepAnimationState CLIENT_SLEEP_ANIMATION = new ClientSleepAnimationState();
/*    */ 
/*    */   
/*    */   public void onInitializeClient() {
/* 18 */     AquaSecClientConfigManager.init();
/* 19 */     SleepAnimationNetworking.initClient();
/*    */ 
/*    */     
/* 22 */     WorldRenderEvents.START.register(context -> {
/*    */           class_638 world = context.world();
/*    */           
/*    */           if (world == null) {
/*    */             return;
/*    */           }
/*    */           
/*    */           if (!world.method_27983().equals(class_1937.field_25179)) {
/*    */             return;
/*    */           }
/*    */           if (CLIENT_SLEEP_ANIMATION.isActive()) {
/*    */             CLIENT_SLEEP_ANIMATION.tick(world);
/*    */           }
/*    */         });
/* 36 */     SleepStatusOverlay.register(CLIENT_SLEEP_ANIMATION);
/*    */   }
/*    */ }


/* Location:              C:\Users\wapzi\Desktop\Plugins mC\mod\seamless-sleep-2.4.1+1.20.1.jar!\net\aqualoco\sec\AquaSecClient.class
 * Java compiler version: 17 (61.0)
 * JD-Core Version:       1.1.3
 */