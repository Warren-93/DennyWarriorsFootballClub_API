package com.clubsite.api.common;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TextTest {

    @Test
    void slugifiesHeadlines() {
        assertThat(Text.slugify("Cup Final Report: 3–1 Win!")).isEqualTo("cup-final-report-3-1-win");
        assertThat(Text.slugify("  Café Société  ")).isEqualTo("cafe-societe");
        assertThat(Text.slugify("!!!")).isEmpty();
    }

    @Test
    void sameTextIgnoresCaseAndSurroundingSpace() {
        assertThat(Text.sameText(" Example FC", "example fc ")).isTrue();
        assertThat(Text.sameText(null, null)).isTrue();
        assertThat(Text.sameText("Example FC", null)).isFalse();
    }
}
