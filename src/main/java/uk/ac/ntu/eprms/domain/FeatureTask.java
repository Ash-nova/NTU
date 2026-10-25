package uk.ac.ntu.eprms.domain;

import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

/**
 * Work that adds or changes a product feature. The extra detail is the area of the
 * product the feature belongs to, such as "Billing" or "Reports".
 */
public class FeatureTask extends Task {

    private String featureArea;

    /**
     * Creates a new feature task that still has to be done.
     *
     * @param title       task title; must not be blank
     * @param priority    urgency from 1 to 5
     * @param dueDate     due date; must not be null
     * @param featureArea product area; must not be blank
     */
    public FeatureTask(String title, int priority, LocalDate dueDate, String featureArea) {
        super(title, priority, dueDate);
        this.featureArea = cleanArea(featureArea);
    }

    /**
     * Recreates a feature task from stored data.
     *
     * @param id          existing id
     * @param title       task title
     * @param priority    urgency from 1 to 5
     * @param dueDate     due date
     * @param status      current status
     * @param assigneeId  member id, or null
     * @param featureArea product area
     * @param dependsOn   prerequisite task ids
     */
    public FeatureTask(UUID id, String title, int priority, LocalDate dueDate,
                       TaskStatus status, UUID assigneeId, String featureArea,
                       Set<UUID> dependsOn) {
        super(id, title, priority, dueDate, status, assigneeId, dependsOn);
        this.featureArea = cleanArea(featureArea);
    }

    /**
     * Used when a repository rebuilds the object from a file or a database row.
     */
    public FeatureTask() {
    }

    @Override
    public String getTypeName() {
        return "Feature";
    }

    /**
     * @return product area this feature belongs to
     */
    public String getFeatureArea() {
        return featureArea;
    }

    /**
     * @param featureArea product area; must not be blank
     */
    public void setFeatureArea(String featureArea) {
        this.featureArea = cleanArea(featureArea);
    }

    @Override
    public void validate() {
        super.validate();
        cleanArea(featureArea);
    }

    private static String cleanArea(String featureArea) {
        if (featureArea == null || featureArea.isBlank()) {
            throw new IllegalArgumentException("Feature area is required");
        }
        return featureArea.trim();
    }
}
