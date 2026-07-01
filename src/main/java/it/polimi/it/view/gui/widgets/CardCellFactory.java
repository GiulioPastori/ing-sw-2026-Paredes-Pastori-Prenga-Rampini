package it.polimi.it.view.gui.widgets;

import it.polimi.it.enums.CharacterType;
import it.polimi.it.enums.Era;
import it.polimi.it.enums.TotemColour;
import it.polimi.it.observer.CardSnapshot;
import it.polimi.it.observer.OfferTileSnapshot;
import it.polimi.it.observer.PlayerSnapshot;
import it.polimi.it.observer.TurnOrderTileSnapshot;
import it.polimi.it.view.CardLocalization;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.Button;
import javafx.scene.control.OverrunStyle;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.TextAlignment;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
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
 * mouse handler. Tile cells additionally apply {@code .placed} when occupied.
 */
public final class CardCellFactory {

    private static final double CARD_W = 110;
    private static final double CARD_H = 148;
    private static final double TILE_W = 110;
    private static final double TILE_H = 148;
    private static final double TRIBE_THUMB_W = 34;
    private static final double TRIBE_THUMB_H = 48;
    private static final double TRIBE_SLOT_W = 38;
    private static final double TRIBE_SLOT_H = 54;
    private static final double TRIBE_ZOOM_CARD_W = 70;
    private static final double TRIBE_ZOOM_CARD_H = 96;
    private static final CharacterType[] TRIBE_CHARACTER_ORDER = {
            CharacterType.HUNTER,
            CharacterType.GATHERER,
            CharacterType.SHAMAN,
            CharacterType.BUILDER,
            CharacterType.INVENTOR,
            CharacterType.ARTIST
    };

    /** Turn Order Tile node size, roughly 0.65 of the source image ratio. */
    private static final double TURN_TILE_W = 110;
    private static final double TURN_TILE_H = 165;

    /**
     * Y-fractions of the boxes inside each player-count tile image.
     * Indexed by {@code numPlayers - 2}: index 0 = 2-player tile, ...,
     * index 3 = 5-player tile. Inner array is the list of box centres
     * (one per position) as fractions of the image height.
     *
     * <p>Measured from the actual artwork after trimming the ~10 px white
     * margins; the variation across re-rasterisations is &lt; 0.005 so these
     * constants are stable.
     */
    private static final double[][] TURN_BOX_Y_FRACTIONS = {
            { 0.306, 0.478 },                            // 2 players
            { 0.261, 0.435, 0.608 },                     // 3 players
            { 0.218, 0.390, 0.562, 0.731 },              // 4 players
            { 0.151, 0.325, 0.498, 0.670, 0.841 },       // 5 players
    };

    /** All boxes are horizontally centred on the tile artwork. */
    private static final double TURN_BOX_X_FRACTION = 0.50;

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

        // Tooltip only; no overlay strip on the card image.
        attachTooltip(cell, card);

        if (onClick != null && !card.isEvent()) {
            cell.getStyleClass().add("selectable");
            final int id = card.id();
            cell.setOnMouseClicked(e -> onClick.accept(id));
        }
        // Do not add "disabled" only because onClick is null: this may be a
        // read-only card in a tribe popup or event overlay. The "disabled"
        // style is added explicitly where needed, such as board rows.
        return cell;
    }

    /**
     * Builds a card cell at default size, explicitly marking it as disabled
     * (greyed out) when it cannot be selected. Used for board display where
     * non-selectable cards should appear dimmed.
     */
    public static StackPane buildCard(CardSnapshot card, Consumer<Integer> onClick,
                                      boolean markDisabledIfNoClick) {
        StackPane cell = buildCard(card, onClick, CARD_W, CARD_H);
        if (markDisabledIfNoClick && onClick == null && !card.isEvent()) {
            cell.getStyleClass().add("disabled");
        }
        return cell;
    }

    /**
     * Builds the icon strip shown at the bottom of a character card. Returns
     * null if there's nothing useful to display (Gatherer / Artist have no
     * distinguishing data beyond their type).
     *
     * <p>Glyphs intentionally use only common Unicode characters (★, dot, plus
     * sign) plus short ASCII labels, so they render reliably in the absence
     * of an emoji-capable font.
     */
    private static HBox buildCharacterIconStrip(CardSnapshot card) {
        if (card.details() == null || card.characterType() == null) return null;
        CardSnapshot.CharacterDetails d = card.details();

        HBox strip = new HBox(3);
        strip.getStyleClass().add("char-icon-strip");
        strip.setAlignment(Pos.CENTER);

        switch (card.characterType()) {
            case HUNTER -> {
                if (d.triggerIcon()) strip.getChildren().add(badge("◎", "char-trigger"));
                else                 return null;
            }
            case SHAMAN -> {
                strip.getChildren().add(badge(
                        "★".repeat(Math.max(1, d.starIcons())),
                        "char-stars"));
            }
            case INVENTOR -> {
                if (d.inventionIcon().isEmpty()) return null;
                strip.getChildren().add(badge("✦ " + d.inventionIcon(), "char-invention"));
            }
            case BUILDER -> {
                if (d.buildingDiscount() != 0)
                    strip.getChildren().add(badge("⚒ -" + d.buildingDiscount() + " ◆",
                            "char-discount"));
                if (d.builderPoints() != 0)
                    strip.getChildren().add(badge("★ +" + d.builderPoints(),
                            "char-pp"));
                if (strip.getChildren().isEmpty()) return null;
            }
            case GATHERER, ARTIST -> { return null; }
        }
        return strip;
    }

    /** Small text badge with a CSS class for colouring. Marked as emoji-text
     *  so it inherits the bundled emoji font for the glyph fallback. */
    private static Label badge(String text, String styleClass) {
        Label l = new Label(text);
        l.getStyleClass().addAll("char-icon-badge", "emoji-text", styleClass);
        return l;
    }

    private static void attachTooltip(StackPane cell, CardSnapshot card) {
        String text = switch (card.type()) {
            case BUILDING  -> "Edificio #" + card.id()
                    + "\nCosto: " + card.foodCost() + " ◆   PP: " + card.finalPoints()
                    + "\n" + CardLocalization.buildingDescription(card.id(), card.effectDescription());
            case CHARACTER -> characterTooltip(card);
            case EVENT     -> "Evento  (id " + card.id() + ", " + card.era() + ")"
                    + "\n" + card.effectDescription();
        };
        Tooltip tip = new Tooltip(text);
        tip.getStyleClass().add("emoji-text");
        tip.setShowDelay(Duration.millis(250));
        Tooltip.install(cell, tip);
    }

    /** Tooltip text for a character card, including its distinguishing data. */
    private static String characterTooltip(CardSnapshot c) {
        String header = (c.characterType() != null
                ? roleEmoji(c.characterType()) + " " + roleNameIt(c.characterType())
                : "Personaggio")
                + "  (id " + c.id() + ", " + c.era() + ")";
        if (c.details() == null || c.characterType() == null) return header;

        CardSnapshot.CharacterDetails d = c.details();
        String tail = switch (c.characterType()) {
            case HUNTER   -> d.triggerIcon()
                    ? "\n🎯 Icona Trigger: SI (caccia immediata al pescaggio)"
                    : "\nIcona Trigger: no";
            case SHAMAN   -> "\n" + "★".repeat(d.starIcons())
                    + "  (" + d.starIcons() + " icone stella)";
            case INVENTOR -> d.inventionIcon().isEmpty()
                    ? ""
                    : "\n💡 Invenzione: " + d.inventionIcon();
            case BUILDER  -> {
                StringBuilder sb = new StringBuilder();
                if (d.buildingDiscount() != 0)
                    sb.append("\n⚒ Sconto edifici: -").append(d.buildingDiscount()).append(" ◆");
                if (d.builderPoints() != 0)
                    sb.append("\n★ Fine partita: +").append(d.builderPoints()).append(" PP");
                yield sb.toString();
            }
            case GATHERER -> "\n◈ Sostentamento: -3 ◆ per ogni Raccoglitore";
            case ARTIST   -> "\n🎨 Fine partita: 10 PP per ogni coppia di Artisti";
        };
        return header + tail;
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

        Rectangle outline = new Rectangle(TILE_W - 4, TILE_H - 4);
        outline.setArcWidth(8);
        outline.setArcHeight(8);
        outline.setMouseTransparent(true);
        outline.getStyleClass().add("tile-outline");
        cell.getChildren().add(outline);

        // Letter badge in the top-left so it stays visible over the image
        Label letterBadge = new Label(String.valueOf(tile.letter()));
        letterBadge.getStyleClass().add("tile-letter-badge");
        StackPane.setAlignment(letterBadge, Pos.TOP_LEFT);
        StackPane.setMargin(letterBadge, new Insets(4, 0, 0, 4));
        cell.getChildren().add(letterBadge);

        // Totem overlay if occupied: small totem fitted inside the top-center
        // white box on the tile artwork.
        if (!tile.isFree() && tile.placedTotem() != null) {
            double totemSize = TILE_H * 0.22;   // ~24 px -- fits the small box
            Node totem = buildTotemImage(tile.placedTotem(), totemSize);
            StackPane.setAlignment(totem, Pos.TOP_CENTER);
            StackPane.setMargin(totem, new Insets(6, 0, 0, 0));
            cell.getChildren().add(totem);
        }

        if (onClick != null) {
            cell.getStyleClass().add("selectable");
            final char l = tile.letter();
            cell.setOnMouseClicked(e -> onClick.accept(l));
        }
        return cell;
    }

    /**
     * Builds a coloured totem badge (filled circle with thin border) of the
     * requested diameter. Used as a small corner indicator on offer tiles and
     * inside player panels where space is tight.
     */
    public static StackPane buildTotemBadge(TotemColour colour, double diameter) {
        Circle c = new Circle(diameter / 2.0);
        c.getStyleClass().addAll("totem-badge", totemCssClass(colour));
        StackPane wrap = new StackPane(c);
        wrap.setPrefSize(diameter, diameter);
        wrap.setMaxSize(diameter, diameter);
        return wrap;
    }

    /**
     * Builds a totem visual using the carved-stone PNG asset, scaled to the
     * given height. Returns an {@link ImageView} when the asset is available;
     * falls back to {@link #buildTotemBadge(TotemColour, double)} (a coloured
     * circle of comparable size) when the asset is missing.
     *
     * @param colour the totem colour
     * @param height target on-screen height in pixels
     */
    public static Node buildTotemImage(TotemColour colour, double height) {
        Image img = TotemImageCache.imageFor(colour);
        if (img == null) {
            // Fallback: large coloured dot, ~half the requested height
            return buildTotemBadge(colour, height * 0.55);
        }
        ImageView iv = new ImageView(img);
        iv.setFitHeight(height);
        iv.setPreserveRatio(true);
        iv.setSmooth(true);
        iv.getStyleClass().addAll("totem-image", totemCssClass(colour));
        return iv;
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
     * Builds the Turn Order Tile as a vertical card with the player-count
     * artwork as background and totems positioned at the centre of each
     * physical box on the artwork.
     *
     * <p>Box centres are pre-measured per player count (see
     * {@link #TURN_BOX_Y_FRACTIONS}); the image is rendered at fixed
     * {@link #TURN_TILE_W}×{@link #TURN_TILE_H} so layout is deterministic.
     *
     * <p>If the artwork is missing the method falls back to a procedural
     * vertical strip of position cells with the same {@code numPlayers}
     * positions, food bonuses and totems.
     */
    public static StackPane buildTurnOrderTile(TurnOrderTileSnapshot snap) {
        StackPane root = new StackPane();
        root.getStyleClass().addAll("turn-order-tile",
                "turn-order-" + (snap == null ? "?" : snap.numPlayers()));
        root.setPrefSize(TURN_TILE_W, TURN_TILE_H);
        root.setMinSize(TURN_TILE_W, TURN_TILE_H);

        if (snap == null) return root;

        Image bg = TileImageCache.turnOrderTileFor(snap.numPlayers());
        if (bg != null) {
            ImageView iv = new ImageView(bg);
            iv.setFitWidth(TURN_TILE_W);
            iv.setFitHeight(TURN_TILE_H);
            iv.setPreserveRatio(true);
            iv.getStyleClass().add("turn-order-image");
            root.getChildren().add(iv);

            // Overlay totems on top of the artwork at the precomputed centres.
            // The Pane is unmanaged-positioned: each totem is placed via
            // translateX/Y relative to the StackPane's centre.
            int idx = snap.numPlayers() - 2;
            if (idx >= 0 && idx < TURN_BOX_Y_FRACTIONS.length) {
                double[] yFracs = TURN_BOX_Y_FRACTIONS[idx];
                List<TurnOrderTileSnapshot.PositionSnap> positions = snap.positions();
                for (int i = 0; i < Math.min(yFracs.length, positions.size()); i++) {
                    var p = positions.get(i);
                    if (p.placedTotem() == null) continue;
                    Node totem = buildTotemImage(p.placedTotem(), TURN_TILE_H * 0.18);
                    // StackPane.setAlignment + translate puts the node where
                    // we want relative to the centre of the parent.
                    StackPane.setAlignment(totem, Pos.TOP_CENTER);
                    // y in pixels from the top of the parent
                    double y = yFracs[i] * TURN_TILE_H - (TURN_TILE_H * 0.09);
                    totem.setTranslateY(y);
                    // Centre x: use TOP_CENTER alignment so x is already
                    // centred; translateX 0 keeps it on the box centre.
                    totem.setTranslateX(0);
                    root.getChildren().add(totem);
                }
            }
        } else {
            // Fallback: procedural vertical strip with text labels and
            // coloured-circle totems, no image.
            VBox positions = new VBox(2);
            positions.setAlignment(Pos.CENTER);
            positions.setPadding(new Insets(4));
            for (TurnOrderTileSnapshot.PositionSnap p : snap.positions()) {
                positions.getChildren().add(buildTurnPosition(p));
            }
            root.getChildren().add(positions);
        }
        return root;
    }

    /**
     * Procedural fallback for a single turn-order position: number, food
     * bonus, and totem slot. Used only when the artwork PNG is missing.
     */
    private static HBox buildTurnPosition(TurnOrderTileSnapshot.PositionSnap p) {
        HBox row = new HBox(4);
        row.getStyleClass().add("turn-position");
        if (p.isLast())               row.getStyleClass().add("last");
        if (p.placedTotem() != null)  row.getStyleClass().add("occupied");
        row.setAlignment(Pos.CENTER);

        Label num = new Label(String.valueOf(p.position()));
        num.getStyleClass().add("turn-position-number");
        row.getChildren().add(num);

        if (p.foodBonus() > 0) {
            Label bonus = new Label("+" + p.foodBonus() + "🍖");
            bonus.getStyleClass().addAll("turn-position-bonus", "emoji-text");
            row.getChildren().add(bonus);
        }
        if (p.placedTotem() != null) {
            row.getChildren().add(buildTotemBadge(p.placedTotem(), 14));
        }
        return row;
    }

    // ── Deck ──────────────────────────────────────────────────────────────────

    /**
     * Builds the tribe deck visualization: a stack of card backs with a badge
     * showing the number of remaining cards. Uses {@code card_back.png} via
     * the cache; falls back to a styled rectangle if missing.
     */
    /** Overload without era — uses generic card back. */
    public static StackPane buildDeck(int remaining) {
        return buildDeck(remaining, null);
    }

    public static StackPane buildDeck(int remaining, Era era) {
        StackPane deck = new StackPane();
        deck.getStyleClass().add("deck");
        deck.setPrefSize(CARD_W, CARD_H);

        Image back = (era != null) ? CardImageCache.cardBackForEra(era) : CardImageCache.cardBack();
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
     * Builds the player panel shown at the bottom of the board.
     * Includes name, totem, resources, and a compact tribe summary.
     *
     * @param p        the player snapshot
     * @param active   true if this player is the current active player
     * @param isMe     true if this panel belongs to the local player
     * @param onViewTribe  callback invoked when the user clicks "view tribe";
     *                     receives the PlayerSnapshot. May be null (button hidden).
     */
    public static VBox buildPlayerPanel(PlayerSnapshot p, boolean active,
                                        boolean isMe,
                                        Consumer<PlayerSnapshot> onViewTribe) {
        VBox panel = new VBox(4);
        panel.getStyleClass().add("player-panel");
        if (active) panel.getStyleClass().add("active");
        if (isMe)   panel.getStyleClass().add("mine");
        panel.setAlignment(Pos.TOP_CENTER);
        panel.setPadding(new Insets(6, 8, 6, 8));

        // ── Header: totem + name + (eye button for other players) ────────────
        HBox header = new HBox(8);
        header.setAlignment(Pos.CENTER_LEFT);
        header.getChildren().add(buildTotemBadge(p.totemColour(), 18));
        Label name = new Label(p.name());
        name.getStyleClass().add("player-name");
        header.getChildren().add(name);

        if (onViewTribe != null) {
            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);
            Button eyeBtn = new Button("👁");
            eyeBtn.getStyleClass().add("tribe-eye-btn");
            eyeBtn.setOnAction(e -> onViewTribe.accept(p));
            Tooltip.install(eyeBtn, new Tooltip("Vedi tribù di " + p.name()));
            header.getChildren().addAll(spacer, eyeBtn);
        }
        panel.getChildren().add(header);

        // ── Stats ─────────────────────────────────────────────────────────────
        HBox stats = new HBox(6);
        stats.setAlignment(Pos.CENTER_LEFT);
        Label food = new Label("🍖 " + p.food());
        food.getStyleClass().addAll("player-food", "emoji-text");
        Label pp = new Label("⭐ " + p.prestigePoints());
        pp.getStyleClass().addAll("player-pp", "emoji-text");
        stats.getChildren().addAll(food, statSeparator(), pp);

        String aggregate = buildCharacterAggregateText(p, isMe);
        if (!aggregate.isEmpty()) {
            Label aggregateLabel = new Label(aggregate);
            aggregateLabel.getStyleClass().addAll("player-aggregate", "emoji-text");
            aggregateLabel.setTextOverrun(OverrunStyle.ELLIPSIS);
            aggregateLabel.setMinWidth(0);
            aggregateLabel.setPrefWidth(132);
            aggregateLabel.setMaxWidth(132);
            Tooltip.install(aggregateLabel, new Tooltip(buildCharacterAggregateTooltip(p, isMe)));
            stats.getChildren().addAll(statSeparator(), aggregateLabel);
        }
        panel.getChildren().add(stats);

        HBox tribeStacks = buildTribeStackRow(p, isMe);
        if (tribeStacks != null) {
            panel.getChildren().add(tribeStacks);
        }

        return panel;
    }

    private static HBox buildTribeStackRow(PlayerSnapshot p, boolean expandable) {
        boolean hasCards = !p.tribeCharacters().isEmpty() || !p.tribeBuildings().isEmpty();
        if (!hasCards) return null;

        Map<CharacterType, List<CardSnapshot>> byType = new EnumMap<>(CharacterType.class);
        for (CharacterType type : TRIBE_CHARACTER_ORDER) {
            byType.put(type, new ArrayList<>());
        }
        for (CardSnapshot card : p.tribeCharacters()) {
            if (card.characterType() != null) {
                byType.computeIfAbsent(card.characterType(), ignored -> new ArrayList<>()).add(card);
            }
        }

        HBox row = new HBox(4);
        row.getStyleClass().add("tribe-stack-row");
        row.setAlignment(Pos.CENTER_LEFT);
        for (CharacterType type : TRIBE_CHARACTER_ORDER) {
            row.getChildren().add(buildTribeStackSlot(
                    roleEmoji(type), roleNameIt(type), byType.getOrDefault(type, List.of()), expandable));
        }
        row.getChildren().add(buildTribeStackSlot("🏛", "Edifici", p.tribeBuildings(), expandable));
        return row;
    }

    private static StackPane buildTribeStackSlot(String label, String title,
                                                List<CardSnapshot> cards,
                                                boolean expandable) {
        StackPane slot = new StackPane();
        slot.getStyleClass().add("tribe-stack-slot");
        boolean canExpand = expandable && !cards.isEmpty();
        slot.getStyleClass().add(canExpand ? "mine" : "readonly");
        slot.setMinSize(TRIBE_SLOT_W, TRIBE_SLOT_H);
        slot.setPrefSize(TRIBE_SLOT_W, TRIBE_SLOT_H);
        slot.setMaxSize(TRIBE_SLOT_W, TRIBE_SLOT_H);

        Label typeLabel = new Label(label);
        typeLabel.getStyleClass().addAll("tribe-stack-label", "emoji-text");
        StackPane.setAlignment(typeLabel, Pos.BOTTOM_CENTER);
        slot.getChildren().add(typeLabel);

        if (cards.isEmpty()) {
            slot.getStyleClass().add("empty");
            Tooltip.install(slot, new Tooltip(title + ": 0"));
            return slot;
        }

        List<StackPane> cardNodes = new ArrayList<>();
        for (CardSnapshot c : cards) {
            StackPane card = buildCard(c, null, TRIBE_THUMB_W, TRIBE_THUMB_H);
            card.getStyleClass().add("tribe-stack-card");
            card.setMouseTransparent(true);
            StackPane.setAlignment(card, Pos.CENTER);
            slot.getChildren().add(card);
            cardNodes.add(card);
        }

        Label count = null;
        if (cards.size() > 1) {
            count = new Label(String.valueOf(cards.size()));
            count.getStyleClass().add("tribe-stack-count");
            StackPane.setAlignment(count, Pos.TOP_RIGHT);
            StackPane.setMargin(count, new Insets(0, 1, 0, 0));
            slot.getChildren().add(count);
        }
        Label finalCount = count;
        layoutCollapsedTribeStack(cardNodes, finalCount);
        Tooltip.install(slot, canExpand
                ? buildTribeStackTooltip(title, cards)
                : new Tooltip(title + ": " + cards.size()));
        if (canExpand) {
            slot.setOnMouseEntered(e -> {
                if (finalCount != null) finalCount.setVisible(false);
            });
            slot.setOnMouseExited(e -> {
                if (finalCount != null) finalCount.setVisible(true);
            });
        }
        return slot;
    }

    private static void layoutCollapsedTribeStack(List<StackPane> cards, Label count) {
        if (cards.isEmpty()) return;
        int firstVisible = Math.max(0, cards.size() - 4);
        int visibleIndex = 0;
        for (int i = 0; i < cards.size(); i++) {
            StackPane card = cards.get(i);
            boolean visible = i >= firstVisible;
            card.setVisible(visible);
            card.setScaleX(1.0);
            card.setScaleY(1.0);
            if (visible) {
                card.setTranslateX(visibleIndex * 1.6);
                card.setTranslateY(-visibleIndex * 1.6 - 2.0);
                visibleIndex++;
            }
        }
        if (count != null) {
            count.setVisible(true);
            count.toFront();
        }
    }

    private static Tooltip buildTribeStackTooltip(String title, List<CardSnapshot> cards) {
        Tooltip tip = new Tooltip(title + ": " + cards.size());
        tip.getStyleClass().add("emoji-text");
        tip.setShowDelay(Duration.millis(120));
        tip.setHideDelay(Duration.millis(80));
        tip.setGraphic(buildTribeStackPreview(cards));
        return tip;
    }

    private static Pane buildTribeStackPreview(List<CardSnapshot> cards) {
        int n = cards.size();
        double spacing = n <= 1 ? 0.0 : Math.min(48.0, Math.max(18.0, 340.0 / (n - 1)));
        double width = TRIBE_ZOOM_CARD_W + Math.max(0, n - 1) * spacing;

        Pane pane = new Pane();
        pane.getStyleClass().add("tribe-stack-preview");
        pane.setMinSize(width, TRIBE_ZOOM_CARD_H);
        pane.setPrefSize(width, TRIBE_ZOOM_CARD_H);
        pane.setMaxSize(width, TRIBE_ZOOM_CARD_H);

        for (int i = 0; i < cards.size(); i++) {
            StackPane card = buildCard(cards.get(i), null, TRIBE_ZOOM_CARD_W, TRIBE_ZOOM_CARD_H);
            card.getStyleClass().add("tribe-tooltip-card");
            card.setMouseTransparent(true);
            card.setLayoutX(i * spacing);
            card.setLayoutY(0);
            pane.getChildren().add(card);
        }
        return pane;
    }

    /** Overload for backward compat — no tribe viewer, not marked as mine. */
    public static VBox buildPlayerPanel(PlayerSnapshot p, boolean active) {
        return buildPlayerPanel(p, active, false, null);
    }

    /**
     * Computes the compact aggregate visible in the player panel.
     * Shaman icons are public; building discount is shown only to its owner.
     */
    private static String buildCharacterAggregateText(PlayerSnapshot p, boolean isMe) {
        int shamanIcons = 0;
        int  builderDiscount = 0;

        for (CardSnapshot c : p.tribeCharacters()) {
            if (c.details() == null) continue;
            shamanIcons += c.details().starIcons();
            if (isMe) builderDiscount += c.details().buildingDiscount();
        }

        List<String> parts = new ArrayList<>();
        if (shamanIcons > 0)      parts.add("🌀 " + shamanIcons);
        if (builderDiscount > 0)  parts.add("🔨 -" + builderDiscount);
        return String.join(" | ", parts);
    }

    private static String buildCharacterAggregateTooltip(PlayerSnapshot p, boolean isMe) {
        int shamanIcons = 0;
        int builderDiscount = 0;

        for (CardSnapshot c : p.tribeCharacters()) {
            if (c.details() == null) continue;
            shamanIcons += c.details().starIcons();
            if (isMe) builderDiscount += c.details().buildingDiscount();
        }

        List<String> parts = new ArrayList<>();
        if (shamanIcons > 0) parts.add("Icone sciamano: " + shamanIcons);
        if (builderDiscount > 0) parts.add("Sconto edifici: -" + builderDiscount);
        return String.join(" | ", parts);
    }

    private static Label statSeparator() {
        Label separator = new Label("|");
        separator.getStyleClass().add("player-stat-separator");
        return separator;
    }

    /** Role emoji (uses the bundled emoji font when applied to .emoji-text labels). */
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
