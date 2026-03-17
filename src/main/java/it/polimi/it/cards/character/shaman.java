package it.polimi.it.cards.character;

import it.polimi.it.Player.Player;
import it.polimi.it.Player.Tribe;
import it.polimi.it.enums.CharacterType;
import it.polimi.it.enums.Era;


public abstract class Shaman extends Character {
    private int shamanIcons;

    public Shaman(String name, Era era, CharacterType type, int icon, int shamanIcons) {
        super(name, era, type, icon);
        this.shamanIcons = shamanIcons;
    }

    @Override
    public void applyEffect(Player player) {
        if (player != null && player.getTribe() != null) {
            Tribe tribe = player.getTribe();
            tribe.addShamanIcons(this.shamanIcons);
            tribe.addCharacter(this);
        }
    }

    public int getShamanIcons() {
        return this.shamanIcons;
    }

    public void setShamanIcons(int shamanIcons) {
        this.shamanIcons = shamanIcons;
    }

    @Override
    public java.lang.Character getTypeOfCharacter() {
        return CharacterType.SHAMAN;
    }

    @Override
    public int getIcon() {
        return this.icon;
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