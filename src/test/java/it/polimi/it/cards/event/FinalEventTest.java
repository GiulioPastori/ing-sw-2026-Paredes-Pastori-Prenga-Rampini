package it.polimi.it.cards.event;

import it.polimi.it.cards.events.FinalEvent;
import it.polimi.it.enums.Era;
import it.polimi.it.enums.EventType;
import it.polimi.it.enums.TotemColour;
import it.polimi.it.player.Player;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("FinalEvent")
class FinalEventTest {

    @Test
    @DisplayName("FinalEvent always belongs to ERA_III")
    void finalEventBelongsToEraIII() {
        FinalEvent fe = new FinalEvent(2091, EventType.HUNT, 4, ps -> {});
        assertEquals(Era.ERA_III, fe.getEra());
    }

    @Test
    @DisplayName("FinalEvent getType returns correct event type")
    void getTypeReturnsCorrect() {
        FinalEvent fe = new FinalEvent(2091, EventType.HUNT, 4, ps -> {});
        assertEquals(EventType.HUNT, fe.getType());
    }

    @Test
    @DisplayName("FinalEvent getValue returns correct value")
    void getValueReturnsCorrect() {
        FinalEvent fe = new FinalEvent(2092, EventType.SUSTENANCE, 4, ps -> {});
        assertEquals(4, fe.getValue());
    }

    @Test
    @DisplayName("FinalEvent resolve invokes the strategy")
    void resolveInvokesStrategy() {
        Player player = new Player("Alice", TotemColour.RED);
        FinalEvent fe = new FinalEvent(2091, EventType.HUNT, 4,
                ps -> ps.forEach(p -> p.addPrestigePoints(10)));

        fe.resolve(List.of(player));

        assertEquals(10, player.getPrestigePoints());
    }
}