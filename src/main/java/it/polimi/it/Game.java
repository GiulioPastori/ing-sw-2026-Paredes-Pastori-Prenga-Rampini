package it.polimi.it;


import it.polimi.it.player.Player;
import it.polimi.it.cards.building.Building;
import it.polimi.it.cards.Card;
import it.polimi.it.cards.events.Event;
import it.polimi.it.tiles.OfferTile;
import it.polimi.it.enums.Era;
import it.polimi.it.cards.character.Character;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class Game {

    private static final int TOTAL_ROUNDS = 10;
    private final List<Player> players;
    private final Board board;
    private int currentRound;
    private int currentPlayerIndex;
    private Era currentEra;

    public Game(List<Player> players, Board board) {
        this.players            = players;
        this.board              = board;
        this.currentRound       = 0;
        this.currentPlayerIndex = 0;
        this.currentEra         = Era.ERA_I;
    }

    public void setup() {
        currentRound = 0;
        currentEra = Era.ERA_I;
        board.setupBuildingDecks();
    }

    public void startGame() {
        setup();
        for (int i = 0; i < TOTAL_ROUNDS; i++) {
            nextRound();
        }
        endGame();
    }

    /** Prepara i dati per l'inizio di un nuovo round */
    public void startNewRound() {
        currentRound++;
        updateCurrentEra();
    }

    public void nextRound() {
        currentRound++;
        updateCurrentEra();

        // ── Phase 1: Piazzamento Totem
        List<Player> placementOrder = getPlacementOrder();
        for (Player player : placementOrder) {
            OfferTile chosen = chooseOfferTile(player);
            // FIX: Passiamo l'intero Player se la Board è flessibile,
            // o player.getTotem() se la Board accetta TotemColour.
            board.placeTotem(player.getTotem(), chosen);
        }

        // ── Phase 2: Risoluzione Azioni
        List<OfferTile> offerTrack = board.getOfferTrack();
        for (OfferTile tile : offerTrack) {
            if (tile.isFree()) continue;

            // Identifichiamo chi ha il totem su questa tessera
            Player acting = getPlayerByTotem(tile.getPlacedTotem());

            if (acting != null) {
                // Eseguiamo l'azione della tessera (ex "doAction")
                resolveOfferAction(acting, tile);

                // Spostiamo il totem sulla tessera Ordine di Turno per il prossimo round
                tile.removeTotem();
                board.getTurnOrderTile().placeTotem(acting);
            }
        }

        endOfRound();
    }

    private void updateCurrentEra() {
        if (currentRound <= 3) currentEra = Era.ERA_I;
        else if (currentRound <= 7) currentEra = Era.ERA_II;
        else currentEra = Era.ERA_III;
    }

    private void resolveOfferAction(Player player, OfferTile tile) {
        if (tile.isFoodOnly()) {
            player.addFood(3);
            return;
        }

        for (int i = 0; i < tile.getBottomRowCards(); i++) {
            Card chosen = chooseCardFromRow(player, board.getBottomRow());
            if (chosen != null) takeCard(player, chosen);
        }

        for (int i = 0; i < tile.getTopRowCards(); i++) {
            Card chosen = chooseCardFromRow(player, board.getTopRow());
            if (chosen != null) takeCard(player, chosen);
        }
    }

    protected void takeCard(Player player, Card card) {
        if (card instanceof Building b) {
            if (player.takeBuildingCard(b, board)) {
                board.takeCard(b, player);
            }
        } else if (card instanceof Character c) {
            player.takeCharacterCard(c, board);
            board.takeCard(card, player);

            for (Building b : player.getTribe().getBuildings()) {
                b.onNewCharacter(player, c);
            }
        }
    }

    protected void endOfRound() {
        if (currentRound == TOTAL_ROUNDS) {
            board.resolveEndOfGameEvents(players);
        } else {
            board.resolveEvents(players);
        }

        if (currentRound == 3 || currentRound == 7 || currentRound == 10) {
            players.forEach(this::resolveSustenance);
        }

        if (currentRound < TOTAL_ROUNDS) {
            board.clearBottomRow();
            board.moveTopToBottom();
            // FIX: Passiamo i DUE argomenti (int, Era)
            board.refillTopRow(players.size(), currentEra);
        }

        board.getTurnOrderTile().reset();
    }

    private void resolveSustenance(Player p) {
        p.addFood(-p.getTribe().getSustenanceCost());
    }

    public void endGame() {
        players.forEach(Player::calculateFinalScore);
    }

    // --- HELPER DI SUPPORTO PER GLI ERRORI DI TIPO ---

    /**
     * Risolve il problema del TotemColour cercando il player che possiede quel colore.
     */
    protected Player getPlayerByTotem(Object totemId) {
        return players.stream()
                .filter(p -> p.getTotem().equals(totemId))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Player non trovato per il totem: " + totemId));
    }

    protected List<Player> getPlacementOrder() {
        List<Player> ordered = new ArrayList<>(players);
        ordered.sort(Comparator.comparingInt(Player::getTurnOrder));
        return ordered;
    }

    protected OfferTile chooseOfferTile(Player player) {
        return board.getAvailableOfferTiles().stream()
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Nessuna tessera libera"));
    }

    protected Card chooseCardFromRow(Player player, List<Card> row) {
        return row.stream()
                .filter(c -> !(c instanceof Event))
                .findFirst()
                .orElse(null);
    }

    // Getters
    public List<Player> getPlayers()      { return players; }
    public Board        getBoard()        { return board; }

    public Era getCurrentEra() {return currentEra;}
    public int          getCurrentRound() { return currentRound; }
    public Player       getCurrentPlayer(){ return players.get(currentPlayerIndex); }
}