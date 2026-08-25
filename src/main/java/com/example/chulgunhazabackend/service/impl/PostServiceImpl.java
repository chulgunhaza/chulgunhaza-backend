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

    public Long deleteById(Long postNumber) throws MalformedURLException {
        Post post = validAfterGetPost(postNumber);
        post.delete();
        return postRepository.save(post).getId();
    }

    @Transactional(rollbackFor = IOException.class)
    public Long modifyById(Long postNumber, PostModifyRequestDto dto, List<MultipartFile> postFiles) throws IOException {
        Post post = validAfterGetPost(postNumber);
        post.updatePost(dto.getTitle(), dto.getContent(), new Category(dto.getCategoryName()), fileService.savePostFiles(postFiles));
        return postRepository.save(post).getId();
    }

    @Transactional(readOnly = true)
    public PageDto<PostListResponseDto> findAllByDelFlagFalseAndCategory(Pageable pageable, String category){
        Page<PostListResponseDto> contents = postRepository.findAllByDelFlagFalseAndCategory(pageable, new Category(category)).map(post -> new PostListResponseDto().fromEntity(post));
        return new PageDto<PostListResponseDto>(contents);
    }

    private Post validAfterGetPost(Long postNumber){
        return postRepository.findByIdAndDelFlagFalse(postNumber).orElseThrow(() ->new PostException(PostExceptionType.POST_NOT_FOUND));
    }
}
