package it.polimi.it.observer;

import it.polimi.it.cards.building.Building;
import it.polimi.it.cards.Card;
import it.polimi.it.cards.character.Character;
import it.polimi.it.enums.CharacterType;
import it.polimi.it.enums.Era;

import java.io.Serial;
import java.io.Serializable;

/**
 * Immutable snapshot of a card, carried by GameEvents and sent over the network.
 *
 * Implements Serializable so it can be passed inside Message objects
 * over RMI and via Java object serialisation over sockets.
 */
public record CardSnapshot(
        int           id,
        Era           era,
        Type          type,
        CharacterType characterType,
        int           foodCost,
        int           finalPoints,
        String        effectDescription
) implements Serializable {
    @Serial private static final long serialVersionUID = 1L;

    public enum Type implements Serializable {
        CHARACTER, BUILDING, EVENT;
        @Serial private static final long serialVersionUID = 1L;
    }

    /** Creates a snapshot from a concrete card. */
    public static CardSnapshot of(Card card) {
        if (card instanceof Character c) {
            return new CardSnapshot(
                    c.getId(), c.getEra(), Type.CHARACTER,
                    c.getType(), 0, 0, "");
        }
        if (card instanceof Building b) {
            return new CardSnapshot(
                    b.getId(), b.getEra(), Type.BUILDING,
                    null, b.getFoodCost(), b.getFinalPoints(),
                    b.getEffectDescription());
        }
        return new CardSnapshot(card.getId(), card.getEra(), Type.EVENT, null, 0, 0, "");
    }

    public boolean isCharacter() { return type == Type.CHARACTER; }
    public boolean isBuilding()  { return type == Type.BUILDING; }
    public boolean isEvent()     { return type == Type.EVENT; }
}