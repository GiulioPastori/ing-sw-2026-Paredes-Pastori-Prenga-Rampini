package it.polimi.it.actions;

/**
 * The player takes a card from the board.
 * @param playerName name of the player making this move
 * @param cardId     unique ID of the chosen card
 * @param fromTopRow true if the card is in the top row
 */
public record TakeCardAction(String playerName, int cardId, boolean fromTopRow)
        implements PlayerAction {}