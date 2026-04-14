package it.polimi.it.network.message;

import it.polimi.it.observer.PlayerSnapshot;

/** Server → Client: a player's totem was returned to the Turn Order Tile. */
public record TotemReturnedMessage(
        PlayerSnapshot player,
        int            position
) implements Message {}
