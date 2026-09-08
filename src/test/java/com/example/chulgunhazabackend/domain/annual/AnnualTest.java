package com.example.chulgunhazabackend.domain.annual;

import com.example.chulgunhazabackend.exception.annualException.AnnualException;
import com.example.chulgunhazabackend.exception.annualException.AnnualExceptionType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

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

    @ParameterizedTest(name = "잔여 {0}일에서 {1}일 사용 -> 잔여 {2}일, 사용 {3}일")
    @CsvSource({
            "15.0, 1.0, 14.0, 1.0",
            "15.0, 0.5, 14.5, 0.5",
            "15.0, 15.0, 0.0, 15.0",
            "1.0, 0.5, 0.5, 0.5",
            "0.5, 0.5, 0.0, 0.5",
            "10.0, 0.0, 10.0, 0.0",
    })
    @DisplayName("다양한 잔여/사용 조합에서 use()가 정확히 차감한다")
    void use_다양한_조합에서_정확히_차감된다(double remaining, double useDays, double expectedRemaining, double expectedUse) {
        Annual annual = Annual.builder()
                .totalAnnualCount(remaining + 0.0) // totalAnnualCount 자체는 이 테스트에서 의미 없음
                .useCount(0.0)
                .remainingAnnualCount(remaining)
                .sickAnnualCount(0.0)
                .build();

        Annual used = annual.use(useDays);

        assertThat(used.getRemainingAnnualCount()).isEqualTo(expectedRemaining);
        assertThat(used.getUseCount()).isEqualTo(expectedUse);
    }

    @ParameterizedTest(name = "잔여 {0}일보다 {1}일 더 많이 쓰려 하면 예외")
    @CsvSource({
            "0.0, 0.5",
            "0.5, 1.0",
            "5.0, 5.5",
            "14.5, 15.0",
    })
    @DisplayName("잔여 연차를 초과해서 쓰려고 하면 항상 예외가 난다 (경계값 포함)")
    void use_잔여를_초과하면_항상_예외(double remaining, double useDays) {
        Annual annual = Annual.builder()
                .totalAnnualCount(15.0)
                .useCount(15.0 - remaining)
                .remainingAnnualCount(remaining)
                .sickAnnualCount(0.0)
                .build();

        assertThatThrownBy(() -> annual.use(useDays))
                .isInstanceOf(AnnualException.class)
                .satisfies(ex -> assertThat(((AnnualException) ex).getAnnualExceptionType())
                        .isEqualTo(AnnualExceptionType.INSUFFICIENT_BALANCE));
    }

    @Test
    @DisplayName("sickAnnualCount는 use()로 영향받지 않는다")
    void use_해도_병가_연차는_그대로다() {
        Annual annual = Annual.builder()
                .totalAnnualCount(15.0)
                .useCount(0.0)
                .remainingAnnualCount(15.0)
                .sickAnnualCount(3.0)
                .build();

        Annual used = annual.use(2.0);

        assertThat(used.getSickAnnualCount()).isEqualTo(3.0);
    }

    // --- refund() : 관리자가 이미 사용 처리된 연차를 반려했을 때 되돌리는 로직 (#79/Epic 4) ---

    @Test
    @DisplayName("refund()는 사용한 만큼 잔여 연차를 되돌리고 사용 일수를 줄인다")
    void refund_정상적으로_되돌린다() {
        Annual annual = Annual.builder()
                .totalAnnualCount(15.0)
                .useCount(3.0)
                .remainingAnnualCount(12.0)
                .sickAnnualCount(0.0)
                .build();

        Annual refunded = annual.refund(1.0);

        assertThat(refunded.getUseCount()).isEqualTo(2.0);
        assertThat(refunded.getRemainingAnnualCount()).isEqualTo(13.0);
        assertThat(refunded.getTotalAnnualCount()).isEqualTo(15.0); // 총 연차는 불변
    }

    @Test
    @DisplayName("use() 후 refund()로 같은 일수를 되돌리면 원래 상태로 정확히 복원된다 (왕복 검증)")
    void use_후_refund_왕복하면_원상복구된다() {
        Annual original = Annual.builder()
                .totalAnnualCount(15.0)
                .useCount(4.0)
                .remainingAnnualCount(11.0)
                .sickAnnualCount(1.0)
                .build();

        Annual roundTripped = original.use(2.0).refund(2.0);

        assertThat(roundTripped.getUseCount()).isEqualTo(original.getUseCount());
        assertThat(roundTripped.getRemainingAnnualCount()).isEqualTo(original.getRemainingAnnualCount());
        assertThat(roundTripped.getTotalAnnualCount()).isEqualTo(original.getTotalAnnualCount());
        assertThat(roundTripped.getSickAnnualCount()).isEqualTo(original.getSickAnnualCount());
    }

    @Test
    @DisplayName("refund()가 useCount보다 큰 값을 되돌려도 useCount는 음수가 되지 않고 0에서 멈춘다")
    void refund_사용량보다_많이_돌려줘도_음수가_안된다() {
        Annual annual = Annual.builder()
                .totalAnnualCount(15.0)
                .useCount(0.5)
                .remainingAnnualCount(14.5)
                .sickAnnualCount(0.0)
                .build();

        Annual refunded = annual.refund(2.0);

        assertThat(refunded.getUseCount()).isEqualTo(0.0);
        assertThat(refunded.getRemainingAnnualCount()).isEqualTo(16.5);
    }

    @ParameterizedTest
    @ValueSource(doubles = {0.0, 0.5, 1.0, 3.5})
    @DisplayName("refund(0)을 포함해 다양한 값으로 되돌려도 remainingAnnualCount는 정확히 그만큼 늘어난다")
    void refund_다양한_값으로_되돌리면_잔여가_정확히_늘어난다(double refundDays) {
        Annual annual = Annual.builder()
                .totalAnnualCount(15.0)
                .useCount(5.0)
                .remainingAnnualCount(10.0)
                .sickAnnualCount(0.0)
                .build();

        Annual refunded = annual.refund(refundDays);

        assertThat(refunded.getRemainingAnnualCount()).isEqualTo(10.0 + refundDays);
    }

    @Test
    @DisplayName("refund()는 예외를 던지지 않는다 (use()와 달리 상한 검증이 없음)")
    void refund_예외를_던지지_않는다() {
        Annual annual = Annual.builder()
                .totalAnnualCount(15.0)
                .useCount(0.0)
                .remainingAnnualCount(15.0)
                .sickAnnualCount(0.0)
                .build();

        assertThat(annual.refund(100.0).getRemainingAnnualCount()).isEqualTo(115.0);
    }

    @Test
    @DisplayName("use()는 원본 객체를 변경하지 않고 새 인스턴스를 반환한다 (불변성)")
    void use는_원본을_변경하지_않는다() {
        Annual original = Annual.builder()
                .totalAnnualCount(15.0)
                .useCount(0.0)
                .remainingAnnualCount(15.0)
                .sickAnnualCount(0.0)
                .build();

        Annual used = original.use(1.0);

        assertThat(original.getRemainingAnnualCount()).isEqualTo(15.0);
        assertThat(used).isNotSameAs(original);
    }

    @Test
    @DisplayName("refund()는 원본 객체를 변경하지 않고 새 인스턴스를 반환한다 (불변성)")
    void refund는_원본을_변경하지_않는다() {
        Annual original = Annual.builder()
                .totalAnnualCount(15.0)
                .useCount(3.0)
                .remainingAnnualCount(12.0)
                .sickAnnualCount(0.0)
                .build();

        Annual refunded = original.refund(1.0);

        assertThat(original.getUseCount()).isEqualTo(3.0);
        assertThat(refunded).isNotSameAs(original);
    }
}
