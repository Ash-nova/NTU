package uk.ac.ntu.eprms.ui;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import uk.ac.ntu.eprms.domain.BugTask;
import uk.ac.ntu.eprms.domain.FeatureTask;
import uk.ac.ntu.eprms.domain.Project;
import uk.ac.ntu.eprms.domain.Severity;
import uk.ac.ntu.eprms.domain.Task;
import uk.ac.ntu.eprms.domain.TaskStatus;
import uk.ac.ntu.eprms.domain.TeamMember;
import uk.ac.ntu.eprms.service.CircularDependencyException;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Task table for the selected project, including search, filter, sort and the task dialog.
 */
public final class TaskDetailController {

    private static final DateTimeFormatter DATES = DateTimeFormatter.ofPattern("d MMM yyyy");

    private final Workspace workspace;
    private final ObjectProperty<Project> current = new SimpleObjectProperty<>();
    private final TableView<Task> table = new TableView<>();
    private final TextField search = new TextField();
    private final ComboBox<String> statusFilter = new ComboBox<>();
    private final ComboBox<String> priorityFilter = new ComboBox<>();
    private final ComboBox<String> memberFilter = new ComboBox<>();
    private final ComboBox<String> sortChoice = new ComboBox<>();
    private final Label empty = new Label("Select a project, or add a task to this one.");
    private final BorderPane root = new BorderPane();

    public TaskDetailController(Workspace workspace) {
        this.workspace = workspace;
        build();
    }

    public Parent getRoot() {
        return root;
    }

    /**
     * Shows the tasks of one project. Pass null when nothing is selected.
     *
     * @param project selected project, or null
     */
    public void show(Project project) {
        current.set(project);
        refreshMemberFilter();
        refreshTable();
    }

    private void build() {
        search.setPromptText("Search title");
        search.textProperty().addListener((obs, old, value) -> refreshTable());

        statusFilter.getItems().addAll("All statuses", "To do", "In progress", "Done");
        statusFilter.setValue("All statuses");
        statusFilter.setOnAction(event -> refreshTable());

        priorityFilter.getItems().addAll("All priorities", "1", "2", "3", "4", "5");
        priorityFilter.setValue("All priorities");
        priorityFilter.setOnAction(event -> refreshTable());

        memberFilter.setOnAction(event -> refreshTable());
        sortChoice.getItems().addAll("Due date", "Priority");
        sortChoice.setValue("Due date");
        sortChoice.setOnAction(event -> refreshTable());

        Button add = new Button("Add task");
        Button edit = new Button("Edit task");
        Button delete = new Button("Delete task");
        add.disableProperty().bind(current.isNull());
        edit.disableProperty().bind(table.getSelectionModel().selectedItemProperty().isNull());
        delete.disableProperty().bind(table.getSelectionModel().selectedItemProperty().isNull());
        add.setOnAction(event -> editTask(null));
        edit.setOnAction(event -> editTask(table.getSelectionModel().getSelectedItem()));
        delete.setOnAction(event -> deleteSelected());

        HBox filters = new HBox(8, search, statusFilter, priorityFilter, memberFilter, sortChoice);
        filters.getStyleClass().add("toolbar");
        HBox.setHgrow(search, Priority.ALWAYS);
        HBox actions = new HBox(8, add, edit, delete);
        actions.getStyleClass().add("toolbar");

        configureTable();
        empty.getStyleClass().add("hint");
        empty.setPadding(new Insets(12));

        VBox top = new VBox(8, filters, actions);
        root.setTop(top);
        root.setCenter(table);
        root.setBottom(empty);
        BorderPane.setMargin(top, new Insets(0, 0, 8, 0));
    }

    private void configureTable() {
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        table.getColumns().add(column("Title", task -> task.getTitle()));
        table.getColumns().add(column("Type", Task::getTypeName));
        table.getColumns().add(priorityColumn());
        table.getColumns().add(column("Due", task -> task.getDueDate() == null ? "" : DATES.format(task.getDueDate())));
        table.getColumns().add(column("Status", task -> labelFor(task.getStatus())));
        table.getColumns().add(column("Assignee", this::assigneeName));
        table.setRowFactory(view -> new TableRow<>() {
            @Override
            protected void updateItem(Task task, boolean emptyRow) {
                super.updateItem(task, emptyRow);
                getStyleClass().remove("overdue-row");
                if (task != null && !emptyRow && task.isOverdue()) {
                    getStyleClass().add("overdue-row");
                }
            }
        });
        table.setPlaceholder(new Label("No tasks match the current filters."));
    }

    private TableColumn<Task, String> column(String title, java.util.function.Function<Task, String> value) {
        TableColumn<Task, String> column = new TableColumn<>(title);
        column.setCellValueFactory(data -> new SimpleStringProperty(value.apply(data.getValue())));
        return column;
    }

    private TableColumn<Task, String> priorityColumn() {
        TableColumn<Task, String> column = new TableColumn<>("Priority");
        column.setCellValueFactory(data -> new SimpleStringProperty(Integer.toString(data.getValue().getPriority())));
        column.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean emptyCell) {
                super.updateItem(item, emptyCell);
                if (emptyCell || item == null) {
                    setText(null);
                    setGraphic(null);
                    return;
                }
                Label chip = new Label(item);
                int priority = Integer.parseInt(item);
                if (priority >= 4) {
                    chip.getStyleClass().add("priority-high");
                } else if (priority == 3) {
                    chip.getStyleClass().add("priority-mid");
                } else {
                    chip.getStyleClass().add("priority-low");
                }
                setGraphic(chip);
                setText(null);
            }
        });
        return column;
    }

    private void refreshMemberFilter() {
        String previous = memberFilter.getValue();
        memberFilter.getItems().clear();
        memberFilter.getItems().add("All members");
        memberFilter.getItems().add("Unassigned");
        for (TeamMember member : workspace.members) {
            memberFilter.getItems().add(member.getName());
        }
        if (previous != null && memberFilter.getItems().contains(previous)) {
            memberFilter.setValue(previous);
        } else {
            memberFilter.setValue("All members");
        }
    }

    private void refreshTable() {
        Project project = current.get();
        if (project == null) {
            table.getItems().clear();
            empty.setText("Select a project to see its tasks.");
            return;
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
        if ("Priority".equals(sortChoice.getValue())) {
            tasks = workspace.taskService.sortByPriority(tasks);
        } else {
            tasks = workspace.taskService.sortByDueDate(tasks);
        }
        UUID selectedId = table.getSelectionModel().getSelectedItem() == null
                ? null : table.getSelectionModel().getSelectedItem().getId();
        table.getItems().setAll(tasks);
        if (selectedId != null) {
            tasks.stream()
                    .filter(task -> task.getId().equals(selectedId))
                    .findFirst()
                    .ifPresent(task -> table.getSelectionModel().select(task));
        }
        if (project.getTasks().isEmpty()) {
            empty.setText("This project has no tasks yet.");
        } else if (tasks.isEmpty()) {
            empty.setText("No tasks match the current filters.");
        } else {
            empty.setText(tasks.size() + " task(s) shown. Overdue rows are highlighted.");
        }
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
        if (value == null || value.startsWith("All")) {
            return null;
        }
        return Integer.valueOf(value);
    }

    private UUID selectedMemberId() {
        String value = memberFilter.getValue();
        if (value == null || "All members".equals(value) || "Unassigned".equals(value)) {
            return null;
        }
        for (TeamMember member : workspace.members) {
            if (member.getName().equals(value)) {
                return member.getId();
            }
        }
        return null;
    }

    private String assigneeName(Task task) {
        TeamMember member = workspace.eprms.findMember(task.getAssigneeId());
        return member == null ? "Unassigned" : member.getName();
    }

    private static String labelFor(TaskStatus status) {
        return switch (status) {
            case TO_DO -> "To do";
            case IN_PROGRESS -> "In progress";
            case DONE -> "Done";
        };
    }

    private void deleteSelected() {
        Task task = table.getSelectionModel().getSelectedItem();
        Project project = current.get();
        if (task == null || project == null) {
            return;
        }
        if (!Dialogs.confirm(root.getScene().getWindow(), "Delete task",
                "Delete \"" + task.getTitle() + "\"?")) {
            return;
        }
        workspace.eprms.deleteTask(project.getId(), task.getId());
        workspace.saveInBackground();
        refreshTable();
    }

    private void editTask(Task existing) {
        Project project = current.get();
        if (project == null) {
            return;
        }
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle(existing == null ? "Add task" : "Edit task");
        dialog.initOwner(root.getScene().getWindow());
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        TextField title = new TextField(existing == null ? "" : existing.getTitle());
        ComboBox<String> type = new ComboBox<>();
        type.getItems().addAll("Feature", "Bug");
        type.setValue(existing instanceof BugTask ? "Bug" : "Feature");

        TextField area = new TextField(existing instanceof FeatureTask feature ? feature.getFeatureArea() : "");
        ComboBox<Severity> severity = new ComboBox<>();
        severity.getItems().addAll(Severity.values());
        severity.setValue(existing instanceof BugTask bug ? bug.getSeverity() : Severity.MEDIUM);

        int startingPriority = existing == null ? 3 : existing.getPriority();
        Spinner<Integer> priority = new Spinner<>();
        priority.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 5, startingPriority));
        priority.setEditable(true);

        DatePicker due = new DatePicker(existing == null ? LocalDate.now().plusDays(7) : existing.getDueDate());
        ComboBox<TaskStatus> status = new ComboBox<>();
        status.getItems().addAll(TaskStatus.values());
        status.setValue(existing == null ? TaskStatus.TO_DO : existing.getStatus());

        ComboBox<TeamMember> assignee = new ComboBox<>();
        assignee.getItems().add(null);
        assignee.getItems().addAll(workspace.members);
        assignee.setButtonCell(memberCell());
        assignee.setCellFactory(list -> memberCell());
        assignee.setValue(existing == null ? null : workspace.eprms.findMember(existing.getAssigneeId()));

        VBox dependencyBox = new VBox(4);
        for (Task candidate : project.getTasks()) {
            if (existing != null && candidate.getId().equals(existing.getId())) {
                continue;
            }
            CheckBox box = new CheckBox(candidate.getTitle());
            box.setUserData(candidate.getId());
            if (existing != null && existing.getDependsOn().contains(candidate.getId())) {
                box.setSelected(true);
            }
            dependencyBox.getChildren().add(box);
        }
        if (dependencyBox.getChildren().isEmpty()) {
            dependencyBox.getChildren().add(new Label("No other tasks in this project."));
        }
        ScrollPane dependencyScroll = new ScrollPane(dependencyBox);
        dependencyScroll.setFitToWidth(true);
        dependencyScroll.setPrefHeight(120);

        Label feedback = new Label();
        feedback.getStyleClass().add("error-text");

        VBox form = new VBox(8,
                Dialogs.fieldRow("Title", title),
                Dialogs.fieldRow("Type", type),
                Dialogs.fieldRow("Feature area", area),
                Dialogs.fieldRow("Severity", severity),
                Dialogs.fieldRow("Priority", priority),
                Dialogs.fieldRow("Due date", due),
                Dialogs.fieldRow("Status", status),
                Dialogs.fieldRow("Assignee", assignee),
                new Label("Depends on"),
                dependencyScroll,
                feedback);
        form.setPadding(new Insets(8));
        Runnable toggleType = () -> {
            boolean feature = "Feature".equals(type.getValue());
            area.setVisible(feature);
            area.setManaged(feature);
            severity.setVisible(!feature);
            severity.setManaged(!feature);
        };
        type.setOnAction(event -> toggleType.run());
        toggleType.run();

        dialog.getDialogPane().setContent(form);
        Button ok = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);
        ok.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            String error = validate(title.getText(), type.getValue(), area.getText(),
                    severity.getValue(), due.getValue(), priority.getValue());
            if (error != null) {
                feedback.setText(error);
                event.consume();
            }
        });

        if (dialog.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) {
            return;
        }

        Set<UUID> dependsOn = new HashSet<>();
        for (var node : dependencyBox.getChildren()) {
            if (node instanceof CheckBox box && box.isSelected() && box.getUserData() instanceof UUID id) {
                dependsOn.add(id);
            }
        }
        UUID id = existing == null ? UUID.randomUUID() : existing.getId();
        TaskStatus chosenStatus = status.getValue();
        UUID assigneeId = assignee.getValue() == null ? null : assignee.getValue().getId();
        Task proposed;
        if ("Bug".equals(type.getValue())) {
            proposed = new BugTask(id, title.getText(), priority.getValue(), due.getValue(),
                    chosenStatus, assigneeId, severity.getValue(), dependsOn);
        } else {
            proposed = new FeatureTask(id, title.getText(), priority.getValue(), due.getValue(),
                    chosenStatus, assigneeId, area.getText(), dependsOn);
        }

        try {
            if (existing == null) {
                workspace.eprms.addTask(project.getId(), proposed);
            } else {
                workspace.eprms.updateTask(project.getId(), proposed);
            }
            workspace.dependencies.workingOrder(project.getTasks());
        } catch (CircularDependencyException ex) {
            if (existing == null) {
                workspace.eprms.deleteTask(project.getId(), proposed.getId());
            } else {
                workspace.eprms.updateTask(project.getId(), existing);
            }
            Dialogs.warn(root.getScene().getWindow(), "Circular dependency", ex.getMessage());
            return;
        } catch (RuntimeException ex) {
            workspace.eprms.logFailure("Could not save task", ex);
            Dialogs.warn(root.getScene().getWindow(), "Task not saved", ex.getMessage());
            return;
        }

        workspace.saveInBackground();
        refreshTable();
    }

    private static String validate(String title, String type, String area, Severity severity,
                                   LocalDate due, Integer priority) {
        if (title == null || title.isBlank()) {
            return "Enter a title.";
        }
        if (due == null) {
            return "Choose a due date.";
        }
        if (priority == null || priority < 1 || priority > 5) {
            return "Priority must be between 1 and 5.";
        }
        if ("Feature".equals(type) && (area == null || area.isBlank())) {
            return "Enter the feature area.";
        }
        if ("Bug".equals(type) && severity == null) {
            return "Choose a severity.";
        }
        return null;
    }

    private static javafx.scene.control.ListCell<TeamMember> memberCell() {
        return new javafx.scene.control.ListCell<>() {
            @Override
            protected void updateItem(TeamMember member, boolean emptyCell) {
                super.updateItem(member, emptyCell);
                if (emptyCell) {
                    setText(null);
                } else if (member == null) {
                    setText("Unassigned");
                } else {
                    setText(member.getName());
                }
            }
        };
    }
}
