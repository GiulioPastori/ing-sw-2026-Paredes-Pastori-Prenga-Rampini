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
 * Entry point of the client.
 *
 * <p>Usage: {@code java -jar mesos-client.jar [host] [socketPort]}
 * (default: host=localhost, socketPort=12345; the RMI port is socketPort+1).
 *
 * <p>The app asks via stdin whether to use TUI or GUI:
 * <ul>
 * <li>TUI → continues the classic flow on the console and creates a {@link TuiView};</li>
 * <li>GUI → hands over control to {@link JavaFxApp}, which takes care of everything
 * (lobby + connection + board) on the JavaFX runtime. In this branch
 * the other stdin inputs (host/port/nickname) are skipped:
 * they are collected by the graphical lobby.</li>
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
            // We hand over control to JavaFX: the lobby collects host/port/
            // protocol/nickname/number of players and manages the connection.
            // Application.launch is blocking and only ends when the window
            // is closed (or Platform.exit() is invoked).
            Application.launch(JavaFxApp.class, args);
            return;
        }

        // ── TUI Branch (classic flow) ────────────────────────────────────────
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

        // ── Connection ─────────────────────────────────────────────────────────
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

        // ── Number of players ──────────────────────────────────────────────────
        System.out.print("Numero di giocatori (2-5): ");
        int numPlayers = 2;
        try { numPlayers = Integer.parseInt(readLine(stdin).trim()); }
        catch (NumberFormatException ignored) {}
        numPlayers = Math.max(2, Math.min(5, numPlayers));

        // ── Colour totem ──────────────────────────────────────────────────────────
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

        // ── Controller + connection ──────────────────────────────────────────
        ClientController controller = new ClientController(proxy, view, nickname);
        proxy.setOnMessage(controller::onMessage);
        proxy.setOnDisconnect(controller::onDisconnect);

        int connectPort = useRmi ? rmiPort : socketPort;
        try {
            proxy.connect(nickname, numPlayers, chosenColour);
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

    /** Reads a line from StdinReader with retry until something arrives. */
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