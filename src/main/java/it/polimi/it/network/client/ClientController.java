package it.polimi.it.network.client;

import it.polimi.it.enums.Era;
import it.polimi.it.enums.EventType;
import it.polimi.it.network.message.Message;
import it.polimi.it.network.server.persistence.LeaderboardEntry;
import it.polimi.it.observer.BoardSnapshot;
import it.polimi.it.observer.CardSnapshot;
import it.polimi.it.observer.PlayerSnapshot;
import it.polimi.it.view.GameView;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

/**
 * The controller on the client side.
 *
 * <p>It connects the network and the user interface. When a message comes from
 * the server, this class reads it, updates the local game state and tells the
 * {@link GameView} what to show. When the user makes a choice, it builds a
 * {@link Message} and sends it to the server through the {@link ServerProxy}.
 *
 * <p>{@link #onMessage(Message)} is called by the network thread, so it must
 * not block. When the program has to wait for the user (for example to pick a
 * tile or a card), the work is sent to {@code inputExecutor}, a single thread
 * used only for user input. This keeps the network thread free to read new
 * messages.
 *
 * <p>{@code inputExecutor} is made once and used for the whole game. It is
 * never restarted while playing. The server asks a player to act only when it
 * is really that player's turn, so the input tasks run one after another and
 * no old task is left in the queue. It is closed only in {@link #run()} and
 * {@link #onDisconnect()}.
 *
 * <p>{@link #run()} waits on a {@link CountDownLatch} until the game is over.
 * The latch is freed when the game ends (after the {@code LEADERBOARD} message,
 * or after a short timeout if it does not come) or when the connection drops.
 */
public class ClientController {

    private static final Logger LOG = Logger.getLogger(ClientController.class.getName());

    private final ServerProxy proxy;
    private final GameView    view;
    private final String      myNickname;

    private List<PlayerSnapshot>        currentTurnOrder      = new ArrayList<>();
    private final java.util.Set<String> alreadyPlaced         = new java.util.HashSet<>();
    private final java.util.Set<String> disconnectedNicknames = new java.util.HashSet<>();
    private volatile BoardSnapshot      lastBoard             = null;

    private final CountDownLatch  gameLatch     = new CountDownLatch(1);
    private final ExecutorService inputExecutor = Executors.newSingleThreadExecutor();
    /** Used to enforce a deadline after GAME_ENDED to wait for the optional LEADERBOARD. */
    private final java.util.concurrent.ScheduledExecutorService postGameScheduler =
            java.util.concurrent.Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "client-postgame-timeout");
                t.setDaemon(true);
                return t;
            });
    /** Set to true when GAME_ENDED has been processed, used to gate LEADERBOARD handling. */
    private volatile boolean gameEndedReceived = false;

    public ClientController(ServerProxy proxy, GameView view, String myNickname) {
        this.proxy      = proxy;
        this.view       = view;
        this.myNickname = myNickname;
    }

    // Lifecycle
    /** Chiamato quando la connessione cade. */
    public void onDisconnect() {
        inputExecutor.shutdownNow();
        postGameScheduler.shutdownNow();
        view.showInvalidAction("SERVER", "Connessione persa.");
        gameLatch.countDown();
    }

    /** Blocca finché la partita non finisce. Da chiamare dopo proxy.connect(). */
    public void run() {
        try { gameLatch.await(); }
        catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        finally {
            inputExecutor.shutdownNow();
            postGameScheduler.shutdownNow();
        }
    }

    // ── Dispatch

    public void onMessage(Message msg) {
        try {
            dispatch(msg);
        } catch (Exception e) {
            LOG.severe("Eccezione in onMessage tipo=" + msg.getType() + ": " + e);
            for (StackTraceElement el : e.getStackTrace()) LOG.severe("  at " + el);
        }
    }

    private void dispatch(Message msg) {
        switch (msg.getType()) {
            case Message.LOBBY_UPDATE -> {
                var players = msg.strList("connectedPlayers");
                var needed  = msg.num("expectedPlayers");
                System.out.println("Lobby: " + players.size() + "/" + needed + " — " + players);
                view.showLobbyUpdate(players, needed);
            }
            case Message.ERROR ->
                    view.showInvalidAction("SERVER", msg.str("reason"));
            // Heartbeat e keep-alive: aggiornano solo il timestamp nel proxy, niente altro
            case Message.HEARTBEAT, Message.PING, Message.PONG -> { /* ignorati */ }
            case Message.ROUND_STARTED      -> handleRoundStarted(msg);
            case Message.TOTEM_PLACED       -> handleTotemPlaced(msg);
            case Message.CARD_SLOT_READY    -> handleCardSlotReady(msg);
            case Message.CARD_ACQUIRED ->
                    view.showCardAcquired(msg.playerObj("player"), msg.cardObj("card"));
            case Message.CARD_ACQUISITION_FAILED -> handleCardAcquisitionFailed(msg);
            case Message.TOTEM_RETURNED ->
                    view.showTotemReturnedToTurnOrder(
                            msg.playerObj("player"),
                            msg.num("position"),
                            msg.boardObj("board"));
            case Message.POST_ROUND_STARTED -> handlePostRoundStarted(msg);
            case Message.EVENT_RESOLVED ->
                    view.showEventResolved(
                            msg.num("eventId"),
                            Era.valueOf(msg.str("era")),
                            EventType.valueOf(msg.str("eventType")),
                            msg.playerResourceDeltaList("deltas"));
            case Message.ERA_CHANGED ->
                    view.showEraChanged(Era.valueOf(msg.str("newEra")));
            case Message.ROUND_ENDED ->
                    view.showRoundEnded(msg.num("round"));
            case Message.GAME_ENDED -> {
                view.showGameEnded(
                        msg.playerList("winners"),
                        msg.playerList("finalScores"),
                        msg.finalScoreBreakdownList("scoreBreakdowns"));
                gameEndedReceived = true;
                // Wait up to 5 s for the optional LEADERBOARD message that the
                // server sends right after GAME_ENDED when the leaderboard DB
                // is enabled. If it never arrives (DB down on the server side)
                // we release the latch ourselves so the client shuts down.
                postGameScheduler.schedule(() -> {
                    if (gameLatch.getCount() > 0) gameLatch.countDown();
                }, 5, TimeUnit.SECONDS);
            }
            case Message.LEADERBOARD -> {
                List<LeaderboardEntry> entries = msg.leaderboardList("entries");
                view.showLeaderboard(
                        msg.str("recipient"),
                        msg.num("myScore"),
                        msg.num("myRank"),
                        msg.num("totalEntries"),
                        msg.num("numPlayers"),
                        entries);
                if (gameEndedReceived) gameLatch.countDown();
            }
            case Message.PLAYER_DISCONNECTED -> {
                String who = msg.str("nickname");
                if (who.equals(myNickname)) {
                    view.showInvalidAction("DISCONNECT", "Sei stato disconnesso dal server.");
                    gameLatch.countDown();
                } else {
                    disconnectedNicknames.add(who);
                    view.showPlayerDisconnected(who);
                    // Re-prompt placement in case it was this player's turn.
                    if (lastBoard != null) promptNextTotemPlacement(lastBoard);
                }
            }
            case Message.PLAYER_RECONNECTED -> {
                String who = msg.str("nickname");
                disconnectedNicknames.remove(who);
                if (who.equals(myNickname)) {
                    view.showInvalidAction("RECONNECT", "Sei tornato in gioco!");
                } else {
                    view.showPlayerReconnected(who);
                }
            }
            default -> LOG.warning("Tipo messaggio sconosciuto: " + msg.getType());
        }
    } // fine dispatch()

    // ── Handler: piazzamento totem

    private void handleRoundStarted(Message msg) {
        alreadyPlaced.clear();
        currentTurnOrder = msg.playerList("turnOrder");
        BoardSnapshot board = msg.boardObj("board");
        lastBoard = board;
        view.showRoundStarted(msg.num("round"), currentTurnOrder, board);
        promptNextTotemPlacement(board);
    }

    private void handleTotemPlaced(Message msg) {
        PlayerSnapshot placed = msg.playerObj("player");
        BoardSnapshot  board  = msg.boardObj("board");
        lastBoard = board;
        view.showTotemPlaced(placed, msg.ch("tileLetter"), board);
        alreadyPlaced.add(placed.name());
        promptNextTotemPlacement(board);
    }

    private void promptNextTotemPlacement(BoardSnapshot board) {
        for (PlayerSnapshot p : currentTurnOrder) {
            if (alreadyPlaced.contains(p.name())) continue;
            if (disconnectedNicknames.contains(p.name())) continue;
            if (p.name().equals(myNickname)) {
                final PlayerSnapshot me = p;
                final BoardSnapshot  b  = board;
                inputExecutor.submit(() -> {
                    char chosen = view.requestOfferTileChoice(me, b);
                    proxy.send(Message.placeTotem(myNickname, chosen));
                });
            }
            return;
        }
    }

    // Handler: scelta carte

    private void handleCardSlotReady(Message msg) {
        PlayerSnapshot     player        = msg.playerObj("player");
        boolean            isTop         = msg.bool("isTopRow");
        List<CardSnapshot> cards         = msg.cardList("availableCards");
        List<CardSnapshot> visibleEvents = msg.cardList("visibleEvents");

        view.showCardSlotReady(player, isTop, cards, visibleEvents);

        // We ask for input only from the player whose turn it is.
        // We do NOT call shutdownNow or rebuild the executor: the server sends
        // CARD_SLOT_READY only when it is really this player's turn, so the tasks
        // queue up correctly and are never "old".
        if (!player.name().equals(myNickname)) return;

        inputExecutor.submit(() -> {
            int chosen = view.requestCardChoice(player, cards);
            if (chosen == -1) proxy.send(Message.pass(myNickname));
            else              proxy.send(Message.takeCard(myNickname, chosen, isTop));
        });
    }

    private void handleCardAcquisitionFailed(Message msg) {
        PlayerSnapshot     player       = msg.playerObj("player");
        CardSnapshot       card         = msg.cardObj("card");
        List<CardSnapshot> alternatives = msg.cardList("alternatives");

        view.showCardAcquisitionFailed(player, card, msg.num("missingFood"));

        if (!player.name().equals(myNickname)) return;

        inputExecutor.submit(() -> {
            int chosen = view.requestCardChoice(player, alternatives);
            if (chosen == -1) proxy.send(Message.pass(myNickname));
            else proxy.send(Message.takeCard(myNickname, chosen, msg.bool("fromTopRow")));
        });
    }

    // ── Handler: post-round

    private void handlePostRoundStarted(Message msg) {
        List<PlayerSnapshot> watchtower = msg.playerList("watchtowerPlayers");
        List<CardSnapshot>   cards      = msg.cardList("availableCards");

        view.showPostRoundStarted(watchtower, cards);

        boolean isMine = watchtower.stream().anyMatch(p -> p.name().equals(myNickname));
        if (!isMine) return;

        PlayerSnapshot me = watchtower.stream()
                .filter(p -> p.name().equals(myNickname))
                .findFirst().orElseThrow();

        inputExecutor.submit(() -> {
            int chosen = view.requestPostRoundCardChoice(me, cards);
            proxy.send(Message.postRound(myNickname, chosen));
        });
    }
}
