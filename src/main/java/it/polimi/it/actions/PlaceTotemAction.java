package it.polimi.it.actions;

/**
 * the player places their totem on an offer tile.
 * Carries only the tile letter (a char), which is the natural identifier
 * both for the game rules and for network serialisation.
 * Game resolves the letter to the concrete OfferTile internally.
 *
 * @param playerName name of the acting player
 * @param tileLetter letter of the chosen tile (e.g. 'B', 'C', 'D'...)
 */
public record PlaceTotemAction(String playerName, char tileLetter)
        implements PlayerAction {}