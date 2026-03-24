package it.polimi.it.view;

import it.polimi.it.enums.Era;
import it.polimi.it.observer.CardSnapshot;
import it.polimi.it.observer.OfferTileSnapshot;
import it.polimi.it.observer.PlayerSnapshot;

import java.util.Comparator;
import java.util.List;

/**
 * Stub implementation of GameView for development and testing.
 *
 * Prints to stdout and always picks the first available option.
 * Replace with a real TUI or GUI implementation when ready.
 */
public class StubGameView implements GameView {

    @Override
    public void showRoundStarted(int round, List<PlayerSnapshot> turnOrder) {
        System.out.println("\n=== Round " + round + " ===");
        turnOrder.forEach(p -> System.out.print(p.name() + " "));
        System.out.println();
    }

    @Override
    public void showTotemPlaced(PlayerSnapshot player, char tileLetter) {
        System.out.println(player.name() + " -> tile " + tileLetter);
    }

    @Override
    public void showCardSlotReady(PlayerSnapshot player, boolean isTopRow,
                                  List<CardSnapshot> availableCards) {
        String row = isTopRow ? "top" : "bottom";
        System.out.println(player.name() + " choosing from " + row + " row ("
                + availableCards.size() + " cards)");
    }

    @Override
    public void showCardAcquired(PlayerSnapshot player, CardSnapshot card) {
        System.out.println(player.name() + " takes card #" + card.id());
    }

    @Override
    public void showCardAcquisitionFailed(PlayerSnapshot player, CardSnapshot card,
                                          int missingFood) {
        System.out.println(player.name() + " cannot afford card #" + card.id()
                + " (missing " + missingFood + " food)");
    }

    @Override
    public void showTotemReturnedToTurnOrder(PlayerSnapshot player, int position) {
        System.out.println(player.name() + " -> turn order " + position
                + " | food: " + player.food() + " | PP: " + player.prestigePoints());
    }

    @Override
    public void showPostRoundStarted(List<PlayerSnapshot> watchtowerPlayers,
                                     List<CardSnapshot> availableCards) {
        watchtowerPlayers.forEach(p ->
                System.out.println(p.name() + " has Watchtower (" + availableCards.size() + " cards available)"));
    }

    @Override
    public void showEventResolved(int eventId, Era era, it.polimi.it.enums.EventType type) {
        System.out.println("[Event resolved: " + type + " #" + eventId + " / " + era + "]");
    }

    @Override
    public void showEraChanged(Era newEra) {
        System.out.println("*** Era: " + newEra + " ***");
    }

    @Override
    public void showRoundEnded(int round) {
        System.out.println("--- End of round " + round + " ---");
    }

    @Override
    public void showInvalidAction(String playerName, String reason) {
        System.out.println("[Invalid: " + playerName + " - " + reason + "]");
    }

    @Override
    public void showGameEnded(List<PlayerSnapshot> winners, List<PlayerSnapshot> finalScores) {
        System.out.println("\n=== GAME OVER ===");
        finalScores.stream()
                .sorted(Comparator.comparingInt(PlayerSnapshot::prestigePoints).reversed())
                .forEach(p -> System.out.printf("  %-15s %4d PP | %2d food%n",
                        p.name(), p.prestigePoints(), p.food()));
        System.out.print("Winner(s): ");
        winners.forEach(w -> System.out.print(w.name() + " "));
        System.out.println();
    }

    // Input stubs — always pick first available option

    @Override
    public char requestOfferTileChoice(PlayerSnapshot player, List<OfferTileSnapshot> freeTiles) {
        return freeTiles.get(0).letter();
    }

    @Override
    public int requestCardChoice(PlayerSnapshot player, List<CardSnapshot> available) {
        return available.isEmpty() ? -1 : available.get(0).id();
    }

    @Override
    public int requestPostRoundCardChoice(PlayerSnapshot player, List<CardSnapshot> available) {
        return -1; // stub always declines
    }
}