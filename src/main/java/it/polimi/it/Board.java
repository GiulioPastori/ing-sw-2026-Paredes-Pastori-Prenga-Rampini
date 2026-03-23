package it.polimi.it;


import it.polimi.it.cards.building.Building;
import it.polimi.it.cards.Card;
import it.polimi.it.cards.events.Event;
import it.polimi.it.enums.Era;
import it.polimi.it.enums.EventType;
import it.polimi.it.Player.Player;
import it.polimi.it.tiles.OfferTile;
import it.polimi.it.tiles.OfferTiles;
import it.polimi.it.tiles.TurnOrderTile;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.List;
import java.util.Map;

/**
 * Ground truth of the current game state (the Board).
 *
 * Holds the observable state of the table:
 * - the tribe deck
 * - the top row and bottom row of visible cards
 * - the building decks per era (I, II, III)
 * - the Offer Track
 * - the Turn Order Tile
 * - the current era
 *
 * Board does not contain complex game logic: it exposes methods to
 * modify and query state. Orchestration logic (event order, era change,
 * etc.) lives in Game.
 */
public class Board {

    /** Cards in the top row: players + 4 (rulebook p.2). */
    public static final int TOP_ROW_EXTRA = 4;

    /** Cards in the bottom row during setup: players + 1 (rulebook p.2). */
    public static final int BOTTOM_ROW_SETUP_EXTRA = 1;

    /** Tribe deck (characters + events), from top to bottom. */
    private final Deque<Card> tribeDeck;

    /**
     * Top row: most recently drawn cards.
     * Includes characters, events, and buildings for the current era.
     * Event cards cannot be taken by players.
     */
    private final List<Card> topRow;

    /**
     * Bottom row: cards that moved down from the top row last round.
     * Buildings never move to the bottom row and are not discarded
     * with characters (rulebook p.6-7).
     */
    private final List<Card> bottomRow;

    /**
     * Building decks per era (Era -> deck).
     * Shuffled during setup; Era I buildings are placed in the top row;
     * others remain face-down until their era begins.
     */
    private final Map<Era, Deque<Building>> buildingDecks;

    /** The Offer Track with the active tiles for this game. */
    private final OfferTiles offerTiles;

    /** The Turn Order Tile for this game. */
    private final TurnOrderTile turnOrderTile;

    /** Current era (changes when the first card of a new era is drawn). */
    private Era currentEra;

    public Board(Deque<Card> tribeDeck,
                 Map<Era, Deque<Building>> buildingDecks,
                 OfferTiles offerTiles,
                 TurnOrderTile turnOrderTile) {
        this.tribeDeck     = tribeDeck;
        this.buildingDecks = buildingDecks;
        this.offerTiles    = offerTiles;
        this.turnOrderTile = turnOrderTile;
        this.topRow        = new ArrayList<>();
        this.bottomRow     = new ArrayList<>();
        this.currentEra    = Era.ERA_I;
    }

    // Initial row setup

    /**
     * Draws cards from the tribe deck to form the initial bottom row
     * (players + 1 cards, rulebook p.2 point 4).
     * Event cards drawn here go to the top row instead; drawing continues
     * until enough non-event cards are in the bottom row.
     *
     * @param numPlayers number of players
     */
    public void setupBottomRow(int numPlayers) {
        int needed = numPlayers + BOTTOM_ROW_SETUP_EXTRA;
        int drawn  = 0;
        while (drawn < needed && !tribeDeck.isEmpty()) {
            Card card = tribeDeck.poll();
            if (card instanceof Event) {
                topRow.add(card);
            } else {
                bottomRow.add(card);
                drawn++;
            }
        }
    }

    /**
     * Draws cards from the tribe deck to form the initial top row
     * (players + 4 cards, rulebook p.2 point 5).
     * Accounts for event cards already in the top row from setupBottomRow.
     *
     * @param numPlayers number of players
     */
    public void setupTopRow(int numPlayers) {
        int needed              = numPlayers + TOP_ROW_EXTRA;
        long eventsAlreadyInTop = topRow.stream().filter(c -> c instanceof Event).count();
        int  toDraw             = (int) (needed - eventsAlreadyInTop);

        for (int i = 0; i < toDraw && !tribeDeck.isEmpty(); i++) {
            topRow.add(tribeDeck.poll());
        }

        // Place Era I buildings in the top row (rulebook p.3 point 6a)
        Deque<Building> eraIBuildings = buildingDecks.get(Era.ERA_I);
        if (eraIBuildings != null) {
            while (!eraIBuildings.isEmpty()) topRow.add(eraIBuildings.poll());
        }
    }

    // End-of-round row management

    /**
     * Removes all character and event cards from the bottom row.
     * Buildings remain (rulebook p.6 point 2).
     */
    public void clearBottomRowExceptBuildings() {
        bottomRow.removeIf(card -> !(card instanceof Building));
    }

    /**
     * Moves character and event cards from the top row to the bottom row.
     * Buildings remain in the top row (rulebook p.6 point 3).
     */
    public void moveTopToBottom() {
        List<Card> toMove = new ArrayList<>();
        for (Card card : topRow) {
            if (!(card instanceof Building)) toMove.add(card);
        }
        topRow.removeAll(toMove);
        bottomRow.addAll(toMove);
    }

    /**
     * Refills the top row by drawing new cards from the tribe deck
     * (players + 4, rulebook p.6 point 4).
     * Also detects whether a new era has begun.
     * Note: currentEra is updated by advanceEra(), called by Game when this returns true.
     *
     * @param numPlayers number of players
     * @return true if a card from a new era was revealed during the refill
     */
    public boolean refillTopRow(int numPlayers) {
        int needed      = numPlayers + TOP_ROW_EXTRA;
        long nonBuildings = topRow.stream().filter(c -> !(c instanceof Building)).count();
        int  toDraw     = (int) (needed - nonBuildings);

        boolean eraChanged = false;
        for (int i = 0; i < toDraw && !tribeDeck.isEmpty(); i++) {
            Card card = tribeDeck.poll();
            topRow.add(card);
            if (card.getEra().ordinal() > currentEra.ordinal()) {
                eraChanged = true;
            }
        }
        return eraChanged;
    }

    // Era transition

    /**
     * Advances to the next era, applying the transition rules (rulebook p.7):
     * 1. When advancing to Era III: discard buildings from the bottom row
     * 2. Move buildings from the top row to the bottom row
     * 3. Place the new era's buildings in the top row
     *
     * @param newEra the era that has just begun
     */
    public void advanceEra(Era newEra) {
        if (newEra == Era.ERA_III) {
            bottomRow.removeIf(card -> card instanceof Building);
        }

        List<Card> buildingsFromTop = new ArrayList<>();
        for (Card card : topRow) {
            if (card instanceof Building) buildingsFromTop.add(card);
        }
        topRow.removeAll(buildingsFromTop);
        bottomRow.addAll(buildingsFromTop);

        Deque<Building> newEraBuildings = buildingDecks.get(newEra);
        if (newEraBuildings != null) {
            while (!newEraBuildings.isEmpty()) topRow.add(newEraBuildings.poll());
        }

        currentEra = newEra;
    }

    // Event resolution

    /**
     * Collects and resolves events in the bottom row.
     * Sustenance is always resolved last (rulebook p.5).
     * When two events of the same type appear in the same round,
     * the lower-era one is resolved first (rulebook p.5).
     * Returns the list of resolved events so Game can publish EventResolved for each.
     *
     * @param players list of all players
     * @return list of resolved events
     */
    public List<Event> resolveBottomRowEvents(List<Player> players) {
        List<Event> events = collectEvents(bottomRow);
        resolveEventsSorted(events, players);
        return events;
    }

    /**
     * Collects and resolves all visible events (top and bottom row).
     * Returns the list of resolved events so Game can publish EventResolved for each.
     * Used during the final round (rulebook p.7).
     *
     * @param players list of all players
     * @return list of resolved events
     */
    public List<Event> resolveAllVisibleEvents(List<Player> players) {
        List<Event> events = new ArrayList<>();
        events.addAll(collectEvents(bottomRow));
        events.addAll(collectEvents(topRow));
        resolveEventsSorted(events, players);
        return events;
    }

    private List<Event> collectEvents(List<Card> row) {
        List<Event> events = new ArrayList<>();
        for (Card card : row) {
            if (card instanceof Event event) events.add(event);
        }
        return events;
    }

    /**
     * Sorts and resolves events:
     * 1. Ascending era order (I before II before III)
     * 2. Sustenance always last
     */
    private void resolveEventsSorted(List<Event> events, List<Player> players) {
        events.sort(Comparator
                .comparingInt((Event e) -> e.getEra().ordinal())
                .thenComparingInt(e -> e.getType() == EventType.SUSTENANCE ? 1 : 0));
        for (Event event : events) {
            event.resolve(players);
        }
    }

    // Card removal

    /**
     * Removes a card from the top or bottom row when a player takes it.
     *
     * @param card the card to remove
     * @throws IllegalArgumentException if the card is not present in either row
     */
    public void removeCard(Card card) {
        if (topRow.remove(card)) return;
        if (bottomRow.remove(card)) return;
        throw new IllegalArgumentException("Card not found on the board: ID " + card.getId());
    }

    // Observable state

    /** Returns an unmodifiable view of the top row. */
    public List<Card> getTopRow()    { return List.copyOf(topRow); }

    /** Returns an unmodifiable view of the bottom row. */
    public List<Card> getBottomRow() { return List.copyOf(bottomRow); }

    /** Returns true if the tribe deck is empty. */
    public boolean isTribeDeckEmpty() { return tribeDeck.isEmpty(); }

    /** Returns the number of cards remaining in the tribe deck. */
    public int getTribeDeckSize() { return tribeDeck.size(); }

    public OfferTiles    getOfferTiles()    { return offerTiles; }
    public TurnOrderTile getTurnOrderTile() { return turnOrderTile; }
    public Era           getCurrentEra()    { return currentEra; }
}