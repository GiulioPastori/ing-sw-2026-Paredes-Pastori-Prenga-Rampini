package it.polimi.it.tiles;

import it.polimi.it.enums.TotemColour;

/**
 * Rappresenta una singola posizione sulla tessera Ordine di Turno.
 */
public class TurnPosition {
    private final int position;
    private final int foodTokens;
    private final int penalties;
    private TotemColour placedTotem; // Salviamo solo il COLORE (Enum)

    public TurnPosition(int position, int foodTokens, int penalties) {
        this.position    = position;
        this.foodTokens  = foodTokens;
        this.penalties   = penalties;
        this.placedTotem = null;
    }

    /**
     * Verifica se la posizione è libera.
     */
    public boolean isFree() {
        return placedTotem == null;
    }

    /**
     * Piazza il colore del totem in questa posizione.
     * Questo metodo risolve l'errore a riga 37 della TurnOrderTile.
     */
    public void place(TotemColour totem) {
        this.placedTotem = totem;
    }

    /**
     * Svuota la posizione a fine round.
     */
    public void free() {
        this.placedTotem = null;
    }

    // --- Getters ---
    public int getPosition() {
        return position;
    }

    public int getFoodTokens() {
        return foodTokens;
    }

    public int getPenalties() {
        return penalties;
    }

    public TotemColour getPlacedTotem() {
        return placedTotem;
    }
}