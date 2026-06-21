package it.polimi.it.actions;

/**
 * The player skips the current card slot instead of acquiring a card.
 * A pass is a legal action whenever it is that player's turn to resolve a
 * card slot; the model consumes the current top-row slot first, otherwise
 * the current bottom-row slot.
 *
 * @param playerName name of the player making this move
 */
public record PassAction(String playerName) implements PlayerAction {}
