package com.example.chulgunhazabackend.service;

import com.example.chulgunhazabackend.dto.PageDto;
import com.example.chulgunhazabackend.dto.board.*;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.List;

public interface PostService {
    Long create(PostCreateRequestDto dto, List<MultipartFile> postFiles, Long executor) throws IOException;
    PostSearchResponseDto findById(Long postNumber) throws MalformedURLException;
    Long deleteById(Long postNumber) throws MalformedURLException;
    Long modifyById(Long postNumber, PostModifyRequestDto dto, List<MultipartFile> postFiles) throws IOException;
    PageDto<PostListResponseDto> findAllByDelFlagFalseAndCategory(Pageable pageable, String category);
}
