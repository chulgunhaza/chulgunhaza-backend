package com.example.chulgunhazabackend.domain.annual;

import com.example.chulgunhazabackend.exception.annualException.AnnualException;
import com.example.chulgunhazabackend.exception.annualException.AnnualExceptionType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * #48 연차 사용 동시성 제어의 순수 도메인 로직 단위 테스트.
 *
 * <p>{@link Annual} 은 원래 데이터만 들고 있는 값 객체였고, "연차를 쓴다"는 행위 자체가
 * 코드 어디에도 없었다. 이 테스트를 먼저 작성해서 {@link Annual#use(double)} 의 스펙
 * (정상 차감 / 경계값 / 잔여 부족 시 예외)을 못박은 뒤 구현했다.</p>
 */
class AnnualTest {

    @Test
    @DisplayName("잔여 연차에서 사용 일수만큼 정상 차감된다")
    void 연차를_정상적으로_사용한다() {
        Annual annual = Annual.builder()
                .totalAnnualCount(15.0)
                .useCount(0.0)
                .remainingAnnualCount(15.0)
                .sickAnnualCount(0.0)
                .build();

        Annual used = annual.use(1.0);

        assertThat(used.getUseCount()).isEqualTo(1.0);
        assertThat(used.getRemainingAnnualCount()).isEqualTo(14.0);
        assertThat(used.getTotalAnnualCount()).isEqualTo(15.0); // 총 연차는 불변
    }

    @Test
    @DisplayName("반차(0.5일) 단위로도 차감된다")
    void 반차_단위로_사용한다() {
        Annual annual = Annual.builder()
                .totalAnnualCount(15.0)
                .useCount(0.0)
                .remainingAnnualCount(15.0)
                .sickAnnualCount(0.0)
                .build();

        Annual used = annual.use(0.5);

        assertThat(used.getRemainingAnnualCount()).isEqualTo(14.5);
    }

    @Test
    @DisplayName("남은 연차와 정확히 같은 만큼 사용하면 0이 된다 (경계값)")
    void 잔여_연차를_전부_사용하면_0이_된다() {
        Annual annual = Annual.builder()
                .totalAnnualCount(15.0)
                .useCount(14.0)
                .remainingAnnualCount(1.0)
                .sickAnnualCount(0.0)
                .build();

        Annual used = annual.use(1.0);

        assertThat(used.getRemainingAnnualCount()).isEqualTo(0.0);
    }

    @Test
    @DisplayName("잔여 연차보다 많이 쓰려고 하면 AnnualException(INSUFFICIENT_BALANCE)이 발생하고 원본은 변경되지 않는다")
    void 잔여_연차보다_많이_쓰면_예외가_발생한다() {
        Annual annual = Annual.builder()
                .totalAnnualCount(15.0)
                .useCount(14.5)
                .remainingAnnualCount(0.5)
                .sickAnnualCount(0.0)
                .build();

        assertThatThrownBy(() -> annual.use(1.0))
                .isInstanceOf(AnnualException.class)
                .satisfies(ex -> assertThat(((AnnualException) ex).getAnnualExceptionType())
                        .isEqualTo(AnnualExceptionType.INSUFFICIENT_BALANCE));

        // 예외가 나도 원본 remainingAnnualCount는 그대로여야 한다 (불변 객체)
        assertThat(annual.getRemainingAnnualCount()).isEqualTo(0.5);
    }
}
