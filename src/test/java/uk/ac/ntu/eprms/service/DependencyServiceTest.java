package uk.ac.ntu.eprms.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import uk.ac.ntu.eprms.domain.FeatureTask;
import uk.ac.ntu.eprms.domain.Task;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DependencyServiceTest {

    private final DependencyService service = new DependencyService();

    @Test
    @DisplayName("A task is ordered after the task it depends on")
    void ordersPrerequisitesFirst() {
        FeatureTask design = task("Design");
        FeatureTask build = task("Build");
        build.setDependsOn(Set.of(design.getId()));

        List<Task> order = service.workingOrder(List.of(build, design));

        assertEquals(List.of(design, build), order);
    }

    @Test
    @DisplayName("A dependency loop is reported")
    void cycleThrows() {
        FeatureTask first = task("First");
        FeatureTask second = task("Second");
        first.setDependsOn(Set.of(second.getId()));
        second.setDependsOn(Set.of(first.getId()));

        assertThrows(CircularDependencyException.class,
                () -> service.workingOrder(List.of(first, second)));
    }

    @Test
    @DisplayName("A task cannot depend on itself")
    void selfDependencyThrows() {
        FeatureTask task = task("Alone");
        task.setDependsOn(Set.of(task.getId()));

        assertThrows(CircularDependencyException.class, () -> service.workingOrder(List.of(task)));
    }

    @Test
    @DisplayName("No tasks produces an empty working order")
    void emptyList() {
        assertTrue(service.workingOrder(List.of()).isEmpty());
    }

    private static FeatureTask task(String title) {
        return new FeatureTask(title, 2, LocalDate.of(2026, 11, 1), "General");
    }
}
