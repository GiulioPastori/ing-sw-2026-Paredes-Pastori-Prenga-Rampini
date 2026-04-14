package it.polimi.it.network.message;

import it.polimi.it.enums.Era;
import it.polimi.it.enums.EventType;

/** Server → Client: an event card was resolved. */
public record EventResolvedMessage(
        int       eventId,
        Era       era,
        EventType type
) implements Message {}
