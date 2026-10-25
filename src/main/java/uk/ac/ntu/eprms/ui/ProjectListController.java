package uk.ac.ntu.eprms.ui;

import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import uk.ac.ntu.eprms.domain.Project;

import java.util.UUID;

/**
 * Left-hand project list and the task pane for the project that is selected.
 */
public final class ProjectListController {

    private final Workspace workspace;
    private final ListView<Project> projects = new ListView<>();
    private final TaskDetailController tasks;
    private final BorderPane root = new BorderPane();

    public ProjectListController(Workspace workspace) {
        this.workspace = workspace;
        this.tasks = new TaskDetailController(workspace);
        build();
    }

    public Parent getRoot() {
        return root;
    }

    public void refresh() {
        UUID selectedId = projects.getSelectionModel().getSelectedItem() == null
                ? null : projects.getSelectionModel().getSelectedItem().getId();
        projects.setItems(workspace.projects);
        if (selectedId != null) {
            workspace.projects.stream()
                    .filter(project -> project.getId().equals(selectedId))
                    .findFirst()
                    .ifPresent(project -> projects.getSelectionModel().select(project));
        } else if (!workspace.projects.isEmpty() && projects.getSelectionModel().getSelectedItem() == null) {
            projects.getSelectionModel().selectFirst();
        }
        tasks.show(projects.getSelectionModel().getSelectedItem());
    }

    private void build() {
        projects.setItems(workspace.projects);
        projects.getSelectionModel().selectedItemProperty().addListener((obs, old, value) -> {
            tasks.show(value);
            if (value == null) {
                workspace.windowTitle.set("EPRMS");
            } else {
                workspace.windowTitle.set("EPRMS — " + value.getName());
            }
        });

        Button add = new Button("New");
        Button edit = new Button("Edit");
        Button delete = new Button("Delete");
        edit.disableProperty().bind(projects.getSelectionModel().selectedItemProperty().isNull());
        delete.disableProperty().bind(projects.getSelectionModel().selectedItemProperty().isNull());
        add.setOnAction(event -> editProject(null));
        edit.setOnAction(event -> editProject(projects.getSelectionModel().getSelectedItem()));
        delete.setOnAction(event -> deleteSelected());

        Label heading = new Label("Projects");
        heading.getStyleClass().add("section-title");
        HBox buttons = new HBox(8, add, edit, delete);
        VBox side = new VBox(8, heading, projects, buttons);
        side.setPadding(new Insets(0, 12, 0, 0));
        side.setPrefWidth(240);
        VBox.setVgrow(projects, Priority.ALWAYS);

        root.setLeft(side);
        root.setCenter(tasks.getRoot());
        root.getStyleClass().add("content");
    }

    private void deleteSelected() {
        Project project = projects.getSelectionModel().getSelectedItem();
        if (project == null) {
            return;
        }
        if (!Dialogs.confirm(root.getScene().getWindow(), "Delete project",
                "Delete \"" + project.getName() + "\" and all of its tasks?")) {
            return;
        }
        workspace.eprms.deleteProject(project.getId());
        workspace.refreshFromService();
        workspace.saveInBackground();
        refresh();
    }

    private void editProject(Project existing) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle(existing == null ? "New project" : "Edit project");
        dialog.initOwner(root.getScene().getWindow());
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        TextField name = new TextField(existing == null ? "" : existing.getName());
        TextArea description = new TextArea(existing == null ? "" : existing.getDescription());
        description.setPrefRowCount(4);
        Label feedback = new Label();
        feedback.getStyleClass().add("error-text");
        VBox form = new VBox(8,
                Dialogs.fieldRow("Name", name),
                Dialogs.fieldRow("Description", description),
                feedback);
        form.setPadding(new Insets(8));
        dialog.getDialogPane().setContent(form);

        Button ok = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);
        ok.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            if (name.getText() == null || name.getText().isBlank()) {
                feedback.setText("Enter a project name.");
                event.consume();
            }
        });
        if (dialog.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) {
            return;
        }

        UUID id;
        if (existing == null) {
            id = workspace.eprms.addProject(name.getText(), description.getText()).getId();
        } else {
            id = existing.getId();
            workspace.eprms.updateProject(id, name.getText(), description.getText());
        }
        workspace.refreshFromService();
        workspace.saveInBackground();
        refresh();
        workspace.projects.stream()
                .filter(project -> project.getId().equals(id))
                .findFirst()
                .ifPresent(project -> projects.getSelectionModel().select(project));
    }
}
