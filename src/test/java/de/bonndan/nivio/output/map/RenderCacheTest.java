package de.bonndan.nivio.output.map;

import de.bonndan.nivio.ProcessingFinishedEvent;
import de.bonndan.nivio.input.AppearanceResolver;
import de.bonndan.nivio.input.ProcessLog;
import de.bonndan.nivio.input.dto.LandscapeDescription;
import de.bonndan.nivio.input.http.HttpService;
import de.bonndan.nivio.model.Group;
import de.bonndan.nivio.model.Item;
import de.bonndan.nivio.model.Landscape;
import de.bonndan.nivio.model.LandscapeFactory;
import de.bonndan.nivio.output.icons.IconService;
import de.bonndan.nivio.output.map.svg.MapStyleSheetFactory;
import de.bonndan.nivio.output.map.svg.SVGRenderer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class RenderCacheTest {

    private RenderCache renderCache;
    private MapStyleSheetFactory stylesheetFactory;
    private SVGRenderer svgRenderer;

    @BeforeEach
    public void setup() {

        stylesheetFactory = mock(MapStyleSheetFactory.class);
        svgRenderer = new SVGRenderer(stylesheetFactory);
        renderCache = new RenderCache(svgRenderer);
        when(stylesheetFactory.getMapStylesheet(any(), any())).thenReturn("");
    }

    @Test
    void toPNGCached() {
        Landscape landscape = getLandscape("test");
        String first = renderCache.getSVG(landscape, false);
        String second = renderCache.getSVG(landscape, false);

        verify(stylesheetFactory, times(1)).getMapStylesheet(any(), any());
    }

    @Test
    void cachesBasedOnIdentifier() {
        Landscape one = getLandscape("test");
        String first = renderCache.getSVG(getLandscape("test"), false);
        Landscape two = getLandscape("test");
        two.setProcessLog(one.getLog()); //sync last update
        two.setIdentifier("second");
        String second = renderCache.getSVG(two, false);

        verify(stylesheetFactory, times(2)).getMapStylesheet(any(), any());
    }

    @Test
    void toSVG() {
        String svg = renderCache.getSVG(getLandscape("test"), true);
        assertNotNull(svg);
        assertTrue(svg.contains("svg"));
    }

    @Test
    void onProcessingFinishedEvent() {
        renderCache.onApplicationEvent(new ProcessingFinishedEvent(new LandscapeDescription(), getLandscape("test")));

        verify(stylesheetFactory, times(1)).getMapStylesheet(any(), any());
    }

    private Landscape getLandscape(String identifier) {
        Landscape landscape = LandscapeFactory.create(identifier);

        Item item = new Item("bar", "foo");
        landscape.setItems(Set.of(item));
        landscape.setItems(Collections.singleton(item));

        Group bar = new Group("bar");
        bar.addItem(item);
        landscape.getGroups().put("bar", bar);

        ProcessLog test = new ProcessLog(LoggerFactory.getLogger("test"));
        test.info("foo");
        landscape.setProcessLog(test);

        HttpService httpService = mock(HttpService.class);
        new AppearanceResolver(landscape.getLog(), mock(IconService.class)).process(null, landscape);
        return landscape;
    }
}