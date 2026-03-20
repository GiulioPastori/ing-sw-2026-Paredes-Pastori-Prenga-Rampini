package it.polimi.it.cards.character;

import it.polimi.it.Board;
import it.polimi.it.Player.Player;
import it.polimi.it.enums.CharacterType;
import it.polimi.it.enums.Era;

public class Inventor extends Character {
    private final int inventionIconId; //identifica l'invenzione

    public Inventor(String name, Era era, int icon, int inventionIconId, boolean totemIcon) {
        // basePrestige è 0 perché i punti dell'inventore si calcolano solo alla fine
        super(name, era, CharacterType.INVENTOR, icon, 0, totemIcon);
        this.inventionIconId = inventionIconId;
    }

    public int getInventionIconId() {
        return inventionIconId;
    }

    @Override
    protected void onAcquire(Player player, Board board) {
        // Nessun effetto immediato all'acquisto
    }

    @Override
    public int calculateFinalPoints(Player player) {
        // Qui non restituiamo ancora il calcolo

        return 0;
    }
}