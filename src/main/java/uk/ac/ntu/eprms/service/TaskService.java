package uk.ac.ntu.eprms.service;

import uk.ac.ntu.eprms.domain.Task;
import uk.ac.ntu.eprms.domain.TaskStatus;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

/**
 * Search, filter, sort and "what to do next" for a list of tasks.
 * Methods take a list and return a new list so they can be unit tested without files or JavaFX.
 * Filtering and sorting walk the list once and then sort, so they are O(n) or O(n log n).
 */
public class TaskService {

    /**
     * Finds tasks whose title contains the query, ignoring letter case.
     * A null or blank query returns a copy of every task.
     *
     * @param tasks tasks to search; must not be null
     * @param query text to look for
     * @return matching tasks, in the original order
     */
    public List<Task> searchByTitle(List<Task> tasks, String query) {
        Objects.requireNonNull(tasks, "tasks");
        if (query == null || query.isBlank()) {
            return List.copyOf(tasks);
        }
        String needle = query.trim().toLowerCase(Locale.ROOT);
        return tasks.stream()
                .filter(task -> task.getTitle().toLowerCase(Locale.ROOT).contains(needle))
                .toList();
    }

    /**
     * Keeps tasks with the given status.
     *
     * @param tasks  tasks to filter; must not be null
     * @param status status to keep; null returns a copy of every task
     * @return matching tasks
     */
    public List<Task> filterByStatus(List<Task> tasks, TaskStatus status) {
        Objects.requireNonNull(tasks, "tasks");
        if (status == null) {
            return List.copyOf(tasks);
        }
        return tasks.stream()
                .filter(task -> task.getStatus() == status)
                .toList();
    }

    /**
     * Keeps tasks with this exact priority.
     *
     * @param tasks    tasks to filter; must not be null
     * @param priority priority from 1 to 5
     * @return matching tasks
     */
    public List<Task> filterByPriority(List<Task> tasks, int priority) {
        Objects.requireNonNull(tasks, "tasks");
        return tasks.stream()
                .filter(task -> task.getPriority() == priority)
                .toList();
    }

    /**
     * Keeps tasks assigned to one member. A null id keeps tasks that have no assignee.
     *
     * @param tasks    tasks to filter; must not be null
     * @param memberId member to match, or null for unassigned tasks
     * @return matching tasks
     */
    public List<Task> filterByMember(List<Task> tasks, UUID memberId) {
        Objects.requireNonNull(tasks, "tasks");
        return tasks.stream()
                .filter(task -> Objects.equals(task.getAssigneeId(), memberId))
                .toList();
    }

    /**
     * Sorts by due date, earliest first. A missing date goes last.
     *
     * @param tasks tasks to sort; must not be null
     * @return a new sorted list
     */
    public List<Task> sortByDueDate(List<Task> tasks) {
        Objects.requireNonNull(tasks, "tasks");
        return tasks.stream()
                .sorted(Comparator.comparing(Task::getDueDate, Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
    }

    /**
     * Sorts by priority, highest first.
     *
     * @param tasks tasks to sort; must not be null
     * @return a new sorted list
     */
    public List<Task> sortByPriority(List<Task> tasks) {
        Objects.requireNonNull(tasks, "tasks");
        return tasks.stream()
                .sorted(Comparator.comparingInt(Task::getPriority).reversed())
                .toList();
    }

    /**
     * Returns tasks that are not done, ranked by the given strategy.
     *
     * @param tasks    the full list of tasks; must not be null
     * @param strategy how to rank the remaining tasks; must not be null
     * @return a new list of open tasks, never null
     */
    public List<Task> getNextTasks(List<Task> tasks, PrioritisationStrategy strategy) {
        Objects.requireNonNull(tasks, "tasks");
        Objects.requireNonNull(strategy, "strategy");
        List<Task> open = tasks.stream()
                .filter(task -> task.getStatus() != TaskStatus.DONE)
                .toList();
        return strategy.prioritise(open);
    }
}
