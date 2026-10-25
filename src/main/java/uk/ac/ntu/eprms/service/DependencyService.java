package uk.ac.ntu.eprms.service;

import uk.ac.ntu.eprms.domain.Task;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Orders tasks so that a task appears after the tasks it depends on.
 * The algorithm is Kahn's topological sort. It visits each task and each dependency
 * a constant number of times, so the running time is O(V + E): V tasks and E dependency links.
 */
public class DependencyService {

    /**
     * Builds a valid working order for the given tasks.
     * A dependency that points at a task outside this list is ignored, because that
     * work is not part of the set being ordered. A task that depends on itself is a cycle.
     *
     * @param tasks tasks to order; must not be null
     * @return tasks in an order that respects dependencies
     * @throws CircularDependencyException when the links form a loop
     */
    public List<Task> workingOrder(List<Task> tasks) {
        if (tasks.isEmpty()) {
            return List.of();
        }

        Map<UUID, Task> byId = new HashMap<>();
        for (Task task : tasks) {
            byId.put(task.getId(), task);
        }

        Map<UUID, Integer> indegree = new HashMap<>();
        Map<UUID, List<UUID>> dependents = new HashMap<>();
        for (UUID id : byId.keySet()) {
            indegree.put(id, 0);
            dependents.put(id, new ArrayList<>());
        }

        for (Task task : tasks) {
            for (UUID prerequisiteId : task.getDependsOn()) {
                if (prerequisiteId.equals(task.getId())) {
                    throw new CircularDependencyException(
                            "\"" + task.getTitle() + "\" depends on itself.");
                }
                if (!byId.containsKey(prerequisiteId)) {
                    continue;
                }
                indegree.merge(task.getId(), 1, Integer::sum);
                dependents.get(prerequisiteId).add(task.getId());
            }
        }

        ArrayDeque<UUID> ready = new ArrayDeque<>();
        for (Task task : tasks) {
            if (indegree.get(task.getId()) == 0) {
                ready.add(task.getId());
            }
        }

        List<Task> ordered = new ArrayList<>();
        while (!ready.isEmpty()) {
            UUID id = ready.removeFirst();
            ordered.add(byId.get(id));
            for (UUID dependentId : dependents.get(id)) {
                int remaining = indegree.merge(dependentId, -1, Integer::sum);
                if (remaining == 0) {
                    ready.add(dependentId);
                }
            }
        }

        if (ordered.size() != byId.size()) {
            throw new CircularDependencyException(
                    "These tasks depend on each other in a loop, so there is no valid order.");
        }
        return List.copyOf(ordered);
    }
}
