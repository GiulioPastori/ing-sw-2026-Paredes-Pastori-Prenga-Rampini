package it.polimi.it.cards.character;


import it.polimi.it.enums.CharacterType;
import it.polimi.it.enums.Era;
import it.polimi.it.player.Player;

/**
 * Cave Paintings event: number of artists vs. threshold determines PP gain/loss.
 * End of game: 10 PP for every pair of artists (integer division, rulebook p.7).
 */
public class Artist extends Character {

    public Artist(int id, Era era) {
        super(id, era, CharacterType.ARTIST, 0);
    }

    @Override
    public void applyImmediateEffect(Player player) { /* no immediate effect */ }
}

