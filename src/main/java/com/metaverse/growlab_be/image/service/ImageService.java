package com.metaverse.growlab_be.image.service;

import com.metaverse.growlab_be.image.domain.Image;
import com.metaverse.growlab_be.image.repository.ImageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ImageService {
    private final ImageRepository imageRepository;

    @Transactional
    public void deleteImage(Image image) {
        // DB 데이터 삭제
        imageRepository.delete(image);
    }
}