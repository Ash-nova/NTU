package uk.ac.ntu.eprms.ui;

import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import uk.ac.ntu.eprms.domain.Project;
import uk.ac.ntu.eprms.domain.Task;
import uk.ac.ntu.eprms.domain.TaskStatus;
import uk.ac.ntu.eprms.domain.TeamMember;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Cross-project search and filter screen. Uses the same {@link uk.ac.ntu.eprms.service.TaskService}
 * rules as the project task table, but looks across every project.
 */
public final class SearchController {

    private static final DateTimeFormatter DATES = DateTimeFormatter.ofPattern("d MMM yyyy");

    private final Workspace workspace;
    private final TableView<Row> table = new TableView<>();
    private final TextField search = new TextField();
    private final ComboBox<String> projectFilter = new ComboBox<>();
    private final ComboBox<String> statusFilter = new ComboBox<>();
    private final ComboBox<String> priorityFilter = new ComboBox<>();
    private final ComboBox<String> memberFilter = new ComboBox<>();
    private final DatePicker fromDate = new DatePicker();
    private final DatePicker toDate = new DatePicker();
    private final Label summary = new Label();
    private final BorderPane root = new BorderPane();

    public SearchController(Workspace workspace) {
        this.workspace = workspace;
        build();
    }

    public Parent getRoot() {
        return root;
    }

    public void refresh() {
        refreshProjectFilter();
        refreshMemberFilter();
        applyFilters();
    }

    private void build() {
        Label heading = new Label("Search");
        heading.getStyleClass().add("section-title");

        search.setPromptText("Search task titles across all projects");
        search.textProperty().addListener((obs, old, value) -> applyFilters());

        statusFilter.getItems().addAll("Any status", "To do", "In progress", "Done");
        statusFilter.setValue("Any status");
        statusFilter.setOnAction(event -> applyFilters());

        priorityFilter.getItems().addAll("Any priority", "1", "2", "3", "4", "5");
        priorityFilter.setValue("Any priority");
        priorityFilter.setOnAction(event -> applyFilters());

        projectFilter.setOnAction(event -> applyFilters());
        memberFilter.setOnAction(event -> applyFilters());
        fromDate.setPromptText("Due from");
        toDate.setPromptText("Due to");
        fromDate.valueProperty().addListener((obs, old, value) -> applyFilters());
        toDate.valueProperty().addListener((obs, old, value) -> applyFilters());

        Button clear = new Button("Clear");
        clear.setOnAction(event -> clearFilters());

        HBox row1 = new HBox(8, search, clear);
        HBox.setHgrow(search, Priority.ALWAYS);
        row1.getStyleClass().add("toolbar");

        HBox row2 = new HBox(8, projectFilter, statusFilter, priorityFilter, memberFilter, fromDate, toDate);
        row2.getStyleClass().add("toolbar");

        configureTable();
        summary.getStyleClass().add("hint");

        VBox top = new VBox(8, heading, row1, row2);
        root.setTop(top);
        root.setCenter(table);
        root.setBottom(summary);
        root.setPadding(new Insets(8));
        root.getStyleClass().add("content");
        BorderPane.setMargin(top, new Insets(0, 0, 8, 0));
        BorderPane.setMargin(summary, new Insets(8, 0, 0, 0));
    }

    private void configureTable() {
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        table.getColumns().add(textColumn("Type", row -> row.task.getTypeName()));
        table.getColumns().add(textColumn("Title", row -> row.task.getTitle()));
        table.getColumns().add(textColumn("Project", row -> row.project.getName()));
        table.getColumns().add(textColumn("Priority", row -> Integer.toString(row.task.getPriority())));
        table.getColumns().add(textColumn("Due", row -> row.task.getDueDate() == null
                ? "" : DATES.format(row.task.getDueDate())));
        table.getColumns().add(textColumn("Status", row -> labelFor(row.task.getStatus())));
        table.getColumns().add(textColumn("Assignee", this::assigneeName));
        table.setRowFactory(view -> new TableRow<>() {
            @Override
            protected void updateItem(Row row, boolean emptyRow) {
                super.updateItem(row, emptyRow);
                getStyleClass().remove("overdue-row");
                if (row != null && !emptyRow && row.task.isOverdue()) {
                    getStyleClass().add("overdue-row");
                }
            }
        });
        table.setPlaceholder(new Label("No tasks match the current search."));
    }

    private TableColumn<Row, String> textColumn(String title, java.util.function.Function<Row, String> value) {
        TableColumn<Row, String> column = new TableColumn<>(title);
        column.setCellValueFactory(data -> new SimpleStringProperty(value.apply(data.getValue())));
        return column;
    }

    private void refreshProjectFilter() {
        String previous = projectFilter.getValue();
        projectFilter.getItems().clear();
        projectFilter.getItems().add("Any project");
        for (Project project : workspace.projects) {
            projectFilter.getItems().add(project.getName());
        }
        if (previous != null && projectFilter.getItems().contains(previous)) {
            projectFilter.setValue(previous);
        } else {
            projectFilter.setValue("Any project");
        }
    }

    private void refreshMemberFilter() {
        String previous = memberFilter.getValue();
        memberFilter.getItems().clear();
        memberFilter.getItems().add("Any member");
        memberFilter.getItems().add("Unassigned");
        for (TeamMember member : workspace.members) {
            memberFilter.getItems().add(member.getName());
        }
        if (previous != null && memberFilter.getItems().contains(previous)) {
            memberFilter.setValue(previous);
        } else {
            memberFilter.setValue("Any member");
        }
    }

    private void clearFilters() {
        search.clear();
        projectFilter.setValue("Any project");
        statusFilter.setValue("Any status");
        priorityFilter.setValue("Any priority");
        memberFilter.setValue("Any member");
        fromDate.setValue(null);
        toDate.setValue(null);
        applyFilters();
    }

    private void applyFilters() {
        List<Row> rows = new ArrayList<>();
        for (Project project : workspace.eprms.getProjects()) {
            if (!matchesProject(project)) {
                continue;
            }
            List<Task> tasks = project.getTasks();
            tasks = workspace.taskService.searchByTitle(tasks, search.getText());
            tasks = workspace.taskService.filterByStatus(tasks, selectedStatus());
            if (selectedPriority() != null) {
                tasks = workspace.taskService.filterByPriority(tasks, selectedPriority());
            }
            UUID memberId = selectedMemberId();
            if (memberId != null || "Unassigned".equals(memberFilter.getValue())) {
                tasks = workspace.taskService.filterByMember(tasks, memberId);
            }
            for (Task task : tasks) {
                if (inDueRange(task.getDueDate())) {
                    rows.add(new Row(project, task));
                }
            }
        }
        table.getItems().setAll(rows);
        if (rows.isEmpty()) {
            summary.setText("No matching tasks.");
        } else {
            summary.setText(rows.size() + " task(s) across all projects. Overdue rows are highlighted.");
        }
    }

    private boolean matchesProject(Project project) {
        String selected = projectFilter.getValue();
        return selected == null || "Any project".equals(selected) || project.getName().equals(selected);
    }

    private boolean inDueRange(LocalDate due) {
        if (due == null) {
            return fromDate.getValue() == null && toDate.getValue() == null;
        }
        if (fromDate.getValue() != null && due.isBefore(fromDate.getValue())) {
            return false;
        }
        if (toDate.getValue() != null && due.isAfter(toDate.getValue())) {
            return false;
        }
        return true;
    }

    private TaskStatus selectedStatus() {
        return switch (statusFilter.getValue()) {
            case "To do" -> TaskStatus.TO_DO;
            case "In progress" -> TaskStatus.IN_PROGRESS;
            case "Done" -> TaskStatus.DONE;
            default -> null;
        };
    }

    private Integer selectedPriority() {
        String value = priorityFilter.getValue();
        if (value == null || value.startsWith("Any")) {
            return null;
        }
        return Integer.valueOf(value);
    }

    private UUID selectedMemberId() {
        String value = memberFilter.getValue();
        if (value == null || "Any member".equals(value) || "Unassigned".equals(value)) {
            return null;
        }
        for (TeamMember member : workspace.members) {
            if (member.getName().equals(value)) {
                return member.getId();
            }
        }
        return null;
    }

    private String assigneeName(Row row) {
        TeamMember member = workspace.eprms.findMember(row.task.getAssigneeId());
        return member == null ? "Unassigned" : member.getName();
    }

    private static String labelFor(TaskStatus status) {
        return switch (status) {
            case TO_DO -> "To do";
            case IN_PROGRESS -> "In progress";
            case DONE -> "Done";
        };
    }

    private static final class Row {
        private final Project project;
        private final Task task;

        private Row(Project project, Task task) {
            this.project = project;
            this.task = task;
        }
    }
}
