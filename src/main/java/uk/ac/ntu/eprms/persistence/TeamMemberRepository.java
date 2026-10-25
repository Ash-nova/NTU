package uk.ac.ntu.eprms.persistence;

import uk.ac.ntu.eprms.domain.TeamMember;

import java.util.List;

/**
 * Stores and loads team members. Callers depend on this interface, not on a file or a database.
 */
public interface TeamMemberRepository {

    /**
     * Writes the full member list, replacing whatever was stored before.
     *
     * @param members members to keep; must not be null
     */
    void save(List<TeamMember> members);

    /**
     * Reads the stored members. A missing or unreadable file returns an empty list.
     *
     * @return the stored members, never null
     */
    List<TeamMember> load();
}
