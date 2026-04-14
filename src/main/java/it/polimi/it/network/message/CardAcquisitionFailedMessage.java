package it.polimi.it.network.message;

import it.polimi.it.observer.CardSnapshot;
import it.polimi.it.observer.PlayerSnapshot;

import java.util.List;

/** Server → Client: a player could not afford a building. */
public record CardAcquisitionFailedMessage(
        PlayerSnapshot     player,
        CardSnapshot       card,
        int                missingFood,
        boolean            fromTopRow,
        List<CardSnapshot> alternatives
) implements Message {}
