package it.polimi.it.actions;

/**
 * An action a player can perform during their turn (Command pattern).
 *
 * Sealed interface: the only permitted types are declared with "permits".
 * Each action is an immutable record carrying all the data needed to
 * validate and execute it in Game.
 *
 * The controller constructs an action from user input; Game validates
 * and applies it. Input never enters the model directly.
 */
public sealed interface PlayerAction
        permits PlaceTotemAction, TakeCardAction, PassAction, PostRoundAction {

    /** Returns the name of the player who generated this action. */
    String playerName();
}
