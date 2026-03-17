package it.polimi.it.Player;

import it.polimi.it.cards.Building;
import it.polimi.it.enums.CharacterType;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Tribe {
    private List<Character> characters;
    private List<Building> buildings;

    public Tribe(){
        this.characters= new ArrayList<>();
        this.buildings= new ArrayList<>();
    }

    public void addCharacter(CharacterType p){
        characters.add(p); //aggiunge personaggio
    }

    public void addBuilding( Building e){
        buildings.add(e);  //aggiunge edificio
    }

    public int countType(CharacterType type){    //conta personaggi di un certo tipo
        int count=0;
        for (Character c: characters){
            if(c.getTypeOfCharacter() == type){
                count++;
            }
        }
        return count;
    }

    public int shamanIcons(){  //conta icone shaman
        int total=0;
        for(Character c : characters){
            if (c.CharacterType == "SHAMAN")
            total += c.shamanIcons();
        }
        return total;
    }

    public int uniqueInventorIcon(){   //conta le icone inventore uniche
        Set<String> icons = new HashSet<>();
        for (Character c : characters){
            icons.addAll(c.getIventorActions());
        }
        return icons.size();
    }

    public int compBuildingDiscount(){
        int discount=0;
        for (Character c: characters){
            discount+=c.getConstructionDiscount();
        }
        return discount;
    }

    public int ppBuilders(){
        int points=0;
        for( Character c : characters){
            if(c.getType() == CharacterType.BUILDERS)
                points+= c.getPoints();
        }
        return points;
    }

    public int ppInventors(){
        int points=0;
        for (Character c : characters){
            if(c.getType() == CharacterType.INVENTORS){
                points+=c.getPoints();
            }

        }
        return points;
    }

    public int ppArtists(){
        int points=0;
        for(Character c : characters){
            if(c.getType()== CharacterType.ARTISTS){
                points += c.getPoints();
            }
        }
        return points;
    }

    public int ppBuildings(){
        int points= 0;
        for (Building b : buildings){
            points+=b.getPoints();
        }
        return points;
    }

    public void addInventionIcon(int inventionIcon) {
    }

    public void addShamanIcons(int shamanIcons) {
    }

    public void addHuntBonus(int huntBonus) {

    }

    public void addCavePaintingScore(int cavePaintingScores) {
    }

    public void addEndGameSetBonus(int endGameSetBonus) {
    }
}
