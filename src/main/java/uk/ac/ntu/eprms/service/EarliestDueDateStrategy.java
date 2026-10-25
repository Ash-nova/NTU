package uk.ac.ntu.eprms.service;

import uk.ac.ntu.eprms.domain.Task;

import java.util.Comparator;
import java.util.List;

/**
 * Ranks tasks so the soonest due date comes first. A missing due date is placed last.
 * Comparison is O(n log n) because it sorts the list.
 */
public class EarliestDueDateStrategy implements PrioritisationStrategy {

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Task> prioritise(List<Task> tasks) {
        if (tasks.isEmpty()) {
            return List.of();
        }
        return tasks.stream()
                .sorted(Comparator.comparing(Task::getDueDate, Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
    }
}
