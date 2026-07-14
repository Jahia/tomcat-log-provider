package org.jahia.community.external.tomcat.log;

import org.jahia.api.Constants;
import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.content.JCRSessionFactory;
import org.jahia.services.content.JCRSessionWrapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

/**
 * S30 (G5) — JCR privilege gating, defense-in-depth twin of the Cypress permission spec.
 *
 * <p>Real signature is {@code getPrivilegesNames(String username, String path)}. The session is
 * NOT a parameter — it is obtained internally via the static singleton
 * {@code JCRSessionFactory.getInstance().getCurrentUserSession(EDIT_WORKSPACE)}, so we mock it
 * with {@link MockedStatic} (Mockito 5 default inline mock-maker) rather than an instance mock.
 */
@DisplayName("TomcatLogDataSource JCR privilege gating")
class TomcatLogDataSourcePrivilegesTest {

    private final TomcatLogDataSource dataSource = new TomcatLogDataSource();

    @Test
    @DisplayName("S30a: grants read_live when the user has tomcatLogProviderAdmin on /")
    void getPrivilegesNames_withPermission_grantsRead() throws Exception {
        JCRNodeWrapper root = mock(JCRNodeWrapper.class);
        when(root.hasPermission("tomcatLogProviderAdmin")).thenReturn(true);
        JCRSessionWrapper session = mock(JCRSessionWrapper.class);
        when(session.getNode("/")).thenReturn(root);
        JCRSessionFactory factory = mock(JCRSessionFactory.class);
        when(factory.getCurrentUserSession(Constants.EDIT_WORKSPACE)).thenReturn(session);

        try (MockedStatic<JCRSessionFactory> mocked = mockStatic(JCRSessionFactory.class)) {
            mocked.when(JCRSessionFactory::getInstance).thenReturn(factory);

            String[] privileges = dataSource.getPrivilegesNames("alice", "/jahia.log");

            assertThat(privileges)
                    .containsExactly(Constants.JCR_READ_RIGHTS + "_" + Constants.EDIT_WORKSPACE);
        }
    }

    @Test
    @DisplayName("S30b: grants nothing when the user lacks tomcatLogProviderAdmin")
    void getPrivilegesNames_withoutPermission_grantsNothing() throws Exception {
        JCRNodeWrapper root = mock(JCRNodeWrapper.class);
        when(root.hasPermission("tomcatLogProviderAdmin")).thenReturn(false);
        JCRSessionWrapper session = mock(JCRSessionWrapper.class);
        when(session.getNode("/")).thenReturn(root);
        JCRSessionFactory factory = mock(JCRSessionFactory.class);
        when(factory.getCurrentUserSession(Constants.EDIT_WORKSPACE)).thenReturn(session);

        try (MockedStatic<JCRSessionFactory> mocked = mockStatic(JCRSessionFactory.class)) {
            mocked.when(JCRSessionFactory::getInstance).thenReturn(factory);

            String[] privileges = dataSource.getPrivilegesNames("bob", "/jahia.log");

            assertThat(privileges).isEmpty();
        }
    }
}
