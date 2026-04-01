package it.polimi.it.observer;

import it.polimi.it.cards.building.Building;
import it.polimi.it.cards.character.Hunter;
import it.polimi.it.cards.events.Event;
import it.polimi.it.cards.events.Hunt;
import it.polimi.it.enums.Era;
import it.polimi.it.enums.EventType;
import it.polimi.it.enums.CharacterType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("CardSnapshot")
class CardSnapshotTest {

    // ── of() factory — CHARACTER ──────────────────────────────────────────────

    @Test
    @DisplayName("of(Character) returns CHARACTER type")
    void ofCharacterReturnsCharacterType() {
        Hunter hunter = new Hunter(1001, Era.ERA_I, false);
        CardSnapshot snap = CardSnapshot.of(hunter);
        assertEquals(CardSnapshot.Type.CHARACTER, snap.type());
        assertTrue(snap.isCharacter());
    }

    @Test
    @DisplayName("of(Character) sets correct id, era and characterType")
    void ofCharacterSetsFields() {
        Hunter hunter = new Hunter(1001, Era.ERA_I, true);
        CardSnapshot snap = CardSnapshot.of(hunter);
        assertEquals(1001,              snap.id());
        assertEquals(Era.ERA_I,         snap.era());
        assertEquals(CharacterType.HUNTER, snap.characterType());
    }

    @Test
    @DisplayName("of(Character) sets foodCost and finalPoints to zero")
    void ofCharacterZerosBuildingFields() {
        CardSnapshot snap = CardSnapshot.of(new Hunter(1001, Era.ERA_I, false));
        assertEquals(0, snap.foodCost());
        assertEquals(0, snap.finalPoints());
        assertEquals("", snap.effectDescription());
    }

    // ── of() factory — BUILDING ───────────────────────────────────────────────

    @Test
    @DisplayName("of(Building) returns BUILDING type")
    void ofBuildingReturnsBuildingType() {
        Building b = new Building(301, Era.ERA_III, 8, 5, "25 fixed PP");
        CardSnapshot snap = CardSnapshot.of(b);
        assertEquals(CardSnapshot.Type.BUILDING, snap.type());
        assertTrue(snap.isBuilding());
    }

    @Test
    @DisplayName("of(Building) sets correct foodCost, finalPoints and effectDescription")
    void ofBuildingSetsFields() {
        Building b = new Building(301, Era.ERA_III, 8, 5, "25 fixed PP");
        CardSnapshot snap = CardSnapshot.of(b);
        assertEquals(301,          snap.id());
        assertEquals(Era.ERA_III,  snap.era());
        assertEquals(8,            snap.foodCost());
        assertEquals(5,            snap.finalPoints());
        assertEquals("25 fixed PP", snap.effectDescription());
    }

    @Test
    @DisplayName("of(Building) sets characterType to null")
    void ofBuildingNullCharacterType() {
        CardSnapshot snap = CardSnapshot.of(new Building(301, Era.ERA_III, 8, 5, ""));
        assertNull(snap.characterType());
    }

    // ── of() factory — EVENT ──────────────────────────────────────────────────

    @Test
    @DisplayName("of(Event) returns EVENT type")
    void ofEventReturnsEventType() {
        Event event = new Event(2001, Era.ERA_I, EventType.HUNT, 1, new Hunt(1));
        CardSnapshot snap = CardSnapshot.of(event);
        assertEquals(CardSnapshot.Type.EVENT, snap.type());
        assertTrue(snap.isEvent());
    }

    @Test
    @DisplayName("of(Event) sets correct id and era")
    void ofEventSetsFields() {
        Event event = new Event(2001, Era.ERA_I, EventType.HUNT, 1, new Hunt(1));
        CardSnapshot snap = CardSnapshot.of(event);
        assertEquals(2001,      snap.id());
        assertEquals(Era.ERA_I, snap.era());
        assertNull(snap.characterType());
    }

    // ── Type predicates ───────────────────────────────────────────────────────

    @Test
    @DisplayName("isCharacter returns false for building and event")
    void isCharacterFalseForOthers() {
        assertFalse(CardSnapshot.of(new Building(301, Era.ERA_III, 8, 5, "")).isCharacter());
        assertFalse(CardSnapshot.of(
                new Event(2001, Era.ERA_I, EventType.HUNT, 1, new Hunt(1))).isCharacter());
    }

    @Test
    @DisplayName("isBuilding returns false for character and event")
    void isBuildingFalseForOthers() {
        assertFalse(CardSnapshot.of(new Hunter(1001, Era.ERA_I, false)).isBuilding());
        assertFalse(CardSnapshot.of(
                new Event(2001, Era.ERA_I, EventType.HUNT, 1, new Hunt(1))).isBuilding());
    }

    @Test
    @DisplayName("isEvent returns false for character and building")
    void isEventFalseForOthers() {
        assertFalse(CardSnapshot.of(new Hunter(1001, Era.ERA_I, false)).isEvent());
        assertFalse(CardSnapshot.of(new Building(301, Era.ERA_III, 8, 5, "")).isEvent());
    }
}