package it.polimi.it.network.server;

import it.polimi.it.GameBuilder;
import it.polimi.it.GameController;
import it.polimi.it.actions.*;
import it.polimi.it.enums.TotemColour;
import it.polimi.it.network.message.Message;
import it.polimi.it.observer.GameEvent;
import it.polimi.it.observer.GameEventListener;
import it.polimi.it.validation.ValidationResult;
import it.polimi.it.view.StubGameView;

import java.util.*;
import java.util.concurrent.*;
import java.util.logging.Logger;

/**
 * One active game session on the server.
 *
 * <p>Responsible only for the <em>network</em> layer:
 * managing connections, routing messages to the controller, and
 * broadcasting GameEvents (received from the model via the controller)
 * to all connected clients.
 *
 * <p>All game logic passes through {@link GameController}, which is the
 * proper MVC controller. This class registers itself as a
 * {@link GameEventListener} <em>alongside</em> the controller so it can
 * serialise and broadcast events; the controller handles local view feedback
 * (validation errors, logging via {@link StubGameView}).
 */
public class GameSession implements GameEventListener {

    private static final Logger LOG             = Logger.getLogger(GameSession.class.getName());
    private static final long   TIMEOUT_SECONDS = 120;

    private final int                           expectedPlayers;
    private final List<ClientConnection>        clients   = new CopyOnWriteArrayList<>();
    private final Map<String, ClientConnection> clientMap = new ConcurrentHashMap<>();

    /** The MVC controller — the single point of entry for all game actions. */
    private GameController controller;

    private boolean started    = false;
    private boolean terminated = false;
    private boolean suspended  = false;

    private final ScheduledExecutorService scheduler =
            Executors.newSingleThreadScheduledExecutor();
    private ScheduledFuture<?> timeoutFuture;

    public GameSession(int expectedPlayers) {
        this.expectedPlayers = expectedPlayers;
    }

    // ── Connection ────────────────────────────────────────────────────────────

    public synchronized void addClient(ClientConnection c) {
        if (terminated || started) return;
        clients.add(c);
        clientMap.put(c.getNickname(), c);
        LOG.info("'" + c.getNickname() + "' joined (" + clients.size() + "/" + expectedPlayers + ")");
        broadcast(Message.lobbyUpdate(
                clients.stream().map(ClientConnection::getNickname).toList(),
                expectedPlayers));
        if (clients.size() == expectedPlayers) startGame();
    }

    public synchronized void reconnectClient(ClientConnection c) {
        if (terminated) return;
        String nick = c.getNickname();
        ClientConnection old = clientMap.get(nick);
        // Replace old handler in both collections atomically (synchronized).
        // The old handler may still be alive (watchdog), but once removed from
        // clientMap its subsequent onPlayerDisconnected call will be rejected
        // by the identity check (source != current).
        if (old != null) { clients.remove(old); old.close(); }
        clients.add(c);
        clientMap.put(nick, c);
        if (timeoutFuture != null) { timeoutFuture.cancel(false); timeoutFuture = null; }
        if (controller != null) controller.getGame().markReconnected(nick);
        boolean wasSuspended = suspended;
        suspended = false;
        LOG.info("RECONNECT nick=" + nick
                + " handler=" + System.identityHashCode(c));
        c.send(Message.playerReconnected(nick));
        broadcast(Message.playerReconnected(nick));
        if (wasSuspended) {
            LOG.info("Game resumed after reconnection of '" + nick + "'.");
            broadcast(Message.error("Partita ripresa — " + nick + " si è riconnesso."));
        }
        // Resync the reconnecting client with current game state.
        if (controller != null && !controller.getGame().isGameOver()) {
            var game = controller.getGame();
            c.send(Message.roundStarted(
                    game.getCurrentRound(),
                    game.getTurnOrderSnapshots(),
                    game.getBoardSnapshot()));
        }
    }

    public synchronized boolean hasRoom()      { return !started && !terminated && clients.size() < expectedPlayers; }
    public synchronized boolean isStarted()    { return started; }
    public synchronized boolean isTerminated() { return terminated; }
    public synchronized boolean isSuspended()  { return suspended; }
    public int getExpectedPlayers()            { return expectedPlayers; }
    public synchronized boolean hasPlayer(String nick) { return clientMap.containsKey(nick); }

    /**
     * Number of players currently connected (not marked disconnected in the model).
     * Uses clientMap — always holds the CURRENT handler per nickname, never stale ones.
     */
    private long connectedCount() {
        if (controller == null) return clientMap.size();
        return clients.stream()
                .filter(c -> !controller.getGame().isDisconnected(c.getNickname()))
                .count();
    }

    // ── Game startup ──────────────────────────────────────────────────────────

    private void startGame() {
        try {
            List<String>      names   = new ArrayList<>();
            List<TotemColour> colours = new ArrayList<>();
            List<TotemColour> used    = new ArrayList<>();
            TotemColour[]     all     = TotemColour.values();
            for (ClientConnection cl : clients) {
                names.add(cl.getNickname());
                TotemColour preferred = cl.getTotemColour();
                if (preferred == null || used.contains(preferred)) {
                    // Fallback: pick first unused colour
                    for (TotemColour tc : all) {
                        if (!used.contains(tc)) { preferred = tc; break; }
                    }
                }
                colours.add(preferred);
                used.add(preferred);
            }

            var game = new GameBuilder()
                    .withPlayers(names, colours)
                    .withCards(ServerApp.getCardList())
                    .build();

            // Build the MVC controller with a StubGameView for server-side
            // feedback (validation error logs). Register this session as a
            // SECOND listener so it can broadcast events over the network.
            controller = new GameController(game, new StubGameView());
            game.addListener(this); // network broadcaster

            started = true;
            LOG.info("Game started with " + expectedPlayers + " players.");

            // Start via the controller, not directly on the game.
            controller.startGame();

        } catch (Exception e) {
            LOG.severe("Failed to start: " + e.getMessage());
            terminate("Server error starting the game.");
        }
    }

    // ── Message handling ──────────────────────────────────────────────────────

    /**
     * Routes an incoming client message to the {@link GameController}.
     *
     * <p>If the action is rejected (invalid), the controller already logs it
     * via the {@link StubGameView}; we additionally send an error message back
     * to the client that sent the bad action.
     */
    public synchronized void handleMessage(Message msg, ClientConnection sender) {
        // HEARTBEAT/PONG/PING: aggiornano solo il timestamp nel handler (già fatto dal reader),
        // non devono produrre azioni di gioco né messaggi di errore.
        if (Message.HEARTBEAT.equals(msg.getType())
                || Message.PONG.equals(msg.getType())
                || Message.PING.equals(msg.getType())) {
            return;
        }

        if (!started || terminated || suspended || controller == null) return;

        PlayerAction action = switch (msg.getType()) {
            case Message.PLACE_TOTEM ->
                    new PlaceTotemAction(msg.str("playerName"), msg.ch("tileLetter"));
            case Message.TAKE_CARD ->
                    new TakeCardAction(msg.str("playerName"), msg.num("cardId"), msg.bool("fromTopRow"));
            case Message.PASS ->
                    new PassAction(msg.str("playerName"));
            case Message.POST_ROUND ->
                    new PostRoundAction(msg.str("playerName"), msg.num("cardId"));
            default -> {
                sender.send(Message.error("Messaggio inatteso: " + msg.getType()));
                yield null;
            }
        };

        if (action == null) return;

        try {
            // Delegate entirely to the controller — it validates, executes, and
            // calls the view if something went wrong.
            ValidationResult result = controller.handle(action);
            if (!result.isOk()) {
                sender.send(Message.error(((ValidationResult.Fail) result).reason()));
            }
        } catch (Exception e) {
            LOG.severe("Eccezione non gestita durante handle(" + action.getClass().getSimpleName()
                    + ") da '" + action.playerName() + "': " + e);
            for (StackTraceElement el : e.getStackTrace()) {
                LOG.severe("  at " + el);
            }
            sender.send(Message.error("Errore interno del server: " + e.getMessage()));
        }
    }

    // ── GameEventListener (network broadcaster) ───────────────────────────────

    /**
     * Converts model events to network messages and broadcasts them to all clients.
     *
     * <p>This listener is registered on the game <em>in addition to</em> the
     * controller's own listener. The controller handles local view feedback;
     * this method handles network serialisation.
     */
    @Override
    public void onEvent(GameEvent event) {
        Message msg = switch (event) {
            case GameEvent.RoundStarted e ->
                    Message.roundStarted(e.round(), e.turnOrder(), e.board());
            case GameEvent.TotemPlaced e ->
                    Message.totemPlaced(e.player(), e.tileLetter(), e.board());
            case GameEvent.CardSlotReady e ->
                    Message.cardSlotReady(e.player(), e.isTopRow(),
                            e.availableCards(), e.visibleEvents(), e.slotsRemaining());
            case GameEvent.CardAcquired e ->
                    Message.cardAcquired(e.player(), e.card());
            case GameEvent.CardAcquisitionFailed e ->
                    Message.cardAcquisitionFailed(e.player(), e.card(),
                            e.missingFood(), e.fromTopRow(), e.alternatives());
            case GameEvent.TotemReturnedToTurnOrder e ->
                    Message.totemReturned(e.player(), e.position(), e.board());
            case GameEvent.PostRoundStarted e ->
                    Message.postRoundStarted(e.round(), e.watchtowerPlayers(), e.availableCards());
            case GameEvent.EventResolved e ->
                    Message.eventResolved(e.eventId(), e.era(), e.type());
            case GameEvent.EraChanged e ->
                    Message.eraChanged(e.newEra());
            case GameEvent.RoundEnded e ->
                    Message.roundEnded(e.round());
            case GameEvent.GameEnded e -> {
                terminated = true;
                scheduler.shutdownNow();
                yield Message.gameEnded(e.winners(), e.finalScores());
            }
        };
        broadcast(msg);
    }

    // ── Disconnection ─────────────────────────────────────────────────────────

    /**
     * Called by SocketClientHandler when it detects a disconnect.
     * @param source the handler reporting the disconnect — compared against
     *               the CURRENT handler in clientMap to reject stale notifications
     *               from old handlers that die after a reconnect.
     */
    public synchronized void onPlayerDisconnected(String nick, ClientConnection source) {
        if (terminated) return;

        // CRITICAL: ignore disconnect from a stale/old handler.
        // After reconnect, clientMap holds the NEW handler. If the OLD handler
        // dies later and calls this method, we must not mark the player disconnected.
        ClientConnection current = clientMap.get(nick);
        if (current != source) {
            LOG.info("Ignoring stale disconnect from old handler of '" + nick                    + "' (handler=" + System.identityHashCode(source) + ")");
            return;
        }

        LOG.warning("DISCONNECT nick=" + nick
                + " handler=" + System.identityHashCode(source));

        if (!started || controller == null) {
            terminate("Giocatore '" + nick + "' ha abbandonato prima dell'inizio.");
            return;
        }

        broadcast(Message.playerDisconnected(nick));
        controller.getGame().markDisconnected(nick);
        // Keep nick in clientMap so reconnection is possible (hasPlayer check).
        clients.remove(source);

        long active = connectedCount();
        if (active == 0) {
            terminate("Tutti i giocatori si sono disconnessi.");
            return;
        }
        // ≥ 2 connected: game continues uninterrupted.
        if (active >= 2) {
            LOG.info("'" + nick + "' disconnesso — la partita continua con "
                    + active + " giocatori attivi.");
            if (timeoutFuture != null) { timeoutFuture.cancel(false); timeoutFuture = null; }
            suspended = false;
            return;
        }
        // 1 player left: suspend and start reconnection timeout.
        suspended = true;
        String survivor = clients.stream().map(ClientConnection::getNickname)
                .findFirst().orElse("?");
        broadcast(Message.error("Partita sospesa — in attesa di riconnessione ("
                + TIMEOUT_SECONDS + "s). Giocatore rimasto: " + survivor));
        LOG.warning("Solo 1 giocatore rimasto — partita sospesa per " + TIMEOUT_SECONDS + "s.");
        timeoutFuture = scheduler.schedule(() -> {
            synchronized (GameSession.this) {
                if (terminated || !suspended) return;
                broadcast(Message.error(survivor + " vince per abbandono (timeout)."));
                terminate("Timeout riconnessione.");
            }
        }, TIMEOUT_SECONDS, TimeUnit.SECONDS);
    }

    // ── Internal ──────────────────────────────────────────────────────────────

    private void terminate(String reason) {
        terminated = true;
        scheduler.shutdownNow();
        // Use error() not playerDisconnected(): reason is a server message, not a nickname.
        broadcast(Message.error("Partita terminata: " + reason));
        clients.forEach(ClientConnection::close);
        clients.clear();
        clientMap.clear();
    }

    private void broadcast(Message msg) {
        // clientMap always holds only current (non-stale) handlers.
        clients.forEach(c -> c.send(msg));
    }
}