package it.polimi.it.validation;

/**
 * Result of validating a PlayerAction.
 *
 * Instead of throwing unchecked exceptions for invalid user input,
 * Game returns a ValidationResult that the controller can inspect
 * and present to the user with a readable message.
 *
 * Exceptions remain reserved for truly exceptional conditions
 * (internal bugs, corrupted state), not for invalid user input.
 */
public sealed interface ValidationResult permits ValidationResult.Ok, ValidationResult.Fail {

    boolean isOk();

    record Ok() implements ValidationResult {
        public boolean isOk() { return true; }
    }

    record Fail(String reason) implements ValidationResult {
        public boolean isOk() { return false; }
    }

    static ValidationResult ok()           { return new Ok(); }
    static ValidationResult fail(String r) { return new Fail(r); }
}
