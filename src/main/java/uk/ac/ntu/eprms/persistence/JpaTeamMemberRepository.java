package uk.ac.ntu.eprms.persistence;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import uk.ac.ntu.eprms.domain.TeamMember;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Stores team members with JPA, behind {@link TeamMemberRepository}.
 */
public class JpaTeamMemberRepository implements TeamMemberRepository {

    private static final Logger LOG = Logger.getLogger(JpaTeamMemberRepository.class.getName());

    private final EntityManagerFactory factory;

    /**
     * @param factory shared factory for the {@code eprms} persistence unit
     */
    public JpaTeamMemberRepository(EntityManagerFactory factory) {
        this.factory = factory;
    }

    @Override
    public void save(List<TeamMember> members) {
        EntityManager manager = factory.createEntityManager();
        try {
            manager.getTransaction().begin();
            List<TeamMember> existing = manager.createQuery(
                    "select m from TeamMember m", TeamMember.class).getResultList();
            for (TeamMember member : existing) {
                manager.remove(member);
            }
            manager.flush();
            manager.clear();
            for (TeamMember member : members) {
                manager.merge(member);
            }
            manager.getTransaction().commit();
            LOG.info("Saved " + members.size() + " members to the database");
        } catch (RuntimeException ex) {
            if (manager.getTransaction().isActive()) {
                manager.getTransaction().rollback();
            }
            LOG.log(Level.SEVERE, "Could not save members to the database", ex);
            throw ex;
        } finally {
            manager.close();
        }
    }

    @Override
    public List<TeamMember> load() {
        EntityManager manager = factory.createEntityManager();
        try {
            List<TeamMember> members = manager.createQuery(
                    "select m from TeamMember m", TeamMember.class).getResultList();
            LOG.info("Loaded " + members.size() + " members from the database");
            return members;
        } catch (RuntimeException ex) {
            LOG.log(Level.WARNING, "Could not load members from the database", ex);
            return List.of();
        } finally {
            manager.close();
        }
    }
}
