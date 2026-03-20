package it.polimi.it;

import it.polimi.it.Player.Player;
import it.polimi.it.cards.building.Building;
import it.polimi.it.cards.building.BuildingFactory;
import it.polimi.it.cards.Card;
import it.polimi.it.cards.events.Event;
import it.polimi.it.enums.Era;
import it.polimi.it.tiles.OfferTile;
import it.polimi.it.tiles.TurnOrderTile; // Assicurati di avere questa classe

import java.util.*;

public class Board {

    private final List<OfferTile> offerTrack;
    private final List<Card> topRow;
    private final List<Card> bottomRow;

    // Mazzi separati per tipologia ed Era
    private final Stack<it.polimi.it.cards.character.Character> characterDeck;
    private final Map<Era, Stack<Building>> buildingDecks;
    private final TurnOrderTile turnOrderTile;

    public Board() {
        this.offerTrack = new ArrayList<>();
        this.topRow = new ArrayList<>();
        this.bottomRow = new ArrayList<>();
        this.characterDeck = new Stack<>();
        this.buildingDecks = new HashMap<>();
        this.turnOrderTile = new TurnOrderTile(); // Inizializza la tessera ordine di turno

        for (Era era : Era.values()) {
            buildingDecks.put(era, new Stack<>());
        }
    }

    /**
     * Popola i mazzi degli edifici usando la Factory che abbiamo scritto.
     */
    public void setupBuildingDecks() {
        // Esempio: carichiamo i 21 edifici totali
        for (int i = 1; i <= 6; i++) buildingDecks.get(Era.ERA_I).push(BuildingFactory.create(i, Era.ERA_I));
        for (int i = 1; i <= 7; i++) buildingDecks.get(Era.ERA_II).push(BuildingFactory.create(i, Era.ERA_II));
        for (int i = 1; i <= 8; i++) buildingDecks.get(Era.ERA_III).push(BuildingFactory.create(i, Era.ERA_III));

        // Mescoliamo i mazzi
        buildingDecks.values().forEach(Collections::shuffle);
    }

    /**
     * Svuota la riga inferiore a fine round.
     */
    public void clearBottomRow() {
        this.bottomRow.clear();
    }

    /**
     * Sposta le carte superstiti dalla riga superiore a quella inferiore.
     */
    public void moveTopToBottom() {
        bottomRow.addAll(topRow);
        topRow.clear();
    }

    /**
     * Riempie la riga superiore: numPlayers + 4 carte.
     * Mescola Personaggi ed Edifici dell'era corrente.
     */
    public void refillTopRow(int numPlayers, Era currentEra) {
        int targetSize = numPlayers + 4;
        Stack<Building> currentEraBuildings = buildingDecks.get(currentEra);

        while (topRow.size() < targetSize) {
            // Alterna o pesca casualmente tra personaggi ed edifici se i mazzi non sono vuoti
            if (!characterDeck.isEmpty() && (Math.random() > 0.5 || currentEraBuildings.isEmpty())) {
                topRow.add(characterDeck.pop());
            } else if (!currentEraBuildings.isEmpty()) {
                topRow.add(currentEraBuildings.pop());
            } else {
                break; // Mazzi esauriti
            }
        }
    }

    /**
     * Rimuove la carta dal tabellone quando un giocatore la prende.
     */
    public void takeCard(Card card, Player player) {
        topRow.remove(card);
        bottomRow.remove(card);
        // L'aggiunta alla tribù e l'applicazione effetti sono già gestite nel Game.takeCard()
    }

    /**
     * Risolve gli eventi presenti nella riga inferiore.
     */
    public void resolveEvents(List<Player> players) {
        for (Card c : new ArrayList<>(bottomRow)) {
            if (c instanceof Event event) {
                event.apply(players);
                bottomRow.remove(c);
            }
        }
    }

    // --- GETTERS ---

    public List<OfferTile> getOfferTrack() {
        return offerTrack;
    }

    public TurnOrderTile getTurnOrderTile() {
        return turnOrderTile;
    }

    public List<Card> getTopRow() {
        return topRow;
    }

    public List<Card> getBottomRow() {
        return bottomRow;
    }

    public void placeTotem(it.polimi.it.enums.Totem totem, OfferTile chosen) {
        chosen.placeTotem(totem);
    }

    public List<OfferTile> getAvailableOfferTiles() {
        return offerTrack.stream().filter(OfferTile::isFree).toList();
    }

    public void resolveEndOfGameEvents(List<Player> players) {
        // Nel round finale si risolvono tutti gli eventi rimasti in entrambe le righe
        resolveEvents(players);
        for (Card c : new ArrayList<>(topRow)) {
            if (c instanceof Event event) event.apply(players);
        }
    }
}
