package it.polimi.it.network.server;

import it.polimi.it.Game;
import it.polimi.it.GameBuilder;
import it.polimi.it.actions.*;
import it.polimi.it.enums.TotemColour;
import it.polimi.it.network.message.Message;
import it.polimi.it.observer.GameEvent;
import it.polimi.it.observer.GameEventListener;

import java.util.*;
import java.util.concurrent.*;
import java.util.logging.Logger;

/**
 * One active game session on the server.
 * Uses Message (org.json) instead of typed record messages.
 */
public class GameSession implements GameEventListener {

    private static final Logger LOG             = Logger.getLogger(GameSession.class.getName());
    private static final long   TIMEOUT_SECONDS = 60;

    private final int                           expectedPlayers;
    private final List<ClientConnection>        clients   = new CopyOnWriteArrayList<>();
    private final Map<String, ClientConnection> clientMap = new ConcurrentHashMap<>();

    private Game    game;
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
        if (old != null) { clients.remove(old); old.close(); }
        clients.add(c);
        clientMap.put(nick, c);
        if (timeoutFuture != null) { timeoutFuture.cancel(false); timeoutFuture = null; }
        if (game != null) game.markReconnected(nick);
        suspended = false;
        LOG.info("'" + nick + "' reconnected.");
        c.send(Message.error("Welcome back, " + nick + "! Game resumed."));
    }

    public synchronized boolean hasRoom()      { return !started && !terminated && clients.size() < expectedPlayers; }
    public synchronized boolean isStarted()    { return started; }
    public synchronized boolean isTerminated() { return terminated; }
    public synchronized boolean isSuspended()  { return suspended; }
    public int getExpectedPlayers()            { return expectedPlayers; }
    public synchronized boolean hasPlayer(String nick) { return clientMap.containsKey(nick); }

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
            LOG.severe("Failed to start: " + e.getMessage());
            terminate("Server error starting the game.");
        }
    }

    // ── Message handling ──────────────────────────────────────────────────────

    public synchronized void handleMessage(Message msg, ClientConnection sender) {
        if (!started || terminated || suspended) return;
        switch (msg.getType()) {
            case Message.PLACE_TOTEM ->
                    game.execute(new PlaceTotemAction(
                            msg.str("playerName"), msg.ch("tileLetter")));
            case Message.TAKE_CARD ->
                    game.execute(new TakeCardAction(
                            msg.str("playerName"), msg.num("cardId"), msg.bool("fromTopRow")));
            case Message.PASS ->
                    game.execute(new PassAction(msg.str("playerName")));
            case Message.POST_ROUND ->
                    game.execute(new PostRoundAction(
                            msg.str("playerName"), msg.num("cardId")));
            case Message.PONG -> {}
            default -> sender.send(Message.error("Unexpected: " + msg.getType()));
        }
    }

    // ── GameEventListener ─────────────────────────────────────────────────────

    @Override
    public void onEvent(GameEvent event) {
        Message msg = switch (event) {
            case GameEvent.RoundStarted e ->
                    Message.roundStarted(e.round(), e.turnOrder(), e.availableTiles());
            case GameEvent.TotemPlaced e ->
                    Message.totemPlaced(e.player(), e.tileLetter(), e.remainingTiles());
            case GameEvent.CardSlotReady e ->
                    Message.cardSlotReady(e.player(), e.isTopRow(),
                            e.availableCards(), e.slotsRemaining());
            case GameEvent.CardAcquired e ->
                    Message.cardAcquired(e.player(), e.card());
            case GameEvent.CardAcquisitionFailed e ->
                    Message.cardAcquisitionFailed(e.player(), e.card(),
                            e.missingFood(), e.fromTopRow(), e.alternatives());
            case GameEvent.TotemReturnedToTurnOrder e ->
                    Message.totemReturned(e.player(), e.position());
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

    public synchronized void onPlayerDisconnected(String nick) {
        if (terminated) return;
        LOG.warning("'" + nick + "' disconnected.");
        broadcast(Message.playerDisconnected(nick + " disconnected."));
        if (!started) { terminate("Player left before game started."); return; }
        game.markDisconnected(nick);
        long active = clients.stream()
                .filter(c -> !game.isDisconnected(c.getNickname())).count();
        if (active == 0) {
            terminate("All players disconnected.");
        } else if (active == 1) {
            suspended = true;
            broadcast(Message.error("Game suspended — waiting for reconnection (" +
                    TIMEOUT_SECONDS + "s timeout)."));
            timeoutFuture = scheduler.schedule(() -> {
                synchronized (GameSession.this) {
                    if (terminated || !suspended) return;
                    clients.stream()
                            .filter(c -> !game.isDisconnected(c.getNickname()))
                            .findFirst()
                            .ifPresent(w -> {
                                broadcast(Message.error(w.getNickname() + " wins by default."));
                                terminate("Timeout.");
                            });
                }
            }, TIMEOUT_SECONDS, TimeUnit.SECONDS);
        }
    }

    // ── Internal ──────────────────────────────────────────────────────────────

    private void terminate(String reason) {
        terminated = true;
        scheduler.shutdownNow();
        broadcast(Message.playerDisconnected(reason));
        clients.forEach(ClientConnection::close);
    }

    private void broadcast(Message msg) {
        clients.forEach(c -> c.send(msg));
    }
}