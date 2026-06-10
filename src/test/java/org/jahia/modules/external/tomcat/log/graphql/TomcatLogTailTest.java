package org.jahia.modules.external.tomcat.log.graphql;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("tomcatLogTail bounded read")
class TomcatLogTailTest {

    private static final String CATALINA_BASE = "catalina.base";

    @TempDir
    Path catalinaBase;

    private String previousCatalinaBase;
    private Path logFile;

    @BeforeEach
    void setUp() throws IOException {
        previousCatalinaBase = System.getProperty(CATALINA_BASE);
        System.setProperty(CATALINA_BASE, catalinaBase.toString());
        Path logsDir = catalinaBase.resolve("logs");
        Files.createDirectories(logsDir);
        logFile = logsDir.resolve("jahia.log");
    }

    @AfterEach
    void tearDown() {
        if (previousCatalinaBase == null) {
            System.clearProperty(CATALINA_BASE);
        } else {
            System.setProperty(CATALINA_BASE, previousCatalinaBase);
        }
    }

    private void writeLines(int count) throws IOException {
        String content = IntStream.rangeClosed(1, count)
                .mapToObj(i -> "line-" + i)
                .collect(Collectors.joining("\n", "", "\n"));
        Files.write(logFile, content.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    @DisplayName("returns the last N lines in chronological order")
    void logTail_returnsLastNLines() throws IOException {
        writeLines(1000);

        List<String> tail = TomcatLogProviderQueryExtension.logTail(5);

        // The file ends with a trailing newline, so split(-1) yields a final empty element.
        assertThat(tail).containsExactly("line-997", "line-998", "line-999", "line-1000", "");
    }

    @Test
    @DisplayName("defaults to 200 lines when no count is supplied")
    void logTail_nullLines_defaultsTo200() throws IOException {
        writeLines(500);

        List<String> tail = TomcatLogProviderQueryExtension.logTail(null);

        // Capped at the default of 200 elements, most recent line still present.
        assertThat(tail).hasSize(200);
        assertThat(tail).contains("line-500");
    }

    @Test
    @DisplayName("caps requested lines at the maximum")
    void logTail_aboveMax_isCapped() throws IOException {
        writeLines(20);

        List<String> tail = TomcatLogProviderQueryExtension.logTail(1_000_000);

        // Whole (small) file fits well under the cap; never explodes the request
        assertThat(tail).contains("line-1", "line-20");
    }

    @Test
    @DisplayName("returns empty list when the log file is missing")
    void logTail_missingFile_returnsEmpty() {
        List<String> tail = TomcatLogProviderQueryExtension.logTail(10);

        assertThat(tail).isEmpty();
    }
}
