package it.polimi.it.network.client;

import it.polimi.it.enums.Era;
import it.polimi.it.enums.TotemColour;
import it.polimi.it.enums.EventType;
import it.polimi.it.network.message.Message;
import it.polimi.it.network.server.persistence.LeaderboardEntry;
import it.polimi.it.observer.BoardSnapshot;
import it.polimi.it.observer.CardSnapshot;
import it.polimi.it.observer.PlayerSnapshot;
import it.polimi.it.view.GameView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

/**
 * Client-side controller.
 *
 * <p>Receives messages from the server and drives the view.
 *
 * <p><b>Input executor rule:</b> there is exactly one executor for the whole
 * game. It is never recreated or interrupted while the game is active. Input
 * tasks naturally queue up: the server sends {@code CARD_SLOT_READY} only when
 * it is actually that player's turn, so queued tasks are not obsolete. The
 * executor is shut down only in {@link #onDisconnect()} and {@link #run()}.
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
    private volatile int                currentRound          = 0;
    private volatile String             lastPhase             = "WAITING";
    private volatile PromptKey          activePrompt          = null;

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

    private record PromptKey(String kind, String player, String detail) {}

    public ClientController(ServerProxy proxy, GameView view, String myNickname) {
        this.proxy      = proxy;
        this.view       = view;
        this.myNickname = myNickname;
    }

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    /** Called when the connection drops. */
    public void onDisconnect() {
        inputExecutor.shutdownNow();
        postGameScheduler.shutdownNow();
        view.showInvalidAction("SERVER", "Connessione persa.");
        gameLatch.countDown();
    }

    /** Blocks until the game ends. Call after proxy.connect(). */
    public void run() {
        try { gameLatch.await(); }
        catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        finally {
            inputExecutor.shutdownNow();
            postGameScheduler.shutdownNow();
        }
    }

    // ── Dispatch ──────────────────────────────────────────────────────────────

    public void onMessage(Message msg) {
        try {
            dispatch(msg);
        } catch (Exception e) {
            LOG.severe("Exception in onMessage type=" + msg.getType() + ": " + e);
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
            // Heartbeat and keep-alive messages only refresh proxy timestamps.
            case Message.HEARTBEAT, Message.PING, Message.PONG -> { /* ignored */ }
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
                        entries,
                        msg.intMap("playerRanks"));
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
                    if (isPlacementPhase() && lastBoard != null) promptNextTotemPlacement(lastBoard);
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
                if (isPlacementPhase() && lastBoard != null) promptNextTotemPlacement(lastBoard);
            }
            default -> LOG.warning("Unknown message type: " + msg.getType());
        }
    }

    // Totem placement handler.

    private void handleRoundStarted(Message msg) {
        currentRound = msg.num("round");
        String phase = msg.optStr("phase");
        lastPhase = (phase == null || phase.isBlank()) ? "PLACING_TOTEMS" : phase;
        alreadyPlaced.clear();
        currentTurnOrder = msg.playerList("turnOrder");
        BoardSnapshot board = msg.boardObj("board");
        lastBoard = board;
        if (isPlacementPhase()) rememberPlacedTotems(board);
        view.showRoundStarted(currentRound, currentTurnOrder, board);
        if (isPlacementPhase()) promptNextTotemPlacement(board);
    }

    private void handleTotemPlaced(Message msg) {
        lastPhase = "PLACING_TOTEMS";
        PlayerSnapshot placed = msg.playerObj("player");
        BoardSnapshot  board  = msg.boardObj("board");
        lastBoard = board;
        view.showTotemPlaced(placed, msg.ch("tileLetter"), board);
        alreadyPlaced.add(placed.name());
        promptNextTotemPlacement(board);
    }

    private void rememberPlacedTotems(BoardSnapshot board) {
        Map<TotemColour, String> playersByTotem = new HashMap<>();
        for (PlayerSnapshot player : currentTurnOrder) {
            if (player.totemColour() != null) {
                playersByTotem.put(player.totemColour(), player.name());
            }
        }
        for (var tile : board.offerTiles()) {
            if (tile.isFree() || tile.placedTotem() == null) continue;
            String playerName = playersByTotem.get(tile.placedTotem());
            if (playerName != null) alreadyPlaced.add(playerName);
        }
    }

    private void promptNextTotemPlacement(BoardSnapshot board) {
        if (!isPlacementPhase()) return;
        for (PlayerSnapshot p : currentTurnOrder) {
            if (alreadyPlaced.contains(p.name())) continue;
            if (disconnectedNicknames.contains(p.name())) continue;
            if (p.name().equals(myNickname)) {
                final PlayerSnapshot me = p;
                final BoardSnapshot  b  = board;
                submitPrompt(new PromptKey("PLACE_TOTEM", myNickname, String.valueOf(currentRound)), () -> {
                    char chosen = view.requestOfferTileChoice(me, b);
                    proxy.send(Message.placeTotem(myNickname, chosen));
                });
            }
            return;
        }
    }

    // Card-choice handler.

    private void handleCardSlotReady(Message msg) {
        lastPhase = "RESOLVING_ACTIONS";
        PlayerSnapshot     player        = msg.playerObj("player");
        boolean            isTop         = msg.bool("isTopRow");
        int                slotsRemaining = msg.num("slotsRemaining");
        List<CardSnapshot> cards         = msg.cardList("availableCards");
        List<CardSnapshot> visibleEvents = msg.cardList("visibleEvents");

        view.showCardSlotReady(player, isTop, cards, visibleEvents);

        // Only the active player is prompted. Do not call shutdownNow() or
        // recreate the executor: the server sends CARD_SLOT_READY only when it
        // is actually this player's turn, so tasks queue without becoming
        // obsolete.
        if (!player.name().equals(myNickname)) return;

        PromptKey promptKey = new PromptKey("CARD", myNickname,
                currentRound + ":" + isTop + ":" + slotsRemaining + ":" + cardIds(cards));
        submitPrompt(promptKey, () -> {
            int chosen = view.requestCardChoice(player, cards, /*canPass*/ true);
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

        PromptKey promptKey = new PromptKey("CARD_RETRY", myNickname,
                currentRound + ":" + msg.bool("fromTopRow") + ":" + cardIds(alternatives));
        submitPrompt(promptKey, () -> {
            // The player cannot afford any alternative, so passing is allowed.
            int chosen = view.requestCardChoice(player, alternatives, /*canPass*/ true);
            if (chosen == -1) proxy.send(Message.pass(myNickname));
            else proxy.send(Message.takeCard(myNickname, chosen, msg.bool("fromTopRow")));
        });
    }

    // Post-round handler for Watchtower.

    private void handlePostRoundStarted(Message msg) {
        lastPhase = "POST_ROUND";
        List<PlayerSnapshot> watchtower = msg.playerList("watchtowerPlayers");
        List<CardSnapshot>   cards      = msg.cardList("availableCards");

        view.showPostRoundStarted(watchtower, cards);

        boolean isMine = watchtower.stream().anyMatch(p -> p.name().equals(myNickname));
        if (!isMine) return;

        PlayerSnapshot me = watchtower.stream()
                .filter(p -> p.name().equals(myNickname))
                .findFirst().orElseThrow();

        submitPrompt(new PromptKey("POST_ROUND", myNickname,
                currentRound + ":" + cardIds(cards)), () -> {
            int chosen = view.requestPostRoundCardChoice(me, cards);
            proxy.send(Message.postRound(myNickname, chosen));
        });
    }

    private boolean isPlacementPhase() {
        return "PLACING_TOTEMS".equals(lastPhase);
    }

    private String cardIds(List<CardSnapshot> cards) {
        return cards.stream()
                .map(c -> String.valueOf(c.id()))
                .reduce((a, b) -> a + "," + b)
                .orElse("");
    }

    private void submitPrompt(PromptKey key, Runnable task) {
        synchronized (this) {
            if (key.equals(activePrompt)) return;
            activePrompt = key;
        }
        inputExecutor.submit(() -> {
            try {
                task.run();
            } finally {
                synchronized (ClientController.this) {
                    if (key.equals(activePrompt)) activePrompt = null;
                }
            }
        });
    }
}
