
package it.polimi.it;

import it.polimi.it.cards.building.Building;
import it.polimi.it.cards.character.*;
import it.polimi.it.cards.events.*;
import it.polimi.it.enums.CharacterType;
import it.polimi.it.enums.Era;
import it.polimi.it.enums.EventType;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Loads all game cards from a JSON file.
 *
 * Expected root structure:
 * <pre>
 * {
 *   "characters": [ ... ],
 *   "events":     [ ... ],
 *   "buildings":  [ ... ]
 * }
 * </pre>
 *
 * Buildings 206 and 302 have a dynamic EndGameScorer that depends on
 * "ppPerCharacter" and "targetCharacterType" fields in the JSON.
 * After parsing, CardLoader calls BuildingEffectRegistry.overrideEndGameScorer()
 * so the registry reflects the correct values.
 *
 * See {@code src/main/resources/cards.json} for the full format.
 */
public class CardLoader {

    /**
     * Loads and returns all cards from the given JSON file.
     *
     * @param jsonPath path to the JSON file
     * @return all cards (characters, events, buildings)
     * @throws IOException if the file cannot be read
     */
    public static List<Card> loadAll(Path jsonPath) throws IOException {
        String content = Files.readString(jsonPath);
        JSONObject root = new JSONObject(content);

        List<Card> all = new ArrayList<>();
        all.addAll(parseCharacters(root.getJSONArray("characters")));
        all.addAll(parseEvents(root.getJSONArray("events")));
        all.addAll(parseBuildings(root.getJSONArray("buildings")));
        return all;
    }

    // ── Character parsing ─────────────────────────────────────────────────────

    private static List<Character> parseCharacters(JSONArray arr) {
        List<Character> list = new ArrayList<>();
        for (int i = 0; i < arr.length(); i++) {
            list.add(parseCharacter(arr.getJSONObject(i)));
        }
        return list;
    }

    /**
     * Expected format for each character entry:
     * <pre>
     * {
     *   "id":   int,
     *   "era":  "ERA_I" | "ERA_II" | "ERA_III",
     *   "type": "HUNTER" | "GATHERER" | "SHAMAN" | "BUILDER" | "INVENTOR" | "ARTIST",
     *
     *   // HUNTER only:
     *   "hasTriggerIcon": true | false,
     *
     *   // SHAMAN only:
     *   "starIcons": 1 | 2 | 3,
     *
     *   // BUILDER only:
     *   "buildingDiscount": int,
     *   "endGamePoints":    int,
     *
     *   // INVENTOR only:
     *   "inventionIcon": "arrow" | "fire" | ...
     * }
     * </pre>
     */
    private static Character parseCharacter(JSONObject obj) {
        int    id   = obj.getInt("id");
        Era    era  = Era.valueOf(obj.getString("era"));
        String type = obj.getString("type");

        return switch (type) {
            case "HUNTER"   -> new Hunter(id, era, obj.getBoolean("hasTriggerIcon"));
            case "GATHERER" -> new Gatherer(id, era);
            case "SHAMAN"   -> new Shaman(id, era, obj.getInt("starIcons"));
            case "BUILDER"  -> new Builder(id, era,
                    obj.getInt("buildingDiscount"),
                    obj.getInt("endGamePoints"));
            case "INVENTOR" -> new Inventor(id, era, obj.getString("inventionIcon"));
            case "ARTIST"   -> new Artist(id, era);
            default -> throw new IllegalArgumentException("Unknown character type: " + type);
        };
    }

    // ── Event parsing ─────────────────────────────────────────────────────────

    private static List<Event> parseEvents(JSONArray arr) {
        List<Event> list = new ArrayList<>();
        for (int i = 0; i < arr.length(); i++) {
            list.add(parseEvent(arr.getJSONObject(i)));
        }
        return list;
    }

    /**
     * Expected format for each event entry:
     * <pre>
     * {
     *   "id":      int,
     *   "era":     "ERA_I" | "ERA_II" | "ERA_III",
     *   "type":    "SUSTENANCE" | "HUNT" | "SHAMANIC_RITUAL" | "CAVE_PAINTINGS",
     *   "isFinal": true | false,
     *
     *   // SUSTENANCE:      "penaltyPerUnfedCharacter": int
     *   // HUNT:            "prestigePerHunter": int
     *   // SHAMANIC_RITUAL: "rewardForMost": int, "penaltyForFewest": int
     *   // CAVE_PAINTINGS:  "minArtistsForBonus": int, "penaltyPoints": int,
     *   //                  "prestigePerArtist": int
     * }
     * </pre>
     */
    private static Event parseEvent(JSONObject obj) {
        int       id      = obj.getInt("id");
        Era       era     = Era.valueOf(obj.optString("era", "ERA_I"));
        EventType type    = EventType.valueOf(obj.getString("type"));
        boolean   isFinal = obj.optBoolean("isFinal", false);
        int       value   = obj.optInt("value", 0);

        EventStrategy strategy = parseStrategy(type, obj);

        if (isFinal) return new FinalEvent(id, type, value, strategy);
        return new Event(id, era, type, value, strategy);
    }

    private static EventStrategy parseStrategy(EventType type, JSONObject obj) {
        return switch (type) {
            case SUSTENANCE ->
                    new Sustenance(obj.getInt("penaltyPerUnfedCharacter"));
            case HUNT ->
                    new Hunt(obj.getInt("prestigePerHunter"));
            case SHAMANIC_RITUAL ->
                    new ShamanicRitual(obj.getInt("rewardForMost"), obj.getInt("penaltyForFewest"));
            case CAVE_PAINTINGS ->
                    new CavePaintings(obj.getInt("minArtistsForBonus"),
                            obj.getInt("penaltyPoints"),
                            obj.getInt("prestigePerArtist"));
        };
    }

    // ── Building parsing ──────────────────────────────────────────────────────

    private static List<Building> parseBuildings(JSONArray arr) {
        List<Building> list = new ArrayList<>();
        for (int i = 0; i < arr.length(); i++) {
            list.add(parseBuilding(arr.getJSONObject(i)));
        }
        return list;
    }

    /**
     * Expected format for each building entry:
     * <pre>
     * {
     *   "id":                int,
     *   "era":               "ERA_I" | "ERA_II" | "ERA_III",
     *   "foodCost":          int,
     *   "finalPoints":       int,
     *   "effectDescription": string,
     *
     *   // Only for buildings 206 and 302:
     *   "ppPerCharacter":       int,
     *   "targetCharacterType":  "HUNTER" | "SHAMAN" | "ARTIST" | ...
     * }
     * </pre>
     */
    private static Building parseBuilding(JSONObject obj) {
        int    id  = obj.getInt("id");
        Era    era = Era.valueOf(obj.getString("era"));
        int    foodCost     = obj.getInt("foodCost");
        int    finalPoints  = obj.getInt("finalPoints");
        String effectDesc   = obj.optString("effectDescription", "");

        // Override the EndGameScorer for buildings with a per-character-type bonus
        if (obj.has("ppPerCharacter") && obj.has("targetCharacterType")) {
            int           ppPerCharacter = obj.getInt("ppPerCharacter");
            CharacterType targetType     = CharacterType.valueOf(
                    obj.getString("targetCharacterType"));
            BuildingEffectRegistry.overrideEndGameScorer(id,
                    player -> player.getTribe().countByType(targetType) * ppPerCharacter);
        }

        return new Building(id, era, foodCost, finalPoints, effectDesc);
    }
}
