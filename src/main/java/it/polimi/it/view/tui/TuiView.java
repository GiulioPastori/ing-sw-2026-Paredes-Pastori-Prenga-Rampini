package it.polimi.it.view.tui;

import it.polimi.it.enums.CharacterType;
import it.polimi.it.enums.Era;
import it.polimi.it.enums.EventType;
import it.polimi.it.observer.BoardSnapshot;
import it.polimi.it.observer.CardSnapshot;
import it.polimi.it.observer.OfferTileSnapshot;
import it.polimi.it.observer.PlayerSnapshot;
import it.polimi.it.observer.TurnOrderTileSnapshot;
import it.polimi.it.view.CardLocalization;
import it.polimi.it.view.GameView;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static it.polimi.it.view.tui.AnsiStyle.*;

/**
 * Interfaccia utente testuale (TUI).
 *
 * <p>Output su stdout con codici ANSI per il colore, emoji per i ruoli dei
 * personaggi e per i tipi di evento, e una formattazione a riquadri per
 * separare visivamente i diversi contesti (round / fase azione / post-round
 * / fine partita).
 *
 * <p>Input tramite {@link StdinReader}: un singolo thread daemon legge stdin
 * con {@code BufferedReader.readLine()} bloccante e mette le righe in una
 * {@code BlockingQueue}. I metodi di input prelevano dalla coda con un
 * timeout di {@value #INPUT_TIMEOUT_S}s, così il thread di input è sempre
 * interrompibile e non tiene mai bloccato il sistema.
 *
 * <p>Sostituibile con una GUI JavaFX implementando {@link GameView}.
 */
public class TuiView implements GameView {

    /** Timeout per ogni singola lettura da stdin (secondi).
     *  Se scade, viene ritentato — il loop continua finché l'utente non risponde
     *  o il thread viene interrotto. */
    private static final long INPUT_TIMEOUT_S = 5;

    /** Larghezza fissa dei riquadri principali (in caratteri). */
    private static final int BOX_WIDTH = 64;

    private final StdinReader stdin = StdinReader.getInstance();

    // ── Display ───────────────────────────────────────────────────────────────

    @Override
    public void showRoundStarted(int round, List<PlayerSnapshot> turnOrder,
                                 BoardSnapshot board) {
        printBoxTop("ROUND " + round + " — INIZIO");
        printBoxBody("Ordine di turno:");
        for (int i = 0; i < turnOrder.size(); i++) {
            PlayerSnapshot p = turnOrder.get(i);
            printBoxBody("  " + (i + 1) + ". " + formatPlayer(p));
        }
        printBoxBody("");
        printBoxBody("Mazzo Tribù: " + paint(FG_B_CYAN, true,
                "🂠 " + board.tribeDeckSize()) + paint(DIM, " carte rimanenti"));
        printBoxBottom();
        renderBoard(board);
        renderAllTribes(turnOrder);
        println("");
    }

    @Override
    public void showTotemPlaced(PlayerSnapshot player, char tileLetter,
                                BoardSnapshot board) {
        println(paint(FG_CYAN, "▸ ") + formatPlayerName(player)
                + " ha piazzato il totem "
                + paint(totemColor(player.totemColour()), true, "●")
                + " sulla tessera "
                + paint(FG_B_YELLOW, true, String.valueOf(tileLetter)) + ".");
        // Riassunto dello stato del tracciato dopo il piazzamento
        printOfferTrackOneLiner(board.offerTiles());
    }

    @Override
    public void showCardSlotReady(PlayerSnapshot player, boolean isTopRow,
                                  List<CardSnapshot> availableCards,
                                  List<CardSnapshot> visibleEvents) {
        String row = isTopRow ? "RIGA SUPERIORE" : "RIGA INFERIORE";
        String arrow = isTopRow ? "↑" : "↓";
        println("");
        println(paint(FG_B_CYAN, true, "── " + arrow + " " + formatPlayerName(player)
                + " — " + row + " ──"));
        if (!visibleEvents.isEmpty()) {
            println(paint(FG_B_RED, "  ⚠ Carte evento presenti (non acquisibili):"));
            visibleEvents.forEach(e -> println("    " + formatCard(e)));
        }
        if (availableCards.isEmpty()) {
            println(paint(DIM, "  (nessuna carta disponibile)"));
        } else {
            println(paint(FG_GRAY, "  Carte disponibili (" + availableCards.size() + "):"));
            printCards(availableCards);
        }
    }

    @Override
    public void showCardAcquired(PlayerSnapshot player, CardSnapshot card) {
        println(paint(FG_B_GREEN, "✔ ") + formatPlayerName(player)
                + " ha acquisito " + formatCard(card));
    }

    @Override
    public void showCardAcquisitionFailed(PlayerSnapshot player, CardSnapshot card,
                                          int missingFood) {
        println(paint(FG_B_RED, "✘ ") + formatPlayerName(player)
                + " non può permettersi " + formatCard(card)
                + paint(FG_RED, "  (mancano " + missingFood + " 🍖)"));
    }

    @Override
    public void showTotemReturnedToTurnOrder(PlayerSnapshot player, int position,
                                             BoardSnapshot board) {
        println(paint(FG_CYAN, "▸ ") + formatPlayerName(player)
                + " → Ordine di turno "
                + paint(FG_B_YELLOW, true, "#" + position)
                + paint(DIM, "  [🍖 " + player.food()
                + " | ⭐ " + player.prestigePoints() + "]"));
        // Stato aggiornato della Turn Order Tile
        renderTurnOrderTile(board.turnOrderTile());
    }

    @Override
    public void showPostRoundStarted(List<PlayerSnapshot> watchtowerPlayers,
                                     List<CardSnapshot> availableCards) {
        printBoxTop("TORRE DI GUARDIA — Post-round");
        printBoxBody("Possessori della Torre di Guardia:");
        watchtowerPlayers.forEach(p -> printBoxBody("  • " + formatPlayer(p)));
        printBoxBody("Carte disponibili:");
        printBoxBottom();
        printCards(availableCards);
    }

    @Override
    public void showEventResolved(int eventId, Era era, EventType type) {
        String desc = switch (type) {
            case SUSTENANCE      -> "Ogni giocatore paga 1 cibo per personaggio.";
            case HUNT            -> "Ogni giocatore guadagna cibo in base ai Cacciatori.";
            case SHAMANIC_RITUAL -> "Gli Sciamani garantiscono Punti Prestigio.";
            case CAVE_PAINTINGS  -> "Artisti e Inventori segnano punti.";
        };
        println("");
        println(paint(FG_B_MAGENTA, true,
                "✦ EVENTO RISOLTO ✦  " + eventEmoji(type) + " "
                        + eventNameIt(type)) + "  " + eraBadge(era)
                + paint(DIM, "  (id:" + eventId + ")"));
        println(paint(FG_GRAY, "  " + desc));
    }

    @Override
    public void showEraChanged(Era era) {
        String banner = "★ ★ ★   INIZIA " + (switch (era) {
            case ERA_I   -> "L'ERA I";
            case ERA_II  -> "L'ERA II";
            case ERA_III -> "L'ERA III";
        }) + "   ★ ★ ★";
        println("");
        println(paint(switch (era) {
            case ERA_I   -> FG_B_GREEN;
            case ERA_II  -> FG_B_YELLOW;
            case ERA_III -> FG_B_RED;
        }, true, banner));
        println("");
    }

    @Override
    public void showRoundEnded(int round) {
        println(paint(DIM, "── Round " + round + " terminato ──"));
    }

    @Override
    public void showGameEnded(List<PlayerSnapshot> winners, List<PlayerSnapshot> finalScores) {
        println("");
        printBoxTop("PARTITA FINITA");
        String winnerNames = winners.stream()
                .map(PlayerSnapshot::name)
                .reduce((a, b) -> a + ", " + b).orElse("nessuno");
        printBoxBody(paint(FG_B_YELLOW, true, "🏆 VINCITORE/I: " + winnerNames));
        printBoxBody("");
        printBoxBody("Punteggi finali:");
        finalScores.stream()
                .sorted((a, b) -> b.prestigePoints() - a.prestigePoints())
                .forEach(p -> printBoxBody("  " + formatPlayer(p)));
        printBoxBottom();
    }

    @Override
    public void showInvalidAction(String playerName, String reason) {
        println(paint(FG_B_RED, true, "[!] ") + paint(FG_B_RED, playerName) + ": " + reason);
    }

    // ── Input ─────────────────────────────────────────────────────────────────

    @Override
    public char requestOfferTileChoice(PlayerSnapshot player,
                                       BoardSnapshot board) {
        println("");
        println(paint(FG_B_CYAN, true, "→ " + formatPlayerName(player)
                + ", scegli una tessera:"));
        renderBoard(board);
        // L'utente vede tutto il tracciato (libere + occupate) ma può scegliere
        // solo le libere; la validazione è nel loop sotto.
        List<Character> freeLetters = board.offerTiles().stream()
                .filter(OfferTileSnapshot::isFree)
                .map(OfferTileSnapshot::letter)
                .toList();
        print(paint(FG_B_YELLOW, "Lettera tessera (libere: "
                + freeLetters.stream().map(String::valueOf)
                .reduce((a, b) -> a + ", " + b).orElse("nessuna")
                + "): "));
        while (true) {
            String raw = nextLine();
            if (raw == null) continue;
            String upper = raw.trim().toUpperCase();
            if (upper.length() == 1) {
                char letter = upper.charAt(0);
                if (freeLetters.contains(letter)) return letter;
            }
            print(paint(FG_RED, "Scelta non valida. Riprova: "));
        }
    }

    @Override
    public int requestCardChoice(PlayerSnapshot player, List<CardSnapshot> available) {
        if (available.isEmpty()) {
            println(paint(DIM, "[" + player.name() + "] Nessuna carta disponibile — passo."));
            return -1;
        }
        println("");
        println(paint(FG_B_CYAN, true, "→ " + formatPlayerName(player)
                + ", scegli una carta (0 per passare):"));
        printCards(available);
        print(paint(FG_B_YELLOW, "Numero: "));
        while (true) {
            String line = nextLine();
            if (line == null) continue; // timeout, riprova
            try {
                int choice = Integer.parseInt(line.trim());
                if (choice == 0) return -1;
                if (choice >= 1 && choice <= available.size())
                    return available.get(choice - 1).id();
                print(paint(FG_RED, "Scelta non valida (1-" + available.size() + " o 0). Riprova: "));
            } catch (NumberFormatException ignored) {
                print(paint(FG_RED, "Inserisci un numero. Riprova: "));
            }
        }
    }

    @Override
    public int requestPostRoundCardChoice(PlayerSnapshot player,
                                          List<CardSnapshot> available) {
        if (available.isEmpty()) return -1;
        println("");
        println(paint(FG_B_CYAN, true, "→ TORRE DI GUARDIA: " + formatPlayerName(player)
                + ", prendi una carta (0 per rifiutare):"));
        printCards(available);
        print(paint(FG_B_YELLOW, "Numero: "));
        while (true) {
            String line = nextLine();
            if (line == null) continue;
            try {
                int choice = Integer.parseInt(line.trim());
                if (choice == 0) return -1;
                if (choice >= 1 && choice <= available.size())
                    return available.get(choice - 1).id();
                print(paint(FG_RED, "Scelta non valida. Riprova: "));
            } catch (NumberFormatException ignored) {
                print(paint(FG_RED, "Inserisci un numero. Riprova: "));
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

    /**
     * Renders the full board state: Turn Order Tile on the left, Offer Track
     * (with totems on occupied tiles), and the two card rows.
     */
    private void renderBoard(BoardSnapshot board) {
        renderTurnOrderTile(board.turnOrderTile());
        renderOfferTrack(board.offerTiles());
    }

    /**
     * Single-line summary of the offer track. Each tile shows its letter and,
     * if occupied, the coloured ● glyph for the placed totem. Used after
     * {@code showTotemPlaced} so the player sees the new state at a glance.
     */
    private void printOfferTrackOneLiner(List<OfferTileSnapshot> tiles) {
        StringBuilder sb = new StringBuilder(paint(FG_GRAY, "  Tracciato: "));
        for (OfferTileSnapshot t : tiles) {
            String letter = paint(t.isFoodOnly() ? FG_B_GREEN : FG_B_YELLOW,
                    true, String.valueOf(t.letter()));
            String totem = t.isFree()
                    ? paint(DIM, "○")
                    : paint(totemColor(t.placedTotem()), true, "●");
            sb.append(letter).append(totem).append(" ");
        }
        println(sb.toString().stripTrailing());
    }

    /**
     * Renders the offer track with header + card rows.
     * Includes occupied tiles (showing the totem glyph) so the player can see
     * the entire board, not just the tiles still selectable.
     */
    private void renderOfferTrack(List<OfferTileSnapshot> tiles) {
        StringBuilder header = new StringBuilder(paint(FG_GRAY, "Tessere: "));
        for (OfferTileSnapshot t : tiles) {
            String letter = paint(
                    t.isFoodOnly() ? FG_B_GREEN : FG_B_YELLOW,
                    true,
                    String.valueOf(t.letter()));
            String totem = t.isFree()
                    ? ""
                    : paint(totemColor(t.placedTotem()), true, "●");
            String tag;
            if (t.isFoodOnly())  tag = paint(FG_GREEN, " [+3 🍖]  ");
            else                  tag = paint(FG_GRAY,
                    " [↑" + t.topRowCards() + "  ↓" + t.bottomRowCards() + "]  ");
            header.append(letter).append(totem).append(tag);
        }
        println(header.toString().stripTrailing());

        // Card rows are shared across non-food tiles: print them once.
        OfferTileSnapshot ref = tiles.stream()
                .filter(t -> !t.isFoodOnly())
                .filter(t -> !t.topCards().isEmpty() || !t.bottomCards().isEmpty())
                .findFirst().orElse(null);
        if (ref == null) return;

        if (!ref.topCards().isEmpty()) {
            println(paint(FG_GRAY, "  ↑ Riga superiore (" + ref.topCards().size() + " carte):"));
            ref.topCards().forEach(c -> println("      " + formatCard(c)));
        }
        if (!ref.bottomCards().isEmpty()) {
            println(paint(FG_GRAY, "  ↓ Riga inferiore (" + ref.bottomCards().size() + " carte):"));
            ref.bottomCards().forEach(c -> println("      " + formatCard(c)));
        }
    }

    /**
     * Renders the Turn Order Tile horizontally, e.g.:
     *   Ord. Turno [4P]: 1●R 2 3 4●B (last)
     * Each occupied space shows the coloured ● glyph; food bonuses appear as
     * "+N🍖" above the position number.
     */
    private void renderTurnOrderTile(TurnOrderTileSnapshot tot) {
        if (tot == null) return;
        StringBuilder sb = new StringBuilder(paint(FG_GRAY,
                "Ord. Turno [" + tot.numPlayers() + "P]: "));
        for (TurnOrderTileSnapshot.PositionSnap p : tot.positions()) {
            sb.append(paint(FG_B_WHITE, true, String.valueOf(p.position())));
            if (p.placedTotem() != null) {
                sb.append(paint(totemColor(p.placedTotem()), true, "●"));
            } else {
                sb.append(paint(DIM, "○"));
            }
            if (p.foodBonus() > 0) sb.append(paint(FG_GREEN, "+" + p.foodBonus() + "🍖"));
            if (p.isLast())        sb.append(paint(FG_RED, "(last)"));
            sb.append("  ");
        }
        println(sb.toString().stripTrailing());
    }

    /**
     * Compact tribe view shown after the round header. For each player:
     *   ● Alice — 🏹×2 🌾×1 🌀×1  |  🏛 102, 205
     * Buildings appear by ID (with a tooltip on the long form when the player
     * actually picks a card).
     */
    private void renderAllTribes(List<PlayerSnapshot> players) {
        boolean any = players.stream()
                .anyMatch(p -> !p.tribeCharacters().isEmpty()
                        || !p.tribeBuildings().isEmpty());
        if (!any) return;
        println(paint(FG_GRAY, "Tribù:"));
        players.forEach(this::renderTribeLine);
    }

    private void renderTribeLine(PlayerSnapshot p) {
        StringBuilder sb = new StringBuilder("  ");
        sb.append(paint(totemColor(p.totemColour()), true, "●"));
        sb.append(' ').append(formatPlayerName(p)).append(" — ");

        // Characters grouped by type with count
        for (CharacterType ct : CharacterType.values()) {
            long n = p.tribeCharacters().stream()
                    .filter(c -> c.characterType() == ct).count();
            if (n == 0) continue;
            sb.append(paint(roleColor(ct), true, emoji(ct)))
                    .append(paint(FG_GRAY, "×" + n)).append(' ');
        }
        if (p.tribeCharacters().isEmpty()) sb.append(paint(DIM, "(nessun personaggio) "));

        // Aggregated character details: total Shaman stars, unique inventions,
        // hunter triggers, total builder discount/PP. Drives end-game intuition.
        String agg = aggregateCharacterDetails(p);
        if (!agg.isEmpty()) sb.append(paint(DIM, "[")).append(agg).append(paint(DIM, "] "));

        // Buildings as id list
        if (!p.tribeBuildings().isEmpty()) {
            sb.append(paint(DIM, "| ")).append(paint(FG_B_YELLOW, true, "🏛 "));
            String ids = p.tribeBuildings().stream()
                    .map(c -> String.valueOf(c.id()))
                    .reduce((a, b) -> a + ", " + b).orElse("");
            sb.append(paint(FG_YELLOW, ids));
        }
        println(sb.toString());
    }

    /**
     * Computes a one-line summary of character distinguishing data:
     *  - total stars from all Shamans
     *  - distinct invention icon count
     *  - hunter triggers
     *  - total Builder discount / end-game PP
     * Returns "" if there's nothing to show.
     */
    private String aggregateCharacterDetails(PlayerSnapshot p) {
        int stars = 0;
        long triggers = 0;
        int  builderDiscount = 0;
        int  builderPoints = 0;
        java.util.Set<String> inventions = new java.util.HashSet<>();

        for (CardSnapshot c : p.tribeCharacters()) {
            if (c.details() == null) continue;
            stars           += c.details().starIcons();
            builderDiscount += c.details().buildingDiscount();
            builderPoints   += c.details().builderPoints();
            if (c.details().triggerIcon()) triggers++;
            if (!c.details().inventionIcon().isEmpty())
                inventions.add(c.details().inventionIcon());
        }

        StringBuilder sb = new StringBuilder();
        if (stars > 0)               sb.append(paint(FG_B_YELLOW, true, "★" + stars)).append(' ');
        if (triggers > 0)            sb.append(paint(FG_B_RED, true, "🎯×" + triggers)).append(' ');
        if (!inventions.isEmpty())   sb.append(paint(FG_B_CYAN, true,
                "💡×" + inventions.size())).append(' ');
        if (builderDiscount > 0)     sb.append(paint(FG_B_GREEN, true,
                "🔨-" + builderDiscount)).append(' ');
        if (builderPoints > 0)       sb.append(paint(FG_B_YELLOW, true,
                "⭐+" + builderPoints)).append(' ');
        return sb.toString().stripTrailing();
    }

    private void printCards(List<CardSnapshot> cards) {
        for (int i = 0; i < cards.size(); i++) {
            String idx = paint(FG_B_WHITE, true, String.format("%2d.", i + 1));
            println("  " + idx + " " + formatCard(cards.get(i)));
        }
    }

    /**
     * Formato carta dettagliato:
     *   PERSONAGGIO  → emoji + ruolo colorato + era
     *   EDIFICIO     → costo, PP finali, descrizione effetto in italiano
     *   EVENTO       → tipo + emoji + nota "non acquisibile"
     */
    private String formatCard(CardSnapshot c) {
        return switch (c.type()) {
            case CHARACTER -> formatCharacterCard(c);
            case BUILDING  -> formatBuildingCard(c);
            case EVENT     -> formatEventCard(c);
        };
    }

    private String formatCharacterCard(CardSnapshot c) {
        CharacterType ct = c.characterType();
        String label = paint(roleColor(ct), true,
                emoji(ct) + " " + roleNameIt(ct).toUpperCase());
        String details = formatCharacterDetails(c);
        return label
                + (details.isEmpty() ? "" : "  " + details)
                + "  " + eraBadge(c.era())
                + paint(DIM, "  (id:" + c.id() + ")");
    }

    /**
     * Costruisce la stringa con le icone distintive di un personaggio in base
     * al tipo. Per:
     *  - HUNTER:   "🎯" se ha l'icona trigger (caccia immediata), altrimenti vuoto
     *  - SHAMAN:   stelline pari a starIcons (★ ★★ ★★★)
     *  - INVENTOR: l'identificatore dell'invenzione fra parentesi
     *  - BUILDER:  sconto al costo edifici e PP a fine partita
     *  - GATHERER, ARTIST: nessun dato distintivo (effetto fisso)
     */
    private String formatCharacterDetails(CardSnapshot c) {
        if (c.details() == null || c.characterType() == null) return "";
        CardSnapshot.CharacterDetails d = c.details();
        return switch (c.characterType()) {
            case HUNTER -> d.triggerIcon()
                    ? paint(FG_B_RED, true, "🎯 trigger")
                    : "";
            case SHAMAN -> paint(FG_B_YELLOW, true,
                    "★".repeat(Math.max(0, d.starIcons())));
            case INVENTOR -> d.inventionIcon().isEmpty()
                    ? ""
                    : paint(FG_B_CYAN, true, "💡 " + d.inventionIcon());
            case BUILDER -> {
                StringBuilder sb = new StringBuilder();
                if (d.buildingDiscount() != 0) {
                    sb.append(paint(FG_B_GREEN, true,
                            "🔨 -" + d.buildingDiscount() + " cibo"));
                }
                if (d.builderPoints() != 0) {
                    if (sb.length() > 0) sb.append(" ");
                    sb.append(paint(FG_B_YELLOW, true,
                            "⭐ +" + d.builderPoints()));
                }
                yield sb.toString();
            }
            case GATHERER, ARTIST -> ""; // effetto fisso, niente da differenziare
        };
    }

    private String formatBuildingCard(CardSnapshot c) {
        String head = paint(FG_B_YELLOW, true, "🏛 EDIFICIO");
        String stats = paint(FG_B_GREEN, "🍖 " + c.foodCost())
                + paint(DIM, "  •  ")
                + paint(FG_B_YELLOW, true, "⭐ " + c.finalPoints());
        String desc = CardLocalization.buildingDescription(c.id(), c.effectDescription());
        return head + "  " + eraBadge(c.era()) + "  " + stats
                + "\n        " + paint(ITALIC, FG_GRAY) + desc + RESET
                + paint(DIM, "  (id:" + c.id() + ")");
    }

    private String formatEventCard(CardSnapshot c) {
        EventType et = parseEventType(c.effectDescription());
        String head = paint(FG_B_RED, true, "⚡ EVENTO " + eventEmoji(et)
                + " " + eventNameIt(et));
        return head + "  " + eraBadge(c.era())
                + paint(FG_RED, "  ✗ non acquisibile")
                + paint(DIM, "  (id:" + c.id() + ")");
    }

    /** Player riepilogo lungo (ordine di turno + risorse). */
    private String formatPlayer(PlayerSnapshot p) {
        return formatPlayerName(p)
                + paint(DIM, "  [")
                + paint(FG_B_GREEN, "🍖 " + p.food())
                + paint(DIM, " | ")
                + paint(FG_B_YELLOW, "⭐ " + p.prestigePoints())
                + paint(DIM, "]");
    }

    /** Just the player's name, colored by their totem. */
    private String formatPlayerName(PlayerSnapshot p) {
        return paint(totemColor(p.totemColour()), true, p.name());
    }

    /**
     * Recupera l'EventType dall'effectDescription dello snapshot.
     * Lo snapshot di un evento mette il nome dell'enum dentro effectDescription
     * (vedi {@link CardSnapshot#of}).
     */
    private static EventType parseEventType(String name) {
        try { return EventType.valueOf(name); }
        catch (IllegalArgumentException | NullPointerException e) { return null; }
    }

    // ── Box drawing ───────────────────────────────────────────────────────────

    /** Stampa un riquadro con titolo. La larghezza è fissa per allinearsi tra messaggi. */
    private void printBoxTop(String title) {
        String inner = " " + title + " ";
        // padding centered
        int pad = Math.max(0, BOX_WIDTH - 2 - inner.length());
        int left = pad / 2;
        int right = pad - left;
        println(paint(FG_B_YELLOW, true,
                "╔" + "═".repeat(left) + inner + "═".repeat(right) + "╗"));
    }

    private void printBoxBody(String body) {
        // Visible-length padding: ANSI codes don't take screen columns, so we
        // strip them when computing the pad to keep the right border aligned.
        int visibleLen = stripAnsi(body).length();
        int pad = Math.max(0, BOX_WIDTH - 4 - visibleLen);
        println(paint(FG_B_YELLOW, "║ ") + body + " ".repeat(pad)
                + paint(FG_B_YELLOW, " ║"));
    }

    private void printBoxBottom() {
        println(paint(FG_B_YELLOW, true, "╚" + "═".repeat(BOX_WIDTH - 2) + "╝"));
    }

    /** Removes ANSI escape sequences for length calculations. */
    private static String stripAnsi(String s) {
        return s.replaceAll("\u001B\\[[;\\d]*m", "");
    }

    private void println(String s) { System.out.println(s); }
    private void print(String s)   { System.out.print(s); System.out.flush(); }
}