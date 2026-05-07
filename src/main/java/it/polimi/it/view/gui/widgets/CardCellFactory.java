package it.polimi.it.view.gui.widgets;

import it.polimi.it.enums.CharacterType;
import it.polimi.it.enums.TotemColour;
import it.polimi.it.observer.CardSnapshot;
import it.polimi.it.observer.OfferTileSnapshot;
import it.polimi.it.observer.PlayerSnapshot;
import it.polimi.it.observer.TurnOrderTileSnapshot;
import it.polimi.it.view.CardLocalization;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.scene.text.TextAlignment;
import javafx.util.Duration;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Factory for reusable JavaFX nodes: cards, offer tiles, the turn order tile,
 * the tribe deck, and the player/tribe summary.
 *
 * <p>All factory methods return a self-contained {@link javafx.scene.Node} (or
 * a subtype) so they can be added to any parent. Card images are loaded by
 * {@link CardImageCache}; tile images by {@link TileImageCache}. Both fall
 * back to readable text placeholders if the image is missing.
 *
 * <p>Selectability is opt-in: passing a non-null {@code onClick} consumer
 * makes the node interactive ({@code .selectable} CSS class) and wires the
 * mouse handler. Tile cells additionally apply {@code .placed} when occupied
 * and {@code .disabled} when not selectable.
 */
public final class CardCellFactory {

    private static final double CARD_W = 90;
    private static final double CARD_H = 120;
    private static final double TILE_W = 80;
    private static final double TILE_H = 110;
    private static final double TRIBE_CARD_W = 50;
    private static final double TRIBE_CARD_H = 70;

    private CardCellFactory() { /* utility */ }

    // ── Card ──────────────────────────────────────────────────────────────────

    /**
     * Builds a full-size card cell suitable for the offer rows.
     *
     * @param card    the card snapshot to render
     * @param onClick if non-null and the card is acquirable (not an event),
     *                the cell becomes selectable and invokes this consumer
     *                with the card ID on click
     */
    public static StackPane buildCard(CardSnapshot card, Consumer<Integer> onClick) {
        return buildCard(card, onClick, CARD_W, CARD_H);
    }

    /**
     * Builds a card cell at a custom size. Used by {@link #buildPlayerPanel}
     * to render small thumbnails inside the tribe view.
     */
    public static StackPane buildCard(CardSnapshot card, Consumer<Integer> onClick,
                                      double w, double h) {
        StackPane cell = new StackPane();
        cell.getStyleClass().add("card-cell");
        cell.setPrefSize(w, h);

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
            iv.setFitWidth(w - 8);
            iv.setFitHeight(h - 8);
            iv.setPreserveRatio(true);
            iv.getStyleClass().add("card-cell-image");
            cell.getChildren().add(iv);
        } else {
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
                Label info = new Label("Costo " + card.foodCost() + " · " + card.finalPoints() + " PP");
                info.getStyleClass().add("card-label");
                box.getChildren().add(info);
            }
            cell.getChildren().add(box);
        }

        // Tooltip: italian description for buildings, role for characters,
        // event type for events. Useful both on the offer rows and on the
        // small tribe thumbnails.
        attachTooltip(cell, card);

        if (onClick != null && !card.isEvent()) {
            cell.getStyleClass().add("selectable");
            final int id = card.id();
            cell.setOnMouseClicked(e -> onClick.accept(id));
        } else {
            cell.getStyleClass().add("disabled");
        }
        return cell;
    }

    private static void attachTooltip(StackPane cell, CardSnapshot card) {
        String text = switch (card.type()) {
            case BUILDING  -> "Edificio #" + card.id()
                    + "\nCosto: " + card.foodCost() + " 🍖   PP: " + card.finalPoints()
                    + "\n" + CardLocalization.buildingDescription(card.id(), card.effectDescription());
            case CHARACTER -> (card.characterType() != null
                    ? card.characterType().name() : "Personaggio")
                    + "  (id " + card.id() + ", " + card.era() + ")";
            case EVENT     -> "Evento  (id " + card.id() + ", " + card.era() + ")"
                    + "\n" + card.effectDescription();
        };
        Tooltip tip = new Tooltip(text);
        tip.setShowDelay(Duration.millis(250));
        Tooltip.install(cell, tip);
    }

    private static String shortLabel(CardSnapshot c) {
        return switch (c.type()) {
            case CHARACTER -> c.characterType() != null ? c.characterType().name() : "PERSONAGGIO";
            case BUILDING  -> "EDIFICIO";
            case EVENT     -> "EVENTO";
        };
    }

    // ── Offer Tile ────────────────────────────────────────────────────────────

    /**
     * Builds an offer tile cell. If the tile is occupied, a coloured totem is
     * overlaid in the bottom-right corner. The cell is always shown (occupied
     * or not) so the player sees the full offer track; the {@code onClick}
     * consumer is invoked even on occupied tiles so the controller can show
     * a "tile already occupied" message — making the validation visible
     * rather than swallowed.
     *
     * @param tile     snapshot of the offer tile
     * @param onClick  if non-null, invoked with the tile letter on click
     *                 (works on both free and occupied tiles)
     */
    public static StackPane buildTile(OfferTileSnapshot tile, Consumer<Character> onClick) {
        StackPane cell = new StackPane();
        cell.getStyleClass().add("tile-cell");
        cell.setPrefSize(TILE_W, TILE_H);

        if (tile.isFoodOnly()) cell.getStyleClass().add("food-only");
        if (!tile.isFree())    cell.getStyleClass().add("placed");

        // Background: image if present, text placeholder otherwise.
        Image img = TileImageCache.imageFor(tile);
        if (img != null) {
            ImageView iv = new ImageView(img);
            iv.setFitWidth(TILE_W - 4);
            iv.setFitHeight(TILE_H - 4);
            iv.setPreserveRatio(true);
            iv.getStyleClass().add("tile-cell-image");
            cell.getChildren().add(iv);
        } else {
            VBox box = new VBox(2);
            box.setAlignment(Pos.CENTER);
            Label letter = new Label(String.valueOf(tile.letter()));
            letter.getStyleClass().add("tile-letter");
            Label info = new Label(tile.isFoodOnly()
                    ? "+3 Cibo"
                    : "↑" + tile.topRowCards() + "  ↓" + tile.bottomRowCards());
            info.getStyleClass().add("tile-info");
            box.getChildren().addAll(letter, info);
            cell.getChildren().add(box);
        }

        // Letter badge in the top-left so it stays visible over the image
        Label letterBadge = new Label(String.valueOf(tile.letter()));
        letterBadge.getStyleClass().add("tile-letter-badge");
        StackPane.setAlignment(letterBadge, Pos.TOP_LEFT);
        StackPane.setMargin(letterBadge, new Insets(4, 0, 0, 4));
        cell.getChildren().add(letterBadge);

        // Totem overlay if occupied
        if (!tile.isFree() && tile.placedTotem() != null) {
            StackPane totem = buildTotemBadge(tile.placedTotem(), 24);
            StackPane.setAlignment(totem, Pos.BOTTOM_RIGHT);
            StackPane.setMargin(totem, new Insets(0, 6, 6, 0));
            cell.getChildren().add(totem);
        }

        if (onClick != null) {
            cell.getStyleClass().add("selectable");
            final char l = tile.letter();
            cell.setOnMouseClicked(e -> onClick.accept(l));
        } else {
            cell.getStyleClass().add("disabled");
        }
        return cell;
    }

    /**
     * Builds a coloured totem badge (filled circle with thin border) of the
     * requested diameter. Used both as an overlay on offer tiles and inside
     * the turn order tile spaces.
     */
    public static StackPane buildTotemBadge(TotemColour colour, double diameter) {
        Circle c = new Circle(diameter / 2.0);
        c.getStyleClass().addAll("totem-badge", totemCssClass(colour));
        StackPane wrap = new StackPane(c);
        wrap.setPrefSize(diameter, diameter);
        wrap.setMaxSize(diameter, diameter);
        return wrap;
    }

    private static String totemCssClass(TotemColour t) {
        if (t == null) return "totem-none";
        return switch (t) {
            case RED    -> "totem-red";
            case BLUE   -> "totem-blue";
            case YELLOW -> "totem-yellow";
            case WHITE  -> "totem-white";
            case BLACK  -> "totem-black";
        };
    }

    // ── Turn Order Tile ───────────────────────────────────────────────────────

    /**
     * Builds the Turn Order Tile as a horizontal strip of {@code numPlayers + 1}
     * (or the actual number of positions) cells. Each cell shows the position
     * number and any food bonus; if a totem is on it, a coloured badge is
     * overlaid; if it is the last (penalty) position, the {@code .last} CSS
     * class is added so the stylesheet can highlight it.
     *
     * <p>The whole strip is wrapped in a {@link StackPane} so a background
     * image can be applied via CSS; the variant comes from {@code numPlayers}
     * (the stylesheet selects {@code .turn-order-2}, {@code .turn-order-3}, …).
     */
    public static StackPane buildTurnOrderTile(TurnOrderTileSnapshot snap) {
        StackPane root = new StackPane();
        root.getStyleClass().addAll("turn-order-tile",
                "turn-order-" + (snap == null ? "?" : snap.numPlayers()));
        root.setPrefSize(TILE_W * 1.4, TILE_H);
        root.setMinHeight(TILE_H);

        if (snap == null) return root;

        // Optional background image variant (turn_order_2.png ... turn_order_5.png)
        Image bg = TileImageCache.turnOrderTileFor(snap.numPlayers());
        if (bg != null) {
            ImageView iv = new ImageView(bg);
            iv.setFitWidth(TILE_W * 1.4 - 4);
            iv.setFitHeight(TILE_H - 4);
            iv.setPreserveRatio(true);
            iv.getStyleClass().add("turn-order-image");
            root.getChildren().add(iv);
        }

        HBox positions = new HBox(2);
        positions.setAlignment(Pos.CENTER);
        positions.setPadding(new Insets(4));

        for (TurnOrderTileSnapshot.PositionSnap p : snap.positions()) {
            positions.getChildren().add(buildTurnPosition(p));
        }
        root.getChildren().add(positions);
        return root;
    }

    private static VBox buildTurnPosition(TurnOrderTileSnapshot.PositionSnap p) {
        VBox v = new VBox(2);
        v.getStyleClass().add("turn-position");
        if (p.isLast())               v.getStyleClass().add("last");
        if (p.placedTotem() != null)  v.getStyleClass().add("occupied");
        v.setAlignment(Pos.CENTER);
        v.setPrefWidth(28);

        Label num = new Label(String.valueOf(p.position()));
        num.getStyleClass().add("turn-position-number");
        v.getChildren().add(num);

        if (p.foodBonus() > 0) {
            Label bonus = new Label("+" + p.foodBonus() + "🍖");
            bonus.getStyleClass().add("turn-position-bonus");
            v.getChildren().add(bonus);
        }

        // Totem area: always present so columns line up; empty if free.
        StackPane slot = new StackPane();
        slot.setPrefSize(20, 20);
        if (p.placedTotem() != null) {
            slot.getChildren().add(buildTotemBadge(p.placedTotem(), 18));
        }
        v.getChildren().add(slot);
        return v;
    }

    // ── Deck ──────────────────────────────────────────────────────────────────

    /**
     * Builds the tribe deck visualization: a stack of card backs with a badge
     * showing the number of remaining cards. Uses {@code card_back.png} via
     * the cache; falls back to a styled rectangle if missing.
     */
    public static StackPane buildDeck(int remaining) {
        StackPane deck = new StackPane();
        deck.getStyleClass().add("deck");
        deck.setPrefSize(CARD_W, CARD_H);

        Image back = CardImageCache.cardBack();
        if (back != null) {
            // Two stacked images for a "pile" effect; the offset is purely visual
            ImageView shadow = new ImageView(back);
            shadow.setFitWidth(CARD_W - 8); shadow.setFitHeight(CARD_H - 8);
            shadow.setPreserveRatio(true);
            shadow.setOpacity(0.5);
            shadow.setTranslateX(3); shadow.setTranslateY(3);

            ImageView front = new ImageView(back);
            front.setFitWidth(CARD_W - 8); front.setFitHeight(CARD_H - 8);
            front.setPreserveRatio(true);
            deck.getChildren().addAll(shadow, front);
        } else {
            VBox box = new VBox(4);
            box.setAlignment(Pos.CENTER);
            Label l1 = new Label("MAZZO");      l1.getStyleClass().add("deck-label");
            Label l2 = new Label("Tribù");      l2.getStyleClass().add("deck-label");
            box.getChildren().addAll(l1, l2);
            deck.getChildren().add(box);
        }

        Label badge = new Label(String.valueOf(remaining));
        badge.getStyleClass().add("deck-badge");
        StackPane.setAlignment(badge, Pos.TOP_RIGHT);
        StackPane.setMargin(badge, new Insets(-6, -6, 0, 0));
        deck.getChildren().add(badge);

        Tooltip.install(deck, new Tooltip(remaining + " carte rimaste nel mazzo Tribù"));
        return deck;
    }

    // ── Player + Tribe panel ──────────────────────────────────────────────────

    /**
     * Builds the per-player panel: name + totem + resources, plus the player's
     * tribe (characters grouped by role with counts, then buildings as small
     * thumbnails with tooltips). The whole panel is highlighted when the
     * player is the active one this turn.
     */
    public static VBox buildPlayerPanel(PlayerSnapshot p, boolean active) {
        VBox panel = new VBox(6);
        panel.getStyleClass().add("player-panel");
        if (active) panel.getStyleClass().add("active");
        panel.setAlignment(Pos.TOP_CENTER);
        panel.setPadding(new Insets(8));

        // Header: totem + name + resources
        HBox header = new HBox(8);
        header.setAlignment(Pos.CENTER_LEFT);
        header.getChildren().add(buildTotemBadge(p.totemColour(), 18));
        Label name = new Label(p.name());
        name.getStyleClass().add("player-name");
        header.getChildren().add(name);
        panel.getChildren().add(header);

        HBox stats = new HBox(10);
        stats.setAlignment(Pos.CENTER_LEFT);
        Label food = new Label("🍖 " + p.food());      food.getStyleClass().add("player-food");
        Label pp   = new Label("⭐ " + p.prestigePoints()); pp.getStyleClass().add("player-pp");
        stats.getChildren().addAll(food, pp);
        panel.getChildren().add(stats);

        // Character count by role: e.g. 🏹 ×2  🌾 ×1
        if (!p.tribeCharacters().isEmpty()) {
            HBox chars = new HBox(6);
            chars.setAlignment(Pos.CENTER_LEFT);
            Map<CharacterType, Integer> counts = new HashMap<>();
            for (CardSnapshot c : p.tribeCharacters()) {
                if (c.characterType() == null) continue;
                counts.merge(c.characterType(), 1, Integer::sum);
            }
            for (CharacterType ct : CharacterType.values()) {
                int n = counts.getOrDefault(ct, 0);
                if (n == 0) continue;
                Label l = new Label(roleEmoji(ct) + " ×" + n);
                l.getStyleClass().addAll("char-count", "role-" + ct.name().toLowerCase());
                Tooltip.install(l, new Tooltip(roleNameIt(ct) + ": " + n));
                chars.getChildren().add(l);
            }
            panel.getChildren().add(chars);
        }

        // Building thumbnails
        if (!p.tribeBuildings().isEmpty()) {
            HBox blds = new HBox(4);
            blds.setAlignment(Pos.CENTER_LEFT);
            for (CardSnapshot b : p.tribeBuildings()) {
                blds.getChildren().add(buildCard(b, null, TRIBE_CARD_W, TRIBE_CARD_H));
            }
            panel.getChildren().add(blds);
        }

        return panel;
    }

    private static String roleEmoji(CharacterType t) {
        return switch (t) {
            case HUNTER   -> "🏹";
            case GATHERER -> "🌾";
            case BUILDER  -> "🔨";
            case SHAMAN   -> "🌀";
            case INVENTOR -> "💡";
            case ARTIST   -> "🎨";
        };
    }

    private static String roleNameIt(CharacterType t) {
        return switch (t) {
            case HUNTER   -> "Cacciatore";
            case GATHERER -> "Raccoglitore";
            case BUILDER  -> "Costruttore";
            case SHAMAN   -> "Sciamano";
            case INVENTOR -> "Inventore";
            case ARTIST   -> "Artista";
        };
    }
}
