package uk.ac.ntu.eprms.persistence;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import uk.ac.ntu.eprms.domain.BugTask;
import uk.ac.ntu.eprms.domain.FeatureTask;
import uk.ac.ntu.eprms.domain.Project;
import uk.ac.ntu.eprms.domain.Severity;
import uk.ac.ntu.eprms.domain.Task;
import uk.ac.ntu.eprms.domain.TaskStatus;
import uk.ac.ntu.eprms.domain.TeamMember;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JsonRepositoryTest {

    @TempDir
    Path folder;

    @Test
    @DisplayName("A project with both task types survives a save and load")
    void roundTripMixedTasks() {
        JsonProjectRepository repository = new JsonProjectRepository(folder.resolve("projects.json"));
        TeamMember member = new TeamMember("Ada Lovelace", "ada@ntu.ac.uk", "Developer");
        Project project = new Project("Website", "Client site");
        FeatureTask feature = new FeatureTask("Home page", 3, LocalDate.of(2026, 11, 2), "UI");
        feature.setAssigneeId(member.getId());
        feature.setStatus(TaskStatus.IN_PROGRESS);
        BugTask bug = new BugTask("Broken link", 4, LocalDate.of(2026, 11, 4), Severity.HIGH);
        bug.setDependsOn(Set.of(feature.getId()));
        project.addTask(feature);
        project.addTask(bug);

        repository.save(List.of(project));
        List<Project> loaded = repository.load();

        assertEquals(1, loaded.size());
        assertEquals("Website", loaded.get(0).getName());
        assertEquals(2, loaded.get(0).getTasks().size());
        Task first = loaded.get(0).getTasks().get(0);
        Task second = loaded.get(0).getTasks().get(1);
        assertInstanceOf(FeatureTask.class, first);
        assertEquals("UI", ((FeatureTask) first).getFeatureArea());
        assertEquals(member.getId(), first.getAssigneeId());
        assertEquals(TaskStatus.IN_PROGRESS, first.getStatus());
        assertInstanceOf(BugTask.class, second);
        assertEquals(Severity.HIGH, ((BugTask) second).getSeverity());
        assertEquals(Set.of(feature.getId()), second.getDependsOn());
    }

    @Test
    @DisplayName("A missing project file loads as an empty list")
    void missingFileReturnsEmpty() {
        JsonProjectRepository repository = new JsonProjectRepository(folder.resolve("missing.json"));

        assertTrue(repository.load().isEmpty());
    }

    @Test
    @DisplayName("Corrupt JSON loads as an empty list")
    void corruptJsonReturnsEmpty() throws Exception {
        Path file = folder.resolve("projects.json");
        Files.writeString(file, "{ this is not json");
        JsonProjectRepository repository = new JsonProjectRepository(file);

        assertTrue(repository.load().isEmpty());
    }

    @Test
    @DisplayName("Members survive a save and load")
    void memberRoundTrip() {
        JsonTeamMemberRepository repository = new JsonTeamMemberRepository(folder.resolve("members.json"));
        TeamMember member = new TeamMember("Grace Hopper", "grace@ntu.ac.uk", "Lead");

        repository.save(List.of(member));
        List<TeamMember> loaded = repository.load();

        assertEquals(1, loaded.size());
        assertEquals(member.getId(), loaded.get(0).getId());
        assertEquals("Grace Hopper", loaded.get(0).getName());
        assertEquals("Lead", loaded.get(0).getRole());
    }
}
