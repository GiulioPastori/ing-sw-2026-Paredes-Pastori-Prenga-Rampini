package it.polimi.it;

import it.polimi.it.Player.Player;
import it.polimi.it.cards.Building;
import it.polimi.it.cards.Card;
import it.polimi.it.enums.Era;
import it.polimi.it.tiles.OfferTile;
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
    //disposizione carte in alto è numero giocatori+4
   public void drawTopRow(int numPlayers){
        int cardsToDraw = numPlayers+4;
        for( int i=0; i< cardsToDraw; i++){
            if(!tribeDeck.isEmpty()){
                topRow.add(tribeDeck.remove(0));
            }
        }
   }


    public void clearBottomRow(){
        this.bootomRow.clear();
    }

   //finito round carte in alto vanno in basso
   public void moveTopToBottom(){
       bootomRow.addAll(topRow);
       topRow.clear();
   }


   public void resolveEvents(List<Player> player){
        //ogni evento controlla requisti giocatori
       for (Player p : player){

       }
   }

   public void takeCard(Card card, Player player){
        if(topRow.contains(card)){
            topRow.remove(card);
        }
        else if(bootomRow.contains(card)){
            bootomRow.remove(card);
        }

        player.getTribe().addCharacter((Character) card);
   }

   public void refillTopRow(int numPlayers){

   }

    public void resolveEndOfGameEvents(List<Player> players) {
    }

    public List<OfferTile> getOfferTrack() {
        return List.of();
    }

    public OfferTile getTurnOrderTile() {
        return getTurnOrderTile();
    }

    public List<Card> getBottomRow() {
        return List.of();
    }
}
