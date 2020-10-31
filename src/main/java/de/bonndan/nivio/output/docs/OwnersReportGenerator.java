package de.bonndan.nivio.output.docs;

import de.bonndan.nivio.model.*;
import de.bonndan.nivio.output.Color;
import de.bonndan.nivio.output.FormatUtils;
import de.bonndan.nivio.output.LocalServer;
import de.bonndan.nivio.output.icons.IconService;
import de.bonndan.nivio.output.icons.LocalIcons;
import j2html.tags.ContainerTag;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static j2html.TagCreator.*;

public class OwnersReportGenerator extends HtmlGenerator {

    public OwnersReportGenerator(LocalServer localServer, IconService iconService) {
        super(localServer, iconService);
    }

    public String toDocument(Landscape landscape) {

        return writeLandscape(landscape);
    }

    private String writeLandscape(Landscape landscape) {

        return html(
                getHead(landscape),
                body(
                        h1("Owner Report: " + landscape.getName()),
                        br(),
                        rawHtml(writeOwnerGroups(Groups.by(Component::getOwner, new ArrayList<>(landscape.getItems().all()))))
                )
        ).renderFormatted();
    }

    private String writeOwnerGroups(Groups ownerGroups) {
        final StringBuilder builder = new StringBuilder();
        ownerGroups.getAll().forEach((owner, landscapeItems) -> {
            builder.append(
                    h2(rawHtml(owner)).attr("class", "rounded").render()
            );
            builder.append(writeGroups(Groups.by(Item::getGroup, landscapeItems)).render());
        });

        return builder.toString();
    }

    private ContainerTag writeGroups(Groups groups) {
        List<ContainerTag> collect = new ArrayList<>();
        groups.getAll().entrySet().forEach(entry -> collect.add(writeGroup(entry)));
        return ul().with(collect);
    }

    private ContainerTag writeGroup(Map.Entry<String, List<Item>> services) {
        return li().with(services.getValue().stream().map(this::writeItem));
    }

    private ContainerTag writeItem(Item item) {
        String groupColor = "#" + Color.nameToRGB(item.getGroup());

        return div(rawHtml("<span style=\"color: " + groupColor + "\">&#9899;</span> " + FormatUtils.nice(item.getGroup()) + ": " + item.toString() + " (" + item.getFullyQualifiedIdentifier().toString() + ")"));

    }

}
