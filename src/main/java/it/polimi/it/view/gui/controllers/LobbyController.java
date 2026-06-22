package it.polimi.it.view.gui.controllers;

import javafx.application.Platform;
import it.polimi.it.enums.TotemColour;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.RadioButton;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TextField;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.VBox;

import java.util.function.Consumer;

/**
 * Controller for the lobby/login screen.
 *
 * <p>Collects connection parameters: host, port, protocol, nickname, and
 * player count. When the user presses the connect button, it invokes the
 * {@link #onConnectRequest} callback provided by {@code JavaFxView}, which
 * handles the network flow.
 *
 * <p>The connecting state is exposed through {@link #showConnecting} and
 * {@link #showError}; {@code JavaFxView} handles the scene transition to the
 * board once the connection is established.
 */
public class LobbyController {

    @FXML private javafx.scene.layout.VBox  formPane;
    @FXML private javafx.scene.layout.StackPane waitingPane;
    @FXML private Label             waitingTitle;
    @FXML private Label             waitingPlayers;
    @FXML private TextField         hostField;
    @FXML private TextField         portField;
    @FXML private TextField         nicknameField;
    @FXML private Spinner<Integer>  playerCountSpinner;
    @FXML private RadioButton       socketRadio;
    @FXML private RadioButton       rmiRadio;
    @FXML private ToggleGroup       netGroup;
    @FXML private Button            connectButton;
    @FXML private ProgressIndicator spinner;
    @FXML private javafx.scene.layout.HBox totemPickerBox;
    @FXML private Label             statusLabel;

    /**
     * Root node of the lobby scene, set by JavaFxApp after loading FXML.
     * It is not injected via @FXML because the root node has no fx:id.
     */
    private javafx.scene.layout.StackPane sceneRoot;

    /** Called by JavaFxApp after FXMLLoader.load() to provide the root node. */
    public void setSceneRoot(javafx.scene.layout.StackPane root) {
        this.sceneRoot = root;
    }

    private Consumer<ConnectionParams> onConnectRequest;
    private TotemColour selectedColour = TotemColour.RED; // default

    /**
     * Parameters collected from the form and passed to {@code JavaFxView} for
     * the actual connection.
     *
     * @param host server host
     * @param port socket port
     * @param useRmi whether to use RMI instead of sockets
     * @param nickname player nickname
     * @param numPlayers requested player count
     * @param totemColour selected totem colour
     */
    public record ConnectionParams(
            String      host,
            int         port,
            boolean     useRmi,
            String      nickname,
            int         numPlayers,
            TotemColour totemColour) {}

    @FXML
    private void initialize() {
        playerCountSpinner.setValueFactory(
                new SpinnerValueFactory.IntegerSpinnerValueFactory(2, 5, 2));
        initTotemPicker();
    }

    private void initTotemPicker() {
        totemPickerBox.getChildren().clear();
        for (TotemColour tc : TotemColour.values()) {
            javafx.scene.shape.Circle circle = new javafx.scene.shape.Circle(22);
            circle.getStyleClass().addAll("totem-badge", totemCssClass(tc));
            circle.setCursor(javafx.scene.Cursor.HAND);

            javafx.scene.layout.StackPane btn = new javafx.scene.layout.StackPane(circle);
            btn.setPrefSize(52, 52);
            btn.setMaxSize(52, 52);
            btn.getStyleClass().add("totem-picker-lobby-btn");
            if (tc == selectedColour) btn.getStyleClass().add("totem-picker-selected");

            btn.setOnMouseClicked(e -> {
                selectedColour = tc;
                // Update selection highlight
                totemPickerBox.getChildren().forEach(n ->
                        n.getStyleClass().remove("totem-picker-selected"));
                btn.getStyleClass().add("totem-picker-selected");
            });

            javafx.scene.control.Tooltip.install(btn,
                    new javafx.scene.control.Tooltip(totemNameIt(tc)));
            totemPickerBox.getChildren().add(btn);
        }
    }

    private static String totemCssClass(TotemColour tc) {
        return switch (tc) {
            case RED    -> "totem-red";
            case BLUE   -> "totem-blue";
            case YELLOW -> "totem-yellow";
            case WHITE  -> "totem-white";
            case BLACK  -> "totem-black";
        };
    }

    private static String totemNameIt(TotemColour tc) {
        return switch (tc) {
            case RED    -> "Rosso";
            case BLUE   -> "Blu";
            case YELLOW -> "Giallo";
            case WHITE  -> "Bianco";
            case BLACK  -> "Nero";
        };
    }

    /** Sets the callback invoked when the connect button is clicked. */
    public void setOnConnectRequest(Consumer<ConnectionParams> handler) {
        this.onConnectRequest = handler;
    }

    @FXML
    private void onConnect() {
        if (onConnectRequest == null) return;

        String host     = hostField.getText().trim();
        String nickname = nicknameField.getText().trim();
        if (host.isEmpty())     { showError("Inserisci un host valido.");        return; }
        if (nickname.isEmpty()) { showError("Inserisci un nickname.");           return; }

        int port;
        try { port = Integer.parseInt(portField.getText().trim()); }
        catch (NumberFormatException e) { showError("Porta non valida."); return; }

        boolean useRmi    = rmiRadio.isSelected();
        int     numPlayers = playerCountSpinner.getValue();

        showConnecting("Connessione in corso a " + host + ":" + port + "…");
        onConnectRequest.accept(new ConnectionParams(host, port, useRmi, nickname, numPlayers, selectedColour));
    }

    public void showConnecting(String msg) {
        runFx(() -> {
            connectButton.setDisable(true);
            spinner.setVisible(true);
            statusLabel.getStyleClass().removeAll("error", "ok");
            statusLabel.setText(msg);
        });
    }

    /** Called when the server confirms the player joined and reports lobby state. */
    public void showLobbyWaiting(String message, java.util.List<String> players, int needed) {
        runFx(() -> {
            formPane.setVisible(false);
            waitingPane.setVisible(true);
            waitingTitle.setText("In attesa degli altri giocatori…");
            waitingPlayers.setText(
                    String.join("  ·  ", players) + "  (" + players.size() + "/" + needed + ")");
        });
    }

    public void showError(String msg) {
        runFx(() -> {
            connectButton.setDisable(false);
            spinner.setVisible(false);
            statusLabel.getStyleClass().remove("ok");
            if (!statusLabel.getStyleClass().contains("error")) {
                statusLabel.getStyleClass().add("error");
            }
            statusLabel.setText("Errore: " + msg);
        });
    }

    public void showOk(String msg) {
        runFx(() -> {
            spinner.setVisible(false);
            statusLabel.getStyleClass().remove("error");
            if (!statusLabel.getStyleClass().contains("ok")) {
                statusLabel.getStyleClass().add("ok");
            }
            statusLabel.setText(msg);
        });
    }

    /**
     * Shows an overlay with the list of open sessions.
     * After the choice, or after selecting a new game, completes
     * {@code future} with the selected session id or {@code null}.
     *
     * <p>Uses {@link #sceneRoot}, set by {@code JavaFxApp}; it should not be
     * null at this point.
     */
    public void showSessionList(
            java.util.List<it.polimi.it.network.message.Message.SessionInfo> sessions,
            java.util.concurrent.CompletableFuture<String> future) {

        runFx(() -> {
            if (sceneRoot == null) { future.complete(null); return; }

            // Center panel.
            javafx.scene.layout.VBox panel = new javafx.scene.layout.VBox(8);
            panel.setStyle(
                    "-fx-background-color: #2a2a2a;" +
                            "-fx-padding: 20;" +
                            "-fx-background-radius: 8;" +
                            "-fx-border-radius: 8;" +
                            "-fx-border-color: #555;" +
                            "-fx-border-width: 1;");
            panel.setMaxWidth(500);

            javafx.scene.control.Label title =
                    new javafx.scene.control.Label("Partite aperte — scegli o crea nuova");
            title.setStyle("-fx-text-fill: #fff; -fx-font-size: 14px; -fx-font-weight: bold;");
            panel.getChildren().add(title);

            // Semi-transparent overlay covering the whole lobby.
            javafx.scene.layout.StackPane overlay = new javafx.scene.layout.StackPane(panel);
            overlay.setStyle("-fx-background-color: rgba(0,0,0,0.65);");

            // One button for each open session.
            for (var s : sessions) {
                String lbl = String.format("[%s]  %d/%d giocatori  —  %s",
                        s.sessionId(), s.currentPlayers(), s.expectedPlayers(),
                        String.join(", ", s.players()));
                javafx.scene.control.Button btn = new javafx.scene.control.Button(lbl);
                btn.setMaxWidth(Double.MAX_VALUE);
                btn.setStyle("-fx-background-color: #3a5a3a; -fx-text-fill: #ddd;" +
                        "-fx-cursor: hand; -fx-font-size: 12px;");
                String sid = s.sessionId();
                btn.setOnAction(e -> {
                    sceneRoot.getChildren().remove(overlay);
                    future.complete(sid);
                });
                panel.getChildren().add(btn);
            }

            // New-game button.
            javafx.scene.control.Button newGame =
                    new javafx.scene.control.Button("+ Crea nuova partita");
            newGame.setMaxWidth(Double.MAX_VALUE);
            newGame.setStyle("-fx-background-color: #1a3a5a; -fx-text-fill: #ddd;" +
                    "-fx-cursor: hand; -fx-font-size: 12px;");
            newGame.setOnAction(e -> {
                sceneRoot.getChildren().remove(overlay);
                // Use the NEW sentinel instead of null so the server always
                // creates a new session instead of reusing an existing one.
                future.complete(it.polimi.it.network.server.LobbyServer.NEW_SESSION);
            });
            panel.getChildren().add(newGame);

            sceneRoot.getChildren().add(overlay);
        });
    }

    public javafx.scene.layout.StackPane getRoot() { return sceneRoot; }

    private static void runFx(Runnable r) {
        if (Platform.isFxApplicationThread()) r.run();
        else Platform.runLater(r);
    }
}
