package it.polimi.it.tiles;

import it.polimi.it.player.Player;

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
            tile.occupy();
            turnOrderTiles.placeTotem(Player);
        }
    }

    public void doAction(Player player) {
        for (OfferTile tile : card) {
            if (!tile.isFree()) {
                player.addFood(tile.getFoodReward());
            }
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
            tile.free();
        }
        turnOrderTiles.reset();
    }

    public TurnOrderTile getTurnOrderTiles() {
        return turnOrderTiles;
    }
}