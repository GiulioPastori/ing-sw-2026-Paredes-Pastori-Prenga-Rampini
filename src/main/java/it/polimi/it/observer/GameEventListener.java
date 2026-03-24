package it.polimi.it.observer;

/**
 * Listener for events published by Game (Observer pattern).
 *
 * Anything that wants to be notified of game state changes —
 * the controller, a TUI view, a GUI view, a test logger — implements
 * this interface and registers via Game.addListener().
 *
 * The single method receives a sealed GameEvent; the listener uses
 * pattern matching (switch or instanceof) to handle only the types
 * it cares about.
 *
 * Listeners must never throw exceptions or modify game state in response
 * to an event.
 */
@FunctionalInterface
public interface GameEventListener {

    /**
     * Called by Game whenever a significant state change occurs.
     *
     * @param event the domain event that just occurred
     */
    void onEvent(GameEvent event);
}