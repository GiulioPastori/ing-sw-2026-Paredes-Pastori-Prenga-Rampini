package it.polimi.it.actions;

/**
 * The player places their totem on an offer tile.
 * @param playerName name of the player making this move
 * @param tileLetter letter of the chosen tile
 */
public record PlaceTotemAction(String playerName, char tileLetter)
        implements PlayerAction {}