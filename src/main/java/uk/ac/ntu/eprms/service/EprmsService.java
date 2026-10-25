package uk.ac.ntu.eprms.service;

import uk.ac.ntu.eprms.domain.BugTask;
import uk.ac.ntu.eprms.domain.FeatureTask;
import uk.ac.ntu.eprms.domain.Project;
import uk.ac.ntu.eprms.domain.Severity;
import uk.ac.ntu.eprms.domain.Task;
import uk.ac.ntu.eprms.domain.TaskStatus;
import uk.ac.ntu.eprms.domain.TeamMember;
import uk.ac.ntu.eprms.persistence.ProjectRepository;
import uk.ac.ntu.eprms.persistence.TeamMemberRepository;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Loads and saves the consultancy's projects and members, and applies create,
 * update and delete requests from the window. It depends on repository interfaces,
 * not on JSON or the database.
 */
public class EprmsService {

    private static final Logger LOG = Logger.getLogger(EprmsService.class.getName());

    private final ProjectRepository projectRepository;
    private final TeamMemberRepository memberRepository;
    private final List<Project> projects = new ArrayList<>();
    private final List<TeamMember> members = new ArrayList<>();

    /**
     * @param projectRepository where projects are stored; must not be null
     * @param memberRepository  where members are stored; must not be null
     */
    public EprmsService(ProjectRepository projectRepository, TeamMemberRepository memberRepository) {
        this.projectRepository = Objects.requireNonNull(projectRepository, "projectRepository");
        this.memberRepository = Objects.requireNonNull(memberRepository, "memberRepository");
    }

    /**
     * Replaces the in-memory lists with whatever the repositories currently hold.
     */
    public synchronized void load() {
        projects.clear();
        projects.addAll(projectRepository.load());
        members.clear();
        members.addAll(memberRepository.load());
        LOG.info("Loaded " + projects.size() + " projects and " + members.size() + " members");
    }

    /**
     * Writes the current projects and members. Called after a change, from a background thread.
     */
    public synchronized void save() {
        projectRepository.save(List.copyOf(projects));
        memberRepository.save(List.copyOf(members));
        LOG.info("Saved " + projects.size() + " projects and " + members.size() + " members");
    }

    /**
     * @return a snapshot of the projects; the list itself cannot be modified
     */
    public synchronized List<Project> getProjects() {
        return List.copyOf(projects);
    }

    /**
     * @return a snapshot of the members
     */
    public synchronized List<TeamMember> getMembers() {
        return List.copyOf(members);
    }

    /**
     * Every task across every project, in project order.
     *
     * @return a new list, never null
     */
    public synchronized List<Task> allTasks() {
        List<Task> all = new ArrayList<>();
        for (Project project : projects) {
            all.addAll(project.getTasks());
        }
        return all;
    }

    /**
     * @param name        project name
     * @param description optional description
     * @return the project that was added
     */
    public synchronized Project addProject(String name, String description) {
        Project project = new Project(name, description);
        projects.add(project);
        LOG.info("Added project " + project.getName());
        return project;
    }

    /**
     * @param id          project to change
     * @param name        new name
     * @param description new description
     */
    public synchronized void updateProject(UUID id, String name, String description) {
        Project project = requireProject(id);
        project.setName(name);
        project.setDescription(description);
        LOG.info("Updated project " + project.getName());
    }

    /**
     * Deletes a project and its tasks.
     *
     * @param id project to delete
     * @return true if it was present
     */
    public synchronized boolean deleteProject(UUID id) {
        boolean removed = projects.removeIf(project -> project.getId().equals(id));
        if (removed) {
            LOG.info("Deleted project " + id);
        }
        return removed;
    }

    /**
     * @param projectId project that will own the task
     * @param task      task to add
     */
    public synchronized void addTask(UUID projectId, Task task) {
        Objects.requireNonNull(task, "task");
        checkAssignee(task.getAssigneeId());
        requireProject(projectId).addTask(task);
        LOG.info("Added task " + task.getTitle());
    }

    /**
     * @param projectId project that owns the task
     * @param updated   replacement task with the same id
     */
    public synchronized void updateTask(UUID projectId, Task updated) {
        Objects.requireNonNull(updated, "task");
        checkAssignee(updated.getAssigneeId());
        Project project = requireProject(projectId);
        if (!project.replaceTask(updated)) {
            throw new IllegalArgumentException("That task is not in this project");
        }
        LOG.info("Updated task " + updated.getTitle());
    }

    /**
     * @param projectId project that owns the task
     * @param taskId    task to remove
     * @return true if it was present
     */
    public synchronized boolean deleteTask(UUID projectId, UUID taskId) {
        boolean removed = requireProject(projectId).removeTask(taskId);
        if (removed) {
            LOG.info("Deleted task " + taskId);
        }
        return removed;
    }

    /**
     * Builds a feature task and adds it to the project.
     *
     * @param projectId   owning project
     * @param title       task title
     * @param priority    urgency from 1 to 5
     * @param dueDate     due date
     * @param status      initial status
     * @param assigneeId  member id, or null
     * @param featureArea product area
     * @param dependsOn   prerequisite ids
     * @return the new task
     */
    public synchronized FeatureTask addFeatureTask(UUID projectId, String title, int priority,
                                                   LocalDate dueDate, TaskStatus status,
                                                   UUID assigneeId, String featureArea,
                                                   Set<UUID> dependsOn) {
        FeatureTask task = new FeatureTask(UUID.randomUUID(), title, priority, dueDate,
                status, assigneeId, featureArea, dependsOn);
        addTask(projectId, task);
        return task;
    }

    /**
     * Builds a bug task and adds it to the project.
     *
     * @param projectId  owning project
     * @param title      task title
     * @param priority   urgency from 1 to 5
     * @param dueDate    due date
     * @param status     initial status
     * @param assigneeId member id, or null
     * @param severity   defect severity
     * @param dependsOn  prerequisite ids
     * @return the new task
     */
    public synchronized BugTask addBugTask(UUID projectId, String title, int priority,
                                           LocalDate dueDate, TaskStatus status,
                                           UUID assigneeId, Severity severity,
                                           Set<UUID> dependsOn) {
        BugTask task = new BugTask(UUID.randomUUID(), title, priority, dueDate,
                status, assigneeId, severity, dependsOn);
        addTask(projectId, task);
        return task;
    }

    /**
     * @param name  member name
     * @param email member email
     * @param role  member role
     * @return the member that was added
     */
    public synchronized TeamMember addMember(String name, String email, String role) {
        TeamMember member = new TeamMember(name, email, role);
        members.add(member);
        LOG.info("Added member " + member.getName());
        return member;
    }

    /**
     * @param id    member to change
     * @param name  new name
     * @param email new email
     * @param role  new role
     */
    public synchronized void updateMember(UUID id, String name, String email, String role) {
        TeamMember member = requireMember(id);
        member.setName(name);
        member.setEmail(email);
        member.setRole(role);
        LOG.info("Updated member " + member.getName());
    }

    /**
     * @param id member to check
     * @return true when at least one task still names this member
     */
    public synchronized boolean memberHasTasks(UUID id) {
        return allTasks().stream().anyMatch(task -> id.equals(task.getAssigneeId()));
    }

    /**
     * Deletes a member who has no tasks.
     *
     * @param id member to delete
     * @throws IllegalStateException when tasks are still assigned to this member
     */
    public synchronized void deleteMember(UUID id) {
        if (memberHasTasks(id)) {
            throw new IllegalStateException("This member still has tasks. Reassign them first.");
        }
        if (!members.removeIf(member -> member.getId().equals(id))) {
            throw new IllegalArgumentException("Member not found");
        }
        LOG.info("Deleted member " + id);
    }

    /**
     * @param memberId member whose tasks are required
     * @return tasks assigned to that member, across every project
     */
    public synchronized List<Task> tasksForMember(UUID memberId) {
        return allTasks().stream()
                .filter(task -> memberId.equals(task.getAssigneeId()))
                .toList();
    }

    /**
     * Looks up a project that must already exist.
     *
     * @param id project id
     * @return the project
     */
    public synchronized Project requireProject(UUID id) {
        for (Project project : projects) {
            if (project.getId().equals(id)) {
                return project;
            }
        }
        throw new IllegalArgumentException("Project not found");
    }

    /**
     * @param id member id, or null
     * @return the member, or null when {@code id} is null
     */
    public synchronized TeamMember findMember(UUID id) {
        if (id == null) {
            return null;
        }
        for (TeamMember member : members) {
            if (member.getId().equals(id)) {
                return member;
            }
        }
        return null;
    }

    private void checkAssignee(UUID assigneeId) {
        if (assigneeId != null && findMember(assigneeId) == null) {
            throw new IllegalArgumentException("That team member does not exist");
        }
    }

    private TeamMember requireMember(UUID id) {
        TeamMember member = findMember(id);
        if (member == null) {
            throw new IllegalArgumentException("Member not found");
        }
        return member;
    }

    /**
     * Records a failure that the window has already shown to the user.
     *
     * @param message what failed
     * @param error   the cause
     */
    public void logFailure(String message, Throwable error) {
        LOG.log(Level.WARNING, message, error);
    }
}
