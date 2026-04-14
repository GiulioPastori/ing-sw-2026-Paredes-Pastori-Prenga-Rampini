package it.polimi.it.network.message;

/** Server → Client: action rejected or protocol error. */
public record ErrorMessage(String reason) implements Message {}
