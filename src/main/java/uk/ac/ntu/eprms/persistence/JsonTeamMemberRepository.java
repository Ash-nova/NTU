package uk.ac.ntu.eprms.persistence;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import uk.ac.ntu.eprms.domain.TeamMember;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Saves team members as JSON in the same style as {@link JsonProjectRepository}.
 */
public class JsonTeamMemberRepository implements TeamMemberRepository {

    private static final Logger LOG = Logger.getLogger(JsonTeamMemberRepository.class.getName());

    private final Path file;
    private final ObjectMapper mapper;

    /**
     * Stores members in {@code data/members.json} under the working directory.
     */
    public JsonTeamMemberRepository() {
        this(Path.of("data", "members.json"));
    }

    /**
     * @param file JSON file to read and write
     */
    public JsonTeamMemberRepository(Path file) {
        this.file = file;
        this.mapper = JsonCodec.mapper();
    }

    @Override
    public void save(List<TeamMember> members) {
        try {
            if (file.getParent() != null) {
                Files.createDirectories(file.getParent());
            }
            mapper.writerWithDefaultPrettyPrinter()
                    .writeValue(file.toFile(), JsonCodec.membersToFiles(members));
            LOG.info("Saved " + members.size() + " members to " + file);
        } catch (IOException ex) {
            LOG.log(Level.SEVERE, "Could not save members to " + file, ex);
            throw new IllegalStateException("Could not save members", ex);
        }
    }

    @Override
    public List<TeamMember> load() {
        if (Files.notExists(file)) {
            LOG.info("No member file at " + file + "; starting with an empty list");
            return List.of();
        }
        try {
            List<JsonCodec.MemberFile> files = mapper.readValue(
                    file.toFile(), new TypeReference<List<JsonCodec.MemberFile>>() { });
            List<TeamMember> members = JsonCodec.membersFromFiles(files);
            LOG.info("Loaded " + members.size() + " members from " + file);
            return members;
        } catch (JsonProcessingException ex) {
            LOG.log(Level.WARNING, "Member file is not valid JSON: " + file, ex);
            return List.of();
        } catch (NoSuchFileException ex) {
            LOG.info("No member file at " + file + "; starting with an empty list");
            return List.of();
        } catch (IOException | IllegalArgumentException | NullPointerException ex) {
            LOG.log(Level.WARNING, "Could not read members from " + file, ex);
            return List.of();
        }
    }
}
