package it.polimi.it.network.message;

/** Server → Client: a round has ended. */
public record RoundEndedMessage(int round) implements Message {}
