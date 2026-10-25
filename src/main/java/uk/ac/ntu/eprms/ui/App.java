package uk.ac.ntu.eprms.ui;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;
import uk.ac.ntu.eprms.persistence.RepositoryFactory;
import uk.ac.ntu.eprms.service.DependencyService;
import uk.ac.ntu.eprms.service.EprmsService;
import uk.ac.ntu.eprms.service.StatisticsService;
import uk.ac.ntu.eprms.service.TaskService;

import java.util.Objects;
import java.util.logging.LogManager;
import java.util.logging.Logger;

/**
 * JavaFX application. It builds the window and asks {@link EprmsService} to load data
 * on a background thread. The window does not open files itself.
 */
public class App extends Application {

    private static final Logger LOG = Logger.getLogger(App.class.getName());

    @Override
    public void init() throws Exception {
        try (var in = App.class.getResourceAsStream("/logging.properties")) {
            if (in != null) {
                LogManager.getLogManager().readConfiguration(in);
            }
        }
    }

    @Override
    public void start(Stage stage) {
        RepositoryFactory factory = RepositoryFactory.fromConfig();
        Workspace workspace = new Workspace(
                new EprmsService(factory.projects(), factory.members()),
                new TaskService(),
                new StatisticsService(),
                new DependencyService());
        MainController main = new MainController(workspace);

        Scene scene = new Scene(main.getRoot(), 1100, 720);
        scene.getStylesheets().add(Objects.requireNonNull(
                App.class.getResource("/eprms.css"), "eprms.css").toExternalForm());
        stage.setScene(scene);
        stage.titleProperty().bind(workspace.windowTitle);
        stage.setMinWidth(900);
        stage.setMinHeight(560);
        stage.show();
        LOG.info("EPRMS window opened");
        main.load();
    }
}
