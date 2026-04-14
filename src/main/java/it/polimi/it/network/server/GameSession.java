
package it.polimi.it.network.server;

import it.polimi.it.Game;
import it.polimi.it.GameBuilder;
import it.polimi.it.actions.*;
import it.polimi.it.enums.TotemColour;
import it.polimi.it.network.message.*;
import it.polimi.it.observer.GameEvent;
import it.polimi.it.observer.GameEventListener;

import java.util.*;
        import java.util.concurrent.*;
        import java.util.logging.Logger;

/**
 * Represents one active game session on the server.
 *
 * Resilience behaviour (rulebook requirements):
 * - When a player disconnects, their turns are skipped automatically.
 * - If only one player remains connected, the game is SUSPENDED and a
 *   60-second countdown starts.
 * - If the disconnected player reconnects within 60 seconds, the game resumes.
 * - If the countdown expires, the remaining player wins by default.
 * - If ALL players disconnect, the session is terminated immediately.
 *
 * A player reconnects by sending a JOIN message with the same nickname.
 * LobbyServer detects that the nickname belongs to a suspended session and
 * calls {@link #reconnectClient(ClientConnection)} instead of addClient().
 */
public class GameSession implements GameEventListener {

    private static final Logger LOG             = Logger.getLogger(GameSession.class.getName());
    private static final long   TIMEOUT_SECONDS = 60;

    private final int                           expectedPlayers;
    private final List<ClientConnection>        clients;
    private final Map<String, ClientConnection> clientMap;

    private Game    game;
    private boolean started;
    private boolean terminated;
    private boolean suspended;

    private final ScheduledExecutorService scheduler =
            Executors.newSingleThreadScheduledExecutor();
    private ScheduledFuture<?> timeoutFuture;

    public GameSession(int expectedPlayers) {
        this.expectedPlayers = expectedPlayers;
        this.clients         = new CopyOnWriteArrayList<>();
        this.clientMap       = new ConcurrentHashMap<>();
        this.started         = false;
        this.terminated      = false;
        this.suspended       = false;
    }

    // ── Player connection ─────────────────────────────────────────────────────

    /** Adds a new client to the lobby. Starts the game when full. */
    public synchronized void addClient(ClientConnection connection) {
        if (terminated || started) return;
        clients.add(connection);
        clientMap.put(connection.getNickname(), connection);
        LOG.info("Player '" + connection.getNickname() + "' joined ("
                + clients.size() + "/" + expectedPlayers + ")");
        broadcastLobbyUpdate();
        if (clients.size() == expectedPlayers) startGame();
    }

    /**
     * Handles a reconnecting player.
     * Replaces the old dead connection with the new one and resumes the game.
     */
    public synchronized void reconnectClient(ClientConnection connection) {
        if (terminated) return;
        String nickname = connection.getNickname();

        // Replace old connection
        ClientConnection old = clientMap.get(nickname);
        if (old != null) {
            clients.remove(old);
            old.close();
        }
        clients.add(connection);
        clientMap.put(nickname, connection);

        LOG.info("Player '" + nickname + "' reconnected.");

        // Cancel pending timeout
        if (timeoutFuture != null && !timeoutFuture.isDone()) {
            timeoutFuture.cancel(false);
            timeoutFuture = null;
        }

        // Unmark disconnected in game model
        if (game != null) game.markReconnected(nickname);

        suspended = false;

        broadcast(new LobbyUpdateMessage(
                clients.stream().map(ClientConnection::getNickname).toList(),
                expectedPlayers));
        connection.send(new ErrorMessage(
                "Welcome back, " + nickname + "! Game resumed."));
    }

    public synchronized boolean hasRoom()      { return !started && !terminated && clients.size() < expectedPlayers; }
    public synchronized boolean isStarted()    { return started; }
    public synchronized boolean isTerminated() { return terminated; }
    public synchronized boolean isSuspended()  { return suspended; }
    public int                  getExpectedPlayers() { return expectedPlayers; }

    /** Returns true if the given nickname belongs to a player in this session. */
    public synchronized boolean hasPlayer(String nickname) {
        return clientMap.containsKey(nickname);
    }

    // ── Game startup ──────────────────────────────────────────────────────────

    private void startGame() {
        try {
            List<String>      names   = new ArrayList<>();
            List<TotemColour> colours = new ArrayList<>();
            TotemColour[]     all     = TotemColour.values();
            for (int i = 0; i < clients.size(); i++) {
                names.add(clients.get(i).getNickname());
                colours.add(all[i]);
            }
            game = new GameBuilder()
                    .withPlayers(names, colours)
                    .withCards(ServerApp.getCardList())
                    .build();
            game.addListener(this);
            started = true;
            LOG.info("Game started with " + expectedPlayers + " players.");
            game.start();
        } catch (Exception e) {
            LOG.severe("Failed to start game: " + e.getMessage());
            terminate("Server error while starting the game.");
        }
    }

    // ── Action handling ───────────────────────────────────────────────────────

    /** Converts an incoming Message to a PlayerAction and passes it to Game. */
    public synchronized void handleMessage(Message message, ClientConnection sender) {
        if (!started || terminated || suspended) return;
        switch (message) {
            case PlaceTotemMessage m ->
                    game.execute(new PlaceTotemAction(m.playerName(), m.tileLetter()));
            case TakeCardMessage m ->
                    game.execute(new TakeCardAction(m.playerName(), m.cardId(), m.fromTopRow()));
            case PassMessage m ->
                    game.execute(new PassAction(m.playerName()));
            case PostRoundMessage m ->
                    game.execute(new PostRoundAction(m.playerName(), m.cardId()));
            case PongMessage ignored -> {}
            default -> sender.send(new ErrorMessage("Unexpected message: "
                    + message.getClass().getSimpleName()));
        }
    }

    // ── GameEventListener ─────────────────────────────────────────────────────

    @Override
    public void onEvent(GameEvent event) {
        Message msg = switch (event) {
            case GameEvent.RoundStarted e ->
                    new RoundStartedMessage(e.round(), e.turnOrder(), e.availableTiles());
            case GameEvent.TotemPlaced e ->
                    new TotemPlacedMessage(e.player(), e.tileLetter(), e.remainingTiles());
            case GameEvent.CardSlotReady e ->
                    new CardSlotReadyMessage(e.player(), e.isTopRow(),
                            e.availableCards(), e.slotsRemaining());
            case GameEvent.CardAcquired e ->
                    new CardAcquiredMessage(e.player(), e.card());
            case GameEvent.CardAcquisitionFailed e ->
                    new CardAcquisitionFailedMessage(e.player(), e.card(),
                            e.missingFood(), e.fromTopRow(), e.alternatives());
            case GameEvent.TotemReturnedToTurnOrder e ->
                    new TotemReturnedMessage(e.player(), e.position());
            case GameEvent.PostRoundStarted e ->
                    new PostRoundStartedMessage(e.round(),
                            e.watchtowerPlayers(), e.availableCards());
            case GameEvent.EventResolved e ->
                    new EventResolvedMessage(e.eventId(), e.era(), e.type());
            case GameEvent.EraChanged e ->
                    new EraChangedMessage(e.newEra());
            case GameEvent.RoundEnded e ->
                    new RoundEndedMessage(e.round());
            case GameEvent.GameEnded e -> {
                terminated = true;
                scheduler.shutdownNow();
                yield new GameEndedMessage(e.winners(), e.finalScores());
            }
        };
        broadcast(msg);
    }

    // ── Disconnection & resilience ────────────────────────────────────────────

    /**
     * Called when a player disconnects or crashes.
     *
     * - Notifies all clients
     * - If pre-game: terminates the session
     * - If in-game: marks the player as disconnected (turns skipped)
     *   - 0 active players left → terminate
     *   - 1 active player left  → suspend + start 60s timeout
     *   - 2+ active players     → game continues with skipped turns
     */
    public synchronized void onPlayerDisconnected(String nickname) {
        if (terminated) return;
        LOG.warning("Player '" + nickname + "' disconnected.");
        broadcast(new PlayerDisconnectedMessage(nickname + " disconnected."));

        if (!started) {
            terminate("A player left before the game started.");
            return;
        }

        game.markDisconnected(nickname);

        long activeCount = clients.stream()
                .filter(c -> !game.isDisconnected(c.getNickname()))
                .count();

        if (activeCount == 0) {
            LOG.info("All players disconnected — terminating.");
            terminate("All players disconnected.");
        } else if (activeCount == 1) {
            LOG.info("One player left — suspending, " + TIMEOUT_SECONDS + "s timeout.");
            suspended = true;
            broadcast(new ErrorMessage(
                    "Game suspended: waiting for reconnection ("
                            + TIMEOUT_SECONDS + "s timeout)."));
            startTimeout();
        }
        // activeCount > 1: game continues, turns of disconnected players are skipped
    }

    // ── Timeout ───────────────────────────────────────────────────────────────

    private void startTimeout() {
        timeoutFuture = scheduler.schedule(() -> {
            synchronized (GameSession.this) {
                if (terminated || !suspended) return;
                LOG.info("Timeout expired — declaring default winner.");
                clients.stream()
                        .filter(c -> !game.isDisconnected(c.getNickname()))
                        .findFirst()
                        .ifPresent(winner -> {
                            broadcast(new ErrorMessage(
                                    winner.getNickname()
                                            + " wins by default (timeout)."));
                            terminate("Game over by timeout.");
                        });
            }
        }, TIMEOUT_SECONDS, TimeUnit.SECONDS);
    }

    // ── Internal ──────────────────────────────────────────────────────────────

    private void terminate(String reason) {
        terminated = true;
        scheduler.shutdownNow();
        broadcast(new PlayerDisconnectedMessage(reason));
        clients.forEach(ClientConnection::close);
    }

    private void broadcast(Message message) {
        clients.forEach(c -> c.send(message));
    }

    private void broadcastLobbyUpdate() {
        List<String> names = clients.stream()
                .map(ClientConnection::getNickname).toList();
        broadcast(new LobbyUpdateMessage(names, expectedPlayers));
    }
}