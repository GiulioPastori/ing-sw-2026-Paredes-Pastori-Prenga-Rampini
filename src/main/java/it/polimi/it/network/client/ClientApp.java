package it.polimi.it.network.client;

import it.polimi.it.view.GameView;
import it.polimi.it.view.gui.GuiView;
import it.polimi.it.view.tui.StdinReader;
import it.polimi.it.view.tui.TuiView;

import java.io.IOException;
import java.rmi.RemoteException;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

/**
 * Punto di ingresso del client.
 *
 * Uso: java -jar mesos-client.jar [host] [socketPort]
 * Default: host=localhost, socketPort=12345 (RMI usa socketPort+1=12346)
 */
public class ClientApp {

    private static final Logger LOG = Logger.getLogger(ClientApp.class.getName());

    public static void main(String[] args) {
        String host       = args.length >= 1 ? args[0] : "localhost";
        int    socketPort = 12345;
        if (args.length >= 2) {
            try { socketPort = Integer.parseInt(args[1]); }
            catch (NumberFormatException e) { LOG.warning("Porta non valida, uso 12345"); }
        }
        int rmiPort = socketPort + 1;

        // StdinReader è un singleton: avvia il thread di lettura stdin una sola volta.
        // Viene usato sia qui per il setup che dalla TuiView durante la partita.
        StdinReader stdin = StdinReader.getInstance();

        System.out.println("=== MESOS ===");

        // ── 1. Interfaccia ────────────────────────────────────────────────────
        System.out.println("Scegli interfaccia:  1) TUI   2) GUI");
        System.out.print("> ");
        boolean useGui = readLine(stdin).equals("2");

        // ── 2. Trasporto ──────────────────────────────────────────────────────
        System.out.println("Scegli connessione:  1) Socket (TCP)   2) RMI");
        System.out.print("> ");
        String netChoice = readLine(stdin).toLowerCase();
        boolean useRmi = netChoice.equals("2") || netChoice.equals("rmi");

        // ── 3. Nickname ───────────────────────────────────────────────────────
        System.out.print("Il tuo nickname: ");
        String nickname = readLine(stdin).trim();
        while (nickname.isBlank()) {
            System.out.print("Il nickname non può essere vuoto. Riprova: ");
            nickname = readLine(stdin).trim();
        }

        // ── 4. Numero giocatori ───────────────────────────────────────────────
        System.out.print("Numero di giocatori (2-5): ");
        int numPlayers = 2;
        try { numPlayers = Integer.parseInt(readLine(stdin).trim()); }
        catch (NumberFormatException ignored) {}
        numPlayers = Math.max(2, Math.min(5, numPlayers));

        // ── 5. View ───────────────────────────────────────────────────────────
        GameView view = useGui ? new GuiView() : new TuiView();

        // ── 6. Proxy ──────────────────────────────────────────────────────────
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

        // ── 7. Controller ─────────────────────────────────────────────────────
        ClientController controller = new ClientController(proxy, view, nickname);
        proxy.setOnMessage(controller::onMessage);
        proxy.setOnDisconnect(controller::onDisconnect);

        // ── 8. Connetti ───────────────────────────────────────────────────────
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

        // ── 9. Avvia ──────────────────────────────────────────────────────────
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