package uk.ac.ntu.eprms.persistence;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import uk.ac.ntu.eprms.domain.BugTask;
import uk.ac.ntu.eprms.domain.FeatureTask;
import uk.ac.ntu.eprms.domain.Project;
import uk.ac.ntu.eprms.domain.Severity;
import uk.ac.ntu.eprms.domain.Task;
import uk.ac.ntu.eprms.domain.TaskStatus;
import uk.ac.ntu.eprms.domain.TeamMember;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Turns domain objects into plain structures that Jackson can write as JSON.
 * The domain classes stay free of file annotations.
 */
final class JsonCodec {

    private JsonCodec() {
    }

    static ObjectMapper mapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return mapper;
    }

    static List<ProjectFile> projectsToFiles(List<Project> projects) {
        List<ProjectFile> files = new ArrayList<>();
        for (Project project : projects) {
            ProjectFile file = new ProjectFile();
            file.id = project.getId();
            file.name = project.getName();
            file.description = project.getDescription();
            for (Task task : project.getTasks()) {
                file.tasks.add(taskToFile(task));
            }
            files.add(file);
        }
        return files;
    }

    static List<Project> projectsFromFiles(List<ProjectFile> files) {
        List<Project> projects = new ArrayList<>();
        if (files == null) {
            return projects;
        }
        for (ProjectFile file : files) {
            Project project = new Project(file.id, file.name, file.description);
            if (file.tasks != null) {
                for (TaskFile taskFile : file.tasks) {
                    project.addTask(taskFromFile(taskFile));
                }
            }
            project.validate();
            projects.add(project);
        }
        return projects;
    }

    static List<MemberFile> membersToFiles(List<TeamMember> members) {
        List<MemberFile> files = new ArrayList<>();
        for (TeamMember member : members) {
            MemberFile file = new MemberFile();
            file.id = member.getId();
            file.name = member.getName();
            file.email = member.getEmail();
            file.role = member.getRole();
            files.add(file);
        }
        return files;
    }

    static List<TeamMember> membersFromFiles(List<MemberFile> files) {
        List<TeamMember> members = new ArrayList<>();
        if (files == null) {
            return members;
        }
        for (MemberFile file : files) {
            TeamMember member = new TeamMember(file.id, file.name, file.email, file.role);
            member.validate();
            members.add(member);
        }
        return members;
    }

    private static TaskFile taskToFile(Task task) {
        TaskFile file = new TaskFile();
        file.id = task.getId();
        file.title = task.getTitle();
        file.priority = task.getPriority();
        file.dueDate = task.getDueDate();
        file.status = task.getStatus();
        file.assigneeId = task.getAssigneeId();
        file.dependsOn = new HashSet<>(task.getDependsOn());
        if (task instanceof FeatureTask feature) {
            file.type = "feature";
            file.featureArea = feature.getFeatureArea();
        } else if (task instanceof BugTask bug) {
            file.type = "bug";
            file.severity = bug.getSeverity();
        } else {
            throw new IllegalArgumentException("Unknown task type " + task.getClass().getName());
        }
        return file;
    }

    private static Task taskFromFile(TaskFile file) {
        Set<UUID> dependsOn = file.dependsOn == null ? Set.of() : file.dependsOn;
        if ("feature".equals(file.type)) {
            return new FeatureTask(file.id, file.title, file.priority, file.dueDate,
                    file.status, file.assigneeId, file.featureArea, dependsOn);
        }
        if ("bug".equals(file.type)) {
            return new BugTask(file.id, file.title, file.priority, file.dueDate,
                    file.status, file.assigneeId, file.severity, dependsOn);
        }
        throw new IllegalArgumentException("Unknown task type \"" + file.type + "\"");
    }

    static final class ProjectFile {
        public UUID id;
        public String name;
        public String description;
        public List<TaskFile> tasks = new ArrayList<>();
    }

    static final class TaskFile {
        public String type;
        public UUID id;
        public String title;
        public int priority;
        public java.time.LocalDate dueDate;
        public TaskStatus status;
        public UUID assigneeId;
        public String featureArea;
        public Severity severity;
        public Set<UUID> dependsOn = new HashSet<>();
    }

    static final class MemberFile {
        public UUID id;
        public String name;
        public String email;
        public String role;
    }
}
