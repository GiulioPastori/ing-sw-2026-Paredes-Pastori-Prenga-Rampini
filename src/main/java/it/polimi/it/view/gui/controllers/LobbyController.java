package it.polimi.it.view.gui.controllers;

import javafx.application.Platform;
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
 * Controller della schermata di lobby/login.
 *
 * <p>Raccoglie i parametri di connessione (host, porta, protocollo,
 * nickname, numero giocatori). Quando l'utente preme "Connetti" invoca il
 * {@link #onConnectRequest} fornito dalla {@code JavaFxView}, che si occupa
 * della logica di rete vera e propria.
 *
 * <p>Lo stato "connecting" è esposto via {@link #showConnecting} /
 * {@link #showError}: è la {@code JavaFxView} a gestire la transizione di
 * scena al tabellone una volta che la connessione è stabilita.
 */
public class LobbyController {

    @FXML private VBox              root;
    @FXML private TextField         hostField;
    @FXML private TextField         portField;
    @FXML private TextField         nicknameField;
    @FXML private Spinner<Integer>  playerCountSpinner;
    @FXML private RadioButton       socketRadio;
    @FXML private RadioButton       rmiRadio;
    @FXML private ToggleGroup       netGroup;
    @FXML private Button            connectButton;
    @FXML private ProgressIndicator spinner;
    @FXML private Label             statusLabel;

    private Consumer<ConnectionParams> onConnectRequest;

    /**
     * Parametri raccolti dal form e passati alla {@code JavaFxView} per la
     * connessione vera e propria.
     */
    public record ConnectionParams(
            String  host,
            int     port,
            boolean useRmi,
            String  nickname,
            int     numPlayers) {}

    @FXML
    private void initialize() {
        playerCountSpinner.setValueFactory(
                new SpinnerValueFactory.IntegerSpinnerValueFactory(2, 5, 2));
    }

    /** Imposta il consumer chiamato al click di "Connetti". */
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
        onConnectRequest.accept(new ConnectionParams(host, port, useRmi, nickname, numPlayers));
    }

    public void showConnecting(String msg) {
        runFx(() -> {
            connectButton.setDisable(true);
            spinner.setVisible(true);
            statusLabel.getStyleClass().removeAll("error", "ok");
            statusLabel.setText(msg);
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

    public VBox getRoot() { return root; }

    private static void runFx(Runnable r) {
        if (Platform.isFxApplicationThread()) r.run();
        else Platform.runLater(r);
    }
}
