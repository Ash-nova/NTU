package uk.ac.ntu.eprms.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import uk.ac.ntu.eprms.domain.BugTask;
import uk.ac.ntu.eprms.domain.FeatureTask;
import uk.ac.ntu.eprms.domain.Project;
import uk.ac.ntu.eprms.domain.Severity;
import uk.ac.ntu.eprms.domain.Task;
import uk.ac.ntu.eprms.domain.TaskStatus;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StatisticsServiceTest {

    private final StatisticsService statistics = new StatisticsService();
    private final LocalDate today = LocalDate.of(2026, 10, 20);

    @Test
    @DisplayName("Completion is 0 when nothing is done")
    void completionZero() {
        Project project = projectWith(
                task("A", TaskStatus.TO_DO),
                task("B", TaskStatus.IN_PROGRESS));

        assertEquals(0.0, statistics.completionPercentage(project));
    }

    @Test
    @DisplayName("Completion is 50 when half the tasks are done")
    void completionHalf() {
        Project project = projectWith(
                task("A", TaskStatus.DONE),
                task("B", TaskStatus.TO_DO));

        assertEquals(50.0, statistics.completionPercentage(project));
    }

    @Test
    @DisplayName("Completion is 100 when every task is done")
    void completionFull() {
        Project project = projectWith(
                task("A", TaskStatus.DONE),
                task("B", TaskStatus.DONE));

        assertEquals(100.0, statistics.completionPercentage(project));
    }

    @Test
    @DisplayName("A project with no tasks is 0 percent complete")
    void completionEmptyProject() {
        assertEquals(0.0, statistics.completionPercentage(new Project("Empty", "")));
    }

    @Test
    @DisplayName("Overdue means due before today and not done")
    void overdueExcludesDoneAndFuture() {
        FeatureTask late = task("Late", TaskStatus.TO_DO);
        late.setDueDate(today.minusDays(1));
        FeatureTask future = task("Future", TaskStatus.IN_PROGRESS);
        future.setDueDate(today.plusDays(2));
        FeatureTask finishedLate = task("Finished", TaskStatus.DONE);
        finishedLate.setDueDate(today.minusDays(5));

        List<Task> overdue = statistics.overdueTasks(List.of(late, future, finishedLate), today);

        assertEquals(List.of(late), overdue);
    }

    @Test
    @DisplayName("Every status is present even when the count is zero")
    void countByStatusIncludesZeros() {
        Map<TaskStatus, Long> counts = statistics.countByStatus(List.of(task("Only", TaskStatus.TO_DO)));

        assertEquals(1L, counts.get(TaskStatus.TO_DO));
        assertEquals(0L, counts.get(TaskStatus.IN_PROGRESS));
        assertEquals(0L, counts.get(TaskStatus.DONE));
    }

    @Test
    @DisplayName("Tasks per member skips tasks with no assignee")
    void tasksByMemberSkipsUnassigned() {
        UUID ada = UUID.randomUUID();
        FeatureTask assigned = task("Assigned", TaskStatus.TO_DO);
        assigned.setAssigneeId(ada);
        FeatureTask open = task("Open", TaskStatus.TO_DO);

        Map<UUID, Long> counts = statistics.tasksByMember(List.of(assigned, open));

        assertEquals(1L, counts.get(ada));
        assertTrue(counts.size() == 1);
    }

    @Test
    @DisplayName("Workload counts only tasks that are not done")
    void activeWorkloadIgnoresDoneTasks() {
        UUID ada = UUID.randomUUID();
        FeatureTask open = task("Open", TaskStatus.IN_PROGRESS);
        open.setAssigneeId(ada);
        FeatureTask done = task("Done", TaskStatus.DONE);
        done.setAssigneeId(ada);

        assertEquals(1L, statistics.activeTasksByMember(List.of(open, done)).get(ada));
    }

    private static Project projectWith(Task... tasks) {
        Project project = new Project("Website", "");
        for (Task task : tasks) {
            project.addTask(task);
        }
        return project;
    }

    private static FeatureTask task(String title, TaskStatus status) {
        FeatureTask task = new FeatureTask(title, 2, LocalDate.of(2026, 11, 1), "General");
        task.setStatus(status);
        return task;
    }

    @Test
    @DisplayName("A bug can be counted alongside a feature")
    void mixedTypesAreCounted() {
        Project project = new Project("Website", "");
        project.addTask(new FeatureTask("Page", 2, LocalDate.of(2026, 11, 1), "UI"));
        project.addTask(new BugTask("Crash", 4, LocalDate.of(2026, 11, 2), Severity.HIGH));

        assertEquals(2L, statistics.countByStatus(project.getTasks()).get(TaskStatus.TO_DO));
    }
}
