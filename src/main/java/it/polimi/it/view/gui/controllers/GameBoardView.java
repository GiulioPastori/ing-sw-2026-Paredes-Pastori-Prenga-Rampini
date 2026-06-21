package it.polimi.it.view.gui.controllers;

import it.polimi.it.enums.Era;
import it.polimi.it.enums.TotemColour;
import it.polimi.it.enums.EventType;
import it.polimi.it.observer.BoardSnapshot;
import it.polimi.it.observer.CardSnapshot;
import it.polimi.it.observer.OfferTileSnapshot;
import it.polimi.it.observer.PlayerSnapshot;
import it.polimi.it.observer.PlayerResourceDelta;
import it.polimi.it.view.gui.widgets.CardCellFactory;
import it.polimi.it.view.gui.widgets.CardImageCache;
import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.animation.SequentialTransition;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.NumberBinding;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.geometry.Pos;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.function.Consumer;

/**
 * JavaFX view controller for the main game scene ({@code game.fxml}).
 *
 * <p>This is the JavaFX-side view component, not the MVC Controller — that
 * role belongs to {@link it.polimi.it.GameController}. This class only
 * receives snapshots from {@link it.polimi.it.view.gui.JavaFxView} and
 * paints them; it never touches the model.
 *
 * <p>State kept here is purely UI state:
 * <ul>
 *   <li>{@link #lastBoard}: most recent {@link BoardSnapshot}, used to
 *       repaint after partial events (e.g. a card acquisition that updates
 *       a player but does not change the board).</li>
 *   <li>{@link #players}: per-name map of player snapshots, updated as
 *       events arrive so each player's tribe stays visible to everyone.</li>
 *   <li>{@link #activePlayer}: nickname of the player currently choosing,
 *       so the corresponding panel is highlighted.</li>
 * </ul>
 *
 * <p>For input requests the controller exposes {@code prompt*} methods that
 * push the user's choice onto blocking queues injected by the {@code JavaFxView}.
 */
public class GameBoardView {

    private static final double DESIGN_WIDTH  = 1280.0;
    private static final double DESIGN_HEIGHT = 800.0;

    @FXML private StackPane  sceneRoot;
    @FXML private BorderPane root;
    @FXML private ImageView  backgroundImageView;
    @FXML private Label      roundLabel;
    @FXML private Label      eraLabel;
    @FXML private Label      bannerLabel;
    @FXML private Label      topRowTitle;
    @FXML private Label      bottomRowTitle;
    @FXML private Label      drawPromptLabel;

    @FXML private HBox       topRowBox;
    @FXML private HBox       bottomRowBox;

    /** Container for: turn-order-tile  +  offer-track  +  deck */
    @FXML private HBox       offerTrackBox;
    @FXML private HBox       turnOrderBox;   // left of offerTrackBox
    @FXML private HBox       deckBox;        // right of offerTrackBox

    @FXML private HBox       playersBox;

    @FXML private TextArea   logArea;

    @FXML private HBox       actionBar;
    @FXML private Button     passButton;
    @FXML private Button     declineButton;

    @FXML private StackPane  boardWaitingPane;
    @FXML private Label      boardWaitingPlayers;
    @FXML private StackPane  eventOverlay;
    @FXML private VBox       eventOverlayBox;
    @FXML private Label      eventOverlayTitle;
    @FXML private Label      eventOverlayName;
    @FXML private Label      eventOverlayDelta;
    // FIX 5: carte evento in grande (fino a 2 affiancate)
    @FXML private HBox       eventCardBox;
    @FXML private javafx.scene.image.ImageView eventCardImageView;
    @FXML private javafx.scene.image.ImageView eventCardImageView2;

    /** Insertion-ordered map by player name so the order is stable across redraws. */
    private final Map<String, PlayerSnapshot> players = new LinkedHashMap<>();
    private BoardSnapshot        lastBoard;
    private Era                  currentEra = Era.ERA_I;
    private String        activePlayer;
    private String        myNickname;
    private boolean       lastIsTopRow = true;
    private List<CardSnapshot> activeChoiceRowCards = List.of();

    /** True while we are awaiting a tile choice (so click handlers know to push). */
    private boolean awaitingTileChoice;

    /** Injected by JavaFxView so the user's selection is pushed back. */
    private BlockingQueue<Integer>   cardChoiceQueue;
    private BlockingQueue<Character> tileChoiceQueue;

    @FXML
    private void initialize() {
        if (sceneRoot == null || root == null) return;

        if (backgroundImageView != null) {
            backgroundImageView.fitWidthProperty().bind(sceneRoot.widthProperty());
            backgroundImageView.fitHeightProperty().bind(sceneRoot.heightProperty());
        }

        NumberBinding scale = Bindings.max(0.1, Bindings.min(
                sceneRoot.widthProperty().divide(DESIGN_WIDTH),
                sceneRoot.heightProperty().divide(DESIGN_HEIGHT)));
        root.setMinSize(DESIGN_WIDTH, DESIGN_HEIGHT);
        root.prefWidthProperty().bind(sceneRoot.widthProperty().divide(scale));
        root.prefHeightProperty().bind(sceneRoot.heightProperty().divide(scale));
        root.maxWidthProperty().bind(root.prefWidthProperty());
        root.maxHeightProperty().bind(root.prefHeightProperty());
        root.scaleXProperty().bind(scale);
        root.scaleYProperty().bind(scale);

        if (actionBar != null) {
            actionBar.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
            actionBar.setPickOnBounds(false);
        }
    }

    public void bindInputQueues(BlockingQueue<Integer> cardQueue,
                                BlockingQueue<Character> tileQueue) {
        this.cardChoiceQueue = cardQueue;
        this.tileChoiceQueue = tileQueue;
    }

    public void onGameStartingHandover(String myNickname) {
        this.myNickname = myNickname;
        runFx(() -> bannerLabel.setText("Connesso come " + myNickname));
    }

    /** Called by JavaFxView.showLobbyUpdate to update the in-board waiting text. */
    public void updateBoardWaiting(java.util.List<String> players, int needed) {
        runFx(() -> {
            if (boardWaitingPane != null) {
                boardWaitingPane.setVisible(true);
                boardWaitingPlayers.setText(
                        String.join("  ·  ", players) + "  (" + players.size() + "/" + needed + ")");
            }
        });
    }

    // ── Push events from the network/view layer ───────────────────────────────

    public void onRoundStarted(int round, List<PlayerSnapshot> turnOrder, BoardSnapshot board) {
        runFx(() -> {
            boardWaitingPane.setVisible(false);
            roundLabel.setText("Round " + round);
            // Refresh the player order from this round's turn order; preserve
            // tribes by replacing whole snapshots.
            players.clear();
            for (PlayerSnapshot p : turnOrder) players.put(p.name(), p);
            this.lastBoard = board;
            this.activePlayer = null;
            this.activeChoiceRowCards = List.of();
            clearDrawPrompt();
            renderAll();
            log("══ ROUND " + round + " ══");
        });
    }

    public void onTotemPlaced(PlayerSnapshot player, char tileLetter, BoardSnapshot board) {
        runFx(() -> {
            players.put(player.name(), player);
            this.lastBoard = board;
            this.activeChoiceRowCards = List.of();
            clearDrawPrompt();
            renderAll();
            log(player.name() + " ha piazzato il totem sulla tessera " + tileLetter);
        });
    }

    public void onCardSlotReady(PlayerSnapshot player, boolean isTopRow,
                                List<CardSnapshot> available,
                                List<CardSnapshot> visibleEvents) {
        runFx(() -> {
            players.put(player.name(), player);
            this.activePlayer = player.name();
            this.lastIsTopRow = isTopRow;
            renderPlayers();
            log("[" + player.name() + "] turno: scegli dalla riga "
                    + (isTopRow ? "superiore" : "inferiore") + ".");

            // Show available + non-acquirable events on the relevant row.
            // Selectability is added later in promptCardChoice for this player.
            List<CardSnapshot> all = new ArrayList<>(available);
            all.addAll(visibleEvents);
            activeChoiceRowCards = List.copyOf(all);
            HBox target = isTopRow ? topRowBox : bottomRowBox;
            renderCards(target, all);
            showDrawPrompt(player.name(), isTopRow, player.name().equals(myNickname));
        });
    }

    public void onCardAcquired(PlayerSnapshot player, CardSnapshot card) {
        runFx(() -> {
            // The PlayerSnapshot now carries the tribe with the new card.
            players.put(player.name(), player);
            renderPlayers();
            log(player.name() + " ha acquisito la carta #" + card.id());
        });
    }

    public void onCardAcquisitionFailed(PlayerSnapshot player, CardSnapshot card, int missingFood) {
        runFx(() -> {
            players.put(player.name(), player);
            log(player.name() + " non può permettersi la carta #" + card.id()
                    + " (mancano " + missingFood + " 🍖)");
        });
    }

    public void onTotemReturnedToTurnOrder(PlayerSnapshot player, int position, BoardSnapshot board) {
        runFx(() -> {
            players.put(player.name(), player);
            this.lastBoard = board;
            this.activeChoiceRowCards = List.of();
            clearDrawPrompt();
            renderAll();
            log(player.name() + " → Ordine di turno #" + position);

            // Annuncio visivo quando tutti i totem hanno scelto la posizione.
            boolean allPlaced = board.turnOrderTile() != null &&
                    board.turnOrderTile().positions().stream()
                            .allMatch(p -> p.placedTotem() != null);
            if (allPlaced) {
                flashBanner("Tutti i giocatori hanno scelto l'ordine di turno!");
            }
        });
    }

    public void onPostRoundStarted(List<PlayerSnapshot> watchtowerPlayers,
                                   List<CardSnapshot> available) {
        runFx(() -> {
            for (PlayerSnapshot p : watchtowerPlayers) players.put(p.name(), p);
            String names = watchtowerPlayers.stream()
                    .map(PlayerSnapshot::name)
                    .reduce((a, b) -> a + ", " + b).orElse("nessuno");
            List<CardSnapshot> displayed = boardRowCards(true);
            if (displayed.isEmpty()) displayed = available;
            log("[POST-ROUND] Torre di Guardia: " + names);
            renderPlayers();
            activeChoiceRowCards = List.copyOf(displayed);
            renderCards(topRowBox, displayed);
        });
    }

    private record PendingEvent(int eventId, Era era, EventType type,
                                String label, String localDeltaText) {}

    // Coda overlay: gli eventi arrivano spesso in rapida successione, ma vanno
    // mostrati uno alla volta per evitare banner con tipo/immagine sbagliati.
    private final java.util.concurrent.BlockingQueue<PendingEvent> pendingEvents
            = new java.util.concurrent.LinkedBlockingQueue<>();
    private final List<Runnable> afterEventOverlayActions = new ArrayList<>();
    private boolean eventOverlayRunning = false;

    public void onEventResolved(int eventId, Era era, EventType type) {
        onEventResolved(eventId, era, type, List.of());
    }

    public void onEventResolved(int eventId, Era era, EventType type,
                                List<PlayerResourceDelta> deltas) {
        runFx(() -> {
            String label = eventLabel(type);
            String localDeltaText = localDeltaText(deltas);
            applyResourceDeltas(deltas);
            log("▶ EVENTO: " + label + " (" + era + ", carta #" + eventId + ")");
            if (deltas == null || deltas.isEmpty()) {
                log("  Nessuna variazione di cibo o punti prestigio.");
            } else {
                for (PlayerResourceDelta d : deltas) {
                    log("  " + d.playerName()
                            + " | cibo " + formatSigned(d.foodDelta())
                            + " (" + d.foodBefore() + " → " + d.foodAfter() + ")"
                            + " | PP " + formatSigned(d.prestigeDelta())
                            + " (" + d.prestigeBefore() + " → " + d.prestigeAfter() + ")");
                }
            }
            renderPlayers();
            pendingEvents.offer(new PendingEvent(eventId, era, type, label, localDeltaText));
            if (!eventOverlayRunning) showNextEventOverlay();
        });
    }

    private void showNextEventOverlay() {
        PendingEvent event = pendingEvents.poll();
        if (event == null) {
            eventOverlayRunning = false;
            drainAfterEventOverlayActions();
            return;
        }
        eventOverlayRunning = true;
        showEventOverlay(event.era(), event.type(), event.label(),
                event.eventId(), event.localDeltaText());
    }

    public void runAfterEventOverlays(Runnable action) {
        if (action == null) return;
        runFx(() -> {
            if (!eventOverlayRunning && pendingEvents.isEmpty()) {
                action.run();
            } else {
                afterEventOverlayActions.add(action);
            }
        });
    }

    private void drainAfterEventOverlayActions() {
        if (afterEventOverlayActions.isEmpty()) return;
        List<Runnable> actions = new ArrayList<>(afterEventOverlayActions);
        afterEventOverlayActions.clear();
        actions.forEach(Runnable::run);
    }

    private void showEventOverlay(Era era, EventType type, String label,
                                  int eventId, String localDeltaText) {
        // Stile testo per tipo
        eventOverlayName.getStyleClass().removeIf(s -> s.startsWith("event-"));
        String eventClass = switch (type) {
            case SUSTENANCE      -> "event-sustenance";
            case SHAMANIC_RITUAL -> "event-shamanic";
            default              -> "event-default";
        };
        eventOverlayName.getStyleClass().add(eventClass);
        eventOverlayTitle.setText("Evento — " + era);
        eventOverlayName.setText(label);
        if (eventOverlayDelta != null) {
            eventOverlayDelta.setText(localDeltaText == null ? "" : localDeltaText);
            boolean showDelta = localDeltaText != null && !localDeltaText.isBlank();
            eventOverlayDelta.setVisible(showDelta);
            eventOverlayDelta.setManaged(showDelta);
        }

        boolean hasImg1 = loadEventCard(eventCardImageView, eventId);
        if (eventCardImageView2 != null) {
            eventCardImageView2.setVisible(false);
            eventCardImageView2.setManaged(false);
        }

        if (eventCardBox != null) {
            boolean show = hasImg1;
            eventCardBox.setVisible(show);
            eventCardBox.setManaged(show);
        }

        eventOverlay.setOpacity(0);
        eventOverlay.setVisible(true);

        FadeTransition fadeIn = new FadeTransition(Duration.millis(400), eventOverlay);
        fadeIn.setFromValue(0);
        fadeIn.setToValue(1);
        PauseTransition hold = new PauseTransition(Duration.millis(3500));
        FadeTransition fadeOut = new FadeTransition(Duration.millis(600), eventOverlay);
        fadeOut.setFromValue(1);
        fadeOut.setToValue(0);
        fadeOut.setOnFinished(e -> {
            eventOverlay.setVisible(false);
            showNextEventOverlay();
        });
        new SequentialTransition(fadeIn, hold, fadeOut).play();
    }

    private String localDeltaText(List<PlayerResourceDelta> deltas) {
        if (myNickname == null || myNickname.isBlank()) return "";
        if (deltas != null) {
            for (PlayerResourceDelta d : deltas) {
                if (myNickname.equals(d.playerName())) {
                    return "Tu  |  cibo " + formatSigned(d.foodDelta())
                            + " (" + d.foodBefore() + " → " + d.foodAfter() + ")"
                            + "   ·   PP " + formatSigned(d.prestigeDelta())
                            + " (" + d.prestigeBefore() + " → " + d.prestigeAfter() + ")";
                }
            }
        }
        PlayerSnapshot me = players.get(myNickname);
        if (me == null) return "Tu  |  cibo +0   ·   PP +0";
        return "Tu  |  cibo +0 (" + me.food() + " → " + me.food() + ")"
                + "   ·   PP +0 (" + me.prestigePoints() + " → "
                + me.prestigePoints() + ")";
    }

    private void applyResourceDeltas(List<PlayerResourceDelta> deltas) {
        if (deltas == null) return;
        for (PlayerResourceDelta d : deltas) {
            PlayerSnapshot old = players.get(d.playerName());
            if (old == null) continue;
            players.put(d.playerName(), new PlayerSnapshot(
                    old.name(),
                    old.totemColour(),
                    d.foodAfter(),
                    d.prestigeAfter(),
                    old.turnOrder(),
                    old.tribeCharacters(),
                    old.tribeBuildings()));
        }
    }

    private static String eventLabel(EventType type) {
        return switch (type) {
            case SUSTENANCE      -> "Sostentamento";
            case HUNT            -> "Caccia";
            case SHAMANIC_RITUAL -> "Rito Sciamanico";
            case CAVE_PAINTINGS  -> "Pitture Rupestri";
        };
    }

    private static String formatSigned(int value) {
        return value > 0 ? "+" + value : String.valueOf(value);
    }

    private boolean loadEventCard(javafx.scene.image.ImageView iv, int cardId) {
        if (iv == null) return false;
        javafx.scene.image.Image img = CardImageCache.imageFor(cardId);
        if (img != null) {
            iv.setImage(img);
            iv.setSmooth(true);
            iv.setPreserveRatio(true);
            iv.setVisible(true);
            iv.setManaged(true);
            return true;
        }
        iv.setVisible(false);
        iv.setManaged(false);
        return false;
    }

    public void onEraChanged(Era era) {
        this.currentEra = era;
        runFx(() -> {
            eraLabel.setText("Era " + era);
            log("★ Inizia l'" + era + " ★");
            flashBanner("Inizia l'" + era);
        });
    }

    public void onRoundEnded(int round) {
        runFx(() -> log("── Fine round " + round + " ──"));
    }

    public void onInvalidAction(String playerName, String reason) {
        runFx(() -> {
            log("[!] " + playerName + ": " + reason);
            flashBanner("⚠ " + reason);
        });
    }

    /**
     * Called when another player disconnects mid-game.
     * Shows a non-blocking banner and logs the event; the game continues.
     */
    public void onPlayerDisconnected(String nickname) {
        runFx(() -> {
            log("⚡ " + nickname + " si è disconnesso. Il gioco continua.");
            flashBanner("⚡ " + nickname + " disconnesso — la partita prosegue");
        });
    }

    /**
     * Called when a previously disconnected player reconnects mid-game.
     */
    public void onPlayerReconnected(String nickname) {
        runFx(() -> {
            log("✓ " + nickname + " si è riconnesso.");
            flashBanner("✓ " + nickname + " è tornato in gioco");
        });
    }

    // ── Input prompts (called from JavaFxView; they push to queues) ───────────

    /**
     * Asks the user to pick a tile. The whole board is shown; clicks on
     * occupied tiles produce an inline error rather than being silently
     * ignored — making the validation visible to the user.
     */
    public void promptOfferTileChoice(PlayerSnapshot player, BoardSnapshot board) {
        runFx(() -> {
            players.put(player.name(), player);
            this.lastBoard = board;
            this.activePlayer = player.name();
            this.awaitingTileChoice = true;
            bannerLabel.setText("[" + player.name() + "] scegli una tessera.");
            renderAll();
        });
    }

    public void promptCardChoice(PlayerSnapshot player,
                                 List<CardSnapshot> available, boolean showPass) {
        runFx(() -> {
            players.put(player.name(), player);
            this.activePlayer = player.name();
            renderPlayers();
            bannerLabel.setText("[" + player.name() + "] scegli una carta"
                    + (showPass ? " o passa." : "."));
            showDrawPrompt(player.name(), lastIsTopRow, player.name().equals(myNickname));

            HBox target = lastIsTopRow ? topRowBox : bottomRowBox;
            renderCardsForChoice(target, cardsForCurrentPrompt(available, lastIsTopRow),
                    available, this::pushCardChoice);

            setActionButton(passButton,    showPass, () -> pushCardChoice(-1));
            setActionButton(declineButton, false, null);
        });
    }

    public void promptPostRoundCardChoice(PlayerSnapshot player, List<CardSnapshot> available) {
        runFx(() -> {
            players.put(player.name(), player);
            this.activePlayer = player.name();
            renderPlayers();
            bannerLabel.setText("[Torre di Guardia] " + player.name() + ": scegli o rifiuta.");
            showDrawPrompt(player.name(), true, player.name().equals(myNickname));
            renderCardsForChoice(topRowBox, cardsForCurrentPrompt(available, true),
                    available, this::pushCardChoice);

            setActionButton(declineButton, true, () -> pushCardChoice(-1));
            setActionButton(passButton,    false, null);
        });
    }

    // ── Action buttons ────────────────────────────────────────────────────────

    @FXML private javafx.scene.layout.VBox logPanel;
    @FXML private javafx.scene.control.Button logToggleBtn;

    @FXML private void onPass()    { pushCardChoice(-1); }
    @FXML private void onDecline() { pushCardChoice(-1); }

    @FXML
    private void onToggleLog() {
        boolean nowVisible = !logPanel.isVisible();
        logPanel.setVisible(nowVisible);
        logPanel.setManaged(nowVisible);
        logToggleBtn.setText(nowVisible ? "✖" : "📋");
    }

    private void setActionButton(Button btn, boolean visible, Runnable action) {
        btn.setVisible(visible);
        btn.setManaged(visible);
        btn.setOnAction(visible && action != null ? e -> action.run() : null);
        updateActionBarVisibility();
    }

    private void updateActionBarVisibility() {
        if (actionBar == null) return;
        boolean visible = (passButton != null && passButton.isVisible())
                || (declineButton != null && declineButton.isVisible());
        actionBar.setVisible(visible);
        actionBar.setManaged(visible);
    }

    // ── Push to queues ────────────────────────────────────────────────────────

    private void pushCardChoice(int id) {
        if (cardChoiceQueue != null) cardChoiceQueue.offer(id);
        bannerLabel.setText("");
        clearDrawPrompt();
        clearCardSelectability();
        passButton.setVisible(false);    passButton.setManaged(false);
        declineButton.setVisible(false); declineButton.setManaged(false);
        updateActionBarVisibility();
    }

    private void pushTileChoice(char letter) {
        if (tileChoiceQueue != null) tileChoiceQueue.offer(letter);
        this.awaitingTileChoice = false;
        bannerLabel.setText("");
    }

    private void showDrawPrompt(String playerName, boolean topRow, boolean mine) {
        String row = topRow ? "riga superiore" : "riga inferiore";
        if (drawPromptLabel != null) {
            drawPromptLabel.setText(mine
                    ? "Tocca a te: " + row
                    : playerName + " pesca dalla " + row);
            drawPromptLabel.setVisible(true);
            drawPromptLabel.setManaged(true);
        }
        updateRowTitle(topRowTitle, "Riga superiore", mine && topRow);
        updateRowTitle(bottomRowTitle, "Riga inferiore", mine && !topRow);
    }

    private void clearDrawPrompt() {
        if (drawPromptLabel != null) {
            drawPromptLabel.setText("");
            drawPromptLabel.setVisible(false);
            drawPromptLabel.setManaged(false);
        }
        updateRowTitle(topRowTitle, "Riga superiore", false);
        updateRowTitle(bottomRowTitle, "Riga inferiore", false);
    }

    private void updateRowTitle(Label label, String baseText, boolean active) {
        if (label == null) return;
        label.setText(active ? baseText + " • pesca qui" : baseText);
        label.getStyleClass().remove("row-title-active");
        if (active) label.getStyleClass().add("row-title-active");
    }

    /**
     * Click handler used by the offer track. If the chosen tile is free, the
     * letter is pushed to the queue; otherwise the user gets a banner message
     * and the click is ignored.
     */
    private void onTileClicked(char letter) {
        if (!awaitingTileChoice) return;
        if (lastBoard == null) return;
        OfferTileSnapshot tile = lastBoard.offerTiles().stream()
                .filter(t -> t.letter() == letter)
                .findFirst().orElse(null);
        if (tile == null) return;
        if (!tile.isFree()) {
            flashBanner("Tessera " + letter + " già occupata — scegli una libera.");
            return;
        }
        pushTileChoice(letter);
    }

    // ── Rendering ─────────────────────────────────────────────────────────────

    /** Repaints everything driven by {@link #lastBoard} + {@link #players}. */
    private void renderAll() {
        renderOfferTrack();
        renderBoardRows();
        renderPlayers();
    }

    private void renderOfferTrack() {
        offerTrackBox.getChildren().clear();
        turnOrderBox.getChildren().clear();
        deckBox.getChildren().clear();
        if (lastBoard == null) return;

        // Turn Order Tile — always visible to the LEFT of the offer track.
        turnOrderBox.getChildren().add(
                CardCellFactory.buildTurnOrderTile(lastBoard.turnOrderTile()));

        // Offer Track — every tile is rendered, occupied ones get a totem
        // overlay. Click handler is wired only while we're awaiting a choice.
        Consumer<Character> onClick = awaitingTileChoice ? this::onTileClicked : null;
        for (OfferTileSnapshot t : lastBoard.offerTiles()) {
            offerTrackBox.getChildren().add(CardCellFactory.buildTile(t, onClick));
        }

        // Deck — pile of card backs with remaining count badge.
        deckBox.getChildren().add(CardCellFactory.buildDeck(lastBoard.tribeDeckSize(), currentEra));
    }

    /**
     * Repaints the two card rows from the current board snapshot. Cards are
     * shared across all non-food tiles so we read from the first one that
     * carries them.
     */
    private void renderBoardRows() {
        if (lastBoard == null) return;
        OfferTileSnapshot ref = lastBoard.offerTiles().stream()
                .filter(t -> !t.isFoodOnly() && !t.topCards().isEmpty())
                .findFirst().orElse(null);
        if (ref == null) return;
        renderCards(topRowBox,    ref.topCards());
        renderCards(bottomRowBox, ref.bottomCards());
    }

    private void renderCards(HBox target, List<CardSnapshot> cards) {
        target.getChildren().clear();
        for (CardSnapshot c : cards) {
            target.getChildren().add(CardCellFactory.buildCard(c, null, false));
        }
    }

    private void renderCardsForChoice(HBox target, List<CardSnapshot> displayedCards,
                                      List<CardSnapshot> selectableCards,
                                      Consumer<Integer> onClick) {
        Set<Integer> selectableIds = new HashSet<>();
        for (CardSnapshot c : selectableCards) {
            if (!c.isEvent()) selectableIds.add(c.id());
        }

        target.getChildren().clear();
        for (CardSnapshot c : displayedCards) {
            boolean selectable = selectableIds.contains(c.id());
            Consumer<Integer> handler = selectable ? onClick : null;
            StackPane cell = CardCellFactory.buildCard(c, handler, false);
            if (!selectable) cell.getStyleClass().add("disabled");
            target.getChildren().add(cell);
        }
    }

    private List<CardSnapshot> cardsForCurrentPrompt(List<CardSnapshot> available,
                                                     boolean topRow) {
        if (!activeChoiceRowCards.isEmpty()) return activeChoiceRowCards;
        List<CardSnapshot> fromBoard = boardRowCards(topRow);
        return fromBoard.isEmpty() ? available : fromBoard;
    }

    private List<CardSnapshot> boardRowCards(boolean topRow) {
        if (lastBoard == null) return List.of();
        return lastBoard.offerTiles().stream()
                .filter(t -> !t.isFoodOnly())
                .map(t -> topRow ? t.topCards() : t.bottomCards())
                .filter(cards -> !cards.isEmpty())
                .findFirst()
                .orElse(List.of());
    }

    private void clearCardSelectability() {
        for (Node node : topRowBox.getChildren()) {
            node.getStyleClass().remove("selectable");
            node.getStyleClass().remove("disabled");
            node.setOnMouseClicked(null);
        }
        for (Node node : bottomRowBox.getChildren()) {
            node.getStyleClass().remove("selectable");
            node.getStyleClass().remove("disabled");
            node.setOnMouseClicked(null);
        }
    }

    private void renderPlayers() {
        playersBox.getChildren().clear();
        for (PlayerSnapshot p : players.values()) {
            boolean active = p.name().equals(activePlayer);
            boolean isMe   = p.name().equals(myNickname);
            Consumer<PlayerSnapshot> onView = isMe ? null : this::showTribeOverlay;
            playersBox.getChildren().add(
                    CardCellFactory.buildPlayerPanel(p, active, isMe, onView));
        }
    }

    private void showTribeOverlay(PlayerSnapshot p) {
        javafx.stage.Stage popup = new javafx.stage.Stage();
        popup.initModality(javafx.stage.Modality.APPLICATION_MODAL);
        popup.setTitle("Tribù di " + p.name());

        VBox root = new VBox(14);
        root.getStyleClass().add("tribe-popup-root");
        root.setPadding(new javafx.geometry.Insets(20));

        // Header
        HBox header = new HBox(12);
        header.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        header.getChildren().add(
                it.polimi.it.view.gui.widgets.CardCellFactory.buildTotemBadge(p.totemColour(), 32));
        Label title = new Label(p.name());
        title.getStyleClass().add("tribe-popup-title");
        Label stats = new Label("  \uD83C\uDF3F " + p.food() + "  \u2605 " + p.prestigePoints() + " PP");
        stats.getStyleClass().addAll("tribe-popup-section", "emoji-text");
        header.getChildren().addAll(title, stats);
        root.getChildren().add(header);

        boolean empty = true;

        // Personaggi — tutte le carte in un WrapPane, niente scroll
        if (!p.tribeCharacters().isEmpty()) {
            empty = false;
            Label lbl = new Label("Personaggi");
            lbl.getStyleClass().add("tribe-popup-section");
            root.getChildren().add(lbl);
            root.getChildren().add(buildCardRow(p.tribeCharacters()));
        }

        // Edifici
        if (!p.tribeBuildings().isEmpty()) {
            empty = false;
            Label lbl = new Label("Edifici");
            lbl.getStyleClass().add("tribe-popup-section");
            root.getChildren().add(lbl);
            root.getChildren().add(buildCardRow(p.tribeBuildings()));
        }

        if (empty) {
            Label e = new Label("Tribù vuota");
            e.getStyleClass().add("tribe-popup-empty");
            root.getChildren().add(e);
        }

        javafx.scene.control.Button closeBtn = new javafx.scene.control.Button("Chiudi");
        closeBtn.getStyleClass().add("gameover-button");
        closeBtn.setOnAction(e -> popup.close());
        VBox.setMargin(closeBtn, new javafx.geometry.Insets(6, 0, 0, 0));
        root.getChildren().add(closeBtn);

        javafx.scene.Scene scene = new javafx.scene.Scene(root);
        scene.getStylesheets().add(
                getClass().getResource("/it/polimi/it/view/gui/styles.css").toExternalForm());
        popup.setScene(scene);
        popup.sizeToScene();
        popup.setResizable(true);
        popup.show();
    }

    /** Builds a horizontal row of cards with no scroll — all always visible. */
    private static HBox buildCardRow(java.util.List<it.polimi.it.observer.CardSnapshot> cards) {
        HBox row = new HBox(8);
        row.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        for (var card : cards) {
            javafx.scene.layout.StackPane cell =
                    it.polimi.it.view.gui.widgets.CardCellFactory.buildCard(card, null, 110, 154);
            // Rimuovi qualsiasi effetto che oscura la carta
            cell.setEffect(null);
            cell.setOpacity(1.0);
            row.getChildren().add(cell);
        }
        return row;
    }

    // ── Utils ─────────────────────────────────────────────────────────────────

    private void log(String msg) { logArea.appendText(msg + "\n"); }

    /**
     * Shows a transient message in the banner; the previous banner content is
     * restored after 2.5s. Used for non-blocking feedback (era change, invalid
     * tile click, …).
     */

    private void flashBanner(String text) {
        String previous = bannerLabel.getText();
        bannerLabel.setText(text);
        PauseTransition pause = new PauseTransition(Duration.millis(2500));
        pause.setOnFinished(e -> {
            // Don't override a banner the user has changed in the meantime.
            if (text.equals(bannerLabel.getText())) bannerLabel.setText(previous);
        });
        pause.play();
    }

    /**
     * Runs {@code r} on the JavaFX Application Thread. Safe to call from any
     * thread; if already on the FX thread, {@code r} runs immediately.
     */
    private static void runFx(Runnable r) {
        if (Platform.isFxApplicationThread()) r.run();
        else                                  Platform.runLater(r);
    }

    public BorderPane getRoot() { return root; }
}
