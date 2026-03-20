package it.polimi.it.cards.events;


import it.polimi.it.Board;
import it.polimi.it.Player.Player;
import it.polimi.it.cards.Card;
import it.polimi.it.enums.Era;

import java.util.List;
import java.util.Objects;

/**
 * Classe Event aggiornata per essere compatibile con i vincoli di 'Card'.
 */
public class Event extends Card {

    private final String eventName;
    private final EventStrategy strategy;

    /**
     * Il costruttore ora passa 2 argomenti a super() come richiesto da Card.
     */
    public Event(String eventName, Era era, EventStrategy strategy) {
        super(eventName, era);

        this.eventName = Objects.requireNonNull(eventName);
        this.strategy = Objects.requireNonNull(strategy);
    }

    @Override
    public void applyEffect(Player player, Board board) {
        if (strategy != null && player != null) {
            // Applichiamo l'effetto al singolo giocatore delegando alla strategia
            strategy.applyEffect(player);
        }
    }

    /**
     * Metodo specifico per gli eventi di fine Era che colpiscono tutti i giocatori.
     */
    public void trigger(List<Player> players) {
        if (players != null && !players.isEmpty() && strategy != null) {
            System.out.println("\n--- [ESECUZIONE EVENTO: " + eventName.toUpperCase() + "] ---");
            strategy.apply(players);
        }
    }

    // --- GETTER ---

    public String getEventName() {
        return eventName;
    }

    public EventStrategy getStrategy() {
        return strategy;
    }


    @Override
    public String toString() {
        return "Event{" +
                "name='" + eventName + '\'' +
                ", strategy=" + strategy.getClass().getSimpleName() +
                '}';
    }
}