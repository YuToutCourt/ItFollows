/*    */ package ESM;
/*    */ 
/*    */ import net.minecraft.core.BlockPos;
/*    */ import net.minecraft.server.level.ServerPlayer;
/*    */ import net.minecraft.world.entity.player.Player;
/*    */ 
/*    */ 
/*    */ 
/*    */ public class SetPlayerSpawn
/*    */ {
/*    */   public SetPlayerSpawn(Player Player_Entity, BlockPos Pos) {
/* 12 */     ServerPlayer Server_Player = (ServerPlayer)Player_Entity;
/* 13 */     Server_Player.m_9158_(Server_Player.m_8963_(), Pos, 0.0F, true, true);
/*    */   }
/*    */ }


/* Location:              C:\Users\wapzi\Desktop\Plugins mC\mod\NightmareESM_1_20_4.jar!\ESM\SetPlayerSpawn.class
 * Java compiler version: 17 (61.0)
 * JD-Core Version:       1.1.3
 */