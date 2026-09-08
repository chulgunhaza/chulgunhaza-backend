package com.example.chulgunhazabackend.service.impl;

import com.example.chulgunhazabackend.client.AttendanceStatsClient;
import com.example.chulgunhazabackend.dto.dashboard.DashboardStatsResponseDto;
import com.example.chulgunhazabackend.repository.EmployeeRepository;
import com.example.chulgunhazabackend.repository.PostRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

/**
 * Epic 6 — 대시보드 통계 집계의 서비스 계층 단위 테스트.
 * #100: attendance-server 물리 분리로 오늘 출근 수는 더 이상 리포지토리 직접
 * 조회가 아니라 AttendanceStatsClient(내부 API 호출) 값이다 — 그 클라이언트를
 * 모킹한다. 당일 00:00~내일 00:00 날짜 범위 계산 로직 자체는 attendance-server의
 * InternalAttendanceController로 옮겨갔다.
 */
@ExtendWith(MockitoExtension.class)
class DashboardStatsServiceImplTest {

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private PostRepository postRepository;

    @Mock
    private AttendanceStatsClient attendanceStatsClient;

    @InjectMocks
    private DashboardStatsServiceImpl dashboardStatsService;

    @Test
    @DisplayName("모든 카운트가 0이면 통계도 전부 0/빈 맵으로 반환된다")
    void getStats_데이터가_없으면_전부_0이다() {
        given(employeeRepository.countByDelFlagFalse()).willReturn(0L);
        given(employeeRepository.countActiveEmployeesByDepartment()).willReturn(List.of());
        given(attendanceStatsClient.getTodayCheckInCount()).willReturn(0L);
        given(postRepository.countByDelFlagFalse()).willReturn(0L);

        DashboardStatsResponseDto stats = dashboardStatsService.getStats();

        assertThat(stats.getTotalEmployees()).isEqualTo(0);
        assertThat(stats.getDepartmentCounts()).isEmpty();
        assertThat(stats.getTodayAttendanceCount()).isEqualTo(0);
        assertThat(stats.getTotalPosts()).isEqualTo(0);
    }

    @Test
    @DisplayName("사원 수/오늘 출근 수/게시글 수가 각 소스 값 그대로 반영된다")
    void getStats_기본_카운트가_정확히_반영된다() {
        given(employeeRepository.countByDelFlagFalse()).willReturn(42L);
        given(employeeRepository.countActiveEmployeesByDepartment()).willReturn(List.of());
        given(attendanceStatsClient.getTodayCheckInCount()).willReturn(17L);
        given(postRepository.countByDelFlagFalse()).willReturn(8L);

        DashboardStatsResponseDto stats = dashboardStatsService.getStats();

        assertThat(stats.getTotalEmployees()).isEqualTo(42);
        assertThat(stats.getTodayAttendanceCount()).isEqualTo(17);
        assertThat(stats.getTotalPosts()).isEqualTo(8);
    }

    @Test
    @DisplayName("부서별 사원 수가 Object[] 배열에서 Map<String,Long>으로 정확히 변환된다")
    void getStats_부서별_카운트가_정확히_매핑된다() {
        given(employeeRepository.countByDelFlagFalse()).willReturn(5L);
        given(employeeRepository.countActiveEmployeesByDepartment()).willReturn(List.of(
                new Object[]{"개발팀", 3L},
                new Object[]{"인사팀", 2L}
        ));
        given(attendanceStatsClient.getTodayCheckInCount()).willReturn(0L);
        given(postRepository.countByDelFlagFalse()).willReturn(0L);

        DashboardStatsResponseDto stats = dashboardStatsService.getStats();

        assertThat(stats.getDepartmentCounts())
                .containsEntry("개발팀", 3L)
                .containsEntry("인사팀", 2L)
                .hasSize(2);
    }

    @Test
    @DisplayName("부서별 카운트는 리포지토리가 반환한 순서를 그대로 유지한다 (LinkedHashMap)")
    void getStats_부서별_카운트는_순서를_유지한다() {
        given(employeeRepository.countByDelFlagFalse()).willReturn(6L);
        given(employeeRepository.countActiveEmployeesByDepartment()).willReturn(List.of(
                new Object[]{"인사팀", 1L},
                new Object[]{"개발팀", 3L},
                new Object[]{"영업팀", 2L}
        ));
        given(attendanceStatsClient.getTodayCheckInCount()).willReturn(0L);
        given(postRepository.countByDelFlagFalse()).willReturn(0L);

        DashboardStatsResponseDto stats = dashboardStatsService.getStats();

        assertThat(stats.getDepartmentCounts().keySet())
                .containsExactly("인사팀", "개발팀", "영업팀");
    }

    @Test
    @DisplayName("오늘 출근 수는 AttendanceStatsClient가 준 값을 그대로 사용한다")
    void getStats_오늘_출근수는_클라이언트_값을_그대로_사용한다() {
        given(employeeRepository.countByDelFlagFalse()).willReturn(0L);
        given(employeeRepository.countActiveEmployeesByDepartment()).willReturn(List.of());
        given(attendanceStatsClient.getTodayCheckInCount()).willReturn(9L);
        given(postRepository.countByDelFlagFalse()).willReturn(0L);

        DashboardStatsResponseDto stats = dashboardStatsService.getStats();

        assertThat(stats.getTodayAttendanceCount()).isEqualTo(9);
    }

    @Test
    @DisplayName("Map은 Map.of가 아니라 수정 가능한 LinkedHashMap 계열로 반환된다 (동일 부서 키 중복 시 첫 값 유지)")
    void getStats_같은_부서_키가_중복되면_첫값을_유지한다() {
        given(employeeRepository.countByDelFlagFalse()).willReturn(0L);
        // 실제로는 GROUP BY 쿼리라 중복이 나올 수 없지만, 병합 함수(a,b)->a 분기 자체를 검증한다.
        given(employeeRepository.countActiveEmployeesByDepartment()).willReturn(List.of(
                new Object[]{"개발팀", 1L},
                new Object[]{"개발팀", 99L}
        ));
        given(attendanceStatsClient.getTodayCheckInCount()).willReturn(0L);
        given(postRepository.countByDelFlagFalse()).willReturn(0L);

        DashboardStatsResponseDto stats = dashboardStatsService.getStats();

        assertThat(stats.getDepartmentCounts().get("개발팀")).isEqualTo(1L);
    }
}
