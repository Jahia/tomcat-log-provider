package org.jahia.community.external.tomcat.log;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * S37 (G11) — the shipped default OSGi config must carry the Jahia extender "won't be overridden"
 * sentinel on line 1 (per project convention) so redeploy does not clobber admin edits, and must
 * ship the default mountPath.
 */
@DisplayName("Shipped default .cfg sentinel")
class DefaultConfigResourceTest {

    private static final String RESOURCE = "/META-INF/configurations/org.jahia.modules.tomcatlogprovider.cfg";

    @Test
    @DisplayName("first line is the sentinel and the file declares the default mountPath")
    void defaultCfg_hasSentinelAndMountPath() throws IOException {
        try (InputStream in = getClass().getResourceAsStream(RESOURCE)) {
            assertThat(in).as("default cfg resource must be present").isNotNull();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
                List<String> lines = reader.lines().collect(Collectors.toList());

                assertThat(lines).isNotEmpty();
                assertThat(lines.get(0)).isEqualTo("# default configuration - won't be overridden");
                assertThat(lines).anyMatch(l -> l.trim().equals("mountPath=/sites/systemsite/files/tomcat-logs"));
            }
        }
    }
}
