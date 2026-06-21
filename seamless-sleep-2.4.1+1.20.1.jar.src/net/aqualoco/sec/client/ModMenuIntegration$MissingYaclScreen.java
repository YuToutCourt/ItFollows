/*     */ package net.aqualoco.sec.client;
/*     */ 
/*     */ import net.minecraft.class_124;
/*     */ import net.minecraft.class_2561;
/*     */ import net.minecraft.class_332;
/*     */ import net.minecraft.class_364;
/*     */ import net.minecraft.class_4185;
/*     */ import net.minecraft.class_437;
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
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ class MissingYaclScreen
/*     */   extends class_437
/*     */ {
/*     */   private final class_437 parent;
/*     */   
/*     */   protected MissingYaclScreen(class_437 parent) {
/*  81 */     super((class_2561)class_2561.method_43470("Seamless Sleep"));
/*  82 */     this.parent = parent;
/*     */   }
/*     */ 
/*     */   
/*     */   public void method_25419() {
/*  87 */     this.field_22787.method_1507(this.parent);
/*     */   }
/*     */ 
/*     */   
/*     */   protected void method_25426() {
/*  92 */     if (this.field_22787 == null)
/*  93 */       return;  method_37063((class_364)class_4185.method_46430(
/*  94 */           (class_2561)class_2561.method_43471("gui.back"), b -> method_25419())
/*     */         
/*  96 */         .method_46434(this.field_22789 / 2 - 50, this.field_22790 / 2 + 10, 100, 20)
/*  97 */         .method_46431());
/*     */   }
/*     */ 
/*     */   
/*     */   public void method_25394(class_332 context, int mouseX, int mouseY, float delta) {
/* 102 */     method_25420(context);
/* 103 */     int x = this.field_22789 / 2;
/* 104 */     int y = this.field_22790 / 2 - 20;
/* 105 */     context.method_27534(this.field_22793, (class_2561)class_2561.method_43470("YACL nao encontrado").method_27692(class_124.field_1061), x, y, 16777215);
/* 106 */     context.method_27534(this.field_22793, (class_2561)class_2561.method_43470("Instale YetAnotherConfigLib v3 para editar a config."), x, y + 12, 16777215);
/* 107 */     super.method_25394(context, mouseX, mouseY, delta);
/*     */   }
/*     */ }


/* Location:              C:\Users\wapzi\Desktop\Plugins mC\mod\seamless-sleep-2.4.1+1.20.1.jar!\net\aqualoco\sec\client\ModMenuIntegration$MissingYaclScreen.class
 * Java compiler version: 17 (61.0)
 * JD-Core Version:       1.1.3
 */