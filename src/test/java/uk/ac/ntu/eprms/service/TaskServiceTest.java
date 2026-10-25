package uk.ac.ntu.eprms.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import uk.ac.ntu.eprms.domain.BugTask;
import uk.ac.ntu.eprms.domain.FeatureTask;
import uk.ac.ntu.eprms.domain.Severity;
import uk.ac.ntu.eprms.domain.Task;
import uk.ac.ntu.eprms.domain.TaskStatus;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TaskServiceTest {

    private TaskService service;
    private FeatureTask design;
    private BugTask crash;
    private FeatureTask report;
    private UUID ada;

    @BeforeEach
    void setUp() {
        service = new TaskService();
        ada = UUID.randomUUID();
        design = new FeatureTask("Design home", 2, LocalDate.of(2026, 11, 10), "UI");
        crash = new BugTask("Crash on save", 5, LocalDate.of(2026, 11, 1), Severity.HIGH);
        report = new FeatureTask("Monthly report", 4, LocalDate.of(2026, 12, 1), "Reports");
        crash.setAssigneeId(ada);
        report.setStatus(TaskStatus.DONE);
    }

    @Test
    @DisplayName("Title search ignores case")
    void searchMatchesIgnoringCase() {
        List<Task> found = service.searchByTitle(List.of(design, crash, report), "CRASH");

        assertEquals(List.of(crash), found);
    }

    @Test
    @DisplayName("A title that does not occur returns no tasks")
    void searchWithNoMatchReturnsEmpty() {
        assertTrue(service.searchByTitle(List.of(design, crash), "missing").isEmpty());
    }

    @Test
    @DisplayName("A blank search returns every task")
    void blankSearchReturnsEveryTask() {
        List<Task> tasks = List.of(design, crash);
        assertEquals(tasks, service.searchByTitle(tasks, "  "));
    }

    @Test
    @DisplayName("Search still works when the title contains punctuation")
    void searchAllowsPunctuation() {
        FeatureTask odd = new FeatureTask("Fix (API) timeout!", 2, LocalDate.of(2026, 11, 3), "API");

        assertEquals(List.of(odd), service.searchByTitle(List.of(odd, design), "(api)"));
    }

    @Test
    @DisplayName("Status filter keeps one status")
    void filterByStatus() {
        assertEquals(List.of(report), service.filterByStatus(List.of(design, crash, report), TaskStatus.DONE));
    }

    @Test
    @DisplayName("Priority filter keeps one priority")
    void filterByPriority() {
        assertEquals(List.of(crash), service.filterByPriority(List.of(design, crash, report), 5));
    }

    @Test
    @DisplayName("Member filter can select one person or the unassigned tasks")
    void filterByMemberAndUnassigned() {
        assertEquals(List.of(crash), service.filterByMember(List.of(design, crash), ada));
        assertEquals(List.of(design), service.filterByMember(List.of(design, crash), null));
    }

    @Test
    @DisplayName("Due-date sort is earliest first")
    void sortByDueDate() {
        List<Task> sorted = service.sortByDueDate(List.of(report, design, crash));

        assertEquals(List.of(crash, design, report), sorted);
    }

    @Test
    @DisplayName("Priority sort is highest first")
    void sortByPriorityPutsHighestFirst() {
        List<Task> sorted = service.sortByPriority(List.of(design, report, crash));

        assertEquals(List.of(crash, report, design), sorted);
    }

    @Test
    @DisplayName("Next tasks leaves out work that is already done")
    void nextTasksExcludeDone() {
        List<Task> next = service.getNextTasks(List.of(design, crash, report), new HighestPriorityStrategy());

        assertEquals(List.of(crash, design), next);
    }
}
