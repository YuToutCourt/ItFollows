/*    */ package ESM.MobBehavs;
/*    */ 
/*    */ import net.minecraft.world.entity.Entity;
/*    */ import net.minecraft.world.entity.Mob;
/*    */ import net.minecraft.world.item.ItemStack;
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ public class ItemChecker
/*    */ {
/*    */   public static boolean EntityHasPickaxe(Entity Entity_Class) {
/* 16 */     Mob Mob_Class = (Mob)Entity_Class;
/* 17 */     ItemStack Item_In_Entity_Hand = Mob_Class.m_21205_();
/* 18 */     String Item_Name = Item_In_Entity_Hand.m_41778_();
/* 19 */     return Item_Name.toLowerCase().contains("pick");
/*    */   }
/*    */ }


/* Location:              C:\Users\wapzi\Desktop\Plugins mC\mod\NightmareESM_1_20_4.jar!\ESM\MobBehavs\ItemChecker.class
 * Java compiler version: 17 (61.0)
 * JD-Core Version:       1.1.3
 */