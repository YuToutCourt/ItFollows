/*    */ package net.aqualoco.sec.mixin.client;
/*    */ 
/*    */ import net.minecraft.class_2561;
/*    */ import net.minecraft.class_2588;
/*    */ import net.minecraft.class_329;
/*    */ import net.minecraft.class_7417;
/*    */ import org.spongepowered.asm.mixin.Mixin;
/*    */ import org.spongepowered.asm.mixin.injection.At;
/*    */ import org.spongepowered.asm.mixin.injection.Inject;
/*    */ import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
/*    */ 
/*    */ @Mixin({class_329.class})
/*    */ public abstract class InGameHudSleepMessageMixin {
/*    */   @Inject(method = {"setOverlayMessage"}, at = {@At("HEAD")}, cancellable = true)
/*    */   private void aquasec$skipVanillaSleepMessage(class_2561 message, boolean tinted, CallbackInfo ci) {
/* 16 */     if (message == null) {
/*    */       return;
/*    */     }
/* 19 */     class_7417 class_7417 = message.method_10851(); if (class_7417 instanceof class_2588) { class_2588 content = (class_2588)class_7417;
/* 20 */       String key = content.method_11022();
/* 21 */       if ("sleep.skipping_night".equals(key))
/* 22 */         ci.cancel();  }
/*    */   
/*    */   }
/*    */ }


/* Location:              C:\Users\wapzi\Desktop\Plugins mC\mod\seamless-sleep-2.4.1+1.20.1.jar!\net\aqualoco\sec\mixin\client\InGameHudSleepMessageMixin.class
 * Java compiler version: 17 (61.0)
 * JD-Core Version:       1.1.3
 */