package it.polimi.it.network.message;

import java.util.List;

/** Server → Client: lobby status update. */
public record LobbyUpdateMessage(
        List<String> connectedPlayers,
        int          expectedPlayers
) implements Message {}
