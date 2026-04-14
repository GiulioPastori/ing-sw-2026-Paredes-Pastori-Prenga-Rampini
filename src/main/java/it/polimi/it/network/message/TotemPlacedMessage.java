package it.polimi.it.network.message;

import it.polimi.it.observer.OfferTileSnapshot;
import it.polimi.it.observer.PlayerSnapshot;

import java.util.List;

/** Server → Client: a player placed their totem. */
public record TotemPlacedMessage(
        PlayerSnapshot          player,
        char                    tileLetter,
        List<OfferTileSnapshot> remainingTiles
) implements Message {}
