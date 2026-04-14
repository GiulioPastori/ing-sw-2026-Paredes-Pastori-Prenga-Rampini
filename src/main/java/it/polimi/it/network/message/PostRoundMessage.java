package it.polimi.it.network.message;

/**
 * Client → Server: player uses or declines the Watchtower post-round action.
 *
 * @param playerName name of the acting player
 * @param cardId     ID of the chosen card, or -1 to decline
 */
public record PostRoundMessage(String playerName, int cardId) implements Message {}
