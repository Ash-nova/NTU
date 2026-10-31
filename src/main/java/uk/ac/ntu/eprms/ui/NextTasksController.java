package uk.ac.ntu.eprms.ui;

import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import uk.ac.ntu.eprms.domain.Project;
import uk.ac.ntu.eprms.domain.Task;
import uk.ac.ntu.eprms.domain.TaskStatus;
import uk.ac.ntu.eprms.service.CircularDependencyException;
import uk.ac.ntu.eprms.service.EarliestDueDateStrategy;
import uk.ac.ntu.eprms.service.HighestPriorityStrategy;
import uk.ac.ntu.eprms.service.PrioritisationStrategy;

import java.util.ArrayList;
import java.util.List;

/**
 * Ranked list of tasks that are not done. The ranking strategy can be changed
 * without reloading the window.
 */
public final class NextTasksController {

    private final Workspace workspace;
    private final ListView<String> list = new ListView<>();
    private final Label note = new Label();
    private final ToggleGroup group = new ToggleGroup();
    private final RadioButton byDate = new RadioButton("By due date");
    private final RadioButton byPriority = new RadioButton("By priority");
    private final RadioButton byDependency = new RadioButton("By dependency order");
    private final VBox root = new VBox(10);

    public NextTasksController(Workspace workspace) {
        this.workspace = workspace;
        byDate.setToggleGroup(group);
        byPriority.setToggleGroup(group);
        byDependency.setToggleGroup(group);
        byDate.setSelected(true);
        group.selectedToggleProperty().addListener((obs, old, value) -> refresh());

        Label heading = new Label("What to do next");
        heading.getStyleClass().add("section-title");
        note.getStyleClass().add("hint");
        HBox toggles = new HBox(16, byDate, byPriority, byDependency);
        list.setPlaceholder(new Label("No open tasks."));
        VBox.setVgrow(list, Priority.ALWAYS);
        root.getChildren().addAll(heading, toggles, note, list);
        root.setPadding(new Insets(8));
        root.getStyleClass().add("content");
    }

    public Parent getRoot() {
        return root;
    }

    public void refresh() {
        List<Task> all = workspace.eprms.allTasks();
        list.getItems().clear();
        if (byDependency.isSelected()) {
            showDependencyOrder(all);
            return;
        }
        PrioritisationStrategy strategy = byPriority.isSelected()
                ? new HighestPriorityStrategy()
                : new EarliestDueDateStrategy();
        List<Task> ranked = workspace.taskService.getNextTasks(all, strategy);
        note.setText("Completed tasks are hidden. " + ranked.size() + " open task(s).");
        fill(ranked);
    }

    private void showDependencyOrder(List<Task> all) {
        List<Task> open = new ArrayList<>();
        for (Task task : all) {
            if (task.getStatus() != TaskStatus.DONE) {
                open.add(task);
            }
        }
        try {
            List<Task> ordered = workspace.dependencies.workingOrder(open);
            note.setText("Working order respects dependencies. Time complexity is O(V + E).");
            fill(ordered);
        } catch (CircularDependencyException ex) {
            note.setText(ex.getMessage());
            list.getItems().add("Fix the dependency loop before a working order can be shown.");
        }
    }

    private void fill(List<Task> tasks) {
        if (tasks.isEmpty()) {
            list.getItems().add("Nothing to do. Every task is done, or there are no tasks yet.");
            return;
        }
        int rank = 1;
        for (Task task : tasks) {
            StringBuilder line = new StringBuilder();
            line.append(rank).append(".  ").append(task.getTitle())
                    .append("   ·   ").append(projectName(task))
                    .append("   ·   due ").append(task.getDueDate())
                    .append("   ·   priority ").append(task.getPriority());
            String blockers = blockerSummary(task);
            if (!blockers.isEmpty()) {
                line.append("   ·   waiting on: ").append(blockers);
            }
            if (task.isOverdue()) {
                line.append("   ·   OVERDUE");
            }
            list.getItems().add(line.toString());
            rank++;
        }
    }

    private String projectName(Task task) {
        Project project = findProject(task);
        return project == null ? "Unknown project" : project.getName();
    }

    private String blockerSummary(Task task) {
        Project project = findProject(task);
        if (project == null || task.getDependsOn().isEmpty()) {
            return "";
        }
        List<String> waiting = new ArrayList<>();
        for (var id : task.getDependsOn()) {
            Task other = project.findTask(id);
            if (other != null && other.getStatus() != TaskStatus.DONE) {
                waiting.add(other.getTitle());
            }
        }
        return String.join(", ", waiting);
    }

    private Project findProject(Task task) {
        for (Project project : workspace.projects) {
            if (project.findTask(task.getId()) != null) {
                return project;
            }
        }
        return null;
    }
}
