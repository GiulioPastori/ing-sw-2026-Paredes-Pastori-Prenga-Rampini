package it.polimi.it.view.gui;

import javafx.application.Platform;

/**
 * Shared utilities for JavaFX controllers.
 */
public final class FxUtils {

    private FxUtils() { /* utility */ }

    /**
     * Runs the task on the JavaFX Application Thread.
     * If the current thread is already the FX thread, the task runs immediately;
     * otherwise it is scheduled with {@link Platform#runLater(Runnable)}.
     */
    public static void runFx(Runnable r) {
        if (Platform.isFxApplicationThread()) r.run();
        else Platform.runLater(r);
    }
}
