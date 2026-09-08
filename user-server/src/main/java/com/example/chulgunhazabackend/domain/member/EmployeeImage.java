package com.example.chulgunhazabackend.domain.member;

import jakarta.persistence.Embeddable;
import lombok.*;

@Embeddable
@Getter
@ToString
@NoArgsConstructor
public class EmployeeImage {

    private String imageName;

    private String imagePath;

    private Long imageSize;

    private String imageType;

    @Builder
    public EmployeeImage(String imageName, String imagePath, Long imageSize, String imageType) {
        this.imageName = imageName;
        this.imagePath = imagePath;
        this.imageSize = imageSize;
        this.imageType = imageType;
    }
}
