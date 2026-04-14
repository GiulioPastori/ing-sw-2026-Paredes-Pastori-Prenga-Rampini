package it.polimi.it.network.message;

/** Client → Server: heartbeat reply. */
public record PongMessage() implements Message {}
