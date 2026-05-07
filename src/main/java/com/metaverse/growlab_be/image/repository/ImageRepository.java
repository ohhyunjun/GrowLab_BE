package com.metaverse.growlab_be.image.repository;

import com.metaverse.growlab_be.image.domain.Image;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ImageRepository extends JpaRepository<Image, Long> {
}