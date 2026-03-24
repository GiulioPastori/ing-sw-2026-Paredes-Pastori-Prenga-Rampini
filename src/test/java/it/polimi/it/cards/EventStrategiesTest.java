package it.polimi.it.cards;

import it.polimi.it.cards.character.Hunter;
import it.polimi.it.cards.character.Shaman;
import it.polimi.it.cards.character.Artist;
import it.polimi.it.enums.Era;
import it.polimi.it.enums.TotemColour;
import it.polimi.it.player.Player;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

// ── Hunt ──────────────────────────────────────────────────────────────────────

/**
 * Unit tests for the Hunt event strategy.
 *
 * Transition coverage:
 * - T1: no hunters -> no food, no PP
 * - T2: N hunters -> +N food, +N*prestigePerHunter PP
 * - T3: multiple players resolved independently
 */
@DisplayName("Hunt")
class HuntTest {

    private static final int PP_PER_HUNTER = 2;
    private final Hunt hunt = new Hunt(PP_PER_HUNTER);

    private Player playerWithHunters(int count) {
        Player p = new Player("P", TotemColour.RED);
        for (int i = 0; i < count; i++) {
            p.takeCharacterCard(new Hunter(7200 + i, Era.ERA_I, false));
        }
        return p;
    }

    @Test
    @DisplayName("T1: no hunters leaves food and PP unchanged")
    void noHuntersNoEffect() {
        Player p = new Player("P", TotemColour.RED);
        hunt.apply(List.of(p));
        assertEquals(0, p.getFood());
        assertEquals(0, p.getPrestigePoints());
    }

    @Test
    @DisplayName("T2: N hunters grant N food and N*PP")
    void nHuntersGrantFoodAndPP() {
        Player p = playerWithHunters(3);
        hunt.apply(List.of(p));
        assertEquals(3, p.getFood());
        assertEquals(3 * PP_PER_HUNTER, p.getPrestigePoints());
    }

    @Test
    @DisplayName("T3: players with different hunter counts are independent")
    void multiplePlayersIndependent() {
        Player p1 = playerWithHunters(2);
        Player p2 = playerWithHunters(0);
        hunt.apply(List.of(p1, p2));
        assertEquals(2, p1.getFood());
        assertEquals(0, p2.getFood());
    }
}

// ── ShamanicRitual ────────────────────────────────────────────────────────────

/**
 * Unit tests for the Shamanic Ritual event strategy.
 *
 * Transition coverage:
 * - T1: majority -> +reward PP
 * - T2: minority -> -penalty PP
 * - T3: tie for max -> all tied players get reward
 * - T4: tie for min -> all tied players lose penalty
 * - T5: single player is both max and min -> both reward and penalty applied
 */
@DisplayName("ShamanicRitual")
class ShamanicRitualTest {

    private static final int REWARD  = 5;
    private static final int PENALTY = 3;
    private final ShamanicRitual ritual = new ShamanicRitual(REWARD, PENALTY);

    private Player playerWithIcons(int icons, TotemColour colour) {
        Player p = new Player("P_" + colour, colour);
        if (icons > 0) p.takeCharacterCard(new Shaman(2001, Era.ERA_I, Math.min(icons, 3)));
        // Add additional shamans if icons > 3
        int remaining = icons - 3;
        while (remaining > 0) {
            int batch = Math.min(remaining, 3);
            p.takeCharacterCard(new Shaman(2002, Era.ERA_I, batch));
            remaining -= batch;
        }
        return p;
    }

    @Test
    @DisplayName("T1: player with most icons gains reward")
    void majorityGainsReward() {
        Player high = playerWithIcons(3, TotemColour.RED);
        Player low  = playerWithIcons(1, TotemColour.BLUE);
        ritual.apply(List.of(high, low));
        assertEquals(REWARD,   high.getPrestigePoints());
        assertEquals(-PENALTY, low.getPrestigePoints());
    }

    @Test
    @DisplayName("T3: tie for max -> all tied players gain reward")
    void tieForMaxAllGainReward() {
        Player p1 = playerWithIcons(2, TotemColour.RED);
        Player p2 = playerWithIcons(2, TotemColour.BLUE);
        Player p3 = playerWithIcons(1, TotemColour.YELLOW);
        ritual.apply(List.of(p1, p2, p3));
        assertEquals(REWARD,   p1.getPrestigePoints());
        assertEquals(REWARD,   p2.getPrestigePoints());
        assertEquals(-PENALTY, p3.getPrestigePoints());
    }

    @Test
    @DisplayName("T4: tie for min -> all tied players lose penalty")
    void tieForMinAllLosePenalty() {
        Player high = playerWithIcons(3, TotemColour.RED);
        Player low1 = playerWithIcons(1, TotemColour.BLUE);
        Player low2 = playerWithIcons(1, TotemColour.YELLOW);
        ritual.apply(List.of(high, low1, low2));
        assertEquals(REWARD,   high.getPrestigePoints());
        assertEquals(-PENALTY, low1.getPrestigePoints());
        assertEquals(-PENALTY, low2.getPrestigePoints());
    }

    @Test
    @DisplayName("T5: single player receives both reward and penalty")
    void singlePlayerReceivesBothEffects() {
        Player p = playerWithIcons(2, TotemColour.RED);
        ritual.apply(List.of(p)); // only player = max and min
        assertEquals(REWARD - PENALTY, p.getPrestigePoints());
    }
}

// ── CavePaintings ─────────────────────────────────────────────────────────────

/**
 * Unit tests for the Cave Paintings event strategy.
 *
 * Transition coverage:
 * - T1: artists < threshold -> -penalty PP
 * - T2: artists == threshold -> +PP * count
 * - T3: artists > threshold -> +PP * count
 * - T4: multiple players resolved independently
 */
@DisplayName("CavePaintings")
class CavePaintingsTest {

    private static final int MIN_ARTISTS  = 2;
    private static final int PENALTY      = 3;
    private static final int PP_PER_ARTIST = 2;
    private final CavePaintings cave = new CavePaintings(MIN_ARTISTS, PENALTY, PP_PER_ARTIST);

    private Player playerWithArtists(int count) {
        Player p = new Player("P", TotemColour.RED);
        for (int i = 0; i < count; i++) {
            p.takeCharacterCard(new Artist(7300 + i, Era.ERA_I));
        }
        return p;
    }

    @Test
    @DisplayName("T1: zero artists -> -penalty PP")
    void zeroArtistsPenalty() {
        Player p = playerWithArtists(0);
        cave.apply(List.of(p));
        assertEquals(-PENALTY, p.getPrestigePoints());
    }

    @Test
    @DisplayName("T1: one artist (below threshold) -> -penalty PP")
    void oneArtistBelowThresholdPenalty() {
        Player p = playerWithArtists(1);
        cave.apply(List.of(p));
        assertEquals(-PENALTY, p.getPrestigePoints());
    }

    @Test
    @DisplayName("T2: artists == threshold -> bonus PP")
    void atThresholdBonus() {
        Player p = playerWithArtists(MIN_ARTISTS);
        cave.apply(List.of(p));
        assertEquals(MIN_ARTISTS * PP_PER_ARTIST, p.getPrestigePoints());
    }

    @Test
    @DisplayName("T3: artists > threshold -> bonus PP scales with count")
    void aboveThresholdScales() {
        Player p = playerWithArtists(4);
        cave.apply(List.of(p));
        assertEquals(4 * PP_PER_ARTIST, p.getPrestigePoints());
    }

    @Test
    @DisplayName("T4: multiple players resolved independently")
    void multiplePlayersIndependent() {
        Player p1 = playerWithArtists(3);
        Player p2 = playerWithArtists(0);
        cave.apply(List.of(p1, p2));
        assertEquals(3 * PP_PER_ARTIST, p1.getPrestigePoints());
        assertEquals(-PENALTY,           p2.getPrestigePoints());
    }
}