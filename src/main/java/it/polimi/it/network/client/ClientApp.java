package it.polimi.it.network.client;

import it.polimi.it.view.GameView;
import it.polimi.it.view.gui.GuiView;

import java.io.IOException;
import java.rmi.RemoteException;
import java.util.Scanner;
import java.util.logging.Logger;

/**
 * Client entry point.
 *
 * Startup flow:
 * 1. Read server host and port from command-line args (defaults: localhost:12345)
 * 2. Ask the player to choose TUI or GUI
 * 3. Ask the player to choose Socket or RMI
 * 4. Ask for a nickname and number of players
 * 5. Build the view, proxy and controller
 * 6. Connect to the server
 * 7. Block in controller.run() until the game ends
 *
 * Usage:
 *   java -jar mesos-client.jar [host] [socketPort]
 *
 * Defaults:
 *   host       = localhost
 *   socketPort = 12345  (RMI uses socketPort + 1 = 12346)
 */
public class ClientApp {

    private static final Logger LOG = Logger.getLogger(ClientApp.class.getName());

    public static void main(String[] args) {
        String host       = args.length >= 1 ? args[0] : "localhost";
        int    socketPort = 12345;
        if (args.length >= 2) {
            try { socketPort = Integer.parseInt(args[1]); }
            catch (NumberFormatException e) { LOG.warning("Invalid port, using 12345"); }
        }
        int rmiPort = socketPort + 1;

        Scanner scanner = new Scanner(System.in);

        // ── 1. Choose interface ───────────────────────────────────────────────
        System.out.println("=== MESOS ===");
        System.out.println("Choose interface:");
        System.out.println("  1) TUI (text)");
        System.out.println("  2) GUI (graphical)");
        System.out.print("> ");
        String uiChoice = scanner.nextLine().trim();

        // ── 2. Choose transport ───────────────────────────────────────────────
        System.out.println("Choose connection:");
        System.out.println("  1) Socket (TCP)");
        System.out.println("  2) RMI");
        System.out.print("> ");
        String netChoice = scanner.nextLine().trim();

        // ── 3. Nickname ───────────────────────────────────────────────────────
        System.out.print("Your nickname: ");
        String nickname = scanner.nextLine().trim();
        while (nickname.isBlank()) {
            System.out.print("Nickname cannot be blank. Try again: ");
            nickname = scanner.nextLine().trim();
        }

        // ── 4. Number of players (only relevant if creating a new session) ────
        System.out.print("Number of players (2-5, used only if you create the session): ");
        int numPlayers = 2;
        try { numPlayers = Integer.parseInt(scanner.nextLine().trim()); }
        catch (NumberFormatException ignored) {}
        numPlayers = Math.max(2, Math.min(5, numPlayers));

        // ── 5. Build view ─────────────────────────────────────────────────────
        GameView view = "2".equals(uiChoice) ? new GuiView() : new TuiView(scanner);

        // ── 6. Build proxy ────────────────────────────────────────────────────
        ServerProxy proxy;
        if ("2".equals(netChoice)) {
            try {
                proxy = new RmiServerProxy(host, rmiPort);
            } catch (RemoteException e) {
                System.err.println("Could not create RMI proxy: " + e.getMessage());
                System.exit(1);
                return;
            }
        } else {
            proxy = new SocketServerProxy(host, socketPort);
        }

        // ── 7. Build controller ───────────────────────────────────────────────
        ClientController controller = new ClientController(proxy, view, nickname);
        proxy.setOnMessage(controller::onMessage);
        proxy.setOnDisconnect(controller::onDisconnect);

        // ── 8. Connect ────────────────────────────────────────────────────────
        try {
            proxy.connect(nickname, numPlayers);
            System.out.println("Connected! Waiting for other players...");
        } catch (IOException e) {
            System.err.println("Could not connect to server at "
                    + host + ":" + socketPort + " — " + e.getMessage());
            System.exit(1);
        }

        // ── 9. Block until the game ends ──────────────────────────────────────
        controller.run();
        proxy.disconnect();
    }
}