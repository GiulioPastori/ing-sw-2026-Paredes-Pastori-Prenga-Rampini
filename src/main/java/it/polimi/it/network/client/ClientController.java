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
 * Controller lato client.
 *
 * <p>Riceve i messaggi dal server e guida la view.
 *
 * <p><b>Regola fondamentale sull'inputExecutor:</b>
 * L'executor è <em>uno solo</em> per tutta la durata della partita e non
 * viene mai ricreato né interrotto durante il gioco. I task di input si
 * accodano naturalmente: il server invia {@code CARD_SLOT_READY} solo
 * quando è effettivamente il turno di quel giocatore, quindi non ci
 * saranno mai task "obsoleti" in coda.
 * L'executor viene terminato solo in {@link #onDisconnect()} e {@link #run()}.
 */
public class ClientController {

    private static final Logger LOG = Logger.getLogger(ClientController.class.getName());

    private final ServerProxy proxy;
    private final GameView    view;
    private final String      myNickname;

    private List<PlayerSnapshot>        currentTurnOrder      = new ArrayList<>();
    private final java.util.Set<String> alreadyPlaced         = new java.util.HashSet<>();
    private final java.util.Set<String> disconnectedNicknames = new java.util.HashSet<>();

    private final CountDownLatch  gameLatch     = new CountDownLatch(1);
    private final ExecutorService inputExecutor = Executors.newSingleThreadExecutor();

    public ClientController(ServerProxy proxy, GameView view, String myNickname) {
        this.proxy      = proxy;
        this.view       = view;
        this.myNickname = myNickname;
    }

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    /** Chiamato quando la connessione cade. */
    public void onDisconnect() {
        inputExecutor.shutdownNow();
        view.showInvalidAction("SERVER", "Connessione persa.");
        gameLatch.countDown();
    }

    /** Blocca finché la partita non finisce. Da chiamare dopo proxy.connect(). */
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
                String who = msg.str("nickname");
                if (who.equals(myNickname)) {
                    // We were kicked — close the client.
                    view.showInvalidAction("DISCONNECT", "Sei stato disconnesso dal server.");
                    gameLatch.countDown();
                } else {
                    disconnectedNicknames.add(who);
                    view.showPlayerDisconnected(who);
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

    // ── Handler: piazzamento totem ────────────────────────────────────────────

    private void handleRoundStarted(Message msg) {
        alreadyPlaced.clear();
        currentTurnOrder = msg.playerList("turnOrder");
        BoardSnapshot board = msg.boardObj("board");
        view.showRoundStarted(msg.num("round"), currentTurnOrder, board);
        promptNextTotemPlacement(board);
    }

    private void handleTotemPlaced(Message msg) {
        PlayerSnapshot placed = msg.playerObj("player");
        BoardSnapshot  board  = msg.boardObj("board");
        view.showTotemPlaced(placed, msg.ch("tileLetter"), board);
        alreadyPlaced.add(placed.name());
        promptNextTotemPlacement(board);
    }

    /**
     * Finds the next player in currentTurnOrder who has not yet placed
     * and is not disconnected, then asks for input if it is our turn.
     *
     * Disconnected players are skipped silently: the server will auto-place
     * them and send a TOTEM_PLACED event, which calls this method again.
     * This correctly handles disconnected players anywhere in the order.
     */
    /**
     * Finds the next player in currentTurnOrder who has not yet placed
     * and is not disconnected, then asks for input if it is our turn.
     * Disconnected players are skipped entirely — the server no longer
     * places their totem, so no TOTEM_PLACED event will arrive for them.
     */
    private void promptNextTotemPlacement(BoardSnapshot board) {
        for (PlayerSnapshot p : currentTurnOrder) {
            if (alreadyPlaced.contains(p.name())) continue;
            if (disconnectedNicknames.contains(p.name())) continue; // skip disconnected
            // Next connected player who must place
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

    // ── Handler: scelta carte ─────────────────────────────────────────────────

    private void handleCardSlotReady(Message msg) {
        PlayerSnapshot     player        = msg.playerObj("player");
        boolean            isTop         = msg.bool("isTopRow");
        List<CardSnapshot> cards         = msg.cardList("availableCards");
        List<CardSnapshot> visibleEvents = msg.cardList("visibleEvents");

        view.showCardSlotReady(player, isTop, cards, visibleEvents);

        // L'input viene richiesto solo al giocatore di turno.
        // NON si fa shutdownNow/ricreazione: il server manda CARD_SLOT_READY
        // solo quando è effettivamente il turno di questo giocatore,
        // quindi i task si accodano correttamente senza mai essere "obsoleti".
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

    // ── Handler: post-round (Torre di Guardia) ────────────────────────────────

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