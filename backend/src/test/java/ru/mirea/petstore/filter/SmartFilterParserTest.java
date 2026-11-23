package ru.mirea.petstore.filter;

import org.junit.jupiter.api.Test;
import ru.mirea.petstore.domain.Priority;
import ru.mirea.petstore.domain.ReservationStatus;
import ru.mirea.petstore.error.BadRequestException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SmartFilterParserTest {
    private final SmartFilterParser parser = new SmartFilterParser();

    @Test
    void parsesMixedSmartQuery() {
        ReservationFilter result = parser.parse("Барсик клиент:CL-001 задаток<5000 уход:да приоритет:высокий тег:привит");
        assertThat(result.freeText()).isEqualTo("Барсик");
        assertThat(result.client()).isEqualTo("CL-001");
        assertThat(result.status()).isNull();
        assertThat(result.needsCare()).isTrue();
        assertThat(result.priority()).isEqualTo(Priority.HIGH);
        assertThat(result.tag()).isEqualTo("привит");
        assertThat(result.archived()).isFalse();
        assertThat(result.deposits()).hasSize(1);
    }

    @Test
    void supportsQuotedFreeTextAndStatus() {
        ReservationFilter result = parser.parse("\"Рыжий кот\" status:confirmed archived:all");
        assertThat(result.freeText()).isEqualTo("Рыжий кот");
        assertThat(result.status()).isEqualTo(ReservationStatus.CONFIRMED);
        assertThat(result.archived()).isNull();
    }

    @Test
    void rejectsOutOfRangeDeposit() {
        assertThatThrownBy(() -> parser.parse("задаток>1000001"))
                .isInstanceOf(BadRequestException.class);
    }
}
