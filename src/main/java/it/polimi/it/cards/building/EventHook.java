package it.polimi.it.cards.building;


import it.polimi.it.enums.EventType;
import it.polimi.it.player.Player;

/**
 * Building effect that fires during the resolution of a specific event.
 *
 * Used for buildings that modify event behaviour (e.g. food discount on
 * Sustenance, immunity or bonus on Shamanic Ritual, extra food on Hunt or
 * Cave Paintings).
 *
 * The event strategy calls this hook before or after its main logic,
 * passing the player it is currently processing.
 */
@FunctionalInterface
public interface EventHook {

    /**
     * Applies the building effect during event resolution.
     *
     * @param player    the player who owns the building
     * @param eventType the event currently being resolved
     */
    void apply(Player player, EventType eventType);
}