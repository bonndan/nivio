package de.bonndan.nivio.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RelationTest {

    @Test
    void toApiModel() {
        Item one = new Item("foo", "bar");
        Item two = new Item("foo", "baz");
        Relation relation = new Relation(one, two);
        relation.setType(RelationType.PROVIDER);

        Relation.ApiModel apiModel = new Relation.ApiModel(relation, one);

        assertThat(apiModel).isNotNull();
        assertThat(apiModel.direction).isEqualTo(Relation.ApiModel.OUTBOUND);
        assertThat(apiModel.name).isEqualTo("baz");
        assertThat(apiModel.type).isEqualTo(relation.getType());
        assertThat(apiModel.source).isEqualTo(relation.getSource());
        assertThat(apiModel.target).isEqualTo(relation.getTarget());
        assertThat(apiModel.description).isEqualTo(relation.getDescription());
        assertThat(apiModel.format).isEqualTo(relation.getFormat());
    }

    @Test
    void inbound() {
        Item one = new Item("foo", "bar");
        Item two = new Item("foo", "baz");
        Relation relation = new Relation(one, two);

        Relation.ApiModel apiModel = new Relation.ApiModel(relation, two);

        assertThat(apiModel).isNotNull();
        assertThat(apiModel.direction).isEqualTo(Relation.ApiModel.INBOUND);
        assertThat(apiModel.name).isEqualTo("bar");
    }

    @Test
    void inboundName() {
        Item one = new Item("foo", "bar");
        one.setName("huhu");
        Item two = new Item("foo", "baz");
        Relation relation = new Relation(one, two);

        Relation.ApiModel apiModel = new Relation.ApiModel(relation, two);

        assertThat(apiModel).isNotNull();
        assertThat(apiModel.direction).isEqualTo(Relation.ApiModel.INBOUND);
        assertThat(apiModel.name).isEqualTo("huhu");
    }
}