package com.example.chulgunhazabackend.domain.common;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.ColumnDefault;


@MappedSuperclass
@Getter
@ToString
@NoArgsConstructor
public abstract class BaseFile {

    private String originalFileName;

    private String uuidFileName;

    private String filePath;

    private String fileType;

    private Long fileSize;

    public BaseFile(String originalFileName, String uuidFileName, String filePath, String fileType, Long fileSize) {
        this.originalFileName = originalFileName;
        this.uuidFileName = uuidFileName;
        this.filePath = filePath;
        this.fileType = fileType;
        this.fileSize = fileSize;
    }

}
