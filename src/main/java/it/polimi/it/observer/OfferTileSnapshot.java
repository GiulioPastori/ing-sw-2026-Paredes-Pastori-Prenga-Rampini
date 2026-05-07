package it.polimi.it.observer;

import it.polimi.it.cards.Card;
import it.polimi.it.enums.TotemColour;
import it.polimi.it.tiles.OfferTile;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * Snapshot immutabile di una tessera offerta.
 *
 * <p>Include le liste COMPLETE delle carte visibili nelle due righe del
 * tabellone (inclusi gli eventi, non acquisibili ma visibili), in modo che
 * la view possa mostrare al giocatore TUTTE le carte disponibili prima di
 * decidere dove piazzare il totem.
 *
 * <p>Nota: le carte sono condivise tra tutte le tessere — non sono
 * "assegnate" a una tessera specifica. {@code topRowCards} e
 * {@code bottomRowCards} indicano quante carte il giocatore PUÒ PRENDERE
 * da quella riga se sceglie questa tessera, non quante carte ci sono.
 *
 * <p>{@link #placedTotem()} riporta il colore del totem attualmente sulla
 * tessera, o {@code null} se libera. La view lo usa per disegnare il totem
 * sopra la tessera (e per evidenziare le tessere dei vari giocatori in modo
 * coerente con il loro colore).
 */
public record OfferTileSnapshot(
        char               letter,
        int                topRowCards,
        int                bottomRowCards,
        boolean            isFoodOnly,
        boolean            isFree,
        TotemColour        placedTotem,
        List<CardSnapshot> topCards,
        List<CardSnapshot> bottomCards
) implements Serializable {
    @Serial private static final long serialVersionUID = 3L;

    /**
     * Crea uno snapshot da una tessera concreta, includendo TUTTE le carte
     * visibili nelle due righe del tabellone (eventi inclusi) e il totem
     * eventualmente presente.
     *
     * @param tile      la tessera offerta
     * @param topRow    la riga superiore corrente del tabellone
     * @param bottomRow la riga inferiore corrente del tabellone
     */
    public static OfferTileSnapshot of(OfferTile tile,
                                       List<Card> topRow,
                                       List<Card> bottomRow) {
        List<CardSnapshot> top    = topRow.stream().map(CardSnapshot::of).toList();
        List<CardSnapshot> bottom = bottomRow.stream().map(CardSnapshot::of).toList();
        return new OfferTileSnapshot(
                tile.getLetter(),
                tile.getTopRowCards(),
                tile.getBottomRowCards(),
                tile.isFoodOnly(),
                tile.isFree(),
                tile.getPlacedTotem(),
                top,
                bottom);
    }

    /**
     * Versione senza liste di carte (usata dove non si ha accesso alle righe).
     * Le liste saranno vuote — la view mostra solo i conteggi.
     */
    public static OfferTileSnapshot of(OfferTile tile) {
        return new OfferTileSnapshot(
                tile.getLetter(),
                tile.getTopRowCards(),
                tile.getBottomRowCards(),
                tile.isFoodOnly(),
                tile.isFree(),
                tile.getPlacedTotem(),
                List.of(),
                List.of());
    }
}