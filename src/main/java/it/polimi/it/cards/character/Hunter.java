package it.polimi.it.cards.character;


import it.polimi.it.Player.Player;
import it.polimi.it.Player.Tribe;
import it.polimi.it.enums.CharacterType;
import it.polimi.it.enums.Era;

public class Hunter extends Character {
    private int huntBonus;

    public Hunter(String name, Era era, CharacterType type, int icon, int huntBonus) {
        super(name, era, type, icon);
        this.huntBonus = huntBonus;
    }

    public void applyEffect(Player player) {
        if (player != null && player.getTribe() != null) {
            Tribe tribe = player.getTribe();
            tribe.addHuntBonus(this.huntBonus);
            tribe.addCharacter(this);
        }
    }

    public int getHuntBonus() {
        return this.huntBonus;
    }

    public void setHuntBonus(int huntBonus) {
        this.huntBonus = huntBonus;
    }

    @Override
    public CharacterType getTypeOfCharacter() {
        return CharacterType.HUNTER;
    }

    @Override
    public int getIcon() {
        return this.icon;
    }

}
