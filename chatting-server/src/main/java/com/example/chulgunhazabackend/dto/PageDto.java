package com.example.chulgunhazabackend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Page;

import java.util.List;

// #101: user-server/attendance-server의 dto.PageDto와 동일한 내용을 그대로
// 복제(서비스 간 wire 계약이 아니라 각자 자기 REST 응답만 직렬화하는 뷰 모델이라
// #100과 동일한 이유로 공유 모듈에 안 넣고 복제한다).
@Getter
@NoArgsConstructor
public class PageDto<T> {
    private List<T> contents;
    private long totalElements;
    private int totalPages;
    private int currentPage;
    private boolean hasNextPage;

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
