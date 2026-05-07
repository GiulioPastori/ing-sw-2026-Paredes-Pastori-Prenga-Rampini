package it.polimi.it.view;

import java.util.Map;

/**
 * Localized Italian descriptions for building effects, keyed by card ID.
 *
 * <p>The card data in {@code cards.json} carries English descriptions
 * (the JSON is the canonical source — see {@code CLAUDE.md}, "do not change
 * the schema of cards.json"). This class holds the Italian display strings
 * shown in the UI without touching the JSON.
 *
 * <p>If a building ID is missing from this map, callers should fall back to
 * the original English {@code effectDescription} from the snapshot.
 *
 * <p>Reusable by both {@code TuiView} and {@code JavaFxView} (e.g. as a tooltip
 * on building cards).
 */
public final class CardLocalization {

    private CardLocalization() { /* utility */ }

    /**
     * Map from building ID to Italian description. Updated whenever a new
     * building is added to {@code cards.json}.
     */
    private static final Map<Integer, String> BUILDING_IT = Map.ofEntries(
            // ── Era I ─────────────────────────────────────────────────────
            Map.entry(101, "Per ogni nuova serie completa di 6 tipi: +5 cibo"),
            Map.entry(102, "Sostentamento: -1 cibo per ogni Raccoglitore"),
            Map.entry(103, "Rituale Sciamanico: immune alla penalità di minoranza"),
            Map.entry(104, "Ordine di turno: +1 cibo se atterri su uno spazio bonus cibo"),
            Map.entry(105, "Coppia di Inventori con la stessa icona: +3 cibo"),
            Map.entry(106, "Sostentamento: -1 cibo per ogni Artista"),

            // ── Era II ────────────────────────────────────────────────────
            Map.entry(201, "Rituale Sciamanico: raddoppia il bonus di maggioranza"),
            Map.entry(202, "Caccia: +1 cibo e +1 PP per ogni Cacciatore"),
            Map.entry(203, "Fine partita: PP dei Costruttori raddoppiati"),
            Map.entry(204, "Pitture Rupestri: +1 cibo per ogni Artista"),
            Map.entry(205, "Fine partita: 6 PP per ogni serie completa di 6 tipi"),
            Map.entry(206, "Sostentamento: -1 cibo per ogni Inventore"),
            Map.entry(207, "Rituale Sciamanico: +3 icone stella virtuali"),

            // ── Era III ───────────────────────────────────────────────────
            Map.entry(301, "Fine partita: 25 PP fissi"),
            Map.entry(302, "Prendi una carta in più dalla riga superiore alla fine di ogni round"),
            Map.entry(303, "Fine partita: 3 PP per ogni Cacciatore"),
            Map.entry(304, "Fine partita: 4 PP per ogni Costruttore"),
            Map.entry(305, "Fine partita: 4 PP per ogni Raccoglitore"),
            Map.entry(306, "Fine partita: 4 PP per ogni Artista"),
            Map.entry(307, "Fine partita: 4 PP per ogni Sciamano"),
            Map.entry(308, "Fine partita: 2 PP per ogni Inventore")
    );

    /**
     * Returns the Italian description for a building card.
     *
     * @param buildingId the card ID
     * @param fallback   the English description from the snapshot
     * @return the Italian translation, or {@code fallback} if no translation exists
     */
    public static String buildingDescription(int buildingId, String fallback) {
        return BUILDING_IT.getOrDefault(buildingId, fallback);
    }
}
