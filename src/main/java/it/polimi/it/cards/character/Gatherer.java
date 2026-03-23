package it.polimi.it.cards.character;

import it.polimi.it.Board;
import it.polimi.it.player.Player;
import it.polimi.it.enums.CharacterType;
import it.polimi.it.enums.Era;

public class Gatherer extends Character {
    private static int sustenanceDiscount;

    public Gatherer(String name, Era era, int icon, int basePrestige, int sustenanceDiscount, boolean totemIcon) {
        // Passiamo i parametri. Il tipo è GATHERER
        super(name, era, CharacterType.GATHERER, icon, basePrestige, totemIcon);
        this.sustenanceDiscount = sustenanceDiscount;
    }

    @Override
    protected void onAcquire(Player player, Board board) {
        // Spesso il raccoglitore dà cibo appena preso
        // Se il valore nel JSON è lo stesso dello sconto, va bene così
        player.addFood(this.sustenanceDiscount);
    }

    /**
     * Questo valore verrà letto dal GameController durante l'evento Sustenance
     * per ridurre il cibo che il giocatore deve pagare.
     */
    public int getSustenanceDiscount() {
        return this.sustenanceDiscount;
    }
}
