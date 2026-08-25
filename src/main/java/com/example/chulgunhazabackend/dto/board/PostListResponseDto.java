package com.example.chulgunhazabackend.dto.board;

import com.example.chulgunhazabackend.domain.board.Post;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@AllArgsConstructor
@Getter
@NoArgsConstructor
public class PostListResponseDto {
    private String title;
    private String author; // #59: Post-Employee 연동 후 작성자 이름 노출
    private int count;
    private LocalDateTime createdAt;


    public PostListResponseDto fromEntity(Post post){
        return new PostListResponseDto(
                post.getTitle()
                ,post.getEmployee() != null ? post.getEmployee().getName() : null // 마이그레이션 이전 게시글 대비
                ,post.getCount()
                ,post.getCreatedAt()
        );
    }
}
