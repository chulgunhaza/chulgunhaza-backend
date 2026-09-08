package com.example.chulgunhazabackend.domain.board;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 관리자 백로그 Epic 5 — 공지 고정(pinned)의 순수 도메인 로직 단위 테스트.
 */
class PostTest {

    @Test
    @DisplayName("새로 만든 게시글은 기본적으로 고정돼있지 않다")
    void 기본값은_고정_아님이다() {
        Post post = Post.builder().title("제목").content("본문").build();

        assertThat(post.isPinned()).isFalse();
    }

    @Test
    @DisplayName("togglePinned()를 한 번 호출하면 고정 상태가 된다")
    void 한번_토글하면_고정된다() {
        Post post = Post.builder().title("제목").content("본문").build();

        post.togglePinned();

        assertThat(post.isPinned()).isTrue();
    }

    @Test
    @DisplayName("togglePinned()를 두 번 호출하면 원래 상태로 돌아온다")
    void 두번_토글하면_원상복구된다() {
        Post post = Post.builder().title("제목").content("본문").build();

        post.togglePinned();
        post.togglePinned();

        assertThat(post.isPinned()).isFalse();
    }

    @RepeatedTest(5)
    @DisplayName("togglePinned()를 홀수 번 호출하면 항상 고정 상태다")
    void 홀수번_토글하면_고정_상태다(org.junit.jupiter.api.RepetitionInfo repetitionInfo) {
        Post post = Post.builder().title("제목").content("본문").build();
        int times = repetitionInfo.getCurrentRepetition() * 2 - 1; // 1, 3, 5, 7, 9

        for (int i = 0; i < times; i++) {
            post.togglePinned();
        }

        assertThat(post.isPinned()).isTrue();
    }

    @Test
    @DisplayName("updatePost()는 pinned 상태에 영향을 주지 않는다")
    void updatePost는_고정_상태를_안바꾼다() {
        Post post = Post.builder().title("제목").content("본문").build();
        post.togglePinned();

        post.updatePost("새 제목", "새 본문", new Category("공지"), java.util.List.of());

        assertThat(post.isPinned()).isTrue();
    }

    @Test
    @DisplayName("delete()는 pinned 상태에 영향을 주지 않는다")
    void delete는_고정_상태를_안바꾼다() {
        Post post = Post.builder().title("제목").content("본문").build();
        post.togglePinned();

        post.delete();

        assertThat(post.isPinned()).isTrue();
        assertThat(post.getDelFlag()).isTrue();
    }
}
