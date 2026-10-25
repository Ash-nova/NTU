package uk.ac.ntu.eprms.service;

/**
 * Raised when task dependencies cannot be ordered because they contain a cycle.
 */
public class CircularDependencyException extends RuntimeException {

    /**
     * @param message a sentence that can be shown to the user
     */
    public CircularDependencyException(String message) {
        super(message);
    }
}
