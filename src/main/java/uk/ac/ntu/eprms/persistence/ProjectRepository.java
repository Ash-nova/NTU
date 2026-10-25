package uk.ac.ntu.eprms.persistence;

import uk.ac.ntu.eprms.domain.Project;

import java.util.List;

/**
 * Stores and loads projects. The rest of the application depends on this interface,
 * so JSON and the database can be swapped without changing the window or the services.
 */
public interface ProjectRepository {

    /**
     * Writes the full project list, replacing whatever was stored before.
     *
     * @param projects projects to keep; must not be null
     */
    void save(List<Project> projects);

    /**
     * Reads the stored projects. A missing or unreadable file returns an empty list
     * rather than crashing the application.
     *
     * @return the stored projects, never null
     */
    List<Project> load();
}
