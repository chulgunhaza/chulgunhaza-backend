package com.example.chulgunhazabackend.domain.annual;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 관리자 백로그 Epic 4 — 연차 반려(reject)의 순수 도메인 로직 단위 테스트.
 */
class AnnualRecordTest {

    private AnnualRecord recordWithStatus(AnnualApprovalStatus status) {
        return AnnualRecord.builder()
                .employeeId(1L)
                .approvedId(1L)
                .annualDate(LocalDate.of(2026, 9, 1))
                .annualType(AnnualType.ANNUAL)
                .annualReason("개인 사유")
                .annualApprovalStatus(status)
                .build();
    }

    @Test
    @DisplayName("APPROVED 상태는 isRejected()가 false다")
    void APPROVED는_반려_상태가_아니다() {
        AnnualRecord record = recordWithStatus(AnnualApprovalStatus.APPROVED);

        assertThat(record.isRejected()).isFalse();
    }

    @Test
    @DisplayName("PENDING 상태는 isRejected()가 false다")
    void PENDING은_반려_상태가_아니다() {
        AnnualRecord record = recordWithStatus(AnnualApprovalStatus.PENDING);

        assertThat(record.isRejected()).isFalse();
    }

    @Test
    @DisplayName("REJECTED 상태는 isRejected()가 true다")
    void REJECTED는_반려_상태다() {
        AnnualRecord record = recordWithStatus(AnnualApprovalStatus.REJECTED);

        assertThat(record.isRejected()).isTrue();
    }

    @ParameterizedTest
    @EnumSource(AnnualApprovalStatus.class)
    @DisplayName("어떤 상태에서든 reject()를 호출하면 REJECTED로 바뀐다")
    void 어떤_상태든_reject_호출하면_REJECTED가_된다(AnnualApprovalStatus initialStatus) {
        AnnualRecord record = recordWithStatus(initialStatus);

        record.reject();

        assertThat(record.getAnnualApprovalStatus()).isEqualTo(AnnualApprovalStatus.REJECTED);
        assertThat(record.isRejected()).isTrue();
    }

    @Test
    @DisplayName("reject()를 두 번 호출해도 상태는 REJECTED로 유지된다 (멱등)")
    void reject를_두번_호출해도_REJECTED로_유지된다() {
        AnnualRecord record = recordWithStatus(AnnualApprovalStatus.APPROVED);

        record.reject();
        record.reject();

        assertThat(record.getAnnualApprovalStatus()).isEqualTo(AnnualApprovalStatus.REJECTED);
    }

    @Test
    @DisplayName("reject()는 employeeId/annualDate/annualType 등 다른 필드에 영향을 주지 않는다")
    void reject해도_다른_필드는_그대로다() {
        AnnualRecord record = recordWithStatus(AnnualApprovalStatus.APPROVED);

        record.reject();

        assertThat(record.getEmployeeId()).isEqualTo(1L);
        assertThat(record.getAnnualDate()).isEqualTo(LocalDate.of(2026, 9, 1));
        assertThat(record.getAnnualType()).isEqualTo(AnnualType.ANNUAL);
        assertThat(record.getAnnualReason()).isEqualTo("개인 사유");
    }
}
