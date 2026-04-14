package it.polimi.it.network.message;

import it.polimi.it.observer.PlayerSnapshot;

import java.util.List;

/** Server → Client: the game is over. */
public record GameEndedMessage(
        List<PlayerSnapshot> winners,
        List<PlayerSnapshot> finalScores
) implements Message {}
