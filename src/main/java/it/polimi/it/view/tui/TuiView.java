package it.polimi.it.view.tui;

import it.polimi.it.enums.CharacterType;
import it.polimi.it.enums.Era;
import it.polimi.it.enums.EventType;
import it.polimi.it.network.server.persistence.LeaderboardEntry;
import it.polimi.it.observer.BoardSnapshot;
import it.polimi.it.observer.CardSnapshot;
import it.polimi.it.observer.FinalScoreBreakdown;
import it.polimi.it.observer.OfferTileSnapshot;
import it.polimi.it.observer.PlayerSnapshot;
import it.polimi.it.observer.PlayerResourceDelta;
import it.polimi.it.observer.TurnOrderTileSnapshot;
import it.polimi.it.view.CardLocalization;
import it.polimi.it.view.GameView;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static it.polimi.it.view.tui.AnsiStyle.*;

/**
 * Text user interface (TUI).
 *
 * <p>Writes to stdout using ANSI colours, role/event glyphs, and boxed sections
 * to separate the main contexts: round start, action phase, post-round, and
 * game over.
 *
 * <p>Input goes through {@link StdinReader}: a single daemon thread reads stdin
 * with blocking {@code BufferedReader.readLine()} and puts lines in a
 * {@code BlockingQueue}. Input methods poll the queue with a
 * {@value #INPUT_TIMEOUT_S}s timeout, so input handling remains interruptible.
 *
 * <p>Can be replaced by any other {@link GameView}, such as the JavaFX GUI.
 */
public class TuiView implements GameView {

    /**
     * Timeout for each stdin poll, in seconds. If it expires, the prompt loop
     * retries until the user answers or the thread is interrupted.
     */
    private static final long INPUT_TIMEOUT_S = 5;

    /** Fixed width of the main boxes, in terminal columns. */
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
        // Summary of the offer track after placement.
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
        // Updated Turn Order Tile state.
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
        showEventResolved(eventId, era, type, List.of());
    }

    @Override
    public void showEventResolved(int eventId, Era era, EventType type,
                                  List<PlayerResourceDelta> deltas) {
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
        for (PlayerResourceDelta d : deltas) {
            println("  " + d.playerName()
                    + " | cibo " + signed(d.foodDelta())
                    + " (" + d.foodBefore() + " -> " + d.foodAfter() + ")"
                    + " | PP " + signed(d.prestigeDelta())
                    + " (" + d.prestigeBefore() + " -> " + d.prestigeAfter() + ")");
        }
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
        showGameEnded(winners, finalScores, List.of());
    }

    @Override
    public void showGameEnded(List<PlayerSnapshot> winners, List<PlayerSnapshot> finalScores,
                              List<FinalScoreBreakdown> scoreBreakdowns) {
        println("");
        printBoxTop("PARTITA FINITA");
        String winnerNames = winners.stream()
                .map(PlayerSnapshot::name)
                .reduce((a, b) -> a + ", " + b).orElse("nessuno");
        printBoxBody(paint(FG_B_YELLOW, true, "🏆 VINCITORE/I: " + winnerNames));
        printBoxBody("");
        if (!scoreBreakdowns.isEmpty()) {
            printBoxBody("Resoconto calcolo punti:");
            scoreBreakdowns.forEach(b -> printBoxBody("  " + b.playerName()
                    + ": " + b.inGamePrestige() + " in partita"
                    + " + " + b.builderPoints() + " costruttori"
                    + " + " + b.inventorPoints() + " inventori"
                    + " + " + b.artistPoints() + " artisti"
                    + " + " + b.buildingPoints() + " edifici"
                    + " = " + b.totalPrestige()
                    + " | cibo " + b.food()));
            printBoxBody("");
        }
        printBoxBody("Punteggi finali:");
        finalScores.stream()
                .sorted((a, b) -> b.prestigePoints() - a.prestigePoints())
                .forEach(p -> printBoxBody("  " + formatPlayer(p)));
        printBoxBottom();
    }

    private static String signed(int value) {
        return value > 0 ? "+" + value : String.valueOf(value);
    }

    /** Date formatter for leaderboard entries (date only, locale-neutral). */
    private static final DateTimeFormatter LEADERBOARD_DATE_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    /** How many rows of the global leaderboard to show before truncating. */
    private static final int LEADERBOARD_MAX_ROWS = 10;

    @Override
    public void showLeaderboard(String recipient,
                                int myScore,
                                int myRank,
                                int totalEntries,
                                int numPlayers,
                                List<LeaderboardEntry> leaderboard) {
        showLeaderboard(recipient, myScore, myRank, totalEntries, numPlayers, leaderboard, Map.of());
    }

    @Override
    public void showLeaderboard(String recipient,
                                int myScore,
                                int myRank,
                                int totalEntries,
                                int numPlayers,
                                List<LeaderboardEntry> leaderboard,
                                Map<String, Integer> playerRanks) {
        println("");
        printBoxTop("CLASSIFICA — Partite a " + numPlayers + " giocatori");
        // Personal summary
        String rankText = myRank > 0
                ? "#" + myRank + " su " + totalEntries
                : "non disponibile";
        printBoxBody(paint(FG_B_CYAN, true, "Sei " + shorten(recipient, 24)
                + " — punteggio: " + myScore));
        printBoxBody(paint(FG_B_CYAN, true, "Posizione: " + rankText));
        printBoxBody("");
        if (leaderboard == null || leaderboard.isEmpty()) {
            printBoxBody(paint(DIM, "Nessuna partita registrata."));
        } else {
            printBoxBody(paint(FG_B_YELLOW, "  #   Giocatore       PP   Data"));
            int shown = 0;
            for (LeaderboardEntry e : leaderboard) {
                if (shown++ >= LEADERBOARD_MAX_ROWS) break;
                String name = shorten(e.nickname(), 14);
                String row = String.format("  %-3d %-14s %5d  %s",
                        e.rank(),
                        name,
                        e.finalScore(),
                        e.playedAt() == null ? "—" : LEADERBOARD_DATE_FMT.format(e.playedAt()));
                // Highlight the recipient's own row(s)
                if (recipient != null && recipient.equals(e.nickname())
                        && e.finalScore() == myScore) {
                    printBoxBody(paint(FG_B_GREEN, true, row));
                } else {
                    printBoxBody(row);
                }
            }
            if (leaderboard.size() > LEADERBOARD_MAX_ROWS) {
                printBoxBody(paint(DIM,
                        "  … (" + (leaderboard.size() - LEADERBOARD_MAX_ROWS)
                                + " righe non mostrate)"));
            }
        }
        printBoxBottom();
    }

    private static String shorten(String value, int maxVisibleChars) {
        if (value == null) return "N.D.";
        if (value.length() <= maxVisibleChars) return value;
        if (maxVisibleChars <= 1) return value.substring(0, maxVisibleChars);
        return value.substring(0, maxVisibleChars - 1) + "…";
    }

    @Override
    public void showInvalidAction(String playerName, String reason) {
        println(paint(FG_B_RED, true, "[!] ") + paint(FG_B_RED, playerName) + ": " + reason);
    }


    @Override
    public void showPlayerDisconnected(String nickname) {
        println(paint(FG_B_RED, true, "⚡ ") + paint(FG_B_RED, true, nickname)
                + paint(FG_RED, " si è disconnesso.")
                + paint(DIM, " Il gioco continua senza di lui."));
    }

    @Override
    public void showPlayerReconnected(String nickname) {
        println(paint(FG_B_GREEN, true, "✓ ") + paint(FG_B_GREEN, true, nickname)
                + paint(FG_B_GREEN, " si è riconnesso."));
    }

    // ── Input ─────────────────────────────────────────────────────────────────

    @Override
    public char requestOfferTileChoice(PlayerSnapshot player,
                                       BoardSnapshot board) {
        println("");
        println(paint(FG_B_CYAN, true, "→ " + formatPlayerName(player)
                + ", scegli una tessera:"));
        renderBoard(board);
        // The user sees the whole offer track, including occupied tiles, but
        // can choose only free tiles. Validation happens in the loop below.
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
    public int requestCardChoice(PlayerSnapshot player, List<CardSnapshot> available, boolean canPass) {
        if (available.isEmpty()) {
            println(paint(DIM, "[" + player.name() + "] Nessuna carta disponibile — passo."));
            return -1;
        }
        println("");
        String passHint = canPass ? " (0 per passare)" : "";
        println(paint(FG_B_CYAN, true, "→ " + formatPlayerName(player)
                + ", scegli una carta" + passHint + ":"));
        printCards(available);
        print(paint(FG_B_YELLOW, "Numero: "));
        while (true) {
            String line = nextLine();
            if (line == null) continue; // timeout, retry
            try {
                int choice = Integer.parseInt(line.trim());
                if (choice == 0) {
                    if (canPass) return -1;
                    print(paint(FG_RED, "Non puoi passare: devi scegliere una carta. Riprova: "));
                    continue;
                }
                if (choice >= 1 && choice <= available.size())
                    return available.get(choice - 1).id();
                String range = canPass ? "0-" + available.size() : "1-" + available.size();
                print(paint(FG_RED, "Scelta non valida (" + range + "). Riprova: "));
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

    @Override
    public void showLobbyUpdate(List<String> players, int needed) {

    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /**
     * Reads the next line with a {@value #INPUT_TIMEOUT_S}s timeout.
     * Returns {@code null} on timeout, so the caller can retry. Also returns
     * {@code null} when interrupted, so the caller can leave the loop or pass.
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
     * Detailed card format:
     *   CHARACTER: role glyph, coloured role name, and era
     *   BUILDING: cost, final points, and localized effect description
     *   EVENT: event type, glyph, and non-acquirable marker
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
     * Builds the string with character-specific printed icons:
     *  - HUNTER: trigger marker, when printed on the card
     *  - SHAMAN: as many stars as {@code starIcons}
     *  - INVENTOR: invention identifier
     *  - BUILDER: building-cost discount and end-game points
     *  - GATHERER, ARTIST: no distinguishing data because the effect is fixed
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
            case GATHERER, ARTIST -> ""; // fixed effect, no distinguishing data
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

    /** Long player summary with turn order and resources. */
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
     * Parses the EventType from the snapshot effectDescription. Event snapshots
     * store the enum name in that field; see {@link CardSnapshot#of}.
     */
    private static EventType parseEventType(String name) {
        try { return EventType.valueOf(name); }
        catch (IllegalArgumentException | NullPointerException e) { return null; }
    }

    // ── Box drawing ───────────────────────────────────────────────────────────

    /** Prints a titled box; fixed width keeps messages aligned. */
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
