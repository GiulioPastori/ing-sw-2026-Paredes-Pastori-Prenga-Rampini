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
 * Entry point JavaFX dell'applicazione client.
 *
 * <p>Flusso:
 * <ol>
 *   <li>{@link #start(Stage)} mostra la lobby (FXML);</li>
 *   <li>quando l'utente preme "Connetti" il
 *       {@link LobbyController} invoca il consumer registrato qui;</li>
 *   <li>la connessione viene tentata su un thread dedicato (non FX);</li>
 *   <li>al successo, {@link JavaFxView#showBoard(String)} sostituisce la
 *       scena della lobby con la board, e il thread di rete entra in
 *       {@link ClientController#run()} (bloccante fino a fine partita).</li>
 * </ol>
 *
 * <p>I parametri opzionali da CLI (host/port di default) vengono passati
 * tramite {@link Application#getParameters()}; vedi {@link #main(String[])}
 * e {@link it.polimi.it.network.client.ClientApp} per il punto di lancio.
 */
public class JavaFxApp extends Application {

    private static final Logger LOG = Logger.getLogger(JavaFxApp.class.getName());

    private Stage primaryStage;

    @Override
    public void start(Stage stage) throws IOException {
        this.primaryStage = stage;

        FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/it/polimi/it/view/gui/lobby.fxml"));
        VBox lobbyRoot = loader.load();
        LobbyController lobby = loader.getController();

        lobby.setOnConnectRequest(this::handleConnect);

        Scene scene = new Scene(lobbyRoot);
        scene.getStylesheets().add(
                getClass().getResource("/it/polimi/it/view/gui/styles.css").toExternalForm());
        stage.setTitle("MESOS — Lobby");
        stage.setScene(scene);
        stage.setOnCloseRequest(e -> {
            // Quando l'utente chiude la finestra, abbatti il runtime JavaFX
            Platform.exit();
            // System.exit chiude anche eventuali thread di rete bloccati su take()
            System.exit(0);
        });
        stage.show();
    }

    /**
     * Callback dalla lobby. Esegue la connessione di rete su un thread
     * dedicato per non bloccare il JavaFX Application Thread.
     *
     * <p>Il feedback di errore alla lobby (in caso di fallimento) avviene
     * tramite {@link #reportLobbyError(String)}, che cerca i nodi della
     * scena ancora attiva via lookup CSS.
     */
    private void handleConnect(LobbyController.ConnectionParams p) {
        Thread netThread = new Thread(() -> doConnect(p), "mesos-client-network");
        netThread.setDaemon(false);
        netThread.start();
    }

    /**
     * Costruisce il proxy, il controller di rete, mostra la board e blocca
     * fino a fine partita. Gira sempre su un thread non-FX.
     */
    private void doConnect(LobbyController.ConnectionParams p) {
        // Per RMI usiamo la convenzione del progetto: porta_rmi = porta_socket+1.
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

        // Crea la View JavaFX prima di tentare la connessione, così il
        // ClientController può chiamarne i metodi non appena arrivano i primi
        // messaggi del server.
        JavaFxView view = new JavaFxView(primaryStage);
        ClientController controller = new ClientController(proxy, view, p.nickname());
        proxy.setOnMessage(controller::onMessage);
        proxy.setOnDisconnect(controller::onDisconnect);

        try {
            proxy.connect(p.nickname(), p.numPlayers(), p.totemColour());
        } catch (IOException e) {
            try { proxy.disconnect(); } catch (Exception ignored) { /* best effort */ }
            reportLobbyError("Connessione fallita a " + p.host() + ":" + connectPort
                    + " — " + e.getMessage());
            return;
        }

        // Connessione OK: scambia la scena con il tabellone.
        view.showBoard(p.nickname());

        // Blocca questo thread fino al termine della partita. Il dispatching
        // dei messaggi avviene su altri thread (i reader del proxy);
        // run() resta in attesa sul gameLatch del controller.
        try {
            controller.run();
        } finally {
            try { proxy.disconnect(); } catch (Exception ignored) { /* best effort */ }
            // Quando la partita finisce, la GameOverController gestisce il bottone
            // "Chiudi". Non chiudiamo qui automaticamente: l'utente vuole vedere
            // la classifica.
        }
    }

    /**
     * Mostra un messaggio di errore nella lobby. Recupera il
     * {@link LobbyController} dalla scena attiva: questo metodo va invocato
     * solo finché la lobby è ancora a video.
     */
    private void reportLobbyError(String msg) {
        Platform.runLater(() -> {
            try {
                // La lobby è ancora la scena corrente: il suo root è il VBox
                // gestito dal LobbyController. Ricarichiamo il controller via
                // userData impostata da setOnConnectRequest? In modo più
                // pulito: usiamo lookup sul campo statusLabel.
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
                LOG.log(Level.WARNING, "Impossibile mostrare errore nella lobby", e);
            }
        });
    }

    /**
     * Punto di ingresso quando l'app è lanciata direttamente (es. via
     * {@code mvn javafx:run}). Per l'integrazione col flusso TUI/GUI esistente
     * vedi {@link it.polimi.it.network.client.ClientApp}, che instrada l'utente
     * verso questa Application a seconda della scelta.
     */
    public static void main(String[] args) {
        Application.launch(JavaFxApp.class, args);
    }
}