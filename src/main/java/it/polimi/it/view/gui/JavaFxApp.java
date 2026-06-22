package it.polimi.it.view.gui;

import it.polimi.it.network.client.ClientController;
import it.polimi.it.network.client.RmiServerProxy;
import it.polimi.it.network.client.ServerProxy;
import it.polimi.it.network.client.SocketServerProxy;
import it.polimi.it.view.gui.controllers.LobbyController;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.IOException;
import java.rmi.RemoteException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * JavaFX entry point for the client application.
 *
 * <p>Flow:
 * <ol>
 *   <li>{@link #start(Stage)} shows the lobby FXML.</li>
 *   <li>When the user presses the connect button, {@link LobbyController}
 *       invokes the callback registered here.</li>
 *   <li>The connection attempt runs on a dedicated non-FX thread.</li>
 *   <li>On success, {@link JavaFxView#showBoard(String)} replaces the lobby
 *       scene with the board, then the network thread enters
 *       {@link ClientController#run()} until the game ends.</li>
 * </ol>
 *
 * <p>Optional CLI parameters are passed through
 * {@link Application#getParameters()}; see {@link #main(String[])} and
 * {@link it.polimi.it.network.client.ClientApp} for the launch path.
 */
public class JavaFxApp extends Application {

    private static final Logger LOG = Logger.getLogger(JavaFxApp.class.getName());

    private Stage primaryStage;
    private LobbyController lobbyController;

    @Override
    public void start(Stage stage) throws IOException {
        this.primaryStage = stage;

        FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/it/polimi/it/view/gui/lobby.fxml"));
        javafx.scene.layout.StackPane lobbyRoot = loader.load();
        this.lobbyController = loader.getController();
        lobbyController.setSceneRoot(lobbyRoot);   // root is FXML root node, not @FXML field

        lobbyController.setOnConnectRequest(this::handleConnect);

        Scene scene = new Scene(lobbyRoot);
        scene.getStylesheets().add(
                getClass().getResource("/it/polimi/it/view/gui/styles.css").toExternalForm());
        stage.setTitle("MESOS — Lobby");
        stage.setScene(scene);
        stage.setOnCloseRequest(e -> {
            // Stop the JavaFX runtime when the user closes the window.
            Platform.exit();
            // System.exit also closes network threads that may be blocked on take().
            System.exit(0);
        });
        stage.show();
    }

    /**
     * Lobby callback. Performs the network connection on a dedicated thread so
     * the JavaFX Application Thread is not blocked.
     *
     * <p>On failure, lobby feedback is sent through
     * {@link #reportLobbyError(String)}, which looks up nodes in the still
     * active lobby scene.
     */
    private void handleConnect(LobbyController.ConnectionParams p) {
        Thread netThread = new Thread(() -> doConnect(p), "mesos-client-network");
        netThread.setDaemon(false);
        netThread.start();
    }

    /**
     * Builds the proxy and network controller, shows the board, and blocks
     * until the game ends. Always runs on a non-FX thread.
     */
    private void doConnect(LobbyController.ConnectionParams p) {
        // Project convention: the RMI port is the socket port plus one.
        int connectPort = p.useRmi() ? p.port() + 1 : p.port();

        ServerProxy proxy;
        try {
            proxy = p.useRmi()
                    ? new RmiServerProxy(p.host(), connectPort)
                    : new SocketServerProxy(p.host(), p.port());
        } catch (RemoteException e) {
            reportLobbyError("Impossibile creare il proxy RMI: " + e.getMessage());
            return;
        }

        // Multiple games: fetch open sessions and show the choice dialog.
        String chosenSessionId = null;
        try {
            java.util.List<it.polimi.it.network.message.Message.SessionInfo> sessions =
                    proxy.fetchSessionList(p.numPlayers()).sessionInfoList();
            if (!sessions.isEmpty()) {
                java.util.concurrent.CompletableFuture<String> future =
                        new java.util.concurrent.CompletableFuture<>();
                javafx.application.Platform.runLater(
                        () -> lobbyController.showSessionList(sessions, future));
                try {
                    chosenSessionId = future.get(60, java.util.concurrent.TimeUnit.SECONDS);
                } catch (Exception ignored) { /* timeout, create a new session */ }
            }
        } catch (IOException e) {
            LOG.warning("Session list unavailable: " + e.getMessage());
        }

        // Create the JavaFX view before connecting so ClientController can call
        // it as soon as the first server messages arrive.
        JavaFxView view = new JavaFxView(primaryStage, lobbyController);
        ClientController controller = new ClientController(proxy, view, p.nickname());
        proxy.setOnMessage(controller::onMessage);
        proxy.setOnDisconnect(controller::onDisconnect);

        final String sessionIdToJoin = chosenSessionId;
        try {
            proxy.connectWithSession(p.nickname(), p.numPlayers(), p.totemColour(), sessionIdToJoin);
        } catch (IOException e) {
            try { proxy.disconnect(); } catch (Exception ignored) { /* best effort */ }
            reportLobbyError("Connessione fallita a " + p.host() + ":" + connectPort
                    + " — " + e.getMessage());
            return;
        }

        // Connection succeeded: replace the lobby scene with the board.
        view.showBoard(p.nickname());

        // Block this thread until the game ends. Message dispatching happens
        // on proxy reader threads; run() waits on the controller game latch.
        try {
            controller.run();
        } finally {
            try { proxy.disconnect(); } catch (Exception ignored) { /* best effort */ }
            // When the game ends, GameOverController owns the close button. Do
            // not close automatically because the user needs to see the ranking.
        }
    }

    /**
     * Shows an error message in the lobby. This method must only be invoked
     * while the lobby scene is still active.
     */
    private void reportLobbyError(String msg) {
        Platform.runLater(() -> {
            try {
                // The lobby is still the current scene, so CSS lookup can
                // access the status controls without reloading the controller.
                var scene = primaryStage.getScene();
                if (scene == null) return;
                var statusNode = scene.lookup("#statusLabel");
                if (statusNode instanceof javafx.scene.control.Label lbl) {
                    lbl.getStyleClass().remove("ok");
                    if (!lbl.getStyleClass().contains("error")) lbl.getStyleClass().add("error");
                    lbl.setText("Errore: " + msg);
                }
                var connectBtn = scene.lookup("#connectButton");
                if (connectBtn instanceof javafx.scene.control.Button btn) btn.setDisable(false);
                var spinner = scene.lookup("#spinner");
                if (spinner != null) spinner.setVisible(false);
            } catch (Exception e) {
                LOG.log(Level.WARNING, "Unable to show lobby error", e);
            }
        });
    }

    /**
     * Entry point used when the app is launched directly, for example through
     * {@code mvn javafx:run}. The TUI/GUI selection flow starts from
     * {@link it.polimi.it.network.client.ClientApp}, which delegates here when
     * the user chooses the GUI.
     */
    public static void main(String[] args) {
        Application.launch(JavaFxApp.class, args);
    }
}
