/*     */ package ESM.MobBehavs;
/*     */ 
/*     */ import ESM.ESMConfig;
/*     */ import ESM.MobInfo;
/*     */ import ESM.RNG;
/*     */ import net.minecraft.core.BlockPos;
/*     */ import net.minecraft.core.Direction;
/*     */ import net.minecraft.server.level.ServerPlayer;
/*     */ import net.minecraft.sounds.SoundEvents;
/*     */ import net.minecraft.sounds.SoundSource;
/*     */ import net.minecraft.world.entity.LivingEntity;
/*     */ import net.minecraft.world.entity.Mob;
/*     */ import net.minecraft.world.level.BlockGetter;
/*     */ import net.minecraft.world.level.Level;
/*     */ import net.minecraft.world.level.block.BaseFireBlock;
/*     */ import net.minecraft.world.level.block.Block;
/*     */ import net.minecraft.world.level.block.state.BlockState;
/*     */ import org.apache.logging.log4j.LogManager;
/*     */ import org.apache.logging.log4j.Logger;
/*     */ 
/*     */ 
/*     */ 
/*     */ public class MobStartFires
/*     */ {
/*  25 */   private static final Logger LOGGER = LogManager.getLogger();
/*     */   
/*  27 */   String[] Excluded_Tags = new String[] { "_sign", "_bed", "pressure_plate", "_button", "sugar_cane", "juke", "trapdoor", "enchanting", "brewing", "potted_", "skull", "_head", "chest", "dropper", "prismarine", "book", "grass", "snow", "flower", "sapling", "bush", "stem", "dragon", "carrots", "potatoes", "carpet", "banner", "structure", "compost", "piston", "log" };
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   
/*     */   public MobStartFires(Mob M) {
/*  35 */     double New_RNG = (new RNG()).GetDouble(0.0D, 100.0D);
/*     */ 
/*     */     
/*  38 */     double Chance_Burn_Wood = ((Double)ESMConfig.ChanceOfZombieLightFire.get()).doubleValue();
/*     */ 
/*     */     
/*  41 */     if (New_RNG < Chance_Burn_Wood) {
/*  42 */       StartFire(M);
/*     */     }
/*     */   }
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   
/*     */   void StartFire(Mob M) {
/*  51 */     BlockPos Block_Pos = M.m_20183_();
/*     */ 
/*     */     
/*  54 */     LivingEntity Living_Entity = M.m_5448_();
/*  55 */     if (Living_Entity != null && 
/*  56 */       Living_Entity instanceof ServerPlayer) {
/*     */ 
/*     */       
/*  59 */       ServerPlayer Server_Player = (ServerPlayer)Living_Entity;
/*  60 */       Level Lvl = MobInfo.GetMobLevel(M);
/*     */ 
/*     */       
/*  63 */       boolean isDone = false;
/*     */ 
/*     */       
/*  66 */       for (int x = -1; x <= 1; x++) {
/*  67 */         for (int y = -1; y <= 1; y++) {
/*  68 */           for (int z = -1; z <= 1; z++) {
/*     */             
/*  70 */             if (!isDone) {
/*     */               
/*  72 */               BlockPos Curr_Block_Pos = Block_Pos.m_7918_(x, y, z);
/*  73 */               BlockState CurrBlock_State = Lvl.m_8055_(Curr_Block_Pos);
/*  74 */               Block B = CurrBlock_State.m_60734_();
/*  75 */               boolean canBurn = CurrBlock_State.isFlammable((BlockGetter)Lvl, Curr_Block_Pos, Direction.NORTH);
/*  76 */               if (canBurn && isNotExcluded(B)) {
/*  77 */                 LightFire(Server_Player, Lvl, Curr_Block_Pos, CurrBlock_State);
/*  78 */                 isDone = true;
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
/*     */   boolean isNotExcluded(Block B) {
/*  91 */     for (int i = 0; i < this.Excluded_Tags.length; i++) {
/*  92 */       String Curr_Excl = this.Excluded_Tags[i];
/*  93 */       if (B.m_49954_().toString().contains(Curr_Excl)) {
/*  94 */         return false;
/*     */       }
/*     */     } 
/*     */     
/*  98 */     return true;
/*     */   }
/*     */ 
/*     */   
/*     */   void LightFire(ServerPlayer Curr_Tgt, Level Lvl, BlockPos Fire_Pos, BlockState Curr_Blockstate) {
/* 103 */     Lvl.m_7785_(Fire_Pos.m_123341_(), Fire_Pos.m_123342_(), Fire_Pos.m_123343_(), SoundEvents.f_11942_, SoundSource.BLOCKS, 1.0F, 1.0F, false);
/*     */     
/* 105 */     BlockState Block_State_Fire = BaseFireBlock.m_49245_((BlockGetter)Lvl, Fire_Pos);
/* 106 */     Lvl.m_7731_(Fire_Pos, Block_State_Fire, 11);
/*     */   }
/*     */ }


/* Location:              C:\Users\wapzi\Desktop\Plugins mC\mod\NightmareESM_1_20_4.jar!\ESM\MobBehavs\MobStartFires.class
 * Java compiler version: 17 (61.0)
 * JD-Core Version:       1.1.3
 */