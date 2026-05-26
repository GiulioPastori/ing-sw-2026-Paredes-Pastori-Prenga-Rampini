package it.polimi.it.network.client;

import it.polimi.it.enums.TotemColour;
import it.polimi.it.view.tui.AnsiStyle;
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

        // ── Colore totem ──────────────────────────────────────────────────────────
        TotemColour[] allColours = TotemColour.values();
        System.out.println("Scegli il colore del tuo totem:");
        for (int i = 0; i < allColours.length; i++) {
            System.out.println("  " + (i + 1) + ". " + AnsiStyle.totemNameIt(allColours[i]));
        }
        System.out.print("> ");
        TotemColour chosenColour = allColours[0];
        try {
            int idx = Integer.parseInt(readLine(stdin).trim()) - 1;
            if (idx >= 0 && idx < allColours.length) chosenColour = allColours[idx];
        } catch (NumberFormatException ignored) {}

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

        // ── Partite multiple: mostra sessioni aperte e lascia scegliere ───────
        String chosenSessionId = null;
        try {
            java.util.List<it.polimi.it.network.message.Message.SessionInfo> sessions =
                    proxy.fetchSessionList(numPlayers).sessionInfoList();
            if (!sessions.isEmpty()) {
                System.out.println("\nPartite aperte per " + numPlayers + " giocatori:");
                System.out.println("  0. Crea una nuova partita");
                for (int i = 0; i < sessions.size(); i++) {
                    var s = sessions.get(i);
                    System.out.printf("  %d. [%s] %d/%d — %s%n",
                            i + 1, s.sessionId(), s.currentPlayers(), s.expectedPlayers(),
                            String.join(", ", s.players()));
                }
                System.out.print("Scelta> ");
                try {
                    int choice = Integer.parseInt(readLine(stdin).trim());
                    if (choice > 0 && choice <= sessions.size())
                        chosenSessionId = sessions.get(choice - 1).sessionId();
                    else
                        chosenSessionId = it.polimi.it.network.server.LobbyServer.NEW_SESSION;
                } catch (NumberFormatException ignored) {
                    chosenSessionId = it.polimi.it.network.server.LobbyServer.NEW_SESSION;
                }
                System.out.println(it.polimi.it.network.server.LobbyServer.NEW_SESSION.equals(chosenSessionId)
                        ? "Verrà creata una nuova partita."
                        : "Unione alla sessione " + chosenSessionId);
            } else {
                System.out.println("Nessuna partita aperta — verrà creata una nuova sessione.");
            }
        } catch (IOException e) {
            System.out.println("(Lista sessioni non disponibile: " + e.getMessage() + ")");
        }

        try {
            proxy.connectWithSession(nickname, numPlayers, chosenColour, chosenSessionId);
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