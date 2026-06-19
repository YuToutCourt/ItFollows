/*     */ package ESM.Base;
/*     */ 
/*     */ import ESM.ESMConfig;
/*     */ import ESM.RNG;
/*     */ import net.minecraft.nbt.CompoundTag;
/*     */ import net.minecraft.world.effect.MobEffectInstance;
/*     */ import net.minecraft.world.effect.MobEffects;
/*     */ import net.minecraft.world.entity.Entity;
/*     */ import net.minecraft.world.entity.LivingEntity;
/*     */ import net.minecraft.world.entity.ai.attributes.AttributeModifier;
/*     */ import net.minecraft.world.entity.ai.attributes.Attributes;
/*     */ import net.minecraft.world.entity.monster.Creeper;
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
/*     */ public class NightmareCreeper
/*     */ {
/*     */   public NightmareCreeper(Entity Entity_Class, int ChargedChance) {
/*  27 */     int Creeper_RNG = (new RNG()).GetInt(0, 100);
/*     */ 
/*     */     
/*  30 */     if (Creeper_RNG < ChargedChance) {
/*  31 */       MakeCharged(Entity_Class);
/*     */     }
/*     */ 
/*     */     
/*  35 */     double Nuclear_Val = (new RNG()).GetDouble(0.0D, 100.0D);
/*     */     
/*  37 */     if (Nuclear_Val < ((Double)ESMConfig.ChanceOfNuclearCreeper.get()).doubleValue()) {
/*  38 */       MakeNuclear(Entity_Class);
/*     */     }
/*     */   }
/*     */ 
/*     */ 
/*     */   
/*     */   void MakeCharged(Entity Entity_Class) {
/*  45 */     Creeper C = (Creeper)Entity_Class;
/*  46 */     CompoundTag nbt = C.serializeNBT();
/*  47 */     nbt.m_128379_("powered", true);
/*  48 */     C.deserializeNBT(nbt);
/*     */   }
/*     */ 
/*     */   
/*     */   void MakeNuclear(Entity Entity_Class) {
/*  53 */     Creeper C = (Creeper)Entity_Class;
/*  54 */     CompoundTag nbt = C.serializeNBT();
/*  55 */     nbt.m_128379_("powered", true);
/*  56 */     int IntNuclearCreeperBlastRadius = ((Integer)ESMConfig.NuclearCreeperExplosionRadius.get()).intValue();
/*  57 */     if (IntNuclearCreeperBlastRadius < 0)
/*  58 */       IntNuclearCreeperBlastRadius = 0; 
/*  59 */     if (IntNuclearCreeperBlastRadius > 255)
/*  60 */       IntNuclearCreeperBlastRadius = 255; 
/*  61 */     nbt.m_128344_("ExplosionRadius", (byte)IntNuclearCreeperBlastRadius);
/*  62 */     nbt.m_128405_("Fuse", ((Integer)ESMConfig.NuclearCreeperFuse.get()).intValue());
/*     */     
/*  64 */     C.deserializeNBT(nbt);
/*     */     
/*  66 */     LivingEntity Living_Entity = (LivingEntity)Entity_Class;
/*     */ 
/*     */     
/*  69 */     Living_Entity.m_21153_(Living_Entity.m_21223_() / 4.0F);
/*     */ 
/*     */     
/*  72 */     Living_Entity.m_21051_(Attributes.f_22279_).m_22125_(new AttributeModifier("Nuclear_Powered", 1.2000000476837158D, AttributeModifier.Operation.MULTIPLY_BASE));
/*     */ 
/*     */ 
/*     */     
/*  76 */     Living_Entity.m_7292_(new MobEffectInstance(MobEffects.f_19611_, 1000, 1));
/*     */   }
/*     */ 
/*     */ 
/*     */   
/*     */   public static void CreeperTick(Creeper C) {
/*  82 */     if (C.m_8077_() && C.m_7770_().getString().compareTo("ICBM") == 0)
/*     */     {
/*     */ 
/*     */       
/*  86 */       if (!C.m_32311_())
/*     */       {
/*     */         
/*  89 */         ICBMCreeper(C);
/*     */       }
/*     */     }
/*     */   }
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   
/*     */   static void ICBMCreeper(Creeper C) {
/* 100 */     CompoundTag nbt = C.serializeNBT();
/* 101 */     nbt.m_128379_("powered", true);
/* 102 */     nbt.m_128344_("ExplosionRadius", (byte)100);
/* 103 */     nbt.m_128344_("ExplosionPower", (byte)10);
/* 104 */     nbt.m_128405_("Fuse", 200);
/*     */     
/* 106 */     C.deserializeNBT(nbt);
/*     */ 
/*     */     
/* 109 */     C.m_32312_();
/*     */   }
/*     */   
/*     */   void KillIfOverXTicks() {}
/*     */ }


/* Location:              C:\Users\wapzi\Desktop\Plugins mC\mod\NightmareESM_1_20_4.jar!\ESM\Base\NightmareCreeper.class
 * Java compiler version: 17 (61.0)
 * JD-Core Version:       1.1.3
 */