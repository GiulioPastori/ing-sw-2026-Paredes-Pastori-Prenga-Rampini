package it.polimi.it.cards.events;

public class Sustenance implements EventStrategy {

    private int penaltyPerMissingFood;

    public SustenanceStrategy(int penaltyPerMissingFood) {
        this.penaltyPerMissingFood = penaltyPerMissingFood;
    }

    @Override
    public void apply(List<Player> players, Board board) {
        for (Player p : players) {

            int characters = p.getTribe().getCharacters().size();
            int discount = p.getTribe().countByType(CharacterType.GATHERER) * 3;

            int foodNeeded = Math.max(0, characters - discount);
            int availableFood = p.getFood();

            int toPay = Math.min(foodNeeded, availableFood);
            p.spendFood(toPay);

            int missing = foodNeeded - toPay;

            if (missing > 0) {
                p.addPrestige(-missing * penaltyPerMissingFood);
            }
        }
    }
}