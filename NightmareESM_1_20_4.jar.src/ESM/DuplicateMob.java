/*     */ package ESM;
/*     */ 
/*     */ import com.mojang.logging.LogUtils;
/*     */ import java.util.ArrayList;
/*     */ import net.minecraft.core.BlockPos;
/*     */ import net.minecraft.server.level.ServerLevel;
/*     */ import net.minecraft.world.entity.EntityType;
/*     */ import net.minecraft.world.entity.Mob;
/*     */ import net.minecraft.world.entity.MobSpawnType;
/*     */ import net.minecraft.world.item.ItemStack;
/*     */ import net.minecraft.world.item.Items;
/*     */ import net.minecraft.world.level.ItemLike;
/*     */ import org.slf4j.Logger;
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ public class DuplicateMob
/*     */ {
/*  22 */   private static final Logger LOGGER = LogUtils.getLogger();
/*     */ 
/*     */   
/*  25 */   int Swarm_Placement_Proximity = 2;
/*     */ 
/*     */ 
/*     */   
/*  29 */   ArrayList<BlockPos> Used_Points = new ArrayList<>();
/*     */ 
/*     */   
/*  32 */   EntityType[] Duplication_Whitelist = new EntityType[] { EntityType.f_20551_, EntityType.f_20554_, EntityType.f_20562_, EntityType.f_20566_, EntityType.f_20568_, EntityType.f_20453_, EntityType.f_20456_, EntityType.f_20458_, EntityType.f_20511_, EntityType.f_20512_, EntityType.f_20513_, EntityType.f_20523_, EntityType.f_20524_, EntityType.f_20479_, EntityType.f_20518_, EntityType.f_20491_, EntityType.f_20493_, EntityType.f_20495_, EntityType.f_20497_, EntityType.f_20500_, EntityType.f_20501_, EntityType.f_20530_, EntityType.f_20531_, EntityType.f_20481_ };
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   
/*     */   public DuplicateMob(Mob M, int Count) {
/*  46 */     if (isWhitelisted(M))
/*     */     {
/*  48 */       for (int i = 0; i < Count; i++)
/*     */       {
/*     */ 
/*     */         
/*  52 */         Clone(M);
/*     */       }
/*     */     }
/*     */   }
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   
/*     */   void Clone(Mob M) {
/*  62 */     ServerLevel Lvl = MobInfo.GetMobServerLevel(M);
/*  63 */     if (Lvl != null) {
/*     */ 
/*     */       
/*  66 */       BlockPos pos = M.m_20183_();
/*     */ 
/*     */       
/*  69 */       BlockPos Offset_Pos = GetOffsetPosition(pos);
/*     */       
/*  71 */       if (isPositionGood(Lvl, Offset_Pos)) {
/*  72 */         M.m_6095_().m_20592_(Lvl, new ItemStack((ItemLike)Items.f_41890_), null, Offset_Pos, MobSpawnType.MOB_SUMMONED, true, false);
/*     */       }
/*     */     } 
/*     */   }
/*     */ 
/*     */ 
/*     */   
/*     */   boolean isWhitelisted(Mob M) {
/*  80 */     for (int i = 0; i < this.Duplication_Whitelist.length; i++) {
/*  81 */       if (this.Duplication_Whitelist[i] == M.m_6095_())
/*     */       {
/*  83 */         return true;
/*     */       }
/*     */     } 
/*     */ 
/*     */     
/*  88 */     return false;
/*     */   }
/*     */ 
/*     */ 
/*     */   
/*     */   BlockPos GetOffsetPosition(BlockPos Original_Pos) {
/*  94 */     double Rand_X_Off = (new RNG()).GetInt(-this.Swarm_Placement_Proximity, this.Swarm_Placement_Proximity);
/*  95 */     double Rand_Z_Off = (new RNG()).GetInt(-this.Swarm_Placement_Proximity, this.Swarm_Placement_Proximity);
/*     */ 
/*     */     
/*  98 */     BlockPos New_Pos = PositionInfo.OffsetPos(Original_Pos, Rand_X_Off, 0.0D, Rand_Z_Off);
/*     */     
/* 100 */     return New_Pos;
/*     */   }
/*     */ 
/*     */ 
/*     */ 
/*     */   
/*     */   boolean isPositionGood(ServerLevel Lvl, BlockPos Position) {
/* 107 */     boolean isBottomOK = Lvl.m_8055_(Position).m_60795_();
/* 108 */     boolean isTopOK = Lvl.m_8055_(PositionInfo.OffsetPos(Position, 0.0D, 1.0D, 0.0D)).m_60795_();
/* 109 */     return (isBottomOK && isTopOK);
/*     */   }
/*     */ }


/* Location:              C:\Users\wapzi\Desktop\Plugins mC\mod\NightmareESM_1_20_4.jar!\ESM\DuplicateMob.class
 * Java compiler version: 17 (61.0)
 * JD-Core Version:       1.1.3
 */