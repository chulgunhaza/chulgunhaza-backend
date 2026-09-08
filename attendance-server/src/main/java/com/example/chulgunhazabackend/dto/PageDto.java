package com.example.chulgunhazabackend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Page;

import java.util.List;

// #100: user-server의 dto.PageDto와 동일한 내용을 그대로 복제. 이건 각 서비스가
// 자기 REST 응답을 스스로 직렬화하는 순수 뷰 모델이라(서비스 간 wire 계약이
// 아님) BaseEntity처럼 각자 복제해서 둔다 — MainNotificationDto/
// AppCorsConfigurationSource(실제로 여러 서비스에 걸친 계약/설정 로직)와는
// 성격이 다르다.
@Getter
@NoArgsConstructor
public class PageDto<T> {
    private List<T> contents;
    private long totalElements;
    private int totalPages;
    private int currentPage;
    private boolean hasNextPage;

    // #84: Lombok 기본 게터(isFirstPage/isLastPage)를 Jackson이 is 접두어를 벗겨서
    // firstPage/lastPage로 내려버리는 문제 — 게터를 직접 선언해 이름 고정.
    private boolean isFirstPage;
    private boolean isLastPage;

    public PageDto(Page<T> page) {
        this.contents = page.getContent();
        this.totalElements = page.getTotalElements();
        this.totalPages = page.getTotalPages();
        this.currentPage = page.getNumber();
        this.hasNextPage = page.hasNext();
        this.isFirstPage = page.isFirst();
        this.isLastPage = page.isLast();
    }

    @JsonProperty("isFirstPage")
    public boolean isFirstPage() {
        return isFirstPage;
    }

    @JsonProperty("isLastPage")
    public boolean isLastPage() {
        return isLastPage;
    }
}
