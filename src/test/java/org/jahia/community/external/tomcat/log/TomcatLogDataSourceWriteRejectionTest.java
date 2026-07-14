package org.jahia.community.external.tomcat.log;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * S17-S19 (G2) — read-only lock. All write operations hard-throw {@link UnsupportedOperationException}
 * immediately, before touching any state, so no setRoot()/Mockito is required.
 */
@DisplayName("TomcatLogDataSource write rejection (read-only)")
class TomcatLogDataSourceWriteRejectionTest {

    private final TomcatLogDataSource dataSource = new TomcatLogDataSource();

    @Test
    @DisplayName("S17: saveItem throws UnsupportedOperationException")
    void saveItem_throwsUnsupported() {
        assertThatThrownBy(() -> dataSource.saveItem(null))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    @DisplayName("S18: move throws UnsupportedOperationException")
    void move_throwsUnsupported() {
        assertThatThrownBy(() -> dataSource.move("/a.log", "/b.log"))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    @DisplayName("S19: removeItemByPath throws UnsupportedOperationException")
    void removeItemByPath_throwsUnsupported() {
        assertThatThrownBy(() -> dataSource.removeItemByPath("/jahia.log"))
                .isInstanceOf(UnsupportedOperationException.class);
    }
}
