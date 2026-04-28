package it.polimi.it.view.tui;

import it.polimi.it.enums.Era;
import it.polimi.it.enums.EventType;
import it.polimi.it.observer.CardSnapshot;
import it.polimi.it.observer.OfferTileSnapshot;
import it.polimi.it.observer.PlayerSnapshot;
import it.polimi.it.view.GameView;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Interfaccia utente testuale (TUI).
 *
 * <p>Output su stdout. Input tramite {@link StdinReader}: un singolo thread
 * daemon legge stdin con {@code BufferedReader.readLine()} bloccante e mette
 * le righe in una {@code BlockingQueue}. I metodi di input prelevano dalla coda
 * con un timeout di {@value #INPUT_TIMEOUT_S}s, così il thread di input
 * è sempre interrompibile e non tiene mai bloccato il sistema.
 *
 * <p>Sostituibile con una GUI JavaFX implementando {@link GameView}.
 */
public class TuiView implements GameView {

    /** Timeout per ogni singola lettura da stdin (secondi).
     *  Se scade, viene ritentato — il loop continua finché l'utente non risponde
     *  o il thread viene interrotto. */
    private static final long INPUT_TIMEOUT_S = 5;

    private final StdinReader stdin = StdinReader.getInstance();

    // ── Display ───────────────────────────────────────────────────────────────

    @Override
    public void showRoundStarted(int round, List<PlayerSnapshot> turnOrder,
                                 List<OfferTileSnapshot> availableTiles) {
        println("\n╔══════════════════════════════╗");
        println("║       ROUND " + pad2(round) + " INIZIATO        ║");
        println("╚══════════════════════════════╝");
        println("Ordine di turno:");
        for (int i = 0; i < turnOrder.size(); i++) {
            PlayerSnapshot p = turnOrder.get(i);
            println("  " + (i + 1) + ". " + p.name()
                    + "  [Cibo: " + p.food() + " | PP: " + p.prestigePoints() + "]");
        }
        println("");
        showBoardWithTiles(availableTiles);
    }

    @Override
    public void showTotemPlaced(PlayerSnapshot player, char tileLetter) {
        println(player.name() + " ha piazzato il totem sulla tessera " + tileLetter);
    }

    @Override
    public void showCardSlotReady(PlayerSnapshot player, boolean isTopRow,
                                  List<CardSnapshot> availableCards,
                                  List<CardSnapshot> visibleEvents) {
        String row = isTopRow ? "RIGA SUPERIORE" : "RIGA INFERIORE";
        println("\n[" + player.name() + "] " + row
                + " — " + availableCards.size() + " carta/e disponibile/i:");
        if (!visibleEvents.isEmpty()) {
            println("  ⚠ Carte evento presenti (non acquisibili):");
            visibleEvents.forEach(e -> println("    • " + formatCard(e)));
        }
        printCards(availableCards);
    }

    @Override
    public void showCardAcquired(PlayerSnapshot player, CardSnapshot card) {
        println(player.name() + " ha acquisito: " + formatCard(card));
    }

    @Override
    public void showCardAcquisitionFailed(PlayerSnapshot player, CardSnapshot card,
                                          int missingFood) {
        println("[!] " + player.name() + " non può permettersi " + formatCard(card)
                + " (mancano " + missingFood + " cibo).");
    }

    @Override
    public void showTotemReturnedToTurnOrder(PlayerSnapshot player, int position) {
        println(player.name() + " → Ordine di turno #" + position
                + "  [Cibo: " + player.food() + " | PP: " + player.prestigePoints() + "]");
    }

    @Override
    public void showPostRoundStarted(List<PlayerSnapshot> watchtowerPlayers,
                                     List<CardSnapshot> availableCards) {
        println("\n[TORRE DI GUARDIA] Azione post-round:");
        watchtowerPlayers.forEach(p -> println("  • " + p.name()));
        println("Carte disponibili:");
        printCards(availableCards);
    }

    @Override
    public void showEventResolved(int eventId, Era era, EventType type) {
        String desc = switch (type) {
            case SUSTENANCE      -> "Ogni giocatore paga 1 cibo per personaggio";
            case HUNT            -> "Ogni giocatore guadagna cibo in base ai Cacciatori";
            case SHAMANIC_RITUAL -> "Gli Sciamani garantiscono Punti Prestigio";
            case CAVE_PAINTINGS  -> "Artisti e Inventori segnano punti";
        };
        println("[EVENTO RISOLTO] " + formatEra(era) + " — "
                + formatEventType(type.name()) + " (id:" + eventId + "): " + desc);
    }

    @Override
    public void showEraChanged(Era era) {
        println("\n★★★  INIZIA L'ERA " + formatEra(era) + "  ★★★\n");
    }

    @Override
    public void showRoundEnded(int round) {
        println("── Round " + round + " terminato ──");
    }

    @Override
    public void showGameEnded(List<PlayerSnapshot> winners, List<PlayerSnapshot> finalScores) {
        println("\n╔══════════════════════════════╗");
        println("║         PARTITA FINITA       ║");
        println("╚══════════════════════════════╝");
        String winnerNames = winners.stream()
                .map(PlayerSnapshot::name)
                .reduce((a, b) -> a + ", " + b).orElse("nessuno");
        println("VINCITORE/I: " + winnerNames);
        println("\nPunteggi finali:");
        finalScores.stream()
                .sorted((a, b) -> b.prestigePoints() - a.prestigePoints())
                .forEach(p -> println("  " + p.name()
                        + ": " + p.prestigePoints() + " PP"
                        + " | Cibo: " + p.food()));
    }

    @Override
    public void showInvalidAction(String playerName, String reason) {
        println("[!] " + playerName + ": " + reason);
    }

    // ── Input ─────────────────────────────────────────────────────────────────

    @Override
    public char requestOfferTileChoice(PlayerSnapshot player,
                                       List<OfferTileSnapshot> freeTiles) {
        println("\n[" + player.name() + "] Scegli una tessera:");
        showBoardWithTiles(freeTiles);
        print("Lettera tessera: ");
        while (true) {
            String raw = nextLine();
            if (raw == null) continue;
            String upper = raw.trim().toUpperCase();
            if (upper.length() == 1) {
                char letter = upper.charAt(0);
                if (freeTiles.stream().anyMatch(t -> t.letter() == letter)) return letter;
            }
            print("Scelta non valida. Riprova: ");
        }
    }

    @Override
    public int requestCardChoice(PlayerSnapshot player, List<CardSnapshot> available) {
        if (available.isEmpty()) {
            println("[" + player.name() + "] Nessuna carta disponibile — passo.");
            return -1;
        }
        println("\n[" + player.name() + "] Scegli una carta (0 per passare):");
        printCards(available);
        print("Numero: ");
        while (true) {
            String line = nextLine();
            if (line == null) continue; // timeout, riprova
            try {
                int choice = Integer.parseInt(line.trim());
                if (choice == 0) return -1;
                if (choice >= 1 && choice <= available.size())
                    return available.get(choice - 1).id();
                print("Scelta non valida (1-" + available.size() + " o 0). Riprova: ");
            } catch (NumberFormatException ignored) {
                print("Inserisci un numero. Riprova: ");
            }
        }
    }

    @Override
    public int requestPostRoundCardChoice(PlayerSnapshot player,
                                          List<CardSnapshot> available) {
        if (available.isEmpty()) return -1;
        println("\n[TORRE DI GUARDIA " + player.name() + "] Prendi una carta (0 per rifiutare):");
        printCards(available);
        print("Numero: ");
        while (true) {
            String line = nextLine();
            if (line == null) continue;
            try {
                int choice = Integer.parseInt(line.trim());
                if (choice == 0) return -1;
                if (choice >= 1 && choice <= available.size())
                    return available.get(choice - 1).id();
                print("Scelta non valida. Riprova: ");
            } catch (NumberFormatException ignored) {
                print("Inserisci un numero. Riprova: ");
            }
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /**
     * Legge la prossima riga con timeout {@value #INPUT_TIMEOUT_S}s.
     * Ritorna {@code null} se il timeout scade (il chiamante deve riprovare).
     * Ritorna {@code null} anche se il thread viene interrotto
     * (il chiamante deve uscire dal loop o passare).
     */
    private String nextLine() {
        try {
            return stdin.readLine(INPUT_TIMEOUT_S, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        }
    }

    /** Mostra tessere con le carte di entrambe le righe. */
    /**
     * Mostra le tessere disponibili nel formato richiesto:
     * - Prima riga: elenco delle tessere con le loro lettere e quante carte si possono prendere
     * - Poi una volta sola la riga superiore con tutte le carte
     * - Poi una volta sola la riga inferiore con tutte le carte
     *
     * Le carte sono condivise tra tutte le tessere, quindi non ha senso
     * ripeterle per ogni tessera.
     */
    private void showBoardWithTiles(List<OfferTileSnapshot> tiles) {
        // Riga 1: elenco tessere con i loro slot
        StringBuilder tileHeader = new StringBuilder("Tessere: ");
        List<OfferTileSnapshot> nonFood = tiles.stream()
                .filter(t -> !t.isFoodOnly()).toList();
        List<OfferTileSnapshot> food = tiles.stream()
                .filter(OfferTileSnapshot::isFoodOnly).toList();

        food.forEach(t -> tileHeader.append(t.letter()).append(" [+3 CIBO]  "));
        nonFood.forEach(t -> tileHeader
                .append(t.letter())
                .append(" [sup:").append(t.topRowCards())
                .append(" inf:").append(t.bottomRowCards()).append("]  "));
        println(tileHeader.toString().stripTrailing());

        // Le carte sono le stesse per tutte le tessere non-food: prendi la prima
        if (!nonFood.isEmpty()) {
            OfferTileSnapshot ref = nonFood.get(0);
            if (!ref.topCards().isEmpty()) {
                println("  ↑ Riga superiore (" + ref.topCards().size() + " carte):");
                ref.topCards().forEach(c -> println("      • " + formatCard(c)));
            }
            if (!ref.bottomCards().isEmpty()) {
                println("  ↓ Riga inferiore (" + ref.bottomCards().size() + " carte):");
                ref.bottomCards().forEach(c -> println("      • " + formatCard(c)));
            }
        }
    }

    /** @deprecated Usa showBoardWithTiles */
    private void showTiles(List<OfferTileSnapshot> tiles) {
        showBoardWithTiles(tiles);
    }

    private void printCards(List<CardSnapshot> cards) {
        for (int i = 0; i < cards.size(); i++) {
            println("  " + (i + 1) + ". " + formatCard(cards.get(i)));
        }
    }

    private String formatCard(CardSnapshot c) {
        return switch (c.type()) {
            case CHARACTER -> "[" + formatEra(c.era()) + "] PERSONAGGIO "
                    + (c.characterType() != null ? formatCharType(c.characterType().name()) : "")
                    + " (id:" + c.id() + ")";
            case BUILDING  -> "[" + formatEra(c.era()) + "] EDIFICIO"
                    + "  costo:" + c.foodCost()
                    + "  PP:"    + c.finalPoints()
                    + "  "       + c.effectDescription()
                    + " (id:"    + c.id() + ")";
            case EVENT     -> "[" + formatEra(c.era()) + "] EVENTO "
                    + formatEventType(c.effectDescription())
                    + " (id:" + c.id() + ")  ⚠ non acquisibile";
        };
    }

    private String formatEra(Era era) {
        return switch (era) {
            case ERA_I   -> "Era I";
            case ERA_II  -> "Era II";
            case ERA_III -> "Era III";
        };
    }

    private String formatEventType(String typeName) {
        return switch (typeName) {
            case "SUSTENANCE"      -> "Sostentamento";
            case "HUNT"            -> "Caccia";
            case "SHAMANIC_RITUAL" -> "Rituale Sciamanico";
            case "CAVE_PAINTINGS"  -> "Pitture Rupestri";
            default                -> typeName;
        };
    }

    private String formatCharType(String typeName) {
        return switch (typeName) {
            case "HUNTER"   -> "Cacciatore";
            case "GATHERER" -> "Raccoglitore";
            case "BUILDER"  -> "Costruttore";
            case "SHAMAN"   -> "Sciamano";
            case "INVENTOR" -> "Inventore";
            case "ARTIST"   -> "Artista";
            default         -> typeName;
        };
    }

    private String pad2(int n) { return n < 10 ? " " + n : String.valueOf(n); }
    private void println(String s) { System.out.println(s); }
    private void print(String s)   { System.out.print(s); System.out.flush(); }
}

