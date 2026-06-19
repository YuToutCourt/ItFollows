/*    */ package ESM.Base;
/*    */ 
/*    */ import ESM.ESMConfig;
/*    */ import ESM.GiveRandomArmor;
/*    */ import ESM.GiveRandomPickaxe;
/*    */ import ESM.RNG;
/*    */ import net.minecraft.world.entity.Entity;
/*    */ import net.minecraft.world.entity.monster.Monster;
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
/*    */ public class NightmareZombie
/*    */ {
/*    */   public NightmareZombie(Entity Entity_Class) {
/* 26 */     int ZombWeightValue = (new RNG()).GetInt(0, 100);
/*    */ 
/*    */     
/* 29 */     Monster M = (Monster)Entity_Class;
/*    */ 
/*    */     
/* 32 */     if (((Boolean)ESMConfig.GiveZombiesPickaxe.get()).booleanValue()) {
/*    */       
/* 34 */       int PickaxeWeightVal = (new RNG()).GetInt(0, 100);
/*    */       
/* 36 */       if (PickaxeWeightVal < ((Integer)ESMConfig.ChanceOfPickaxe.get()).intValue())
/*    */       {
/* 38 */         new GiveRandomPickaxe(M);
/*    */       }
/*    */     } 
/*    */ 
/*    */     
/* 43 */     if (((Boolean)ESMConfig.GiveZombiesArmor.get()).booleanValue())
/*    */     {
/*    */       
/* 46 */       new GiveRandomArmor(Entity_Class);
/*    */     }
/*    */   }
/*    */ 
/*    */ 
/*    */   
/*    */   void SetTicks(Monster M, int Amt) {
/* 53 */     M.getPersistentData().m_128405_("placecooloff", Amt);
/* 54 */     M.getPersistentData().m_128405_("digcooloff", Amt);
/*    */   }
/*    */ }


/* Location:              C:\Users\wapzi\Desktop\Plugins mC\mod\NightmareESM_1_20_4.jar!\ESM\Base\NightmareZombie.class
 * Java compiler version: 17 (61.0)
 * JD-Core Version:       1.1.3
 */