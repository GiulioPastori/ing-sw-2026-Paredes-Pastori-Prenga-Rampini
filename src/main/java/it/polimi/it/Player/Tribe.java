package it.polimi.it.Player;

import it.polimi.it.cards.Building;
import it.polimi.it.enums.CharacterType;

import java.util.ArrayList;
import java.util.List;

public class Tribe {
    private List<Character> characters;
    private List<Building> buildings;

    public Tribe(){
        this.characters= new ArrayList<>();
        this.buildings= new ArrayList<>();
    }

    public void addCharacter(Character p){
        characters.add(p); //aggiunge personaggio
    }

    public void addBuilding( Building e){
        buildings.add(e);  //aggiunge edificio
    }

    public int countType(CharacterType type)
}
