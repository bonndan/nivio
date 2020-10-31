package de.bonndan.nivio.model;

import de.bonndan.nivio.input.dto.LandscapeDescription;

public class LandscapeFactory {

    /**
     * Creates a new landscape impl.
     *
     * @param input the description
     */
    public static Landscape create(LandscapeDescription input) {
        Landscape landscape = create(input.getIdentifier());
        landscape.setSource(input.getSource());
        return landscape;
    }

    /**
     * This factory method can be used to create landscapes for testing.
     *
     * @param identifier landscape identifier
     * @return new landscape
     */
    public static Landscape create(String identifier) {
        return new Landscape(identifier, new Group(Group.COMMON));
    }

    public static void assignAll(LandscapeDescription input, Landscape landscape) {
        landscape.setName(input.getName());
        landscape.setContact(input.getContact());
        landscape.setConfig(input.getConfig());
        landscape.setDescription(input.getDescription());
        landscape.setOwner(input.getOwner());
        input.getLabels().forEach((s, s2) -> landscape.getLabels().put(s, s2));
        input.getLinks().forEach((s, link) -> landscape.getLinks().put(s, link));
    }
}
