package de.bonndan.nivio.model;

import com.fasterxml.jackson.annotation.JsonIdentityReference;
import com.fasterxml.jackson.annotation.JsonInclude;
import org.springframework.lang.NonNull;
import org.springframework.util.StringUtils;

import java.io.Serializable;
import java.util.Objects;

/**
 * Indication of an incoming or outgoing relation like data flow or dependency (provider).
 *
 * <p>
 * Outgoing flows having a target which matches a service identifier will cause a relation to be created.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Relation implements Serializable {

    @JsonIdentityReference(alwaysAsId = true)
    private final Item source;

    @JsonIdentityReference(alwaysAsId = true)
    private final Item target;

    private final String description;

    private final String format;

    private final RelationType type;

    public Relation(@NonNull final Item source,
                    @NonNull final Item target) {
        this(source, target, null, null, null);
    }

    public Relation(@NonNull final Item source,
                    @NonNull final Item target,
                    final String description,
                    final String format,
                    final RelationType type
    ) {
        if (source == null || target == null) {
            throw new IllegalArgumentException("Null arguments passed.");
        }

        if (source.equals(target)) {
            throw new IllegalArgumentException("Relation source and target are equal.");
        }

        this.source = source;
        this.target = target;
        this.description = description;
        this.format = format;
        this.type = type;
    }

    public RelationType getType() {
        return type;
    }

    public String getDescription() {
        return description;
    }

    public String getFormat() {
        return format;
    }

    public Item getTarget() {
        return target;
    }

    public Item getSource() {
        return source;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Relation relation = (Relation) o;
        return Objects.equals(source, relation.source) && Objects.equals(target, relation.target);
    }

    @Override
    public int hashCode() {
        return Objects.hash(source, target);
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    static class ApiModel {

        public static final String INBOUND = "inbound";
        public static final String OUTBOUND = "outbound";

        @JsonIdentityReference(alwaysAsId = true)
        public final Item source;

        @JsonIdentityReference(alwaysAsId = true)
        public final Item target;

        public final String description;

        public final String format;

        public final RelationType type;

        public final String name;

        public final String id;

        public final String direction;

        ApiModel(Relation relation, Item owner) {
            source = relation.source;
            target = relation.target;
            description = relation.description;
            format = relation.format;
            type = relation.type;

            if (relation.source == owner) {
                name = StringUtils.isEmpty(target.getName()) ? target.getIdentifier() : target.getName();
                id = target.getFullyQualifiedIdentifier().toString();
                direction = OUTBOUND;
            } else {
                name = StringUtils.isEmpty(source.getName()) ? source.getIdentifier() : source.getName();
                id = source.getFullyQualifiedIdentifier().toString();
                direction = INBOUND;
            }
        }
    }
}
