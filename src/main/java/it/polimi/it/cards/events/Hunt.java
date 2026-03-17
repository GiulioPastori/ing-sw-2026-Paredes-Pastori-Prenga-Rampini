package it.polimi.it.cards.events;

public class Hunt implements EventStrategy {

    private int prestigePerHunter;

    public HuntStrategy(int prestigePerHunter) {
        this.prestigePerHunter = prestigePerHunter;
    }

    @Override
    public void apply(List<Player> players, Board board) {
        for (Player p : players) {
            int hunters = p.getTribe().countByType(CharacterType.HUNTER);

            p.addFood(hunters);
            p.addPrestige(hunters * prestigePerHunter);
        }
    }
}

