package it.polimi.it.network.message;

/** Server → Client: a player disconnected; game is terminated. */
public record PlayerDisconnectedMessage(String nickname) implements Message {}
