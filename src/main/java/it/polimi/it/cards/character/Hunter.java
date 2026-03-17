package it.polimi.it.cards.character;


import it.polimi.it.enums.CharacterType;


public class Hunter extends Character {
    private final boolean hasTriggerIcon;

    public Hunter(String name, Era era, boolean hasTriggerIcon) {
        super(name, era, CharacterType.HUNTER, hasTriggerIcon ? 1 : 0);
        this.hasTriggerIcon  = hasTriggerIcon;
    }

    public boolean hasTriggerIcon()    { return hasTriggerIcon; }

    /**
     * If this Hunter has the trigger icon, the player immediately takes
     * 1 food per Hunter currently in their tribe (including this one).
     */
    @Override
    public void applyEffect(Player player) {
        if (hasTriggerIcon) {
            int hunters = player.getTribe().countTypeCharacter(CharacterType.HUNTER);
            player.addFood(hunters);
        }
    }
}

}
