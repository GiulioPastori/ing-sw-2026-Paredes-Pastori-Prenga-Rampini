package it.polimi.it;

import it.polimi.it.player.Player;
import it.polimi.it.cards.Card;
import it.polimi.it.cards.building.Building;
import it.polimi.it.tiles.OfferTile;
import java.util.List;
import java.util.Scanner;
import java.util.stream.Collectors;

public class GameController {
    private final Game game;
    private final Scanner scanner;

    public GameController(Game game) {
        this.game = game;
        this.scanner = new Scanner(System.in);
    }

    public void run() {
        game.setup();
        System.out.println("--- INIZIO PARTITA A MESOS ---");

        while (game.getCurrentRound() < 10) {
            game.startNewRound();
            printRoundHeader();

            // 1. PIAZZAMENTO (Strategico: chi è primo decide dove mettersi)
            handlePlacementPhase();

            // 2. RISOLUZIONE (Meccanico: si segue il tracciato da A a E)
            handleResolutionPhase();

            // 3. MANUTENZIONE (Sostentamento ed Eventi)
            game.endOfRound();
        }

        showFinalResults();
    }

    private void handlePlacementPhase() {
        System.out.println("\n[FASE 1] PIAZZAMENTO TOTEM");
        List<Player> order = game.getPlacementOrder();

        for (Player p : order) {
            List<OfferTile> available = game.getBoard().getAvailableOfferTiles();
            System.out.println("\n> " + p.getName() + " (" + p.getFood() + " Cibo), scegli dove piazzarti:");

            for (int i = 0; i < available.size(); i++) {
                OfferTile t = available.get(i);
                System.out.printf("[%d] Tessera %s (Sotto: %d, Sopra: %d, Cibo: %d)%n",
                        i, t.getLetter(), t.getBottomRowCards(), t.getTopRowCards(), t.getFoodBonus());
            }

            int choice = getIntInput(0, available.size() - 1);
            game.getBoard().placeTotem(p.getTotem(), available.get(choice));
        }
    }

    private void handleResolutionPhase() {
        System.out.println("\n[FASE 2] RISOLUZIONE AZIONI");
        // Ordiniamo le tessere per lettera (A, B, C...) per sicurezza
        List<OfferTile> track = game.getBoard().getOfferTrack();

        for (OfferTile tile : track) {
            if (tile.isFree()) continue;

            Player acting = game.getPlayerByTotem(tile.getPlacedTotem());
            System.out.println("\n--- Risoluzione Tessera " + tile.getLetter() + " per " + acting.getName() + " ---");

            // A. Bonus Cibo (Eseguito prima di tutto)
            if (tile.getFoodBonus() > 0) {
                acting.addFood(tile.getFoodBonus());
                System.out.println("+ Ricevuti " + tile.getFoodBonus() + " cibo.");
            }

            // B. Draft Carte (Se non è la tessera A "Solo Cibo")
            if (!tile.isFoodOnly()) {
                // Regola: prima si pesca dalla riga sotto, poi da quella sopra
                processDraft(acting, tile.getBottomRowCards(), game.getBoard().getBottomRow(), "INFERIORE");
                processDraft(acting, tile.getTopRowCards(), game.getBoard().getTopRow(), "SUPERIORE");
            }

            // C. Turn Order: Il totem si sposta sulla tessera ordine di turno
            tile.removeTotem();
            game.getBoard().getTurnOrderTile().placeTotem(acting);
            System.out.println("> Totem spostato per l'ordine di turno del prossimo round.");
        }
    }

    private void processDraft(Player p, int count, List<Card> row, String label) {
        for (int i = 0; i < count; i++) {
            // Filtriamo solo le carte che non sono eventi (gli eventi si attivano nella board)
            List<Card> draftable = row.stream()
                    .filter(c -> !(c instanceof it.polimi.it.cards.events.Event))
                    .collect(Collectors.toList());

            if (draftable.isEmpty()) {
                System.out.println("Nessuna carta disponibile nella riga " + label);
                break;
            }

            System.out.println("\nScegli carta da riga " + label + " (" + (i+1) + "/" + count + "):");
            for (int j = 0; j < draftable.size(); j++) {
                System.out.println("[" + j + "] " + draftable.get(j).toString());
            }
            System.out.println("[" + draftable.size() + "] PASSA (non prendere nulla)");

            int choice = getIntInput(0, draftable.size());

            if (choice < draftable.size()) {
                Card selected = draftable.get(choice);
                // Controllo Cibo per Edifici
                if (selected instanceof Building b) {
                    int cost = b.getFoodCost(); // Qui dovresti considerare gli sconti della Tribe
                    if (p.getFood() < cost) {
                        System.out.println("! Cibo insufficiente (" + p.getFood() + "/" + cost + "). Scegli altro.");
                        i--; // Ripeti il draft per questa posizione
                        continue;
                    }
                }
                game.takeCard(p, selected);
            }
        }
    }

    // --- HELPER METHODS ---

    private void printRoundHeader() {
        System.out.println("\n========================================");
        System.out.println("   ROUND " + game.getCurrentRound() + " - ERA: " + game.getCurrentEra());
        System.out.println("========================================");
    }

    private int getIntInput(int min, int max) {
        while (true) {
            try {
                System.out.print("> Selezione: ");
                int val = Integer.parseInt(scanner.nextLine());
                if (val >= min && val <= max) return val;
                System.out.println("Scelta fuori range.");
            } catch (NumberFormatException e) {
                System.out.println("Inserisci un numero valido.");
            }
        }
    }

    private void showFinalResults() {
        System.out.println("\n PUNTEGGI FINALI ");
        game.endGame();
        game.getPlayers().stream()
                .sorted((p1, p2) -> Integer.compare(p2.getPrestigePoint(), p1.getPrestigePoint()))
                .forEach(p -> System.out.println("- " + p.getName() + ": " + p.getPrestigePoint() + " PP"));
    }
}