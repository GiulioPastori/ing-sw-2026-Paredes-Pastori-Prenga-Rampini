package it.polimi.it.cards.character;


import it.polimi.it.enums.CharacterType;
import it.polimi.it.enums.Era;
import it.polimi.it.player.Player;

/** Artist character card. */
public class Artist extends Character {

    /**
     * Creates an Artist card.
     *
     * @param id card identifier
     * @param era card era
     */
    public Artist(int id, Era era) {
        super(id, era, CharacterType.ARTIST, 0);
    }

    @Override
    public void applyImmediateEffect(Player player) { /* no immediate effect */ }
}
