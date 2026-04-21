package it.polimi.it;

import it.polimi.it.cards.building.Building;
import it.polimi.it.cards.Card;
import it.polimi.it.cards.events.Event;
import it.polimi.it.cards.events.FinalEvent;
import it.polimi.it.enums.Era;
import it.polimi.it.enums.TotemColour;
import it.polimi.it.player.Player;
import it.polimi.it.tiles.OfferTile;
import it.polimi.it.tiles.OfferTiles;
import it.polimi.it.tiles.TurnOrderTile;

import java.util.*;

/**
 * Builds and configures a complete game (Builder pattern).
 *
 * Centralises all setup logic that would otherwise be scattered across
 * Game, Board and App: deck shuffling, filtering by player count, initial
 * food distribution, row and tile setup.
 *
 * Typical usage:
 * <pre>
 *   Game game = new GameBuilder()
 *       .withPlayers(List.of("Alice", "Bob"), List.of(RED, BLUE))
 *       .withCards(cardLoader.loadAll(path))
 *       .build();
 * </pre>
 *
 * After {@code build()} the Game is ready for {@code game.start()}.
 */
public class GameBuilder {

    // Starting food by position (rulebook p.3: 1st->2, 2nd/3rd->3, 4th/5th->4 food)
    private static final int[] STARTING_FOOD = { 0, 2, 3, 3, 4, 4 };

    private List<String>      playerNames;
    private List<TotemColour> playerColours;
    private List<Card>        allCards;
    private Random            rng = new Random();

    public GameBuilder withPlayers(List<String> names, List<TotemColour> colours) {
        if (names.size() != colours.size()) throw new IllegalArgumentException(
                "Names and colours lists must have the same length.");
        if (names.size() < 2 || names.size() > 5) throw new IllegalArgumentException(
                "The game supports 2 to 5 players.");
        this.playerNames   = new ArrayList<>(names);
        this.playerColours = new ArrayList<>(colours);
        return this;
    }

    public GameBuilder withCards(List<Card> cards) {
        this.allCards = new ArrayList<>(cards);
        return this;
    }

    /** Allows injecting a fixed-seed Random for deterministic tests. */
    public GameBuilder withRandom(Random rng) {
        this.rng = rng;
        return this;
    }

    /**
     * Builds the Game ready to be started.
     *
     * @throws IllegalStateException if required data has not been provided
     */
    public Game build() {
        validate();

        int n = playerNames.size();

        // 1. Create players and distribute starting food
        List<Player> players = buildPlayers(n);

        // 2. Build and shuffle the decks
        List<Card>                tribeDeckList  = buildTribeDeck();
        Map<Era, Deque<Building>> buildingDecks  = buildBuildingDecks(n);

        // 3. Build the Offer Track for this player count
        OfferTiles    offerTiles    = buildOfferTiles(n);
        TurnOrderTile turnOrderTile = new TurnOrderTile(n);

        // 4. Assemble the Board
        Deque<Card> tribeDeck = new ArrayDeque<>(tribeDeckList);
        Board board = new Board(tribeDeck, buildingDecks, offerTiles, turnOrderTile);

        // 5. Set up the initial rows (rulebook p.2-3)
        board.setupBottomRow(n);
        board.setupTopRow(n);

        // 6. Assign a random initial turn order (rulebook p.3)
        assignInitialTurnOrder(players);

        return new Game(players, board);
    }

    // Player construction

    private List<Player> buildPlayers(int n) {
        List<Player> players = new ArrayList<>();
        // Shuffle indices to randomise starting food assignment
        List<Integer> indices = new ArrayList<>();
        for (int i = 0; i < n; i++) indices.add(i);
        Collections.shuffle(indices, rng);

        for (int i = 0; i < n; i++) {
            Player p = new Player(playerNames.get(i), playerColours.get(i));
            int startingFood = STARTING_FOOD[indices.indexOf(i) + 1];
            p.addFood(startingFood);
            players.add(p);
        }
        return players;
    }

    // Tribe deck construction

    /**
     * Builds the tribe deck (rulebook p.2):
     * - Characters and non-final events are shuffled in three era groups
     * - Groups are added in era order (Era I on top, Era III at the bottom)
     * - The 2 FinalEvent cards are placed at the very bottom
     */
    private List<Card> buildTribeDeck() {
        List<Card>       eraI   = new ArrayList<>();
        List<Card>       eraII  = new ArrayList<>();
        List<Card>       eraIII = new ArrayList<>();
        List<FinalEvent> finals = new ArrayList<>();

        for (Card card : allCards) {
            if (card instanceof Building) continue;
            if (card instanceof FinalEvent fe) { finals.add(fe); continue; }
            switch (card.getEra()) {
                case ERA_I   -> eraI.add(card);
                case ERA_II  -> eraII.add(card);
                case ERA_III -> eraIII.add(card);
            }
        }

        Collections.shuffle(eraI,   rng);
        Collections.shuffle(eraII,  rng);
        Collections.shuffle(eraIII, rng);
        Collections.shuffle(finals, rng);

        List<Card> deck = new ArrayList<>();
        deck.addAll(eraI);
        deck.addAll(eraII);
        deck.addAll(eraIII);
        deck.addAll(finals);
        return deck;
    }

    // Building deck construction

    /**
     * Builds and shuffles the three building decks (one per era).
     * The number of buildings per era varies with player count
     * (rulebook p.2: 2P->1/2/3, 3P->2/2/4, 4P->2/3/4, 5P->2/3/5).
     */
    private Map<Era, Deque<Building>> buildBuildingDecks(int numPlayers) {
        // Counts: [eraI, eraII, eraIII] indexed by player count (index 2-5)
        int[][] counts = {
                {}, {},
                { 1, 2, 3 },
                { 2, 2, 4 },
                { 2, 3, 4 },
                { 2, 3, 5 }
        };
        int[] c = counts[numPlayers];

        Map<Era, List<Building>> byEra = new EnumMap<>(Era.class);
        byEra.put(Era.ERA_I,   new ArrayList<>());
        byEra.put(Era.ERA_II,  new ArrayList<>());
        byEra.put(Era.ERA_III, new ArrayList<>());

        for (Card card : allCards) {
            if (card instanceof Building b) byEra.get(b.getEra()).add(b);
        }

        Map<Era, Deque<Building>> decks = new EnumMap<>(Era.class);
        for (Era era : Era.values()) {
            List<Building> pool = byEra.get(era);
            Collections.shuffle(pool, rng);
            int take = switch (era) {
                case ERA_I   -> c[0];
                case ERA_II  -> c[1];
                case ERA_III -> c[2];
            };
            Deque<Building> deck = new ArrayDeque<>();
            pool.stream().limit(take).forEach(deck::add);
            decks.put(era, deck);
        }
        return decks;
    }

    // Offer Track construction

    /**
     * Builds the Offer Track with tiles active for the given player count.
     * Tile configuration (rulebook p.4):
     * - A (5P only): 3 food, no card draws
     * - B: 0 bottom, 1 top  (2P+)
     * - C: 1 bottom, 0 top  (2P+)
     * - D: 1 bottom, 1 top  (3P+)
     * - E: 2 bottom, 1 top  (3P+)
     * - F: 2 bottom, 2 top  (4P+)
     * - G: 3 bottom, 2 top  (5P+)
     */
    private OfferTiles buildOfferTiles(int numPlayers) {
        List<OfferTile> tiles = new ArrayList<>();

        // Tessera A: solo con 5 giocatori, dà 3 food
        if (numPlayers >= 5) tiles.add(new OfferTile('A', 5, 0, 0, true));

        // B e C: sempre presenti (2P+)
        if (numPlayers >= 2) tiles.add(new OfferTile('B', 2, 0, 1, false));
        if (numPlayers >= 2) tiles.add(new OfferTile('C', 2, 1, 0, false));

        // D: da 3 giocatori in su
        if (numPlayers >= 3) tiles.add(new OfferTile('D', 3,0, 2, false));

        // E: sempre presente (2P+)
        if (numPlayers >= 2) tiles.add(new OfferTile('E', 2, 1, 1, false));

        // F: sempre presente (2P+)
        if (numPlayers >= 2) tiles.add(new OfferTile('F', 2, 2, 0, false));

        // G: da 4 giocatori in su
        if (numPlayers >= 4) tiles.add(new OfferTile('G', 4, 2, 1, false));

        return new OfferTiles(tiles);
    }

    // Initial turn order

    /**
     * Assigns a random initial turn order. Bonuses and penalties of the
     * Turn Order Tile do not apply during setup (rulebook p.3).
     */
    private void assignInitialTurnOrder(List<Player> players) {
        List<Player> shuffled = new ArrayList<>(players);
        Collections.shuffle(shuffled, rng);
        for (int i = 0; i < shuffled.size(); i++) {
            shuffled.get(i).setTurnOrder(i + 1);
        }
    }

    // Validation

    private void validate() {
        if (playerNames == null || playerNames.isEmpty())
            throw new IllegalStateException("Provide players via withPlayers().");
        if (allCards == null || allCards.isEmpty())
            throw new IllegalStateException("Provide cards via withCards().");
        if (new HashSet<>(playerColours).size() != playerColours.size())
            throw new IllegalArgumentException("Each player must have a unique totem colour.");
        if (new HashSet<>(playerNames).size() != playerNames.size())
            throw new IllegalArgumentException("Each player must have a unique name.");
    }
}