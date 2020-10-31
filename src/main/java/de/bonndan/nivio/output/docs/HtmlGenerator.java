package de.bonndan.nivio.output.docs;

import de.bonndan.nivio.model.Landscape;
import de.bonndan.nivio.output.LocalServer;
import de.bonndan.nivio.output.icons.IconService;
import j2html.tags.ContainerTag;

import java.net.URL;

import static j2html.TagCreator.*;


public abstract class HtmlGenerator {

    protected final LocalServer localServer;
    protected final IconService iconService;

    protected HtmlGenerator(LocalServer localServer, IconService iconService) {
        this.localServer = localServer;
        this.iconService = iconService;
    }

    protected ContainerTag getHead(Landscape landscape) {

        URL css = localServer.getUrl("/css/bootstrap.min.css").orElse(null);
        return head(
                title(landscape.getName()),
                link().condAttr(css != null, "rel", "stylesheet").attr("href", css),
                meta().attr("charset", "utf-8"),
                meta().attr("name", "viewport").attr("content", "width=device-width, initial-scale=1, shrink-to-fit=no"),
                meta().attr("name", "description").attr("content", landscape.getName()),
                meta().attr("name", "author").attr("content", landscape.getContact()),
                meta().attr("generator", "author").attr("content", "nivio"),
                style("html {margin: 1rem} .group{margin-top: 1rem;} .card{margin-bottom: 1rem;}").attr("type", "text/css")
        );
    }

}
