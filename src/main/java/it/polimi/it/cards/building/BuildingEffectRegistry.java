package it.polimi.it.cards.building;

import it.polimi.it.cards.building.modifier.ShamanicModifier;
import it.polimi.it.cards.building.modifier.SustenanceModifier;
import it.polimi.it.enums.CharacterType;
import it.polimi.it.enums.EventType;

import java.util.HashMap;
import java.util.Map;

/**
 * Centralised registry of all building effects.
 *
 * Maps each buildingId to the corresponding BuildingEffect.
 * Every effect is built via BuildingEffect.Builder, eliminating
 * long sequences of null arguments.
 *
 * Contextual effects for Shamanic Ritual and Sustenance are expressed
 * via ShamanicModifier and SustenanceModifier instead of numeric ID
 * comparisons inside the event strategies.
 *
 * Buildings 206 and 302 have a dynamic EndGameScorer that depends on
 * data from the JSON (character type and PP per card). CardLoader must
 * call overrideEndGameScorer() after loading these buildings.
 *
 * Building IDs: 101-106 Era I, 201-207 Era II, 301-308 Era III.
 */
public final class BuildingEffectRegistry {

    private static final Map<Integer, BuildingEffect> REGISTRY = new HashMap<>();

    static { registerAll(); }

    private BuildingEffectRegistry() {}

    public static BuildingEffect get(int buildingId) {
        return REGISTRY.getOrDefault(buildingId, BuildingEffect.NONE);
    }

    /**
     * Overrides the EndGameScorer for buildings whose scoring depends on
     * data read from JSON at runtime (e.g. buildings 206 and 302).
     *
     * Preserves all existing hooks (event, turn, acquisition, shamanic,
     * sustenance, postRound) and replaces only the EndGameScorer.
     *
     * Must be called by CardLoader after parsing the relevant building entry,
     * before any Building object with this ID is used for scoring.
     *
     * @param buildingId the building ID to update
     * @param scorer     the EndGameScorer to set
     * @throws IllegalArgumentException if no effect is registered for the given ID
     */
    public static void overrideEndGameScorer(int buildingId, EndGameScorer scorer) {
        BuildingEffect existing = REGISTRY.get(buildingId);
        if (existing == null) {
            throw new IllegalArgumentException(
                    "No effect registered for building ID: " + buildingId);
        }
        BuildingEffect.Builder builder = new BuildingEffect.Builder();
        if (existing.hasEventHook())
            builder.onEvent(existing.getEventTrigger(), existing.getEventHook());
        if (existing.hasTurnHook())
            builder.onTurn(existing.getTurnHook());
        if (existing.hasAcquisitionHook())
            builder.onAcquisition(existing.getAcquisitionHook());
        if (existing.hasShamanicModifier())
            builder.withShamanicModifier(existing.getShamanicModifier());
        if (existing.hasSustenanceModifier())
            builder.withSustenanceModifier(existing.getSustenanceModifier());
        if (existing.hasPostRound())
            builder.withPostRound();
        builder.atEndGame(scorer);
        REGISTRY.put(buildingId, builder.build());
    }

    private static void registerAll() {

        // ── ERA I ────────────────────────────────────────────────────────────

        // 101: each new complete set of 6 distinct types → +5 food (not retroactive)
        REGISTRY.put(101, new BuildingEffect.Builder()
                .onAcquisition((player, acquired) -> {
                    int beforeSets = computeSetsExcludingOne(player, acquired.getType());
                    int afterSets  = computeCompleteSets(player);
                    int newSets    = afterSets - beforeSets;
                    if (newSets > 0) {
                        player.addFood(newSets * 5);
                    }
                })
                .build());

        // 102: Sustenance → -1 food per Artist/Inventor/Gatherer
        REGISTRY.put(102, new BuildingEffect.Builder()
                .withSustenanceModifier(owner ->
                        owner.getTribe().countByType(CharacterType.ARTIST)
                                + owner.getTribe().countByType(CharacterType.INVENTOR)
                                + owner.getTribe().countByType(CharacterType.GATHERER)
                )
                .build());

        // 103: Shamanic Ritual → immunity to minimum-icons penalty
        REGISTRY.put(103, new BuildingEffect.Builder()
                .withShamanicModifier(new ShamanicModifier() {
                    @Override
                    public boolean isImmuneToMinPenalty(it.polimi.it.player.Player owner) {
                        return true;
                    }
                })
                .build());

        // 104: totem repositioning → +1 food if landing on food-bonus space (not last)
        REGISTRY.put(104, new BuildingEffect.Builder()
                .onTurn((player, position) -> {
                    if (!position.isLastPosition() && position.getFoodBonus() > 0) {
                        player.addFood(1);
                    }
                })
                .build());

        // 105: inventor pair with same icon → +3 food (not retroactive)
        REGISTRY.put(105, new BuildingEffect.Builder()
                .onAcquisition((player, acquired) -> {
                    if (acquired.getType() != CharacterType.INVENTOR) return;
                    var inventor = (it.polimi.it.cards.character.Inventor) acquired;
                    long sameIcon = player.getTribe().getCharacters().stream()
                            .filter(c -> c.getType() == CharacterType.INVENTOR)
                            .filter(c -> ((it.polimi.it.cards.character.Inventor) c)
                                    .getInventionIcon().equals(inventor.getInventionIcon()))
                            .count();
                    // Fires every time the count of the same icon becomes even (new pair formed)
                    if (sameIcon >= 2 && sameIcon % 2 == 0) {
                        player.addFood(3);
                    }
                })
                .build());

        // 106: Shamanic Ritual → +3 virtual star icons
        REGISTRY.put(106, new BuildingEffect.Builder()
                .withShamanicModifier(new ShamanicModifier() {
                    @Override
                    public int extraStarIcons(it.polimi.it.player.Player owner) {
                        return 3;
                    }
                })
                .build());

        // ── ERA II ───────────────────────────────────────────────────────────

        // 201: Shamanic Ritual → doubles majority reward
        REGISTRY.put(201, new BuildingEffect.Builder()
                .withShamanicModifier(new ShamanicModifier() {
                    @Override
                    public boolean doublesMaxReward(it.polimi.it.player.Player owner) {
                        return true;
                    }
                })
                .build());

        // 202: Hunt → +1 food and +1 PP per Hunter
        REGISTRY.put(202, new BuildingEffect.Builder()
                .onEvent(EventType.HUNT, (player, eventType) -> {
                    int hunters = player.getTribe().countByType(CharacterType.HUNTER);
                    player.addFood(hunters);
                    player.addPrestigePoints(hunters);
                })
                .build());

        // 203: end of game → double PP from Builder cards
        REGISTRY.put(203, new BuildingEffect.Builder()
                .atEndGame(player -> player.getTribe().computeBuilderPoints())
                .build());

        // 204: Cave Paintings → +1 food per Artist
        REGISTRY.put(204, new BuildingEffect.Builder()
                .onEvent(EventType.CAVE_PAINTINGS, (player, eventType) -> {
                    int artists = player.getTribe().countByType(CharacterType.ARTIST);
                    player.addFood(artists);
                })
                .build());

        // 205: end of game → 6 PP per complete set of 6 distinct types
        REGISTRY.put(205, new BuildingEffect.Builder()
                .atEndGame(player -> computeCompleteSets(player) * 6)
                .build());

        // 206: end of game → PP per character of indicated type
        // Placeholder: overridden by CardLoader.overrideEndGameScorer() at load time.
        REGISTRY.put(206, new BuildingEffect.Builder()
                .atEndGame(player -> 0)
                .build());

        // 207: Watchtower → extra post-round action
        REGISTRY.put(207, new BuildingEffect.Builder()
                .withPostRound()
                .build());

        // ── ERA III ──────────────────────────────────────────────────────────

        // 301: end of game → 25 fixed PP
        REGISTRY.put(301, new BuildingEffect.Builder()
                .atEndGame(player -> 25)
                .build());

        // 302: end of game → PP per character of indicated type
        // Placeholder: overridden by CardLoader.overrideEndGameScorer() at load time.
        REGISTRY.put(302, new BuildingEffect.Builder()
                .atEndGame(player -> 0)
                .build());

        // 303: end of game → 6 PP per complete set of 6 distinct types
        REGISTRY.put(303, new BuildingEffect.Builder()
                .atEndGame(player -> computeCompleteSets(player) * 6)
                .build());

        // 304: Shamanic Ritual → doubles majority reward
        REGISTRY.put(304, new BuildingEffect.Builder()
                .withShamanicModifier(new ShamanicModifier() {
                    @Override
                    public boolean doublesMaxReward(it.polimi.it.player.Player owner) {
                        return true;
                    }
                })
                .build());

        // 305: each new complete set of 6 distinct types → +5 food (not retroactive)
        REGISTRY.put(305, new BuildingEffect.Builder()
                .onAcquisition((player, acquired) -> {
                    int beforeSets = computeSetsExcludingOne(player, acquired.getType());
                    int afterSets  = computeCompleteSets(player);
                    int newSets    = afterSets - beforeSets;
                    if (newSets > 0) {
                        player.addFood(newSets * 5);
                    }
                })
                .build());

        // 306: Hunt → +1 food and +1 PP per Hunter
        REGISTRY.put(306, new BuildingEffect.Builder()
                .onEvent(EventType.HUNT, (player, eventType) -> {
                    int hunters = player.getTribe().countByType(CharacterType.HUNTER);
                    player.addFood(hunters);
                    player.addPrestigePoints(hunters);
                })
                .build());

        // 307: Sustenance → -1 food per Artist/Inventor/Gatherer
        REGISTRY.put(307, new BuildingEffect.Builder()
                .withSustenanceModifier(owner ->
                        owner.getTribe().countByType(CharacterType.ARTIST)
                                + owner.getTribe().countByType(CharacterType.INVENTOR)
                                + owner.getTribe().countByType(CharacterType.GATHERER)
                )
                .build());

        // 308: Shamanic Ritual → +3 virtual star icons
        REGISTRY.put(308, new BuildingEffect.Builder()
                .withShamanicModifier(new ShamanicModifier() {
                    @Override
                    public int extraStarIcons(it.polimi.it.player.Player owner) {
                        return 3;
                    }
                })
                .build());
    }

    /**
     * Returns the number of complete sets of 6 distinct character types in the tribe.
     * A set is complete when all 6 types are present; the count equals
     * the minimum occurrences across all types.
     */
    static int computeCompleteSets(it.polimi.it.player.Player player) {
        int[] counts = new int[CharacterType.values().length];
        for (var c : player.getTribe().getCharacters()) {
            counts[c.getType().ordinal()]++;
        }
        int min = Integer.MAX_VALUE;
        for (int count : counts) {
            if (count == 0) return 0;
            min = Math.min(min, count);
        }
        return min == Integer.MAX_VALUE ? 0 : min;
    }

    /**
     * Returns the number of complete sets if one card of the given type were removed.
     * Used by buildings 101/305 to determine how many new sets were completed
     * by the last acquisition, without needing to persist state between calls.
     */
    private static int computeSetsExcludingOne(it.polimi.it.player.Player player,
                                               CharacterType excludedType) {
        int[] counts = new int[CharacterType.values().length];
        for (var c : player.getTribe().getCharacters()) {
            counts[c.getType().ordinal()]++;
        }
        counts[excludedType.ordinal()] = Math.max(0, counts[excludedType.ordinal()] - 1);
        int min = Integer.MAX_VALUE;
        for (int count : counts) {
            if (count == 0) return 0;
            min = Math.min(min, count);
        }
        return min == Integer.MAX_VALUE ? 0 : min;
    }
}