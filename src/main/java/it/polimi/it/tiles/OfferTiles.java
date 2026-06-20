package it.polimi.it.tiles;

import it.polimi.it.enums.TotemColour;
import it.polimi.it.player.Player;

import java.util.ArrayList;
import java.util.List;

/** The Offer Track: the ordered sequence of active offer tiles for a given game.
 * During setup, tiles are filtered by player count and ordered by letter (A–G).
    - Track which tiles are free or occupied
    - Return the list of free tiles for totem placement
    - Return the tile occupied by a given totem colour */

public class OfferTiles {

    /** Active tiles for this game, ordered by letter. */
    private final List<OfferTile> tiles;

    public OfferTiles(List<OfferTile> tiles) {
        this.tiles = new ArrayList<>(tiles);
    }

    // Totem placement

    /** Places the player's totem on the chosen tile.
     * player=the player placing their totem
     * tile=the chosen tile (must be free and belong to this track)
     * IllegalArgumentException if the tile does not belong to this track
     * IllegalStateException    if the tile is already occupied
     */
    public void placeTotem(Player player, OfferTile tile) {
        if (!tiles.contains(tile)) {
            throw new IllegalArgumentException(
                    "Tile " + tile.getLetter() + " does not belong to this Offer Track.");
        }
        tile.placeTotem(player.getTotemColour());
    }

    // State queries

    /** Returns the free tiles in track order */
    public List<OfferTile> getFreeTiles() {
        List<OfferTile> free = new ArrayList<>();
        for (OfferTile tile : tiles) {
            if (tile.isFree()) free.add(tile);
        }
        return free;
    }

    /** Returns the tile currently occupied by the given totem colour.
     * totem is the totem colour to look up
     * return the tile occupied by that totem
     *  IllegalStateException if that totem is not placed on any tile */


    public OfferTile getTileByTotem(TotemColour totem) {
        for (OfferTile tile : tiles) {
            if (!tile.isFree() && tile.getPlacedTotem() == totem) return tile;
        }
        throw new IllegalStateException("No tile occupied by totem: " + totem);
    }

    /** Returns all tiles in track order (free and occupied)
     * return unmodifiable list of all tiles in order */
    public List<OfferTile> getAllTiles() { return List.copyOf(tiles); }

    /** Returns true if all tiles are free. */

    public boolean allFree() { return tiles.stream().allMatch(OfferTile::isFree); }
}
