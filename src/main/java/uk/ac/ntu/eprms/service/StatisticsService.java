package uk.ac.ntu.eprms.service;

import uk.ac.ntu.eprms.domain.Project;
import uk.ac.ntu.eprms.domain.Task;
import uk.ac.ntu.eprms.domain.TaskStatus;

import java.time.LocalDate;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Summary figures for projects and tasks. Every figure is computed with streams
 * so the same rules can be tested without opening the window.
 */
public class StatisticsService {

    /**
     * Percentage of tasks in the project that are done. An empty project is 0, not an error.
     *
     * @param project project to measure; must not be null
     * @return a value from 0 to 100
     */
    public double completionPercentage(Project project) {
        Objects.requireNonNull(project, "project");
        List<Task> tasks = project.getTasks();
        if (tasks.isEmpty()) {
            return 0.0;
        }
        long done = tasks.stream()
                .filter(task -> task.getStatus() == TaskStatus.DONE)
                .count();
        return (done * 100.0) / tasks.size();
    }

    /**
     * Counts tasks in each status. Statuses that do not appear are returned as zero
     * so a chart can still show all three columns.
     *
     * @param tasks tasks to count; must not be null
     * @return a map with an entry for every {@link TaskStatus}
     */
    public Map<TaskStatus, Long> countByStatus(List<Task> tasks) {
        Objects.requireNonNull(tasks, "tasks");
        Map<TaskStatus, Long> grouped = tasks.stream()
                .collect(Collectors.groupingBy(Task::getStatus, Collectors.counting()));
        Map<TaskStatus, Long> complete = new EnumMap<>(TaskStatus.class);
        for (TaskStatus status : TaskStatus.values()) {
            complete.put(status, grouped.getOrDefault(status, 0L));
        }
        return complete;
    }

    /**
     * Tasks whose due date is before today and that are not done.
     * A task due today is not overdue.
     *
     * @param tasks tasks to check; must not be null
     * @return overdue tasks, in the original order
     */
    public List<Task> overdueTasks(List<Task> tasks) {
        return overdueTasks(tasks, LocalDate.now());
    }

    /**
     * Same rule as {@link #overdueTasks(List)}, with an explicit "today" so tests do not
     * depend on the clock.
     *
     * @param tasks tasks to check; must not be null
     * @param today the date to compare against
     * @return overdue tasks
     */
    public List<Task> overdueTasks(List<Task> tasks, LocalDate today) {
        Objects.requireNonNull(tasks, "tasks");
        Objects.requireNonNull(today, "today");
        return tasks.stream()
                .filter(task -> task.getStatus() != TaskStatus.DONE)
                .filter(task -> task.getDueDate() != null && task.getDueDate().isBefore(today))
                .toList();
    }

    /**
     * Counts every assigned task, including finished ones. Unassigned tasks are skipped.
     *
     * @param tasks tasks to count; must not be null
     * @return member id to number of tasks
     */
    public Map<UUID, Long> tasksByMember(List<Task> tasks) {
        Objects.requireNonNull(tasks, "tasks");
        return tasks.stream()
                .filter(task -> task.getAssigneeId() != null)
                .collect(Collectors.groupingBy(Task::getAssigneeId, Collectors.counting()));
    }

    /**
     * Counts tasks that are not done, grouped by assignee. This is the workload figure.
     *
     * @param tasks tasks to count; must not be null
     * @return member id to number of active tasks
     */
    public Map<UUID, Long> activeTasksByMember(List<Task> tasks) {
        Objects.requireNonNull(tasks, "tasks");
        return tasks.stream()
                .filter(task -> task.getAssigneeId() != null)
                .filter(task -> task.getStatus() != TaskStatus.DONE)
                .collect(Collectors.groupingBy(Task::getAssigneeId, Collectors.counting()));
    }
}
