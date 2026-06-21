package io.github.yutoutcourt.itfollows.client.render;

import io.github.yutoutcourt.itfollows.Itfollows;
import io.github.yutoutcourt.itfollows.client.PresenceVisuals;
import io.github.yutoutcourt.itfollows.config.ItFollowsConfig;
import io.github.yutoutcourt.itfollows.mixin.client.PostChainAccessor;
import net.minecraft.client.Minecraft;
import com.mojang.blaze3d.shaders.AbstractUniform;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.client.renderer.PostPass;
import net.minecraft.resources.ResourceLocation;

import java.io.IOException;

/**
 * Gère la {@link PostChain} d'effets de présence (Phase 4b) : création paresseuse, redimensionnement,
 * écriture des uniforms custom et exécution après le rendu du monde. On gère la chaîne nous-mêmes
 * (pas via {@code GameRenderer#loadEffect}, qui est écrasé par les vues d'entités en spectateur).
 *
 * <p>Pilotée par {@link PresenceVisuals}. Le pipeline ne s'allume que lorsqu'un effet est actif
 * (sinon {@code process()} n'est pas appelé — coût nul au repos).
 */
public final class PresencePostProcessor {

    private static final ResourceLocation CHAIN =
            new ResourceLocation(Itfollows.MOD_ID, "shaders/post/presence.json");

    /**
     * Iris / Oculus remplacent le pipeline de rendu : notre {@link PostChain} ne s'appliquerait pas
     * (au mieux ignorée, au pire image corrompue). Détecté une fois au chargement. Les mixins
     * FOV/caméra, eux, restent actifs — ils ne dépendent pas de cette chaîne.
     */
    private static final boolean SHADER_MOD_PRESENT =
            FabricLoader.getInstance().isModLoaded("iris")
                    || FabricLoader.getInstance().isModLoaded("oculus");

    private boolean loggedIrisSkip;
    private PostChain chain;
    private boolean failed;
    private int lastWidth = -1;
    private int lastHeight = -1;

    /** À appeler après le rendu du monde (WorldRenderEvents.LAST), avant le HUD. */
    public void renderAfterWorld(float tickDelta) {
        // Toujours faire avancer les niveaux lissés (l'overlay HUD lit PresenceVisuals.closeness()).
        PresenceVisuals.tick(tickDelta);

        ItFollowsConfig cfg = ItFollowsConfig.get();
        if (SHADER_MOD_PRESENT) {
            if (!loggedIrisSkip) {
                Itfollows.LOGGER.info("[ItFollows] Iris/Oculus détecté : pipeline GLSL de présence désactivé (mixins FOV/caméra conservés).");
                loggedIrisSkip = true;
            }
            return;
        }
        if (failed || !cfg.presenceGlslEnabled || !PresenceVisuals.anyActive()) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return;
        }

        ensureLoaded(mc);
        if (chain == null) {
            return;
        }
        ensureSize(mc);
        writeUniforms();
        chain.process(tickDelta);
        // Re-bind l'écran pour que le HUD se dessine ensuite au bon endroit.
        mc.getMainRenderTarget().bindWrite(true);
    }

    private void ensureLoaded(Minecraft mc) {
        if (chain != null || failed) {
            return;
        }
        try {
            chain = new PostChain(mc.getTextureManager(), mc.getResourceManager(),
                    mc.getMainRenderTarget(), CHAIN);
            lastWidth = -1;
            lastHeight = -1;
        } catch (IOException e) {
            failed = true;
            Itfollows.LOGGER.error("[ItFollows] Échec du chargement du shader de présence, GLSL désactivé.", e);
        }
    }

    private void ensureSize(Minecraft mc) {
        int w = mc.getWindow().getWidth();
        int h = mc.getWindow().getHeight();
        if (w != lastWidth || h != lastHeight) {
            chain.resize(w, h);
            lastWidth = w;
            lastHeight = h;
        }
    }

    private void writeUniforms() {
        setUniform("DesatAmount", PresenceVisuals.desat());
        setUniform("WaveAmount", PresenceVisuals.wave());
        setUniform("BlurAmount", PresenceVisuals.blur());
        setUniform("GhostAmount", PresenceVisuals.ghost());
        setUniform("NoiseAmount", PresenceVisuals.noise());
        setUniform("VignetteAmount", PresenceVisuals.vignette());
    }

    /** Écrit un uniform sur toutes les passes (safeGetUniform renvoie un no-op pour celles qui l'ignorent). */
    private void setUniform(String name, float value) {
        for (PostPass pass : ((PostChainAccessor) chain).itfollows$getPasses()) {
            AbstractUniform uniform = pass.getEffect().safeGetUniform(name);
            uniform.set(value);
        }
    }

    /** À appeler au reload de resources (F3+T) : la chaîne sera recréée à la prochaine frame utile. */
    public void reload() {
        if (chain != null) {
            chain.close();
            chain = null;
        }
        failed = false;
        lastWidth = -1;
        lastHeight = -1;
    }
}
