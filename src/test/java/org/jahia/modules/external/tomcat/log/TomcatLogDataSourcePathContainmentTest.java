package org.jahia.modules.external.tomcat.log;

import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

@DisplayName("TomcatLogDataSource path containment")
class TomcatLogDataSourcePathContainmentTest {

    private static final String CATALINA_BASE = "catalina.base";

    @TempDir
    Path catalinaBase;

    private String previousCatalinaBase;
    private Path logsDir;
    private TomcatLogDataSource dataSource;

    @BeforeEach
    void setUp() throws IOException {
        previousCatalinaBase = System.getProperty(CATALINA_BASE);
        System.setProperty(CATALINA_BASE, catalinaBase.toString());

        logsDir = catalinaBase.resolve("logs");
        Files.createDirectories(logsDir);
        Files.write(logsDir.resolve("jahia.log"), "hello\n".getBytes(StandardCharsets.UTF_8));

        dataSource = new TomcatLogDataSource();
        dataSource.setRoot();
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
    @DisplayName("resolves a legitimate log file inside the log root")
    void getFile_legitimateFile_resolvesWithinRoot() throws FileSystemException {
        FileObject file = dataSource.getFile("/jahia.log");

        assertThat(file.getName().getPath()).startsWith(dataSource.getRootPath());
    }

    @Test
    @DisplayName("blank or null path returns the log root itself")
    void getFile_blankPath_returnsRoot() throws FileSystemException {
        FileObject root = dataSource.getRoot();
        assertThat((Object) dataSource.getFile(null)).isSameAs(root);
        assertThat((Object) dataSource.getFile("")).isSameAs(root);
        assertThat((Object) dataSource.getFile("/")).isSameAs(root);
    }

    @Test
    @DisplayName("rejects dot-dot traversal escaping the log root")
    void getFile_dotDotTraversal_rejected() {
        assertThatThrownBy(() -> dataSource.getFile("/../../etc/passwd"))
                .isInstanceOf(FileSystemException.class);
    }

    @Test
    @DisplayName("rejects an absolute path escaping the log root")
    void getFile_parentEscape_rejected() {
        assertThatThrownBy(() -> dataSource.getFile("/../secret.txt"))
                .isInstanceOf(FileSystemException.class);
    }

    @Test
    @DisplayName("rejects a symlink inside the log root that points outside it")
    void getFile_symlinkEscape_rejected() throws IOException {
        Path secret = catalinaBase.resolve("secret.txt");
        Files.write(secret, "top secret\n".getBytes(StandardCharsets.UTF_8));
        Path link = logsDir.resolve("escape.log");
        try {
            Files.createSymbolicLink(link, secret);
        } catch (IOException | UnsupportedOperationException e) {
            assumeTrue(false, "symlinks not supported on this platform");
        }

        assertThatThrownBy(() -> dataSource.getFile("/escape.log"))
                .isInstanceOf(FileSystemException.class);
    }

    @Test
    @DisplayName("itemExists returns false for null or blank path")
    void itemExists_nullOrBlank_returnsFalse() {
        assertThat(dataSource.itemExists(null)).isFalse();
        assertThat(dataSource.itemExists("   ")).isFalse();
    }

    @Test
    @DisplayName("itemExists returns true for an existing log file")
    void itemExists_existingFile_returnsTrue() {
        assertThat(dataSource.itemExists("/jahia.log")).isTrue();
    }
}
