package it.polimi.it.view.gui;

import it.polimi.it.enums.Era;
import it.polimi.it.enums.TotemColour;
import it.polimi.it.enums.EventType;
import it.polimi.it.observer.CardSnapshot;
import it.polimi.it.observer.BoardSnapshot;
import it.polimi.it.observer.OfferTileSnapshot;
import it.polimi.it.observer.PlayerSnapshot;
import it.polimi.it.view.GameView;
import it.polimi.it.view.gui.controllers.GameBoardView;
import it.polimi.it.view.gui.controllers.LobbyController;
import it.polimi.it.view.gui.controllers.GameOverController;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Implementazione JavaFX di {@link GameView}.
 *
 * <p>È istanziata sul thread di rete (dal {@link it.polimi.it.network.client.ClientController})
 * e fa da ponte verso il {@link GameBoardView} che vive sul JavaFX
 * Application Thread.
 *
 * <p><b>Threading:</b>
 * <ul>
 *   <li>Tutti i metodi {@code showXxx} sono asincroni: incapsulano la
 *       chiamata in {@link Platform#runLater(Runnable)}.</li>
 *   <li>Tutti i metodi {@code requestXxx} sono bloccanti: riempiono il
 *       prompt sulla UI e attendono la scelta dell'utente tramite
 *       {@link BlockingQueue}.</li>
 * </ul>
 *
 * <p><b>Race-free initialisation:</b> il network thread può ricevere i primi
 * messaggi del server (in particolare {@code ROUND_STARTED}) <em>prima</em>
 * che {@link #showBoard(String)} abbia caricato l'FXML e creato il
 * {@link GameBoardView}. Per evitare che questi eventi vengano persi,
 * tutti i metodi {@code showXxx}/{@code requestXxx} passano per
 * {@link #whenReady(Consumer)}: se il controller è pronto eseguono subito,
 * altrimenti vengono accodati in {@link #pendingActions} e replayati in
 * ordine non appena {@link #showBoard(String)} ha terminato l'inizializzazione.
 *
 * <p>La coda è solo "pre-init": una volta riempito il riferimento al
 * controller, ogni successiva chiamata viene eseguita immediatamente.
 */
public class JavaFxView implements GameView {

    private static final Logger LOG = Logger.getLogger(JavaFxView.class.getName());

    private final Stage           primaryStage;
    private final LobbyController lobbyController;

    /**
     * Set on the JavaFX thread inside {@link #showBoard(String)}.
     * Read from any thread; writes are guarded by {@link #lock} together
     * with the drain of {@link #pendingActions}.
     */
    private volatile GameBoardView boardController;

    /** Lock that protects the controller-pending-queue handover. */
    private final Object lock = new Object();

    /**
     * Actions queued before the board controller was ready. Drained in order
     * once {@link #showBoard(String)} completes. Empty thereafter.
     */
    private final List<Consumer<GameBoardView>> pendingActions = new ArrayList<>();

    private final BlockingQueue<Integer>   cardChoiceQueue = new ArrayBlockingQueue<>(1);
    private final BlockingQueue<Character> tileChoiceQueue = new ArrayBlockingQueue<>(1);

    /**
     * @param primaryStage lo {@link Stage} principale dove montare la board.
     *                     Tipicamente la stessa usata dalla lobby — la view
     *                     ne sostituisce la scena.
     */
    public JavaFxView(Stage primaryStage, LobbyController lobbyController) {
        this.lobbyController = lobbyController;
        this.primaryStage = primaryStage;
    }

    /**
     * Costruisce e mostra la board nello stage. Da chiamare DOPO che la lobby
     * ha confermato la connessione; sincronizza la chiamata sul JavaFX
     * Application Thread.
     *
     * <p>Quando questo metodo termina, il {@link GameBoardView} è
     * disponibile e qualsiasi evento precedentemente accodato viene replayato.
     */
    public void showBoard(String myNickname) {
        CountDownLatch ready = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                FXMLLoader loader = new FXMLLoader(
                        getClass().getResource("/it/polimi/it/view/gui/game.fxml"));
                javafx.scene.layout.StackPane boardRoot = loader.load();
                GameBoardView bc = loader.getController();
                bc.bindInputQueues(cardChoiceQueue, tileChoiceQueue);
                bc.onGameStartingHandover(myNickname);

                Scene scene = new Scene(boardRoot, 1280, 800);
                scene.getStylesheets().add(
                        getClass().getResource("/it/polimi/it/view/gui/styles.css").toExternalForm());
                primaryStage.setScene(scene);
                primaryStage.setTitle("MESOS — " + myNickname);
                primaryStage.show();

                // Publish the controller and drain anything queued in the meantime.
                List<Consumer<GameBoardView>> toReplay;
                synchronized (lock) {
                    boardController = bc;
                    toReplay = new ArrayList<>(pendingActions);
                    pendingActions.clear();
                }
                if (!toReplay.isEmpty()) {
                    LOG.info("Replaying " + toReplay.size() + " event(s) buffered before board init");
                    toReplay.forEach(a -> a.accept(bc));
                }
            } catch (IOException e) {
                LOG.log(Level.SEVERE, "Impossibile caricare game.fxml", e);
            } finally {
                ready.countDown();
            }
        });
        try { ready.await(); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }

    /**
     * Runs {@code action} against the board controller as soon as it is ready.
     * If the controller already exists, the action runs immediately (on the
     * caller's thread; controller methods themselves dispatch to the FX thread).
     * Otherwise the action is queued and replayed by {@link #showBoard(String)}.
     */
    private void whenReady(Consumer<GameBoardView> action) {
        GameBoardView bc = boardController;
        if (bc != null) {
            action.accept(bc);
            return;
        }
        synchronized (lock) {
            bc = boardController;
            if (bc != null) {
                action.accept(bc);
            } else {
                pendingActions.add(action);
            }
        }
    }

    // ── show* (push) ──────────────────────────────────────────────────────────

    @Override
    public void showLobbyUpdate(java.util.List<String> players, int needed) {
        Platform.runLater(() -> lobbyController.showLobbyWaiting("", players, needed));
        // Also update the in-board cover if the board is already loaded
        whenReady(bc -> bc.updateBoardWaiting(players, needed));
    }

    @Override
    public void showRoundStarted(int round, List<PlayerSnapshot> turnOrder,
                                 BoardSnapshot board) {
        whenReady(bc -> bc.onRoundStarted(round, turnOrder, board));
    }

    @Override
    public void showTotemPlaced(PlayerSnapshot player, char tileLetter,
                                BoardSnapshot board) {
        whenReady(bc -> bc.onTotemPlaced(player, tileLetter, board));
    }

    @Override
    public void showCardSlotReady(PlayerSnapshot player, boolean isTopRow,
                                  List<CardSnapshot> availableCards,
                                  List<CardSnapshot> visibleEvents) {
        whenReady(bc -> bc.onCardSlotReady(player, isTopRow, availableCards, visibleEvents));
    }

    @Override
    public void showCardAcquired(PlayerSnapshot player, CardSnapshot card) {
        whenReady(bc -> bc.onCardAcquired(player, card));
    }

    @Override
    public void showCardAcquisitionFailed(PlayerSnapshot player, CardSnapshot card,
                                          int missingFood) {
        whenReady(bc -> bc.onCardAcquisitionFailed(player, card, missingFood));
    }

    @Override
    public void showTotemReturnedToTurnOrder(PlayerSnapshot player, int position,
                                             BoardSnapshot board) {
        whenReady(bc -> bc.onTotemReturnedToTurnOrder(player, position, board));
    }

    @Override
    public void showPostRoundStarted(List<PlayerSnapshot> watchtowerPlayers,
                                     List<CardSnapshot> availableCards) {
        whenReady(bc -> bc.onPostRoundStarted(watchtowerPlayers, availableCards));
    }

    @Override
    public void showEventResolved(int eventId, Era era, EventType type) {
        whenReady(bc -> bc.onEventResolved(eventId, era, type));
    }

    @Override
    public void showEraChanged(Era newEra) {
        whenReady(bc -> bc.onEraChanged(newEra));
    }

    @Override
    public void showRoundEnded(int round) {
        whenReady(bc -> bc.onRoundEnded(round));
    }

    @Override
    public void showInvalidAction(String playerName, String reason) {
        whenReady(bc -> bc.onInvalidAction(playerName, reason));
    }

    @Override
    public void showPlayerDisconnected(String nickname) {
        whenReady(bc -> bc.onPlayerDisconnected(nickname));
    }

    @Override
    public void showPlayerReconnected(String nickname) {
        whenReady(bc -> bc.onPlayerReconnected(nickname));
    }

    @Override
    public void showGameEnded(List<PlayerSnapshot> winners,
                              List<PlayerSnapshot> finalScores) {
        whenReady(bc -> Platform.runLater(() -> {
            try {
                FXMLLoader loader = new FXMLLoader(
                        getClass().getResource("/it/polimi/it/view/gui/gameover.fxml"));
                VBox root = loader.load();
                GameOverController c = loader.getController();
                c.setResults(winners, finalScores);

                Scene scene = new Scene(root, 640, 640);
                scene.getStylesheets().add(
                        getClass().getResource("/it/polimi/it/view/gui/styles.css").toExternalForm());
                primaryStage.setScene(scene);
                primaryStage.setTitle("MESOS — Game Over");
            } catch (IOException e) {
                LOG.log(Level.SEVERE, "Impossibile caricare gameover.fxml", e);
            }
        }));
    }

    // ── request* (pull, blocking) ─────────────────────────────────────────────

    @Override
    public char requestOfferTileChoice(PlayerSnapshot player, BoardSnapshot board) {
        char fallback = board.offerTiles().stream()
                .filter(OfferTileSnapshot::isFree)
                .map(OfferTileSnapshot::letter)
                .findFirst().orElse('?');
        whenReady(bc -> bc.promptOfferTileChoice(player, board));
        try {
            return tileChoiceQueue.take();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return fallback;
        }
    }

    @Override
    public int requestCardChoice(PlayerSnapshot player, List<CardSnapshot> available) {
        if (available.isEmpty()) return -1;
        whenReady(bc -> bc.promptCardChoice(player, available, /*showPass*/ true));
        try {
            return cardChoiceQueue.take();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return -1;
        }
    }

    @Override
    public int requestPostRoundCardChoice(PlayerSnapshot player, List<CardSnapshot> available) {
        if (available.isEmpty()) return -1;
        whenReady(bc -> bc.promptPostRoundCardChoice(player, available));
        try {
            return cardChoiceQueue.take();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return -1;
        }
    }
}