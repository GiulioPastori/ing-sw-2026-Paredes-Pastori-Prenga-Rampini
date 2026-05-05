package it.polimi.it.view.gui.controllers;

import it.polimi.it.enums.Era;
import it.polimi.it.observer.CardSnapshot;
import it.polimi.it.observer.OfferTileSnapshot;
import it.polimi.it.observer.PlayerSnapshot;
import it.polimi.it.view.gui.widgets.CardCellFactory;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.function.Consumer;

/**
 * Controller della schermata principale di gioco.
 *
 * <p>Collegato a {@code game.fxml}. È stateless rispetto al modello: riceve
 * solo gli aggiornamenti dalla {@code JavaFxView} e popola la UI sul JavaFX
 * Application Thread.
 *
 * <p>Per le richieste di input (scelta carta / tessera) usa due
 * {@link BlockingQueue} configurate dalla {@code JavaFxView}: il click sui
 * nodi mette il valore nella coda, il thread chiamante lato model/network
 * sblocca la {@code take()}.
 */
public class GameBoardController {

    @FXML private BorderPane root;
    @FXML private Label      roundLabel;
    @FXML private Label      eraLabel;
    @FXML private Label      bannerLabel;

    @FXML private HBox       topRowBox;
    @FXML private HBox       offerTrackBox;
    @FXML private HBox       bottomRowBox;
    @FXML private HBox       playersBox;

    @FXML private TextArea   logArea;

    @FXML private HBox       actionBar;
    @FXML private Button     passButton;
    @FXML private Button     declineButton;

    /** Ultimo turn order ricevuto: usato per evidenziare il giocatore attivo. */
    private List<PlayerSnapshot> lastPlayers = new ArrayList<>();
    private String               activePlayer;

    /** Iniettati dalla JavaFxView per ricevere le scelte dell'utente. */
    private BlockingQueue<Integer>   cardChoiceQueue;
    private BlockingQueue<Character> tileChoiceQueue;

    public void wireQueues(BlockingQueue<Integer> cardQueue,
                           BlockingQueue<Character> tileQueue) {
        this.cardChoiceQueue = cardQueue;
        this.tileChoiceQueue = tileQueue;
    }

    // ── Aggiornamenti push (chiamate da JavaFxView, già su FX thread) ─────────

    public void onRoundStarted(int round, List<PlayerSnapshot> turnOrder,
                               List<OfferTileSnapshot> tiles) {
        runFx(() -> {
            roundLabel.setText("Round " + round);
            this.lastPlayers = new ArrayList<>(turnOrder);
            this.activePlayer = null;
            renderPlayers();
            renderTiles(tiles, /*selectable*/ false);
            log("══ ROUND " + round + " STARTED ══");
        });
    }

    public void onTotemPlaced(PlayerSnapshot player, char tileLetter) {
        runFx(() -> log(player.name() + " ha piazzato un totem sulla tessera " + tileLetter));
    }

    public void onCardSlotReady(PlayerSnapshot player, boolean isTopRow,
                                List<CardSnapshot> available,
                                List<CardSnapshot> visibleEvents) {
        runFx(() -> {
            this.activePlayer = player.name();
            renderPlayers();
            log("[" + player.name() + "] turno: scegli una carta dalla riga "
                    + (isTopRow ? "superiore" : "inferiore") + ".");

            // Mostra le carte disponibili sulla riga giusta. La selectability
            // verrà attivata in requestCardChoice/requestPostRoundCardChoice.
            // visibleEvents (non acquisibili) sono mostrati con stile "event".
            List<CardSnapshot> all = new ArrayList<>(available);
            all.addAll(visibleEvents);
            HBox target = isTopRow ? topRowBox : bottomRowBox;
            renderCards(target, all, null);
        });
    }

    public void onCardAcquired(PlayerSnapshot player, CardSnapshot card) {
        runFx(() -> log(player.name() + " ha acquisito la carta #" + card.id()));
    }

    public void onCardAcquisitionFailed(PlayerSnapshot player, CardSnapshot card, int missingFood) {
        runFx(() -> log(player.name() + " non può permettersi la carta #" + card.id()
                + " (mancano " + missingFood + " food)"));
    }

    public void onTotemReturnedToTurnOrder(PlayerSnapshot player, int position) {
        runFx(() -> log(player.name() + " → Turn Order #" + position));
    }

    public void onPostRoundStarted(List<PlayerSnapshot> watchtowerPlayers,
                                   List<CardSnapshot> available) {
        runFx(() -> {
            log("[POST-ROUND] Watchtower: "
                    + watchtowerPlayers.stream()
                    .map(PlayerSnapshot::name)
                    .reduce((a, b) -> a + ", " + b).orElse("nessuno"));
            renderCards(topRowBox, available, null);
        });
    }

    public void onEventResolved(int eventId, Era era, it.polimi.it.enums.EventType type) {
        runFx(() -> log("[EVENTO] " + era + " — " + type));
    }

    public void onEraChanged(Era era) {
        runFx(() -> {
            eraLabel.setText("Era " + era);
            log("★ ERA " + era + " ★");
            bannerLabel.setText("Inizia la " + era);
        });
    }

    public void onRoundEnded(int round) {
        runFx(() -> log("── Round " + round + " terminato ──"));
    }

    public void onInvalidAction(String playerName, String reason) {
        runFx(() -> log("[!] " + playerName + ": " + reason));
    }

    // ── Richieste di input (chiamate da JavaFxView, già su FX thread) ─────────

    /**
     * Mostra le tessere come selezionabili. Il click pushed sulla coda gestita
     * dalla view.
     */
    public void promptOfferTileChoice(PlayerSnapshot player, List<OfferTileSnapshot> freeTiles) {
        runFx(() -> {
            this.activePlayer = player.name();
            renderPlayers();
            bannerLabel.setText("[" + player.name() + "] scegli una tessera.");
            renderTiles(freeTiles, /*selectable*/ true);
        });
    }

    /**
     * Mostra le carte come selezionabili. Inserisce {@code -1} sulla coda se
     * l'utente preme "Passa".
     */
    public void promptCardChoice(PlayerSnapshot player,
                                 List<CardSnapshot> available,
                                 boolean showPass) {
        runFx(() -> {
            this.activePlayer = player.name();
            renderPlayers();
            bannerLabel.setText("[" + player.name() + "] scegli una carta"
                    + (showPass ? " o passa." : "."));
            renderCards(topRowBox, available, this::pushCardChoice);

            // Mostra il bottone giusto per questa fase
            setActionButton(passButton, showPass, () -> pushCardChoice(-1));
            setActionButton(declineButton, false, null);
        });
    }

    /** Variante per il post-round / Watchtower (rifiuta invece di passa). */
    public void promptPostRoundCardChoice(PlayerSnapshot player, List<CardSnapshot> available) {
        runFx(() -> {
            this.activePlayer = player.name();
            renderPlayers();
            bannerLabel.setText("[Watchtower] " + player.name() + ": scegli o rifiuta.");
            renderCards(topRowBox, available, this::pushCardChoice);

            setActionButton(declineButton, true, () -> pushCardChoice(-1));
            setActionButton(passButton, false, null);
        });
    }

    public void onGameStartingHandover(String myNickname) {
        // chiamato dalla view dopo che tutto è inizializzato
        runFx(() -> bannerLabel.setText("Connesso come " + myNickname));
    }

    // ── Pulsanti azione ───────────────────────────────────────────────────────

    @FXML
    private void onPass() {
        pushCardChoice(-1);
    }

    @FXML
    private void onDecline() {
        pushCardChoice(-1);
    }

    private void setActionButton(Button btn, boolean visible, Runnable action) {
        btn.setVisible(visible);
        btn.setManaged(visible);
        if (visible && action != null) {
            btn.setOnAction(e -> action.run());
        }
    }

    // ── Push di scelta sulle code ─────────────────────────────────────────────

    private void pushCardChoice(int id) {
        if (cardChoiceQueue != null) cardChoiceQueue.offer(id);
        bannerLabel.setText("");
        // Disabilita ulteriori click finché la view non riapre il prompt
        clearCardSelectability();
        passButton.setVisible(false);    passButton.setManaged(false);
        declineButton.setVisible(false); declineButton.setManaged(false);
    }

    private void pushTileChoice(char letter) {
        if (tileChoiceQueue != null) tileChoiceQueue.offer(letter);
        bannerLabel.setText("");
    }

    // ── Render helpers ────────────────────────────────────────────────────────

    private void renderCards(HBox target, List<CardSnapshot> cards, Consumer<Integer> onClick) {
        target.getChildren().clear();
        for (CardSnapshot c : cards) {
            // Se è un evento non si può comunque cliccare (CardCellFactory già gestisce)
            Consumer<Integer> handler = (c.isEvent() ? null : onClick);
            StackPane cell = CardCellFactory.buildCard(c, handler);
            target.getChildren().add(cell);
        }
    }

    private void clearCardSelectability() {
        for (var node : topRowBox.getChildren()) {
            node.getStyleClass().remove("selectable");
            node.setOnMouseClicked(null);
        }
        for (var node : bottomRowBox.getChildren()) {
            node.getStyleClass().remove("selectable");
            node.setOnMouseClicked(null);
        }
    }

    private void renderTiles(List<OfferTileSnapshot> tiles, boolean selectable) {
        offerTrackBox.getChildren().clear();
        for (OfferTileSnapshot t : tiles) {
            Consumer<Character> onClick = selectable ? this::pushTileChoice : null;
            offerTrackBox.getChildren().add(CardCellFactory.buildTile(t, onClick));
        }
    }

    private void renderPlayers() {
        playersBox.getChildren().clear();
        for (PlayerSnapshot p : lastPlayers) {
            boolean active = p.name().equals(activePlayer);
            playersBox.getChildren().add(CardCellFactory.buildPlayerCard(p, active));
        }
    }

    private void log(String msg) {
        logArea.appendText(msg + "\n");
    }

    /** Esegue il task sul JavaFX Application Thread. Se siamo già lì lo esegue subito. */
    private static void runFx(Runnable r) {
        if (Platform.isFxApplicationThread()) r.run();
        else Platform.runLater(r);
    }

    /** Espone il root per chi vuole sostituire la scena (es. game over). */
    public BorderPane getRoot() { return root; }
}
