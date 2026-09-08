package com.example.chulgunhazabackend.dto.board;

import com.example.chulgunhazabackend.domain.annual.Annual;
import com.example.chulgunhazabackend.domain.board.Category;
import com.example.chulgunhazabackend.domain.board.Post;
import com.example.chulgunhazabackend.domain.member.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * React 프론트를 만들다가 발견한 회귀: {@link PostListResponseDto} 에 postNumber(id)가
 * 아예 없어서 목록 화면에서 상세로 이동할 방법이 없었다. 이 테스트는 fromEntity가
 * postNumber를 채우는지 고정해서, 실수로 다시 빠지면 바로 잡히게 한다.
 */
class PostListResponseDtoTest {

    @Test
    @DisplayName("fromEntity는 게시글의 id를 postNumber로 채운다")
    void fromEntity는_postNumber를_채운다() {
        Employee employee = new Employee(
                "김태동", "kim@chulgunhaza.com", Gender.MALE,
                LocalDate.of(1999, 1, 1), LocalDate.of(2025, 1, 1), null,
                "개발팀", Position.EMPLOYEE, List.of(UserRole.USER),
                new EmployeeImage("default", "path", 1L, "PNG"), new Annual()
        );
        Post post = Post.builder()
                .title("테스트 제목입니다 열자이상")
                .content("본문")
                .category(new Category("공지"))
                .postFilesList(List.of())
                .employee(employee)
                .build();
        setField(post, "id", 42L);

        PostListResponseDto dto = new PostListResponseDto().fromEntity(post);

        assertThat(dto.getPostNumber()).isEqualTo(42L);
        assertThat(dto.getAuthor()).isEqualTo("김태동");
    }

    private void setField(Post post, String fieldName, Object value) {
        try {
            var field = Post.class.getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(post, value);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }
}
