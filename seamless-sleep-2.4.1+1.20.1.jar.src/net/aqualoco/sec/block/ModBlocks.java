/*    */ package net.aqualoco.sec.block;
/*    */ 
/*    */ import net.aqualoco.sec.AquaSec;
/*    */ import net.minecraft.class_1747;
/*    */ import net.minecraft.class_1792;
/*    */ import net.minecraft.class_2248;
/*    */ import net.minecraft.class_2378;
/*    */ import net.minecraft.class_2960;
/*    */ import net.minecraft.class_4970;
/*    */ import net.minecraft.class_7923;
/*    */ 
/*    */ public final class ModBlocks
/*    */ {
/* 14 */   public static final class_2248 SLEEP_BARRIER = registerSleepBarrier("sleep_barrier");
/*    */ 
/*    */ 
/*    */   
/*    */   private static class_2248 registerSleepBarrier(String name) {
/* 19 */     class_2960 id = new class_2960("seamlesssleep", name);
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */     
/* 25 */     class_4970.class_2251 settings = class_4970.class_2251.method_9637().method_9629(-1.0F, 3600000.0F).method_42327().method_45477().method_22488();
/*    */     
/* 27 */     class_2248 block = new class_2248(settings);
/*    */     
/* 29 */     class_2378.method_10230((class_2378)class_7923.field_41175, id, block);
/* 30 */     class_2378.method_10230((class_2378)class_7923.field_41178, id, new class_1747(block, new class_1792.class_1793()));
/*    */     
/* 32 */     return block;
/*    */   }
/*    */   
/*    */   public static void registerModBlocks() {
/* 36 */     AquaSec.LOGGER.info("Registrando blocos do Seamless Sleep");
/*    */   }
/*    */ }


/* Location:              C:\Users\wapzi\Desktop\Plugins mC\mod\seamless-sleep-2.4.1+1.20.1.jar!\net\aqualoco\sec\block\ModBlocks.class
 * Java compiler version: 17 (61.0)
 * JD-Core Version:       1.1.3
 */