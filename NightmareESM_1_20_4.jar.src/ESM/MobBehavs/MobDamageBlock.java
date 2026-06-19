/*    */ package ESM.MobBehavs;
/*    */ 
/*    */ import ESM.ESMConfig;
/*    */ import net.minecraft.core.BlockPos;
/*    */ import net.minecraft.world.entity.Entity;
/*    */ import net.minecraft.world.level.block.Block;
/*    */ import net.minecraft.world.level.block.Blocks;
/*    */ import org.apache.logging.log4j.LogManager;
/*    */ import org.apache.logging.log4j.Logger;
/*    */ 
/*    */ 
/*    */ 
/*    */ public class MobDamageBlock
/*    */ {
/* 15 */   public static final String Block_Blacklist = ((String)ESMConfig.BlocksEntitiesCannotDigThrough.get()).toLowerCase();
/*    */   
/* 17 */   private static final Logger LOGGER = LogManager.getLogger();
/*    */ 
/*    */ 
/*    */   
/*    */   public MobDamageBlock(Entity Entity_Class, Block Curr_Block, BlockPos Block_Pos) {
/* 22 */     if (Curr_Block != null) {
/*    */       
/* 24 */       String Block_Name = Curr_Block.m_49954_().getString();
/*    */ 
/*    */       
/* 27 */       boolean isExcluded = isExcludedBlock(Block_Name);
/*    */ 
/*    */       
/* 30 */       if (!isExcluded)
/*    */       {
/* 32 */         new MobPlaceBlock(Entity_Class, Blocks.f_50016_, Block_Pos);
/*    */       }
/*    */     } 
/*    */   }
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */   
/*    */   boolean isExcludedBlock(String ComparisonBlockName) {
/* 42 */     return Block_Blacklist.contains(ComparisonBlockName.toLowerCase());
/*    */   }
/*    */ }


/* Location:              C:\Users\wapzi\Desktop\Plugins mC\mod\NightmareESM_1_20_4.jar!\ESM\MobBehavs\MobDamageBlock.class
 * Java compiler version: 17 (61.0)
 * JD-Core Version:       1.1.3
 */