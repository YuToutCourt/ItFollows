/*     */ package ESM.MobBehavs;
/*     */ 
/*     */ import ESM.MobInfo;
/*     */ import ESM.PositionInfo;
/*     */ import ESM.SnapToBlockCenter;
/*     */ import net.minecraft.core.BlockPos;
/*     */ import net.minecraft.server.level.ServerLevel;
/*     */ import net.minecraft.server.level.ServerPlayer;
/*     */ import net.minecraft.world.entity.Entity;
/*     */ import net.minecraft.world.entity.LivingEntity;
/*     */ import net.minecraft.world.entity.Mob;
/*     */ import net.minecraft.world.level.block.Block;
/*     */ import net.minecraft.world.level.block.Blocks;
/*     */ import net.minecraft.world.level.block.state.BlockState;
/*     */ import net.minecraft.world.phys.Vec3;
/*     */ import org.apache.logging.log4j.LogManager;
/*     */ import org.apache.logging.log4j.Logger;
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ public class MobBuildUp
/*     */ {
/*  24 */   private static final Logger LOGGER = LogManager.getLogger();
/*     */ 
/*     */   
/*  27 */   int BuildUpDist = 4;
/*     */ 
/*     */   
/*  30 */   Block Block_Type = Blocks.f_50652_;
/*     */ 
/*     */ 
/*     */   
/*     */   public boolean isDoing;
/*     */ 
/*     */ 
/*     */ 
/*     */   
/*     */   public MobBuildUp(Mob M) {
/*  40 */     LivingEntity livingEntity = M.m_5448_();
/*     */ 
/*     */     
/*  43 */     if (livingEntity != null)
/*     */     {
/*     */       
/*  46 */       if (livingEntity instanceof ServerPlayer) {
/*     */ 
/*     */ 
/*     */         
/*  50 */         ServerPlayer Server_Player = (ServerPlayer)livingEntity;
/*     */ 
/*     */         
/*  53 */         if (!Server_Player.m_7500_())
/*     */         {
/*     */ 
/*     */           
/*  57 */           if (isCloseEnough(M, (Entity)livingEntity)) {
/*     */ 
/*     */ 
/*     */ 
/*     */             
/*  62 */             Mob mob = M;
/*     */ 
/*     */             
/*  65 */             if (isBelowPlayer(M, (Entity)livingEntity) && isPlayerHighEnough(M, (Entity)livingEntity)) {
/*     */ 
/*     */ 
/*     */ 
/*     */               
/*  70 */               BlockPos M_BlockPos = M.m_20183_();
/*     */ 
/*     */               
/*  73 */               BlockPos M_BlockPos_Above = M_BlockPos.m_7494_();
/*     */ 
/*     */               
/*  76 */               BlockPos M_BlockPos_AboveHead = M_BlockPos_Above.m_7494_();
/*     */ 
/*     */               
/*  79 */               ServerLevel Server_Level = MobInfo.GetMobServerLevel(Server_Player);
/*     */ 
/*     */               
/*  82 */               BlockState Block_State = Server_Level.m_8055_(M_BlockPos_Above);
/*     */ 
/*     */               
/*  85 */               if (Block_State.m_60795_()) {
/*     */ 
/*     */                 
/*  88 */                 Vec3 Norm_Build_Dir = GetBuildDir(M, (Entity)livingEntity);
/*     */ 
/*     */ 
/*     */ 
/*     */                 
/*  93 */                 BlockPos New_Pos = PositionInfo.OffsetPos(M_BlockPos_Above, Norm_Build_Dir.f_82479_, Norm_Build_Dir.f_82480_, Norm_Build_Dir.f_82481_);
/*     */ 
/*     */                 
/*  96 */                 new MobPlaceBlock((Entity)mob, this.Block_Type, New_Pos);
/*     */ 
/*     */                 
/*  99 */                 M.m_6034_(M.m_20183_().m_123341_(), (M.m_20183_().m_123342_() + 2), M
/* 100 */                     .m_20183_().m_123343_());
/*     */ 
/*     */                 
/* 103 */                 new SnapToBlockCenter((Entity)mob);
/*     */ 
/*     */                 
/* 106 */                 M.m_6674_(M.m_7655_());
/* 107 */                 this.isDoing = true;
/*     */               } 
/*     */             } 
/*     */           } 
/*     */         }
/*     */       } 
/*     */     }
/*     */   }
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   
/*     */   boolean isCloseEnough(Mob M, Entity Tgt) {
/* 123 */     double M_X = M.m_20183_().m_123341_();
/* 124 */     double M_Z = M.m_20183_().m_123343_();
/* 125 */     double Tgt_X = Tgt.m_20183_().m_123341_();
/* 126 */     double Tgt_Z = Tgt.m_20183_().m_123343_();
/*     */     
/* 128 */     return (Math.abs(M_X - Tgt_X) < this.BuildUpDist && Math.abs(M_Z - Tgt_Z) < this.BuildUpDist);
/*     */   }
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   
/*     */   boolean isPlayerHighEnough(Mob M, Entity Tgt) {
/* 136 */     int Dist = Math.abs(M.m_20183_().m_123342_() - Tgt.m_20183_().m_123342_());
/* 137 */     return (Dist > 1);
/*     */   }
/*     */ 
/*     */ 
/*     */   
/*     */   boolean isBelowPlayer(Mob M, Entity Tgt) {
/* 143 */     BlockPos M_BlockPos = M.m_20183_();
/*     */ 
/*     */     
/* 146 */     BlockPos T_BlockPos = Tgt.m_20183_();
/*     */ 
/*     */     
/* 149 */     return (M_BlockPos.m_123342_() < T_BlockPos.m_123342_());
/*     */   }
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   
/*     */   Vec3 GetBuildDir(Mob M, Entity Tgt) {
/* 158 */     BlockPos Tgt_Block_Pos = Tgt.m_20183_();
/*     */ 
/*     */     
/* 161 */     float Tgt_X = Tgt_Block_Pos.m_123341_();
/* 162 */     float Tgt_Y = Tgt_Block_Pos.m_123342_();
/* 163 */     float Tgt_Z = Tgt_Block_Pos.m_123343_();
/*     */ 
/*     */     
/* 166 */     BlockPos M_Block_Pos = M.m_20183_();
/*     */ 
/*     */     
/* 169 */     float Z_X = M_Block_Pos.m_123341_();
/* 170 */     float Z_Y = M_Block_Pos.m_123342_();
/* 171 */     float Z_Z = M_Block_Pos.m_123343_();
/*     */ 
/*     */     
/* 174 */     float Diff_X = Tgt_X - Z_X;
/* 175 */     float Diff_Y = Tgt_Y - Z_Y;
/* 176 */     float Diff_Z = Tgt_Z - Z_Z;
/* 177 */     Vec3 BlockDir = new Vec3(Diff_X, Diff_Y, Diff_Z);
/* 178 */     BlockDir = BlockDir.m_82541_();
/*     */     
/* 180 */     return BlockDir;
/*     */   }
/*     */ }


/* Location:              C:\Users\wapzi\Desktop\Plugins mC\mod\NightmareESM_1_20_4.jar!\ESM\MobBehavs\MobBuildUp.class
 * Java compiler version: 17 (61.0)
 * JD-Core Version:       1.1.3
 */