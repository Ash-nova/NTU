package uk.ac.ntu.eprms.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DomainTest {

    @Test
    @DisplayName("A feature task keeps its area and type name")
    void featureTaskStoresItsArea() {
        FeatureTask task = new FeatureTask("Home page", 3, LocalDate.of(2026, 11, 2), "UI");

        assertEquals("Home page", task.getTitle());
        assertEquals(3, task.getPriority());
        assertEquals(LocalDate.of(2026, 11, 2), task.getDueDate());
        assertEquals(TaskStatus.TO_DO, task.getStatus());
        assertEquals("UI", task.getFeatureArea());
        assertEquals("Feature", task.getTypeName());
    }

    @Test
    @DisplayName("A bug task keeps its severity and type name")
    void bugTaskStoresSeverity() {
        BugTask task = new BugTask("Login fails", 5, LocalDate.of(2026, 10, 21), Severity.CRITICAL);

        assertEquals(Severity.CRITICAL, task.getSeverity());
        assertEquals("Bug", task.getTypeName());
        assertEquals(5, task.getPriority());
    }

    @Test
    @DisplayName("A blank title is rejected")
    void rejectsBlankTitle() {
        assertThrows(IllegalArgumentException.class,
                () -> new FeatureTask("  ", 2, LocalDate.now(), "UI"));
    }

    @Test
    @DisplayName("Priority outside 1 to 5 is rejected")
    void rejectsPriorityOutOfRange() {
        assertThrows(IllegalArgumentException.class,
                () -> new BugTask("Crash", 0, LocalDate.now(), Severity.LOW));
        assertThrows(IllegalArgumentException.class,
                () -> new BugTask("Crash", 6, LocalDate.now(), Severity.LOW));
    }

    @Test
    @DisplayName("A project can add and remove a task")
    void projectAddAndRemoveTask() {
        Project project = new Project("Website", "Client site");
        FeatureTask task = new FeatureTask("Home page", 2, LocalDate.now().plusDays(3), "UI");

        project.addTask(task);

        assertEquals(1, project.getTasks().size());
        assertTrue(project.removeTask(task.getId()));
        assertTrue(project.getTasks().isEmpty());
        assertFalse(project.removeTask(task.getId()));
    }

    @Test
    @DisplayName("A new project has no tasks")
    void emptyProjectHasNoTasks() {
        Project project = new Project("Empty", null);

        assertTrue(project.getTasks().isEmpty());
        assertEquals("", project.getDescription());
    }

    @Test
    @DisplayName("Callers cannot modify the task list returned by the project")
    void taskListCannotBeModifiedFromOutside() {
        Project project = new Project("Website", "");
        project.addTask(new FeatureTask("Home page", 2, LocalDate.now(), "UI"));

        assertThrows(UnsupportedOperationException.class,
                () -> project.getTasks().add(new BugTask("Bug", 1, LocalDate.now(), Severity.LOW)));
    }

    @Test
    @DisplayName("Two members with the same id are equal")
    void membersWithSameIdAreEqual() {
        UUID id = UUID.randomUUID();
        TeamMember first = new TeamMember(id, "Ada Lovelace", "ada@ntu.ac.uk", "Developer");
        TeamMember second = new TeamMember(id, "Ada Lovelace", "ada@ntu.ac.uk", "Developer");

        assertEquals(first, second);
        assertEquals(first.hashCode(), second.hashCode());
        assertNotEquals(first, new TeamMember("Grace Hopper", "grace@ntu.ac.uk", "Lead"));
    }

    @Test
    @DisplayName("A task can move from to-do to done")
    void statusCanChange() {
        FeatureTask task = new FeatureTask("Report", 2, LocalDate.now(), "Reports");

        task.setStatus(TaskStatus.IN_PROGRESS);
        assertEquals(TaskStatus.IN_PROGRESS, task.getStatus());
        task.setStatus(TaskStatus.DONE);
        assertEquals(TaskStatus.DONE, task.getStatus());
    }

    @Test
    @DisplayName("Deleting a task removes it from other dependency lists")
    void removingTaskDropsItFromDependencies() {
        Project project = new Project("Website", "");
        FeatureTask first = new FeatureTask("Design", 2, LocalDate.now(), "UI");
        BugTask second = new BugTask("Broken link", 3, LocalDate.now(), Severity.LOW);
        second.setDependsOn(Set.of(first.getId()));
        project.addTask(first);
        project.addTask(second);

        project.removeTask(first.getId());

        assertTrue(second.getDependsOn().isEmpty());
    }

    @Test
    @DisplayName("A bad email address is rejected")
    void rejectsBadEmail() {
        assertThrows(IllegalArgumentException.class,
                () -> new TeamMember("Ada", "not-an-email", "Developer"));
    }
}
