package it.polimi.it.network.client;

import it.polimi.it.enums.Era;
import it.polimi.it.enums.EventType;
import it.polimi.it.network.message.Message;
import it.polimi.it.observer.CardSnapshot;
import it.polimi.it.observer.OfferTileSnapshot;
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
 * Receives Messages from the server and drives the view.
 * Uses Message (org.json) — no typed record subtypes.
 */
public class ClientController {

    private static final Logger LOG = Logger.getLogger(ClientController.class.getName());
    private int totemPlacedCount = 0;
    private final ServerProxy proxy;
    private final GameView    view;
    private final String      myNickname;
    private List<PlayerSnapshot> currentTurnOrder = new ArrayList<>();
    private final CountDownLatch gameLatch = new CountDownLatch(1);
    private final ExecutorService inputExecutor = Executors.newSingleThreadExecutor();
    public ClientController(ServerProxy proxy, GameView view, String myNickname) {
        this.proxy      = proxy;
        this.view       = view;
        this.myNickname = myNickname;
    }

    /** Blocks until the game ends. Call after proxy.connect(). */
    public void onDisconnect() {
        inputExecutor.shutdownNow();
        view.showInvalidAction("SERVER", "Connection lost.");
        gameLatch.countDown();
    }

    public void run() {
        try { gameLatch.await(); }
        catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        finally { inputExecutor.shutdownNow(); }
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
                    handleTotemPlaced(msg);
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




    // ── Handlers ──────────────────────────────────────────────────────────────

    private void handleRoundStarted(Message msg) {
        totemPlacedCount = 0;
        currentTurnOrder = msg.playerList("turnOrder");   // ← salva l'ordine
        List<OfferTileSnapshot> tiles = msg.tileList("availableTiles");
        view.showRoundStarted(msg.num("round"), currentTurnOrder);

        // chiedi solo se sei il primo
        if (!currentTurnOrder.isEmpty() && currentTurnOrder.get(0).name().equals(myNickname)) {
            final PlayerSnapshot me = currentTurnOrder.get(0);
            inputExecutor.submit(() -> {
                char chosen = view.requestOfferTileChoice(me, tiles);
                proxy.send(Message.placeTotem(myNickname, chosen));
            });
        }
    }
    private void handleTotemPlaced(Message msg) {
        view.showTotemPlaced(msg.playerObj("player"), msg.ch("tileLetter"));

        List<OfferTileSnapshot> remaining = msg.tileList("remainingTiles");


        // il prossimo a piazzare è il primo in turnOrder che non ha ancora piazzato.
        // Contiamo quanti hanno già piazzato = players - remaining - 1 (una tile rimane sempre libera)
        // Oppure usiamo l'ordine: chi ha già piazzato = tutti tranne gli ultimi (remaining.size()) in ordine
        // scorriamo turnOrder e vediamo chi è il prossimo che non ha ancora piazzato
        // Il server garantisce l'ordine sequenziale, quindi:
        // dopo N totem piazzati, il prossimo è turnOrder[N]
        // Contiamo i TOTEM_PLACED ricevuti finora tramite un contatore
        totemPlacedCount++;

        if (totemPlacedCount < currentTurnOrder.size()) {
            PlayerSnapshot next = currentTurnOrder.get(totemPlacedCount);
            if (next.name().equals(myNickname)) {
                inputExecutor.submit(() -> {
                    char chosen = view.requestOfferTileChoice(next, remaining);
                    proxy.send(Message.placeTotem(myNickname, chosen));
                });
            }
        }
    }
    private void handleCardSlotReady(Message msg) {
        PlayerSnapshot     player = msg.playerObj("player");
        boolean            isTop  = msg.bool("isTopRow");
        List<CardSnapshot> cards  = msg.cardList("availableCards");

        view.showCardSlotReady(player, isTop, cards);

        if (!player.name().equals(myNickname)) return;

        inputExecutor.submit(() -> {
            int chosen = view.requestCardChoice(player, cards);
            if (chosen == -1) proxy.send(Message.pass(myNickname));
            else              proxy.send(Message.takeCard(myNickname, chosen, isTop));
        });
    }

    private void handleCardAcquisitionFailed(Message msg) {
        PlayerSnapshot player = msg.playerObj("player");
        CardSnapshot card = msg.cardObj("card");
        List<CardSnapshot> alternatives = msg.cardList("alternatives");

        view.showCardAcquisitionFailed(player, card, msg.num("missingFood"));

        if (!player.name().equals(myNickname)) return;

        inputExecutor.submit(() -> {
            int chosen = view.requestCardChoice(player, alternatives);
            if (chosen == -1) proxy.send(Message.pass(myNickname));
            else proxy.send(Message.takeCard(myNickname, chosen, msg.bool("fromTopRow")));
        });
    }

    private void handlePostRoundStarted(Message msg) {
        List<PlayerSnapshot> watchtower = msg.playerList("watchtowerPlayers");
        List<CardSnapshot> cards = msg.cardList("availableCards");

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