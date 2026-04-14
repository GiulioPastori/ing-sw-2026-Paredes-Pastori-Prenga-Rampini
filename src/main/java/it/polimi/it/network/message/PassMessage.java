package it.polimi.it.network.message;

/**
 * Client → Server: player passes their card slot.
 *
 * @param playerName name of the acting player
 */
public record PassMessage(String playerName) implements Message {}
