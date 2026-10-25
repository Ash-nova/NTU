package uk.ac.ntu.eprms.persistence;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import uk.ac.ntu.eprms.domain.BugTask;
import uk.ac.ntu.eprms.domain.FeatureTask;
import uk.ac.ntu.eprms.domain.Project;
import uk.ac.ntu.eprms.domain.Severity;
import uk.ac.ntu.eprms.domain.TaskStatus;
import uk.ac.ntu.eprms.domain.TeamMember;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class JpaRepositoryTest {

    @Test
    @DisplayName("JPA stores projects, both task types and members")
    void savesAndLoadsThroughTheSameInterfaces() {
        RepositoryFactory factory = RepositoryFactory.forPersistenceUnit("eprms-test");
        try {
            TeamMember member = new TeamMember("Ada Lovelace", "ada@ntu.ac.uk", "Developer");
            Project project = new Project("Website", "Client site");
            FeatureTask feature = new FeatureTask("Home page", 3, LocalDate.of(2026, 11, 2), "UI");
            feature.setAssigneeId(member.getId());
            BugTask bug = new BugTask("Broken link", 5, LocalDate.of(2026, 10, 28), Severity.CRITICAL);
            bug.setStatus(TaskStatus.IN_PROGRESS);
            bug.setDependsOn(Set.of(feature.getId()));
            project.addTask(feature);
            project.addTask(bug);

            factory.members().save(List.of(member));
            factory.projects().save(List.of(project));

            var loadedMembers = factory.members().load();
            var loadedProjects = factory.projects().load();

            assertEquals(1, loadedMembers.size());
            assertEquals(member.getEmail(), loadedMembers.get(0).getEmail());
            assertEquals(1, loadedProjects.size());
            assertEquals("Website", loadedProjects.get(0).getName());
            assertEquals(2, loadedProjects.get(0).getTasks().size());
            assertInstanceOf(FeatureTask.class, loadedProjects.get(0).getTasks().get(0));
            assertInstanceOf(BugTask.class, loadedProjects.get(0).getTasks().get(1));
            assertEquals(Set.of(feature.getId()), loadedProjects.get(0).getTasks().get(1).getDependsOn());
        } finally {
            factory.close();
        }
    }
}
