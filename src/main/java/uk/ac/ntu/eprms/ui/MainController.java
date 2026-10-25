package uk.ac.ntu.eprms.ui;

import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;

/**
 * Root layout: navigation on the left, the current screen in the centre, status along the bottom.
 */
public final class MainController {

    private final Workspace workspace;
    private final ProjectListController projects;
    private final TeamMemberController team;
    private final StatisticsController statistics;
    private final NextTasksController nextTasks;
    private final BorderPane root = new BorderPane();
    private final Button projectsButton = navButton("Projects");
    private final Button teamButton = navButton("Team");
    private final Button statisticsButton = navButton("Statistics");
    private final Button nextButton = navButton("Next tasks");

    public MainController(Workspace workspace) {
        this.workspace = workspace;
        this.projects = new ProjectListController(workspace);
        this.team = new TeamMemberController(workspace);
        this.statistics = new StatisticsController(workspace);
        this.nextTasks = new NextTasksController(workspace);
        build();
    }

    public Parent getRoot() {
        return root;
    }

    public void load() {
        workspace.loadInBackground(this::refreshCurrent);
    }

    private void build() {
        Label brand = new Label("EPRMS");
        brand.getStyleClass().add("brand");
        Label tag = new Label("Projects and tasks");
        tag.getStyleClass().add("sidebar-tag");

        projectsButton.setOnAction(event -> show(projects.getRoot(), projectsButton, projects::refresh));
        teamButton.setOnAction(event -> show(team.getRoot(), teamButton, team::refresh));
        statisticsButton.setOnAction(event -> show(statistics.getRoot(), statisticsButton, statistics::refresh));
        nextButton.setOnAction(event -> show(nextTasks.getRoot(), nextButton, nextTasks::refresh));

        VBox side = new VBox(8, brand, tag, projectsButton, teamButton, statisticsButton, nextButton);
        side.getStyleClass().add("sidebar");
        side.setPrefWidth(190);

        Label status = new Label();
        status.textProperty().bind(workspace.status);
        status.getStyleClass().add("status-bar");
        status.setMaxWidth(Double.MAX_VALUE);
        status.setPadding(new Insets(6, 12, 6, 12));

        root.setLeft(side);
        root.setBottom(status);
        show(projects.getRoot(), projectsButton, projects::refresh);
    }

    private void show(Parent view, Button selected, Runnable refresh) {
        for (Button button : new Button[] {projectsButton, teamButton, statisticsButton, nextButton}) {
            button.getStyleClass().remove("nav-selected");
        }
        if (!selected.getStyleClass().contains("nav-selected")) {
            selected.getStyleClass().add("nav-selected");
        }
        root.setCenter(view);
        refresh.run();
    }

    private void refreshCurrent() {
        Parent center = (Parent) root.getCenter();
        if (center == projects.getRoot()) {
            projects.refresh();
        } else if (center == team.getRoot()) {
            team.refresh();
        } else if (center == statistics.getRoot()) {
            statistics.refresh();
        } else if (center == nextTasks.getRoot()) {
            nextTasks.refresh();
        }
    }

    private static Button navButton(String text) {
        Button button = new Button(text);
        button.getStyleClass().add("nav-button");
        button.setMaxWidth(Double.MAX_VALUE);
        return button;
    }
}
