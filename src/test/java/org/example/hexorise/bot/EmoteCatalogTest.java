package org.example.hexorise.bot;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;
import static org.assertj.core.api.Assertions.*;
class EmoteCatalogTest {
    @Test void resolvesStableNumbersNamesAndAdditionalDirectIdsAndPaginates() throws Exception {
        var catalog = new EmoteCatalog(new ObjectMapper());
        assertThat(catalog.resolve("1").id()).isEqualTo("dance-macarena");
        assertThat(catalog.resolve("HELLO").number()).isEqualTo(2);
        assertThat(catalog.resolve("dance-new-api-emote").id()).isEqualTo("dance-new-api-emote");
        assertThat(catalog.resolve("idle_singing").id()).isEqualTo("idle_singing");
        assertThatThrownBy(() -> catalog.resolve("missing-name")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> catalog.page(0)).isInstanceOf(IllegalArgumentException.class);
        assertThat(catalog.list()).extracting(EmoteCatalog.Emote::id).doesNotHaveDuplicates();
        for (int page = 1; page <= (catalog.list().size() + 4) / 5; page++) assertThat(catalog.page(page).length()).isLessThanOrEqualTo(255);
        assertThat(catalog.list().size()).isGreaterThan(50);
    }
}
