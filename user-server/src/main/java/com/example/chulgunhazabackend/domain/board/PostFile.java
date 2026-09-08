package com.example.chulgunhazabackend.domain.board;

import com.example.chulgunhazabackend.domain.common.BaseEntity;
import com.example.chulgunhazabackend.domain.common.BaseFile;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Embeddable
@Getter
@ToString
public class PostFile extends BaseFile {

    private int ord; // 파일의 순번

    public PostFile(String originalFileName, String uuidFileName, String filePath, String fileType, Long fileSize, int ord) {
        super(originalFileName, uuidFileName, filePath, fileType, fileSize);
        setOrd(ord);// BaseFile의 생성자를 호출
    }

    public PostFile() {
        super();
    }

    public void setOrd(int ord){
        this.ord = ord;
    }

}
