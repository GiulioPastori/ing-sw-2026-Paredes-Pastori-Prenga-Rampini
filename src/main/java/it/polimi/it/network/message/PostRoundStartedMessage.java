package it.polimi.it.network.message;

import it.polimi.it.observer.CardSnapshot;
import it.polimi.it.observer.PlayerSnapshot;

import java.util.List;

/** Server → Client: post-round phase started (Watchtower). */
public record PostRoundStartedMessage(
        int                  round,
        List<PlayerSnapshot> watchtowerPlayers,
        List<CardSnapshot>   availableCards
) implements Message {}
