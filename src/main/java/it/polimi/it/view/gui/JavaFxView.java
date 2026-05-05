package it.polimi.it.view.gui;

import it.polimi.it.enums.Era;
import it.polimi.it.enums.EventType;
import it.polimi.it.observer.CardSnapshot;
import it.polimi.it.observer.OfferTileSnapshot;
import it.polimi.it.observer.PlayerSnapshot;
import it.polimi.it.view.GameView;
import it.polimi.it.view.gui.controllers.GameBoardController;
import it.polimi.it.view.gui.controllers.GameOverController;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Implementazione JavaFX di {@link GameView}.
 *
 * <p>È istanziata sul thread di rete (dal {@link it.polimi.it.network.client.ClientController})
 * e fa da ponte verso il {@link GameBoardController} che vive sul JavaFX
 * Application Thread.
 *
 * <p><b>Threading:</b>
 * <ul>
 *   <li>Tutti i metodi {@code showXxx} sono asincroni: incapsulano la
 *       chiamata in {@link Platform#runLater(Runnable)}.</li>
 *   <li>Tutti i metodi {@code requestXxx} sono bloccanti: riempiono il
 *       prompt sulla UI e attendono la scelta dell'utente tramite
 *       {@link BlockingQueue}, esattamente come faceva l'implementazione
 *       Swing originale.</li>
 * </ul>
 *
 * <p><b>Inizializzazione:</b> il costruttore richiede che il JavaFX runtime
 * sia già avviato (lo fa {@link JavaFxApp#start(Stage)}). La view crea il
 * board, lo monta nello stage che le viene passato e attende le chiamate
 * dal controller di rete.
 */
public class JavaFxView implements GameView {

    private static final Logger LOG = Logger.getLogger(JavaFxView.class.getName());

    private final Stage primaryStage;
    private GameBoardController boardController;

    private final BlockingQueue<Integer>   cardChoiceQueue = new ArrayBlockingQueue<>(1);
    private final BlockingQueue<Character> tileChoiceQueue = new ArrayBlockingQueue<>(1);

    /**
     * @param primaryStage lo {@link Stage} principale dove montare la board.
     *                     Tipicamente la stessa usata dalla lobby — la view
     *                     ne sostituisce la scena.
     */
    public JavaFxView(Stage primaryStage) {
        this.primaryStage = primaryStage;
    }

    /**
     * Costruisce e mostra la board nello stage. Da chiamare DOPO che la lobby
     * ha confermato la connessione; sincronizza la chiamata sul JavaFX
     * Application Thread.
     */
    public void showBoard(String myNickname) {
        CountDownLatch ready = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                FXMLLoader loader = new FXMLLoader(
                        getClass().getResource("/it/polimi/it/view/gui/game.fxml"));
                javafx.scene.layout.BorderPane boardRoot = loader.load();
                boardController = loader.getController();
                boardController.wireQueues(cardChoiceQueue, tileChoiceQueue);
                boardController.onGameStartingHandover(myNickname);

                Scene scene = new Scene(boardRoot, 1280, 800);
                scene.getStylesheets().add(
                        getClass().getResource("/it/polimi/it/view/gui/styles.css").toExternalForm());
                primaryStage.setScene(scene);
                primaryStage.setTitle("MESOS — " + myNickname);
                primaryStage.show();
            } catch (IOException e) {
                LOG.log(Level.SEVERE, "Impossibile caricare game.fxml", e);
            } finally {
                ready.countDown();
            }
        });
        try { ready.await(); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }

    // ── show* (push) ──────────────────────────────────────────────────────────

    @Override
    public void showRoundStarted(int round, List<PlayerSnapshot> turnOrder,
                                 List<OfferTileSnapshot> availableTiles) {
        if (boardController != null)
            boardController.onRoundStarted(round, turnOrder, availableTiles);
    }

    @Override
    public void showTotemPlaced(PlayerSnapshot player, char tileLetter) {
        if (boardController != null) boardController.onTotemPlaced(player, tileLetter);
    }

    @Override
    public void showCardSlotReady(PlayerSnapshot player, boolean isTopRow,
                                  List<CardSnapshot> availableCards,
                                  List<CardSnapshot> visibleEvents) {
        if (boardController != null)
            boardController.onCardSlotReady(player, isTopRow, availableCards, visibleEvents);
    }

    @Override
    public void showCardAcquired(PlayerSnapshot player, CardSnapshot card) {
        if (boardController != null) boardController.onCardAcquired(player, card);
    }

    @Override
    public void showCardAcquisitionFailed(PlayerSnapshot player, CardSnapshot card,
                                          int missingFood) {
        if (boardController != null)
            boardController.onCardAcquisitionFailed(player, card, missingFood);
    }

    @Override
    public void showTotemReturnedToTurnOrder(PlayerSnapshot player, int position) {
        if (boardController != null)
            boardController.onTotemReturnedToTurnOrder(player, position);
    }

    @Override
    public void showPostRoundStarted(List<PlayerSnapshot> watchtowerPlayers,
                                     List<CardSnapshot> availableCards) {
        if (boardController != null)
            boardController.onPostRoundStarted(watchtowerPlayers, availableCards);
    }

    @Override
    public void showEventResolved(int eventId, Era era, EventType type) {
        if (boardController != null) boardController.onEventResolved(eventId, era, type);
    }

    @Override
    public void showEraChanged(Era newEra) {
        if (boardController != null) boardController.onEraChanged(newEra);
    }

    @Override
    public void showRoundEnded(int round) {
        if (boardController != null) boardController.onRoundEnded(round);
    }

    @Override
    public void showInvalidAction(String playerName, String reason) {
        if (boardController != null) boardController.onInvalidAction(playerName, reason);
    }

    @Override
    public void showGameEnded(List<PlayerSnapshot> winners,
                              List<PlayerSnapshot> finalScores) {
        Platform.runLater(() -> {
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
        });
    }

    // ── request* (pull, blocking) ─────────────────────────────────────────────

    @Override
    public char requestOfferTileChoice(PlayerSnapshot player,
                                       List<OfferTileSnapshot> freeTiles) {
        if (boardController == null) {
            // failsafe: non blocchiamo se la board non è pronta
            return freeTiles.isEmpty() ? '?' : freeTiles.get(0).letter();
        }
        boardController.promptOfferTileChoice(player, freeTiles);
        try {
            return tileChoiceQueue.take();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return freeTiles.isEmpty() ? '?' : freeTiles.get(0).letter();
        }
    }

    @Override
    public int requestCardChoice(PlayerSnapshot player, List<CardSnapshot> available) {
        if (available.isEmpty() || boardController == null) return -1;
        boardController.promptCardChoice(player, available, /*showPass*/ true);
        try {
            return cardChoiceQueue.take();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return -1;
        }
    }

    @Override
    public int requestPostRoundCardChoice(PlayerSnapshot player,
                                          List<CardSnapshot> available) {
        if (available.isEmpty() || boardController == null) return -1;
        boardController.promptPostRoundCardChoice(player, available);
        try {
            return cardChoiceQueue.take();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return -1;
        }
    }
}
