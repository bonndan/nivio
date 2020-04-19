package de.bonndan.nivio.output.map.svg;

import de.bonndan.nivio.model.Item;
import de.bonndan.nivio.model.Lifecycle;
import de.bonndan.nivio.output.Rendered;
import j2html.tags.ContainerTag;
import j2html.tags.DomContent;
import org.springframework.util.StringUtils;

import java.awt.geom.Point2D;

import static de.bonndan.nivio.output.map.svg.SvgFactory.ICON_SIZE;

class SVGItem extends Component {

    private final String id;
    private final Item item;
    private DomContent children;
    private final Point2D.Double pixel;

    SVGItem(DomContent children, Item item, Point2D.Double position) {
        this.children = children;
        this.pixel = position;
        this.item = item;
        this.id = item.getFullyQualifiedIdentifier().toString();
    }

    /**
     * Renders the fill as background if possible, otherwise tries explicit icon or shortName.
     *
     */
    public DomContent render() {

        boolean hasText = false;
        boolean hasFill = !StringUtils.isEmpty(item.getFill());
        var fillId = hasFill ? "url(#" + SVGPattern.idForLink(item.getFill()) + ")" : "white";
        DomContent content = null;
        //use the shortname as text instead
        if (!hasFill && StringUtils.isEmpty(item.getType()) && !StringUtils.isEmpty(item.getShortName())) {
            content = new SVGLabelText(item.getShortName(), "0", "3", "item_shortName").render();
            fillId = "white";
            hasText = true;
        }

        DomContent icon = null;
        if (!hasFill && !hasText && !StringUtils.isEmpty(item.getLabel(Rendered.LABEL_RENDERED_ICON))) {
            icon = SvgTagCreator.image()
                    .attr("xlink:href", item.getLabel(Rendered.LABEL_RENDERED_ICON))
                    .attr("width", ICON_SIZE)
                    .attr("height", ICON_SIZE)
                    .attr("transform", "translate(-" + ICON_SIZE / 2 + ",-" + ICON_SIZE / 2 + ")")
            ;
        }

        ContainerTag circle = SvgTagCreator.circle()
                .attr("id", this.id)
                .attr("cx", 0)
                .attr("cy", 0)
                .attr("r", ICON_SIZE - 10)
                .attr("fill", fillId)
                .attr("stroke", "#" + item.getColor());
        if (Lifecycle.PLANNED.equals(item.getLifecycle())) {
            circle.attr("stroke-dasharray", 5);
            circle.attr("opacity", 0.7);
        }
        ContainerTag inner = SvgTagCreator.g(circle, content, children)
                .attr("class", "hexagon");

        return SvgTagCreator.g(inner, icon, getScale())
                .attr("class", "hexagon-group")
                .attr("transform", "translate(" + pixel.x + "," + pixel.y + ")");
    }

    private ContainerTag getScale() {

        if (StringUtils.isEmpty(item.getScale())) {
            return null;
        }

        int scaleVal = 0;
        try {
            scaleVal = Integer.parseInt(item.getScale());
        } catch (NumberFormatException ignored) {
        }

        return SvgTagCreator.g(
                        SvgTagCreator.circle()
                                .attr("cx", 0)
                                .attr("cy", 0)
                                .attr("r", 12)
                                .attr("fill", scaleVal > 0 ? "green" : "red")
                        ,
                        SvgTagCreator.text(item.getScale())
                                .attr("transform", "translate(-" + 4 + "," + 5 + ")")
                ).attr("transform", "translate(" + 30 + "," + 30 + ")");
    }
}