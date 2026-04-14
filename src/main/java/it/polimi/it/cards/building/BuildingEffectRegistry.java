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
 *
 * === ERA I ===
 * 101: Each new complete set of 6 types → +5 food (not retroactive)
 * 102: Sustenance → -1 food per Gatherer
 * 103: Shamanic Ritual → immune to minimum penalty
 * 104: Turn order → +1 food landing on a food-bonus space
 * 105: Inventor pair with same icon → +3 food (not retroactive)
 * 106: Sustenance → -1 food per Artist
 *
 * === ERA II ===
 * 201: Shamanic Ritual → doubles majority reward
 * 202: Hunt → +1 food and +1 PP per Hunter
 * 203: End of game → double PP from Builder cards
 * 204: Cave Paintings → +1 food per Artist
 * 205: End of game → 6 PP per complete set of 6 types
 * 206: Sustenance → -1 food per Inventor
 * 207: Shamanic Ritual → +3 virtual star icons
 *
 * === ERA III ===
 * 301: End of game → 25 fixed PP  (carried by finalPoints in JSON; no scorer needed)
 * 302: Watchtower → extra post-round action
 * 303-308: End of game → PP per character of indicated type
 *          (placeholder scorers overridden by CardLoader at load time)
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
     * data read from JSON at runtime (buildings 303-308).
     *
     * Preserves all existing hooks and replaces only the EndGameScorer.
     * Must be called by CardLoader after parsing the relevant building entry.
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

        // ── ERA I ─────────────────────────────────────────────────────────────

        // 101: each new complete set of 6 distinct types → +5 food (not retroactive)
        REGISTRY.put(101, new BuildingEffect.Builder()
                .onAcquisition((player, acquired) -> {
                    int beforeSets = computeSetsExcludingOne(player, acquired.getType());
                    int afterSets  = computeCompleteSets(player);
                    int newSets    = afterSets - beforeSets;
                    if (newSets > 0) player.addFood(newSets * 5);
                })
                .build());

        // 102: Sustenance → -1 food per Gatherer
        REGISTRY.put(102, new BuildingEffect.Builder()
                .withSustenanceModifier(owner ->
                        owner.getTribe().countByType(CharacterType.GATHERER))
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

        // 104: totem repositioning → +1 food if landing on a food-bonus space (not last)
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
                    if (sameIcon >= 2 && sameIcon % 2 == 0) player.addFood(3);
                })
                .build());

        // 106: Sustenance → -1 food per Artist
        REGISTRY.put(106, new BuildingEffect.Builder()
                .withSustenanceModifier(owner ->
                        owner.getTribe().countByType(CharacterType.ARTIST))
                .build());

        // ── ERA II ────────────────────────────────────────────────────────────

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

        // 206: Sustenance → -1 food per Inventor
        REGISTRY.put(206, new BuildingEffect.Builder()
                .withSustenanceModifier(owner ->
                        owner.getTribe().countByType(CharacterType.INVENTOR))
                .build());

        // 207: Shamanic Ritual → +3 virtual star icons
        REGISTRY.put(207, new BuildingEffect.Builder()
                .withShamanicModifier(new ShamanicModifier() {
                    @Override
                    public int extraStarIcons(it.polimi.it.player.Player owner) {
                        return 3;
                    }
                })
                .build());

        // ── ERA III ───────────────────────────────────────────────────────────

        // 301: end of game → 25 fixed PP.
        // The 25 PP are already in Building.finalPoints from the JSON;
        // no additional EndGameScorer needed here (would double-count).
        REGISTRY.put(301, new BuildingEffect.Builder().build());

        // 302: Watchtower → extra post-round action (take 1 card from top row)
        REGISTRY.put(302, new BuildingEffect.Builder()
                .withPostRound()
                .build());

        // 303-308: end of game → PP per character of indicated type.
        // Placeholder scorers; overridden by CardLoader.overrideEndGameScorer()
        // using ppPerCharacter / targetCharacterType fields from cards.json.
        REGISTRY.put(303, new BuildingEffect.Builder().atEndGame(player -> 0).build());
        REGISTRY.put(304, new BuildingEffect.Builder().atEndGame(player -> 0).build());
        REGISTRY.put(305, new BuildingEffect.Builder().atEndGame(player -> 0).build());
        REGISTRY.put(306, new BuildingEffect.Builder().atEndGame(player -> 0).build());
        REGISTRY.put(307, new BuildingEffect.Builder().atEndGame(player -> 0).build());
        REGISTRY.put(308, new BuildingEffect.Builder().atEndGame(player -> 0).build());
    }

    /**
     * Returns the number of complete sets of 6 distinct character types.
     * A set requires all 6 types; count = minimum occurrences across all types.
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
     * Used by buildings 101 to determine how many new sets were completed
     * by the last acquisition, without persisting state between calls.
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
