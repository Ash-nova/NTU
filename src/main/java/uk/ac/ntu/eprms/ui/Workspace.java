package uk.ac.ntu.eprms.ui;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import uk.ac.ntu.eprms.domain.Project;
import uk.ac.ntu.eprms.domain.TeamMember;
import uk.ac.ntu.eprms.service.DependencyService;
import uk.ac.ntu.eprms.service.EprmsService;
import uk.ac.ntu.eprms.service.StatisticsService;
import uk.ac.ntu.eprms.service.TaskService;

/**
 * Shared application state for the window. Screens read the observable lists
 * and ask this class to load or save. File access stays off the JavaFX thread.
 */
final class Workspace {

    final EprmsService eprms;
    final TaskService taskService;
    final StatisticsService statistics;
    final DependencyService dependencies;
    final ObservableList<Project> projects = FXCollections.observableArrayList();
    final ObservableList<TeamMember> members = FXCollections.observableArrayList();
    final StringProperty status = new SimpleStringProperty("Ready");
    final StringProperty windowTitle = new SimpleStringProperty("EPRMS");

    Workspace(EprmsService eprms, TaskService taskService,
              StatisticsService statistics, DependencyService dependencies) {
        this.eprms = eprms;
        this.taskService = taskService;
        this.statistics = statistics;
        this.dependencies = dependencies;
    }

    void refreshFromService() {
        projects.setAll(eprms.getProjects());
        members.setAll(eprms.getMembers());
    }

    void loadInBackground(Runnable afterSuccess) {
        status.set("Loading...");
        javafx.concurrent.Task<Void> task = new javafx.concurrent.Task<>() {
            @Override
            protected Void call() {
                eprms.load();
                return null;
            }
        };
        task.setOnSucceeded(event -> {
            refreshFromService();
            status.set("Loaded");
            if (afterSuccess != null) {
                afterSuccess.run();
            }
        });
        task.setOnFailed(event -> {
            status.set("Load failed");
            eprms.logFailure("Load failed", task.getException());
            Dialogs.warn(null, "Load failed",
                    "Saved data could not be read. The window will start empty.");
        });
        start(task, "eprms-load");
    }

    void saveInBackground() {
        status.set("Saving...");
        javafx.concurrent.Task<Void> task = new javafx.concurrent.Task<>() {
            @Override
            protected Void call() {
                eprms.save();
                return null;
            }
        };
        task.setOnSucceeded(event -> status.set("Saved"));
        task.setOnFailed(event -> {
            status.set("Save failed");
            eprms.logFailure("Save failed", task.getException());
            Dialogs.warn(null, "Save failed",
                    "The latest change could not be stored. Check that the data folder is writable.");
        });
        start(task, "eprms-save");
    }

    private static void start(javafx.concurrent.Task<?> task, String name) {
        Thread thread = new Thread(task, name);
        thread.setDaemon(true);
        thread.start();
    }
}
