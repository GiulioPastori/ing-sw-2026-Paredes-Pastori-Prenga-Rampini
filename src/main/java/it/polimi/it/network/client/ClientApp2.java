package it.polimi.it.network.client;

import it.polimi.it.view.GameView;
import it.polimi.it.view.gui.GuiView;
import it.polimi.it.view.tui.TuiView;

import java.io.IOException;
import java.rmi.RemoteException;
import java.util.Scanner;
import java.util.logging.Logger;

public class ClientApp2 {

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

            System.out.println("=== MESOS ===");

            // ── 1. Interface ──────────────────────────────────────────────────────
            System.out.println("Choose interface:  1) TUI   2) GUI");
            System.out.print("> ");
            String uiChoice = scanner.nextLine().trim().toLowerCase();
            boolean useGui = uiChoice.equals("2") || uiChoice.equals("gui");

            // ── 2. Transport ──────────────────────────────────────────────────────
            System.out.println("Choose connection: 1) Socket (TCP)   2) RMI");
            System.out.print("> ");
            String netChoice = scanner.nextLine().trim().toLowerCase();
            boolean useRmi = netChoice.equals("2") || netChoice.equals("rmi");

            // ── 3. Nickname ───────────────────────────────────────────────────────
            System.out.print("Your nickname: ");
            String nickname = scanner.nextLine().trim();
            while (nickname.isBlank()) {
                System.out.print("Nickname cannot be blank. Try again: ");
                nickname = scanner.nextLine().trim();
            }

            // ── 4. Number of players ──────────────────────────────────────────────
            System.out.print("Number of players (2-5): ");
            int numPlayers = 2;
            try { numPlayers = Integer.parseInt(scanner.nextLine().trim()); }
            catch (NumberFormatException ignored) {}
            numPlayers = Math.max(2, Math.min(5, numPlayers));

            // ── 5. Build view ─────────────────────────────────────────────────────
            GameView view = useGui ? new GuiView() : new TuiView(scanner);

            // ── 6. Build proxy ────────────────────────────────────────────────────
            ServerProxy proxy;
            if (useRmi) {
                try {
                    proxy = new RmiServerProxy(host, rmiPort);
                    System.out.println("Using RMI on port " + rmiPort);
                } catch (RemoteException e) {
                    System.err.println("Cannot create RMI proxy: " + e.getMessage());
                    System.exit(1);
                    return;
                }
            } else {
                proxy = new SocketServerProxy(host, socketPort);
                System.out.println("Using Socket on port " + socketPort);
            }

            // ── 7. Build controller ───────────────────────────────────────────────
            ClientController controller = new ClientController(proxy, view, nickname);
            proxy.setOnMessage(controller::onMessage);
            proxy.setOnDisconnect(controller::onDisconnect);

            // ── 8. Connect ────────────────────────────────────────────────────────
            int connectPort = useRmi ? rmiPort : socketPort;
            try {
                proxy.connect(nickname, numPlayers);
                System.out.println("Connected to " + host + ":" + connectPort
                        + " as '" + nickname + "'. Waiting for other players...");
            } catch (IOException e) {
                System.err.println("Cannot connect to " + host + ":" + connectPort
                        + " — " + e.getMessage());
                System.exit(1);
            }

            // ── 9. Run ────────────────────────────────────────────────────────────
            controller.run();
            proxy.disconnect();
            System.out.println("Goodbye!");
        }
    }
