package io.github.yutoutcourt.itfollows.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Configuration du mod, sérialisée en JSON via Gson (déjà fourni par Minecraft).
 * Chargée une fois au démarrage ; régénérée avec les valeurs par défaut si absente.
 *
 * <p>Toutes les valeurs liées au gameplay (coûts, seuils, intervalles) sont ici pour
 * faciliter l'équilibrage sans recompiler.
 */
public class ItFollowsConfig {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String FILE_NAME = "itfollows.json";

    private static ItFollowsConfig instance;

    // --- Fatigue : coûts d'actions (points de fatigue retirés) ---
    /** Coût appliqué par tick de sprint. */
    public float sprintCostPerTick = 0.02f;
    /** Coût appliqué par tick de marche (déplacement horizontal sans sprint). */
    public float walkCostPerTick = 0.002f;
    /** Coût appliqué par tick de nage (effort plus élevé que la marche). */
    public float swimCostPerTick = 0.015f;
    /** Coût appliqué par tick passé dans un bateau en mouvement (effort faible : le bateau soulage). */
    public float boatCostPerTick = 0.009f;
    /** Coût d'un saut. */
    public float jumpCost = 0.01f;
    /** Coût d'une attaque en mêlée. */
    public float attackCost = 0.005f;
    /** Coût du minage d'un bloc. */
    public float mineCost = 0.001f;

    // --- Fatigue : échantillonnage périodique ---
    /** Intervalle (en ticks) entre deux passes d'échantillonnage (décroissance/régen/effets/sync). */
    public int sampleIntervalTicks = 20;
    /** Régén passive lente appliquée à chaque échantillonnage quand le joueur n'a rien fait de coûteux. */
    public float passiveRegenPerSample = 0.009f;

    // --- Sieste (Phase 1.5 : récup rapide via un lit, sans changer l'heure du monde) ---
    /** Délai (en ticks) entre l'entrée dans le lit et le début de la récupération. 60 ticks = 3 s. */
    public int siesteDelayTicks = 60;
    /** Récup appliquée à chaque échantillonnage (~1 s) pendant la sieste, une fois le délai passé. 0.5 ≈ 0.5 %/s. */
    public float siesteRegenPerSample = 0.5f;

    // --- Phase 5 : sommeil profond & nuit globale (clic lit la NUIT) ---
    /** Active le sommeil profond + la nuit globale. Si false, le clic lit la nuit retombe sur la sieste. */
    public boolean deepSleepEnabled = true;
    /** Récup appliquée à chaque échantillonnage en sommeil profond (× qualité). > sieste : récup la plus rapide. */
    public float deepSleepRegenPerSample = 2.0f;
    /** Délai (ticks) entre l'entrée dans le lit et le début de la récup (anti micro-sommeil). 60 = 3 s. */
    public int deepSleepDelayTicks = 60;
    /** Durée mini (ticks) de l'animation de transition de nuit (peu de nuit restante). 600 = 30 s. */
    public int nightTransitionMinTicks = 600;
    /** Durée maxi (ticks) de l'animation de transition de nuit (nuit pleine). 1200 = 60 s. */
    public int nightTransitionMaxTicks = 1200;
    /** Facteur d'atténuation du voile noir de sommeil pendant la transition (0-1) pour voir le ciel défiler. */
    public float sleepDarknessFactor = 0.35f;
    /** Active la modulation de la récup/audio par la qualité du sommeil (monstres/pluie/entité/feu de camp). */
    public boolean sleepQualityEnabled = true;
    /** Plancher de qualité (0-1) : même le pire sommeil récupère au moins ce facteur. */
    public float sleepQualityMin = 0.2f;
    /** Active les sons d'ambiance angoissants joués au dormeur (lui seul les entend). */
    public boolean sleepAudioEnabled = true;
    /** Intervalle (ticks) entre deux tentatives de son d'ambiance pendant le sommeil. 120 = 6 s. */
    public int sleepAudioIntervalTicks = 120;

    // --- Évanouissement (quand la fatigue atteint 0) ---
    /** Durée de l'évanouissement en ticks (écran noir + immobilisation). 200 ticks = 10 s. */
    public int faintDurationTicks = 200;
    /** Fatigue restaurée au réveil après un évanouissement. 10 = 10 %. */
    public float faintWakeFatigue = 10.0f;

    // --- Fatigue : seuils de paliers (sur 0-100, 100 = en forme) ---
    public float thresholdLegere = 75.0f;
    public float thresholdMoyenne = 50.0f;
    public float thresholdElevee = 25.0f;
    public float thresholdCritique = 10.0f;

    // --- Seuils d'apparition des effets sensoriels (sur 0-100), indépendants des paliers ---
    /** Fatigue de minage (mining fatigue) sous ce seuil. */
    public float miningFatigueThreshold = 30.0f;
    /** Nausée (par vagues) sous ce seuil. */
    public float nauseaThreshold = 10.0f;
    /** Cécité (blindness) sous ce seuil. */
    public float blindnessThreshold = 5.0f;

    // --- Nausée intermittente : une vague courte, puis un répit aléatoire, pour donner « la tête qui tourne » ---
    /** Durée d'une vague de nausée active (ticks). 100 = 5 s. */
    public int nauseaWaveOnTicks = 100;
    /** Répit minimum entre deux vagues de nausée (ticks). 400 = 20 s. */
    public int nauseaWaveOffMinTicks = 400;
    /** Répit maximum entre deux vagues de nausée (ticks). 1000 = 50 s. */
    public int nauseaWaveOffMaxTicks = 1000;

    // --- Phase 2 : entité traqueuse (Stalker) ---
    /**
     * Délai de grâce (ticks) après l'arrivée du premier joueur avant la <b>sélection</b> de la cible
     * (le plus fatigué). 12000 = ~10 min. Passé ce délai, l'escalade des avertissements démarre
     * (cf. {@code HauntPhaseController}), pas la traque active directement.
     */
    public int graceTicks = 12000;
    /** Vitesse de déplacement de l'entité (attribut MOVEMENT_SPEED). Volontairement lente. */
    public float stalkerSpeed = 0.25f;
    /** Dégâts infligés à la cible au contact (attribut ATTACK_DAMAGE). */
    public float stalkerAttackDamage = 9.0f;
    /** Intervalle (ticks) entre deux passes de la boucle de traque (sélection/simulation/matérialisation). */
    public int hauntSampleIntervalTicks = 20;

    // --- Phase 2 : poursuite virtuelle hors-chunks (modèle C) ---
    // Les distances ne sont PAS fixes : elles dérivent de la config du serveur.
    //  • Apparition : une fraction de la distance de RENDU (getViewDistance × 16) → la traqueuse se
    //    matérialise assez loin pour qu'on la voie arriver.
    //  • Disparition : juste en deçà de la distance de SIMULATION (getSimulationDistance × 16), seule
    //    frontière où l'entité « tourne » encore ; au-delà elle gèlerait puis serait déchargée.
    // L'apparition est plafonnée à l'intérieur de la zone de simulation (sinon l'entité serait figée) :
    // si le rendu dépasse largement la simulation, elle apparaît au bord de la simulation, pas plus loin.
    // En solo, ces deux distances sont celles réglées dans les options vidéo du joueur.
    /** Vitesse de poursuite (blocs/seconde) quand l'entité n'est pas matérialisée. Volontairement < vitesse de marche. */
    public float stalkerVirtualSpeed = 1.5f;
    /** Marge (blocs) en deçà de la frontière de simulation où l'on dématérialise (garantit qu'elle ne gèle jamais). */
    public int stalkerDespawnMargin = 16;
    /** Fraction de la distance de rendu à laquelle la traqueuse apparaît (0.5 = moitié), plafonnée à la zone de simulation. */
    public float stalkerSpawnViewFraction = 0.5f;
    /** Poids de l'écart vertical dans la distance de poursuite : >1 ⇒ fuir en hauteur/profondeur donne du répit. */
    public float stalkerVerticalPenalty = 2.0f;
    /** Demi-fenêtre (blocs) de recherche d'un sol valable autour de l'altitude de la cible à la matérialisation. */
    public int stalkerPlacementSearch = 16;

    // --- Phase 2 : IA de déplacement (casse, escalade, nage) ---
    /** Active la casse des blocs qui bloquent l'entité (sinon elle ne fait que contourner). */
    public boolean stalkerCanBreakBlocks = true;
    /** Vitesse de casse : progression par tick = stalkerBreakSpeed / dureté du bloc. Plus haut = plus rapide. */
    public float stalkerBreakSpeed = 0.08f;
    /** Dureté max d'un bloc cassable (-1 = pas de limite ; les blocs incassables type bedrock ne le sont jamais). */
    public float stalkerBreakMaxHardness = -1.0f;
    /** Les blocs cassés lâchent-ils leurs items ? Défaut : non (destruction permanente sans butin). */
    public boolean stalkerBreakDropItems = false;
    /** Ouvre les portes/portillons/trappes en bois plutôt que de les casser. Le fer est toujours cassé. */
    public boolean stalkerOpenDoors = true;
    /** Facteur de vitesse en nage (multiplie la vitesse de base). Plus bas = nage plus lente. */
    public float stalkerSwimSpeedFactor = 0.06f;
    /**
     * Résistance au courant : fraction du flux d'eau annulée chaque tick (1.0 = courant entièrement
     * neutralisé, 0 = subit le courant comme un mob normal). 1.0 (défaut) : l'entité avance à sa seule
     * vitesse de nage, sans la poussée parasite du courant qui créait une « accélération horrible ».
     * Pour la lave : pas de courant fort, sans effet.
     */
    public float stalkerCurrentResistance = 1.0f;

    /** L'entité peut-elle entrer et nager dans la lave (elle y est ignifugée) ? */
    public boolean stalkerCanEnterLava = true;

    // --- Phase 2 : vol (poursuite verticale quand la marche échoue) ---
    /** Active le vol : l'entité décolle quand la poursuite au sol est bloquée par un obstacle vertical (cible perchée, surplomb). */
    public boolean stalkerCanFly = true;
    /** Facteur de vitesse en vol (multiplie MOVEMENT_SPEED). >1 = monte/avance vite dans les airs. */
    public float stalkerFlySpeedFactor = 0.3f;
    /** Hauteur (blocs) de la cible au-dessus de l'entité requise pour envisager un décollage. */
    public float stalkerFlyTriggerHeight = 1.5f;
    /**
     * Portée horizontale (blocs) sous laquelle l'entité peut décoller / sauter. Au-delà, elle
     * <b>s'approche d'abord au sol</b> : on évite qu'elle s'envole depuis l'autre bout de la map et
     * on garantit que le déploiement des ailes (ou l'élan du saut) se passe sous les yeux du joueur.
     */
    public float stalkerFlyApproachRange = 10.0f;
    /** Durée minimale (ticks) d'un vol avant de pouvoir reposer pied à terre (anti-clignotement décollage/atterrissage). */
    public int stalkerMinFlightTicks = 20;
    /**
     * Délai (ticks) pendant lequel l'entité <b>observe</b> une cible perchée avant de décoller : elle
     * s'approche au sol et « regarde » le joueur ; s'il est toujours trop haut une fois le délai écoulé,
     * elle déploie les ailes. Donne le tempo « It Follows » (lent, inéluctable). 100 = 5 s.
     */
    public int stalkerFlyDelayTicks = 100;

    // --- Phase 2 : saut chargé (alternative au vol pour les petits obstacles verticaux) ---
    /** L'entité peut-elle faire un saut boosté pour franchir un petit obstacle plutôt que de voler ? */
    public boolean stalkerCanJump = true;
    /**
     * Hauteur max (blocs, cible au-dessus de l'entité) franchie par un <b>saut chargé</b> plutôt que
     * par le vol. En deçà : élan + saut (visible, organique) ; au-delà : déploiement des ailes.
     */
    public float stalkerJumpMaxHeight = 4.0f;
    /** Durée de la phase de charge (ticks) avant la détente : laisse jouer l'animation d'élan. 12 ≈ 0,6 s. */
    public int stalkerJumpChargeTicks = 12;
    /** Délai (ticks) imposé après un saut avant d'en réarmer un autre (anti-spam si l'entité retombe). */
    public int stalkerJumpCooldownTicks = 20;
    /** Délai (ticks) sans progression avant de re-matérialiser l'entité près de la cible (anti-blocage). 200 = 10 s. */
    public int stalkerStuckTimeoutTicks = 200;

    // --- Phase 2 : anticipation (interception de la cible) ---
    /**
     * Ticks d'anticipation : l'entité vise où la cible <b>va</b> (position + vélocité × ce facteur)
     * plutôt que sa position actuelle. 0 = vise la position actuelle. Trop haut = incohérent.
     */
    public int stalkerLeadTicks = 6;

    // --- Phase 2b : escalade de la traque (avertissement progressif) ---
    // Une fois la cible choisie (fin de grâce), on ne traque pas tout de suite : on enchaîne trois
    // étapes d'avertissement (bruits → portes/blocs → silhouette) puis la révélation « LOOK BEHIND YOU »,
    // pilotées par HauntPhaseController. Les durées d'étapes totalisent ~10 min par défaut.
    /** Durée (ticks) de l'étape 1 « bruits lointains ». 4000 = ~3 min 20. */
    public int hauntDistantStageTicks = 4000;
    /** Durée (ticks) de l'étape 2 « portes/blocs cassés à proximité ». 4000 = ~3 min 20. */
    public int hauntPhysicalStageTicks = 4000;
    /** Durée (ticks) de l'étape 3 « silhouette aperçue ». 4000 = ~3 min 20. */
    public int hauntSilhouetteStageTicks = 4000;

    /** Intervalle minimum (ticks) entre deux bruits lointains (étape 1). 800 = 40 s. */
    public int hauntDistantSoundMinIntervalTicks = 800;
    /** Intervalle maximum (ticks) entre deux bruits lointains (étape 1). 1200 = 60 s. */
    public int hauntDistantSoundMaxIntervalTicks = 1200;
    /** Distance min (blocs) à laquelle le bruit lointain est spatialisé autour de la cible. */
    public float hauntDistantSoundMinDistance = 10.0f;
    /** Distance max (blocs) à laquelle le bruit lointain est spatialisé autour de la cible. */
    public float hauntDistantSoundMaxDistance = 22.0f;
    /** Volume du bruit lointain (0-1). Bas pour rester « au loin ». */
    public float hauntDistantSoundVolume = 0.6f;

    /** Intervalle minimum (ticks) entre deux événements physiques (étape 2). 200 = 10 s. */
    public int hauntPhysicalEventMinIntervalTicks = 200;
    /** Intervalle maximum (ticks) entre deux événements physiques (étape 2). 500 = 25 s. */
    public int hauntPhysicalEventMaxIntervalTicks = 500;
    /** Rayon (blocs) autour de la cible où l'on ouvre une porte / casse un bloc réel (étape 2). */
    public int hauntPhysicalEventRadius = 20;

    /** Intervalle minimum (ticks) entre deux flashs de silhouette (étape 3). 300 = 15 s. */
    public int hauntSilhouetteMinIntervalTicks = 300;
    /** Intervalle maximum (ticks) entre deux flashs de silhouette (étape 3). 700 = 35 s. */
    public int hauntSilhouetteMaxIntervalTicks = 700;
    /** Durée (ticks) d'un flash de silhouette avant retrait. 20 = 1 s. */
    public int hauntSilhouetteDurationTicks = 20;
    /** Distance min (blocs) à laquelle la silhouette apparaît dans le champ de vision de la cible. */
    public float hauntSilhouetteMinDistance = 16.0f;
    /** Distance max (blocs) à laquelle la silhouette apparaît dans le champ de vision de la cible. */
    public float hauntSilhouetteMaxDistance = 28.0f;
    /** Demi-angle (degrés) du cône frontal où la silhouette peut apparaître (peripheral vision). */
    public float hauntSilhouetteConeDegrees = 50.0f;

    // --- Phase 2c : révélation « LOOK BEHIND YOU » ---
    /** Distance (blocs) derrière la cible où le leurre immobile apparaît à la révélation. */
    public float revealDistance = 5.0f;
    /** Portée (blocs) du raycast de regard pour détecter que la cible vise le leurre. */
    public float revealLookDetectRange = 12.0f;
    /** Ticks consécutifs de regard sur le leurre avant de déclencher la traque (anti faux positif). 3 = 0,15 s. */
    public int revealLookHoldTicks = 3;
    /** Volume de la voix « LOOK BEHIND YOU » (0-1). */
    public float lookBehindVolume = 1.0f;

    // --- Phase 3 : malédiction & transfert ---
    /** Active le système de malédiction. */
    public boolean curseEnabled = true;
    /** Délai (ticks) après l'entrée en HUNTING avant la livraison de l'objectif au maudit. 12000 = 10 min. */
    public int curseObjectiveDelayTicks = 12000;
    /** Intervalle (ticks) entre deux passes de détection des actions de malédiction. 10 = 0,5 s. */
    public int curseSampleIntervalTicks = 10;
    /** Délai (ticks) pendant lequel le nouveau maudit ne peut pas re-maudire celui qui vient de le maudire. 1200 = 1 min. */
    public int curseAntiPingPongTicks = 0;
    /** Nombre d'actions récentes mémorisées pour éviter de retomber sur les mêmes (anti-répétition). */
    public int curseRecentMemory = 5;
    /** Intervalle (ticks) de rappel de l'objectif en barre d'action tant que l'action est active. 40 = 2 s. */
    public int curseReminderIntervalTicks = 40;
    /** Portée (blocs) générique « près de la cible » pour la plupart des actions. */
    public float curseProximityRange = 6.0f;
    /** Demi-angle (degrés) du cône de regard pour « fixer » la cible. */
    public float curseLookConeDegrees = 7.0f;
    /** Durée (ticks) de regard cumulé pour « Le regard ». 240 = 12 s. */
    public int curseStareTicks = 240;
    /** Durée (ticks) de suivi continu pour « L'ombre ». 600 = 30 s. */
    public int curseFollowTicks = 600;
    /** Tolérance (ticks) de décrochage avant reset du suivi/immobilité. 60 = 3 s. */
    public int curseBreakGraceTicks = 60;
    /** Durée (ticks) de l'immobilité fixe pour « Le planté ». 600 = 30 s. */
    public int cursePlanteTicks = 600;
    /** Durée (ticks) de miroir de posture/mouvement pour mime/imitateur. 200 = 10 s. */
    public int curseMimicTicks = 200;
    /** Nombre de bascules d'accroupissement pour « Le sneak rituel ». */
    public int curseSneakCount = 5;
    /** Nombre de cycles accroupi/relevé rapides pour « Le yo-yo ». */
    public int curseYoyoCount = 8;
    /** Nombre de sauts pour « Le métronome ». */
    public int curseJumpCount = 30;
    /** Nombre d'items à gaspiller (lave/vide) pour « Le sacrifice ». */
    public int curseSacrificeCount = 8;
    /** Nombre d'items à déposer au sol pour « L'autel ». */
    public int curseAltarCount = 5;
    /** Profondeur (blocs) à creuser pour « Le fossoyeur ». */
    public int curseGraveDepth = 2;
    /** Longueur (blocs) du tunnel/escalier pour « Le tunnelier ». */
    public int curseTunnelLength = 5;
    /** Hauteur (blocs) du pilier pour « Le bâtisseur ». */
    public int curseTowerHeight = 5;
    /** Nombre de feux/feux de camp pour « Le pyromane ». */
    public int curseFireCount = 3;
    /** Nombre de blocs-signature en cercle pour « L'encerclement ». */
    public int curseRingCount = 4;
    /** Nombre de côtés à boucher pour « Le geôlier ». */
    public int curseCageSides = 3;
    /** Nombre de panneaux à poser (avec le mot imposé) pour « Le facteur ». */
    public int curseSignCount = 3;
    /** Durée (ticks) dans le noir à proximité pour « Le veilleur nocturne ». 160 = 8 s. */
    public int curseDarkTicks = 200;
    /** Durée (ticks) près de la cible endormie pour « Le veilleur de sommeil ». 120 = 6 s. */
    public int curseSleepWatchTicks = 120;

    // --- Phase 4a : effets de présence (2 pistes : fatigue [tous] + distance entité [cible]) ---
    // Piste 1 (fatigue, TOUS les joueurs, paliers 20/10/5 %) : pilotée côté client par la fatigue.
    // Piste 2 (distance à l'entité, cible seule, bandes 30/15/10/5/<3 blocs) : distance envoyée au client.
    // Couche « simple » : overlays HUD + audio ciblé/spatialisé. Les effets shaders (désaturation réelle,
    // flou, distorsion wave, ghosting) et mixins (FOV tunnel, screen shake, zoom) viendront ensuite.
    /** Active les effets de présence. */
    public boolean presenceEnabled = true;
    /** Intervalle (ticks) entre deux passes du calcul de présence (sync distance + roulements audio). */
    public int presenceSampleIntervalTicks = 5;

    // Piste 1 : paliers de fatigue (sur 0-100) déclenchant les effets de présence.
    /** Fatigue ≤ ce palier : vignette légère + battement de cœur faible + sons zombie/creeper rares. 20 %. */
    public float presenceFatigueLight = 20.0f;
    /** Fatigue ≤ ce palier : vignette forte + assombrissement + pulsation rapide + pas derrière soi. 10 %. */
    public float presenceFatigueStrong = 10.0f;
    /** Fatigue ≤ ce palier : ajoute les murmures (.ogg). 5 %. */
    public float presenceFatigueWhisper = 5.0f;
    /** Chance (0-1) de jouer un son zombie/creeper à chaque battement quand fatigué (≤ palier léger). */
    public float presenceMobSoundChance = 0.12f;

    // Piste 2 : bandes de distance (blocs) entre l'entité et la cible.
    /** Distance (blocs) max d'effet de l'entité : premiers effets à ce seuil, rien au-delà. */
    public float presenceEntityMaxDistance = 30.0f;
    /** Bande « présence confirmée » (blocs) : parasite radio + distorsion. */
    public float presenceBandConfirmed = 15.0f;
    /** Bande « danger » (blocs) : silhouettes 1 frame + whispers stéréo. */
    public float presenceBandDanger = 10.0f;
    /** Bande « très proche » (blocs) : vision tunnel + heartbeat synchronisé distance. */
    public float presenceBandClose = 5.0f;
    /** Bande « cut » (blocs) : silence brutal / coupure audio (approximée). */
    public float presenceBandCut = 3.0f;

    // Volumes des sons de présence (0-1).
    /** Volume max du battement de cœur (0-1). */
    public float heartbeatMaxVolume = 0.8f;
    /** Volume des pas « derrière soi » (0-1). */
    public float presenceFootstepVolume = 0.6f;
    /** Volume des murmures (0-1). */
    public float presenceWhisperVolume = 0.7f;
    /** Volume du parasite radio (0-1). */
    public float presenceRadioVolume = 0.6f;
    /** Volume du cri de hunting (0-1). */
    public float presenceHuntingCryVolume = 0.7f;
    /** Volume des sons zombie/creeper d'ambiance (0-1). */
    public float presenceMobVolume = 0.35f;

    // Overlays visuels (alpha 0-1).
    /** Alpha max de la vignette sombre des bords (0-1). */
    public float presenceVignetteMaxAlpha = 0.45f;
    /** Alpha max du voile d'assombrissement plein écran (0-1). */
    public float presenceDarkenMaxAlpha = 0.2f;
    /** Alpha max du voile gris de pseudo-désaturation (0-1). */
    public float presenceDesaturationMaxAlpha = 0.18f;
    /** Léger vacillement (jitter) de la vignette aux bandes proches (approx « tremblement »). */
    public boolean presenceJitterEnabled = true;

    // --- Phase 4b : effets de rendu lourds (pipeline GLSL + mixins FOV/caméra) ---
    /** Active le pipeline post-process GLSL (désat/wave/blur/noise). Coupé auto si Iris est présent. */
    public boolean presenceGlslEnabled = true;
    /** Active la vision tunnel + respiration (mixin getFov). */
    public boolean presenceFovEnabled = true;
    /** Active le screen shake caméra (mixin Camera). */
    public boolean presenceShakeEnabled = true;

    /** Désaturation max au contact (0-1). */
    public float presenceDesatMax = 0.9f;
    /** Distorsion wave max (0-1). */
    public float presenceWaveMax = 1.0f;
    /** Flou max (0-1). */
    public float presenceBlurMax = 1.0f;
    /** Ghosting max (0-1, passe avancée). 1.0 figerait l'image (mix=prev pur) : garder < 0.85. */
    public float presenceGhostMax = 0.7f;
    /** Bruit/grain max au contact (0-1). */
    public float presenceNoiseMax = 0.8f;

    /** Facteur de FOV mini en vision tunnel (0,78 ≈ -22 %). */
    public float presenceFovTunnelFactor = 0.78f;
    /** Amplitude du zoom de « respiration visuelle » (0-1). */
    public float presenceFovBreathAmplitude = 0.02f;
    /** Amplitude max du screen shake (degrés). */
    public float presenceShakeMaxDegrees = 0.5f;

    public static ItFollowsConfig get() {
        if (instance == null) {
            instance = load();
        }
        return instance;
    }

    private static Path configPath() {
        return FabricLoader.getInstance().getConfigDir().resolve(FILE_NAME);
    }

    private static ItFollowsConfig load() {
        Path path = configPath();
        if (Files.exists(path)) {
            try (Reader reader = Files.newBufferedReader(path)) {
                ItFollowsConfig loaded = GSON.fromJson(reader, ItFollowsConfig.class);
                if (loaded != null) {
                    loaded.save(); // réécrit pour combler d'éventuels champs manquants
                    return loaded;
                }
            } catch (IOException e) {
                System.err.println("[ItFollows] Impossible de lire la config, valeurs par défaut utilisées : " + e);
            }
        }
        ItFollowsConfig defaults = new ItFollowsConfig();
        defaults.save();
        return defaults;
    }

    public void save() {
        Path path = configPath();
        try {
            Files.createDirectories(path.getParent());
            try (Writer writer = Files.newBufferedWriter(path)) {
                GSON.toJson(this, writer);
            }
        } catch (IOException e) {
            System.err.println("[ItFollows] Impossible d'écrire la config : " + e);
        }
    }
}
