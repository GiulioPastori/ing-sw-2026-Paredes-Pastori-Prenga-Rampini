package it.polimi.it.actions;

/**
 * Action: the player takes a card from the board.
 *
 * Carries the card ID (integer) and which row it belongs to.
 * The ID is the natural network key — compact, unambiguous, and directly
 * corresponding to the card's unique identifier in the model.
 *
 * @param playerName name of the acting player
 * @param cardId     unique ID of the chosen card
 * @param fromTopRow true if the card is in the top row
 */
public record TakeCardAction(String playerName, int cardId, boolean fromTopRow)
        implements PlayerAction {}