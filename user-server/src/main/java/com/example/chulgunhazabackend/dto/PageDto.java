package com.example.chulgunhazabackend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Page;

import java.util.List;

@Getter
@NoArgsConstructor
public class PageDto<T> {
    private List<T> contents;
    private long totalElements;  // 전체 게시물 수
    private int totalPages;      // 전체 페이지 수
    private int currentPage;     // 현재 페이지
    private boolean hasNextPage; // 다음 페이지가 있는지 여부

    // Lombok이 만드는 게터가 isFirstPage()/isLastPage()인데, Jackson은 isXxx() 형태의
    // boolean 게터를 프로퍼티명 Xxx로 매핑해서(is 접두어를 벗김) 실제 JSON은
    // firstPage/lastPage로 내려가고 있었다(#84) — 프론트 PageDto 타입과 어긋나서
    // 페이지네이션 버튼이 첫/마지막 페이지에서도 안 꺼지는 조용한 버그였다.
    // 필드에 @JsonProperty를 붙이면 Jackson이 필드/게터를 서로 다른 프로퍼티로 보고
    // isFirstPage와 firstPage를 둘 다 내려버려서(중복 직렬화 확인함), 게터를 직접
    // 선언해 이름을 고정한다 — Lombok은 이미 같은 시그니처의 메서드가 있으면 자기
    // 게터를 안 만든다.
    private boolean isFirstPage; // 첫 번째 페이지인지 여부
    private boolean isLastPage;  // 마지막 페이지인지 여부

    public PageDto(Page<T> page) {
        this.contents = page.getContent();
        this.totalElements = page.getTotalElements();
        this.totalPages = page.getTotalPages();
        this.currentPage = page.getNumber();  // 현재 페이지 번호
        this.hasNextPage = page.hasNext();    // 다음 페이지가 있는지 여부
        this.isFirstPage = page.isFirst();    // 첫 번째 페이지인지 여부
        this.isLastPage = page.isLast();      // 마지막 페이지인지 여부
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
