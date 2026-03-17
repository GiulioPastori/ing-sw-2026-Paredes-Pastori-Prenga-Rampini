package it.polimi.it.cards.character;

import it.polimi.it.Player.Player;
import it.polimi.it.Player.Tribe;
import it.polimi.it.enums.CharacterType;
import it.polimi.it.enums.Era;

public class Gatherer extends Character {
    private int sustenanceDiscount;

    public Gatherer(String name, Era era, CharacterType type, int icon, int sustenanceDiscount) {
        super(name, era, type);
        this.sustenanceDiscount = sustenanceDiscount;
    }

    @Override
    public void applyEffect(Player player) {
        if (player != null && player.getTribe() != null) {
            Tribe tribe = player.getTribe();
            tribe.addSustenanceDiscount(this.sustenanceDiscount);
            tribe.addCharacter(this);
        }
    }

    public int getSustenanceDiscount() {

        return this.sustenanceDiscount;
    }

    public void setSustenanceDiscount(int sustenanceDiscount) {

        this.sustenanceDiscount = sustenanceDiscount;
    }

}
