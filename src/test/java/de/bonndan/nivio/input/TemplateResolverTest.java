package de.bonndan.nivio.input;

import de.bonndan.nivio.input.compose2.InputFormatHandlerCompose2;
import de.bonndan.nivio.input.dto.ItemDescription;
import de.bonndan.nivio.input.dto.LandscapeDescription;
import de.bonndan.nivio.input.http.HttpService;
import de.bonndan.nivio.input.nivio.InputFormatHandlerNivio;
import de.bonndan.nivio.model.Label;
import de.bonndan.nivio.model.Tagged;
import de.bonndan.nivio.util.RootPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;

import java.io.File;
import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;

class TemplateResolverTest {

    private TemplateResolver templateResolver;
    private LandscapeDescriptionFactory factory;

    @Mock
    ProcessLog log;

    @BeforeEach
    public void setup() {
        log = new ProcessLog(LoggerFactory.getLogger(TemplateResolver.class));
        templateResolver = new TemplateResolver(mock(ProcessLog.class));
        FileFetcher fileFetcher = new FileFetcher(mock(HttpService.class));
        factory = new LandscapeDescriptionFactory(fileFetcher);
    }


    @Test
    public void assignTemplateToAll() {

        LandscapeDescription landscapeDescription = getLandscapeDescription("/src/test/resources/example/example_templates.yml");
        templateResolver.resolve(landscapeDescription);

        ItemDescription redis = landscapeDescription.getItemDescriptions().pick("redis", null);
        assertNotNull(redis);
        assertEquals("foo", redis.getGroup());

        ItemDescription datadog = landscapeDescription.getItemDescriptions().pick("datadog", null);
        assertNotNull(datadog);
        assertEquals("foo", datadog.getGroup());

        //web has previously been assigned to group "content" and will not be overwritten by further templates
        ItemDescription web = landscapeDescription.getItemDescriptions().pick("web", null);
        assertNotNull(web);
        assertEquals("content", web.getGroup());
    }


    @Test
    public void assignTemplateWithRegex() {

        LandscapeDescription landscapeDescription = getLandscapeDescription("/src/test/resources/example/example_templates2.yml");
        templateResolver.resolve(landscapeDescription);

        ItemDescription one = landscapeDescription.getItemDescriptions().pick("crappy_dockername-78345", null);
        assertNotNull(one);
        assertEquals("alpha", one.getGroup());

        ItemDescription two = landscapeDescription.getItemDescriptions().pick("crappy_dockername-2343a", null);
        assertNotNull(two);
        assertEquals("alpha", two.getGroup());

        ItemDescription three = landscapeDescription.getItemDescriptions().pick("other_crappy_name-2343a", null);
        assertNotNull(three);
        assertEquals("beta", three.getGroup());
    }

    @Test
    public void assignsAllValues() {

        LandscapeDescription landscapeDescription = getLandscapeDescription("/src/test/resources/example/example_templates.yml");
        templateResolver.resolve(landscapeDescription);


        //web has previously been assigned to group "content" and will not be overwritten by further templates
        ItemDescription web = landscapeDescription.getItemDescriptions().pick("web", null);
        assertNotNull(web);
        assertEquals("content", web.getGroup());

        //other values from template
        assertNull(web.getName());
        assertEquals("Wordpress", web.getLabel(Label.software));
        assertEquals("alphateam", web.getLabel(Label.team));
        assertEquals("alphateam@acme.io", web.getContact());
        assertEquals(1, web.getLabels(Tagged.LABEL_PREFIX_TAG).size());
    }

    @Test
    public void assignsOnlyToGivenTargets() {

        LandscapeDescription landscapeDescription = getLandscapeDescription("/src/test/resources/example/example_templates.yml");
        templateResolver.resolve(landscapeDescription);

        ItemDescription redis = landscapeDescription.getItemDescriptions().pick("redis", null);
        assertNotNull(redis);
        assertNull(redis.getLabel(Label.software));
    }

    private LandscapeDescription getLandscapeDescription(String s) {
        File file = new File(RootPath.get() + s);
        InputFormatHandlerFactory formatFactory = new InputFormatHandlerFactory(
                new ArrayList<>(Arrays.asList(new InputFormatHandlerNivio(new FileFetcher(new HttpService())), InputFormatHandlerCompose2.forTesting()))
        );
        SourceReferencesResolver sourceReferencesResolver = new SourceReferencesResolver(formatFactory, new ProcessLog(mock(Logger.class)), mock(ApplicationEventPublisher.class));
        LandscapeDescription landscapeDescription = factory.fromYaml(file);
        sourceReferencesResolver.resolve(landscapeDescription);
        return landscapeDescription;
    }

}
