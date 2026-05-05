package it.polimi.it.network.client;

import it.polimi.it.view.GameView;
import it.polimi.it.view.gui.JavaFxApp;
import it.polimi.it.view.tui.StdinReader;
import it.polimi.it.view.tui.TuiView;
import javafx.application.Application;

import java.io.IOException;
import java.rmi.RemoteException;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

/**
 * Punto di ingresso del client.
 *
 * <p>Uso: {@code java -jar mesos-client.jar [host] [socketPort]}
 * (default: host=localhost, socketPort=12345; la porta RMI è socketPort+1).
 *
 * <p>L'app chiede da stdin se usare TUI o GUI:
 * <ul>
 *   <li>TUI → continua il flusso classico su console e crea una {@link TuiView};</li>
 *   <li>GUI → cede il controllo a {@link JavaFxApp}, che si occupa di tutto
 *       (lobby + connessione + board) sul JavaFX runtime. In questo ramo
 *       gli altri input da stdin (host/porta/nickname) vengono saltati:
 *       li raccoglie la lobby grafica.</li>
 * </ul>
 */
public class ClientApp {

    private static final Logger LOG = Logger.getLogger(ClientApp.class.getName());

    public static void main(String[] args) {
        StdinReader stdin = StdinReader.getInstance();

        System.out.println("=== MESOS ===");
        System.out.println("Scegli interfaccia:  1) TUI   2) GUI");
        System.out.print("> ");
        boolean useGui = readLine(stdin).equals("2");

        if (useGui) {
            // Cediamo il controllo a JavaFX: la lobby raccoglie host/porta/
            // protocollo/nickname/numero giocatori e gestisce la connessione.
            // Application.launch è bloccante e termina solo quando la finestra
            // viene chiusa (o Platform.exit() è invocato).
            Application.launch(JavaFxApp.class, args);
            return;
        }

        // ── Ramo TUI (flusso classico) ────────────────────────────────────────
        runTui(stdin, args);
    }

    private static void runTui(StdinReader stdin, String[] args) {
        String host       = args.length >= 1 ? args[0] : "localhost";
        int    socketPort = 12345;
        if (args.length >= 2) {
            try { socketPort = Integer.parseInt(args[1]); }
            catch (NumberFormatException e) { LOG.warning("Porta non valida, uso 12345"); }
        }
        int rmiPort = socketPort + 1;

        // ── Trasporto ─────────────────────────────────────────────────────────
        System.out.println("Scegli connessione:  1) Socket (TCP)   2) RMI");
        System.out.print("> ");
        String netChoice = readLine(stdin).toLowerCase();
        boolean useRmi = netChoice.equals("2") || netChoice.equals("rmi");

        // ── Nickname ──────────────────────────────────────────────────────────
        System.out.print("Il tuo nickname: ");
        String nickname = readLine(stdin).trim();
        while (nickname.isBlank()) {
            System.out.print("Il nickname non può essere vuoto. Riprova: ");
            nickname = readLine(stdin).trim();
        }

        // ── Numero giocatori ──────────────────────────────────────────────────
        System.out.print("Numero di giocatori (2-5): ");
        int numPlayers = 2;
        try { numPlayers = Integer.parseInt(readLine(stdin).trim()); }
        catch (NumberFormatException ignored) {}
        numPlayers = Math.max(2, Math.min(5, numPlayers));

        // ── View ──────────────────────────────────────────────────────────────
        GameView view = new TuiView();

        // ── Proxy ─────────────────────────────────────────────────────────────
        ServerProxy proxy;
        if (useRmi) {
            try {
                proxy = new RmiServerProxy(host, rmiPort);
                System.out.println("Connessione RMI sulla porta " + rmiPort);
            } catch (RemoteException e) {
                System.err.println("Impossibile creare proxy RMI: " + e.getMessage());
                System.exit(1);
                return;
            }
        } else {
            proxy = new SocketServerProxy(host, socketPort);
            System.out.println("Connessione Socket sulla porta " + socketPort);
        }

        // ── Controller + connessione ──────────────────────────────────────────
        ClientController controller = new ClientController(proxy, view, nickname);
        proxy.setOnMessage(controller::onMessage);
        proxy.setOnDisconnect(controller::onDisconnect);

        int connectPort = useRmi ? rmiPort : socketPort;
        try {
            proxy.connect(nickname, numPlayers);
            System.out.println("Connesso a " + host + ":" + connectPort
                    + " come '" + nickname + "'. In attesa degli altri giocatori...");
        } catch (IOException e) {
            System.err.println("Impossibile connettersi a " + host + ":" + connectPort
                    + " — " + e.getMessage());
            System.exit(1);
        }

        controller.run();
        proxy.disconnect();
        System.out.println("Arrivederci!");
    }

    /** Legge una riga da StdinReader con retry fino a quando arriva qualcosa. */
    private static String readLine(StdinReader stdin) {
        while (true) {
            try {
                String line = stdin.readLine(10, TimeUnit.SECONDS);
                if (line != null) return line.trim();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return "";
            }
        }
    }
}
