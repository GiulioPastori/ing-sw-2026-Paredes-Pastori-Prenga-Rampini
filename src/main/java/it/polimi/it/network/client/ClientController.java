package it.polimi.it.network.client;

import it.polimi.it.network.message.*;
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
 * Bug fix: totem placement now works for all players (not only position 0).
 *
 * Original problem: handleRoundStarted asked for placement only if the player
 * was first in turnOrder. Players in positions 1..N-1 never received the
 * prompt and the game would hang waiting for their PlaceTotemMessage.
 *
 * Fix: a boolean flag {@code placedThisRound} tracks whether this client has
 * already placed in the current round. It is reset to false on every
 * RoundStarted. On every TotemPlaced broadcast (which carries the remaining
 * free tiles), if the flag is still false and there are free tiles left, it
 * means it is now this player's turn to place — so the prompt is shown.
 * The server enforces strict placement order, so receiving a TotemPlaced
 * with remaining tiles while not yet placed unambiguously means "my turn".
 */
public class ClientController {

    private static final Logger LOG = Logger.getLogger(ClientController.class.getName());

    private final ServerProxy proxy;
    private final GameView    view;
    private final String      myNickname;

    /** Released when the game ends or the connection drops. */
    private final CountDownLatch gameLatch = new CountDownLatch(1);

    /** True once this client has placed its totem in the current round. */
    private volatile boolean placedThisRound = false;

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
            case PingMessage ignored            -> {} // handled by the proxy
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
        placedThisRound = false; // reset for the new round
        view.showRoundStarted(m.round(), m.turnOrder());

        // If I am first in the placement order, it is already my turn.
        if (!m.turnOrder().isEmpty()
                && m.turnOrder().get(0).name().equals(myNickname)) {
            requestAndSendTotemPlacement(m.availableTiles());
        }
    }

    /**
     * Called after every totem placement (including other players').
     *
     * The server sends this to all clients each time a totem is placed.
     * {@code remainingTiles} contains tiles still free after this placement.
     *
     * If there are still free tiles AND I have not placed yet, it means it is
     * now my turn (the server enforces strict turn order, so I am next).
     */
    private void handleTotemPlaced(TotemPlacedMessage m) {
        view.showTotemPlaced(m.player(), m.tileLetter());

        if (!placedThisRound
                && !m.remainingTiles().isEmpty()
                && !m.player().name().equals(myNickname)) {
            requestAndSendTotemPlacement(m.remainingTiles());
        }
    }

    private void handleCardSlotReady(CardSlotReadyMessage m) {
        view.showCardSlotReady(m.player(), m.isTopRow(), m.availableCards());

        if (!m.player().name().equals(myNickname)) return;

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

    private void requestAndSendTotemPlacement(List<OfferTileSnapshot> freeTiles) {
        placedThisRound = true;
        // Build a minimal snapshot — the view only needs the name for display
        PlayerSnapshot me = new PlayerSnapshot(myNickname, null, 0, 0, 0);
        char chosen = view.requestOfferTileChoice(me, freeTiles);
        proxy.send(new PlaceTotemMessage(myNickname, chosen));
    }
}