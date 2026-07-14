package org.jahia.community.external.tomcat.log.graphql;

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

    // Each line is padded to ~100 bytes so a modest line count comfortably exceeds the
    // 256 KB tail window, proving the byte-window seek (not just the line cap) bounds the read.
    private void writeLongLines(int count) throws IOException {
        StringBuilder pad = new StringBuilder();
        for (int i = 0; i < 90; i++) {
            pad.append('x');
        }
        String suffix = pad.toString();
        String content = IntStream.rangeClosed(1, count)
                .mapToObj(i -> "line-" + i + "-" + suffix)
                .collect(Collectors.joining("\n", "", "\n"));
        Files.write(logFile, content.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    @DisplayName("returns the last N lines in chronological order")
    void logTail_returnsLastNLines() throws IOException {
        writeLines(1000);

        List<String> tail = new TomcatLogProviderQuery().logTail(5);

        // The file ends with a trailing newline, so split(-1) yields a final empty element.
        assertThat(tail).containsExactly("line-997", "line-998", "line-999", "line-1000", "");
    }

    @Test
    @DisplayName("defaults to 200 lines when no count is supplied")
    void logTail_nullLines_defaultsTo200() throws IOException {
        writeLines(500);

        List<String> tail = new TomcatLogProviderQuery().logTail(null);

        // Capped at the default of 200 elements, most recent line still present.
        assertThat(tail).hasSize(200).contains("line-500");
    }

    @Test
    @DisplayName("S11: caps at MAX_TAIL_LINES (5000) under real pressure with a large file")
    void logTail_aboveMax_isCappedAt5000() throws IOException {
        // 10 000 lines exceeds the 5000 cap (the old 20-line test never exercised the cap).
        writeLines(10_000);

        List<String> tail = new TomcatLogProviderQuery().logTail(1_000_000);

        // 5000 lines + trailing empty element from split("\n", -1).
        assertThat(tail).hasSizeLessThanOrEqualTo(5001);
        assertThat(tail).contains("line-10000");
        assertThat(tail).doesNotContain("line-1");
    }

    @Test
    @DisplayName("S12: reads only the trailing 256 KB window regardless of file size (no DoS)")
    void logTail_readsOnly256KbWindow() throws IOException {
        // ~1 MB file of ~100-byte lines. 256 KB holds far fewer than 5000 such lines, so the
        // byte-window (not the line cap) bounds the read — proving a huge file can't force a huge read.
        writeLongLines(10_000);
        assertThat(logFile.toFile().length())
                .as("file must be well over the 256 KB tail window")
                .isGreaterThan(256 * 1024L);

        long start = System.nanoTime();
        List<String> tail = new TomcatLogProviderQuery().logTail(5000);
        long elapsedMs = (System.nanoTime() - start) / 1_000_000;

        // Fewer lines than requested are returned because only the last 256 KB is read.
        assertThat(tail.size())
                .as("byte window bounds the read below the requested 5000 lines")
                .isLessThan(5000);
        assertThat(tail).noneMatch(l -> l.startsWith("line-1-"));
        assertThat(tail).anyMatch(l -> l.startsWith("line-10000-"));
        assertThat(elapsedMs).as("bounded read completes quickly").isLessThan(5000);
    }

    @Test
    @DisplayName("S13: <=0 requested lines falls back to the default 200")
    void logTail_nonPositiveLines_defaultsTo200() throws IOException {
        writeLines(500);

        assertThat(new TomcatLogProviderQuery().logTail(0)).hasSize(200);
        assertThat(new TomcatLogProviderQuery().logTail(-5)).hasSize(200);
    }

    @Test
    @DisplayName("returns empty list when the log file is missing")
    void logTail_missingFile_returnsEmpty() {
        List<String> tail = new TomcatLogProviderQuery().logTail(10);

        assertThat(tail).isEmpty();
    }

    @Test
    @DisplayName("S15: returns empty (no exception) when catalina.base is unset")
    void logTail_catalinaBaseUnset_returnsEmpty() {
        System.clearProperty(CATALINA_BASE);

        List<String> tail = new TomcatLogProviderQuery().logTail(10);

        assertThat(tail).isEmpty();
    }
}
