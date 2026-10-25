package uk.ac.ntu.eprms.ui;

import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.chart.PieChart;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.VBox;
import uk.ac.ntu.eprms.domain.Project;
import uk.ac.ntu.eprms.domain.Task;
import uk.ac.ntu.eprms.domain.TaskStatus;
import uk.ac.ntu.eprms.domain.TeamMember;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Charts and counts produced by {@link uk.ac.ntu.eprms.service.StatisticsService}.
 */
public final class StatisticsController {

    private final Workspace workspace;
    private final VBox content = new VBox(14);
    private final ScrollPane root = new ScrollPane(content);

    public StatisticsController(Workspace workspace) {
        this.workspace = workspace;
        content.setPadding(new Insets(8));
        content.getStyleClass().add("content");
        root.setFitToWidth(true);
    }

    public Parent getRoot() {
        return root;
    }

    private static void addSlice(PieChart chart, String name, long count) {
        if (count > 0) {
            chart.getData().add(new PieChart.Data(name + " (" + count + ")", count));
        }
    }

    public void refresh() {
        content.getChildren().clear();
        Label heading = new Label("Statistics");
        heading.getStyleClass().add("section-title");
        content.getChildren().add(heading);

        List<Project> projects = workspace.eprms.getProjects();
        List<Task> tasks = workspace.eprms.allTasks();
        if (projects.isEmpty()) {
            Label empty = new Label("Add a project to see statistics.");
            empty.getStyleClass().add("hint");
            content.getChildren().add(empty);
            return;
        }

        content.getChildren().add(new Label("Completion"));
        for (Project project : projects) {
            double percent = workspace.statistics.completionPercentage(project);
            Label name = new Label(project.getName() + "  —  " + Math.round(percent) + "%");
            ProgressBar bar = new ProgressBar(percent / 100.0);
            bar.setMaxWidth(Double.MAX_VALUE);
            content.getChildren().addAll(name, bar);
        }

        Map<TaskStatus, Long> counts = workspace.statistics.countByStatus(tasks);
        if (tasks.isEmpty()) {
            Label noTasks = new Label("No tasks to chart yet.");
            noTasks.getStyleClass().add("hint");
            content.getChildren().add(noTasks);
        } else {
            PieChart chart = new PieChart();
            chart.setTitle("Tasks by status");
            addSlice(chart, "To do", counts.get(TaskStatus.TO_DO));
            addSlice(chart, "In progress", counts.get(TaskStatus.IN_PROGRESS));
            addSlice(chart, "Done", counts.get(TaskStatus.DONE));
            chart.setLegendVisible(true);
            content.getChildren().add(chart);
        }

        int overdue = workspace.statistics.overdueTasks(tasks).size();
        Label overdueLabel = new Label("Overdue tasks: " + overdue);
        if (overdue > 0) {
            overdueLabel.getStyleClass().add("danger");
        }
        content.getChildren().add(overdueLabel);

        content.getChildren().add(new Label("Tasks per member"));
        Map<UUID, Long> byMember = workspace.statistics.tasksByMember(tasks);
        if (workspace.members.isEmpty()) {
            Label none = new Label("No team members yet.");
            none.getStyleClass().add("hint");
            content.getChildren().add(none);
        } else {
            for (TeamMember member : workspace.members) {
                long count = byMember.getOrDefault(member.getId(), 0L);
                content.getChildren().add(new Label(member.getName() + ": " + count));
            }
        }
    }
}
