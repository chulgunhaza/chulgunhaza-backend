package com.example.chulgunhazabackend.service.impl;

import com.example.chulgunhazabackend.domain.annual.Annual;
import com.example.chulgunhazabackend.domain.board.Post;
import com.example.chulgunhazabackend.domain.member.*;
import com.example.chulgunhazabackend.dto.board.PostCreateRequestDto;
import com.example.chulgunhazabackend.exception.employeeException.EmployeeException;
import com.example.chulgunhazabackend.exception.employeeException.EmployeeExceptionType;
import com.example.chulgunhazabackend.repository.EmployeeRepository;
import com.example.chulgunhazabackend.repository.PostRepository;
import com.example.chulgunhazabackend.service.FileService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * #59 Post-User(Employee) 연동 회귀 테스트. 이전엔 {@code Post.employee} 필드 자체가
 * 주석 처리돼 있어 작성자 정보가 아예 저장되지 않았다.
 */
@ExtendWith(MockitoExtension.class)
class PostServiceImplTest {

    @Mock
    private PostRepository postRepository;

    @Mock
    private FileService fileService;

    @Mock
    private EmployeeRepository employeeRepository;

    @InjectMocks
    private PostServiceImpl postService;

    private PostCreateRequestDto createRequestDto;

    @BeforeEach
    void setUp() {
        createRequestDto = new PostCreateRequestDto(
                "테스트 게시글 제목입니다 열자이상", "테스트 게시글 본문입니다.".repeat(10), "공지"
        );
    }

    @Test
    @DisplayName("게시글을 생성하면 작성자(Employee)가 함께 저장된다")
    void create_성공시_작성자가_연결된다() throws Exception {
        Employee author = employeeOf(1L, "김태동");
        given(employeeRepository.findEmployeeById(1L)).willReturn(Optional.of(author));
        given(fileService.savePostFiles(any())).willReturn(List.of());
        given(postRepository.save(any(Post.class))).willAnswer(invocation -> invocation.getArgument(0));

        postService.create(createRequestDto, List.of(), 1L);

        verify(postRepository).save(org.mockito.ArgumentMatchers.argThat(post ->
                post.getEmployee() != null && post.getEmployee().getName().equals("김태동")
        ));
    }

    @Test
    @DisplayName("존재하지 않는 사원이 작성자로 넘어오면 EmployeeException(NOT_EXIST_USER)이 발생하고 저장하지 않는다")
    void create_존재하지_않는_사원이면_예외() {
        given(employeeRepository.findEmployeeById(999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> postService.create(createRequestDto, List.of(), 999L))
                .isInstanceOf(EmployeeException.class)
                .satisfies(ex -> assertThat(((EmployeeException) ex).getEmployeeExceptionType())
                        .isEqualTo(EmployeeExceptionType.NOT_EXIST_USER));

        verify(postRepository, never()).save(any());
    }

    private Employee employeeOf(Long id, String name) {
        EmployeeImage employeeImage = new EmployeeImage("default", "path", 1L, "PNG");
        Employee employee = new Employee(
                name, name + "@chulgunhaza.com", Gender.MALE, LocalDate.of(1999, 1, 1),
                LocalDate.of(2025, 1, 1), null, "개발팀", Position.EMPLOYEE,
                List.of(UserRole.USER), employeeImage, new Annual()
        );
        try {
            var field = Employee.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(employee, id);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
        return employee;
    }
}
