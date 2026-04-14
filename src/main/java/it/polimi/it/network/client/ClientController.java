package it.polimi.it.network.client;

import it.polimi.it.network.message.*;
        import it.polimi.it.observer.CardSnapshot;
import it.polimi.it.observer.OfferTileSnapshot;
import it.polimi.it.observer.PlayerSnapshot;
import it.polimi.it.view.GameView;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.logging.Logger;

/**
 * Client-side controller (Controller role in MVC on the client).
 *
 * Receives Messages from the server via {@link #onMessage(Message)},
 * updates the view with show* calls, and when it is this player's turn
 * asks the view for input with request* calls, then sends the resulting
 * action message back to the server via the proxy.
 *
 * The controller is driven entirely by incoming server messages — it never
 * polls or initiates actions on its own.
 *
 * Thread safety: onMessage() is called from the proxy reader thread.
 * View.show* calls are safe to call from any thread (TUI prints to stdout,
 * GUI dispatches to EDT). View.request* calls block until input is ready.
 */
public class ClientController {

    private static final Logger LOG = Logger.getLogger(ClientController.class.getName());

    private final ServerProxy proxy;
    private final GameView    view;
    private final String      myNickname;

    /** Released when the game ends or the connection drops. */
    private final CountDownLatch gameLatch = new CountDownLatch(1);

    public ClientController(ServerProxy proxy, GameView view, String myNickname) {
        this.proxy      = proxy;
        this.view       = view;
        this.myNickname = myNickname;
    }

    /**
     * Blocks the calling thread until the game ends or the connection drops.
     * Call this after {@link ServerProxy#connect} to keep the main thread alive.
     */
    public void run() {
        try {
            gameLatch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    // ── Incoming message dispatch ─────────────────────────────────────────────

    /**
     * Called by the proxy reader thread for every inbound message.
     * Routes to the appropriate handler.
     *
     * @param message the message received from the server
     */
    public void onMessage(Message message) {
        switch (message) {
            case LobbyUpdateMessage m           -> handleLobbyUpdate(m);
            case ErrorMessage m                 -> handleError(m);
            case RoundStartedMessage m          -> handleRoundStarted(m);
            case TotemPlacedMessage m           -> handleTotemPlaced(m);
            case CardSlotReadyMessage m         -> handleCardSlotReady(m);
            case CardAcquiredMessage m          -> handleCardAcquired(m);
            case CardAcquisitionFailedMessage m -> handleCardAcquisitionFailed(m);
            case TotemReturnedMessage m         -> handleTotemReturned(m);
            case PostRoundStartedMessage m      -> handlePostRoundStarted(m);
            case EventResolvedMessage m         -> handleEventResolved(m);
            case EraChangedMessage m            -> handleEraChanged(m);
            case RoundEndedMessage m            -> handleRoundEnded(m);
            case GameEndedMessage m             -> handleGameEnded(m);
            case PlayerDisconnectedMessage m    -> handlePlayerDisconnected(m);
            case PingMessage ignored            -> {} // handled by proxy
            case PongMessage ignored            -> {}
            default -> LOG.warning("Unknown message: " + message.getClass().getSimpleName());
        }
    }

    /** Called by the proxy when the connection to the server drops. */
    public void onDisconnect() {
        view.showInvalidAction("SERVER", "Connection to server lost.");
        gameLatch.countDown();
    }

    // ── Handlers ──────────────────────────────────────────────────────────────

    private void handleLobbyUpdate(LobbyUpdateMessage m) {
        System.out.println("Lobby: " + m.connectedPlayers().size()
                + "/" + m.expectedPlayers() + " — " + m.connectedPlayers());
    }

    private void handleError(ErrorMessage m) {
        view.showInvalidAction("SERVER", m.reason());
    }

    private void handleRoundStarted(RoundStartedMessage m) {
        view.showRoundStarted(m.round(), m.turnOrder());

        // If I am first in the placement order, request my tile choice
        if (!m.turnOrder().isEmpty()
                && m.turnOrder().get(0).name().equals(myNickname)) {
            requestAndSendTotemPlacement(m.availableTiles());
        }
    }

    private void handleTotemPlaced(TotemPlacedMessage m) {
        view.showTotemPlaced(m.player(), m.tileLetter());

        // If there are still free tiles and I haven't placed yet, ask me
        if (!m.remainingTiles().isEmpty()) {
            boolean iAlreadyPlaced = m.remainingTiles().stream()
                    .noneMatch(t -> false); // server drives the order;
            // The server will send CardSlotReady when it is my turn to act.
            // We only need to place if we receive a RoundStarted with our name first.
        }
    }

    private void handleCardSlotReady(CardSlotReadyMessage m) {
        view.showCardSlotReady(m.player(), m.isTopRow(), m.availableCards());

        if (!m.player().name().equals(myNickname)) return; // not my turn

        int chosen = view.requestCardChoice(m.player(), m.availableCards());
        if (chosen == -1) {
            proxy.send(new PassMessage(myNickname));
        } else {
            proxy.send(new TakeCardMessage(myNickname, chosen, m.isTopRow()));
        }
    }

    private void handleCardAcquired(CardAcquiredMessage m) {
        view.showCardAcquired(m.player(), m.card());
    }

    private void handleCardAcquisitionFailed(CardAcquisitionFailedMessage m) {
        view.showCardAcquisitionFailed(m.player(), m.card(), m.missingFood());

        if (!m.player().name().equals(myNickname)) return;

        // Ask to choose an alternative or pass
        int chosen = view.requestCardChoice(m.player(), m.alternatives());
        if (chosen == -1) {
            proxy.send(new PassMessage(myNickname));
        } else {
            proxy.send(new TakeCardMessage(myNickname, chosen, m.fromTopRow()));
        }
    }

    private void handleTotemReturned(TotemReturnedMessage m) {
        view.showTotemReturnedToTurnOrder(m.player(), m.position());
    }

    private void handlePostRoundStarted(PostRoundStartedMessage m) {
        view.showPostRoundStarted(m.watchtowerPlayers(), m.availableCards());

        boolean iAmWatchtower = m.watchtowerPlayers().stream()
                .anyMatch(p -> p.name().equals(myNickname));
        if (!iAmWatchtower) return;

        PlayerSnapshot me = m.watchtowerPlayers().stream()
                .filter(p -> p.name().equals(myNickname))
                .findFirst().orElseThrow();

        int chosen = view.requestPostRoundCardChoice(me, m.availableCards());
        proxy.send(new PostRoundMessage(myNickname, chosen));
    }

    private void handleEventResolved(EventResolvedMessage m) {
        view.showEventResolved(m.eventId(), m.era(), m.type());
    }

    private void handleEraChanged(EraChangedMessage m) {
        view.showEraChanged(m.newEra());
    }

    private void handleRoundEnded(RoundEndedMessage m) {
        view.showRoundEnded(m.round());
    }

    private void handleGameEnded(GameEndedMessage m) {
        view.showGameEnded(m.winners(), m.finalScores());
        gameLatch.countDown();
    }

    private void handlePlayerDisconnected(PlayerDisconnectedMessage m) {
        view.showInvalidAction("DISCONNECT", m.nickname());
        gameLatch.countDown();
    }

    // ── Totem placement ───────────────────────────────────────────────────────

    /**
     * Asks the view for a tile choice and sends it to the server.
     * Called when RoundStarted indicates it is my turn to place first.
     */
    private void requestAndSendTotemPlacement(List<OfferTileSnapshot> freeTiles) {
        // Find my snapshot (first in turn order)
        PlayerSnapshot me = new PlayerSnapshot(myNickname,
                null, 0, 0, 1); // lightweight snapshot just for the view call
        char chosen = view.requestOfferTileChoice(me, freeTiles);
        proxy.send(new PlaceTotemMessage(myNickname, chosen));
    }
}