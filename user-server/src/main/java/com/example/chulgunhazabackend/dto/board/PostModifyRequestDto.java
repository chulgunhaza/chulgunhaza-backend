package com.example.chulgunhazabackend.dto.board;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;


@AllArgsConstructor
@Getter
public class PostModifyRequestDto {

    @NotBlank(message = "제목을 작성해야합니다.")
    @Size(min = 10, max = 255, message = "제목의 길이는 10자 이상 255자 이하입니다.")
    private String title;

    @NotBlank(message = "내용을 작성해야합니다.")
    @Size(min = 100, max = 1000, message = "분문의 길이는 10자 이상 1000자 이하입니다.")
    private String content;

    @NotBlank(message = "카테고리를 선택해야합니다.")
    private String categoryName;

}
