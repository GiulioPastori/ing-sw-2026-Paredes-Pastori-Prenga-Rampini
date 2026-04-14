package it.polimi.it.network.message;

import it.polimi.it.observer.OfferTileSnapshot;
import it.polimi.it.observer.PlayerSnapshot;

import java.util.List;

/** Server → Client: a new round has started. */
public record RoundStartedMessage(
        int                     round,
        List<PlayerSnapshot>    turnOrder,
        List<OfferTileSnapshot> availableTiles
) implements Message {}
