package it.polimi.it.cards.building;

import it.polimi.it.cards.building.modifier.ShamanicModifier;
import it.polimi.it.cards.building.modifier.SustenanceModifier;
import it.polimi.it.enums.EventType;

/**
 * Describes the effects of a building card.
 *
 * Built via the inner Builder to avoid long sequences of null arguments.
 * Every field defaults to null (or false), meaning no effect of that type.
 *
 * A building may have at most one hook per category:
 * - eventTrigger + eventHook: fires during a specific EventType
 * - turnHook: fires when the player repositions their totem
 * - acquisitionHook: fires when the player acquires a character
 * - endGameScorer: contributes to the final score
 * - hasPostRound: enables the extra post-round action (Watchtower)
 * - shamanicModifier: capability for the Shamanic Ritual event
 * - sustenanceModifier: capability for the Sustenance event
 */
public final class BuildingEffect {

    private final EventType          eventTrigger;
    private final EventHook          eventHook;
    private final TurnHook           turnHook;
    private final AcquisitionHook    acquisitionHook;
    private final EndGameScorer      endGameScorer;
    private final boolean            hasPostRound;
    private final ShamanicModifier   shamanicModifier;
    private final SustenanceModifier sustenanceModifier;

    private BuildingEffect(Builder b) {
        this.eventTrigger       = b.eventTrigger;
        this.eventHook          = b.eventHook;
        this.turnHook           = b.turnHook;
        this.acquisitionHook    = b.acquisitionHook;
        this.endGameScorer      = b.endGameScorer;
        this.hasPostRound       = b.hasPostRound;
        this.shamanicModifier   = b.shamanicModifier;
        this.sustenanceModifier = b.sustenanceModifier;
    }

    /** Singleton representing a building with no special effects. */
    public static final BuildingEffect NONE = new Builder().build();

    public boolean hasEventHook()          { return eventHook != null; }
    public boolean hasTurnHook()           { return turnHook != null; }
    public boolean hasAcquisitionHook()    { return acquisitionHook != null; }
    public boolean hasEndGameScorer()      { return endGameScorer != null; }
    public boolean hasPostRound()          { return hasPostRound; }
    public boolean hasShamanicModifier()   { return shamanicModifier != null; }
    public boolean hasSustenanceModifier() { return sustenanceModifier != null; }

    /** Returns true if this building has an effect that fires for the given event type. */
    public boolean triggersOn(EventType type) {
        return eventHook != null && eventTrigger == type;
    }

    public EventType          getEventTrigger()        { return eventTrigger; }
    public EventHook          getEventHook()            { return eventHook; }
    public TurnHook           getTurnHook()             { return turnHook; }
    public AcquisitionHook    getAcquisitionHook()      { return acquisitionHook; }
    public EndGameScorer      getEndGameScorer()        { return endGameScorer; }
    public ShamanicModifier   getShamanicModifier()    { return shamanicModifier; }
    public SustenanceModifier getSustenanceModifier()  { return sustenanceModifier; }

    // Inner Builder

    public static final class Builder {
        private EventType         eventTrigger;
        private EventHook         eventHook;
        private TurnHook          turnHook;
        private AcquisitionHook   acquisitionHook;
        private EndGameScorer     endGameScorer;
        private boolean           hasPostRound;
        private ShamanicModifier  shamanicModifier;
        private SustenanceModifier sustenanceModifier;

        public Builder onEvent(EventType trigger, EventHook hook) {
            this.eventTrigger = trigger;
            this.eventHook    = hook;
            return this;
        }

        public Builder onTurn(TurnHook hook) {
            this.turnHook = hook;
            return this;
        }

        public Builder onAcquisition(AcquisitionHook hook) {
            this.acquisitionHook = hook;
            return this;
        }

        public Builder atEndGame(EndGameScorer scorer) {
            this.endGameScorer = scorer;
            return this;
        }

        public Builder withPostRound() {
            this.hasPostRound = true;
            return this;
        }

        public Builder withShamanicModifier(ShamanicModifier modifier) {
            this.shamanicModifier = modifier;
            return this;
        }

        public Builder withSustenanceModifier(SustenanceModifier modifier) {
            this.sustenanceModifier = modifier;
            return this;
        }

        public BuildingEffect build() {
            return new BuildingEffect(this);
        }
    }
}