package it.polimi.it.player;

import it.polimi.it.cards.Building;
import it.polimi.it.cards.building.Building;
import it.polimi.it.cards.character.Character;
import it.polimi.it.enums.TotemColour;

/**
 * Represents a player in the game.
 *
 * Personal state: name, totem colour, food supply, prestige points,
 * turn order position, and the player's tribe (characters and buildings).
 *
 * Responsible for managing own resources and delegating card effects.
 * Does not know Game or Board.
 */
public class Player {

    private final String      name;
    private final TotemColour totemColour;
    private int food;
    private int prestigePoints;
    private int turnOrder;
    private final Tribe tribe;

    public Player(String name, TotemColour totemColour) {
        this.name           = name;
        this.totemColour    = totemColour;
        this.food           = 0;
        this.prestigePoints = 0;
        this.turnOrder      = 0;
        this.tribe          = new Tribe();
    }

    // Card acquisition

    /**
     * Adds a character to the tribe, applies the character's immediate effect,
     * then notifies all buildings with an AcquisitionHook (e.g. complete-set bonus,
     * inventor pair bonus). The character is added before the hooks fire so it
     * counts toward the totals they inspect.
     *
     * @param character the character being acquired
     */
    public void takeCharacterCard(Character character) {
        tribe.addCharacter(character);
        character.applyImmediateEffect(this);
        tribe.getBuildings().stream()
                .filter(b -> b.getEffect().hasAcquisitionHook())
                .forEach(b -> b.applyAcquisitionHook(this, character));
    }

    /**
     * Attempts to acquire a building. Verifies the player has enough food after
     * applying the builder discount from the tribe (rulebook p.5).
     *
     * @param building the building to acquire
     * @return true if acquired, false if food was insufficient
     */
    public boolean takeBuildingCard(Building building) {
        int cost = building.getEffectiveCost(tribe);
        if (food < cost) return false;
        food -= cost;
        tribe.addBuilding(building);
        return true;
    }

    // Resource management

    /**
     * Adds food to the player's supply.
     *
     * @param amount amount to add (must be &gt;= 0; use spendFood to subtract)
     * @throws IllegalArgumentException if amount is negative
     */
    public void addFood(int amount) {
        if (amount < 0) throw new IllegalArgumentException(
                "Use spendFood() to subtract food, not addFood() with a negative value.");
        food += amount;
    }

    /**
     * Subtracts food from the player's supply.
     *
     * @param amount amount to spend (must be &gt;= 0 and &lt;= current food)
     * @throws IllegalArgumentException if amount is negative
     * @throws IllegalStateException    if the player does not have enough food
     */
    public void spendFood(int amount) {
        if (amount < 0) throw new IllegalArgumentException(
                "Amount to spend must be >= 0.");
        if (amount > food) throw new IllegalStateException(
                "Insufficient food: need " + amount + ", have " + food);
        food -= amount;
    }

    /**
     * Adds (or subtracts if negative) prestige points.
     * Prestige points may go negative (rulebook p.4).
     *
     * @param amount PP to add (negative to subtract)
     */
    public void addPrestigePoints(int amount) { prestigePoints += amount; }

    // Final score

    /**
     * Computes the player's final score as:
     * in-game PP + builder PP + inventor PP + artist PP + building PP.
     * See rulebook p.7 for details on each component.
     *
     * @return total final score
     */
    public int calculateFinalScore() {
        return prestigePoints
                + tribe.computeBuilderPoints()
                + tribe.computeInventorPoints()
                + tribe.computeArtistPoints()
                + tribe.computeBuildingPoints(this);
    }

    // Getters and setters

    public String      getName()           { return name; }
    public TotemColour getTotemColour()    { return totemColour; }
    public int         getFood()           { return food; }
    public int         getPrestigePoints() { return prestigePoints; }
    public int         getTurnOrder()      { return turnOrder; }
    public Tribe       getTribe()          { return tribe; }

    /**
     * Sets the turn order position for the next round.
     * Called by {@link it.polimi.it.tiles.TurnOrderTile} after totem placement.
     *
     * @param order position (1 = first)
     */
    public void setTurnOrder(int order) { this.turnOrder = order; }

    @Override
    public String toString() {
        return name + " [food=" + food + ", PP=" + prestigePoints
                + ", turnOrder=" + turnOrder + "]";
    }
}