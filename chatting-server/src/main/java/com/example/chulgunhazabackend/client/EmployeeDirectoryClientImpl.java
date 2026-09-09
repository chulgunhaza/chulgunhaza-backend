package com.example.chulgunhazabackend.client;

import com.example.chulgunhazabackend.exception.chatException.ChatException;
import com.example.chulgunhazabackend.exception.chatException.ChatExceptionType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class EmployeeDirectoryClientImpl implements EmployeeDirectoryClient {

    private final RestTemplate restTemplate;

    @Value("${user-server.internal-url}")
    private String userServerInternalUrl;

    @Override
    public List<EmployeeSummary> findByIds(List<Long> ids) {
        if (ids.isEmpty()) {
            return List.of();
        }

        String idsParam = ids.stream().map(String::valueOf).collect(Collectors.joining(","));
        String url = UriComponentsBuilder.fromHttpUrl(userServerInternalUrl + "/internal/employees")
                .queryParam("ids", idsParam)
                .toUriString();

        try {
            EmployeeSummary[] response = restTemplate.getForObject(url, EmployeeSummary[].class);
            return response == null ? List.of() : Arrays.asList(response);
        } catch (RestClientException e) {
            // #101: 대시보드 통계(#100)와 달리 이건 조용히 빈 값으로 대체하면 안 된다 —
            // 채팅방 목록/생성이 사원 정보 없이는 아예 의미가 없어서, 호출부가 명확히
            // 실패를 알고 처리하게 한다. "존재하지 않음"과는 구분한다(EMPLOYEE_NOT_FOUND는
            // 응답은 왔지만 요청한 id가 그 안에 없는 경우).
            log.error("user-server 내부 API(/internal/employees) 호출 실패: {}", e.getMessage());
            throw new ChatException(ChatExceptionType.EMPLOYEE_SERVICE_UNAVAILABLE);
        }
    }

    @Override
    public boolean existsById(Long id) {
        return findByIds(List.of(id)).stream().anyMatch(summary -> summary.getId().equals(id));
    }
}
