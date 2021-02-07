package de.bonndan.nivio.input;

import de.bonndan.nivio.model.*;
import de.bonndan.nivio.output.icons.DataUrlHelper;
import de.bonndan.nivio.output.icons.IconService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static de.bonndan.nivio.model.ItemFactory.getTestItem;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AppearanceProcessorTest {

    private AppearanceProcessor resolver;
    private Landscape landscape;
    private IconService iconService;

    @BeforeEach
    public void setup() {

        iconService = mock(IconService.class);
        resolver = new AppearanceProcessor(new ProcessLog(LoggerFactory.getLogger(AppearanceProcessorTest.class)), iconService);

        landscape = LandscapeFactory.createForTesting("l1", "l1Landscape").build();

        Item s1 = getTestItem("g1", "s1", landscape);
        s1.setLabel(Label.type, "loadbalancer");
        Item s2 = getTestItem("g1", "s2", landscape);
        s2.setLabel(Label.icon, "https://foo.bar/icon.png");

        Group g1 = new Group("g1", "landscapeIdentifier", Set.of(s1, s2));
        landscape.addGroup(g1);
        landscape.setItems(new HashSet<>(Set.of(s1, s2)));
    }

    @Test
    public void setsItemIcons() {
        Item pick = landscape.getItems().pick("s1", "g1");
        when(iconService.getIconUrl(eq(pick))).thenReturn(DataUrlHelper.DATA_IMAGE_SVG_XML_BASE_64 + "foo");
        //when
        resolver.process(null, landscape);

        //then

        //check icon is set
        assertThat(pick.getIcon()).isEqualTo(DataUrlHelper.DATA_IMAGE_SVG_XML_BASE_64 + "foo");
    }
}