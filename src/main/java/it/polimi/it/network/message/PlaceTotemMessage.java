package it.polimi.it.network.message;

/**
 * Client → Server: player places their totem on an offer tile.
 *
 * @param playerName name of the acting player
 * @param tileLetter letter of the chosen tile (e.g. 'B')
 */
public record PlaceTotemMessage(String playerName, char tileLetter) implements Message {}
