package it.polimi.it.network.message;

/**
 * Client → Server: player takes a card from the board.
 *
 * @param playerName name of the acting player
 * @param cardId     unique ID of the chosen card
 * @param fromTopRow true if the card is in the top row
 */
public record TakeCardMessage(String playerName, int cardId, boolean fromTopRow)
        implements Message {}
