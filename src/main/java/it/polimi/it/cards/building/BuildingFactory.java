package it.polimi.it.cards.building;

import it.polimi.it.enums.Era;

/**
 * Factory for Building cards.
 *
 * Each building is constructed by passing only:
 *   - global numeric ID (101-308)
 *   - era
 *   - food cost, final points, effect description
 *
 * All actual effects are handled entirely by BuildingEffectRegistry —
 * no separate Strategy classes are needed.
 *
 * Buildings 206 and 302 have a dynamic EndGameScorer that depends on
 * JSON data: CardLoader must call BuildingEffectRegistry.overrideEndGameScorer()
 * after loading these buildings.
 */
public class BuildingFactory {

    public static Building create(int id, Era era) {
        return switch (era) {
            case ERA_I   -> createEra1(id);
            case ERA_II  -> createEra2(id);
            case ERA_III -> createEra3(id);
        };
    }

    // ── ERA I ────────────────────────────────────────────────────────────────

    private static Building createEra1(int id) {
        return switch (id) {
            // 101: each new complete set of 6 distinct types → +5 food (not retroactive)
            case 101 -> new Building(101, Era.ERA_I, 4, 2,
                    "+5 food per new complete set of 6 distinct types");

            // 102: Sustenance → -1 food per Artist / Inventor / Gatherer
            case 102 -> new Building(102, Era.ERA_I, 2, 2,
                    "Sustenance: -1 food per Artist, Inventor or Gatherer");

            // 103: Shamanic Ritual → immunity to the minimum-icons penalty
            case 103 -> new Building(103, Era.ERA_I, 4, 3,
                    "Shamanic Ritual: immune to minimum-icons penalty");

            // 104: totem repositioning → +1 food when landing on a food-bonus space (not last)
            case 104 -> new Building(104, Era.ERA_I, 2, 2,
                    "Totem repositioning: +1 food when landing on a food-bonus space");

            // 105: acquiring an Inventor with the same icon as an existing one → +3 food per new pair
            case 105 -> new Building(105, Era.ERA_I, 3, 2,
                    "Inventor pair with matching icon: +3 food");

            // 106: Shamanic Ritual → +3 virtual star icons
            case 106 -> new Building(106, Era.ERA_I, 3, 2,
                    "Shamanic Ritual: +3 virtual star icons");

            default -> throw new IllegalArgumentException("Invalid Era I building ID: " + id);
        };
    }

    // ── ERA II ───────────────────────────────────────────────────────────────

    private static Building createEra2(int id) {
        return switch (id) {
            // 201: Shamanic Ritual → doubles the majority reward
            case 201 -> new Building(201, Era.ERA_II, 5, 4,
                    "Shamanic Ritual: doubles the majority reward");

            // 202: Hunt → +1 food and +1 PP per Hunter
            case 202 -> new Building(202, Era.ERA_II, 3, 2,
                    "Hunt: +1 food and +1 PP per Hunter");

            // 203: end of game → doubles PP from Builder cards
            case 203 -> new Building(203, Era.ERA_II, 4, 3,
                    "End of game: doubles PP from Builder cards");

            // 204: Cave Paintings → +1 food per Artist
            case 204 -> new Building(204, Era.ERA_II, 3, 1,
                    "Cave Paintings: +1 food per Artist");

            // 205: end of game → 6 PP per complete set of 6 distinct types
            case 205 -> new Building(205, Era.ERA_II, 4, 2,
                    "End of game: 6 PP per complete set of 6 distinct types");

            // 206: end of game → PP per character of the indicated type (dynamic scorer)
            case 206 -> new Building(206, Era.ERA_II, 4, 2,
                    "End of game: PP per character of the indicated type");

            // 207: Watchtower → extra post-round action
            case 207 -> new Building(207, Era.ERA_II, 4, 2,
                    "Watchtower: extra post-round action");

            default -> throw new IllegalArgumentException("Invalid Era II building ID: " + id);
        };
    }

    // ── ERA III ──────────────────────────────────────────────────────────────

    private static Building createEra3(int id) {
        return switch (id) {
            // 301: end of game → 25 fixed PP
            case 301 -> new Building(301, Era.ERA_III, 6, 25,
                    "End of game: 25 fixed PP");

            // 302: end of game → PP per character of the indicated type (dynamic scorer)
            case 302 -> new Building(302, Era.ERA_III, 4, 0,
                    "End of game: PP per character of the indicated type");

            // 303: end of game → 6 PP per complete set of 6 distinct types
            case 303 -> new Building(303, Era.ERA_III, 5, 0,
                    "End of game: 6 PP per complete set of 6 distinct types");

            // 304: Shamanic Ritual → doubles the majority reward
            case 304 -> new Building(304, Era.ERA_III, 5, 4,
                    "Shamanic Ritual: doubles the majority reward");

            // 305: each new complete set of 6 distinct types → +5 food (not retroactive)
            case 305 -> new Building(305, Era.ERA_III, 4, 0,
                    "+5 food per new complete set of 6 distinct types");

            // 306: Hunt → +1 food and +1 PP per Hunter
            case 306 -> new Building(306, Era.ERA_III, 4, 0,
                    "Hunt: +1 food and +1 PP per Hunter");

            // 307: Sustenance → -1 food per Artist / Inventor / Gatherer
            case 307 -> new Building(307, Era.ERA_III, 4, 0,
                    "Sustenance: -1 food per Artist, Inventor or Gatherer");

            // 308: Shamanic Ritual → +3 virtual star icons
            case 308 -> new Building(308, Era.ERA_III, 4, 0,
                    "Shamanic Ritual: +3 virtual star icons");

            default -> throw new IllegalArgumentException("Invalid Era III building ID: " + id);
        };
    }

    private BuildingFactory() {}
}