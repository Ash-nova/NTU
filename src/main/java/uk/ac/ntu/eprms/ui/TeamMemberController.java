package uk.ac.ntu.eprms.ui;

import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import uk.ac.ntu.eprms.domain.Task;
import uk.ac.ntu.eprms.domain.TeamMember;

import java.util.Map;
import java.util.UUID;

/**
 * Team list with workload, and the tasks assigned to the selected member.
 */
public final class TeamMemberController {

    private final Workspace workspace;
    private final TableView<TeamMember> table = new TableView<>();
    private final ListView<String> assigned = new ListView<>();
    private final Label summary = new Label("Select a member to see their tasks.");
    private final BorderPane root = new BorderPane();

    public TeamMemberController(Workspace workspace) {
        this.workspace = workspace;
        build();
    }

    public Parent getRoot() {
        return root;
    }

    public void refresh() {
        UUID selectedId = table.getSelectionModel().getSelectedItem() == null
                ? null : table.getSelectionModel().getSelectedItem().getId();
        table.getItems().setAll(workspace.members);
        if (selectedId != null) {
            workspace.members.stream()
                    .filter(member -> member.getId().equals(selectedId))
                    .findFirst()
                    .ifPresent(member -> table.getSelectionModel().select(member));
        }
        showAssigned(table.getSelectionModel().getSelectedItem());
        table.refresh();
    }

    private void build() {
        TableColumn<TeamMember, String> name = new TableColumn<>("Name");
        name.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getName()));
        TableColumn<TeamMember, String> email = new TableColumn<>("Email");
        email.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getEmail()));
        TableColumn<TeamMember, String> role = new TableColumn<>("Role");
        role.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getRole()));
        TableColumn<TeamMember, String> workload = new TableColumn<>("Active tasks");
        workload.setCellValueFactory(data -> new SimpleStringProperty(Long.toString(activeCount(data.getValue()))));
        table.getColumns().add(name);
        table.getColumns().add(email);
        table.getColumns().add(role);
        table.getColumns().add(workload);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        table.setPlaceholder(new Label("No team members yet."));
        table.getSelectionModel().selectedItemProperty().addListener((obs, old, value) -> showAssigned(value));

        Button add = new Button("New member");
        Button edit = new Button("Edit");
        Button delete = new Button("Delete");
        edit.disableProperty().bind(table.getSelectionModel().selectedItemProperty().isNull());
        delete.disableProperty().bind(table.getSelectionModel().selectedItemProperty().isNull());
        add.setOnAction(event -> editMember(null));
        edit.setOnAction(event -> editMember(table.getSelectionModel().getSelectedItem()));
        delete.setOnAction(event -> deleteSelected());

        Label heading = new Label("Team");
        heading.getStyleClass().add("section-title");
        summary.getStyleClass().add("hint");
        HBox buttons = new HBox(8, add, edit, delete);
        VBox top = new VBox(8, heading, buttons);
        VBox bottom = new VBox(8, summary, assigned);
        assigned.setPrefHeight(160);
        root.setTop(top);
        root.setCenter(table);
        root.setBottom(bottom);
        root.setPadding(new Insets(4));
        root.getStyleClass().add("content");
        VBox.setVgrow(table, Priority.ALWAYS);
    }

    private long activeCount(TeamMember member) {
        Map<UUID, Long> counts = workspace.statistics.activeTasksByMember(workspace.eprms.allTasks());
        return counts.getOrDefault(member.getId(), 0L);
    }

    private void showAssigned(TeamMember member) {
        assigned.getItems().clear();
        if (member == null) {
            summary.setText("Select a member to see their tasks.");
            return;
        }
        var tasks = workspace.eprms.tasksForMember(member.getId());
        summary.setText(member.getName() + " has " + activeCount(member)
                + " active task(s) and " + tasks.size() + " task(s) in total.");
        if (tasks.isEmpty()) {
            assigned.getItems().add("No tasks assigned.");
            return;
        }
        for (Task task : tasks) {
            assigned.getItems().add(task.getTitle() + "  ·  " + task.getStatus()
                    + "  ·  due " + task.getDueDate());
        }
    }

    private void deleteSelected() {
        TeamMember member = table.getSelectionModel().getSelectedItem();
        if (member == null) {
            return;
        }
        if (workspace.eprms.memberHasTasks(member.getId())) {
            Dialogs.warn(root.getScene().getWindow(), "Member still has tasks",
                    "Reassign or delete " + member.getName() + "'s tasks before removing them.");
            return;
        }
        if (!Dialogs.confirm(root.getScene().getWindow(), "Delete member",
                "Delete " + member.getName() + "?")) {
            return;
        }
        workspace.eprms.deleteMember(member.getId());
        workspace.refreshFromService();
        workspace.saveInBackground();
        refresh();
    }

    private void editMember(TeamMember existing) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle(existing == null ? "New member" : "Edit member");
        dialog.initOwner(root.getScene().getWindow());
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        TextField name = new TextField(existing == null ? "" : existing.getName());
        TextField email = new TextField(existing == null ? "" : existing.getEmail());
        TextField role = new TextField(existing == null ? "" : existing.getRole());
        Label feedback = new Label();
        feedback.getStyleClass().add("error-text");
        VBox form = new VBox(8,
                Dialogs.fieldRow("Name", name),
                Dialogs.fieldRow("Email", email),
                Dialogs.fieldRow("Role", role),
                feedback);
        form.setPadding(new Insets(8));
        dialog.getDialogPane().setContent(form);

        Button ok = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);
        ok.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            String error = check(name.getText(), email.getText(), role.getText());
            if (error != null) {
                feedback.setText(error);
                event.consume();
            }
        });
        if (dialog.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) {
            return;
        }
        try {
            if (existing == null) {
                workspace.eprms.addMember(name.getText(), email.getText(), role.getText());
            } else {
                workspace.eprms.updateMember(existing.getId(), name.getText(), email.getText(), role.getText());
            }
        } catch (RuntimeException ex) {
            Dialogs.warn(root.getScene().getWindow(), "Member not saved", ex.getMessage());
            return;
        }
        workspace.refreshFromService();
        workspace.saveInBackground();
        refresh();
    }

    private static String check(String name, String email, String role) {
        if (name == null || name.isBlank()) {
            return "Enter a name.";
        }
        if (role == null || role.isBlank()) {
            return "Enter a role.";
        }
        if (email == null || !email.contains("@") || !email.substring(email.indexOf('@') + 1).contains(".")) {
            return "Enter an email like name@company.com.";
        }
        return null;
    }
}
