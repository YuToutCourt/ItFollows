/*    */ package ESM.MobBehavs;
/*    */ 
/*    */ import net.minecraft.world.effect.MobEffectInstance;
/*    */ import net.minecraft.world.effect.MobEffects;
/*    */ import net.minecraft.world.entity.Entity;
/*    */ import net.minecraft.world.entity.EntityType;
/*    */ import net.minecraft.world.entity.LivingEntity;
/*    */ import net.minecraft.world.entity.ai.attributes.AttributeModifier;
/*    */ import net.minecraft.world.entity.ai.attributes.Attributes;
/*    */ 
/*    */ 
/*    */ public class MobBloodlust
/*    */ {
/* 14 */   EntityType[] WhiteList = new EntityType[] { EntityType.f_20456_, EntityType.f_20458_, EntityType.f_20501_, EntityType.f_20530_, EntityType.f_20531_ };
/*    */ 
/*    */ 
/*    */ 
/*    */   
/*    */   public MobBloodlust(Entity Entity_Class) {
/* 20 */     if (isOnWhitelist(Entity_Class.m_6095_())) {
/*    */       
/* 22 */       LivingEntity Living_Entity = (LivingEntity)Entity_Class;
/*    */ 
/*    */       
/* 25 */       Living_Entity.m_21051_(Attributes.f_22276_)
/* 26 */         .m_22125_(new AttributeModifier("Bloodlust", (Living_Entity
/* 27 */             .m_21223_() + Living_Entity.m_21223_() * 0.5F), AttributeModifier.Operation.MULTIPLY_BASE));
/*    */       
/* 29 */       Living_Entity.m_21153_(Living_Entity.m_21223_() + Living_Entity.m_21223_() * 0.5F);
/*    */ 
/*    */       
/* 32 */       Living_Entity.m_21051_(Attributes.f_22279_).m_22125_(new AttributeModifier("Bloodlust", 1.25D, AttributeModifier.Operation.MULTIPLY_BASE));
/*    */ 
/*    */ 
/*    */       
/* 36 */       Living_Entity.m_7292_(new MobEffectInstance(MobEffects.f_19616_, 1000, 1));
/*    */     } 
/*    */   }
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */   
/*    */   boolean isOnWhitelist(EntityType Current_Entity_Type) {
/* 49 */     for (int i = 0; i < this.WhiteList.length; i++) {
/* 50 */       EntityType T = this.WhiteList[i];
/* 51 */       if (T == Current_Entity_Type)
/*    */       {
/* 53 */         return true;
/*    */       }
/*    */     } 
/*    */ 
/*    */     
/* 58 */     return false;
/*    */   }
/*    */ }


/* Location:              C:\Users\wapzi\Desktop\Plugins mC\mod\NightmareESM_1_20_4.jar!\ESM\MobBehavs\MobBloodlust.class
 * Java compiler version: 17 (61.0)
 * JD-Core Version:       1.1.3
 */