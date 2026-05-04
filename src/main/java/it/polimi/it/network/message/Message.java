package it.polimi.it.network.message;

import it.polimi.it.enums.Era;
import it.polimi.it.enums.EventType;
import it.polimi.it.observer.CardSnapshot;
import it.polimi.it.observer.OfferTileSnapshot;
import it.polimi.it.observer.PlayerSnapshot;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Network message: a type string + a JSON payload.
 *
 * Serialised as a single JSON line:
 *   {"type":"PLACE_TOTEM","playerName":"Alice","tileLetter":"B"}
 *
 * One class replaces all record subtypes — no Jackson, no multiple files.
 * Uses org.json which is already in the project dependencies.
 */
public class Message {

    // ── Types ─────────────────────────────────────────────────────────────────
    // Client → Server
    public static final String JOIN        = "JOIN";
    public static final String PLACE_TOTEM = "PLACE_TOTEM";
    public static final String TAKE_CARD   = "TAKE_CARD";
    public static final String PASS        = "PASS";
    public static final String POST_ROUND  = "POST_ROUND";
    /** Heartbeat periodico inviato dal client ogni HEARTBEAT_INTERVAL_MS millisecondi. */
    public static final String HEARTBEAT   = "HEARTBEAT";

    // Server → Client
    public static final String LOBBY_UPDATE            = "LOBBY_UPDATE";
    public static final String ERROR                   = "ERROR";
    public static final String ROUND_STARTED           = "ROUND_STARTED";
    public static final String TOTEM_PLACED            = "TOTEM_PLACED";
    public static final String CARD_SLOT_READY         = "CARD_SLOT_READY";
    public static final String CARD_ACQUIRED           = "CARD_ACQUIRED";
    public static final String CARD_ACQUISITION_FAILED = "CARD_ACQUISITION_FAILED";
    public static final String TOTEM_RETURNED          = "TOTEM_RETURNED";
    public static final String POST_ROUND_STARTED      = "POST_ROUND_STARTED";
    public static final String EVENT_RESOLVED          = "EVENT_RESOLVED";
    public static final String ERA_CHANGED             = "ERA_CHANGED";
    public static final String ROUND_ENDED             = "ROUND_ENDED";
    public static final String GAME_ENDED              = "GAME_ENDED";
    public static final String PLAYER_DISCONNECTED     = "PLAYER_DISCONNECTED";
    // Mantenuti per retrocompatibilità RMI (vecchi client)
    public static final String PING = "PING";
    public static final String PONG = "PONG";

    // ── Fields ────────────────────────────────────────────────────────────────
    private final String     type;
    private final JSONObject data;

    private Message(String type, JSONObject data) {
        this.type = type;
        this.data = data;
    }

    public String     getType() { return type; }
    public JSONObject getData() { return data; }

    // ── Codec ─────────────────────────────────────────────────────────────────

    /** Serialises to a single JSON line (safe for BufferedWriter.newLine). */
    public String encode() {
        JSONObject obj = new JSONObject(data.toMap());
        obj.put("type", type);
        return obj.toString();
    }

    /** Deserialises from a single JSON line. */
    public static Message decode(String json) {
        JSONObject obj  = new JSONObject(json);
        String     type = obj.getString("type");
        obj.remove("type");
        return new Message(type, obj);
    }

    // ── Client → Server factories ─────────────────────────────────────────────

    public static Message join(String nickname, int numPlayers) {
        return new Message(JOIN, new JSONObject()
                .put("nickname", nickname)
                .put("numPlayers", numPlayers));
    }

    public static Message placeTotem(String playerName, char tileLetter) {
        return new Message(PLACE_TOTEM, new JSONObject()
                .put("playerName", playerName)
                .put("tileLetter", String.valueOf(tileLetter)));
    }

    public static Message takeCard(String playerName, int cardId, boolean fromTopRow) {
        return new Message(TAKE_CARD, new JSONObject()
                .put("playerName", playerName)
                .put("cardId", cardId)
                .put("fromTopRow", fromTopRow));
    }

    public static Message pass(String playerName) {
        return new Message(PASS, new JSONObject()
                .put("playerName", playerName));
    }

    public static Message postRound(String playerName, int cardId) {
        return new Message(POST_ROUND, new JSONObject()
                .put("playerName", playerName)
                .put("cardId", cardId));
    }

    public static Message ping()      { return new Message(PING,      new JSONObject()); }
    public static Message pong()      { return new Message(PONG,      new JSONObject()); }
    public static Message heartbeat() { return new Message(HEARTBEAT, new JSONObject()); }

    // ── Server → Client factories ─────────────────────────────────────────────

    public static Message error(String reason) {
        return new Message(ERROR, new JSONObject().put("reason", reason));
    }

    public static Message lobbyUpdate(List<String> players, int expected) {
        return new Message(LOBBY_UPDATE, new JSONObject()
                .put("connectedPlayers", new JSONArray(players))
                .put("expectedPlayers", expected));
    }

    public static Message playerDisconnected(String reason) {
        return new Message(PLAYER_DISCONNECTED, new JSONObject()
                .put("nickname", reason));
    }

    public static Message roundStarted(int round,
                                       List<PlayerSnapshot> turnOrder,
                                       List<OfferTileSnapshot> tiles) {
        return new Message(ROUND_STARTED, new JSONObject()
                .put("round", round)
                .put("turnOrder",     playerListToJson(turnOrder))
                .put("availableTiles", tileListToJson(tiles)));
    }

    public static Message totemPlaced(PlayerSnapshot player, char tileLetter,
                                      List<OfferTileSnapshot> remaining) {
        return new Message(TOTEM_PLACED, new JSONObject()
                .put("player",        playerToJson(player))
                .put("tileLetter",    String.valueOf(tileLetter))
                .put("remainingTiles", tileListToJson(remaining)));
    }

    public static Message cardSlotReady(PlayerSnapshot player, boolean isTopRow,
                                        List<CardSnapshot> cards,
                                        List<CardSnapshot> visibleEvents,
                                        int slotsRemaining) {
        return new Message(CARD_SLOT_READY, new JSONObject()
                .put("player",         playerToJson(player))
                .put("isTopRow",       isTopRow)
                .put("availableCards", cardListToJson(cards))
                .put("visibleEvents",  cardListToJson(visibleEvents))
                .put("slotsRemaining", slotsRemaining));
    }

    public static Message cardAcquired(PlayerSnapshot player, CardSnapshot card) {
        return new Message(CARD_ACQUIRED, new JSONObject()
                .put("player", playerToJson(player))
                .put("card",   cardToJson(card)));
    }

    public static Message cardAcquisitionFailed(PlayerSnapshot player, CardSnapshot card,
                                                int missingFood, boolean fromTopRow,
                                                List<CardSnapshot> alternatives) {
        return new Message(CARD_ACQUISITION_FAILED, new JSONObject()
                .put("player",       playerToJson(player))
                .put("card",         cardToJson(card))
                .put("missingFood",  missingFood)
                .put("fromTopRow",   fromTopRow)
                .put("alternatives", cardListToJson(alternatives)));
    }

    public static Message totemReturned(PlayerSnapshot player, int position) {
        return new Message(TOTEM_RETURNED, new JSONObject()
                .put("player",   playerToJson(player))
                .put("position", position));
    }

    public static Message postRoundStarted(int round, List<PlayerSnapshot> watchtower,
                                           List<CardSnapshot> cards) {
        return new Message(POST_ROUND_STARTED, new JSONObject()
                .put("round",             round)
                .put("watchtowerPlayers", playerListToJson(watchtower))
                .put("availableCards",    cardListToJson(cards)));
    }

    public static Message eventResolved(int eventId, Era era, EventType type) {
        return new Message(EVENT_RESOLVED, new JSONObject()
                .put("eventId",   eventId)
                .put("era",       era.name())
                .put("eventType", type.name()));
    }
    public static Message eraChanged(Era newEra) {
        return new Message(ERA_CHANGED, new JSONObject()
                .put("newEra", newEra.name()));
    }

    public static Message roundEnded(int round) {
        return new Message(ROUND_ENDED, new JSONObject().put("round", round));
    }

    public static Message gameEnded(List<PlayerSnapshot> winners,
                                    List<PlayerSnapshot> finalScores) {
        return new Message(GAME_ENDED, new JSONObject()
                .put("winners",     playerListToJson(winners))
                .put("finalScores", playerListToJson(finalScores)));
    }

    // ── JSON helpers ──────────────────────────────────────────────────────────

    private static JSONObject playerToJson(PlayerSnapshot p) {
        return new JSONObject()
                .put("name",           p.name())
                .put("totemColour",    p.totemColour() != null ? p.totemColour().name() : "")
                .put("food",           p.food())
                .put("prestigePoints", p.prestigePoints())
                .put("turnOrder",      p.turnOrder());
    }

    private static JSONArray playerListToJson(List<PlayerSnapshot> list) {
        JSONArray arr = new JSONArray();
        list.forEach(p -> arr.put(playerToJson(p)));
        return arr;
    }

    private static JSONObject tileToJson(OfferTileSnapshot t) {
        return new JSONObject()
                .put("letter",         String.valueOf(t.letter()))
                .put("topRowCards",    t.topRowCards())
                .put("bottomRowCards", t.bottomRowCards())
                .put("isFoodOnly",     t.isFoodOnly())
                .put("isFree",         t.isFree())
                .put("topCards",       cardListToJson(t.topCards()))
                .put("bottomCards",    cardListToJson(t.bottomCards()));
    }

    private static JSONArray tileListToJson(List<OfferTileSnapshot> list) {
        JSONArray arr = new JSONArray();
        list.forEach(t -> arr.put(tileToJson(t)));
        return arr;
    }

    private static JSONObject cardToJson(CardSnapshot c) {
        JSONObject obj = new JSONObject()
                .put("id",                c.id())
                .put("era",               c.era().name())
                .put("type",              c.type().name())
                .put("foodCost",          c.foodCost())
                .put("finalPoints",       c.finalPoints())
                .put("effectDescription", c.effectDescription());
        if (c.characterType() != null)
            obj.put("characterType", c.characterType().name());
        return obj;
    }

    private static JSONArray cardListToJson(List<CardSnapshot> list) {
        JSONArray arr = new JSONArray();
        list.forEach(c -> arr.put(cardToJson(c)));
        return arr;
    }

    // ── Data accessors (used by ClientController) ─────────────────────────────

    public String  str(String key)     { return data.getString(key); }
    public int     num(String key)     { return data.getInt(key); }
    public boolean bool(String key)    { return data.getBoolean(key); }
    public char    ch(String key)      { return data.getString(key).charAt(0); }

    public List<String> strList(String key) {
        JSONArray arr = data.getJSONArray(key);
        List<String> list = new ArrayList<>();
        for (int i = 0; i < arr.length(); i++) list.add(arr.getString(i));
        return list;
    }

    public List<PlayerSnapshot> playerList(String key) {
        JSONArray arr = data.getJSONArray(key);
        List<PlayerSnapshot> list = new ArrayList<>();
        for (int i = 0; i < arr.length(); i++) {
            JSONObject o = arr.getJSONObject(i);
            list.add(new PlayerSnapshot(
                    o.getString("name"),
                    it.polimi.it.enums.TotemColour.valueOf(
                            o.optString("totemColour", "RED")),
                    o.getInt("food"),
                    o.getInt("prestigePoints"),
                    o.getInt("turnOrder")));
        }
        return list;
    }

    public List<OfferTileSnapshot> tileList(String key) {
        JSONArray arr = data.getJSONArray(key);
        List<OfferTileSnapshot> list = new ArrayList<>();
        for (int i = 0; i < arr.length(); i++) {
            JSONObject o = arr.getJSONObject(i);
            List<CardSnapshot> topCards    = parseCardArray(o.getJSONArray("topCards"));
            List<CardSnapshot> bottomCards = parseCardArray(o.getJSONArray("bottomCards"));
            list.add(new OfferTileSnapshot(
                    o.getString("letter").charAt(0),
                    o.getInt("topRowCards"),
                    o.getInt("bottomRowCards"),
                    o.getBoolean("isFoodOnly"),
                    o.getBoolean("isFree"),
                    topCards,
                    bottomCards));
        }
        return list;
    }

    /** Deserializza un JSONArray di carte in List<CardSnapshot>. */
    private static List<CardSnapshot> parseCardArray(JSONArray arr) {
        List<CardSnapshot> list = new ArrayList<>();
        for (int i = 0; i < arr.length(); i++) {
            JSONObject o = arr.getJSONObject(i);
            String ct = o.optString("characterType", "");
            list.add(new CardSnapshot(
                    o.getInt("id"),
                    it.polimi.it.enums.Era.valueOf(o.getString("era")),
                    CardSnapshot.Type.valueOf(o.getString("type")),
                    ct.isEmpty() ? null : it.polimi.it.enums.CharacterType.valueOf(ct),
                    o.getInt("foodCost"),
                    o.getInt("finalPoints"),
                    o.getString("effectDescription")));
        }
        return list;
    }

    public List<CardSnapshot> cardList(String key) {
        JSONArray arr = data.getJSONArray(key);
        List<CardSnapshot> list = new ArrayList<>();
        for (int i = 0; i < arr.length(); i++) {
            JSONObject o = arr.getJSONObject(i);
            String ct = o.optString("characterType", "");
            list.add(new CardSnapshot(
                    o.getInt("id"),
                    Era.valueOf(o.getString("era")),
                    CardSnapshot.Type.valueOf(o.getString("type")),
                    ct.isEmpty() ? null :
                            it.polimi.it.enums.CharacterType.valueOf(ct),
                    o.getInt("foodCost"),
                    o.getInt("finalPoints"),
                    o.getString("effectDescription")));
        }
        return list;
    }

    public PlayerSnapshot player(String key) {
        return playerList(key + "Wrap") // reuse list helper
                .stream().findFirst().orElseThrow();
    }

    /** Reads a single PlayerSnapshot from a nested object (not array). */
    public PlayerSnapshot playerObj(String key) {
        JSONObject o = data.getJSONObject(key);
        return new PlayerSnapshot(
                o.getString("name"),
                it.polimi.it.enums.TotemColour.valueOf(
                        o.optString("totemColour", "RED")),
                o.getInt("food"),
                o.getInt("prestigePoints"),
                o.getInt("turnOrder"));
    }

    /** Reads a single CardSnapshot from a nested object (not array). */
    public CardSnapshot cardObj(String key) {
        JSONObject o = data.getJSONObject(key);
        String ct = o.optString("characterType", "");
        return new CardSnapshot(
                o.getInt("id"),
                Era.valueOf(o.getString("era")),
                CardSnapshot.Type.valueOf(o.getString("type")),
                ct.isEmpty() ? null :
                        it.polimi.it.enums.CharacterType.valueOf(ct),
                o.getInt("foodCost"),
                o.getInt("finalPoints"),
                o.getString("effectDescription"));
    }
}