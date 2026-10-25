package uk.ac.ntu.eprms.domain;

/**
 * How serious a {@link BugTask} is. Stored as a name so a saved file stays readable.
 */
public enum Severity {

    /** Minor issue that does not block other work. */
    LOW,

    /** Normal defect. */
    MEDIUM,

    /** Important defect that should be fixed soon. */
    HIGH,

    /** Defect that blocks delivery. */
    CRITICAL
}
