/*    */ package ESM;
/*    */ 
/*    */ import net.minecraft.world.entity.EquipmentSlot;
/*    */ import net.minecraft.world.entity.monster.Monster;
/*    */ import net.minecraft.world.item.Item;
/*    */ import net.minecraft.world.item.ItemStack;
/*    */ import net.minecraft.world.item.Items;
/*    */ import net.minecraft.world.level.ItemLike;
/*    */ 
/*    */ public class GiveRandomPickaxe
/*    */ {
/* 12 */   Item[] Pickaxes = new Item[] { Items.f_42427_, Items.f_42385_, Items.f_42422_, Items.f_42383_, Items.f_42425_, Items.f_42420_, Items.f_42386_, Items.f_42423_, Items.f_42428_ };
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */   
/*    */   public GiveRandomPickaxe(Monster M) {
/* 20 */     ItemStack Pickaxe_Item_Stack = M.m_6844_(EquipmentSlot.MAINHAND);
/*    */ 
/*    */     
/* 23 */     if (Pickaxe_Item_Stack.m_41619_()) {
/*    */       
/* 25 */       Item Pickaxe_Item = RandomFromList();
/* 26 */       Pickaxe_Item_Stack = new ItemStack((ItemLike)Pickaxe_Item);
/* 27 */       M.m_8061_(EquipmentSlot.MAINHAND, Pickaxe_Item_Stack);
/*    */     } 
/*    */   }
/*    */ 
/*    */ 
/*    */   
/*    */   Item RandomFromList() {
/* 34 */     int PickaxeIndex = (new RNG()).GetInt(0, this.Pickaxes.length);
/* 35 */     return this.Pickaxes[PickaxeIndex];
/*    */   }
/*    */ }


/* Location:              C:\Users\wapzi\Desktop\Plugins mC\mod\NightmareESM_1_20_4.jar!\ESM\GiveRandomPickaxe.class
 * Java compiler version: 17 (61.0)
 * JD-Core Version:       1.1.3
 */