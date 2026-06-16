package it.polimi.it;

import it.polimi.it.actions.PlayerAction;
import it.polimi.it.observer.GameEvent;
import it.polimi.it.observer.GameEventListener;
import it.polimi.it.validation.ValidationResult;
import it.polimi.it.view.GameView;

/**
 * Game controller (Controller role in MVC).
 *
 * <p>In the client-server architecture, the server instantiates one
 * {@code GameController} per game session (inside {@link it.polimi.it.network.server.GameSession}).
 * Player actions arrive as messages over the network (socket or RMI), are
 * forwarded here via {@link #handle(PlayerAction)}, and the resulting
 * {@link GameEvent}s are published to all registered listeners.
 *
 * <p>Two listeners are registered on the model:
 * <ol>
 *   <li>This controller — handles local view feedback (validation errors, logging).</li>
 *   <li>{@link it.polimi.it.network.server.GameSession} — serialises events and
 *       broadcasts them to connected clients.</li>
 * </ol>
 *
 * <p>The controller is deliberately thin:
 * <ul>
 *   <li>It does not build actions itself (those come from the network layer).</li>
 *   <li>It does not interpret events for remote clients (the network layer does that).</li>
 *   <li>It validates, executes, and shows error feedback via the view if a local
 *       view is present (e.g. {@link it.polimi.it.view.StubGameView} on the server,
 *       or a real TUI/GUI in single-player mode).</li>
 * </ul>
 */
public class GameController implements GameEventListener {

    private final Game     game;
    private final GameView view;

    /**
     * @param game the game model this controller drives
     * @param view the view for local feedback (use {@link it.polimi.it.view.StubGameView}
     *             in pure server mode)
     */
    public GameController(Game game, GameView view) {
        this.game = game;
        this.view = view;
        game.addListener(this);
    }

    // ── Startup ───────────────────────────────────────────────────────────────

    /** Starts the game. Called once setup is complete and all players have joined. */
    public void startGame() {
        game.start();
    }

    // ── Action handling ───────────────────────────────────────────────────────

    /**
     * Receives a player action (from the network layer or from a local view) and
     * applies it to the model.
     *
     * <p>Returns the validation outcome so the network layer can send an error
     * message back to the client if the action was rejected.
     *
     * @param action the action sent by a player
     * @return {@link ValidationResult.Ok} if applied;
     *         {@link ValidationResult.Fail} with a reason string if rejected
     */
    public ValidationResult handle(PlayerAction action) {
        ValidationResult result = game.execute(action);
        if (!result.isOk()) {
            view.showInvalidAction(
                    action.playerName(),
                    ((ValidationResult.Fail) result).reason());
        }
        return result;
    }

    // ── GameEventListener ─────────────────────────────────────────────────────

    /**
     * Forwards game events to the local view.
     *
     * <p>In the full client-server setup, {@link it.polimi.it.network.server.GameSession}
     * is registered as a second listener and handles broadcasting to remote clients.
     * This method is responsible only for local display/logging.
     */
    @Override
    public void onEvent(GameEvent event) {
        switch (event) {
            case GameEvent.RoundStarted             e ->
                    view.showRoundStarted(e.round(), e.turnOrder(), e.board());
            case GameEvent.TotemPlaced              e ->
                    view.showTotemPlaced(e.player(), e.tileLetter(), e.board());
            case GameEvent.CardSlotReady            e ->
                    view.showCardSlotReady(e.player(), e.isTopRow(), e.availableCards(), e.visibleEvents());
            case GameEvent.CardAcquired             e ->
                    view.showCardAcquired(e.player(), e.card());
            case GameEvent.CardAcquisitionFailed    e ->
                    view.showCardAcquisitionFailed(e.player(), e.card(), e.missingFood());
            case GameEvent.TotemReturnedToTurnOrder e ->
                    view.showTotemReturnedToTurnOrder(e.player(), e.position(), e.board());
            case GameEvent.PostRoundStarted         e ->
                    view.showPostRoundStarted(e.watchtowerPlayers(), e.availableCards());
            case GameEvent.EventResolved            e ->
                    view.showEventResolved(e.eventId(), e.era(), e.type(), e.deltas());
            case GameEvent.EraChanged               e -> view.showEraChanged(e.newEra());
            case GameEvent.RoundEnded               e -> view.showRoundEnded(e.round());
            case GameEvent.GameEnded                e ->
                    view.showGameEnded(e.winners(), e.finalScores(), e.scoreBreakdowns());
        }
    }

    /**
     * Exposes the underlying game model.
     * Intended for use by the server layer (e.g. {@link it.polimi.it.network.server.GameSession})
     * for reconnection and disconnection checks only — not for executing actions.
     */
    public Game getGame() { return game; }
}
