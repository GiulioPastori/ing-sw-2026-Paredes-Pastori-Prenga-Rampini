package it.polimi.it.cards.character;

import it.polimi.it.Board;
import it.polimi.it.Player.Player;
import it.polimi.it.cards.Card;
import it.polimi.it.enums.CharacterType;
import it.polimi.it.enums.Era;

public abstract class Character extends Card {
    protected CharacterType type;
    protected int icon; // Numero di icone (es. per Shaman o Inventor)
    protected int basePrestige;

    public Character(String name, Era era, CharacterType type, int icon, int basePrestige) {
        super(name, era);
        this.type = type;
        this.icon = icon;
        this.basePrestige = basePrestige;
    }

    public CharacterType getType() {
        return type;
    }

    public int getBasePrestige() {
        return basePrestige;
    }

    public int getIcon() {
        return this.icon;
    }

    /**
     * Applica l'effetto quando la carta viene acquisita.
     */
    @Override
    public void applyEffect(Player player, Board board) {
        // 1. Aggiungi subito alla tribù (così gli effetti vedono la carta)
        player.getTribe().addCharacter(this);

        // 2. Applica i punti prestigio base (se presenti)
        if (basePrestige > 0) {
            player.addPrestigePoint(basePrestige);
        }

        // 3. Esegue l'effetto specifico (Hook)
        onAcquire(player, board);
    }

    /**
     * Hook per le sottoclassi (es. Hunter che aggiunge cibo)
     */
    protected abstract void onAcquire(Player player, Board board);

    /**
     * Bonus specifico durante gli eventi (da sovrascrivere se serve)
     */
    public void applyEventBonus(Player player, Board board) {
    }

    /**
     * Calcolo punti extra a fine partita (es. per Artist o Inventor)
     */
    public int calculateFinalPoints(Player player) {
        return 0;
    }

    public int getPoint() {
        return 0;
    }
}
