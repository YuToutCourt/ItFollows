/*    */ package net.aqualoco.sec;
/*    */ 
/*    */ import net.aqualoco.sec.block.ModBlocks;
/*    */ import net.aqualoco.sec.network.SleepAnimationNetworking;
/*    */ import net.aqualoco.sec.sleep.SleepAnimationState;
/*    */ import net.fabricmc.api.ModInitializer;
/*    */ import org.slf4j.Logger;
/*    */ import org.slf4j.LoggerFactory;
/*    */ 
/*    */ public class AquaSec implements ModInitializer {
/*    */   public static final String MOD_ID = "seamlesssleep";
/* 12 */   public static final Logger LOGGER = LoggerFactory.getLogger("seamlesssleep");
/*    */   
/* 14 */   public static final SleepAnimationState OVERWORLD_SLEEP_ANIMATION = new SleepAnimationState();
/*    */ 
/*    */   
/*    */   public void onInitialize() {
/* 18 */     SleepAnimationNetworking.initCommon();
/* 19 */     ModBlocks.registerModBlocks();
/* 20 */     LOGGER.info("[Seamless Sleep] inicializado. Animacao de sono e bloco sleep_barrier registrados.");
/*    */   }
/*    */ }


/* Location:              C:\Users\wapzi\Desktop\Plugins mC\mod\seamless-sleep-2.4.1+1.20.1.jar!\net\aqualoco\sec\AquaSec.class
 * Java compiler version: 17 (61.0)
 * JD-Core Version:       1.1.3
 */