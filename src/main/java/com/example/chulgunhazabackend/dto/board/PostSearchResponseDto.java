package com.example.chulgunhazabackend.dto.board;

import com.example.chulgunhazabackend.domain.board.Category;
import com.example.chulgunhazabackend.domain.board.Post;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@AllArgsConstructor
@Getter
@NoArgsConstructor
public class PostSearchResponseDto {
    private String title;
    private String content;
    private String author; // #59: Post-Employee 연동 후 작성자 이름 노출
    private List<String> imageList;
    private int count;
    private Category category;

    public PostSearchResponseDto fromEntity(Post post, List<String> imageList){
        return new PostSearchResponseDto(
                post.getTitle()
                ,post.getContent()
                ,post.getEmployee() != null ? post.getEmployee().getName() : null // 마이그레이션 이전 게시글 대비
                ,imageList
                ,post.getCount()
                ,post.getCategory()
        );
    }


}
