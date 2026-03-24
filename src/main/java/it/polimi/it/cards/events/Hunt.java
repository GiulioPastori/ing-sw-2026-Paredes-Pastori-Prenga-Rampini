package it.polimi.it.cards.events;

import it.polimi.it.enums.CharacterType;
import it.polimi.it.enums.EventType;
import it.polimi.it.player.Player;
import it.polimi.it.cards.events.EventStrategy;

import java.util.List;

/**
 * Strategy for the Hunt event.
 *
 * Base rule: for each hunter in the tribe the player takes 1 food
 * and gains the PP shown on the event card.
 *
 * Building effects that modify this event:
 * - Building 202/306: for each hunter, +1 food and +1 PP additional
 *   (stacks with base effect, giving +2 food total and base PP+1 per hunter)
 */
public class Hunt implements EventStrategy {

    private final int prestigePerHunter;

    public Hunt(int prestigePerHunter) {
        this.prestigePerHunter = prestigePerHunter;
    }

    @Override
    public void apply(List<Player> players) {
        for (Player player : players) {
            int hunters = player.getTribe().countByType(CharacterType.HUNTER);
            if (hunters == 0) continue;

            // Base effect
            player.addFood(hunters);
            player.addPrestigePoints(hunters * prestigePerHunter);

            // Buildings 202/306: +1 food and +1 PP for each hunter
            player.getTribe().getBuildings().stream()
                    .filter(b -> b.triggersOnEvent(EventType.HUNT))
                    .forEach(b -> b.applyEventHook(player, EventType.HUNT));
        }
    }

    public int getPrestigePerHunter() { return prestigePerHunter; }
}

