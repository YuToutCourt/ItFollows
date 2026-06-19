/*    */ package ESM;
/*    */ 
/*    */ import net.minecraft.core.BlockPos;
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ public class PositionInfo
/*    */ {
/*    */   public static BlockPos OffsetPos(BlockPos Original_Pos, double X, double Y, double Z) {
/* 17 */     BlockPos New_Pos = Original_Pos.m_7918_((int)X, (int)Y, (int)Z);
/* 18 */     return New_Pos;
/*    */   }
/*    */ }


/* Location:              C:\Users\wapzi\Desktop\Plugins mC\mod\NightmareESM_1_20_4.jar!\ESM\PositionInfo.class
 * Java compiler version: 17 (61.0)
 * JD-Core Version:       1.1.3
 */