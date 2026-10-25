package uk.ac.ntu.eprms.domain;

import java.time.LocalDate;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * Work that fixes a defect. The extra detail is how severe the defect is.
 */
public class BugTask extends Task {

    private Severity severity;

    /**
     * Creates a new bug task that still has to be done.
     *
     * @param title    task title; must not be blank
     * @param priority urgency from 1 to 5
     * @param dueDate  due date; must not be null
     * @param severity how serious the defect is; must not be null
     */
    public BugTask(String title, int priority, LocalDate dueDate, Severity severity) {
        super(title, priority, dueDate);
        this.severity = Objects.requireNonNull(severity, "severity");
    }

    /**
     * Recreates a bug task from stored data.
     *
     * @param id         existing id
     * @param title      task title
     * @param priority   urgency from 1 to 5
     * @param dueDate    due date
     * @param status     current status
     * @param assigneeId member id, or null
     * @param severity   how serious the defect is
     * @param dependsOn  prerequisite task ids
     */
    public BugTask(UUID id, String title, int priority, LocalDate dueDate,
                   TaskStatus status, UUID assigneeId, Severity severity,
                   Set<UUID> dependsOn) {
        super(id, title, priority, dueDate, status, assigneeId, dependsOn);
        this.severity = Objects.requireNonNull(severity, "severity");
    }

    /**
     * Used when a repository rebuilds the object from a file or a database row.
     */
    public BugTask() {
    }

    @Override
    public String getTypeName() {
        return "Bug";
    }

    /**
     * @return how serious this defect is
     */
    public Severity getSeverity() {
        return severity;
    }

    /**
     * @param severity new severity; must not be null
     */
    public void setSeverity(Severity severity) {
        this.severity = Objects.requireNonNull(severity, "severity");
    }

    @Override
    public void validate() {
        super.validate();
        Objects.requireNonNull(severity, "severity");
    }
}
