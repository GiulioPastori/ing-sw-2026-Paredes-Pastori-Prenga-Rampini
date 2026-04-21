package it.polimi.it.network.client;

import it.polimi.it.enums.Era;
import it.polimi.it.enums.EventType;
import it.polimi.it.network.message.Message;
import it.polimi.it.observer.CardSnapshot;
import it.polimi.it.observer.OfferTileSnapshot;
import it.polimi.it.observer.PlayerSnapshot;
import it.polimi.it.view.GameView;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.logging.Logger;

/**
 * Client-side controller.
 * Receives Messages from the server and drives the view.
 * Uses Message (org.json) — no typed record subtypes.
 */
public class ClientController {

    private static final Logger LOG = Logger.getLogger(ClientController.class.getName());

    private final ServerProxy proxy;
    private final GameView    view;
    private final String      myNickname;

    private final CountDownLatch gameLatch = new CountDownLatch(1);

    public ClientController(ServerProxy proxy, GameView view, String myNickname) {
        this.proxy      = proxy;
        this.view       = view;
        this.myNickname = myNickname;
    }

    /** Blocks until the game ends. Call after proxy.connect(). */
    public void run() {
        try { gameLatch.await(); }
        catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }

    // ── Dispatch ──────────────────────────────────────────────────────────────

    public void onMessage(Message msg) {
        switch (msg.getType()) {
            case Message.LOBBY_UPDATE ->
                    System.out.println("Lobby: " + msg.strList("connectedPlayers").size()
                            + "/" + msg.num("expectedPlayers")
                            + " — " + msg.strList("connectedPlayers"));
            case Message.ERROR ->
                    view.showInvalidAction("SERVER", msg.str("reason"));
            case Message.ROUND_STARTED ->
                    handleRoundStarted(msg);
            case Message.TOTEM_PLACED ->
                    view.showTotemPlaced(msg.playerObj("player"), msg.ch("tileLetter"));
            case Message.CARD_SLOT_READY ->
                    handleCardSlotReady(msg);
            case Message.CARD_ACQUIRED ->
                    view.showCardAcquired(msg.playerObj("player"), msg.cardObj("card"));
            case Message.CARD_ACQUISITION_FAILED ->
                    handleCardAcquisitionFailed(msg);
            case Message.TOTEM_RETURNED ->
                    view.showTotemReturnedToTurnOrder(msg.playerObj("player"), msg.num("position"));
            case Message.POST_ROUND_STARTED ->
                    handlePostRoundStarted(msg);
            case Message.EVENT_RESOLVED ->
                    view.showEventResolved(
                            msg.num("eventId"),
                            Era.valueOf(msg.str("era")),
                            EventType.valueOf(msg.str("type")));
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
            case Message.PING -> proxy.send(Message.pong());
            default -> LOG.warning("Unknown message type: " + msg.getType());
        }
    }

    public void onDisconnect() {
        view.showInvalidAction("SERVER", "Connection lost.");
        gameLatch.countDown();
    }

    // ── Handlers ──────────────────────────────────────────────────────────────

    private void handleRoundStarted(Message msg) {
        List<PlayerSnapshot>    order = msg.playerList("turnOrder");
        List<OfferTileSnapshot> tiles = msg.tileList("availableTiles");
        view.showRoundStarted(msg.num("round"), order);

        if (!order.isEmpty() && order.get(0).name().equals(myNickname)) {
            char chosen = view.requestOfferTileChoice(order.get(0), tiles);
            proxy.send(Message.placeTotem(myNickname, chosen));
        }
    }

    private void handleCardSlotReady(Message msg) {
        PlayerSnapshot     player = msg.playerObj("player");
        boolean            isTop  = msg.bool("isTopRow");
        List<CardSnapshot> cards  = msg.cardList("availableCards");

        view.showCardSlotReady(player, isTop, cards);

        if (!player.name().equals(myNickname)) return;

        int chosen = view.requestCardChoice(player, cards);
        if (chosen == -1) proxy.send(Message.pass(myNickname));
        else              proxy.send(Message.takeCard(myNickname, chosen, isTop));
    }

    private void handleCardAcquisitionFailed(Message msg) {
        PlayerSnapshot     player       = msg.playerObj("player");
        CardSnapshot       card         = msg.cardObj("card");
        List<CardSnapshot> alternatives = msg.cardList("alternatives");

        view.showCardAcquisitionFailed(player, card, msg.num("missingFood"));

        if (!player.name().equals(myNickname)) return;

        int chosen = view.requestCardChoice(player, alternatives);
        if (chosen == -1) proxy.send(Message.pass(myNickname));
        else              proxy.send(Message.takeCard(myNickname, chosen, msg.bool("fromTopRow")));
    }

    private void handlePostRoundStarted(Message msg) {
        List<PlayerSnapshot> watchtower = msg.playerList("watchtowerPlayers");
        List<CardSnapshot>   cards      = msg.cardList("availableCards");

        view.showPostRoundStarted(watchtower, cards);

        boolean isMine = watchtower.stream().anyMatch(p -> p.name().equals(myNickname));
        if (!isMine) return;

        PlayerSnapshot me = watchtower.stream()
                .filter(p -> p.name().equals(myNickname))
                .findFirst().orElseThrow();

        int chosen = view.requestPostRoundCardChoice(me, cards);
        proxy.send(Message.postRound(myNickname, chosen));
    }
}