package it.polimi.it.actions;

/**
 * The player skips a card slot. This can happen for three reasons:
 * the row has no cards left, the player cannot pay for any of the
 * buildings on offer, or the player simply does not want to take a card.
 *
 * @param playerName name of the player making this move
 */
public record PassAction(String playerName) implements PlayerAction {}
