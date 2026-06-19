/*    */ package ESM;
/*    */ 
/*    */ import net.minecraft.server.level.ServerLevel;
/*    */ import net.minecraft.server.level.ServerPlayer;
/*    */ import net.minecraft.world.entity.Mob;
/*    */ import net.minecraft.world.level.Level;
/*    */ 
/*    */ 
/*    */ 
/*    */ public class MobInfo
/*    */ {
/*    */   public static ServerLevel GetMobServerLevel(Mob M) {
/* 13 */     return (ServerLevel)M.m_9236_();
/*    */   }
/*    */ 
/*    */   
/*    */   public static Level GetMobLevel(Mob M) {
/* 18 */     return M.m_9236_();
/*    */   }
/*    */ 
/*    */   
/*    */   public static ServerLevel GetMobServerLevel(ServerPlayer S) {
/* 23 */     return (ServerLevel)S.m_9236_();
/*    */   }
/*    */ 
/*    */   
/*    */   public static Level GetMobLevel(ServerPlayer S) {
/* 28 */     return S.m_9236_();
/*    */   }
/*    */ }


/* Location:              C:\Users\wapzi\Desktop\Plugins mC\mod\NightmareESM_1_20_4.jar!\ESM\MobInfo.class
 * Java compiler version: 17 (61.0)
 * JD-Core Version:       1.1.3
 */