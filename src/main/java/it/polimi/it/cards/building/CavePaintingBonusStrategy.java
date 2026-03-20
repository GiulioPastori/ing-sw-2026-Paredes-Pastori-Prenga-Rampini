package it.polimi.it.cards.building;

import it.polimi.it.Player.Player;
import it.polimi.it.cards.events.CavePaintings;

public class CavePaintingBonusStrategy implements BuildingStrategy {
        @Override
        public void onCavePainting(Player p, CavePaintings e) {
            int artists = p.getTribe().countType(it.polimi.it.enums.CharacterType.ARTIST);
            p.addFood(artists);
        }
}
