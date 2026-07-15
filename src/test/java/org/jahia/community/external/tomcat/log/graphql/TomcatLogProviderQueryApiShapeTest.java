package org.jahia.community.external.tomcat.log.graphql;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * S16 (G1) — API-shape regression guard, Critical.
 *
 * <p>The tail operation must never accept a caller-controlled path/filename. It reads only the
 * hardcoded {@code ${catalina.base}/logs/jahia.log}. If anyone ever adds a String path/file/name
 * parameter to {@code logTail}, this test fails and forces a security review before a path
 * traversal can be re-introduced.
 */
@DisplayName("TomcatLogProviderQuery API shape (no caller-controlled path)")
class TomcatLogProviderQueryApiShapeTest {

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

    @Test
    @DisplayName("logTail's only parameter is a single Integer line count — no String path parameter")
    void logTail_hasNoCallerControlledPathParameter() {
        List<Method> tailMethods = Arrays.stream(TomcatLogProviderQuery.class.getMethods())
                .filter(m -> "logTail".equals(m.getName()))
                .toList();

        assertThat(tailMethods)
                .as("exactly one logTail method")
                .hasSize(1);

        Class<?>[] paramTypes = tailMethods.get(0).getParameterTypes();
        assertThat(paramTypes)
                .as("logTail must accept only a single Integer line count and no String path/file/name")
                .containsExactly(Integer.class);
    }

    @Test
    @DisplayName("logTail reads exactly ${catalina.base}/logs/jahia.log")
    void logTail_readsHardcodedJahiaLog() throws IOException {
        Files.write(logFile, "line-1\nline-2\n".getBytes(StandardCharsets.UTF_8));

        List<String> tail = new TomcatLogProviderQuery().logTail(5);

        // Basename is jahia.log by construction; the content proves the file that was read.
        assertThat(logFile.getFileName().toString()).isEqualTo("jahia.log");
        assertThat(tail).contains("line-1", "line-2");
    }
}
