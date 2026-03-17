package it.polimi.it;

import it.polimi.it.Player.Player;
import it.polimi.it.cards.Building;
import it.polimi.it.cards.Card;
import it.polimi.it.enums.Era;
import it.polimi.it.tiles.OfferTiles;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Board {

    private List<OfferTiles> offerTrack;
    private List<Card> topRow;
    private List<Card> bootomRow;
    private List<Card> tribeDeck;
    private Map<Era, List<Building>> buildingDeck;

   public Board(){
       this.offerTrack = new ArrayList<>();
       this.topRow = new ArrayList<>();
       this.bootomRow = new ArrayList<>();
       this.tribeDeck = new ArrayList<>();
   }

   public void drawTopRow(int numPlayers){

   }

   public void moveTopToBottom(){

   }

   public void clearBottomRow(){
       this.bootomRow.clear();
   }

   public void resolveEvents(List<Player> player){

   }

   public void takeCard(Card card, Player player){

   }

   public void refillTopRow(int numPlayers){

   }
}
