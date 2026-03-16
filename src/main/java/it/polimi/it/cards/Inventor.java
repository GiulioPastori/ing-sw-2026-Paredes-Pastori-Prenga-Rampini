package mesos.characters;

import it.polimi.it.enums.CharacterType;

public class Inventor extends Character {
    private String inventionIcon;

    public Inventor(String nome, int era, String icona) {
        super(nome, era, CharacterType.INVENTORS);
        this.inventionIcon = icon;
    }

    public String getinventionIcon() {
        return inventionIcon;
    }
}

