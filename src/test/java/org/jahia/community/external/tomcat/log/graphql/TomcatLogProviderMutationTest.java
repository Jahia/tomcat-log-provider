package org.jahia.community.external.tomcat.log.graphql;

import org.jahia.osgi.BundleUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;

import java.io.IOException;
import java.util.Dictionary;
import java.util.Hashtable;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * S20-S24 + S29 (G6) — saveSettings mutation branch coverage.
 *
 * <p>No injection seam exists: the mutation fetches ConfigurationAdmin via the static
 * {@code BundleUtils.getOsgiService(ConfigurationAdmin.class, null)}, so we mock it with
 * {@link MockedStatic} (Mockito 5 default inline mock-maker).
 */
@DisplayName("TomcatLogProviderMutation.saveSettings")
class TomcatLogProviderMutationTest {

    private static final String PID = "org.jahia.modules.tomcatlogprovider";

    private final TomcatLogProviderMutation mutation = new TomcatLogProviderMutation();

    @Test
    @DisplayName("S20: writes mountPath via ConfigurationAdmin and returns true")
    void saveSettings_writesConfig_returnsTrue() throws IOException {
        Configuration config = mock(Configuration.class);
        when(config.getProperties()).thenReturn(new Hashtable<>());
        ConfigurationAdmin ca = mock(ConfigurationAdmin.class);
        when(ca.getConfiguration(eq(PID), isNull())).thenReturn(config);

        try (MockedStatic<BundleUtils> bu = mockStatic(BundleUtils.class)) {
            bu.when(() -> BundleUtils.getOsgiService(ConfigurationAdmin.class, null)).thenReturn(ca);

            Boolean result = mutation.saveSettings("/sites/systemsite/files/tomcat-logs");

            assertThat(result).isTrue();
            ArgumentCaptor<Dictionary<String, Object>> captor = ArgumentCaptor.forClass(Dictionary.class);
            verify(config).update(captor.capture());
            assertThat(captor.getValue().get("mountPath")).isEqualTo("/sites/systemsite/files/tomcat-logs");
        }
    }

    @Test
    @DisplayName("S21: returns false (no throw) when ConfigurationAdmin is unavailable")
    void saveSettings_noConfigAdmin_returnsFalse() {
        try (MockedStatic<BundleUtils> bu = mockStatic(BundleUtils.class)) {
            bu.when(() -> BundleUtils.getOsgiService(ConfigurationAdmin.class, null)).thenReturn(null);

            assertThat(mutation.saveSettings("/x")).isFalse();
        }
    }

    @Test
    @DisplayName("S22: returns false and swallows when update() throws IOException")
    void saveSettings_updateThrows_returnsFalse() throws IOException {
        Configuration config = mock(Configuration.class);
        when(config.getProperties()).thenReturn(new Hashtable<>());
        doThrowIo(config);
        ConfigurationAdmin ca = mock(ConfigurationAdmin.class);
        when(ca.getConfiguration(eq(PID), isNull())).thenReturn(config);

        try (MockedStatic<BundleUtils> bu = mockStatic(BundleUtils.class)) {
            bu.when(() -> BundleUtils.getOsgiService(ConfigurationAdmin.class, null)).thenReturn(ca);

            assertThat(mutation.saveSettings("/x")).isFalse();
        }
    }

    private static void doThrowIo(Configuration config) throws IOException {
        org.mockito.Mockito.doThrow(new IOException("boom")).when(config).update(any());
    }

    @Test
    @DisplayName("S23 (corrected): null/empty mountPath still returns true and still calls update, skipping the put")
    void saveSettings_nullOrEmpty_stillUpdatesWithoutSettingMountPath() throws IOException {
        assertNullEmptyKeepsExistingMountPath(null);
        assertNullEmptyKeepsExistingMountPath("");
    }

    private void assertNullEmptyKeepsExistingMountPath(String input) throws IOException {
        Configuration config = mock(Configuration.class);
        Dictionary<String, Object> existing = new Hashtable<>();
        existing.put("mountPath", "/sites/systemsite/files/tomcat-logs");
        when(config.getProperties()).thenReturn(existing);
        ConfigurationAdmin ca = mock(ConfigurationAdmin.class);
        when(ca.getConfiguration(eq(PID), isNull())).thenReturn(config);

        try (MockedStatic<BundleUtils> bu = mockStatic(BundleUtils.class)) {
            bu.when(() -> BundleUtils.getOsgiService(ConfigurationAdmin.class, null)).thenReturn(ca);

            Boolean result = mutation.saveSettings(input);

            assertThat(result).as("null/empty is not rejected — it returns true").isTrue();
            ArgumentCaptor<Dictionary<String, Object>> captor = ArgumentCaptor.forClass(Dictionary.class);
            verify(config).update(captor.capture());
            // The put is skipped, so the pre-existing mountPath is unchanged (not cleared).
            assertThat(captor.getValue().get("mountPath")).isEqualTo("/sites/systemsite/files/tomcat-logs");
        }
    }

    @Test
    @DisplayName("S24: creates a fresh Hashtable when existing properties are null")
    void saveSettings_nullProperties_createsHashtable() throws IOException {
        Configuration config = mock(Configuration.class);
        when(config.getProperties()).thenReturn(null);
        ConfigurationAdmin ca = mock(ConfigurationAdmin.class);
        when(ca.getConfiguration(eq(PID), isNull())).thenReturn(config);

        try (MockedStatic<BundleUtils> bu = mockStatic(BundleUtils.class)) {
            bu.when(() -> BundleUtils.getOsgiService(ConfigurationAdmin.class, null)).thenReturn(ca);

            Boolean result = mutation.saveSettings("/sites/systemsite/files/tomcat-logs");

            assertThat(result).isTrue();
            ArgumentCaptor<Dictionary<String, Object>> captor = ArgumentCaptor.forClass(Dictionary.class);
            verify(config).update(captor.capture());
            assertThat(captor.getValue()).isNotNull();
            assertThat(captor.getValue().get("mountPath")).isEqualTo("/sites/systemsite/files/tomcat-logs");
        }
    }

    @Test
    @DisplayName("S29: mountPath is stored verbatim with no server-side format validation (characterization)")
    void saveSettings_acceptsMountPathVerbatim() throws IOException {
        // CHARACTERIZATION, not a bug: only null/empty is rejected. Stage-7 hardening candidate —
        // add a server-side JCR-path guard (mirror the client Browse regex) then flip this to assert rejection.
        Configuration config = mock(Configuration.class);
        when(config.getProperties()).thenReturn(new Hashtable<>());
        ConfigurationAdmin ca = mock(ConfigurationAdmin.class);
        when(ca.getConfiguration(eq(PID), isNull())).thenReturn(config);

        try (MockedStatic<BundleUtils> bu = mockStatic(BundleUtils.class)) {
            bu.when(() -> BundleUtils.getOsgiService(ConfigurationAdmin.class, null)).thenReturn(ca);

            assertThat(mutation.saveSettings("not-a-jcr-path")).isTrue();

            ArgumentCaptor<Dictionary<String, Object>> captor = ArgumentCaptor.forClass(Dictionary.class);
            verify(config).update(captor.capture());
            assertThat(captor.getValue().get("mountPath")).isEqualTo("not-a-jcr-path");
            verify(config, never()).delete();
        }
    }
}
