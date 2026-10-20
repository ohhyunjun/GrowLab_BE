package com.metaverse.growlab_be;

import com.metaverse.growlab_be.article.domain.Article;
import com.metaverse.growlab_be.article.dto.ArticleResponseDto;
import com.metaverse.growlab_be.diary.domain.Diary;
import com.metaverse.growlab_be.file.domain.File;
import com.metaverse.growlab_be.file.repository.FileRepository;
import com.metaverse.growlab_be.file.service.FileService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
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
        FileService service = new FileService(repository);
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
}
