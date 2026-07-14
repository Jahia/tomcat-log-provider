package org.jahia.community.external.tomcat.log;

import org.jahia.modules.external.ExternalContentStoreProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.osgi.service.cm.ConfigurationException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * S25-S28 + S29 (G7) — mount lifecycle (updated/remount/@Deactivate).
 *
 * <p>Uses the behaviour-preserving {@code createProvider()} test seam (see
 * {@link TomcatLogMountPointService#createProvider()}) rather than {@code mockConstruction} —
 * the lower-risk choice: the subclass returns Mockito mocks, so no live Jahia repo is needed and
 * the private-ness of {@code @Deactivate stopProvider()} is avoided (it is package-private).
 */
@DisplayName("TomcatLogMountPointService lifecycle")
class TomcatLogMountPointServiceTest {

    private static final String CATALINA_BASE = "catalina.base";
    private static final String PATH_A = "/sites/systemsite/files/tomcat-logs";
    private static final String PATH_B = "/sites/systemsite/files/other-logs";

    @TempDir
    Path catalinaBase;

    private String previousCatalinaBase;
    private TestableService service;

    /** Records each provider handed to remount() so we can verify per-instance lifecycle calls. */
    private static final class TestableService extends TomcatLogMountPointService {
        final List<ExternalContentStoreProvider> created = new ArrayList<>();

        @Override
        protected ExternalContentStoreProvider createProvider() {
            ExternalContentStoreProvider provider = mock(ExternalContentStoreProvider.class);
            created.add(provider);
            return provider;
        }
    }

    @BeforeEach
    void setUp() throws IOException {
        previousCatalinaBase = System.getProperty(CATALINA_BASE);
        System.setProperty(CATALINA_BASE, catalinaBase.toString());
        Files.createDirectories(catalinaBase.resolve("logs"));
        service = new TestableService();
    }

    @AfterEach
    void tearDown() {
        if (previousCatalinaBase == null) {
            System.clearProperty(CATALINA_BASE);
        } else {
            System.setProperty(CATALINA_BASE, previousCatalinaBase);
        }
    }

    private static Dictionary<String, Object> props(String mountPath) {
        Dictionary<String, Object> d = new Hashtable<>();
        if (mountPath != null) {
            d.put("mountPath", mountPath);
        }
        return d;
    }

    @Test
    @DisplayName("S25: updated() with a valid mountPath mounts a provider at that JCR path")
    void updated_validMountPath_mounts() throws Exception {
        service.updated(props(PATH_A));

        assertThat(service.created).hasSize(1);
        assertThat(service.getMountPath()).isEqualTo(PATH_A);
        ExternalContentStoreProvider provider = service.created.get(0);
        verify(provider).setMountPoint(PATH_A);
        verify(provider).setDynamicallyMounted(false);
        verify(provider).start();
    }

    @Test
    @DisplayName("S26: null/empty/absent mountPath defaults and still remounts")
    void updated_nullOrEmpty_defaultsAndRemounts() throws ConfigurationException {
        service.updated(null);
        assertThat(service.getMountPath()).isEqualTo(TomcatLogMountPointService.DEFAULT_MOUNT_PATH);

        service.updated(props(""));
        assertThat(service.getMountPath()).isEqualTo(TomcatLogMountPointService.DEFAULT_MOUNT_PATH);

        service.updated(props(null)); // empty dictionary — no mountPath key
        assertThat(service.getMountPath()).isEqualTo(TomcatLogMountPointService.DEFAULT_MOUNT_PATH);

        // Each updated() remounts (the empty guard defaults, it does NOT skip the mount).
        assertThat(service.created).hasSize(3);
        service.created.forEach(p -> verify(p).setMountPoint(TomcatLogMountPointService.DEFAULT_MOUNT_PATH));
    }

    @Test
    @DisplayName("S27: a second updated() stops the previous provider before mounting the new one")
    void updated_twice_replacesProvider() throws Exception {
        service.updated(props(PATH_A));
        service.updated(props(PATH_B));

        assertThat(service.created).hasSize(2);
        ExternalContentStoreProvider first = service.created.get(0);
        ExternalContentStoreProvider second = service.created.get(1);
        verify(first).stop();
        verify(second).setMountPoint(PATH_B);
        verify(second).start();
        assertThat(service.getMountPath()).isEqualTo(PATH_B);
    }

    @Test
    @DisplayName("S28: @Deactivate stopProvider() stops once and is idempotent")
    void stopProvider_stopsOnceAndIsIdempotent() throws ConfigurationException {
        service.updated(props(PATH_A));
        ExternalContentStoreProvider provider = service.created.get(0);

        service.stopProvider();
        verify(provider).stop();

        // Calling again must not NPE (provider was nulled out).
        assertThatCode(() -> service.stopProvider()).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("S29: mountPath is passed through verbatim with no server-side validation (characterization)")
    void updated_acceptsMountPathVerbatim() throws ConfigurationException {
        // CHARACTERIZATION, not a bug — Stage-7 hardening candidate (server-side JCR path guard).
        service.updated(props("///../../weird"));

        assertThat(service.getMountPath()).isEqualTo("///../../weird");
        verify(service.created.get(0)).setMountPoint("///../../weird");
    }
}
