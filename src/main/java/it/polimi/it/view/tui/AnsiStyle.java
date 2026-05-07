package it.polimi.it.view.tui;

import it.polimi.it.enums.CharacterType;
import it.polimi.it.enums.Era;
import it.polimi.it.enums.EventType;
import it.polimi.it.enums.TotemColour;

/**
 * ANSI escape sequences and emoji used by {@link TuiView}.
 *
 * <p>All output here is purely cosmetic: if the user's terminal does not
 * support ANSI, the codes will appear as garbage but the underlying text
 * remains readable. We keep emoji and colour usage consistent across the
 * whole TUI so a single style change here propagates everywhere.
 *
 * <p>Note: this helper does not touch any business logic and contains no
 * model state. It is a pure utility class.
 */
final class AnsiStyle {

    // ── Reset / styles ────────────────────────────────────────────────────────
    static final String RESET     = "\u001B[0m";
    static final String BOLD      = "\u001B[1m";
    static final String DIM       = "\u001B[2m";
    static final String ITALIC    = "\u001B[3m";
    static final String UNDERLINE = "\u001B[4m";

    // ── Foreground colours ───────────────────────────────────────────────────
    static final String FG_BLACK   = "\u001B[30m";
    static final String FG_RED     = "\u001B[31m";
    static final String FG_GREEN   = "\u001B[32m";
    static final String FG_YELLOW  = "\u001B[33m";
    static final String FG_BLUE    = "\u001B[34m";
    static final String FG_MAGENTA = "\u001B[35m";
    static final String FG_CYAN    = "\u001B[36m";
    static final String FG_WHITE   = "\u001B[37m";
    static final String FG_GRAY    = "\u001B[90m";

    // Bright variants
    static final String FG_B_RED     = "\u001B[91m";
    static final String FG_B_GREEN   = "\u001B[92m";
    static final String FG_B_YELLOW  = "\u001B[93m";
    static final String FG_B_BLUE    = "\u001B[94m";
    static final String FG_B_MAGENTA = "\u001B[95m";
    static final String FG_B_CYAN    = "\u001B[96m";
    static final String FG_B_WHITE   = "\u001B[97m";

    private AnsiStyle() { /* utility */ }

    // ── Convenience wrappers ──────────────────────────────────────────────────

    /** Wraps {@code text} between {@code style} and {@link #RESET}. */
    static String paint(String style, String text) {
        return style + text + RESET;
    }

    /** Bold + colour. */
    static String paint(String colour, boolean bold, String text) {
        return (bold ? BOLD : "") + colour + text + RESET;
    }

    // ── Emoji / labels per role/era ──────────────────────────────────────────

    /** Emoji rappresentativa del ruolo del personaggio. */
    static String emoji(CharacterType t) {
        if (t == null) return "";
        return switch (t) {
            case HUNTER   -> "🏹";
            case GATHERER -> "🌾";
            case BUILDER  -> "🔨";
            case SHAMAN   -> "🌀";
            case INVENTOR -> "💡";
            case ARTIST   -> "🎨";
        };
    }

    /** Colour associated to a character role (purely visual). */
    static String roleColor(CharacterType t) {
        if (t == null) return FG_WHITE;
        return switch (t) {
            case HUNTER   -> FG_B_RED;
            case GATHERER -> FG_B_GREEN;
            case BUILDER  -> FG_YELLOW;
            case SHAMAN   -> FG_B_MAGENTA;
            case INVENTOR -> FG_B_CYAN;
            case ARTIST   -> FG_MAGENTA;
        };
    }

    /** Italian display name for the role. */
    static String roleNameIt(CharacterType t) {
        if (t == null) return "?";
        return switch (t) {
            case HUNTER   -> "Cacciatore";
            case GATHERER -> "Raccoglitore";
            case BUILDER  -> "Costruttore";
            case SHAMAN   -> "Sciamano";
            case INVENTOR -> "Inventore";
            case ARTIST   -> "Artista";
        };
    }

    /** Italian display name for an event type. */
    static String eventNameIt(EventType t) {
        if (t == null) return "?";
        return switch (t) {
            case SUSTENANCE      -> "Sostentamento";
            case HUNT            -> "Caccia";
            case SHAMANIC_RITUAL -> "Rituale Sciamanico";
            case CAVE_PAINTINGS  -> "Pitture Rupestri";
        };
    }

    /** Emoji for an event type (used in the event banner). */
    static String eventEmoji(EventType t) {
        if (t == null) return "";
        return switch (t) {
            case SUSTENANCE      -> "🍖";
            case HUNT            -> "🏹";
            case SHAMANIC_RITUAL -> "🌀";
            case CAVE_PAINTINGS  -> "🎨";
        };
    }

    /** Compact era badge ("Era I" / "Era II" / "Era III") with colour. */
    static String eraBadge(Era era) {
        String label = switch (era) {
            case ERA_I   -> "Era I";
            case ERA_II  -> "Era II";
            case ERA_III -> "Era III";
        };
        String col = switch (era) {
            case ERA_I   -> FG_B_GREEN;
            case ERA_II  -> FG_B_YELLOW;
            case ERA_III -> FG_B_RED;
        };
        return paint(col, true, label);
    }

    /** Colour for the totem of a player. */
    static String totemColor(TotemColour tc) {
        if (tc == null) return FG_WHITE;
        return switch (tc) {
            case RED    -> FG_B_RED;
            case BLUE   -> FG_B_BLUE;
            case YELLOW -> FG_B_YELLOW;
            case WHITE  -> FG_B_WHITE;
            case BLACK  -> FG_GRAY;
        };
    }

    /** Italian display name for a totem colour. */
    static String totemNameIt(TotemColour tc) {
        if (tc == null) return "?";
        return switch (tc) {
            case RED    -> "Rosso";
            case BLUE   -> "Blu";
            case YELLOW -> "Giallo";
            case WHITE  -> "Bianco";
            case BLACK  -> "Nero";
        };
    }
}
