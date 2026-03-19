package it.polimi.it.Player;

import it.polimi.it.cards.Building;
import it.polimi.it.cards.character.*;
import it.polimi.it.cards.character.Character;
import it.polimi.it.enums.CharacterType;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static it.polimi.it.enums.CharacterType.SHAMAN;

public class Tribe {
    private final List<Character> characters;
    private final List<Building> buildings;
    private final Player player;

    public Tribe(Player player){
        this.player = player;
        this.characters= new ArrayList<>();
        this.buildings= new ArrayList<>();
    }

    public void addCharacter(Character p) {
        if (p != null) {
            characters.add(p); //aggiunge personaggio
        }
    }

    public void addBuilding( Building b) {
        if (b != null) {
            buildings.add(b);  //aggiunge edificio
        }
    }

    public int countType(CharacterType type){    //conta personaggi di un certo tipo
        int count=0;
        for (Character c: characters){
            if(c.getType() == type){
                count++;
            }
        }
        return count;
    }

    /* Somma tutte le icone sciamano presenti sulle carte Shaman.
     */
    public int getTotalShamanIcons() {
        int total = 0;
        for (Character c : characters) {
            if (c instanceof Shaman) {
                total += ((Shaman) c).getShamanIcons();
            }
        }
        return total;
    }

    /**
     * Conta quante icone invenzione UNICHE ha il giocatore (per il calcolo finale).
     */
    public int getUniqueInventorIconsCount() {
        Set<Integer> uniqueIcons = new HashSet<>();
        for (Character c : characters) {
            if (c instanceof Inventor) {
                uniqueIcons.add(((Inventor) c).getInventionIconId());
            }
        }
        return uniqueIcons.size();
    }


    /**
     * Calcola lo sconto totale per la costruzione di edifici (fornito dai Builder).
     */
    public int getBuildingDiscount() {
        int totalDiscount = 0;
        for (Character c : characters) {
            if (c instanceof Builder) {
                totalDiscount += ((Builder) c).getBuildingsDiscount();
            }
        }
        return totalDiscount;
    }

    /**
     * Calcola lo sconto totale di cibo (fornito dai Gatherer) per l'evento Sustenance.
     */
    public int getSustenanceDiscount() {
        int total = 0;
        for (Character c : characters) {
            if (c instanceof Gatherer) {
                total += ((Gatherer) c).getSustenanceDiscount();
            }
        }
        return total;
    }

    /**
     * Somma i punti vittoria base forniti dagli edifici costruiti.
     */
    public int getTotalBuildingPoints() {
        int points = 0;
        for (Building b : buildings) {
            points += b.getFinalPoints();
        }
        return points;
    }

    public int ppBuilders() {
        int points = 0;
        for (Character c : characters) {
            // Controlla che il tipo sia quello corretto (Builder)
            if (c.getType() == CharacterType.BUILDER) {
                points += c.getBasePrestige(); // Usiamo il nome corretto del metodo in Character
            }
        }
        return points;
    }

    public int ppInventors() {
        int points = 0;
        for (Character c : characters) {
            if (c.getType() == CharacterType.INVENTOR) {
                points += c.getBasePrestige();
            }
        }
        return points;
    }

    public int ppArtists() {
        int points = 0;
        for (Character c : characters) {
            if (c.getType() == CharacterType.ARTIST) {
                points += c.getBasePrestige();
            }
        }
        return points;
    }

    public int ppBuildings() {
        int points = 0;
        for (Building b : buildings) {
            points += b.getFinalPoints(); // Assicurati che Building abbia getFinalPoints()
        }
        return points;
    }

    public List<Character> getCharacters() {
        return characters;
    }

    public List<Building> getBuildings() {
        return buildings;
    }

    public Player getPlayer() {
        return player;
    }
}



