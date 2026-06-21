package it.polimi.it.actions;

/**
 * The player skips a card slot. This can happen for two reasons:
 * the row has no cards left, or the player cannot pay for any of the
 * buildings on offer.
 *
 * <p>Voluntary passes (the player simply not wanting to take a card) are
 * <strong>not</strong> allowed by the rules: if cards are available the
 * player must take one. {@link it.polimi.it.validation.ActionValidator}
 * enforces this constraint server-side.
 *
 * @param playerName name of the player making this move
 */
public record PassAction(String playerName) implements PlayerAction {}