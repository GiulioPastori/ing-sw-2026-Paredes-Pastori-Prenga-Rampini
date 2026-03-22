package it.polimi.it;

import it.polimi.it.Player.Player;
import it.polimi.it.cards.building.Building;
import it.polimi.it.cards.building.BuildingFactory;
import it.polimi.it.cards.Card;
import it.polimi.it.cards.events.Event;
import it.polimi.it.enums.Era;
import it.polimi.it.enums.TotemColour;
import it.polimi.it.tiles.OfferTile;
import it.polimi.it.tiles.TurnOrderTile;

import java.util.*;

public class Board {

    private final List<OfferTile> offerTrack;
    private final List<Card> topRow;
    private final List<Card> bottomRow;
    private final Stack<it.polimi.it.cards.character.Character> characterDeck;
    private final Map<Era, Stack<Building>> buildingDecks;
    private final TurnOrderTile turnOrderTile;

    // FIX: Passiamo numPlayers per inizializzare correttamente la TurnOrderTile
    public Board(int numPlayers) {
        this.offerTrack = new ArrayList<>();
        this.topRow = new ArrayList<>();
        this.bottomRow = new ArrayList<>();
        this.characterDeck = new Stack<>();
        this.buildingDecks = new HashMap<>();

        // FIX: La TurnOrderTile ha bisogno di sapere quanti sono i giocatori
        this.turnOrderTile = new TurnOrderTile(numPlayers);

        for (Era era : Era.values()) {
            buildingDecks.put(era, new Stack<>());
        }

        // AGGIUNTA: Inizializziamo le tessere offerta base (A, B, C, D...)
        setupOfferTiles(numPlayers);
    }

    /**
     * Crea le tessere offerta in base al numero di giocatori.
     */
    private void setupOfferTiles(int n) {
        // Esempio standard: Tessera A (isFoodOnly), B, C...
        offerTrack.add(new OfferTile('A', 2, 0, 0, 3, true));  // Solo cibo
        offerTrack.add(new OfferTile('B', 2, 0, 1, 0, false)); // 1 carta sotto
        offerTrack.add(new OfferTile('C', 2, 1, 0, 0, false)); // 1 carta sopra
        if (n >= 3) offerTrack.add(new OfferTile('D', 3, 1, 1, 0, false));
        if (n >= 4) offerTrack.add(new OfferTile('E', 4, 2, 0, 0, false));
    }

    public void setupBuildingDecks() {
        for (int i = 1; i <= 6; i++) buildingDecks.get(Era.ERA_I).push(BuildingFactory.create(i, Era.ERA_I));
        for (int i = 1; i <= 7; i++) buildingDecks.get(Era.ERA_II).push(BuildingFactory.create(i, Era.ERA_II));
        for (int i = 1; i <= 8; i++) buildingDecks.get(Era.ERA_III).push(BuildingFactory.create(i, Era.ERA_III));

        buildingDecks.values().forEach(Collections::shuffle);
    }

    public void clearBottomRow() {
        this.bottomRow.clear();
    }

    public void moveTopToBottom() {
        bottomRow.addAll(topRow);
        topRow.clear();
    }

    public void refillTopRow(int numPlayers, Era currentEra) {
        int targetSize = numPlayers + 4;
        Stack<Building> currentEraBuildings = buildingDecks.get(currentEra);

        while (topRow.size() < targetSize) {
            // Controlliamo che i mazzi non siano null o vuoti
            if (!characterDeck.isEmpty() && (Math.random() > 0.5 || currentEraBuildings == null || currentEraBuildings.isEmpty())) {
                topRow.add(characterDeck.pop());
            } else if (currentEraBuildings != null && !currentEraBuildings.isEmpty()) {
                topRow.add(currentEraBuildings.pop());
            } else {
                break;
            }
        }
    }

    public void takeCard(Card card, Player player) {
        topRow.remove(card);
        bottomRow.remove(card);
    }

    public void resolveEvents(List<Player> players) {
        for (Card c : new ArrayList<>(bottomRow)) {
            if (c instanceof Event event) {
                // FIX: Assicurati che il metodo in Event si chiami applyEffect o apply
                event.applyEffect((Player) players,this);
                bottomRow.remove(c);
            }
        }
    }

    public void placeTotem(TotemColour totem, OfferTile chosen) {
        if (chosen != null) {
            chosen.placeTotem(totem);
        }
    }

    public List<OfferTile> getAvailableOfferTiles() {
        return offerTrack.stream().filter(OfferTile::isFree).toList();
    }

    public void resolveEndOfGameEvents(List<Player> players) {
        resolveEvents(players);
        for (Card c : new ArrayList<>(topRow)) {
            if (c instanceof Event event) event.applyEffect((Player) players,this);
        }
    }

    // GETTERS
    public List<OfferTile> getOfferTrack() { return offerTrack; }
    public TurnOrderTile getTurnOrderTile() { return turnOrderTile; }
    public List<Card> getTopRow() { return topRow; }
    public List<Card> getBottomRow() { return bottomRow; }
    public Stack<it.polimi.it.cards.character.Character> getCharacterDeck() { return characterDeck; }
}
