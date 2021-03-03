package de.bonndan.nivio.observation;

import de.bonndan.nivio.model.Landscape;
import de.bonndan.nivio.model.LandscapeBuilder;
import de.bonndan.nivio.model.LandscapeFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class LocalFileObserverTest {

    private Landscape landscape;
    private ApplicationEventPublisher publisher;

    @BeforeEach
    void setup() {
        landscape = LandscapeFactory.createForTesting("foo", "bar").build();
        publisher = mock(ApplicationEventPublisher.class);
    }

    @Test
    void checksFileExists() {
        assertThrows(Exception.class, () -> new LocalFileObserver(landscape, publisher, new File("boohoo")));
    }

    @Test
    void detectsFileChange() throws IOException, InterruptedException {
        Path tempFile = Files.createTempFile("foo", "bar");
        LocalFileObserver localFileObserver = new LocalFileObserver(landscape, publisher, tempFile.toFile());
        Thread thread = new Thread(localFileObserver);
        thread.start();
        Thread.sleep(1000);
        Files.write(tempFile, "foo".getBytes(StandardCharsets.UTF_8), StandardOpenOption.WRITE);
        Thread.sleep(4000);

        thread.interrupt();
        tempFile.toFile().deleteOnExit();

        verify(publisher).publishEvent(any(InputChangedEvent.class));
    }

    @Test
    void doesNotCareAboutOtherFileChange() throws IOException, InterruptedException {
        Path tempFile = Files.createTempFile("foo", "bar");
        Path tempFile2 = Files.createTempFile("foo", "baz");
        LocalFileObserver localFileObserver = new LocalFileObserver(landscape, publisher, tempFile.toFile());
        Thread thread = new Thread(localFileObserver);
        thread.start();
        Thread.sleep(1000);
        Files.write(tempFile2, "foo".getBytes(StandardCharsets.UTF_8), StandardOpenOption.WRITE);
        Thread.sleep(4000);

        thread.interrupt();
        tempFile.toFile().deleteOnExit();

        verify(publisher, never()).publishEvent(any(InputChangedEvent.class));
    }
}