package it.polimi.it.validation;

import it.polimi.it.TileResolutionState;
import it.polimi.it.actions.*;
import it.polimi.it.player.Player;
import it.polimi.it.tiles.OfferTile;

import java.util.List;

/**
 * Validates PlayerActions before Game applies them to the model.
 * Centralises all admissibility rules in one place, keeping Game.execute()
 * readable. Each method returns a ValidationResult: Ok if the action is
 * legal, Fail with a readable message otherwise. */
public final class ActionValidator {

    private ActionValidator() {}

    public static ValidationResult validate(PlaceTotemAction action,
                                            Player player,
                                            List<OfferTile> freeTiles) {
        boolean found = freeTiles.stream()
                .anyMatch(t -> t.getLetter() == action.tileLetter());
        if (!found) {
            return ValidationResult.fail(
                    "Tile '" + action.tileLetter() + "' is not available.");
        }
        return ValidationResult.ok();
    }

    public static ValidationResult validate(TakeCardAction action,
                                            Player player,
                                            TileResolutionState state) {
        if (!state.isActingPlayer(player)) {
            return ValidationResult.fail(
                    player.getName() + " is not the active player right now.");
        }
        if (action.fromTopRow() && state.remainingTopCards() == 0) {
            return ValidationResult.fail(
                    "No top-row slot available for this tile.");
        }
        if (!action.fromTopRow() && state.remainingBottomCards() == 0) {
            return ValidationResult.fail(
                    "No bottom-row slot available for this tile.");
        }
        return ValidationResult.ok();
    }

    public static ValidationResult validate(PassAction action,
                                            Player player,
                                            TileResolutionState state) {
        if (!state.isActingPlayer(player)) {
            return ValidationResult.fail(
                    player.getName() + " is not the active player right now.");
        }
        return ValidationResult.ok();
    }

    public static ValidationResult validate(PostRoundAction action,
                                            Player player,
                                            List<Player> pendingPlayers) {
        if (!pendingPlayers.contains(player)) {
            return ValidationResult.fail(
                    player.getName() + " does not own an active Watchtower building.");
        }
        return ValidationResult.ok();
    }
}
