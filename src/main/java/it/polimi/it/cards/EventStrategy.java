package it.polimi.it.cards.events;

public interface EventStrategy {
    void apply(List<Player> players, Board board);
}