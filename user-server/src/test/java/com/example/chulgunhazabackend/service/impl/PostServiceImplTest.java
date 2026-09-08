package com.example.chulgunhazabackend.service.impl;

import com.example.chulgunhazabackend.domain.annual.Annual;
import com.example.chulgunhazabackend.domain.board.Category;
import com.example.chulgunhazabackend.domain.board.Post;
import com.example.chulgunhazabackend.domain.member.*;
import com.example.chulgunhazabackend.dto.board.PostCreateRequestDto;
import com.example.chulgunhazabackend.dto.board.PostModifyRequestDto;
import com.example.chulgunhazabackend.exception.employeeException.EmployeeException;
import com.example.chulgunhazabackend.exception.employeeException.EmployeeExceptionType;
import com.example.chulgunhazabackend.exception.postException.PostException;
import com.example.chulgunhazabackend.exception.postException.PostExceptionType;
import com.example.chulgunhazabackend.repository.EmployeeRepository;
import com.example.chulgunhazabackend.repository.PostRepository;
import com.example.chulgunhazabackend.service.FileService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
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

    // --- #87: 게시글 삭제/수정 작성자 검증(+관리자 예외) 회귀 테스트 ---
    // 원래는 deleteById/modifyById에 작성자 검증이 아예 없어서 로그인만 돼 있으면
    // 아무나 남의 글을 지우거나 고칠 수 있었다.

    private Post postWithAuthor(Long postId, Long authorId) {
        Employee author = employeeOf(authorId, "작성자");
        return Post.builder()
                .id(postId)
                .title("원래 제목입니다 열자이상")
                .content("원래 본문입니다.".repeat(10))
                .category(new Category("공지"))
                .postFilesList(List.of())
                .employee(author)
                .build();
    }

    private Post postWithoutAuthor(Long postId) {
        return Post.builder()
                .id(postId)
                .title("마이그레이션 이전 글입니다")
                .content("마이그레이션 이전 본문입니다.".repeat(10))
                .category(new Category("공지"))
                .postFilesList(List.of())
                .build();
    }

    @Test
    @DisplayName("작성자 본인이 삭제하면 성공한다")
    void deleteById_작성자_본인이면_성공() throws Exception {
        Post post = postWithAuthor(1L, 10L);
        given(postRepository.findByIdAndDelFlagFalse(1L)).willReturn(Optional.of(post));
        given(postRepository.save(any(Post.class))).willAnswer(inv -> inv.getArgument(0));

        postService.deleteById(1L, 10L, false);

        assertThat(post.getDelFlag()).isTrue();
    }

    @Test
    @DisplayName("작성자가 아니고 관리자도 아니면 삭제 시 PostException(NOT_POST_AUTHOR)이 발생한다")
    void deleteById_작성자도_관리자도_아니면_예외() {
        Post post = postWithAuthor(1L, 10L);
        given(postRepository.findByIdAndDelFlagFalse(1L)).willReturn(Optional.of(post));

        assertThatThrownBy(() -> postService.deleteById(1L, 999L, false))
                .isInstanceOf(PostException.class)
                .satisfies(ex -> assertThat(((PostException) ex).getPostExceptionType())
                        .isEqualTo(PostExceptionType.NOT_POST_AUTHOR));

        verify(postRepository, never()).save(any());
    }

    @Test
    @DisplayName("작성자가 아니어도 관리자면 강제 삭제할 수 있다")
    void deleteById_관리자면_작성자_아니어도_성공() throws Exception {
        Post post = postWithAuthor(1L, 10L);
        given(postRepository.findByIdAndDelFlagFalse(1L)).willReturn(Optional.of(post));
        given(postRepository.save(any(Post.class))).willAnswer(inv -> inv.getArgument(0));

        postService.deleteById(1L, 999L, true);

        assertThat(post.getDelFlag()).isTrue();
    }

    @Test
    @DisplayName("작성자 정보가 없는(마이그레이션 이전) 글은 관리자만 삭제할 수 있다")
    void deleteById_작성자_없는_글은_관리자만_가능() throws Exception {
        Post post = postWithoutAuthor(2L);
        given(postRepository.findByIdAndDelFlagFalse(2L)).willReturn(Optional.of(post));
        given(postRepository.save(any(Post.class))).willAnswer(inv -> inv.getArgument(0));

        postService.deleteById(2L, 999L, true);

        assertThat(post.getDelFlag()).isTrue();
    }

    @Test
    @DisplayName("작성자 정보가 없는 글은 관리자가 아니면 누구든 삭제할 수 없다")
    void deleteById_작성자_없는_글은_비관리자면_실패() {
        Post post = postWithoutAuthor(2L);
        given(postRepository.findByIdAndDelFlagFalse(2L)).willReturn(Optional.of(post));

        assertThatThrownBy(() -> postService.deleteById(2L, 1L, false))
                .isInstanceOf(PostException.class)
                .satisfies(ex -> assertThat(((PostException) ex).getPostExceptionType())
                        .isEqualTo(PostExceptionType.NOT_POST_AUTHOR));
    }

    @Test
    @DisplayName("존재하지 않는 게시글을 삭제하려 하면 PostException(POST_NOT_FOUND)이 발생한다")
    void deleteById_존재하지_않으면_예외() {
        given(postRepository.findByIdAndDelFlagFalse(999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> postService.deleteById(999L, 1L, false))
                .isInstanceOf(PostException.class)
                .satisfies(ex -> assertThat(((PostException) ex).getPostExceptionType())
                        .isEqualTo(PostExceptionType.POST_NOT_FOUND));
    }

    @ParameterizedTest(name = "executor={0}, isManager={1} -> 허용={2}")
    @CsvSource({
            "10, false, true",   // 작성자 본인
            "999, false, false", // 작성자도 관리자도 아님
            "999, true, true",   // 작성자는 아니지만 관리자
            "10, true, true",    // 작성자면서 관리자이기도 함
    })
    @DisplayName("삭제 허용 여부는 (작성자 일치 OR 관리자)로 결정된다")
    void deleteById_허용_여부_조합(Long executor, boolean isManager, boolean expectAllowed) {
        Post post = postWithAuthor(1L, 10L);
        given(postRepository.findByIdAndDelFlagFalse(1L)).willReturn(Optional.of(post));
        if (expectAllowed) {
            given(postRepository.save(any(Post.class))).willAnswer(inv -> inv.getArgument(0));
        }

        if (expectAllowed) {
            org.junit.jupiter.api.Assertions.assertDoesNotThrow(() -> postService.deleteById(1L, executor, isManager));
        } else {
            assertThatThrownBy(() -> postService.deleteById(1L, executor, isManager))
                    .isInstanceOf(PostException.class);
        }
    }

    @Test
    @DisplayName("작성자 본인이 수정하면 성공하고 내용이 반영된다")
    void modifyById_작성자_본인이면_성공() throws Exception {
        Post post = postWithAuthor(1L, 10L);
        PostModifyRequestDto dto = new PostModifyRequestDto("새 제목입니다 열자이상", "새 본문입니다.".repeat(10), "공지");
        given(postRepository.findByIdAndDelFlagFalse(1L)).willReturn(Optional.of(post));
        given(fileService.savePostFiles(any())).willReturn(List.of());
        given(postRepository.save(any(Post.class))).willAnswer(inv -> inv.getArgument(0));

        postService.modifyById(1L, dto, List.of(), 10L, false);

        assertThat(post.getTitle()).isEqualTo("새 제목입니다 열자이상");
    }

    @Test
    @DisplayName("작성자가 아니고 관리자도 아니면 수정 시 예외가 발생하고 저장되지 않는다")
    void modifyById_작성자도_관리자도_아니면_예외() {
        Post post = postWithAuthor(1L, 10L);
        PostModifyRequestDto dto = new PostModifyRequestDto("새 제목입니다 열자이상", "새 본문입니다.".repeat(10), "공지");
        given(postRepository.findByIdAndDelFlagFalse(1L)).willReturn(Optional.of(post));

        assertThatThrownBy(() -> postService.modifyById(1L, dto, List.of(), 999L, false))
                .isInstanceOf(PostException.class);

        verify(postRepository, never()).save(any());
        assertThat(post.getTitle()).isNotEqualTo("새 제목입니다 열자이상"); // 원본 안 바뀜
    }

    @Test
    @DisplayName("작성자가 아니어도 관리자면 강제 수정할 수 있다")
    void modifyById_관리자면_작성자_아니어도_성공() throws Exception {
        Post post = postWithAuthor(1L, 10L);
        PostModifyRequestDto dto = new PostModifyRequestDto("관리자가 고친 제목입니다", "본문입니다.".repeat(10), "공지");
        given(postRepository.findByIdAndDelFlagFalse(1L)).willReturn(Optional.of(post));
        given(fileService.savePostFiles(any())).willReturn(List.of());
        given(postRepository.save(any(Post.class))).willAnswer(inv -> inv.getArgument(0));

        postService.modifyById(1L, dto, List.of(), 999L, true);

        assertThat(post.getTitle()).isEqualTo("관리자가 고친 제목입니다");
    }

    // --- Epic 5: 공지 고정 토글 ---

    @Test
    @DisplayName("고정 안 된 글을 togglePin하면 고정된다")
    void togglePin_안고정_상태에서_토글하면_고정된다() throws Exception {
        Post post = postWithAuthor(1L, 10L);
        given(postRepository.findByIdAndDelFlagFalse(1L)).willReturn(Optional.of(post));
        given(postRepository.save(any(Post.class))).willAnswer(inv -> inv.getArgument(0));

        boolean result = postService.togglePin(1L);

        assertThat(result).isTrue();
        assertThat(post.isPinned()).isTrue();
    }

    @Test
    @DisplayName("이미 고정된 글을 togglePin하면 고정 해제된다")
    void togglePin_고정_상태에서_토글하면_해제된다() throws Exception {
        Post post = postWithAuthor(1L, 10L);
        post.togglePinned(); // 미리 고정해둠
        given(postRepository.findByIdAndDelFlagFalse(1L)).willReturn(Optional.of(post));
        given(postRepository.save(any(Post.class))).willAnswer(inv -> inv.getArgument(0));

        boolean result = postService.togglePin(1L);

        assertThat(result).isFalse();
        assertThat(post.isPinned()).isFalse();
    }

    @Test
    @DisplayName("존재하지 않는 게시글을 고정하려 하면 예외가 발생한다")
    void togglePin_존재하지_않으면_예외() {
        given(postRepository.findByIdAndDelFlagFalse(999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> postService.togglePin(999L))
                .isInstanceOf(PostException.class)
                .satisfies(ex -> assertThat(((PostException) ex).getPostExceptionType())
                        .isEqualTo(PostExceptionType.POST_NOT_FOUND));
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
