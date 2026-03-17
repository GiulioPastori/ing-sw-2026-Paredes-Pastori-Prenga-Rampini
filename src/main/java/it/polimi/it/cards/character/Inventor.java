package it.polimi.it.cards.character;

import it.polimi.it.Player.Player;
import it.polimi.it.Player.Tribe;
import it.polimi.it.enums.CharacterType;
import it.polimi.it.enums.Era;


public abstract class Inventor extends Character {
    private int inventionIcon;

    public Inventor(String name, Era era, CharacterType type, int icon, int inventionIcon) {
        super(name, era, type, icon);
        this.inventionIcon = inventionIcon;
    }

    @Override
    public void applyEffect(Player player) {
        if (player != null && player.getTribe() != null) {
            Tribe tribe = player.getTribe();
            tribe.addInventionIcon(this.inventionIcon);
        }
    }

    public int getInventionIcon() {
        return this.inventionIcon;
    }

    public void setInventionIcon(int inventionIcon) {
        this.inventionIcon = inventionIcon;
    }


    @Override
    public int getIcon() {
        return this.getIcon();
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public Era getEra() {
        return this.era;
    }


}