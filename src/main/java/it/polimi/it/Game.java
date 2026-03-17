package it.polimi.it;

import it.polimi.it.Player.Player;

import java.util.ArrayList;
import java.util.List;

public class Game {
    private List<Player> players;
    private Board board;
    private int currentRound;
    private int currentPlayerIndex;

    public Game(){
        this.players = new ArrayList<>();
        this.board = new Board();
        this.currentRound = 1;
        this.currentPlayerIndex = 0;
    }

    public void setup(){

    }

    public void startGame(){

    }

    public void nextRound(){
        this.currentRound++;
    }

    public void calculatePoints(){

    }

    public void endGame(){

    }
    public Player getCurrentPlayer() {
        return players.get(currentPlayerIndex);
    }
}
