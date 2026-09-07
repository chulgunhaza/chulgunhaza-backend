package com.example.chulgunhazabackend.controller;

import com.example.chulgunhazabackend.dto.Employee.EmployeeCredentialDto;
import com.example.chulgunhazabackend.dto.PageDto;
import com.example.chulgunhazabackend.dto.board.*;
import com.example.chulgunhazabackend.service.PostService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("v1/post")
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;

    @PostMapping("/create")
    public ResponseEntity<Long> create(@Valid @RequestPart PostCreateRequestDto dto, @RequestParam("list") List<MultipartFile> list,
                                       @AuthenticationPrincipal EmployeeCredentialDto employeeCredentialDto) throws IOException {
        return ResponseEntity.status(201).body(postService.create(dto, list, employeeCredentialDto.getId()));
    }

    @GetMapping("/{postNumber}")
    public ResponseEntity<PostSearchResponseDto> findById(@PathVariable Long postNumber) throws IOException {
        return ResponseEntity.status(200).body(postService.findById(postNumber));
    }

    @PatchMapping("/delete/{postNumber}")
    public ResponseEntity<Long> deleteById(@PathVariable Long postNumber) throws IOException {
        return ResponseEntity.status(204).body(postService.deleteById(postNumber));
    }

    @PutMapping("/modify/{postNumber}")
    public ResponseEntity<Long> modifyById(@PathVariable Long postNumber,
                                           @Valid @RequestPart PostModifyRequestDto dto,
                                           @RequestParam("list") List<MultipartFile> list) throws IOException {
        return ResponseEntity.status(200).body(postService.modifyById(postNumber, dto, list));
    }

    @GetMapping("")
    public ResponseEntity<PageDto<PostListResponseDto>> findAllByDelFlagFalseAndCategory(@RequestParam("category")String category, @PageableDefault(size = 10) Pageable pageable) throws IOException {
        return ResponseEntity.status(200).body(postService.findAllByDelFlagFalseAndCategory(pageable,category));
    }

    // 관리자 백로그 Epic 5 — 공지 고정 토글
    @PatchMapping("/{postNumber}/pin")
    @PreAuthorize("hasAnyRole('ROLE_MANAGER')")
    public ResponseEntity<Boolean> togglePin(@PathVariable Long postNumber) throws IOException {
        return ResponseEntity.ok(postService.togglePin(postNumber));
    }
}
