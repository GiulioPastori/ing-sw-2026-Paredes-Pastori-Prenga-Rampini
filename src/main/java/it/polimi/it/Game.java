package it.polimi.it;

import it.polimi.it.Player.Player;
import it.polimi.it.cards.Building;
import it.polimi.it.cards.Card;
import it.polimi.it.cards.events.Event;
import it.polimi.it.tiles.OfferTile;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class Game {

    private static final int TOTAL_ROUNDS = 10;

    private final List<Player> players;
    private final Board board;
    private int currentRound;
    private final int currentPlayerIndex;

    public Game(List<Player> players, Board board) {
        this.players            = players;
        this.board              = board;
        this.currentRound       = 0;
        this.currentPlayerIndex = 0;
    }


    public void setup() {
        // random totem placement (td)
        currentRound = 0;
    }

    public void startGame() {
        setup();
        for (int i = 0; i < TOTAL_ROUNDS; i++) {
            nextRound();
        }
        endGame();
    }

    public void nextRound() {
        currentRound++;

        // ── Phase 1: place totems on Offer track
        // Players place in the order defined by the Turn Order tile (top → bottom).
        List<Player> placementOrder = getPlacementOrder();
        for (Player player : placementOrder) {
            OfferTile chosen = chooseOfferTile(player);
            board.placeTotem(player.getTotem(), chosen);
        }

        // ── Phase 2: resolve actions (left → right on Offer track)
        List<OfferTile> offerTrack = board.getOfferTrack();
        for (OfferTile tile : offerTrack) {
            if (tile.isFree()) continue;

            Player acting = getPlayerByTotem(tile.getPlacedTotem());
            resolveOfferAction(acting, tile);

            // Return totem to Turn Order tile; bonuses/penalties applied inside
            tile.removeTotem();
            board.getTurnOrderTile().placeTotem(acting);
        }

        endOfRound();
    }

    // Offer action resolution

    private void resolveOfferAction(Player player, OfferTile tile) {
        if (tile.isFoodOnly()) {
            // Tile A only (5-player games): take 3 food, no card draw
            player.addFood(3);
            return;
        }

        // Take cards from bottom row
        for (int i = 0; i < tile.getBottomRowCards(); i++) {
            Card chosen = chooseCardFromRow(player, board.getBottomRow());
            if (chosen == null) break;
            takeCard(player, chosen);
        }

        // Take cards from top row
        for (int i = 0; i < tile.getTopRowCards(); i++) {
            Card chosen = chooseCardFromRow(player, board.getTopRow());
            if (chosen == null) break;
            takeCard(player, chosen);
        }
    }

    /**
     * Assigns a card to the player and removes it from the board.
     * Building cards require food payment; if the player cannot afford it,
     * the card is not taken.
     */
    private void takeCard(Player player, Card card) {
        if (card instanceof Building b) {
            boolean taken = player.takeBuildingCard(b);
            if (taken) board.takeCard(b, player);
        } else if (card instanceof Character c) {
            player.takeCharacterCard(c);
            board.takeCard(card, player);
        }
        // Event cards cannot be taken (enforced by chooseCardFromRow)
    }

    // End of round

    private void endOfRound() {
        boolean isLastRound = (currentRound == TOTAL_ROUNDS);

        // Resolve events:
        // - normal rounds: only bottom row events
        // - last round: also top row events
        if (isLastRound) {
            board.resolveEndOfGameEvents(players);
        } else {
            board.resolveEvents(players);
        }

        if (!isLastRound) {
            board.clearBottomRow();
            board.moveTopToBottom();
            board.refillTopRow(players.size());
        }

        board.getTurnOrderTile().reset();
    }

    // End of game

    public void endGame() {
        calculatePoints();
    }

    /**
     * Triggers final scoring for all players.
     * Each player's calculateFinalScore() already sums in-game PP
     * with end-game contributions from Builders, Artists, Inventors, Buildings.
     */
    public void calculatePoints() {
        for (Player player : players) {
            player.calculateFinalScore();
        }
    }

    /** Returns the winner(s). Tiebreak: most food. If still tied, victory is shared. */
    public List<Player> getWinners() {
        int maxPP = players.stream()
                .mapToInt(Player::calculateFinalScore)
                .max()
                .orElse(0);

        List<Player> topScorers = players.stream()
                .filter(p -> p.calculateFinalScore() == maxPP)
                .toList();

        if (topScorers.size() == 1) return topScorers;

        // Tiebreak: most food
        int maxFood = topScorers.stream()
                .mapToInt(Player::getFood)
                .max()
                .orElse(0);

        return topScorers.stream()
                .filter(p -> p.getFood() == maxFood)
                .toList();
    }

    // Turn order helpers
    //

    /** Returns players sorted by their position on the Turn Order tile (top → bottom). */
    private List<Player> getPlacementOrder() {
        List<Player> ordered = new ArrayList<>(players);
        ordered.sort(Comparator.comparingInt(Player::getTurnOrder));
        return ordered;
    }

    private Player getPlayerByTotem(Totem totem) {
        return players.stream()
                .filter(p -> p.getTotem() == totem)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No player found for totem: " + totem));
    }

    // -----------------------------------------------------------------------
    // Input hooks — override or replace with UI/AI logic
    // -----------------------------------------------------------------------

    /**
     * Hook: the player chooses which free OfferTile to place their totem on.
     * Default implementation picks the first available tile (leftmost).
     * Replace with UI input or AI strategy as needed.
     */
    protected OfferTile chooseOfferTile(Player player) {
        List<OfferTile> available = board.getAvailableOfferTiles();
        if (available.isEmpty())
            throw new IllegalStateException("No available offer tiles for " + player.getName());
        return available.get(0);
    }

    /**
     * Hook: the player chooses a card from the given row.
     * Returns null if no valid card is available or the player passes.
     * Event cards are excluded automatically.
     * Default implementation picks the first non-Event card.
     */
    protected Card chooseCardFromRow(Player player, List<Card> row) {
        return row.stream()
                .filter(c -> !(c instanceof Event))
                .findFirst()
                .orElse(null);
    }

    // -----------------------------------------------------------------------
    // Getters
    // -----------------------------------------------------------------------

    public List<Player> getPlayers()      { return players; }
    public Board        getBoard()        { return board; }
    public int          getCurrentRound() { return currentRound; }

    public int getCurrentPlayer() {
        return currentPlayerIndex;
    }
}