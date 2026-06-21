/*    */ package net.aqualoco.sec.network;
/*    */ import net.aqualoco.sec.AquaSec;
/*    */ import net.aqualoco.sec.AquaSecClient;
/*    */ import net.aqualoco.sec.sleep.SleepAnimationState;
/*    */ import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
/*    */ import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
/*    */ import net.fabricmc.fabric.api.networking.v1.PacketSender;
/*    */ import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
/*    */ import net.minecraft.class_1937;
/*    */ import net.minecraft.class_2540;
/*    */ import net.minecraft.class_2960;
/*    */ import net.minecraft.class_310;
/*    */ import net.minecraft.class_3218;
/*    */ import net.minecraft.class_3222;
/*    */ import net.minecraft.class_634;
/*    */ import net.minecraft.class_638;
/*    */ 
/*    */ public final class SleepAnimationNetworking {
/* 19 */   public static final class_2960 SLEEP_START_ID = new class_2960("seamlesssleep", "sleep_animation_start");
/* 20 */   public static final class_2960 SLEEP_STOP_ID = new class_2960("seamlesssleep", "sleep_animation_stop");
/*    */ 
/*    */ 
/*    */   
/*    */   public static void initCommon() {}
/*    */ 
/*    */ 
/*    */   
/*    */   public static void initClient() {
/* 29 */     ClientPlayNetworking.registerGlobalReceiver(SLEEP_START_ID, (client, handler, buf, responseSender) -> {
/*    */           long startTime = buf.readLong();
/*    */ 
/*    */           
/*    */           long endTime = buf.readLong();
/*    */ 
/*    */           
/*    */           int duration = buf.readInt();
/*    */ 
/*    */           
/*    */           long startMillis = buf.readLong();
/*    */           
/*    */           class_2960 worldId = buf.method_10810();
/*    */           
/*    */           client.execute(());
/*    */         });
/*    */     
/* 46 */     ClientPlayNetworking.registerGlobalReceiver(SLEEP_STOP_ID, (client, handler, buf, responseSender) -> {
/*    */           class_2960 worldId = buf.method_10810();
/*    */ 
/*    */ 
/*    */ 
/*    */           
/*    */           client.execute(());
/*    */         });
/*    */ 
/*    */ 
/*    */     
/* 57 */     AquaSec.LOGGER.info("Registered client handlers for sleep animation.");
/*    */   }
/*    */   
/*    */   public static void sendStart(class_3218 world, SleepAnimationState state) {
/* 61 */     class_2540 buf = PacketByteBufs.create();
/* 62 */     buf.writeLong(state.getStartTimeOfDay());
/* 63 */     buf.writeLong(state.getEndTimeOfDay());
/* 64 */     buf.writeInt(state.getDurationTicks());
/* 65 */     buf.writeLong(state.getStartMillis());
/* 66 */     buf.method_10812(world.method_27983().method_29177());
/*    */     
/* 68 */     for (class_3222 player : world.method_18456()) {
/* 69 */       ServerPlayNetworking.send(player, SLEEP_START_ID, buf);
/*    */     }
/*    */     
/* 72 */     AquaSec.LOGGER.debug("Sent sleep animation payload (start) to {} players ({} -> {}, {} ticks)", new Object[] {
/* 73 */           Integer.valueOf(world.method_18456().size()), Long.valueOf(state.getStartTimeOfDay()), Long.valueOf(state.getEndTimeOfDay()), Integer.valueOf(state.getDurationTicks()) });
/*    */   }
/*    */   
/*    */   public static void sendStop(class_3218 world) {
/* 77 */     class_2540 buf = PacketByteBufs.create();
/* 78 */     buf.method_10812(world.method_27983().method_29177());
/* 79 */     for (class_3222 player : world.method_18456()) {
/* 80 */       ServerPlayNetworking.send(player, SLEEP_STOP_ID, buf);
/*    */     }
/* 82 */     AquaSec.LOGGER.debug("Sent sleep animation payload (stop) to {} players", Integer.valueOf(world.method_18456().size()));
/*    */   }
/*    */ }


/* Location:              C:\Users\wapzi\Desktop\Plugins mC\mod\seamless-sleep-2.4.1+1.20.1.jar!\net\aqualoco\sec\network\SleepAnimationNetworking.class
 * Java compiler version: 17 (61.0)
 * JD-Core Version:       1.1.3
 */