/*    */ package ESM;
/*    */ 
/*    */ import net.minecraft.world.entity.Entity;
/*    */ import net.minecraft.world.entity.EquipmentSlot;
/*    */ import net.minecraft.world.item.Item;
/*    */ import net.minecraft.world.item.ItemStack;
/*    */ import net.minecraft.world.item.Items;
/*    */ import net.minecraft.world.level.ItemLike;
/*    */ 
/*    */ public class GiveRandomArmor
/*    */ {
/* 12 */   float Chance_Of_Armor = 20.0F;
/*    */ 
/*    */   
/* 15 */   Item[] Armor_Head = new Item[] { Items.f_42464_, Items.f_42476_, Items.f_42468_, Items.f_42407_ };
/* 16 */   Item[] Armor_Chest = new Item[] { Items.f_42465_, Items.f_42477_, Items.f_42469_, Items.f_42408_ };
/* 17 */   Item[] Armor_Leggins = new Item[] { Items.f_42466_, Items.f_42478_, Items.f_42470_, Items.f_42462_ };
/* 18 */   Item[] Armor_Boots = new Item[] { Items.f_42467_, Items.f_42479_, Items.f_42471_, Items.f_42463_ };
/*    */ 
/*    */ 
/*    */   
/*    */   public GiveRandomArmor(Entity Entity_Class) {
/* 23 */     int RNG_Val = (new RNG()).GetInt(0, 100);
/* 24 */     if (RNG_Val < this.Chance_Of_Armor) {
/*    */ 
/*    */       
/* 27 */       int ArmorType = (new RNG()).GetInt(0, this.Armor_Head.length);
/* 28 */       Item Head_Armor = this.Armor_Head[ArmorType];
/* 29 */       Item Chest_Armor = this.Armor_Chest[ArmorType];
/* 30 */       Item Leggings_Armor = this.Armor_Leggins[ArmorType];
/* 31 */       Item Boots_Armor = this.Armor_Boots[ArmorType];
/*    */ 
/*    */       
/* 34 */       Entity_Class.m_8061_(EquipmentSlot.HEAD, new ItemStack((ItemLike)Head_Armor));
/* 35 */       Entity_Class.m_8061_(EquipmentSlot.CHEST, new ItemStack((ItemLike)Chest_Armor));
/* 36 */       Entity_Class.m_8061_(EquipmentSlot.LEGS, new ItemStack((ItemLike)Leggings_Armor));
/* 37 */       Entity_Class.m_8061_(EquipmentSlot.FEET, new ItemStack((ItemLike)Boots_Armor));
/*    */     } 
/*    */   }
/*    */ }


/* Location:              C:\Users\wapzi\Desktop\Plugins mC\mod\NightmareESM_1_20_4.jar!\ESM\GiveRandomArmor.class
 * Java compiler version: 17 (61.0)
 * JD-Core Version:       1.1.3
 */