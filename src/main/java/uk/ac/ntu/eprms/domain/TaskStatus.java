package uk.ac.ntu.eprms.domain;

/**
 * Lifecycle of a task. A task moves from {@link #TO_DO} to {@link #IN_PROGRESS}
 * and then to {@link #DONE}. Completed tasks are left out of the "what to do next" list.
 */
public enum TaskStatus {

    /** Work has not started. */
    TO_DO,

    /** Someone is working on the task. */
    IN_PROGRESS,

    /** The task is finished. */
    DONE
}
