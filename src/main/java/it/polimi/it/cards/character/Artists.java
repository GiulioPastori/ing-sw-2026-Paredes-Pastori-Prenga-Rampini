package it.polimi.it.cards.character;

import it.polimi.it.Board;
import it.polimi.it.Player.Player;
import it.polimi.it.Player.Tribe;
import it.polimi.it.enums.CharacterType;
import it.polimi.it.enums.Era;

 public class Artists extends Character {
    public int cavePaintingScores;
    public int endGameSetBonus;

    public Artists(String name, Era era, CharacterType type, int icon, int cavePaintingScores, int endGameSetBonus) {
        super(name, era, CharacterType.ARTIST, 0,0);
        this.cavePaintingScores = cavePaintingScores;
        this.endGameSetBonus = endGameSetBonus;
    }




 @Override
 protected void onAcquire(Player player, Board board) {
     // niente effetto immediato
 }

 @Override
 public void onEvent(Player player, Board board) {
     // usato nel Cave Painting
 }
 }