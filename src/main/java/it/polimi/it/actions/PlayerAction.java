package it.polimi.it.actions;

/**
 * An action that a player can perform during their turn (Command pattern).
 * The controller builds an action from what the user does; Game then
 * checks it and applies it. User input never reaches the model on its own.
 */
public sealed interface PlayerAction
        permits PlaceTotemAction, TakeCardAction, PassAction, PostRoundAction {

    /** Returns the name of the player who generated this action. */
    String playerName();
}
