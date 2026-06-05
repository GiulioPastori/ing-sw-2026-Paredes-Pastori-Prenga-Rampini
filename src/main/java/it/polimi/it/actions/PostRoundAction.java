package it.polimi.it.actions;

/**
 * the player uses the Watchtower building to take one extra card
 * from the top row after all totems have been repositioned.
 * @param playerName name of the acting player
 * @param cardId     ID of the chosen card, or -1 to decline
 */
public record PostRoundAction(String playerName, int cardId)
        implements PlayerAction {}
