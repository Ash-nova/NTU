package uk.ac.ntu.eprms.domain;

import java.util.Objects;
import java.util.UUID;

/**
 * A person who can be assigned to tasks. One task has at most one assignee.
 */
public class TeamMember {

    private UUID id;
    private String name;
    private String email;
    private String role;

    /**
     * Creates a member with a new id.
     *
     * @param name  display name; must not be blank
     * @param email contact email; must look like an email address
     * @param role  job title on the project, such as "Developer"
     */
    public TeamMember(String name, String email, String role) {
        this(UUID.randomUUID(), name, email, role);
    }

    /**
     * Recreates a member that already has an id.
     *
     * @param id    existing id; must not be null
     * @param name  display name
     * @param email contact email
     * @param role  job title
     */
    public TeamMember(UUID id, String name, String email, String role) {
        this.id = Objects.requireNonNull(id, "id");
        this.name = clean(name, "Name");
        this.email = cleanEmail(email);
        this.role = clean(role, "Role");
    }

    /**
     * Used when a repository rebuilds the object from a file or a database row.
     */
    public TeamMember() {
        this.id = UUID.randomUUID();
    }

    /**
     * Checks that the stored fields are still valid.
     */
    public void validate() {
        Objects.requireNonNull(id, "id");
        clean(name, "Name");
        cleanEmail(email);
        clean(role, "Role");
    }

    /**
     * @return stable id of this member
     */
    public UUID getId() {
        return id;
    }

    /**
     * @return display name
     */
    public String getName() {
        return name;
    }

    /**
     * @param name new name; must not be blank
     */
    public void setName(String name) {
        this.name = clean(name, "Name");
    }

    /**
     * @return email address
     */
    public String getEmail() {
        return email;
    }

    /**
     * @param email new email; must contain a name, @ and a domain
     */
    public void setEmail(String email) {
        this.email = cleanEmail(email);
    }

    /**
     * @return role on the team
     */
    public String getRole() {
        return role;
    }

    /**
     * @param role new role; must not be blank
     */
    public void setRole(String role) {
        this.role = clean(role, "Role");
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof TeamMember member)) {
            return false;
        }
        return id.equals(member.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return name;
    }

    private static String clean(String value, String label) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(label + " is required");
        }
        return value.trim();
    }

    private static String cleanEmail(String email) {
        String value = clean(email, "Email");
        int at = value.indexOf('@');
        if (at <= 0 || at != value.lastIndexOf('@') || at >= value.length() - 1 || !value.substring(at + 1).contains(".")) {
            throw new IllegalArgumentException("Email must look like name@company.com");
        }
        return value;
    }
}
