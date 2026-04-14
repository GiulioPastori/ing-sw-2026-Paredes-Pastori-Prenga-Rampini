package it.polimi.it.network.message;

/**
 * Client → Server: player wants to join a game session.
 *
 * @param nickname    chosen nickname (must be unique on the server)
 * @param numPlayers  desired number of players — used only if this player
 *                    creates a new session; ignored by subsequent joiners
 */
public record JoinMessage(String nickname, int numPlayers) implements Message {}
