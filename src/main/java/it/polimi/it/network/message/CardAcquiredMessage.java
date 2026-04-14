package it.polimi.it.network.message;

import it.polimi.it.observer.CardSnapshot;
import it.polimi.it.observer.PlayerSnapshot;

/** Server → Client: a player acquired a card. */
public record CardAcquiredMessage(
        PlayerSnapshot player,
        CardSnapshot   card
) implements Message {}
