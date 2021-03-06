package de.bonndan.nivio.output.map.svg;

import de.bonndan.nivio.model.Group;
import de.bonndan.nivio.model.Item;
import de.bonndan.nivio.model.Landscape;
import de.bonndan.nivio.model.Relation;
import de.bonndan.nivio.output.layout.LayoutedComponent;
import de.bonndan.nivio.output.map.hex.Hex;
import de.bonndan.nivio.output.map.hex.HexMap;
import j2html.tags.ContainerTag;
import j2html.tags.DomContent;
import j2html.tags.UnescapedText;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.util.StringUtils;

import java.awt.geom.Point2D;
import java.util.*;
import java.util.stream.Collectors;

import static j2html.TagCreator.rawHtml;


/**
 * Creates an SVG document based on pre-rendered map items.
 */
public class SVGDocument extends Component {

    private static final Logger LOGGER = LoggerFactory.getLogger(SVGDocument.class);
    public static final int LABEL_WIDTH = 140;

    private final LayoutedComponent layouted;
    private final Landscape landscape;
    private final String cssStyles;
    private boolean debug = false;
    private HexMap hexMap;

    public SVGDocument(@NonNull LayoutedComponent layouted, @Nullable String cssStyles) {
        this.layouted = Objects.requireNonNull(layouted);
        this.landscape = (Landscape) layouted.getComponent();
        this.cssStyles = StringUtils.isEmpty(cssStyles) ? "" : cssStyles;
    }

    public void setDebug(boolean debug) {
        this.debug = debug;
    }

    public DomContent render() {

        List<DomContent> defs = new ArrayList<>();
        List<DomContent> items = new ArrayList<>();

        hexMap = new HexMap(this.debug);

        //transform all item positions to hex map positions
        layouted.getChildren().forEach(group -> {
            LOGGER.info("rendering group {} with items {}", group.getComponent().getIdentifier(), group.getChildren());
            group.getChildren().forEach(layoutedItem -> {

                hexMap.add(layoutedItem);

                Item item = (Item) layoutedItem.getComponent();
                //collect patterns for icons
                if (!StringUtils.isEmpty(layoutedItem.getFill())) {
                    SVGPattern SVGPattern = new SVGPattern(layoutedItem.getFill());
                    defs.add(SVGPattern.render());
                }

                //render icons
                SVGItemLabel label = new SVGItemLabel(item);
                Point2D.Double pos = hexMap.hexForItem(item).toPixel();

                SVGItem SVGItem = new SVGItem(label.render(), layoutedItem, pos);
                items.add(SVGItem.render());
            });
        });

        List<SVGGroupArea> groupAreas = new ArrayList<>();
        List<DomContent> groups = layouted.getChildren().stream().map(groupLayout -> {
            Set<Hex> groupArea = hexMap.getGroupArea((Group) groupLayout.getComponent());
            SVGGroupArea area = SVGGroupAreaFactory.getGroup((Group) groupLayout.getComponent(), groupArea, debug);
            groupAreas.add(area);
            return area.render();
        }).collect(Collectors.toList());

        List<SVGRelation> relations = getRelations(layouted);

        SVGDimension dimension = SVGDimensionFactory.getDimension(groupAreas);

        //render background hexes
        defs.add(SVGBackgroundFactory.getHex());


        List<DomContent> background = new ArrayList<>(
                //SVGBackgroundFactory.getBackgroundTiles(dimension)
        );

        DomContent title = getTitle(dimension);
        DomContent logo = getLogo(dimension);

        UnescapedText style = rawHtml("<style>\n" + cssStyles + "</style>");


        return SvgTagCreator.svg(style)
                .attr("version", "1.1")
                .attr("xmlns", "http://www.w3.org/2000/svg")
                .attr("xmlns:xlink", "http://www.w3.org/1999/xlink")
                .attr("width", dimension.cartesian.horMax)
                .attr("height", dimension.cartesian.vertMax)
                .attr("viewBox", dimension.cartesian.asViewBox())
                .attr("class", "map")

                .with(background)
                .with(logo, title)
                .with(groups)
                .with(relations.stream().map(SVGRelation::render))
                .with(items)
                .with(SvgTagCreator.defs().with(defs));
    }

    @Nullable
    private DomContent getLogo(SVGDimension dimension) {
        DomContent logo = null;
        String logoUrl = landscape.getIcon(); //has been set by appearance resolver
        if (!StringUtils.isEmpty(logoUrl)) {
            logo = SvgTagCreator.image()
                    .attr("xlink:href", logoUrl)
                    .attr("x", dimension.cartesian.horMin - dimension.cartesian.padding)
                    .attr("y", dimension.cartesian.vertMin - dimension.cartesian.padding + 80)
                    .attr("width", LABEL_WIDTH)
                    .attr("height", LABEL_WIDTH);
        }
        return logo;
    }

    private ContainerTag getTitle(SVGDimension dimension) {
        return SvgTagCreator.text(landscape.getName())
                .attr("x", dimension.cartesian.horMin - dimension.cartesian.padding)
                .attr("y", dimension.cartesian.vertMin - dimension.cartesian.padding + 60)
                .attr("class", "title");
    }

    /**
     * Iterates over all items and invokes pathfinding for their relations.
     */
    private List<SVGRelation> getRelations(LayoutedComponent layouted) {
        List<SVGRelation> relations = new ArrayList<>();
        layouted.getChildren().forEach(layoutedGroup -> {
            layoutedGroup.getChildren().forEach(layoutedItem -> {
                Item item = (Item) layoutedItem.getComponent();
                LOGGER.debug("Adding {} relations for {}", item.getRelations().size(), item.getFullyQualifiedIdentifier());
                item.getRelations().stream()
                        .filter(rel -> rel.getSource().equals(item)) //do not paint twice / incoming (inverse) relations
                        .map(rel -> getSvgRelation(layoutedItem, item, rel))
                        .filter(Objects::nonNull)
                        .forEach(relations::add);
            });
        });

        return relations;
    }

    private SVGRelation getSvgRelation(LayoutedComponent layoutedItem, Item item, Relation rel) {
        Optional<HexPath> bestPath = hexMap.getPath(item, rel.getTarget());
        if (bestPath.isPresent()) {
            SVGRelation svgRelation = new SVGRelation(bestPath.get(), layoutedItem.getColor(), rel);
            LOGGER.debug("Added path for item {} relation {} -> {}", item, rel.getSource(), rel.getTarget());
            return svgRelation;
        }
        LOGGER.error("No path found for item {} relation {}", item, rel);
        return null;
    }

    public String getXML() {
        return render().render();
    }
}

