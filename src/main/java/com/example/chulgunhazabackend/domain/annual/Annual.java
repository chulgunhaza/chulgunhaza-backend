package com.example.chulgunhazabackend.domain.annual;

import com.example.chulgunhazabackend.exception.annualException.AnnualException;
import com.example.chulgunhazabackend.exception.annualException.AnnualExceptionType;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.*;
import org.hibernate.annotations.ColumnDefault;

import java.time.LocalDate;

@Embeddable
@Getter
@ToString
public class Annual {

    private double totalAnnualCount;

    private double useCount;

    private double remainingAnnualCount;

    private double sickAnnualCount;

    public Annual() {
        this.totalAnnualCount = 15.0;
        this.useCount = 0.0;
        this.remainingAnnualCount = 15.0;
        this.sickAnnualCount = 0.0;
    }

    @Builder
    public Annual(double totalAnnualCount, double useCount,
                  double remainingAnnualCount, double sickAnnualCount) {
        this.totalAnnualCount = totalAnnualCount;
        this.useCount = useCount;
        this.remainingAnnualCount = remainingAnnualCount;
        this.sickAnnualCount = sickAnnualCount;
    }

    // 연차가 수정되었을 때 사용하는 생성자

    /**
     * 연차를 {@code days} 만큼 사용 처리한 새 {@link Annual} 값을 반환한다.
     * 잔여 연차보다 많이 쓰려고 하면 {@link AnnualException} 을 던지고 원본은 변경하지 않는다.
     * (#48 연차 사용 동시성 제어 — 실제 원자적 차감은 이 메서드를 호출하는 서비스 계층에서
     * 비관적 락으로 조회한 Employee 위에서 이뤄져야 race condition이 없다.)
     */
    public Annual use(double days) {
        if (this.remainingAnnualCount < days) {
            throw new AnnualException(AnnualExceptionType.INSUFFICIENT_BALANCE);
        }
        return Annual.builder()
                .totalAnnualCount(this.totalAnnualCount)
                .useCount(this.useCount + days)
                .remainingAnnualCount(this.remainingAnnualCount - days)
                .sickAnnualCount(this.sickAnnualCount)
                .build();
    }
}
