package it.polimi.it.tiles;

import it.polimi.it.Player.Player;

import java.util.ArrayList;
import java.util.List;

public class OfferTiles {

    private List<OfferTile> card;
    private TurnOrderTile turnOrderTiles;

    public OfferTiles(TurnOrderTile turnOrderTiles) {
        this.card = new ArrayList<>();
        this.turnOrderTiles = turnOrderTiles;
    }

    public void addTile(OfferTile tile) {
        card.add(tile);
    }


    public void placeTotem(Player player, OfferTile tile) {
        if (tile.isFree()) {
            tile.placeTotem(player.getTotem());
        }
    }


    public List<OfferTile> getFreeTile() {
        List<OfferTile> free = new ArrayList<>();

        for (OfferTile tile : card) {
            if (tile.isFree()) {
                free.add(tile);
            }
        }

        return free;
    }

    public void reset() {
        for (OfferTile tile : card) {
            tile.isFree();
        }
        turnOrderTiles.reset();
    }

    public TurnOrderTile getTurnOrderTiles() {
        return turnOrderTiles;
    }
}