package uk.ac.ntu.eprms.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import uk.ac.ntu.eprms.domain.FeatureTask;
import uk.ac.ntu.eprms.domain.Task;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StrategyTest {

    private final LocalDate early = LocalDate.of(2026, 11, 1);
    private final LocalDate middle = LocalDate.of(2026, 11, 15);
    private final LocalDate late = LocalDate.of(2026, 12, 1);

    @Test
    @DisplayName("Earliest due date orders three mixed tasks")
    void earliestDueDateOrdersThreeTasks() {
        Task lateTask = task("Late", 1, late);
        Task earlyTask = task("Early", 5, early);
        Task middleTask = task("Middle", 3, middle);

        List<Task> ranked = new EarliestDueDateStrategy().prioritise(List.of(lateTask, earlyTask, middleTask));

        assertEquals(List.of(earlyTask, middleTask, lateTask), ranked);
    }

    @Test
    @DisplayName("Earliest due date returns an empty list for no tasks")
    void earliestDueDateEmptyList() {
        assertTrue(new EarliestDueDateStrategy().prioritise(List.of()).isEmpty());
    }

    @Test
    @DisplayName("A task with no due date is sorted last")
    void earliestDueDatePutsMissingDateLast() {
        FeatureTask missing = new FeatureTask();
        missing.setTitle("Undated");
        missing.setPriority(2);
        Task dated = task("Dated", 2, early);

        List<Task> ranked = new EarliestDueDateStrategy().prioritise(List.of(missing, dated));

        assertEquals(List.of(dated, missing), ranked);
    }

    @Test
    @DisplayName("Equal due dates both stay in the result")
    void earliestDueDateKeepsTies() {
        Task first = task("First", 2, early);
        Task second = task("Second", 4, early);

        List<Task> ranked = new EarliestDueDateStrategy().prioritise(List.of(second, first));

        assertEquals(2, ranked.size());
        assertTrue(ranked.contains(first));
        assertTrue(ranked.contains(second));
    }

    @Test
    @DisplayName("Highest priority orders three mixed tasks")
    void highestPriorityOrdersThreeTasks() {
        Task low = task("Low", 1, early);
        Task high = task("High", 5, late);
        Task mid = task("Mid", 3, middle);

        List<Task> ranked = new HighestPriorityStrategy().prioritise(List.of(low, high, mid));

        assertEquals(List.of(high, mid, low), ranked);
    }

    @Test
    @DisplayName("Highest priority returns an empty list for no tasks")
    void highestPriorityEmptyList() {
        assertTrue(new HighestPriorityStrategy().prioritise(List.of()).isEmpty());
    }

    @Test
    @DisplayName("Equal priorities both stay in the result")
    void highestPriorityKeepsTies() {
        Task first = task("First", 3, early);
        Task second = task("Second", 3, late);

        List<Task> ranked = new HighestPriorityStrategy().prioritise(List.of(first, second));

        assertEquals(2, ranked.size());
        assertEquals(3, ranked.get(0).getPriority());
        assertEquals(3, ranked.get(1).getPriority());
    }

    private static FeatureTask task(String title, int priority, LocalDate due) {
        return new FeatureTask(title, priority, due, "General");
    }
}
