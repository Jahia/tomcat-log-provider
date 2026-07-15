package org.jahia.community.external.tomcat.log;

import org.apache.commons.vfs2.FileContent;
import org.apache.commons.vfs2.VFS;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * S31-S34 (G8) — TomcatLogBinaryImpl streaming over a real VFS2 {@link FileContent}
 * (no mocks needed). Constructor is {@code (FileContent)}; equals is reference-identity on the
 * underlying FileContent.
 */
@DisplayName("TomcatLogBinaryImpl streaming")
class TomcatLogBinaryImplTest {

    private static final byte[] CONTENT = "0123456789".getBytes(StandardCharsets.UTF_8);

    @TempDir
    Path tempDir;

    private FileContent newContent() throws IOException {
        Path file = tempDir.resolve("data-" + System.nanoTime() + ".log");
        Files.write(file, CONTENT);
        return VFS.getManager().resolveFile(file.toUri().toString()).getContent();
    }

    @Test
    @DisplayName("S31: getSize returns the file byte length")
    void getSize_returnsLength() throws Exception {
        TomcatLogBinaryImpl binary = new TomcatLogBinaryImpl(newContent());

        assertThat(binary.getSize()).isEqualTo(CONTENT.length);
    }

    @Test
    @DisplayName("S32: getStream returns the full file bytes")
    void getStream_returnsFullBytes() throws Exception {
        TomcatLogBinaryImpl binary = new TomcatLogBinaryImpl(newContent());

        try (InputStream is = binary.getStream()) {
            assertThat(is.readAllBytes()).isEqualTo(CONTENT);
        }
    }

    @Test
    @DisplayName("S33: read honours offset, EOF, negative position and null buffer")
    void read_honoursOffsetEofAndErrors() throws Exception {
        TomcatLogBinaryImpl binary = new TomcatLogBinaryImpl(newContent());

        byte[] head = new byte[4];
        assertThat(binary.read(head, 0)).isEqualTo(4);
        assertThat(head).isEqualTo("0123".getBytes(StandardCharsets.UTF_8));

        byte[] tail = new byte[4];
        assertThat(binary.read(tail, 6)).isEqualTo(4);
        assertThat(tail).isEqualTo("6789".getBytes(StandardCharsets.UTF_8));

        // At/after EOF → -1
        assertThat(binary.read(new byte[4], CONTENT.length)).isEqualTo(-1);
        assertThat(binary.read(new byte[4], CONTENT.length + 10)).isEqualTo(-1);

        // Negative position → IOException; null buffer → NPE
        assertThatThrownBy(() -> binary.read(new byte[4], -1)).isInstanceOf(IOException.class);
        assertThatThrownBy(() -> binary.read(null, 0)).isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("S34: equals is reference-identity on FileContent; dispose is safe")
    void equals_referenceIdentity_andDispose() throws Exception {
        FileContent shared = newContent();
        TomcatLogBinaryImpl a = new TomcatLogBinaryImpl(shared);
        TomcatLogBinaryImpl b = new TomcatLogBinaryImpl(shared);
        TomcatLogBinaryImpl other = new TomcatLogBinaryImpl(newContent());

        // Same FileContent instance → equal; different instance (even same file) → not equal.
        assertThat(a).isEqualTo(b);
        assertThat(a.hashCode()).isEqualTo(shared.hashCode());
        assertThat(a).isNotEqualTo(other);
        assertThat(a).isNotEqualTo(null);
        assertThat(a).isEqualTo(a);

        assertThatCode(other::dispose).doesNotThrowAnyException();
    }
}
