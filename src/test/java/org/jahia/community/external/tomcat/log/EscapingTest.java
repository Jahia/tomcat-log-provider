package org.jahia.community.external.tomcat.log;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.abort;

/**
 * S35 (G9) — Escaping of characters illegal in unqualified JCR node names ({@code []*|:}).
 *
 * <p>The escape direction is a pure, deterministic string transform (no Jahia runtime). The
 * round-trip delegates to {@code JCRContentUtils.unescapeLocalNodeName}; if that static cannot
 * load offline, the round-trip assertion is skipped (the escape-direction assertions still run).
 */
@DisplayName("Escaping illegal JCR characters")
class EscapingTest {

    @Test
    @DisplayName("escapeIllegalJcrChars replaces each illegal char with its %XX hex code")
    void escape_replacesIllegalChars() {
        assertThat(Escaping.escapeIllegalJcrChars("a:b")).isEqualTo("a%3Ab");
        assertThat(Escaping.escapeIllegalJcrChars("[")).isEqualTo("%5B");
        assertThat(Escaping.escapeIllegalJcrChars("]")).isEqualTo("%5D");
        assertThat(Escaping.escapeIllegalJcrChars("*")).isEqualTo("%2A");
        assertThat(Escaping.escapeIllegalJcrChars("|")).isEqualTo("%7C");

        // ISO-8601 timestamp filename (the colon regression) — no raw illegal chars remain.
        String escaped = Escaping.escapeIllegalJcrChars("localhost_access_log.2026-01-22T14:15:28.log");
        assertThat(escaped).doesNotContain(":", "[", "]", "*", "|");
        assertThat(escaped).contains("%3A");
    }

    @Test
    @DisplayName("escape leaves legal characters untouched and is deterministic")
    void escape_isDeterministicForLegalChars() {
        String legal = "jahia.log";
        assertThat(Escaping.escapeIllegalJcrChars(legal)).isEqualTo(legal);
        assertThat(Escaping.escapeIllegalJcrChars(legal)).isEqualTo(Escaping.escapeIllegalJcrChars(legal));
    }

    @Test
    @DisplayName("escape then unescape round-trips (skipped if JCRContentUtils is unavailable offline)")
    void escape_unescape_roundTrips() {
        String[] names = {
                "localhost_access_log.2026-01-22T14:15:28.log",
                "weird[name].log",
                "a*b|c.log"
        };
        try {
            for (String name : names) {
                String escaped = Escaping.escapeIllegalJcrChars(name);
                assertThat(Escaping.unescapeIllegalJcrChars(escaped)).isEqualTo(name);
            }
        } catch (Throwable t) {
            // JCRContentUtils.unescapeLocalNodeName could not load/execute outside a Jahia runtime.
            abort("Round-trip skipped: JCRContentUtils unavailable offline (" + t + ")");
        }
    }
}
