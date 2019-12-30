package de.bonndan.nivio.output.map;

import com.fasterxml.jackson.annotation.JsonInclude;
import de.bonndan.nivio.model.Item;
import de.bonndan.nivio.model.LandscapeItem;
import de.bonndan.nivio.output.Color;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static java.lang.Math.sqrt;

/**
 * JSON representation for custom rendering.
 * <p>
 * The x,y coordinates are derived from the rendered representation.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public
class ItemMapItem extends MapItem {

    public long x;
    public long y;
    public double width;
    public double height;
    public int size;
    public final String group;
    public final LandscapeItem landscapeItem;
    public final List<String> relations = new ArrayList<>();

    public ItemMapItem(Item item, String image, long x, long y, long width, long height) {
        super(item.getFullyQualifiedIdentifier().toString(), StringUtils.isEmpty(item.getName()) ? item.getIdentifier() : item.getName(), image, "item", Color.getGroupColor(item));

        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.group = item.getGroup();
        this.landscapeItem = item;
        List<String> collect = item.getRelations().stream()
                .filter(rel -> rel.getSource().equals(item))
                .map(rel -> rel.getTarget().getFullyQualifiedIdentifier().toString())
                .collect(Collectors.toList());
        relations.addAll(collect);
    }

    /**
     * Calculated hexagon map coordinates from x-y coordinates.
     * <p>
     * https://stackoverflow.com/questions/20734438/algorithm-to-generate-a-hexagonal-grid-with-coordinate-system/20751975#20751975
     * https://www.redblobgames.com/grids/hexagons/#rounding
     */
    public Hex getHex() {
        var q = (2. / 3 * x) / size;
        var r = (-1. / 3 * x + sqrt(3) / 3 * y) / size;
        FractionalHex hex = new FractionalHex(q, r, -q - r);
        return hex.hexRound();
    }

}
