package it.polimi.it.network.client;

import it.polimi.it.enums.Era;
import it.polimi.it.enums.TotemColour;
import it.polimi.it.enums.EventType;
import it.polimi.it.network.message.Message;
import it.polimi.it.observer.BoardSnapshot;
import it.polimi.it.observer.CardSnapshot;
import it.polimi.it.observer.PlayerSnapshot;
import it.polimi.it.view.GameView;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

/**
 * Client-side controller.
 *
 * <p>Receives messages from the server and guides the view.
 *
 * <p><b>Fundamental rule about the inputExecutor:</b>
 * The executor is <em>only one</em> for the entire duration of the game and is never
 * recreated or interrupted during play. Input tasks naturally queue up: the server sends {@code CARD_SLOT_READY} only
 * when it is actually that player's turn, so there will
 * never be "obsolete" tasks in the queue.
 * The executor is terminated only in {@link #onDisconnect()} and {@link #run()}.
 */
public class ClientController {

    private static final Logger LOG = Logger.getLogger(ClientController.class.getName());

    private final ServerProxy proxy;
    private final GameView    view;
    private final String      myNickname;

    private List<PlayerSnapshot> currentTurnOrder = new ArrayList<>();
    private int                  totemPlacedCount  = 0;

    private final CountDownLatch  gameLatch     = new CountDownLatch(1);
    private final ExecutorService inputExecutor = Executors.newSingleThreadExecutor();

    public ClientController(ServerProxy proxy, GameView view, String myNickname) {
        this.proxy      = proxy;
        this.view       = view;
        this.myNickname = myNickname;
    }

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    //** Called when the connection drops. */
    public void onDisconnect() {
        inputExecutor.shutdownNow();
        view.showInvalidAction("SERVER", "Connessione persa.");
        gameLatch.countDown();
    }

    /** Blocks until the game ends. To be called after proxy.connect(). */
    public void run() {
        try { gameLatch.await(); }
        catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        finally { inputExecutor.shutdownNow(); }
    }

    // ── Dispatch ──────────────────────────────────────────────────────────────

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
            case Message.LOBBY_UPDATE ->
                    System.out.println("Lobby: "
                            + msg.strList("connectedPlayers").size()
                            + "/" + msg.num("expectedPlayers")
                            + " — " + msg.strList("connectedPlayers"));
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
                            EventType.valueOf(msg.str("eventType")));
            case Message.ERA_CHANGED ->
                    view.showEraChanged(Era.valueOf(msg.str("newEra")));
            case Message.ROUND_ENDED ->
                    view.showRoundEnded(msg.num("round"));
            case Message.GAME_ENDED -> {
                view.showGameEnded(
                        msg.playerList("winners"),
                        msg.playerList("finalScores"));
                gameLatch.countDown();
            }
            case Message.PLAYER_DISCONNECTED -> {
                view.showInvalidAction("DISCONNECT", msg.str("nickname"));
                gameLatch.countDown();
            }
            default -> LOG.warning("Tipo messaggio sconosciuto: " + msg.getType());
        }
    } // end dispatch()

    // ── Handler: placement totem ────────────────────────────────────────────

    private void handleRoundStarted(Message msg) {
        totemPlacedCount = 0;
        currentTurnOrder = msg.playerList("turnOrder");
        BoardSnapshot board = msg.boardObj("board");
        view.showRoundStarted(msg.num("round"), currentTurnOrder, board);

        if (!currentTurnOrder.isEmpty()
                && currentTurnOrder.get(0).name().equals(myNickname)) {
            final PlayerSnapshot me = currentTurnOrder.get(0);
            inputExecutor.submit(() -> {
                char chosen = view.requestOfferTileChoice(me, board);
                proxy.send(Message.placeTotem(myNickname, chosen));
            });
        }
    }

    private void handleTotemPlaced(Message msg) {
        BoardSnapshot board = msg.boardObj("board");
        view.showTotemPlaced(msg.playerObj("player"), msg.ch("tileLetter"), board);
        totemPlacedCount++;

        if (totemPlacedCount < currentTurnOrder.size()) {
            PlayerSnapshot next = currentTurnOrder.get(totemPlacedCount);
            if (next.name().equals(myNickname)) {
                inputExecutor.submit(() -> {
                    char chosen = view.requestOfferTileChoice(next, board);
                    proxy.send(Message.placeTotem(myNickname, chosen));
                });
            }
        }
    }

    // ── Handler: choose cards ─────────────────────────────────────────────────

    private void handleCardSlotReady(Message msg) {
        PlayerSnapshot     player        = msg.playerObj("player");
        boolean            isTop         = msg.bool("isTopRow");
        List<CardSnapshot> cards         = msg.cardList("availableCards");
        List<CardSnapshot> visibleEvents = msg.cardList("visibleEvents");

        view.showCardSlotReady(player, isTop, cards, visibleEvents);

        /** The input is requested only from the player whose turn it is.
// NO shutdownNow/recreation is done: the server sends CARD_SLOT_READY
// only when it is actually this player's turn,
// so the tasks queue correctly without ever being "obsolete". */
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

    // ── Handler: post-round (watchtower) ────────────────────────────────

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