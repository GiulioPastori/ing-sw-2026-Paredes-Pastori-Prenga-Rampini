package it.polimi.it.cards.building;

import it.polimi.it.Player.Player;
import it.polimi.it.cards.events.CavePaintings;
import it.polimi.it.cards.events.Hunt;
import it.polimi.it.cards.events.ShamanicRitual;
import it.polimi.it.cards.events.SustenanceStrategy;
import it.polimi.it.cards.character.Character;

public interface BuildingStrategy {
    default void onHunt(Player player, Hunt events) {}
    default void onSustenance(Player player, SustenanceStrategy events) {}
    default void onRitual(Player player, ShamanicRitual events) {}
    default void onCavePainting(Player player, CavePaintings events) {}


    default void onNewCharacter(Player player, it.polimi.it.cards.character.Character c) {}

    // Ritorna i punti bonus calcolati a fine partita
    default int endGamePoints(Player player) { return 0; }
}