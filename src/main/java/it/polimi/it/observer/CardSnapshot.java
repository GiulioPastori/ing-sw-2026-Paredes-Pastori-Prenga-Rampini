package it.polimi.it.observer;

import it.polimi.it.cards.building.Building;
import it.polimi.it.cards.Card;
import it.polimi.it.cards.character.Artist;
import it.polimi.it.cards.character.Builder;
import it.polimi.it.cards.character.Character;
import it.polimi.it.cards.character.Gatherer;
import it.polimi.it.cards.character.Hunter;
import it.polimi.it.cards.character.Inventor;
import it.polimi.it.cards.character.Shaman;
import it.polimi.it.enums.CharacterType;
import it.polimi.it.enums.Era;

import java.io.Serial;
import java.io.Serializable;

/**
 * Immutable snapshot of a card, carried by GameEvents and sent over the network.
 *
 * <p>Implements Serializable so it can be passed inside Message objects
 * over RMI and via Java object serialisation over sockets.
 *
 * <p>The {@code details} field carries character-specific data that is
 * critical to the player's decision (number of star icons for Shamans,
 * trigger flag for Hunters, invention identifier for Inventors, discount
 * and end-game points for Builders). It is {@code null} for non-character
 * cards. Keeping these fields separate avoids inflating the main record
 * with optional values.
 */
public record CardSnapshot(
        int               id,
        Era               era,
        Type              type,
        CharacterType     characterType,
        int               foodCost,
        int               finalPoints,
        String            effectDescription,
        CharacterDetails  details
) implements Serializable {
    @Serial private static final long serialVersionUID = 2L;

    public enum Type implements Serializable {
        CHARACTER, BUILDING, EVENT;
        @Serial private static final long serialVersionUID = 1L;
    }

    /**
     * Distinguishing data for character cards. The semantics of each field
     * depend on the character type and mirror the printed icons on the card:
     * <ul>
     *   <li>Hunter: {@code triggerIcon}</li>
     *   <li>Shaman: {@code starIcons} (1–3)</li>
     *   <li>Inventor: {@code inventionIcon} (string identifier of which
     *       invention is printed, out of N possible)</li>
     *   <li>Builder: {@code buildingDiscount} and {@code builderPoints}</li>
     *   <li>Gatherer / Artist: all fields are zero/empty (effect is fixed)</li>
     * </ul>
     */
    public record CharacterDetails(
            boolean triggerIcon,
            int     starIcons,
            String  inventionIcon,
            int     buildingDiscount,
            int     builderPoints
    ) implements Serializable {
        @Serial private static final long serialVersionUID = 1L;

        public static CharacterDetails empty() {
            return new CharacterDetails(false, 0, "", 0, 0);
        }
    }

    /** Creates a snapshot from a concrete card. */
    public static CardSnapshot of(Card card) {
        if (card instanceof Character c) {
            return new CardSnapshot(
                    c.getId(), c.getEra(), Type.CHARACTER,
                    c.getType(), 0, 0, "",
                    detailsOf(c));
        }
        if (card instanceof Building b) {
            return new CardSnapshot(
                    b.getId(), b.getEra(), Type.BUILDING,
                    null, b.getFoodCost(), b.getFinalPoints(),
                    b.getEffectDescription(),
                    null);
        }
        return new CardSnapshot(
                card.getId(), card.getEra(), Type.EVENT, null, 0, 0,
                ((it.polimi.it.cards.events.Event) card).getType().name(),
                null);
    }

    private static CharacterDetails detailsOf(Character c) {
        if (c instanceof Hunter h) {
            return new CharacterDetails(h.hasTriggerIcon(), 0, "", 0, 0);
        }
        if (c instanceof Shaman s) {
            return new CharacterDetails(false, s.getStarIcons(), "", 0, 0);
        }
        if (c instanceof Inventor i) {
            return new CharacterDetails(false, 0, i.getInventionIcon(), 0, 0);
        }
        if (c instanceof Builder b) {
            return new CharacterDetails(false, 0, "",
                    b.getBuildingDiscount(), b.getEndGamePoints());
        }
        // Gatherer, Artist: no distinguishing data
        return CharacterDetails.empty();
    }

    public boolean isCharacter() { return type == Type.CHARACTER; }
    public boolean isBuilding()  { return type == Type.BUILDING; }
    public boolean isEvent()     { return type == Type.EVENT; }
}