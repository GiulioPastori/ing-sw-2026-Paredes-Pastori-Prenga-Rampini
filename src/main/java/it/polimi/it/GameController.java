package it.polimi.it;

import it.polimi.it.actions.PlayerAction;
import it.polimi.it.observer.GameEvent;
import it.polimi.it.observer.GameEventListener;
import it.polimi.it.validation.ValidationResult;
import it.polimi.it.view.GameView;

/**
 * Game controller (Controller role in MVC).
 *
 * In the client-server architecture, the server instantiates one
 * GameController per game. Player actions arrive as messages over the
 * network (socket or RMI), are forwarded to {@link #handle(PlayerAction)},
 * and the resulting GameEvents are published to all registered listeners
 * (which include the network layer that broadcasts them to the clients).
 *
 * The controller is deliberately thin:
 * - It does not build actions itself (those come from the network).
 * - It does not interpret events (the view on each client does that).
 * - It validates and forwards, and shows error feedback via the view
 *   if a local view is present (e.g. for single-player or testing).
 */
public class GameController implements GameEventListener {

    private final Game     game;
    private final GameView view;

    /**
     * @param game the game model this controller drives
     * @param view the view for local feedback (may be a stub in pure server mode)
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
     * Receives a player action (from the network or from a local view) and
     * applies it to the model.
     *
     * Returns the validation outcome so the caller (server layer) can send
     * an error message back to the client if the action was rejected.
     *
     * @param action the action sent by a player
     * @return Ok if applied; Fail with a reason string if rejected
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
     * In the full client-server setup, a separate network listener (registered
     * alongside this controller) serialises and broadcasts these same events to
     * all connected clients. This controller only handles local display.
     */
    @Override
    public void onEvent(GameEvent event) {
        switch (event) {
            case GameEvent.RoundStarted             e ->
                    view.showRoundStarted(e.round(), e.turnOrder());
            case GameEvent.TotemPlaced              e ->
                    view.showTotemPlaced(e.player(), e.tileLetter());
            case GameEvent.CardSlotReady            e ->
                    view.showCardSlotReady(e.player(), e.isTopRow(), e.availableCards());
            case GameEvent.CardAcquired             e ->
                    view.showCardAcquired(e.player(), e.card());
            case GameEvent.CardAcquisitionFailed    e ->
                    view.showCardAcquisitionFailed(e.player(), e.card(), e.missingFood());
            case GameEvent.TotemReturnedToTurnOrder e ->
                    view.showTotemReturnedToTurnOrder(e.player(), e.position());
            case GameEvent.PostRoundStarted         e ->
                    view.showPostRoundStarted(e.watchtowerPlayers(), e.availableCards());
            case GameEvent.EventResolved            e ->
                    view.showEventResolved(e.eventId(), e.era(), e.type());
            case GameEvent.EraChanged               e -> view.showEraChanged(e.newEra());
            case GameEvent.RoundEnded               e -> view.showRoundEnded(e.round());
            case GameEvent.GameEnded                e ->
                    view.showGameEnded(e.winners(), e.finalScores());
        }
    }

    public Game getGame() { return game; }
}