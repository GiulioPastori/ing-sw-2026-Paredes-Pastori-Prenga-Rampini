package it.polimi.it.actions;

/**
 * the player skips a card slot — either because the row is empty,
 * they cannot afford any available building, or they choose not to take.
 * @param playerName name of the acting player
 */
public record PassAction(String playerName) implements PlayerAction {}
