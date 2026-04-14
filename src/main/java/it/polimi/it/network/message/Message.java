package it.polimi.it.network.message;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * Sealed marker interface for all network messages.
 *
 * Every message is serialised as a single JSON line with a "type" discriminator.
 * This makes them safe to use with BufferedReader.readLine() over a socket.
 *
 * Client → Server: JoinMessage, PlaceTotemMessage, TakeCardMessage,
 *                  PassMessage, PostRoundMessage
 * Server → Client: all others
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = JoinMessage.class,                    name = "JOIN"),
        @JsonSubTypes.Type(value = PlaceTotemMessage.class,              name = "PLACE_TOTEM"),
        @JsonSubTypes.Type(value = TakeCardMessage.class,                name = "TAKE_CARD"),
        @JsonSubTypes.Type(value = PassMessage.class,                    name = "PASS"),
        @JsonSubTypes.Type(value = PostRoundMessage.class,               name = "POST_ROUND"),
        @JsonSubTypes.Type(value = LobbyUpdateMessage.class,             name = "LOBBY_UPDATE"),
        @JsonSubTypes.Type(value = ErrorMessage.class,                   name = "ERROR"),
        @JsonSubTypes.Type(value = RoundStartedMessage.class,            name = "ROUND_STARTED"),
        @JsonSubTypes.Type(value = TotemPlacedMessage.class,             name = "TOTEM_PLACED"),
        @JsonSubTypes.Type(value = CardSlotReadyMessage.class,           name = "CARD_SLOT_READY"),
        @JsonSubTypes.Type(value = CardAcquiredMessage.class,            name = "CARD_ACQUIRED"),
        @JsonSubTypes.Type(value = CardAcquisitionFailedMessage.class,   name = "CARD_ACQUISITION_FAILED"),
        @JsonSubTypes.Type(value = TotemReturnedMessage.class,           name = "TOTEM_RETURNED"),
        @JsonSubTypes.Type(value = PostRoundStartedMessage.class,        name = "POST_ROUND_STARTED"),
        @JsonSubTypes.Type(value = EventResolvedMessage.class,           name = "EVENT_RESOLVED"),
        @JsonSubTypes.Type(value = EraChangedMessage.class,              name = "ERA_CHANGED"),
        @JsonSubTypes.Type(value = RoundEndedMessage.class,              name = "ROUND_ENDED"),
        @JsonSubTypes.Type(value = GameEndedMessage.class,               name = "GAME_ENDED"),
        @JsonSubTypes.Type(value = PlayerDisconnectedMessage.class,      name = "PLAYER_DISCONNECTED"),
        @JsonSubTypes.Type(value = PingMessage.class,                    name = "PING"),
        @JsonSubTypes.Type(value = PongMessage.class,                    name = "PONG"),
})
public sealed interface Message permits CardAcquiredMessage, CardAcquisitionFailedMessage, CardSlotReadyMessage, EraChangedMessage, ErrorMessage, EventResolvedMessage, GameEndedMessage, JoinMessage, LobbyUpdateMessage, PassMessage, PingMessage, PlaceTotemMessage, PlayerDisconnectedMessage, PongMessage, PostRoundMessage, PostRoundStartedMessage, RoundEndedMessage, RoundStartedMessage, TakeCardMessage, TotemPlacedMessage, TotemReturnedMessage {}
