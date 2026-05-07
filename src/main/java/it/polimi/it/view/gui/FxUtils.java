package it.polimi.it.view.gui;

import javafx.application.Platform;

/**
 * Utilità condivise per i controller JavaFX.
 */
public final class FxUtils {

    private FxUtils() { /* utility */ }

    /**
     * Esegue il task sul JavaFX Application Thread.
     * Se siamo già sul thread FX lo esegue subito, altrimenti usa runLater.
     */
    public static void runFx(Runnable r) {
        if (Platform.isFxApplicationThread()) r.run();
        else Platform.runLater(r);
    }
}