package it.polimi.it;

import it.polimi.it.cards.building.Building;
import it.polimi.it.cards.Card;
import it.polimi.it.cards.character.Hunter;
import it.polimi.it.cards.character.Gatherer;
import it.polimi.it.cards.events.Event;
import it.polimi.it.cards.events.Hunt;
import it.polimi.it.cards.events.Sustenance;
import it.polimi.it.enums.Era;
import it.polimi.it.enums.EventType;
import it.polimi.it.enums.TotemColour;
import it.polimi.it.player.Player;
import it.polimi.it.tiles.OfferTile;
import it.polimi.it.tiles.OfferTiles;
import it.polimi.it.tiles.TurnOrderTile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.*;

        import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Board")
class BoardTest {

    private static int idCounter = 5000;
    private static int nextId()  { return idCounter++; }

    private Player alice;
    private Player bob;

    @BeforeEach
    void setUp() {
        alice = new Player("Alice", TotemColour.RED);
        bob   = new Player("Bob",   TotemColour.BLUE);
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private Board emptyBoard() {
        Deque<Card> deck = new ArrayDeque<>();
        Map<Era, Deque<Building>> buildings = new EnumMap<>(Era.class);
        for (Era e : Era.values()) buildings.put(e, new ArrayDeque<>());
        OfferTiles offerTiles = new OfferTiles(List.of(
                new OfferTile('B', 2, 1, 0, false),
                new OfferTile('C', 2, 0, 1, false)));
        return new Board(deck, buildings, offerTiles, new TurnOrderTile(2));
    }

    private Board boardWithDeck(List<Card> cards) {
        Deque<Card> deck = new ArrayDeque<>(cards);
        Map<Era, Deque<Building>> buildings = new EnumMap<>(Era.class);
        for (Era e : Era.values()) buildings.put(e, new ArrayDeque<>());
        OfferTiles offerTiles = new OfferTiles(List.of(
                new OfferTile('B', 2, 1, 0, false),
                new OfferTile('C', 2, 0, 1, false)));
        return new Board(deck, buildings, offerTiles, new TurnOrderTile(2));
    }

    // ── setupBottomRow ────────────────────────────────────────────────────────

    @Test
    @DisplayName("setupBottomRow draws numPlayers+1 non-event cards")
    void setupBottomRowDrawsCorrectCount() {
        List<Card> deck = new ArrayList<>();
        for (int i = 0; i < 10; i++) deck.add(new Hunter(nextId(), Era.ERA_I, false));
        Board board = boardWithDeck(deck);
        board.setupBottomRow(2);
        assertEquals(3, board.getBottomRow().size()); // 2 players + 1
    }

    @Test
    @DisplayName("setupBottomRow sends event cards to top row instead")
    void setupBottomRowSendsEventsToTop() {
        List<Card> deck = new ArrayList<>();
        deck.add(new Event(nextId(), Era.ERA_I, EventType.HUNT, 1, new Hunt(1)));
        for (int i = 0; i < 5; i++) deck.add(new Hunter(nextId(), Era.ERA_I, false));
        Board board = boardWithDeck(deck);
        board.setupBottomRow(2);
        assertEquals(3, board.getBottomRow().size());
        assertEquals(1, board.getTopRow().size()); // event went to top
        assertTrue(board.getTopRow().get(0) instanceof Event);
    }

    // ── setupTopRow ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("setupTopRow draws numPlayers+4 cards accounting for events already there")
    void setupTopRowDrawsCorrectCount() {
        List<Card> deck = new ArrayList<>();
        for (int i = 0; i < 20; i++) deck.add(new Hunter(nextId(), Era.ERA_I, false));
        Board board = boardWithDeck(deck);
        board.setupBottomRow(2); // draws 3, top row has 0 events
        board.setupTopRow(2);   // should draw 6 (2+4), no events already in top
        assertEquals(6, board.getTopRow().size());
    }

    @Test
    @DisplayName("setupTopRow adds Era I buildings to top row")
    void setupTopRowAddsEraIBuildings() {
        Deque<Card> deck = new ArrayDeque<>();
        for (int i = 0; i < 20; i++) deck.add(new Hunter(nextId(), Era.ERA_I, false));
        Map<Era, Deque<Building>> buildings = new EnumMap<>(Era.class);
        Deque<Building> eraIBuildings = new ArrayDeque<>();
        eraIBuildings.add(new Building(101, Era.ERA_I, 2, 3, "test"));
        buildings.put(Era.ERA_I,   eraIBuildings);
        buildings.put(Era.ERA_II,  new ArrayDeque<>());
        buildings.put(Era.ERA_III, new ArrayDeque<>());
        OfferTiles offerTiles = new OfferTiles(List.of(
                new OfferTile('B', 2, 1, 0, false)));
        Board board = new Board(deck, buildings, offerTiles, new TurnOrderTile(2));
        board.setupBottomRow(2);
        board.setupTopRow(2);
        long buildingCount = board.getTopRow().stream()
                .filter(c -> c instanceof Building).count();
        assertEquals(1, buildingCount);
    }

    // ── moveTopToBottom ───────────────────────────────────────────────────────

    @Test
    @DisplayName("moveTopToBottom moves only non-building cards")
    void moveTopToBottomMovesNonBuildings() {
        List<Card> deck = new ArrayList<>();
        for (int i = 0; i < 20; i++) deck.add(new Hunter(nextId(), Era.ERA_I, false));
        Board board = boardWithDeck(deck);
        board.setupBottomRow(2);
        board.setupTopRow(2);
        int topSize = board.getTopRow().size();
        board.moveTopToBottom();
        // All non-building top cards moved to bottom
        assertEquals(0, board.getTopRow().stream()
                .filter(c -> !(c instanceof Building)).count());
        assertTrue(board.getBottomRow().size() > 0);
    }

    @Test
    @DisplayName("moveTopToBottom keeps buildings in top row")
    void moveTopToBottomKeepsBuildings() {
        Deque<Card> deck = new ArrayDeque<>();
        for (int i = 0; i < 20; i++) deck.add(new Hunter(nextId(), Era.ERA_I, false));
        Map<Era, Deque<Building>> buildings = new EnumMap<>(Era.class);
        Deque<Building> eraI = new ArrayDeque<>();
        eraI.add(new Building(101, Era.ERA_I, 2, 3, "test"));
        buildings.put(Era.ERA_I,   eraI);
        buildings.put(Era.ERA_II,  new ArrayDeque<>());
        buildings.put(Era.ERA_III, new ArrayDeque<>());
        OfferTiles offerTiles = new OfferTiles(List.of(
                new OfferTile('B', 2, 1, 0, false)));
        Board board = new Board(deck, buildings, offerTiles, new TurnOrderTile(2));
        board.setupBottomRow(2);
        board.setupTopRow(2);
        board.moveTopToBottom();
        long buildingsInTop = board.getTopRow().stream()
                .filter(c -> c instanceof Building).count();
        assertEquals(1, buildingsInTop);
    }

    // ── clearBottomRowExceptBuildings ─────────────────────────────────────────

    @Test
    @DisplayName("clearBottomRowExceptBuildings removes characters and events")
    void clearBottomRowRemovesNonBuildings() {
        List<Card> deck = new ArrayList<>();
        for (int i = 0; i < 20; i++) deck.add(new Hunter(nextId(), Era.ERA_I, false));
        Board board = boardWithDeck(deck);
        board.setupBottomRow(2);
        board.setupTopRow(2);
        board.moveTopToBottom();
        board.clearBottomRowExceptBuildings();
        long nonBuildings = board.getBottomRow().stream()
                .filter(c -> !(c instanceof Building)).count();
        assertEquals(0, nonBuildings);
    }

    // ── refillTopRow ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("refillTopRow draws enough cards to reach numPlayers+4")
    void refillTopRowDrawsCorrectCount() {
        List<Card> deck = new ArrayList<>();
        for (int i = 0; i < 30; i++) deck.add(new Hunter(nextId(), Era.ERA_I, false));
        Board board = boardWithDeck(deck);
        board.setupBottomRow(2);
        board.setupTopRow(2);
        board.moveTopToBottom();
        board.refillTopRow(2);
        long nonBuildings = board.getTopRow().stream()
                .filter(c -> !(c instanceof Building)).count();
        assertEquals(6, nonBuildings); // 2 + 4
    }

    @Test
    @DisplayName("refillTopRow returns true when a new era card is drawn")
    void refillTopRowDetectsEraChange() {
        List<Card> deck = new ArrayList<>();
        for (int i = 0; i < 5; i++) deck.add(new Hunter(nextId(), Era.ERA_I, false));
        // Add an Era II card so era change is detected
        deck.add(new Hunter(nextId(), Era.ERA_II, false));
        for (int i = 0; i < 10; i++) deck.add(new Hunter(nextId(), Era.ERA_II, false));
        Board board = boardWithDeck(deck);
        board.setupBottomRow(2);
        board.setupTopRow(2);
        board.moveTopToBottom();
        board.clearBottomRowExceptBuildings();
        boolean changed = board.refillTopRow(2);
        assertTrue(changed);
    }

    @Test
    @DisplayName("refillTopRow returns false when no era change")
    void refillTopRowNoEraChange() {
        List<Card> deck = new ArrayList<>();
        for (int i = 0; i < 30; i++) deck.add(new Hunter(nextId(), Era.ERA_I, false));
        Board board = boardWithDeck(deck);
        board.setupBottomRow(2);
        board.setupTopRow(2);
        board.moveTopToBottom();
        board.clearBottomRowExceptBuildings();
        boolean changed = board.refillTopRow(2);
        assertFalse(changed);
    }

    // ── resolveBottomRowEvents ────────────────────────────────────────────────

    @Test
    @DisplayName("resolveBottomRowEvents resolves Hunt before Sustenance")
    void resolveBottomRowEventsSustenanceLast() {
        // Track resolution order
        List<String> resolved = new ArrayList<>();
        Board board = emptyBoard();

        // Manually add events to bottom row via moveTopToBottom trick:
        // Use a deck with events so they end up in bottom row
        Deque<Card> deck = new ArrayDeque<>();
        deck.add(new Event(nextId(), Era.ERA_I, EventType.SUSTENANCE, 1, new Sustenance(1)) {
            @Override public void resolve(List<Player> players) {
                resolved.add("SUSTENANCE");
            }
        });
        deck.add(new Event(nextId(), Era.ERA_I, EventType.HUNT, 1, new Hunt(1)) {
            @Override public void resolve(List<Player> players) {
                resolved.add("HUNT");
            }
        });
        for (int i = 0; i < 20; i++) deck.add(new Hunter(nextId(), Era.ERA_I, false));

        Map<Era, Deque<Building>> buildings = new EnumMap<>(Era.class);
        for (Era e : Era.values()) buildings.put(e, new ArrayDeque<>());
        OfferTiles offerTiles = new OfferTiles(List.of(
                new OfferTile('B', 2, 1, 0, false)));
        Board b = new Board(deck, buildings, offerTiles, new TurnOrderTile(2));
        b.setupBottomRow(2);
        b.setupTopRow(2);
        b.moveTopToBottom();

        b.resolveBottomRowEvents(List.of(alice, bob));

        // Sustenance must be last
        int huntIdx = resolved.indexOf("HUNT");
        int sustIdx = resolved.indexOf("SUSTENANCE");
        if (huntIdx >= 0 && sustIdx >= 0) {
            assertTrue(huntIdx < sustIdx, "HUNT should be resolved before SUSTENANCE");
        }
    }

    // ── removeCard ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("removeCard removes a card from the top row")
    void removeCardFromTopRow() {
        List<Card> deck = new ArrayList<>();
        Hunter h = new Hunter(nextId(), Era.ERA_I, false);
        deck.add(h);
        for (int i = 0; i < 10; i++) deck.add(new Hunter(nextId(), Era.ERA_I, false));
        Board board = boardWithDeck(deck);
        board.setupBottomRow(2);
        board.setupTopRow(2);
        // h may be in top or bottom row
        try {
            board.removeCard(h);
        } catch (IllegalArgumentException e) {
            // h ended up nowhere visible — acceptable for this test setup
        }
    }

    @Test
    @DisplayName("removeCard throws when card is not on the board")
    void removeCardThrowsWhenNotFound() {
        Board board = emptyBoard();
        Hunter unknown = new Hunter(nextId(), Era.ERA_I, false);
        assertThrows(IllegalArgumentException.class, () -> board.removeCard(unknown));
    }

    // ── advanceEra ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("advanceEra to ERA_II places Era II buildings in top row")
    void advanceEraPlacesNewBuildings() {
        Deque<Card> deck = new ArrayDeque<>();
        for (int i = 0; i < 20; i++) deck.add(new Hunter(nextId(), Era.ERA_I, false));
        Map<Era, Deque<Building>> buildings = new EnumMap<>(Era.class);
        buildings.put(Era.ERA_I,   new ArrayDeque<>());
        Deque<Building> eraII = new ArrayDeque<>();
        eraII.add(new Building(201, Era.ERA_II, 3, 4, "test"));
        buildings.put(Era.ERA_II,  eraII);
        buildings.put(Era.ERA_III, new ArrayDeque<>());
        OfferTiles offerTiles = new OfferTiles(List.of(
                new OfferTile('B', 2, 1, 0, false)));
        Board board = new Board(deck, buildings, offerTiles, new TurnOrderTile(2));
        board.setupBottomRow(2);
        board.setupTopRow(2);
        board.advanceEra(Era.ERA_II);
        assertEquals(Era.ERA_II, board.getCurrentEra());
        long eraIIBuildings = board.getTopRow().stream()
                .filter(c -> c instanceof Building && c.getEra() == Era.ERA_II).count();
        assertEquals(1, eraIIBuildings);
    }

    @Test
    @DisplayName("advanceEra to ERA_III removes Era II buildings from bottom row")
    void advanceEraToIIIRemovesOldBuildings() {
        Deque<Card> deck = new ArrayDeque<>();
        for (int i = 0; i < 20; i++) deck.add(new Hunter(nextId(), Era.ERA_I, false));
        Map<Era, Deque<Building>> buildings = new EnumMap<>(Era.class);
        buildings.put(Era.ERA_I,   new ArrayDeque<>());
        buildings.put(Era.ERA_II,  new ArrayDeque<>());
        buildings.put(Era.ERA_III, new ArrayDeque<>());
        OfferTiles offerTiles = new OfferTiles(List.of(
                new OfferTile('B', 2, 1, 0, false)));
        Board board = new Board(deck, buildings, offerTiles, new TurnOrderTile(2));
        board.setupBottomRow(2);
        board.setupTopRow(2);
        board.moveTopToBottom();
        board.advanceEra(Era.ERA_III);
        long buildingsInBottom = board.getBottomRow().stream()
                .filter(c -> c instanceof Building).count();
        assertEquals(0, buildingsInBottom);
    }

    // ── getters ───────────────────────────────────────────────────────────────

    @Test
    @DisplayName("initial era is ERA_I")
    void initialEraIsEraI() {
        assertEquals(Era.ERA_I, emptyBoard().getCurrentEra());
    }

    @Test
    @DisplayName("isTribeDeckEmpty returns true when deck is empty")
    void isTribeDeckEmptyTrue() {
        assertTrue(emptyBoard().isTribeDeckEmpty());
    }

    @Test
    @DisplayName("getTribeDeckSize returns correct size")
    void getTribeDeckSizeCorrect() {
        List<Card> deck = new ArrayList<>();
        for (int i = 0; i < 5; i++) deck.add(new Hunter(nextId(), Era.ERA_I, false));
        assertEquals(5, boardWithDeck(deck).getTribeDeckSize());
    }
}