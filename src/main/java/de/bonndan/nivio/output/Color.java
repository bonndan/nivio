package de.bonndan.nivio.output;

import de.bonndan.nivio.model.Group;
import de.bonndan.nivio.model.Item;
import de.bonndan.nivio.model.Landscape;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import java.util.Optional;

public class Color {

    public static String DARK = "111111";
    public static final String DARKGRAY = "333333";
    public static final String GRAY = "aaaaaa";

    private static final Logger LOGGER = LoggerFactory.getLogger(Color.class);

    /**
     * https://stackoverflow.com/questions/2464745/compute-hex-color-code-for-an-arbitrary-string
     *
     * @param name of a group etc
     * @return a hex color
     */
    public static String nameToRGB(String name) {
        return nameToRGB(name, "FFFFFF");
    }

    public static String nameToRGB(String name, String defaultColor) {
        if (StringUtils.isEmpty(name))
            return defaultColor;

        return String.format("%X", name.hashCode()).concat("000000").substring(0, 6);
    }

    public static String lighten(String color) {
        try {
            java.awt.Color col = java.awt.Color.decode(color.startsWith("#") ? color : "#" + color);
            return Integer.toHexString(col.brighter().getRGB());
        } catch (IllegalArgumentException ex) {
            LOGGER.error(color + " --> " + ex.getMessage());
            return color;
        }
    }

    public static String getGroupColor(Item item) {
        if (item.getGroup() == null || item.getGroup().startsWith(Group.COMMON))
            return GRAY;

        return getGroupColor(item.getGroup(), item.getLandscape());
    }

    public static String getGroupColor(String name, Landscape landscape) {
        Group g = landscape.getGroup(name).orElse(landscape.getGroup(Group.COMMON).orElse(null));
        return getGroupColor(g);
    }

    public static String getGroupColor(Group group) {
        if (group == null) {
            return Color.DARKGRAY;
        }
        return Optional.ofNullable(group.getColor())
                .orElse(getGroupColor(group.getIdentifier()));
    }

    public static String getGroupColor(String groupIdentifier) {
        return Color.nameToRGB(groupIdentifier, Color.DARKGRAY);
    }
}
