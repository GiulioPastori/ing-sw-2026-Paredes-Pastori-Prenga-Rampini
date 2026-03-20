package it.polimi.it.cards.character;

import it.polimi.it.Board;
import it.polimi.it.Player.Player;
import it.polimi.it.Player.Tribe;
import it.polimi.it.enums.CharacterType;
import it.polimi.it.enums.Era;

 public class Artists extends Character {


    public Artists(String name, Era era,boolean totemIcon) {
        super(name, era, CharacterType.ARTIST,0,0, totemIcon);
    }




    @Override
    protected void onAcquire(Player player, Board board) {
     // niente effetto immediato
    }

     @Override
     public void applyEventBonus(Player player, Board board) {
         // Nessun bonus individuale specifico; l'evento conta il totale degli oggetti Artists
     }

     @Override
     public int calculateFinalPoints(Player player) {

        return 0; // Il calcolo è aggregato: (count / 2) * 10
     }
 }