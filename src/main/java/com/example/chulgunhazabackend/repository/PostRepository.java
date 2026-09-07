package com.example.chulgunhazabackend.repository;

import com.example.chulgunhazabackend.domain.board.Category;
import com.example.chulgunhazabackend.domain.board.Post;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PostRepository extends JpaRepository<Post, Long> {

    @EntityGraph(attributePaths = "postFilesList")
    Optional<Post> findByIdAndDelFlagFalse(Long postId);

    // 고정된 글이 항상 위에 오도록 pinned desc를 우선 정렬 기준으로 둔다(#Epic 5).
    Page<Post> findAllByDelFlagFalseAndCategoryOrderByPinnedDescCreatedAtDesc(Pageable pageable, Category category);

    // 관리자 백로그 Epic 6 — 대시보드 통계 카드용.
    long countByDelFlagFalse();

}
