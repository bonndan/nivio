package de.bonndan.nivio.input;

import de.bonndan.nivio.input.dto.GroupDescription;
import de.bonndan.nivio.input.dto.ItemDescription;
import de.bonndan.nivio.input.dto.LandscapeDescription;
import de.bonndan.nivio.input.external.ExternalLinkHandler;
import de.bonndan.nivio.input.external.LinkHandlerFactory;
import de.bonndan.nivio.model.Group;
import de.bonndan.nivio.model.Item;
import de.bonndan.nivio.model.Link;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static de.bonndan.nivio.input.external.LinkHandlerFactory.GITHUB;
import static java.util.Optional.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class LinksResolverTest {

    private LinkHandlerFactory linkHandlerFactory;
    private LinksResolver resolver;
    private LandscapeDescription landscapeDescription;
    private GroupDescription aGroup;

    @BeforeEach
    void setUp() {
        linkHandlerFactory = mock(LinkHandlerFactory.class);
        resolver = new LinksResolver(mock(ProcessLog.class), linkHandlerFactory);

        landscapeDescription = new LandscapeDescription();
        aGroup = new GroupDescription();
        landscapeDescription.getGroups().put("foo", aGroup);
    }

    @Test
    void testProcessLandscapeLinks() throws MalformedURLException {
        //given
        Link link = new Link(new URL("https://github.com/dedica-team/nivio/"), GITHUB);
        landscapeDescription.setLinks(Map.of(GITHUB, link));
        ExternalLinkHandler mockResolver = mock(ExternalLinkHandler.class);
        when(linkHandlerFactory.getResolver(eq(GITHUB))).thenReturn(ofNullable(mockResolver));
        when(mockResolver.resolve(eq(link))).thenReturn(CompletableFuture.completedFuture(new ItemDescription()));

        //when
        resolver.process(landscapeDescription);

        //then
        verify(linkHandlerFactory, times(1)).getResolver(eq(GITHUB));
        verify(mockResolver, times(1)).resolve(eq(link));
    }

    @Test
    void testProcessGroupLinks() throws MalformedURLException {
        //given
        Link link = new Link(new URL("https://github.com/dedica-team/nivio/"), GITHUB);
        aGroup.setLinks(Map.of(GITHUB, link));

        ExternalLinkHandler mockResolver = mock(ExternalLinkHandler.class);
        when(linkHandlerFactory.getResolver(eq(GITHUB))).thenReturn(ofNullable(mockResolver));
        when(mockResolver.resolve(eq(link))).thenReturn(CompletableFuture.completedFuture(new GroupDescription()));

        //when
        resolver.process(landscapeDescription);

        //then
        verify(linkHandlerFactory, times(1)).getResolver(eq(GITHUB));
        verify(mockResolver, times(1)).resolve(eq(link));
    }

    @Test
    void testProcessItemLinks() throws MalformedURLException {
        //given
        Link link = new Link(new URL("https://github.com/dedica-team/nivio/"), GITHUB);
        ItemDescription item = new ItemDescription("foo");
        item.setLinks(Map.of(GITHUB, link));
        landscapeDescription.getItemDescriptions().add(item);

        ExternalLinkHandler mockResolver = mock(ExternalLinkHandler.class);
        when(linkHandlerFactory.getResolver(eq(GITHUB))).thenReturn(ofNullable(mockResolver));
        when(mockResolver.resolve(eq(link))).thenReturn(CompletableFuture.completedFuture(new ItemDescription()));

        //when
        resolver.process(landscapeDescription);

        //then
        verify(linkHandlerFactory, times(1)).getResolver(eq(GITHUB));
        verify(mockResolver, times(1)).resolve(eq(link));
    }

    @Test
    void testThrowable() throws MalformedURLException {
        //given
        Link link = new Link(new URL("https://github.com/dedica-team/nivio/"), GITHUB);
        landscapeDescription.setLinks(Map.of(GITHUB, link));

        ExternalLinkHandler mockResolver = mock(ExternalLinkHandler.class);
        when(linkHandlerFactory.getResolver(eq(GITHUB))).thenReturn(ofNullable(mockResolver));
        when(mockResolver.resolve(eq(link))).thenThrow(new RuntimeException("foobar"));

        //when
        resolver.process(landscapeDescription);

        //then
        verify(linkHandlerFactory, times(1)).getResolver(eq(GITHUB));
        verify(mockResolver, times(1)).resolve(eq(link));

    }
}