package uk.ac.ntu.eprms.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import uk.ac.ntu.eprms.domain.Project;
import uk.ac.ntu.eprms.domain.TeamMember;
import uk.ac.ntu.eprms.persistence.ProjectRepository;
import uk.ac.ntu.eprms.persistence.TeamMemberRepository;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EprmsServiceTest {

    @Test
    @DisplayName("A member who still has a task cannot be deleted")
    void cannotDeleteMemberWhoHasTasks() {
        MemoryProjects projects = new MemoryProjects();
        MemoryMembers members = new MemoryMembers();
        EprmsService service = new EprmsService(projects, members);
        TeamMember member = service.addMember("Ada Lovelace", "ada@ntu.ac.uk", "Developer");
        Project project = service.addProject("Website", "");
        service.addFeatureTask(project.getId(), "Home page", 2, LocalDate.of(2026, 11, 1),
                uk.ac.ntu.eprms.domain.TaskStatus.TO_DO, member.getId(), "UI", java.util.Set.of());

        assertTrue(service.memberHasTasks(member.getId()));
        assertThrows(IllegalStateException.class, () -> service.deleteMember(member.getId()));
    }

    private static final class MemoryProjects implements ProjectRepository {
        private List<Project> data = new ArrayList<>();

        @Override
        public void save(List<Project> projects) {
            data = new ArrayList<>(projects);
        }

        @Override
        public List<Project> load() {
            return data;
        }
    }

    private static final class MemoryMembers implements TeamMemberRepository {
        private List<TeamMember> data = new ArrayList<>();

        @Override
        public void save(List<TeamMember> members) {
            data = new ArrayList<>(members);
        }

        @Override
        public List<TeamMember> load() {
            return data;
        }
    }
}
