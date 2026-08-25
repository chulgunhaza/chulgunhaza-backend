package com.example.chulgunhazabackend.service.impl;

import com.example.chulgunhazabackend.domain.board.PostFile;
import com.example.chulgunhazabackend.domain.common.BaseFile;
import com.example.chulgunhazabackend.domain.member.EmployeeImage;
import com.example.chulgunhazabackend.service.FileService;
import jakarta.annotation.PostConstruct;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LocalFileServiceImpl implements FileService {

    @Value("${file.upload-dir}")
    private String uploadDir;

    @PostConstruct
    public void init() {
        createUploadDir();
    }

    private void createUploadDir(){
        File directory = new File(uploadDir);
        if(!directory.exists()){
            boolean created = directory.mkdirs();
            if (!created) {
                throw new IllegalStateException("파일 생성에 실패했습니다: " + directory.getAbsolutePath());
            }
        }
    }

    public PostFile saveFile(MultipartFile file, int ord) throws IOException {
        String originalFileName = file.getOriginalFilename();
        String uuidFileName = UUID.randomUUID().toString() + "_" + originalFileName;
        File destination = new File(uploadDir + uuidFileName);
        String fileType = Objects.requireNonNull(file.getOriginalFilename()).substring(file.getOriginalFilename().lastIndexOf("."));
        Long fileSize = file.getSize();
        // 파일 저장
        file.transferTo(destination);
        // 파일 Path 저장
        String filePath = destination.getAbsolutePath();

        return new PostFile(
                originalFileName
                ,uuidFileName
                ,filePath
                ,fileType
                ,fileSize
                ,ord
        );
    }

    @Override
    public List<PostFile> savePostFiles(List<MultipartFile> files) throws IOException {
        if(files.get(0).getOriginalFilename().equals("")){
            return null;
        }
        List<PostFile> postFiles = new ArrayList<>();
        int ord = 1;
        for(MultipartFile file : files){
            postFiles.add(saveFile(file,ord));
            ord++;
        }
        return postFiles;
    }

    @Override
    public List<String> findPostFiles(List<PostFile> postFile) throws MalformedURLException {
        return postFile.stream()
                .map(BaseFile::getFilePath)
                .collect(Collectors.toList());
    }

    // #58: 사원 프로필 이미지 업로드. EmployeeImage는 PostFile과 달리 BaseFile을 상속하지
    // 않는 별도 @Embeddable이라 saveFile()을 그대로 재사용할 수 없어 별도로 구현했다.
    @Override
    public EmployeeImage saveEmployeeImage(MultipartFile file) throws IOException {
        String originalFileName = file.getOriginalFilename();
        String uuidFileName = UUID.randomUUID().toString() + "_" + originalFileName;
        File destination = new File(uploadDir + uuidFileName);
        String fileType = Objects.requireNonNull(originalFileName)
                .substring(originalFileName.lastIndexOf("."));

        file.transferTo(destination);

        return EmployeeImage.builder()
                .imageName(originalFileName)
                .imagePath(destination.getAbsolutePath())
                .imageSize(file.getSize())
                .imageType(fileType)
                .build();
    }

}


