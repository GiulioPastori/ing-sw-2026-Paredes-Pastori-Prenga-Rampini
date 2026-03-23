package it.polimi.it.observer;

import it.polimi.it.cards.building.Building;
import it.polimi.it.cards.Card;
import it.polimi.it.cards.character.Character;
import it.polimi.it.enums.CharacterType;
import it.polimi.it.enums.Era;

/**
 * Immutable snapshot of a card, carried by GameEvents and sent over the network.
 *
 * Cards in the physical game have no printed name, so the integer {@code id}
 * is the natural identifier. It is compact for serialisation and unambiguous.
 *
 * The {@code type} field discriminates the card kind:
 * - CHARACTER: {@code characterType} is set
 * - BUILDING:  {@code foodCost} and {@code finalPoints} are set
 * - EVENT:     not selectable by players
 */
public record CardSnapshot(
        int           id,
        Era           era,
        Type          type,
        CharacterType characterType,
        int           foodCost,
        int           finalPoints,
        String        effectDescription
) {
    public enum Type { CHARACTER, BUILDING, EVENT }

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