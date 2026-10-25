package uk.ac.ntu.eprms.service;

import uk.ac.ntu.eprms.domain.Task;

import java.util.Comparator;
import java.util.List;

/**
 * Ranks tasks so the highest priority number comes first. 5 is more urgent than 1.
 * Comparison is O(n log n) because it sorts the list.
 */
public class HighestPriorityStrategy implements PrioritisationStrategy {

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Task> prioritise(List<Task> tasks) {
        if (tasks.isEmpty()) {
            return List.of();
        }
        return tasks.stream()
                .sorted(Comparator.comparingInt(Task::getPriority).reversed())
                .toList();
    }
}
