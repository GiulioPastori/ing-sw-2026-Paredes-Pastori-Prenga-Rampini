package it.polimi.it.view.tui;

import it.polimi.it.enums.Era;
import it.polimi.it.enums.EventType;
import it.polimi.it.observer.CardSnapshot;
import it.polimi.it.observer.OfferTileSnapshot;
import it.polimi.it.observer.PlayerSnapshot;
import it.polimi.it.view.GameView;

import java.util.List;
import java.util.Scanner;

/**
 * Text-based user interface.
 *
 * All output goes to stdout. Input is read from a Scanner (stdin).
 * Request methods block until the player enters a valid choice.
 */
public class TuiView implements GameView {

    private final Scanner scanner;

    public TuiView(Scanner scanner) {
        this.scanner = scanner;
    }

    // ── Display ───────────────────────────────────────────────────────────────

    @Override
    public void showRoundStarted(int round, List<PlayerSnapshot> turnOrder) {
        println("\n╔══════════════════════════════╗");
        println("║        ROUND " + round + " STARTED         ║");
        println("╚══════════════════════════════╝");
        println("Turn order:");
        for (int i = 0; i < turnOrder.size(); i++) {
            PlayerSnapshot p = turnOrder.get(i);
            println("  " + (i + 1) + ". " + p.name()
                    + "  [Food: " + p.food() + " | PP: " + p.prestigePoints() + "]");
        }
    }

    @Override
    public void showTotemPlaced(PlayerSnapshot player, char tileLetter) {
        println(player.name() + " placed totem on tile " + tileLetter);
    }

    @Override
    public void showCardSlotReady(PlayerSnapshot player, boolean isTopRow,
                                  List<CardSnapshot> availableCards) {
        println("\n[" + player.name() + "] " + (isTopRow ? "TOP" : "BOTTOM")
                + " row — " + availableCards.size() + " card(s) available.");
        printCards(availableCards);
    }

    @Override
    public void showCardAcquired(PlayerSnapshot player, CardSnapshot card) {
        println(player.name() + " acquired: " + formatCard(card));
    }

    @Override
    public void showCardAcquisitionFailed(PlayerSnapshot player, CardSnapshot card,
                                          int missingFood) {
        println(player.name() + " cannot afford " + formatCard(card)
                + " (missing " + missingFood + " food).");
    }

    @Override
    public void showTotemReturnedToTurnOrder(PlayerSnapshot player, int position) {
        println(player.name() + " → Turn Order #" + position
                + "  [Food: " + player.food() + " | PP: " + player.prestigePoints() + "]");
    }

    @Override
    public void showPostRoundStarted(List<PlayerSnapshot> watchtowerPlayers,
                                     List<CardSnapshot> availableCards) {
        println("\n[WATCHTOWER] Post-round action:");
        watchtowerPlayers.forEach(p -> println("  • " + p.name()));
        printCards(availableCards);
    }

    @Override
    public void showEventResolved(int eventId, Era era, EventType type) {
        println("[EVENT] " + era + " " + type + " resolved.");
    }

    @Override
    public void showEraChanged(Era era) {
        println("\n★★★  ERA " + era + " BEGINS  ★★★\n");
    }

    @Override
    public void showRoundEnded(int round) {
        println("── Round " + round + " ended ──");
    }

    @Override
    public void showGameEnded(List<PlayerSnapshot> winners, List<PlayerSnapshot> finalScores) {
        println("\n╔══════════════════════════════╗");
        println("║           GAME OVER          ║");
        println("╚══════════════════════════════╝");
        String winnerNames = winners.stream()
                .map(PlayerSnapshot::name)
                .reduce((a, b) -> a + ", " + b).orElse("none");
        println("WINNER(S): " + winnerNames);
        println("\nFinal Scores:");
        finalScores.stream()
                .sorted((a, b) -> b.prestigePoints() - a.prestigePoints())
                .forEach(p -> println("  " + p.name() + ": " + p.prestigePoints() + " PP"
                        + " | Food: " + p.food()));
    }

    @Override
    public void showInvalidAction(String playerName, String reason) {
        println("[!] " + playerName + ": " + reason);
    }

    // ── Input ─────────────────────────────────────────────────────────────────

    @Override
    public char requestOfferTileChoice(PlayerSnapshot player,
                                       List<OfferTileSnapshot> freeTiles) {
        println("\n[" + player.name() + "] Choose an offer tile:");
        freeTiles.forEach(t -> println("  " + t.letter()
                + (t.isFoodOnly() ? " [+3 FOOD]"
                : " [top:" + t.topRowCards() + " bottom:" + t.bottomRowCards() + "]")));
        println("Enter tile letter: ");
        while (true) {
            String input = scanner.nextLine().trim().toUpperCase();
            if (input.length() == 1) {
                char letter = input.charAt(0);
                if (freeTiles.stream().anyMatch(t -> t.letter() == letter)) {
                    return letter;
                }
            }
            println("Invalid choice. Try again: ");
        }
    }

    @Override
    public int requestCardChoice(PlayerSnapshot player, List<CardSnapshot> available) {
        if (available.isEmpty()) {
            println("[" + player.name() + "] No cards available — passing.");
            return -1;
        }
        println("\n[" + player.name() + "] Choose a card (0 to pass):");
        printCards(available);
        println("Enter number: ");
        while (true) {
            try {
                int choice = Integer.parseInt(scanner.nextLine().trim());
                if (choice == 0) return -1;
                if (choice >= 1 && choice <= available.size())
                    return available.get(choice - 1).id();
            } catch (NumberFormatException ignored) {}
            println("Invalid choice. Try again: ");
        }
    }

    @Override
    public int requestPostRoundCardChoice(PlayerSnapshot player,
                                          List<CardSnapshot> available) {
        if (available.isEmpty()) return -1;
        println("\n[WATCHTOWER " + player.name() + "] Take a card (0 to decline):");
        printCards(available);
        println("Enter number: ");
        while (true) {
            try {
                int choice = Integer.parseInt(scanner.nextLine().trim());
                if (choice == 0) return -1;
                if (choice >= 1 && choice <= available.size())
                    return available.get(choice - 1).id();
            } catch (NumberFormatException ignored) {}
            println("Invalid choice. Try again: ");
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void printCards(List<CardSnapshot> cards) {
        for (int i = 0; i < cards.size(); i++) {
            println("  " + (i + 1) + ". " + formatCard(cards.get(i)));
        }
    }

    private String formatCard(CardSnapshot c) {
        return switch (c.type()) {
            case CHARACTER -> "[" + c.era() + "] " + c.characterType()
                    + " (id:" + c.id() + ")";
            case BUILDING  -> "[" + c.era() + "] BUILDING  cost:" + c.foodCost()
                    + "  PP:" + c.finalPoints()
                    + "  " + c.effectDescription()
                    + " (id:" + c.id() + ")";
            case EVENT     -> "[EVENT] (id:" + c.id() + ")";
        };
    }

    private void println(String text) {
        System.out.println(text);
    }
}