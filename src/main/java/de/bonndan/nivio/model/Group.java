package de.bonndan.nivio.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import de.bonndan.nivio.assessment.Assessable;
import de.bonndan.nivio.assessment.StatusValue;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.util.StringUtils;

import java.util.*;

public class Group implements Labeled, Assessable {

    /**
     * Default group identifier (items are assigned to this group if no group is given
     */
    @NonNull
    public static final String COMMON = "common";

    @NonNull
    private final Map<String, Link> links = new HashMap<>();

    @NonNull
    private final Map<String, String> labels = new HashMap<>();
    /**
     * Items belonging to this group. Order is important for layouting (until items are ordered there).
     */
    @NonNull
    private final Set<Item> items = new LinkedHashSet<>();

    @NonNull
    private final String identifier;
    private String owner;
    private String description;
    private String contact;
    private String icon;
    private String color;
    private String landscapeIdentifier;

    public Group(String identifier) {
        if (StringUtils.isEmpty(identifier)) {
            throw new IllegalArgumentException("Group identifier must not be empty");
        }
        this.identifier = identifier;
    }

    @Override
    @NonNull
    public String getIdentifier() {
        return identifier;
    }

    @Override
    @NonNull
    public FullyQualifiedIdentifier getFullyQualifiedIdentifier() {
        return FullyQualifiedIdentifier.build(landscapeIdentifier, identifier, null);
    }

    @Override
    @NonNull
    public String getName() {
        return identifier;
    }

    @Override
    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    @Override
    @Nullable
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    @Nullable
    public String getContact() {
        return contact;
    }

    public void setContact(String contact) {
        this.contact = contact;
    }

    @Override
    @Nullable
    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    @Schema(name = "_links")
    public Map<String, Link> getLinks() {
        return links;
    }

    /**
     * Returns an immutable copy of the items.
     *
     * @return immutable copy
     */
    public Set<Item> getItems() {
        return Collections.unmodifiableSet(items);
    }

    @Override
    @NonNull
    public Map<String, String> getLabels() {
        return labels;
    }

    @Override
    @Nullable
    public String getLabel(String key) {
        return labels.get(key);
    }

    @Override
    public void setLabel(String key, String value) {
        labels.put(key, value);
    }

    @Override
    @NonNull
    public Set<StatusValue> getAdditionalStatusValues() {
        return StatusValue.fromMapping(indexedByPrefix(Label.status));
    }

    @JsonIgnore
    @Override
    @NonNull
    public List<? extends Assessable> getChildren() {
        return new ArrayList<>(getItems());
    }

    public void setLandscape(String landscapeIdentifier) {
        this.landscapeIdentifier = landscapeIdentifier;
    }

    @Override
    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    @Override
    public String toString() {
        return "Group{" +
                "identifier='" + identifier + '\'' +
                '}';
    }

    /**
     * Adds an item to this group.
     *
     * @param item the item to add.
     * @throws IllegalArgumentException if the item group field mismatches
     */
    public void addItem(Item item) {
        boolean canAdd = item.getGroup() == null || (item.getGroup().equals(identifier));

        if (canAdd) {
            item.setGroup(identifier);
            items.add(item);
            return;
        }

        throw new IllegalArgumentException(String.format("Item group '%s' cannot be added to group '%s'", item.getGroup(), identifier));
    }
}
