/*    */ package ESM.Base;
/*    */ 
/*    */ import ESM.ESMConfig;
/*    */ import ESM.GiveRandomArmor;
/*    */ import ESM.RNG;
/*    */ import net.minecraft.world.entity.Entity;
/*    */ import net.minecraft.world.entity.monster.Monster;
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ public class NightmareSkeleton
/*    */ {
/*    */   public NightmareSkeleton(Entity Entity_Class) {
/* 17 */     if (((Boolean)ESMConfig.GiveSkeletonsArmor.get()).booleanValue())
/*    */     {
/* 19 */       new GiveRandomArmor(Entity_Class);
/*    */     }
/*    */ 
/*    */     
/* 23 */     if (((Boolean)ESMConfig.AllowSuperSkeletons.get()).booleanValue()) {
/*    */       
/* 25 */       int SkeleWeightValue = (new RNG()).GetInt(0, 100);
/* 26 */       double Chance_SuperSkeleton = ((Double)ESMConfig.ChanceOfSuperSkeleton.get()).doubleValue();
/* 27 */       if (SkeleWeightValue < Chance_SuperSkeleton) {
/*    */         
/* 29 */         Monster M = (Monster)Entity_Class;
/*    */         
/* 31 */         new SuperSkeleton(M);
/*    */       } 
/*    */     } 
/*    */   }
/*    */ }


/* Location:              C:\Users\wapzi\Desktop\Plugins mC\mod\NightmareESM_1_20_4.jar!\ESM\Base\NightmareSkeleton.class
 * Java compiler version: 17 (61.0)
 * JD-Core Version:       1.1.3
 */