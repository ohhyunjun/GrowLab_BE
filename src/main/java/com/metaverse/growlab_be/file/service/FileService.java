package com.metaverse.growlab_be.file.service;

import com.metaverse.growlab_be.article.domain.Article;
import com.metaverse.growlab_be.diary.domain.Diary;
import com.metaverse.growlab_be.file.domain.File;
import com.metaverse.growlab_be.file.repository.FileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Service
@RequiredArgsConstructor
public class FileService {
    private final FileRepository fileRepository;
    private final S3StorageService s3StorageService;

    @Value("${file.upload-dir}")
    private String uploadDir;

    // 게시글과 일지 파일 모두 동일한 File 엔터티에 기록한다.
    public File uploadFile(Article article, MultipartFile multipartFile) {
        StoredFile stored = store(multipartFile, "uploads/article");
        return fileRepository.save(new File(stored.originalName(), stored.storedName(),
                stored.path(), stored.url(), article));
    }

    public File uploadFile(Diary diary, MultipartFile multipartFile) {
        StoredFile stored = store(multipartFile, "uploads/diary");
        return fileRepository.save(new File(stored.originalName(), stored.storedName(),
                stored.path(), stored.url(), diary));
    }

    private StoredFile store(MultipartFile file, String folder) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("업로드할 파일이 비어 있습니다.");
        }

        if (s3StorageService.isEnabled()) {
            S3StorageService.UploadResult uploaded = s3StorageService.upload(file, folder);
            return new StoredFile(uploaded.originalFileName(), uploaded.storedFileName(),
                    uploaded.key(), uploaded.url());
        }

        String originalName = file.getOriginalFilename();
        String extension = originalName != null && originalName.contains(".")
                ? originalName.substring(originalName.lastIndexOf(".")) : ".jpg";
        String storedName = java.util.UUID.randomUUID() + extension;
        Path uploadPath = Path.of(uploadDir).toAbsolutePath().normalize();

        try {
            Files.createDirectories(uploadPath);
        } catch (IOException e) {
            throw new RuntimeException("파일 저장 디렉토리 생성 실패: " + uploadPath, e);
        }

        Path filePath = uploadPath.resolve(storedName);

        try {
            file.transferTo(filePath);
        } catch (IOException e) {
            throw new RuntimeException("파일 업로드 실패: " + filePath, e);
        }

        return new StoredFile(originalName == null ? storedName : originalName,
                storedName, filePath.toString(), "/uploads/" + storedName);
    }

    private record StoredFile(String originalName, String storedName, String path, String url) {}
}
