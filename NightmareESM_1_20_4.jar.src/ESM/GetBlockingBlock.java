/*    */ package ESM;
/*    */ 
/*    */ import net.minecraft.core.BlockPos;
/*    */ import net.minecraft.core.Vec3i;
/*    */ import net.minecraft.world.entity.Entity;
/*    */ import net.minecraft.world.entity.Mob;
/*    */ import net.minecraft.world.level.Level;
/*    */ import net.minecraft.world.level.block.Block;
/*    */ import net.minecraft.world.phys.Vec3;
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
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ public class GetBlockingBlock
/*    */ {
/*    */   public BlockPos Current_Position;
/*    */   public Block Current_Block;
/*    */   public boolean isBlocking;
/*    */   public BlockPos Feet_Position;
/*    */   public Block Feet_Block;
/*    */   public boolean isFootBlocked;
/*    */   
/*    */   public GetBlockingBlock(Entity Entity_Class) {
/* 37 */     Vec3 Entity_Eye_Pos = Entity_Class.m_146892_();
/*    */ 
/*    */     
/* 40 */     Vec3 Fwd = Entity_Class.m_20156_().m_82541_();
/*    */ 
/*    */     
/* 43 */     Vec3 Fwd_Block_Pos = Entity_Eye_Pos.m_82549_(Fwd);
/* 44 */     Vec3i Fwd_Block_PosI = new Vec3i((int)Fwd_Block_Pos.f_82479_, (int)Fwd_Block_Pos.f_82480_, (int)Fwd_Block_Pos.f_82481_);
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */     
/* 54 */     BlockPos Fwd_Block = new BlockPos(Fwd_Block_PosI);
/* 55 */     this.Current_Position = Fwd_Block;
/*    */ 
/*    */     
/* 58 */     Level Curr_Level = MobInfo.GetMobLevel((Mob)Entity_Class);
/*    */ 
/*    */     
/* 61 */     this.Current_Block = Curr_Level.m_8055_(Fwd_Block).m_60734_();
/*    */ 
/*    */ 
/*    */     
/* 65 */     this.isBlocking = (!BlockInfo.isAir(this.Current_Block) && !BlockInfo.isLiquid(this.Current_Block));
/*    */ 
/*    */ 
/*    */     
/* 69 */     this.Feet_Position = Fwd_Block.m_7495_();
/* 70 */     this.Current_Block = Curr_Level.m_8055_(this.Feet_Position).m_60734_();
/* 71 */     this.isFootBlocked = (!BlockInfo.isAir(this.Current_Block) && !BlockInfo.isLiquid(this.Current_Block));
/*    */   }
/*    */ }


/* Location:              C:\Users\wapzi\Desktop\Plugins mC\mod\NightmareESM_1_20_4.jar!\ESM\GetBlockingBlock.class
 * Java compiler version: 17 (61.0)
 * JD-Core Version:       1.1.3
 */