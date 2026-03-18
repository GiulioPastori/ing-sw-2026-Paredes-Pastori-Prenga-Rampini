package it.polimi.it.cards.character;


import it.polimi.it.Board;
import it.polimi.it.Player.Player;
import it.polimi.it.cards.Card;
import it.polimi.it.enums.CharacterType;
import it.polimi.it.enums.Era;

public abstract class Character extends Card {
    private CharacterType type;
    private int icon = 0;
    private int basePrestige;

    public Character(String name, Era era, CharacterType type, int i) {
        super(name, era);
        this.type = type;
    }

    public CharacterType getType() {

        return type;
    }

    public int getbasePrestige() {
        return basePrestige;

    }


    /**
     * Effetto quando la carta viene presa
     */
    @Override
    public void applyEffect(Player player, Board board) {
        if (basePrestige > 0) {
            player.addPrestige(basePrestige);
        }
        onAcquire(player, board);
    }

    //restituisce quante icone ha la carta//
    public int getIcon() {

        return this.icon;
    }

    /**
     * Hook per sottoclassi
     */
    protected abstract void onAcquire(Player player, Board board);

    /**
     * Effetto durante eventi (override se serve)
     */
    public void onEvent(Player player, Board board) {
        // default: nessun effetto
    }

    /**
     * Effetto di fine partita (override se serve)
     */
    public int endGamePoints(Player player) {
        return 0;
    }

}

