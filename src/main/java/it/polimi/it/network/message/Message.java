package it.polimi.it.network.message;

import it.polimi.it.enums.Era;
import it.polimi.it.enums.EventType;
import it.polimi.it.network.server.persistence.LeaderboardEntry;
import it.polimi.it.observer.BoardSnapshot;
import it.polimi.it.observer.CardSnapshot;
import it.polimi.it.observer.FinalScoreBreakdown;
import it.polimi.it.observer.OfferTileSnapshot;
import it.polimi.it.observer.PlayerSnapshot;
import it.polimi.it.observer.PlayerResourceDelta;
import it.polimi.it.observer.TurnOrderTileSnapshot;
import org.json.JSONArray;
import org.json.JSONObject;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;


/**
 * Network message: a type string + a JSON payload.
 * Serialised as a single JSON line:
 *   {"type":"PLACE_TOTEM","playerName":"Alice","tileLetter":"B"}
 * One class replaces all record subtypes — no Jackson, no multiple files.
 * Uses org.json which is already in the project dependencies.
 */
public class Message {

    // Types
    // Client → Server
    public static final String JOIN        = "JOIN";
    public static final String PLACE_TOTEM = "PLACE_TOTEM";
    public static final String TAKE_CARD   = "TAKE_CARD";
    public static final String PASS        = "PASS";
    public static final String POST_ROUND  = "POST_ROUND";

    /** periodic heartbeat  */
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
    public static final String PLAYER_RECONNECTED      = "PLAYER_RECONNECTED";
    public static final String LEADERBOARD             = "LEADERBOARD";


    public static final String PING = "PING";
    public static final String PONG = "PONG";

    // Multiple sessions — session discovery
    /** Client → Server: asks for the list of open sessions. */
    public static final String LIST_SESSIONS = "LIST_SESSIONS";
    /** Server → Client: reply with the list of open sessions. */
    public static final String SESSION_LIST  = "SESSION_LIST";

    //Fields
    private final String     type;
    private final JSONObject data;

    private Message(String type, JSONObject data) {
        this.type = type;
        this.data = data;
    }

    public String     getType() { return type; }
    public JSONObject getData() { return data; }

    //  Codec

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

    // Client → Server factories


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

    public static Message pong()      { return new Message(PONG,      new JSONObject()); }
    public static Message heartbeat() { return new Message(HEARTBEAT, new JSONObject()); }

    // Server → Client factories

    public static Message error(String reason) {
        return new Message(ERROR, new JSONObject().put("reason", reason));
    }

    public static Message lobbyUpdate(List<String> players, int expected) {
        return new Message(LOBBY_UPDATE, new JSONObject()
                .put("connectedPlayers", new JSONArray(players))
                .put("expectedPlayers", expected));
    }

    public static Message playerDisconnected(String nickname) {
        return new Message(PLAYER_DISCONNECTED, new JSONObject()
                .put("nickname", nickname));
    }

    public static Message playerReconnected(String nickname) {
        return new Message(PLAYER_RECONNECTED, new JSONObject()
                .put("nickname", nickname));
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

    public static Message eventResolved(int eventId, Era era, EventType type,
                                        List<PlayerResourceDelta> deltas) {
        return new Message(EVENT_RESOLVED, new JSONObject()
                .put("eventId",   eventId)
                .put("era",       era.name())
                .put("eventType", type.name())
                .put("deltas",    playerResourceDeltasToJson(deltas)));
    }
    public static Message eraChanged(Era newEra) {
        return new Message(ERA_CHANGED, new JSONObject()
                .put("newEra", newEra.name()));
    }

    public static Message roundEnded(int round) {
        return new Message(ROUND_ENDED, new JSONObject().put("round", round));
    }

    public static Message gameEnded(List<PlayerSnapshot> winners,
                                    List<PlayerSnapshot> finalScores,
                                    List<FinalScoreBreakdown> breakdowns) {
        return new Message(GAME_ENDED, new JSONObject()
                .put("winners",     playerListToJson(winners))
                .put("finalScores", playerListToJson(finalScores))
                .put("scoreBreakdowns", finalScoreBreakdownsToJson(breakdowns)));
    }

    /** Builds a personalised  message for one player
     * @param recipient     nickname of the player this message is addressed to
     * @param myScore       final score the recipient obtained in this game
     * @param myRank        1-based rank of the recipient among all games of
     *                      the same size (use {@code -1} if unknown)
     * @param totalEntries  total number of recorded scores for that size
     * @param numPlayers    number of players the game had
     * @param leaderboard   full leaderboard for the given game size
     */
    public static Message leaderboard(String recipient,
                                      int myScore, int myRank, int totalEntries,
                                      int numPlayers,
                                      List<LeaderboardEntry> leaderboard) {
        return new Message(LEADERBOARD, new JSONObject()
                .put("recipient",     recipient)
                .put("myScore",       myScore)
                .put("myRank",        myRank)
                .put("totalEntries",  totalEntries)
                .put("numPlayers",    numPlayers)
                .put("entries",       leaderboardEntriesToJson(leaderboard)));
    }

    // JSON helpers

    private static final DateTimeFormatter ISO_LOCAL_DT = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private static JSONObject leaderboardEntryToJson(LeaderboardEntry e) {
        return new JSONObject()
                .put("rank",       e.rank())
                .put("nickname",   e.nickname())
                .put("finalScore", e.finalScore())
                .put("numPlayers", e.numPlayers())
                .put("playedAt",   e.playedAt() == null ? "" : e.playedAt().format(ISO_LOCAL_DT));
    }

    private static JSONArray leaderboardEntriesToJson(List<LeaderboardEntry> list) {
        JSONArray arr = new JSONArray();
        if (list != null) list.forEach(e -> arr.put(leaderboardEntryToJson(e)));
        return arr;
    }

    // JSON helpers

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
        return obj;
    }

    private static JSONArray cardListToJson(List<CardSnapshot> list) {
        JSONArray arr = new JSONArray();
        list.forEach(c -> arr.put(cardToJson(c)));
        return arr;
    }

    private static JSONObject playerResourceDeltaToJson(PlayerResourceDelta d) {
        return new JSONObject()
                .put("playerName",      d.playerName())
                .put("foodBefore",      d.foodBefore())
                .put("foodAfter",       d.foodAfter())
                .put("prestigeBefore",  d.prestigeBefore())
                .put("prestigeAfter",   d.prestigeAfter());
    }

    private static JSONArray playerResourceDeltasToJson(List<PlayerResourceDelta> list) {
        JSONArray arr = new JSONArray();
        if (list != null) list.forEach(d -> arr.put(playerResourceDeltaToJson(d)));
        return arr;
    }

    private static JSONObject finalScoreBreakdownToJson(FinalScoreBreakdown b) {
        return new JSONObject()
                .put("playerName",      b.playerName())
                .put("inGamePrestige",  b.inGamePrestige())
                .put("builderPoints",   b.builderPoints())
                .put("inventorPoints",  b.inventorPoints())
                .put("artistPoints",    b.artistPoints())
                .put("buildingPoints",  b.buildingPoints())
                .put("totalPrestige",   b.totalPrestige())
                .put("food",            b.food());
    }

    private static JSONArray finalScoreBreakdownsToJson(List<FinalScoreBreakdown> list) {
        JSONArray arr = new JSONArray();
        if (list != null) list.forEach(b -> arr.put(finalScoreBreakdownToJson(b)));
        return arr;
    }

    // Data accessors (used by ClientController)

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

    /** Deserializes a JSONArray of cards into a List<CardSnapshot>. */
    private static List<CardSnapshot> parseCardArray(JSONArray arr) {
        List<CardSnapshot> list = new ArrayList<>();

        for (int i = 0; i < arr.length(); i++) {
            JSONObject o = arr.getJSONObject(i);

            String ct = o.optString("characterType", "");

            list.add(new CardSnapshot(
                    o.getInt("id"),
                    it.polimi.it.enums.Era.valueOf(o.getString("era")),
                    CardSnapshot.Type.valueOf(o.getString("type")),
                    ct.isEmpty()
                            ? null
                            : it.polimi.it.enums.CharacterType.valueOf(ct),
                    o.getInt("foodCost"),
                    o.getInt("finalPoints"),
                    o.getString("effectDescription"),
                    null
            ));
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
                    ct.isEmpty()
                            ? null
                            : it.polimi.it.enums.CharacterType.valueOf(ct),
                    o.getInt("foodCost"),
                    o.getInt("finalPoints"),
                    o.getString("effectDescription"),
                    null
            ));
        }

        return list;
    }

    public List<LeaderboardEntry> leaderboardList(String key) {
        JSONArray arr = data.getJSONArray(key);
        List<LeaderboardEntry> list = new ArrayList<>();
        for (int i = 0; i < arr.length(); i++) {
            JSONObject o = arr.getJSONObject(i);
            String     ts = o.optString("playedAt", "");
            LocalDateTime playedAt = ts.isBlank()
                    ? null
                    : LocalDateTime.parse(ts, ISO_LOCAL_DT);
            list.add(new LeaderboardEntry(
                    o.getInt("rank"),
                    o.getString("nickname"),
                    o.getInt("finalScore"),
                    o.getInt("numPlayers"),
                    playedAt));
        }
        return list;
    }

    public List<PlayerResourceDelta> playerResourceDeltaList(String key) {
        if (!data.has(key)) return List.of();
        JSONArray arr = data.getJSONArray(key);
        List<PlayerResourceDelta> list = new ArrayList<>();
        for (int i = 0; i < arr.length(); i++) {
            JSONObject o = arr.getJSONObject(i);
            list.add(new PlayerResourceDelta(
                    o.getString("playerName"),
                    o.getInt("foodBefore"),
                    o.getInt("foodAfter"),
                    o.getInt("prestigeBefore"),
                    o.getInt("prestigeAfter")));
        }
        return list;
    }

    public List<FinalScoreBreakdown> finalScoreBreakdownList(String key) {
        if (!data.has(key)) return List.of();
        JSONArray arr = data.getJSONArray(key);
        List<FinalScoreBreakdown> list = new ArrayList<>();
        for (int i = 0; i < arr.length(); i++) {
            JSONObject o = arr.getJSONObject(i);
            list.add(new FinalScoreBreakdown(
                    o.getString("playerName"),
                    o.getInt("inGamePrestige"),
                    o.getInt("builderPoints"),
                    o.getInt("inventorPoints"),
                    o.getInt("artistPoints"),
                    o.getInt("buildingPoints"),
                    o.getInt("totalPrestige"),
                    o.getInt("food")));
        }
        return list;
    }

    /** Reads a single PlayerSnapshot from a nested object (not array). */
    public PlayerSnapshot playerObj(String key) {
        return parsePlayer(data.getJSONObject(key));
    }

    /** Reads a single CardSnapshot from a nested object (not array). */
    public CardSnapshot cardObj(String key) {
        JSONObject o = data.getJSONObject(key);

        String ct = o.optString("characterType", "");

        return new CardSnapshot(
                o.getInt("id"),
                Era.valueOf(o.getString("era")),
                CardSnapshot.Type.valueOf(o.getString("type")),
                ct.isEmpty()
                        ? null
                        : it.polimi.it.enums.CharacterType.valueOf(ct),
                o.getInt("foodCost"),
                o.getInt("finalPoints"),
                o.getString("effectDescription"),
                null
        );
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
// Multiple sessions: session discovery

    /**
     * Lightweight description of an open session, included in the SESSION_LIST reply.
     */
    public record SessionInfo(
            String       sessionId,
            int          expectedPlayers,
            int          currentPlayers,
            java.util.List<String> players) {}

    /**
     * Client → Server: asks for the list of open sessions.
     * @param numPlayersFilter if &gt; 0, filter by number of players; 0 = all
     */
    public static Message listSessions(int numPlayersFilter) {
        return new Message(LIST_SESSIONS, new JSONObject()
                .put("numPlayersFilter", numPlayersFilter));
    }

    /**
     * Server → Client: list of open sessions.
     */
    public static Message sessionList(List<SessionInfo> sessions) {
        JSONArray arr = new JSONArray();
        for (SessionInfo s : sessions) {
            arr.put(new JSONObject()
                    .put("sessionId",       s.sessionId())
                    .put("expectedPlayers", s.expectedPlayers())
                    .put("currentPlayers",  s.currentPlayers())
                    .put("players",         new JSONArray(s.players())));
        }
        return new Message(SESSION_LIST, new JSONObject().put("sessions", arr));
    }

    /**
     * JOIN with an explicit sessionId (to join a specific game).
     * If sessionId is null or blank it behaves like a normal JOIN.
     */
    public static Message joinWithSession(String nickname, int numPlayers,
                                          String totemColour, String sessionId) {
        JSONObject obj = new JSONObject()
                .put("nickname",    nickname)
                .put("numPlayers",  numPlayers)
                .put("totemColour", totemColour);
        if (sessionId != null && !sessionId.isBlank())
            obj.put("sessionId", sessionId);
        return new Message(JOIN, obj);
    }

    /**
     * Reads the SessionInfo list from a SESSION_LIST message.
     */
    public List<SessionInfo> sessionInfoList() {
        JSONArray arr = data.getJSONArray("sessions");
        List<SessionInfo> result = new ArrayList<>();
        for (int i = 0; i < arr.length(); i++) {
            JSONObject o = arr.getJSONObject(i);
            JSONArray  nickArr = o.getJSONArray("players");
            List<String> nicks = new ArrayList<>();
            for (int j = 0; j < nickArr.length(); j++) nicks.add(nickArr.getString(j));
            result.add(new SessionInfo(
                    o.getString("sessionId"),
                    o.getInt("expectedPlayers"),
                    o.getInt("currentPlayers"),
                    nicks));
        }
        return result;
    }

    /** Reads an optional string field; returns null if missing or JSON-null. */
    public String optStr(String key) {
        return data.isNull(key) ? null : data.optString(key, null);
    }

}
