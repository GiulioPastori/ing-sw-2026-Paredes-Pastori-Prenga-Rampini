package it.polimi.it.cards.events;

import it.polimi.it.cards.events.EventStrategy;
import it.polimi.it.enums.CharacterType;
import it.polimi.it.enums.EventType;
import it.polimi.it.player.Player;

import java.util.List;

/** Strategy for the Cave Paintings event.
 Base rule: if the player has fewer than minArtistsForBonus artists,
 they lose penaltyPoints PP. Otherwise they gain prestigePerArtist PP for
 each artist in their tribe.
 - Building 204: takes 1 food for each artist in the tribe
 */
public class CavePaintings implements EventStrategy {

    private final int minArtistsForBonus;
    private final int penaltyPoints;
    private final int prestigePerArtist;

    public CavePaintings(int minArtistsForBonus, int penaltyPoints, int prestigePerArtist) {
        this.minArtistsForBonus = minArtistsForBonus;
        this.penaltyPoints      = penaltyPoints;
        this.prestigePerArtist  = prestigePerArtist;
    }

    @Override
    public void apply(List<Player> players) {
        for (Player player : players) {
            int artistCount = player.getTribe().countByType(CharacterType.ARTIST);

            // Base effect PP
            if (artistCount < minArtistsForBonus) {
                player.addPrestigePoints(-penaltyPoints);
            } else {
                player.addPrestigePoints(artistCount * prestigePerArtist);
            }

            // Building 204: +1 food for each artist
            player.getTribe().getBuildings().stream()
                    .filter(b -> b.triggersOnEvent(EventType.CAVE_PAINTINGS))
                    .forEach(b -> b.applyEventHook(player, EventType.CAVE_PAINTINGS));
        }
    }

    public int getMinArtistsForBonus() { return minArtistsForBonus; }
    public int getPenaltyPoints()      { return penaltyPoints; }
    public int getPrestigePerArtist()  { return prestigePerArtist; }
}