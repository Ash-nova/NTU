package uk.ac.ntu.eprms.domain;

import java.time.LocalDate;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * A unit of work inside a {@link Project}. Concrete kinds of work extend this class
 * so each kind can carry its own details while sharing title, priority, due date and status.
 * <p>
 * This class does not read files or talk to JavaFX. Persistence code maps it from the outside.
 */
public abstract class Task {

    /** Lowest allowed priority. 1 is the least urgent. */
    public static final int MIN_PRIORITY = 1;

    /** Highest allowed priority. 5 is the most urgent. */
    public static final int MAX_PRIORITY = 5;

    private UUID id;
    private String title;
    private int priority;
    private LocalDate dueDate;
    private TaskStatus status;
    private UUID assigneeId;
    private Set<UUID> dependsOn = new HashSet<>();

    /**
     * Creates a task with a new id and status {@link TaskStatus#TO_DO}.
     *
     * @param title    short name of the work; must not be blank
     * @param priority urgency from {@link #MIN_PRIORITY} to {@link #MAX_PRIORITY}
     * @param dueDate  date the work should be finished; must not be null
     */
    protected Task(String title, int priority, LocalDate dueDate) {
        this(UUID.randomUUID(), title, priority, dueDate, TaskStatus.TO_DO, null, Set.of());
    }

    /**
     * Recreates a task that already has an id, for example when a file is loaded.
     *
     * @param id         existing id; must not be null
     * @param title      short name; must not be blank
     * @param priority   urgency from 1 to 5
     * @param dueDate    due date; must not be null
     * @param status     current status; must not be null
     * @param assigneeId member who owns the task, or null if nobody is assigned
     * @param dependsOn  ids of tasks that must be finished first; null is treated as none
     */
    protected Task(UUID id, String title, int priority, LocalDate dueDate,
                   TaskStatus status, UUID assigneeId, Set<UUID> dependsOn) {
        this.id = Objects.requireNonNull(id, "id");
        this.title = cleanTitle(title);
        this.priority = checkPriority(priority);
        this.dueDate = Objects.requireNonNull(dueDate, "due date");
        this.status = Objects.requireNonNull(status, "status");
        this.assigneeId = assigneeId;
        if (dependsOn != null) {
            this.dependsOn.addAll(dependsOn);
        }
    }

    /**
     * Used by the persistence layer when it builds a task field by field.
     */
    protected Task() {
        this.id = UUID.randomUUID();
        this.status = TaskStatus.TO_DO;
    }

    /**
     * Returns the kind of task, for example "Feature" or "Bug".
     *
     * @return a short label that is never null
     */
    public abstract String getTypeName();

    /**
     * Checks that this task and any subclass fields are complete.
     * Repositories call this after reading a file.
     */
    public void validate() {
        cleanTitle(title);
        checkPriority(priority);
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(dueDate, "due date");
        Objects.requireNonNull(status, "status");
    }

    /**
     * @return stable id of this task
     */
    public UUID getId() {
        return id;
    }

    /**
     * @return title, never blank once the task has been validated
     */
    public String getTitle() {
        return title;
    }

    /**
     * @param title new title; must not be blank
     */
    public void setTitle(String title) {
        this.title = cleanTitle(title);
    }

    /**
     * @return priority from 1 (low) to 5 (high)
     */
    public int getPriority() {
        return priority;
    }

    /**
     * @param priority new priority from 1 to 5
     */
    public void setPriority(int priority) {
        this.priority = checkPriority(priority);
    }

    /**
     * @return the date this task is due
     */
    public LocalDate getDueDate() {
        return dueDate;
    }

    /**
     * @param dueDate new due date; must not be null
     */
    public void setDueDate(LocalDate dueDate) {
        this.dueDate = Objects.requireNonNull(dueDate, "due date");
    }

    /**
     * @return current status
     */
    public TaskStatus getStatus() {
        return status;
    }

    /**
     * @param status new status; must not be null
     */
    public void setStatus(TaskStatus status) {
        this.status = Objects.requireNonNull(status, "status");
    }

    /**
     * @return id of the assigned member, or null when the task is unassigned
     */
    public UUID getAssigneeId() {
        return assigneeId;
    }

    /**
     * @param assigneeId member id, or null to clear the assignee
     */
    public void setAssigneeId(UUID assigneeId) {
        this.assigneeId = assigneeId;
    }

    /**
     * @return ids of tasks that should be finished before this one; never null
     */
    public Set<UUID> getDependsOn() {
        if (dependsOn == null) {
            dependsOn = new HashSet<>();
        }
        return Collections.unmodifiableSet(dependsOn);
    }

    /**
     * Replaces the dependency set. A task should not depend on itself; the dependency
     * service reports that as a cycle.
     *
     * @param dependsOn new ids; null clears the set
     */
    public void setDependsOn(Set<UUID> dependsOn) {
        this.dependsOn = new HashSet<>();
        if (dependsOn != null) {
            this.dependsOn.addAll(dependsOn);
        }
    }

    /**
     * @return true when the due date is before today and the task is not done
     */
    public boolean isOverdue() {
        return status != TaskStatus.DONE
                && dueDate != null
                && dueDate.isBefore(LocalDate.now());
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Task task)) {
            return false;
        }
        return id.equals(task.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return title;
    }

    private static String cleanTitle(String title) {
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("Title is required");
        }
        return title.trim();
    }

    private static int checkPriority(int priority) {
        if (priority < MIN_PRIORITY || priority > MAX_PRIORITY) {
            throw new IllegalArgumentException(
                    "Priority must be between " + MIN_PRIORITY + " and " + MAX_PRIORITY);
        }
        return priority;
    }
}
