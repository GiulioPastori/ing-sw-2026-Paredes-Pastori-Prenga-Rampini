package it.polimi.it.observer;

import it.polimi.it.enums.TotemColour;
import it.polimi.it.player.Player;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * Immutable snapshot of a player's visible state at a specific moment.
 * Includes the player's full tribe (characters and buildings acquired so
 * far) so that every view can render every player's tribe without polling
 * the model.
 *
 * Implements { Serializable} so it can be passed inside Message
 * objects over RMI and via Java object serialisation over sockets.
 */
public record PlayerSnapshot(
        String             name,
        TotemColour        totemColour,
        int                food,
        int                prestigePoints,
        int                turnOrder,
        List<CardSnapshot> tribeCharacters,
        List<CardSnapshot> tribeBuildings
) implements Serializable {
    @Serial private static final long serialVersionUID = 2L;

    /** Creates a snapshot of the player in their current state, including the tribe. */
    public static PlayerSnapshot of(Player player) {
        List<CardSnapshot> chars = player.getTribe().getCharacters().stream()
                .map(CardSnapshot::of).toList();
        List<CardSnapshot> blds  = player.getTribe().getBuildings().stream()
                .map(CardSnapshot::of).toList();
        return new PlayerSnapshot(
                player.getName(),
                player.getTotemColour(),
                player.getFood(),
                player.getPrestigePoints(),
                player.getTurnOrder(),
                chars,
                blds);
    }
}