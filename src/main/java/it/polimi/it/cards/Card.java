package it.polimi.it.cards;

import it.polimi.it.Board;
import it.polimi.it.Player.Player;
import it.polimi.it.enums.Era;

public abstract class Card {
    protected final String name;
    protected final Era era;

    public Card(String name, Era era) {
        this.name = name;
        this.era  = era;
    }

    public String getName() {

        return name;
    }
    public Era getEra()  {

        return era;
    }

    public abstract void applyEffect(Player player, Board board);
}
