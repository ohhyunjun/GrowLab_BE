package com.metaverse.growlab_be;

import com.metaverse.growlab_be.article.domain.Article;
import com.metaverse.growlab_be.article.dto.ArticleResponseDto;
import com.metaverse.growlab_be.diary.domain.Diary;
import com.metaverse.growlab_be.file.domain.File;
import com.metaverse.growlab_be.file.repository.FileRepository;
import com.metaverse.growlab_be.file.service.FileService;
import com.metaverse.growlab_be.file.service.S3StorageService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FileAttachmentTest {
    @TempDir Path uploadDir;

    @Test
    void articleAndDiaryImagesUseFileRowsAndKeepResponseUrls() {
        FileRepository repository = mock(FileRepository.class);
        when(repository.save(any(File.class))).thenAnswer(call -> {
            File file = call.getArgument(0);
            file.setId(file.getArticle() == null ? 2L : 1L);
            return file;
        });
        FileService service = new FileService(repository, mock(S3StorageService.class));
        ReflectionTestUtils.setField(service, "uploadDir", uploadDir.toString());

        Article article = new Article();
        File articleFile = service.uploadFile(article,
                new MockMultipartFile("file", "article.png", "image/png", new byte[]{1, 2}));
        article.addFile(articleFile);
        assertThat(new ArticleResponseDto(article).getImages())
                .extracting(image -> image.getImgUrl())
                .containsExactly(articleFile.getFileUrl());

        Diary diary = new Diary();
        File diaryFile = service.uploadFile(diary,
                new MockMultipartFile("file", "diary.png", "image/png", new byte[]{3, 4}));
        diary.addFile(diaryFile);
        assertThat(diary.getFiles()).containsExactly(diaryFile);
        assertThat(diaryFile.getDiary()).isSameAs(diary);

        assertThat(articleFile.getFileUrl()).startsWith("/uploads/");
        assertThat(diaryFile.getFileUrl()).startsWith("/uploads/");
        assertThat(Files.exists(uploadDir.resolve(articleFile.getStoredFileName()))).isTrue();
        assertThat(Files.exists(uploadDir.resolve(diaryFile.getStoredFileName()))).isTrue();
    }

    @Test
    void s3ArticleAndDiaryUploadsKeepTheirPublicUrlsAndObjectKeys() {
        FileRepository repository = mock(FileRepository.class);
        when(repository.save(any(File.class))).thenAnswer(call -> call.getArgument(0));
        S3StorageService storage = mock(S3StorageService.class);
        when(storage.isEnabled()).thenReturn(true);
        when(storage.upload(any(), eq("uploads/article"))).thenReturn(
                new S3StorageService.UploadResult("article.png", "article-key.png",
                        "uploads/article/article-key.png", "https://example.test/article-key.png"));
        when(storage.upload(any(), eq("uploads/diary"))).thenReturn(
                new S3StorageService.UploadResult("diary.png", "diary-key.png",
                        "uploads/diary/diary-key.png", "https://example.test/diary-key.png"));
        FileService service = new FileService(repository, storage);

        File articleFile = service.uploadFile(new Article(),
                new MockMultipartFile("file", "article.png", "image/png", new byte[]{1}));
        File diaryFile = service.uploadFile(new Diary(),
                new MockMultipartFile("file", "diary.png", "image/png", new byte[]{2}));

        assertThat(articleFile.getFilePath()).isEqualTo("uploads/article/article-key.png");
        assertThat(articleFile.getFileUrl()).isEqualTo("https://example.test/article-key.png");
        assertThat(diaryFile.getFilePath()).isEqualTo("uploads/diary/diary-key.png");
        assertThat(diaryFile.getFileUrl()).isEqualTo("https://example.test/diary-key.png");
        verify(storage).upload(any(), eq("uploads/article"));
        verify(storage).upload(any(), eq("uploads/diary"));
    }
}
