package it.polimi.it.network.client;

import it.polimi.it.view.GameView;
import it.polimi.it.view.gui.GuiView;
import it.polimi.it.view.tui.StdinReader;
import it.polimi.it.view.tui.TuiView;

import java.io.IOException;
import java.rmi.RemoteException;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

public class ClientApp2 {

    private static final Logger LOG = Logger.getLogger(ClientApp2.class.getName());

    public static void main(String[] args) {
        String host       = args.length >= 1 ? args[0] : "localhost";
        int    socketPort = 12345;
        if (args.length >= 2) {
            try { socketPort = Integer.parseInt(args[1]); }
            catch (NumberFormatException e) { LOG.warning("Invalid port, using 12345"); }
        }
        int rmiPort = socketPort + 1;

        StdinReader stdin = StdinReader.getInstance();

        System.out.println("=== MESOS ===");

        System.out.println("Choose interface:  1) TUI   2) GUI");
        System.out.print("> ");
        String uiChoice = readLine(stdin);
        boolean useGui = uiChoice.equals("2") || uiChoice.equals("gui");

        System.out.println("Choose connection: 1) Socket (TCP)   2) RMI");
        System.out.print("> ");
        String netChoice = readLine(stdin).toLowerCase();
        boolean useRmi = netChoice.equals("2") || netChoice.equals("rmi");

        System.out.print("Your nickname: ");
        String nickname = readLine(stdin);
        while (nickname.isBlank()) {
            System.out.print("Nickname cannot be blank. Try again: ");
            nickname = readLine(stdin);
        }

        System.out.print("Number of players (2-5): ");
        int numPlayers = 2;
        try { numPlayers = Integer.parseInt(readLine(stdin)); }
        catch (NumberFormatException ignored) {}
        numPlayers = Math.max(2, Math.min(5, numPlayers));

        GameView view = useGui ? new GuiView() : new TuiView();

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

        ClientController controller = new ClientController(proxy, view, nickname);
        proxy.setOnMessage(controller::onMessage);
        proxy.setOnDisconnect(controller::onDisconnect);

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

        controller.run();
        proxy.disconnect();
        System.out.println("Goodbye!");
    }

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

