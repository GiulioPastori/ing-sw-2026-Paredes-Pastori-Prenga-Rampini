package it.polimi.it.network.message;

import it.polimi.it.observer.CardSnapshot;
import it.polimi.it.observer.PlayerSnapshot;

import java.util.List;

/** Server → Client: a card slot is ready for the indicated player. */
public record CardSlotReadyMessage(
        PlayerSnapshot     player,
        boolean            isTopRow,
        List<CardSnapshot> availableCards,
        int                slotsRemaining
) implements Message {}
