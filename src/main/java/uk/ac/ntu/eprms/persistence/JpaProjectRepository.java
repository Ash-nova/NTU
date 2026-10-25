package uk.ac.ntu.eprms.persistence;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import uk.ac.ntu.eprms.domain.Project;
import uk.ac.ntu.eprms.domain.Task;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Stores projects with JPA. The window still talks to {@link ProjectRepository},
 * so switching from JSON does not change any other layer.
 */
public class JpaProjectRepository implements ProjectRepository {

    private static final Logger LOG = Logger.getLogger(JpaProjectRepository.class.getName());

    private final EntityManagerFactory factory;

    /**
     * @param factory shared factory for the {@code eprms} persistence unit
     */
    public JpaProjectRepository(EntityManagerFactory factory) {
        this.factory = factory;
    }

    @Override
    public void save(List<Project> projects) {
        EntityManager manager = factory.createEntityManager();
        try {
            manager.getTransaction().begin();
            List<Project> existing = manager.createQuery("select p from Project p", Project.class)
                    .getResultList();
            for (Project project : existing) {
                manager.remove(project);
            }
            manager.flush();
            manager.clear();
            for (Project project : projects) {
                manager.merge(project);
            }
            manager.getTransaction().commit();
            LOG.info("Saved " + projects.size() + " projects to the database");
        } catch (RuntimeException ex) {
            if (manager.getTransaction().isActive()) {
                manager.getTransaction().rollback();
            }
            LOG.log(Level.SEVERE, "Could not save projects to the database", ex);
            throw ex;
        } finally {
            manager.close();
        }
    }

    @Override
    public List<Project> load() {
        EntityManager manager = factory.createEntityManager();
        try {
            List<Project> projects = manager.createQuery(
                    "select distinct p from Project p", Project.class).getResultList();
            for (Project project : projects) {
                for (Task task : project.getTasks()) {
                    task.getDependsOn().size();
                }
            }
            LOG.info("Loaded " + projects.size() + " projects from the database");
            return projects;
        } catch (RuntimeException ex) {
            LOG.log(Level.WARNING, "Could not load projects from the database", ex);
            return List.of();
        } finally {
            manager.close();
        }
    }
}
