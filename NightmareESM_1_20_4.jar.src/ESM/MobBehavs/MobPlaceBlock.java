/*    */ package ESM.MobBehavs;
/*    */ 
/*    */ import ESM.MobInfo;
/*    */ import net.minecraft.core.BlockPos;
/*    */ import net.minecraft.sounds.SoundEvent;
/*    */ import net.minecraft.sounds.SoundSource;
/*    */ import net.minecraft.world.entity.Entity;
/*    */ import net.minecraft.world.entity.Mob;
/*    */ import net.minecraft.world.level.Level;
/*    */ import net.minecraft.world.level.block.Block;
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
/*    */ public class MobPlaceBlock
/*    */ {
/*    */   public MobPlaceBlock(Entity Entity_Class, Block Block_Type, BlockPos Position) {
/* 28 */     Mob Mob_Entity = (Mob)Entity_Class;
/* 29 */     Level Lvl = MobInfo.GetMobLevel(Mob_Entity);
/* 30 */     Lvl.m_46597_(Position, Block_Type.m_49966_());
/*    */     
/* 32 */     SoundEvent Snd = Block_Type.m_49962_(Block_Type.m_49966_()).m_56775_();
/*    */     
/* 34 */     Lvl.m_5594_(null, Position, Snd, SoundSource.BLOCKS, 0.6F, 0.5F);
/*    */   }
/*    */ }


/* Location:              C:\Users\wapzi\Desktop\Plugins mC\mod\NightmareESM_1_20_4.jar!\ESM\MobBehavs\MobPlaceBlock.class
 * Java compiler version: 17 (61.0)
 * JD-Core Version:       1.1.3
 */