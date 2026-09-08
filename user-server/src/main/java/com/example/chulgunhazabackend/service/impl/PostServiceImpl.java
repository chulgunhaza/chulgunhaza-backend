package com.example.chulgunhazabackend.service.impl;

import com.example.chulgunhazabackend.domain.board.Category;
import com.example.chulgunhazabackend.domain.board.Post;
import com.example.chulgunhazabackend.domain.member.Employee;
import com.example.chulgunhazabackend.dto.PageDto;
import com.example.chulgunhazabackend.dto.board.*;
import com.example.chulgunhazabackend.exception.employeeException.EmployeeException;
import com.example.chulgunhazabackend.exception.employeeException.EmployeeExceptionType;
import com.example.chulgunhazabackend.exception.postException.PostException;
import com.example.chulgunhazabackend.exception.postException.PostExceptionType;
import com.example.chulgunhazabackend.repository.EmployeeRepository;
import com.example.chulgunhazabackend.repository.PostRepository;
import com.example.chulgunhazabackend.service.FileService;
import com.example.chulgunhazabackend.service.PostService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.List;

@Transactional
@Service
@RequiredArgsConstructor
public class PostServiceImpl implements PostService {

    private final PostRepository postRepository;

    private final FileService fileService;

    private final EmployeeRepository employeeRepository; // #59: Post-Employee 연동

    @Transactional(rollbackFor = IOException.class)
    public Long create(PostCreateRequestDto dto, List<MultipartFile> postFiles, Long executor) throws IOException {
        Employee employee = employeeRepository.findEmployeeById(executor)
                .orElseThrow(() -> new EmployeeException(EmployeeExceptionType.NOT_EXIST_USER));
        return postRepository.save(dto.toEntity(new Category(dto.getCategoryName()), fileService.savePostFiles(postFiles), employee)).getId();
    }

    @Transactional(readOnly = true)
    public PostSearchResponseDto findById(Long postNumber) throws MalformedURLException {
        Post post = validAfterGetPost(postNumber);
        // Redis 추가 시 count 증가 로직.. 추가
        return new PostSearchResponseDto().fromEntity(post, fileService.findPostFiles(post.getPostFilesList()));
    }

    public Long deleteById(Long postNumber, Long executor, boolean isManager) throws MalformedURLException {
        Post post = validAfterGetPost(postNumber);
        assertCanModify(post, executor, isManager);
        post.delete();
        return postRepository.save(post).getId();
    }

    @Transactional(rollbackFor = IOException.class)
    public Long modifyById(Long postNumber, PostModifyRequestDto dto, List<MultipartFile> postFiles, Long executor, boolean isManager) throws IOException {
        Post post = validAfterGetPost(postNumber);
        assertCanModify(post, executor, isManager);
        post.updatePost(dto.getTitle(), dto.getContent(), new Category(dto.getCategoryName()), fileService.savePostFiles(postFiles));
        return postRepository.save(post).getId();
    }

    // #87: 원래는 삭제/수정에 작성자 검증이 아예 없어서 로그인만 돼 있으면 남의 글도
    // 지울 수 있었다. 관리자(MANAGER/ADMIN)는 예외적으로 허용 — Epic 5의 "관리자
    // 전용 강제 삭제" 요구사항과 맞물린다. employee가 없는 마이그레이션 이전 글은
    // 작성자를 특정할 수 없어 관리자만 처리 가능하다.
    private void assertCanModify(Post post, Long executor, boolean isManager) {
        if (isManager) {
            return;
        }
        boolean isAuthor = post.getEmployee() != null && post.getEmployee().getId().equals(executor);
        if (!isAuthor) {
            throw new PostException(PostExceptionType.NOT_POST_AUTHOR);
        }
    }

    @Transactional(readOnly = true)
    public PageDto<PostListResponseDto> findAllByDelFlagFalseAndCategory(Pageable pageable, String category){
        Page<PostListResponseDto> contents = postRepository.findAllByDelFlagFalseAndCategoryOrderByPinnedDescCreatedAtDesc(pageable, new Category(category)).map(post -> new PostListResponseDto().fromEntity(post));
        return new PageDto<PostListResponseDto>(contents);
    }

    public boolean togglePin(Long postNumber) throws MalformedURLException {
        Post post = validAfterGetPost(postNumber);
        post.togglePinned();
        return postRepository.save(post).isPinned();
    }

    private Post validAfterGetPost(Long postNumber){
        return postRepository.findByIdAndDelFlagFalse(postNumber).orElseThrow(() ->new PostException(PostExceptionType.POST_NOT_FOUND));
    }
}
