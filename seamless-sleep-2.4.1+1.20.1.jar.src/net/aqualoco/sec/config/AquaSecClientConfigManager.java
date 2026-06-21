/*    */ package net.aqualoco.sec.config;
/*    */ 
/*    */ import com.google.gson.Gson;
/*    */ import com.google.gson.GsonBuilder;
/*    */ import java.io.IOException;
/*    */ import java.io.Reader;
/*    */ import java.io.Writer;
/*    */ import java.nio.file.Files;
/*    */ import java.nio.file.Path;
/*    */ import java.nio.file.attribute.FileAttribute;
/*    */ import net.aqualoco.sec.AquaSec;
/*    */ import net.fabricmc.loader.api.FabricLoader;
/*    */ 
/*    */ public final class AquaSecClientConfigManager
/*    */ {
/*    */   private static final String FILE_NAME = "seamless_sleep.json";
/* 17 */   private static final Gson GSON = (new GsonBuilder()).setPrettyPrinting().create();
/*    */   
/* 19 */   private static AquaSecClientConfig config = defaultConfig();
/*    */   
/*    */   private static Path configPath;
/*    */ 
/*    */   
/*    */   public static void init() {
/* 25 */     configPath = FabricLoader.getInstance().getConfigDir().resolve("seamless_sleep.json");
/* 26 */     config = loadOrCreate(configPath);
/*    */   }
/*    */   
/*    */   public static AquaSecClientConfig get() {
/* 30 */     return config;
/*    */   }
/*    */   
/*    */   private static AquaSecClientConfig loadOrCreate(Path path) {
/* 34 */     if (Files.notExists(path, new java.nio.file.LinkOption[0])) {
/* 35 */       AquaSecClientConfig cfg = defaultConfig();
/* 36 */       cfg.clamp();
/* 37 */       save(path, cfg);
/* 38 */       return cfg;
/*    */     } 
/*    */     
/* 41 */     try { Reader reader = Files.newBufferedReader(path); 
/* 42 */       try { AquaSecClientConfig cfg = (AquaSecClientConfig)GSON.fromJson(reader, AquaSecClientConfig.class);
/* 43 */         if (cfg == null) {
/* 44 */           cfg = defaultConfig();
/*    */         }
/* 46 */         cfg.clamp();
/* 47 */         save(path, cfg);
/* 48 */         AquaSecClientConfig aquaSecClientConfig1 = cfg;
/* 49 */         if (reader != null) reader.close();  return aquaSecClientConfig1; } catch (Throwable throwable) { if (reader != null) try { reader.close(); } catch (Throwable throwable1) { throwable.addSuppressed(throwable1); }   throw throwable; }  } catch (Exception e)
/* 50 */     { AquaSec.LOGGER.warn("Falha ao ler config {}, usando padrao. Erro: {}", path, e.getMessage());
/* 51 */       AquaSecClientConfig cfg = defaultConfig();
/* 52 */       save(path, cfg);
/* 53 */       return cfg; }
/*    */   
/*    */   }
/*    */   
/*    */   public static void save() {
/* 58 */     if (configPath == null || config == null) {
/*    */       return;
/*    */     }
/* 61 */     save(configPath, config);
/*    */   }
/*    */   
/*    */   private static void save(Path path, AquaSecClientConfig cfg) {
/*    */     
/* 66 */     try { Files.createDirectories(path.getParent(), (FileAttribute<?>[])new FileAttribute[0]);
/* 67 */       Writer writer = Files.newBufferedWriter(path, new java.nio.file.OpenOption[0]); 
/* 68 */       try { GSON.toJson(cfg, writer);
/* 69 */         if (writer != null) writer.close();  } catch (Throwable throwable) { if (writer != null)
/* 70 */           try { writer.close(); } catch (Throwable throwable1) { throwable.addSuppressed(throwable1); }   throw throwable; }  } catch (IOException e)
/* 71 */     { AquaSec.LOGGER.warn("Nao foi possivel salvar config {}: {}", path, e.getMessage()); }
/*    */   
/*    */   }
/*    */   
/*    */   private static AquaSecClientConfig defaultConfig() {
/* 76 */     return new AquaSecClientConfig();
/*    */   }
/*    */ }


/* Location:              C:\Users\wapzi\Desktop\Plugins mC\mod\seamless-sleep-2.4.1+1.20.1.jar!\net\aqualoco\sec\config\AquaSecClientConfigManager.class
 * Java compiler version: 17 (61.0)
 * JD-Core Version:       1.1.3
 */