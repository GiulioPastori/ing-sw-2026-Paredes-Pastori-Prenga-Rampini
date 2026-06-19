package it.polimi.it.player;

import it.polimi.it.cards.building.Building;
import it.polimi.it.cards.character.*;
import it.polimi.it.cards.character.Character;
import it.polimi.it.enums.CharacterType;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * The player's tribe is the collection of character and building cards acquired during the game.
 * Exposes counting and scoring methods used by events, buildings, and the
 * final score calculation. Contains no game logic */
public class Tribe {

    private final List<Character> characters;
    private final List<Building>  buildings;

    public Tribe() {
        this.characters = new ArrayList<>();
        this.buildings  = new ArrayList<>();
    }

    // Card addition

    public void addCharacter(Character character) { characters.add(character); }
    public void addBuilding(Building building)    { buildings.add(building); }

    // Character queries

    /** Returns the total number of characters in the tribe. */
    public int getCharacterCount() { return characters.size(); }

    /** Counts the characters of the given type.
     * type=the type to count
     * return number of characters of that type */
    public int countByType(CharacterType type) {
        int count = 0;
        for (Character c : characters) {
            if (c.getType() == type) count++;
        }
        return count;
    }

    /** Returns the total number of star icons across all shamans in the tribe. */
    public int totalShamanIcons() {
        int total = 0;
        for (Character c : characters) {
            if (c instanceof Shaman s) total += s.getStarIcons();
        }
        return total;
    }

    /** Returns the number of distinct invention icons across all inventors */
    public int countUniqueInventorIcons() {
        Set<String> icons = new HashSet<>();
        for (Character c : characters) {
            if (c instanceof Inventor inv) icons.add(inv.getInventionIcon());
        }
        return icons.size();
    }

    /** Returns the total food discount on building acquisition provided by all builders in the tribe.*/
    public int totalBuildingDiscount() {
        int discount = 0;
        for (Character c : characters) {
            if (c instanceof Builder b) discount += b.getBuildingDiscount();
        }
        return discount;
    }

    // End-of-game scoring by character type

    /** Returns the total prestige points contributed by all builders. */
    public int computeBuilderPoints() {
        int points = 0;
        for (Character c : characters) {
            if (c instanceof Builder b) points += b.getEndGamePoints();
        }
        return points;
    }

    /** Returns the total prestige points contributed by all inventors.
     * (inventor count) × (distinct invention icon count).
     */
    public int computeInventorPoints() {
        return countByType(CharacterType.INVENTOR) * countUniqueInventorIcons();
    }

    /** Returns the total prestige points contributed by all artists.
     * 10 PP for every pair of artists (integer division by 2). */
    public int computeArtistPoints() {
        return (countByType(CharacterType.ARTIST) / 2) * 10;
    }

    /** Returns the total prestige points contributed by all buildings */
    public int computeBuildingPoints(Player player) {
        int points = 0;
        for (Building b : buildings) {
            points += b.getFinalPoints();
            points += b.computeEndGameBonus(player);
        }
        return points;
    }

    // Unmodifiable views

    /** Returns an unmodifiable view of the characters in the tribe. */
    public List<Character> getCharacters() { return List.copyOf(characters); }

    /** Returns an unmodifiable view of the buildings in the tribe. */
    public List<Building>  getBuildings()  { return List.copyOf(buildings); }
}



