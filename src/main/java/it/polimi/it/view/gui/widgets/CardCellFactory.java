package it.polimi.it.view.gui.widgets;

import it.polimi.it.observer.CardSnapshot;
import it.polimi.it.observer.OfferTileSnapshot;
import it.polimi.it.observer.PlayerSnapshot;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextAlignment;
import it.polimi.it.view.gui.widgets.TileImageCache;

import java.util.function.Consumer;

/**
 * Factory per i nodi grafici riutilizzabili: carte, tessere offerta, riepilogo
 * giocatore.
 *
 * <p>Le carte mostrano l'immagine PNG (se disponibile) caricata da
 * {@link CardImageCache}, altrimenti un placeholder testuale con tipo/era.
 *
 * <p>Tutti i nodi ricevono opzionalmente un {@link Consumer} di click — se
 * presente la card è "selectable", altrimenti è disabilitata (informativa).
 */
public final class CardCellFactory {

    private static final double CARD_W = 90;
    private static final double CARD_H = 120;

    private CardCellFactory() { /* utility */ }

    // ── Card ──────────────────────────────────────────────────────────────────

    /**
     * @param card           lo snapshot della carta da disegnare
     * @param onClick        se non null, la carta è cliccabile e invoca questo
     *                       consumer con l'id della carta
     */
    public static StackPane buildCard(CardSnapshot card, Consumer<Integer> onClick) {
        StackPane cell = new StackPane();
        cell.getStyleClass().add("card-cell");
        cell.setPrefSize(CARD_W, CARD_H);

        switch (card.type()) {
            case CHARACTER -> {
                cell.getStyleClass().add("character");
                if (card.characterType() != null) {
                    cell.getStyleClass().add(card.characterType().name());
                }
            }
            case BUILDING -> cell.getStyleClass().add("building");
            case EVENT    -> cell.getStyleClass().add("event");
        }

        Image img = CardImageCache.imageFor(card);
        if (img != null) {
            ImageView iv = new ImageView(img);
            iv.setFitWidth(CARD_W - 8);
            iv.setFitHeight(CARD_H - 8);
            iv.setPreserveRatio(true);
            iv.getStyleClass().add("card-cell-image");
            cell.getChildren().add(iv);
        } else {
            // Placeholder testuale: era + tipo + costo/punti
            VBox box = new VBox(2);
            box.setAlignment(Pos.CENTER);

            Label era = new Label(card.era().name());
            era.getStyleClass().add("card-label");

            Label name = new Label(shortLabel(card));
            name.getStyleClass().add("card-label");
            name.setWrapText(true);
            name.setTextAlignment(TextAlignment.CENTER);

            box.getChildren().addAll(era, name);

            if (card.type() == CardSnapshot.Type.BUILDING) {
                Label info = new Label("Cost " + card.foodCost() + " · " + card.finalPoints() + " PP");
                info.getStyleClass().add("card-label");
                box.getChildren().add(info);
            }
            cell.getChildren().add(box);
        }

        if (onClick != null && !card.isEvent()) {
            cell.getStyleClass().add("selectable");
            final int id = card.id();
            cell.setOnMouseClicked(e -> onClick.accept(id));
        } else {
            cell.getStyleClass().add("disabled");
        }

        return cell;
    }

    private static String shortLabel(CardSnapshot c) {
        return switch (c.type()) {
            case CHARACTER -> c.characterType() != null ? c.characterType().name() : "CHARACTER";
            case BUILDING  -> "BUILDING";
            case EVENT     -> "EVENT";
        };
    }

    // ── Tile (offer track) ────────────────────────────────────────────────────

    /**
     * @param tile     snapshot della tessera offerta
     * @param onClick  se non null, la tessera è cliccabile e invoca il consumer
     *                 con la lettera scelta. Se null la tessera è di sola lettura.
     */
    public static StackPane buildTile(OfferTileSnapshot tile, Consumer<Character> onClick) {
        StackPane cell = new StackPane();
        cell.getStyleClass().add("tile-cell");
        cell.setPrefSize(80, 70);

        if (tile.isFoodOnly())  cell.getStyleClass().add("food-only");
        if (!tile.isFree())     cell.getStyleClass().add("placed");

        Image img = TileImageCache.imageFor(tile);
        if (img != null) {
            ImageView iv = new ImageView(img);
            iv.setFitWidth(76);
            iv.setFitHeight(66);
            iv.setPreserveRatio(true);
            iv.getStyleClass().add("tile-cell-image");
            cell.getChildren().add(iv);
        } else {
            // Placeholder testuale (identico a prima)
            VBox box = new VBox(2);
            box.setAlignment(Pos.CENTER);

            Label letter = new Label(String.valueOf(tile.letter()));
            letter.getStyleClass().add("tile-letter");

            Label info = new Label(
                    tile.isFoodOnly()
                            ? "+3 Food"
                            : "T:" + tile.topRowCards() + "  B:" + tile.bottomRowCards());
            info.getStyleClass().add("tile-info");

            box.getChildren().addAll(letter, info);
            cell.getChildren().add(box);
        }

        if (onClick != null && tile.isFree()) {
            cell.getStyleClass().add("selectable");
            final char l = tile.letter();
            cell.setOnMouseClicked(e -> onClick.accept(l));
        } else {
            cell.getStyleClass().add("disabled");
        }

        return cell;
    }

    // ── Player card ───────────────────────────────────────────────────────────

    /**
     * Mini-pannello giocatore (nome, cibo, punti prestigio). Il flag
     * {@code active} aggiunge un bordo evidenziato.
     */
    public static VBox buildPlayerCard(PlayerSnapshot p, boolean active) {
        VBox card = new VBox(3);
        card.getStyleClass().add("player-card");
        if (active) card.getStyleClass().add("active");
        card.setAlignment(Pos.CENTER);

        Label name = new Label(p.name());
        name.getStyleClass().add("player-name");

        Label totem = new Label(p.totemColour() != null ? p.totemColour().name() : "");
        totem.setStyle("-fx-text-fill: rgba(220,195,150,0.6); -fx-font-size: 10px;");

        Label food = new Label("Food: " + p.food());
        food.getStyleClass().add("player-food");

        Label pp = new Label("PP: " + p.prestigePoints());
        pp.getStyleClass().add("player-pp");

        card.getChildren().addAll(name, totem, food, pp);
        return card;
    }
}
