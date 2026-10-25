package uk.ac.ntu.eprms.service;

import uk.ac.ntu.eprms.domain.Task;

import java.util.List;

/**
 * One way of ranking open tasks. Callers can swap implementations at runtime
 * without changing the code that shows the ranked list. This is the Strategy pattern.
 */
public interface PrioritisationStrategy {

    /**
     * Returns a new list ordered by this strategy. The input list is not changed.
     * Completed tasks are not removed here; {@code TaskService#getNextTasks} does that.
     *
     * @param tasks tasks to rank; must not be null
     * @return a new list in ranked order, or an empty list when {@code tasks} is empty
     */
    List<Task> prioritise(List<Task> tasks);
}
