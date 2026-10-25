package uk.ac.ntu.eprms.persistence;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import uk.ac.ntu.eprms.domain.Project;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Saves projects as JSON. Feature and bug tasks are written with a {@code type} field
 * so they can be rebuilt as the right subclass.
 */
public class JsonProjectRepository implements ProjectRepository {

    private static final Logger LOG = Logger.getLogger(JsonProjectRepository.class.getName());

    private final Path file;
    private final ObjectMapper mapper;

    /**
     * Stores projects in {@code data/projects.json} under the working directory.
     */
    public JsonProjectRepository() {
        this(Path.of("data", "projects.json"));
    }

    /**
     * @param file JSON file to read and write
     */
    public JsonProjectRepository(Path file) {
        this.file = file;
        this.mapper = JsonCodec.mapper();
    }

    @Override
    public void save(List<Project> projects) {
        try {
            if (file.getParent() != null) {
                Files.createDirectories(file.getParent());
            }
            mapper.writerWithDefaultPrettyPrinter()
                    .writeValue(file.toFile(), JsonCodec.projectsToFiles(projects));
            LOG.info("Saved " + projects.size() + " projects to " + file);
        } catch (IOException ex) {
            LOG.log(Level.SEVERE, "Could not save projects to " + file, ex);
            throw new IllegalStateException("Could not save projects", ex);
        }
    }

    @Override
    public List<Project> load() {
        if (Files.notExists(file)) {
            LOG.info("No project file at " + file + "; starting with an empty list");
            return List.of();
        }
        try {
            List<JsonCodec.ProjectFile> files = mapper.readValue(
                    file.toFile(), new TypeReference<List<JsonCodec.ProjectFile>>() { });
            List<Project> projects = JsonCodec.projectsFromFiles(files);
            LOG.info("Loaded " + projects.size() + " projects from " + file);
            return projects;
        } catch (JsonProcessingException ex) {
            LOG.log(Level.WARNING, "Project file is not valid JSON: " + file, ex);
            return List.of();
        } catch (NoSuchFileException ex) {
            LOG.info("No project file at " + file + "; starting with an empty list");
            return List.of();
        } catch (IOException | IllegalArgumentException | NullPointerException ex) {
            LOG.log(Level.WARNING, "Could not read projects from " + file, ex);
            return List.of();
        }
    }
}
