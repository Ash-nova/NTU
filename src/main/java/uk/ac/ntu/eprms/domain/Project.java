package uk.ac.ntu.eprms.domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * A named piece of client work and the ordered tasks that make it up.
 * The project owns its tasks: deleting the project drops the tasks with it.
 */
public class Project {

    private UUID id;
    private String name;
    private String description;
    private List<Task> tasks = new ArrayList<>();

    /**
     * Creates a project with a new id and no tasks.
     *
     * @param name        project name; must not be blank
     * @param description longer note; null is stored as an empty string
     */
    public Project(String name, String description) {
        this(UUID.randomUUID(), name, description);
    }

    /**
     * Recreates a project that already has an id.
     *
     * @param id          existing id; must not be null
     * @param name        project name; must not be blank
     * @param description longer note; null is stored as an empty string
     */
    public Project(UUID id, String name, String description) {
        this.id = Objects.requireNonNull(id, "id");
        this.name = cleanName(name);
        this.description = description == null ? "" : description.trim();
    }

    /**
     * Used when a repository rebuilds the object from a file or a database row.
     */
    public Project() {
        this.id = UUID.randomUUID();
        this.description = "";
    }

    /**
     * Checks the project and every task it contains.
     */
    public void validate() {
        Objects.requireNonNull(id, "id");
        cleanName(name);
        for (Task task : tasks) {
            task.validate();
        }
    }

    /**
     * @return stable id of this project
     */
    public UUID getId() {
        return id;
    }

    /**
     * @return project name
     */
    public String getName() {
        return name;
    }

    /**
     * @param name new name; must not be blank
     */
    public void setName(String name) {
        this.name = cleanName(name);
    }

    /**
     * @return description, which may be empty but is never null
     */
    public String getDescription() {
        return description == null ? "" : description;
    }

    /**
     * @param description new description; null is stored as an empty string
     */
    public void setDescription(String description) {
        this.description = description == null ? "" : description.trim();
    }

    /**
     * Adds a task at the end of the list.
     *
     * @param task task to add; must not be null
     */
    public void addTask(Task task) {
        Objects.requireNonNull(task, "task");
        tasks.add(task);
    }

    /**
     * Removes the task with the given id.
     *
     * @param taskId id to remove
     * @return true if a task was removed
     */
    public boolean removeTask(UUID taskId) {
        boolean removed = tasks.removeIf(task -> task.getId().equals(taskId));
        if (removed) {
            for (Task task : tasks) {
                if (task.getDependsOn().contains(taskId)) {
                    SetCopy.without(task, taskId);
                }
            }
        }
        return removed;
    }

    /**
     * Replaces the task that has the same id, keeping its position in the list.
     *
     * @param updated replacement; must not be null and must match an existing id
     * @return true if a task was replaced
     */
    public boolean replaceTask(Task updated) {
        Objects.requireNonNull(updated, "task");
        for (int i = 0; i < tasks.size(); i++) {
            if (tasks.get(i).getId().equals(updated.getId())) {
                tasks.set(i, updated);
                return true;
            }
        }
        return false;
    }

    /**
     * @return tasks in list order; the returned list cannot be modified
     */
    public List<Task> getTasks() {
        if (tasks == null) {
            tasks = new ArrayList<>();
        }
        return Collections.unmodifiableList(tasks);
    }

    /**
     * Finds a task in this project.
     *
     * @param taskId id to find
     * @return the task, or null if this project does not contain it
     */
    public Task findTask(UUID taskId) {
        for (Task task : tasks) {
            if (task.getId().equals(taskId)) {
                return task;
            }
        }
        return null;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Project project)) {
            return false;
        }
        return id.equals(project.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return name;
    }

    private static String cleanName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Project name is required");
        }
        return name.trim();
    }

    /**
     * Small helper so {@link #removeTask(UUID)} can drop a deleted id from the
     * remaining dependency sets without exposing a mutable set.
     */
    private static final class SetCopy {
        private SetCopy() {
        }

        private static void without(Task task, UUID removedId) {
            var remaining = new java.util.HashSet<>(task.getDependsOn());
            remaining.remove(removedId);
            task.setDependsOn(remaining);
        }
    }
}
