package it.polimi.it.network.message;

import it.polimi.it.enums.Era;

/** Server → Client: a new era has begun. */
public record EraChangedMessage(Era newEra) implements Message {}
