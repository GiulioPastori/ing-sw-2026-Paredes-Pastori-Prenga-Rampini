package it.polimi.it.network.message;

import it.polimi.it.enums.Era;
import it.polimi.it.enums.EventType;
import it.polimi.it.observer.BoardSnapshot;
import it.polimi.it.observer.CardSnapshot;
import it.polimi.it.observer.OfferTileSnapshot;
import it.polimi.it.observer.PlayerSnapshot;
import it.polimi.it.observer.TurnOrderTileSnapshot;
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

    public static Message join(String nickname, int numPlayers, String totemColour) {
        return new Message(JOIN, new JSONObject()
                .put("nickname",     nickname)
                .put("numPlayers",   numPlayers)
                .put("totemColour",  totemColour));
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
                                       BoardSnapshot board) {
        return new Message(ROUND_STARTED, new JSONObject()
                .put("round",     round)
                .put("turnOrder", playerListToJson(turnOrder))
                .put("board",     boardToJson(board)));
    }

    public static Message totemPlaced(PlayerSnapshot player, char tileLetter,
                                      BoardSnapshot board) {
        return new Message(TOTEM_PLACED, new JSONObject()
                .put("player",     playerToJson(player))
                .put("tileLetter", String.valueOf(tileLetter))
                .put("board",      boardToJson(board)));
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

    public static Message totemReturned(PlayerSnapshot player, int position,
                                        BoardSnapshot board) {
        return new Message(TOTEM_RETURNED, new JSONObject()
                .put("player",   playerToJson(player))
                .put("position", position)
                .put("board",    boardToJson(board)));
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
                .put("name",            p.name())
                .put("totemColour",     p.totemColour() != null ? p.totemColour().name() : "")
                .put("food",            p.food())
                .put("prestigePoints",  p.prestigePoints())
                .put("turnOrder",       p.turnOrder())
                .put("tribeCharacters", cardListToJson(p.tribeCharacters()))
                .put("tribeBuildings",  cardListToJson(p.tribeBuildings()));
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
                .put("placedTotem",    t.placedTotem() != null ? t.placedTotem().name() : "")
                .put("topCards",       cardListToJson(t.topCards()))
                .put("bottomCards",    cardListToJson(t.bottomCards()));
    }

    private static JSONArray tileListToJson(List<OfferTileSnapshot> list) {
        JSONArray arr = new JSONArray();
        list.forEach(t -> arr.put(tileToJson(t)));
        return arr;
    }

    private static JSONObject turnOrderTileToJson(TurnOrderTileSnapshot t) {
        if (t == null) return new JSONObject();
        JSONArray positions = new JSONArray();
        for (TurnOrderTileSnapshot.PositionSnap p : t.positions()) {
            positions.put(new JSONObject()
                    .put("position",    p.position())
                    .put("foodBonus",   p.foodBonus())
                    .put("isLast",      p.isLast())
                    .put("placedTotem", p.placedTotem() != null ? p.placedTotem().name() : ""));
        }
        return new JSONObject()
                .put("numPlayers", t.numPlayers())
                .put("positions",  positions);
    }

    private static JSONObject boardToJson(BoardSnapshot b) {
        return new JSONObject()
                .put("offerTiles",    tileListToJson(b.offerTiles()))
                .put("turnOrderTile", turnOrderTileToJson(b.turnOrderTile()))
                .put("tribeDeckSize", b.tribeDeckSize());
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
        if (c.details() != null) {
            CardSnapshot.CharacterDetails d = c.details();
            obj.put("details", new JSONObject()
                    .put("triggerIcon",      d.triggerIcon())
                    .put("starIcons",        d.starIcons())
                    .put("inventionIcon",    d.inventionIcon())
                    .put("buildingDiscount", d.buildingDiscount())
                    .put("builderPoints",    d.builderPoints()));
        }
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
            list.add(parsePlayer(arr.getJSONObject(i)));
        }
        return list;
    }

    /** Shared deserialiser for a player JSON object (used by playerList and playerObj). */
    private static PlayerSnapshot parsePlayer(JSONObject o) {
        List<CardSnapshot> chars = o.has("tribeCharacters")
                ? parseCardArray(o.getJSONArray("tribeCharacters"))
                : List.of();
        List<CardSnapshot> blds = o.has("tribeBuildings")
                ? parseCardArray(o.getJSONArray("tribeBuildings"))
                : List.of();
        return new PlayerSnapshot(
                o.getString("name"),
                it.polimi.it.enums.TotemColour.valueOf(
                        o.optString("totemColour", "RED")),
                o.getInt("food"),
                o.getInt("prestigePoints"),
                o.getInt("turnOrder"),
                chars,
                blds);
    }

    public List<OfferTileSnapshot> tileList(String key) {
        JSONArray arr = data.getJSONArray(key);
        List<OfferTileSnapshot> list = new ArrayList<>();
        for (int i = 0; i < arr.length(); i++) {
            list.add(parseTile(arr.getJSONObject(i)));
        }
        return list;
    }

    /** Shared OfferTileSnapshot deserialiser (used by tileList and boardObj). */
    private static OfferTileSnapshot parseTile(JSONObject o) {
        List<CardSnapshot> topCards    = parseCardArray(o.getJSONArray("topCards"));
        List<CardSnapshot> bottomCards = parseCardArray(o.getJSONArray("bottomCards"));
        String tc = o.optString("placedTotem", "");
        return new OfferTileSnapshot(
                o.getString("letter").charAt(0),
                o.getInt("topRowCards"),
                o.getInt("bottomRowCards"),
                o.getBoolean("isFoodOnly"),
                o.getBoolean("isFree"),
                tc.isEmpty() ? null : it.polimi.it.enums.TotemColour.valueOf(tc),
                topCards,
                bottomCards);
    }

    /** Shared deserialiser for a single card JSON object. */
    private static CardSnapshot parseCard(JSONObject o) {
        String ct = o.optString("characterType", "");
        CardSnapshot.CharacterDetails details = null;
        if (o.has("details")) {
            JSONObject d = o.getJSONObject("details");
            details = new CardSnapshot.CharacterDetails(
                    d.optBoolean("triggerIcon",     false),
                    d.optInt    ("starIcons",       0),
                    d.optString ("inventionIcon",   ""),
                    d.optInt    ("buildingDiscount", 0),
                    d.optInt    ("builderPoints",   0));
        }
        return new CardSnapshot(
                o.getInt("id"),
                Era.valueOf(o.getString("era")),
                CardSnapshot.Type.valueOf(o.getString("type")),
                ct.isEmpty() ? null : it.polimi.it.enums.CharacterType.valueOf(ct),
                o.getInt("foodCost"),
                o.getInt("finalPoints"),
                o.getString("effectDescription"),
                details);
    }

    /** Deserializza un JSONArray di carte in List<CardSnapshot>. */
    private static List<CardSnapshot> parseCardArray(JSONArray arr) {
        List<CardSnapshot> list = new ArrayList<>();
        for (int i = 0; i < arr.length(); i++) {
            list.add(parseCard(arr.getJSONObject(i)));
        }
        return list;
    }

    public List<CardSnapshot> cardList(String key) {
        return parseCardArray(data.getJSONArray(key));
    }

    /** Reads a single PlayerSnapshot from a nested object (not array). */
    public PlayerSnapshot playerObj(String key) {
        return parsePlayer(data.getJSONObject(key));
    }

    /** Reads a single CardSnapshot from a nested object (not array). */
    public CardSnapshot cardObj(String key) {
        return parseCard(data.getJSONObject(key));
    }

    /** Reads a TurnOrderTileSnapshot from a nested object. */
    public TurnOrderTileSnapshot turnOrderTileObj(String key) {
        if (!data.has(key)) return null;
        return parseTurnOrderTile(data.getJSONObject(key));
    }

    private static TurnOrderTileSnapshot parseTurnOrderTile(JSONObject o) {
        if (o == null || !o.has("numPlayers")) return null;
        int numPlayers = o.getInt("numPlayers");
        JSONArray arr = o.getJSONArray("positions");
        List<TurnOrderTileSnapshot.PositionSnap> snaps = new ArrayList<>();
        for (int i = 0; i < arr.length(); i++) {
            JSONObject p  = arr.getJSONObject(i);
            String     tc = p.optString("placedTotem", "");
            snaps.add(new TurnOrderTileSnapshot.PositionSnap(
                    p.getInt("position"),
                    p.getInt("foodBonus"),
                    p.getBoolean("isLast"),
                    tc.isEmpty() ? null : it.polimi.it.enums.TotemColour.valueOf(tc)));
        }
        return new TurnOrderTileSnapshot(numPlayers, snaps);
    }

    /** Reads a BoardSnapshot from a nested object. */
    public BoardSnapshot boardObj(String key) {
        JSONObject o = data.getJSONObject(key);
        JSONArray  tilesArr = o.getJSONArray("offerTiles");
        List<OfferTileSnapshot> tiles = new ArrayList<>();
        for (int i = 0; i < tilesArr.length(); i++) {
            tiles.add(parseTile(tilesArr.getJSONObject(i)));
        }
        TurnOrderTileSnapshot tot = parseTurnOrderTile(o.optJSONObject("turnOrderTile"));
        int deckSize = o.optInt("tribeDeckSize", 0);
        return new BoardSnapshot(tiles, tot, deckSize);
    }
}