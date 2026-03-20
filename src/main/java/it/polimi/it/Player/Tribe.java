package it.polimi.it.Player;

import it.polimi.it.cards.building.Building;
import it.polimi.it.cards.building.BuildingStrategy;
import it.polimi.it.cards.building.ExtraShamanIconsStrategy;
import it.polimi.it.cards.building.SustenanceDiscountStrategy;
import it.polimi.it.cards.character.*;
import it.polimi.it.cards.character.Character;
import it.polimi.it.enums.CharacterType;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Tribe {
    private final List<Character> characters;
    private final List<Building> buildings;
    private final Player player;

    public Tribe(Player player) {
        this.player = player;
        this.characters = new ArrayList<>();
        this.buildings = new ArrayList<>();
    }


    /* Conta quanti tipi diversi di personaggi sono presenti nella tribù.
            */
    public int huniqueinventoricon() {
        Set<CharacterType> distinctTypes = new HashSet<>();
        for (Character c : characters) {
            distinctTypes.add(c.getType());
        }
        return distinctTypes.size();
    }

    /**
     * Verifica se esiste già un inventore con la stessa icona invenzione.
     * Serve per l'edificio che regala cibo per le coppie uguali. [cite: 73, 87, 90]
     */
    public boolean hasDuplicateInventorIcon(Character newCharacter) {
        if (!(newCharacter instanceof Inventor)) return false;

        int newIconId = ((Inventor) newCharacter).getInventionIconId();

        // Controlla se tra i personaggi già presenti c'è un altro inventore con la stessa icona
        for (Character existing : characters) {
            if (existing instanceof Inventor && existing != newCharacter) {
                if (((Inventor) existing).getInventionIconId() == newIconId) {
                    return true;
                }
            }
        }
        return false;
    }

    public int getUniqueCharactersCount() {
        // Usiamo un Set perché per definizione non accetta duplicati
        // Inseriamo i tipi dei personaggi posseduti: il risultato sarà il numero di tipi unici
        java.util.Set<it.polimi.it.enums.CharacterType> uniqueTypes = new java.util.HashSet<>();

        for (it.polimi.it.cards.character.Character c : characters) {
            uniqueTypes.add(c.getType());
        }

        return uniqueTypes.size();
    }
    /**
     * Calcola quanti set completi da 6 personaggi diversi ha il giocatore.
     * Fondamentale per gli edifici dell'Era III. [cite: 62, 66]
     */
    public int countFullSets() {
        if (characters.isEmpty()) return 0;

        // Per ogni tipo, contiamo quante carte abbiamo
        int[] counts = new int[CharacterType.values().length];
        for (Character c : characters) {
            counts[c.getType().ordinal()]++;
        }

        // Il numero di set completi è limitato dal tipo di cui abbiamo meno copie
        int min = Integer.MAX_VALUE;
        for (int count : counts) {
            if (count < min) min = count;
        }
        return min;
    }


    public void addCharacter(Character p) {
        if (p != null) {
            characters.add(p); //aggiunge personaggio

            for (Building b : buildings) {
                b.onNewCharacter(player, p);
            }
        }
    }

    public void addBuilding(Building b) {
        if (b != null) {
            buildings.add(b);//aggiunge edificio


        }
    }

    public int countType(CharacterType type) {    //conta personaggi di un certo tipo
        int count = 0;
        for (Character c : characters) {
            if (c.getType() == type) {
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
        // 2. Somma icone bonus dagli edifici (ExtraShamanIconsStrategy)
        for (Building b : buildings) {
            for (BuildingStrategy s : b.getStrategies()) {
                if (s instanceof ExtraShamanIconsStrategy) {
                    total += ((ExtraShamanIconsStrategy) s).getExtraIcons();
                }
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
     * Calcola il costo totale del sostentamento (1 cibo per personaggio)
     * applicando gli sconti dei Gatherer e degli Edifici (SustenanceDiscountStrategy).
     */
    public int getSustenanceCost() {
        // 1. Costo base: ogni personaggio consuma 1 cibo
        int baseRequirement = characters.size();

        // 2. Sconto base dai Gatherer
        int totalDiscount = getSustenanceDiscount();

        // 3. Sconto extra dagli Edifici
        for (Building b : buildings) {
            for (BuildingStrategy s : b.getStrategies()) {
                // Se la strategia è quella dello sconto cibo
                if (s instanceof SustenanceDiscountStrategy ads) {
                    // Usiamo i tuoi metodi: getType() e getDiscountPerCard()
                    int countForType = countType(ads.getType());
                    totalDiscount += (countForType * ads.getDiscountPerCard());
                }
            }
        }

        return Math.max(0, baseRequirement - totalDiscount);
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



