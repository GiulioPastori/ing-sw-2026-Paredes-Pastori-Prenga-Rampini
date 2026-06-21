package it.polimi.it.observer;

/** Lets something listen to what happens in the game (Observer pattern).

 * Game does not know about the network or the views. When the game state
 * changes, Game just sends a { GameEvent} to everyone who registered with
 * {Game.addListener()}.

 * A listener should only read and react (send a message, update the screen,
 * log). It must never throw an exception or change the game state. */

@FunctionalInterface
public interface GameEventListener {

    /** Called by Game whenever a significant state change occurs.
     event=the domain event that just occurred */
    void onEvent(GameEvent event);
}