package de.bonndan.nivio.output.icons;

import de.bonndan.nivio.model.Item;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class IconServiceTest {

    private LocalIcons localIcons;
    private ExternalIcons externalIcons;
    private IconService iconService;

    @BeforeEach
    public void setup() {
        externalIcons = mock(ExternalIcons.class);
        localIcons = new LocalIcons();
        iconService = new IconService(localIcons, externalIcons);
    }


    @Test
    public void returnsServiceWithUnknownType() {
        Item item = new Item("test", "a");
        item.setType("asb");

        String icon = localIcons.getIconUrl(IconMapping.DEFAULT_ICON.getIcon()).orElseThrow();
        assertThat(iconService.getIconUrl(new Item("test", "a"))).isEqualTo(icon);

    }

    @Test
    public void returnsType() {
        Item item = new Item("test", "a");
        item.setType("account");

        String icon = localIcons.getIconUrl("account").orElseThrow();
        assertThat(iconService.getIconUrl(item)).isEqualTo(icon);
    }

    @Test
    public void returnsCustomIcon() {
        Item item = new Item("test", "a");
        item.setIcon("http://my.icon");

        assertThat(iconService.getIconUrl(item)).isEqualTo("http://my.icon");
    }

    @Test
    public void returnsVendorIcon() throws MalformedURLException {
        Item item = new Item("test", "a");
        item.setIcon("vendor://redis");

        URL url = new URL("http://foo.com/bar.png");
        when(externalIcons.getUrl(eq("redis"))).thenReturn(Optional.of(url.toString()));

        //when
        String s = iconService.getIconUrl(item);
        verify(externalIcons).getUrl(eq("redis"));
        assertEquals(url.toString(), s);
    }

    @Test
    public void getFillUrl() throws MalformedURLException {
        when(externalIcons.getUrl(any(URL.class))).thenReturn(Optional.empty());

        Optional<String> fillUrl = iconService.getFillUrl(new URL("http://my.icon"));
        assertThat(fillUrl).isEmpty();
        verify(externalIcons).getUrl(any(URL.class));
    }

}