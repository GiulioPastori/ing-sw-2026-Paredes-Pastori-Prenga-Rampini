package it.polimi.it.observer;

import java.io.Serial;
import java.io.Serializable;

/* Immutable resource delta for one player after a resolved event. */

public record PlayerResourceDelta(
        String playerName,
        int foodBefore,
        int foodAfter,
        int prestigeBefore,
        int prestigeAfter
) implements Serializable {
    @Serial private static final long serialVersionUID = 1L;

    public int foodDelta() {
        return foodAfter - foodBefore;
    }

    public int prestigeDelta() {
        return prestigeAfter - prestigeBefore;
    }
}
