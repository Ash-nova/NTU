package uk.ac.ntu.eprms.persistence;

import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import java.util.logging.Logger;

/**
 * Builds the repository implementations the application should use.
 * A config flag chooses JSON or JPA. Nothing outside this class names
 * {@link JsonProjectRepository} or {@link JpaProjectRepository}.
 */
public final class RepositoryFactory {

    /** System property or properties-file value that selects JPA. */
    public static final String JPA = "jpa";

    /** System property or properties-file value that selects JSON files. */
    public static final String JSON = "json";

    private static final Logger LOG = Logger.getLogger(RepositoryFactory.class.getName());

    private final String mode;
    private EntityManagerFactory entityManagerFactory;

    private RepositoryFactory(String mode) {
        this.mode = mode;
    }

    /**
     * Reads {@code eprms.persistence} from the JVM, then {@code persistence=} from
     * {@code eprms.properties} in the working directory. Anything else uses JSON.
     *
     * @return a factory for the configured storage
     */
    public static RepositoryFactory fromConfig() {
        String mode = System.getProperty("eprms.persistence");
        if (mode == null || mode.isBlank()) {
            mode = readPropertiesFile();
        }
        if (mode == null || mode.isBlank()) {
            mode = JSON;
        }
        mode = mode.trim().toLowerCase();
        if (!JPA.equals(mode) && !JSON.equals(mode)) {
            LOG.warning("Unknown persistence mode \"" + mode + "\"; using JSON");
            mode = JSON;
        }
        LOG.info("Persistence mode is " + mode);
        return new RepositoryFactory(mode);
    }

    /**
     * Opens the named JPA unit directly. Tests use this for an in-memory database.
     *
     * @param persistenceUnit name from {@code persistence.xml}
     * @return a factory that always uses JPA
     */
    public static RepositoryFactory forPersistenceUnit(String persistenceUnit) {
        RepositoryFactory factory = new RepositoryFactory(JPA);
        factory.entityManagerFactory = Persistence.createEntityManagerFactory(persistenceUnit);
        return factory;
    }

    /**
     * @return true when this factory will use the database
     */
    public boolean usesJpa() {
        return JPA.equals(mode);
    }

    /**
     * @return the project store for the chosen mode
     */
    public ProjectRepository projects() {
        if (usesJpa()) {
            return new JpaProjectRepository(entityManagerFactory());
        }
        return new JsonProjectRepository();
    }

    /**
     * @return the member store for the chosen mode
     */
    public TeamMemberRepository members() {
        if (usesJpa()) {
            return new JpaTeamMemberRepository(entityManagerFactory());
        }
        return new JsonTeamMemberRepository();
    }

    /**
     * Closes the database factory if this instance opened one.
     */
    public void close() {
        if (entityManagerFactory != null && entityManagerFactory.isOpen()) {
            entityManagerFactory.close();
        }
    }

    private EntityManagerFactory entityManagerFactory() {
        if (entityManagerFactory == null) {
            entityManagerFactory = Persistence.createEntityManagerFactory("eprms");
        }
        return entityManagerFactory;
    }

    private static String readPropertiesFile() {
        Path path = Path.of("eprms.properties");
        if (Files.notExists(path)) {
            return null;
        }
        Properties properties = new Properties();
        try (InputStream in = Files.newInputStream(path)) {
            properties.load(in);
        } catch (IOException ex) {
            LOG.warning("Could not read eprms.properties; using JSON");
            return null;
        }
        return properties.getProperty("persistence");
    }
}
