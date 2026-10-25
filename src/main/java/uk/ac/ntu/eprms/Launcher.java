package uk.ac.ntu.eprms;

import javafx.application.Application;

/**
 * Entry point. Run this class from IntelliJ (green arrow), or use {@code mvn javafx:run}.
 * <p>
 * Do not rename or move it: the pom.xml refers to it. It deliberately does not extend
 * {@code Application}. That avoids the "JavaFX runtime components are missing" error
 * when it is run from an IDE.
 */
public final class Launcher {

    private Launcher() {
    }

    /**
     * Starts the JavaFX application.
     *
     * @param args unused command-line arguments
     */
    public static void main(String[] args) {
        Application.launch(uk.ac.ntu.eprms.ui.App.class, args);
    }
}
