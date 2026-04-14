package it.polimi.it.network.message;

import it.polimi.it.enums.Era;
import it.polimi.it.enums.EventType;
import it.polimi.it.observer.CardSnapshot;
import it.polimi.it.observer.OfferTileSnapshot;
import it.polimi.it.observer.PlayerSnapshot;

import java.util.List;

/** Server → Client: lobby status update. */
 record LobbyUpdateMessage(
        List<String> connectedPlayers,
        int          expectedPlayers
) implements Message {}

/** Server → Client: action rejected or protocol error. */
public record ErrorMessage(String reason) implements Message {}

/** Server → Client: a new round has started. */
 record RoundStartedMessage(
        int                     round,
        List<PlayerSnapshot>    turnOrder,
        List<OfferTileSnapshot> availableTiles
) implements Message {}

/** Server → Client: a player placed their totem. */
 record TotemPlacedMessage(
        PlayerSnapshot          player,
        char                    tileLetter,
        List<OfferTileSnapshot> remainingTiles
) implements Message {}

/** Server → Client: a card slot is ready for the indicated player. */
 record CardSlotReadyMessage(
        PlayerSnapshot     player,
        boolean            isTopRow,
        List<CardSnapshot> availableCards,
        int                slotsRemaining
) implements Message {}

/** Server → Client: a player acquired a card. */
record CardAcquiredMessage(
        PlayerSnapshot player,
        CardSnapshot   card
) implements Message {}

/** Server → Client: a player could not afford a building. */
 record CardAcquisitionFailedMessage(
        PlayerSnapshot     player,
        CardSnapshot       card,
        int                missingFood,
        boolean            fromTopRow,
        List<CardSnapshot> alternatives
) implements Message {}

/** Server → Client: a player's totem was returned to the Turn Order Tile. */
 record TotemReturnedMessage(
        PlayerSnapshot player,
        int            position
) implements Message {}

/** Server → Client: post-round phase started (Watchtower). */
 record PostRoundStartedMessage(
        int                  round,
        List<PlayerSnapshot> watchtowerPlayers,
        List<CardSnapshot>   availableCards
) implements Message {}

/** Server → Client: an event card was resolved. */
record EventResolvedMessage(
        int       eventId,
        Era       era,
        EventType type
) implements Message {}

/** Server → Client: a new era has begun. */
record EraChangedMessage(Era newEra) implements Message {}

/** Server → Client: a round has ended. */
 record RoundEndedMessage(int round) implements Message {}

/** Server → Client: the game is over. */
 record GameEndedMessage(
        List<PlayerSnapshot> winners,
        List<PlayerSnapshot> finalScores
) implements Message {}

/** Server → Client: a player disconnected; game is terminated. */
 record PlayerDisconnectedMessage(String nickname) implements Message {}

/** Server → Client: heartbeat ping. */
 record PingMessage() implements Message {}

/** Client → Server: heartbeat reply. */
record PongMessage() implements Message {}