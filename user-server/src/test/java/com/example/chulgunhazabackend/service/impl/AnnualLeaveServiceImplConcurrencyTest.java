package com.example.chulgunhazabackend.service.impl;

import com.example.chulgunhazabackend.domain.annual.Annual;
import com.example.chulgunhazabackend.domain.annual.AnnualType;
import com.example.chulgunhazabackend.domain.member.*;
import com.example.chulgunhazabackend.dto.annual.AnnualUsageRequestDto;
import com.example.chulgunhazabackend.exception.annualException.AnnualException;
import com.example.chulgunhazabackend.repository.AnnualRecordRepository;
import com.example.chulgunhazabackend.repository.EmployeeRepository;
import com.example.chulgunhazabackend.service.AnnualLeaveService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * #48 연차 사용 동시성 제어 — 실제 스레드로 같은 사원의 연차를 동시에 사용해서
 * {@link AnnualLeaveServiceImpl#useAnnualLeave} 가 lost update 없이 안전한지 검증한다.
 *
 * <p>이 테스트를 {@link EmployeeRepository#findEmployeeByIdForUpdate(Long)} 의
 * {@code @Lock(PESSIMISTIC_WRITE)} 를 걷어내고 돌려보면(=일반 findEmployeeById로
 * 바꾸면) 잔여 연차가 음수가 되거나, 성공한 요청 수와 실제 차감량이 어긋나는 걸 바로
 * 재현할 수 있다 — 그게 바로 지금까지 이 프로젝트에 없었던 동시성 버그다.</p>
 */
@SpringBootTest
class AnnualLeaveServiceImplConcurrencyTest {

    private static final int THREAD_COUNT = 8;
    private static final double INITIAL_REMAINING = 5.0; // 8명이 동시에 요청해도 5명만 성공해야 한다

    @Autowired
    private AnnualLeaveService annualLeaveService;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private AnnualRecordRepository annualRecordRepository;

    private Long testEmployeeId;

    @AfterEach
    void cleanUp() {
        if (testEmployeeId != null) {
            annualRecordRepository.findAll().stream()
                    .filter(record -> testEmployeeId.equals(record.getEmployeeId()))
                    .forEach(record -> annualRecordRepository.deleteById(record.getId()));
            employeeRepository.deleteById(testEmployeeId);
        }
    }

    @Test
    @DisplayName("같은 사원에게 잔여 연차보다 많은 동시 요청이 와도 잔여 연차는 음수가 되지 않고, 성공 건수만큼만 정확히 차감된다")
    void 동시_연차_사용_요청은_잔여_연차를_초과해서_차감되지_않는다() throws InterruptedException {
        // given: 잔여 연차 5일짜리 사원 1명 생성
        Employee employee = createTestEmployee(INITIAL_REMAINING);
        testEmployeeId = employeeRepository.save(employee).getId();

        ExecutorService executorService = Executors.newFixedThreadPool(THREAD_COUNT);
        CountDownLatch readyLatch = new CountDownLatch(THREAD_COUNT);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(THREAD_COUNT);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger insufficientBalanceCount = new AtomicInteger(0);

        // when: THREAD_COUNT명이 정확히 같은 순간에 연차 사용을 요청한다
        for (int i = 0; i < THREAD_COUNT; i++) {
            final int idx = i;
            executorService.submit(() -> {
                try {
                    readyLatch.countDown();
                    startLatch.await();
                    AnnualUsageRequestDto requestDto = new AnnualUsageRequestDto(
                            LocalDate.now().plusDays(idx), AnnualType.ANNUAL, "동시성 테스트");
                    annualLeaveService.useAnnualLeave(testEmployeeId, requestDto);
                    successCount.incrementAndGet();
                } catch (AnnualException e) {
                    insufficientBalanceCount.incrementAndGet();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (Exception e) {
                    System.err.println("[DEBUG] thread " + idx + " unexpected exception: " + e);
                    e.printStackTrace();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        readyLatch.await(5, TimeUnit.SECONDS);
        startLatch.countDown(); // 모든 스레드를 동시에 출발시킨다
        boolean finished = doneLatch.await(15, TimeUnit.SECONDS);
        executorService.shutdown();

        // then
        assertThat(finished).as("모든 스레드가 15초 안에 끝나야 한다 (락 대기로 무한정 걸리면 안 됨)").isTrue();
        assertThat(successCount.get() + insufficientBalanceCount.get()).isEqualTo(THREAD_COUNT);

        // 핵심 불변 조건: 성공 건수는 초기 잔여 연차를 넘을 수 없다 (오버부킹 방지)
        assertThat(successCount.get()).isLessThanOrEqualTo((int) INITIAL_REMAINING);

        Employee reloaded = employeeRepository.findEmployeeById(testEmployeeId).orElseThrow();
        double expectedRemaining = INITIAL_REMAINING - successCount.get();

        // 핵심 불변 조건: 최종 잔여 연차는 절대 음수가 아니고, "성공 건수만큼 정확히" 차감돼야 한다
        // (락 없이 lost update가 나면 이 값이 음수이거나 successCount와 어긋난다)
        assertThat(reloaded.getAnnual().getRemainingAnnualCount()).isEqualTo(expectedRemaining);
        assertThat(reloaded.getAnnual().getRemainingAnnualCount()).isGreaterThanOrEqualTo(0.0);
        double initialUseCount = 15.0 - INITIAL_REMAINING;
        assertThat(reloaded.getAnnual().getUseCount()).isEqualTo(initialUseCount + successCount.get());

        // 저장된 AnnualRecord 개수도 성공 건수와 정확히 일치해야 한다
        long savedRecordCount = annualRecordRepository.findAll().stream()
                .filter(record -> testEmployeeId.equals(record.getEmployeeId()))
                .count();
        assertThat(savedRecordCount).isEqualTo(successCount.get());
    }

    private Employee createTestEmployee(double remainingAnnualCount) {
        EmployeeImage employeeImage = new EmployeeImage("imageName", "imagePath", 1L, "JPG");
        Annual annual = Annual.builder()
                .totalAnnualCount(15.0)
                .useCount(15.0 - remainingAnnualCount)
                .remainingAnnualCount(remainingAnnualCount)
                .sickAnnualCount(0.0)
                .build();
        List<UserRole> userRoles = List.of(UserRole.USER);

        return new Employee(
                "동시성테스트", "annual-concurrency-test-" + System.nanoTime() + "@test.com",
                Gender.MALE, LocalDate.of(1990, 1, 1), LocalDate.of(2025, 1, 1), null,
                "테스트팀", Position.EMPLOYEE, userRoles, employeeImage, annual
        );
    }
}
