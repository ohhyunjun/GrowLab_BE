package com.metaverse.growlab_be.photo.service;


import com.metaverse.growlab_be.device.domain.Device;
import com.metaverse.growlab_be.device.repository.DeviceRepository;
import com.metaverse.growlab_be.photo.domain.Photo;
import com.metaverse.growlab_be.photo.dto.PhotoRequestDto;
import com.metaverse.growlab_be.photo.dto.PhotoResponseDto;
import com.metaverse.growlab_be.photo.repository.PhotoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PhotoService {

    private final PhotoRepository  photoRepository;
    private final DeviceRepository deviceRepository;

    @Value("${file.upload-dir.camera}")
    private String uploadDir;

    @Transactional
    public PhotoResponseDto savePhoto(PhotoRequestDto dto) throws IOException {
        MultipartFile imageFile = dto.getImageFile();
        if (imageFile == null || imageFile.isEmpty()) {
            throw new IllegalArgumentException("이미지 파일이 필요합니다.");
        }

        Device device = deviceRepository.findById(dto.getSerialNumber())
                .orElseThrow(() -> new IllegalArgumentException("등록되지 않은 기기입니다: " + dto.getSerialNumber()));

        if (dto.getPortIndex() == null) {
            throw new IllegalArgumentException("포트 번호(portIndex)가 누락되었습니다.");
        }

        File directory = new File(uploadDir);
        if (!directory.exists()) directory.mkdirs();

        String extension = getFileExtension(imageFile.getOriginalFilename());
        String fileName  = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))
                + "_" + UUID.randomUUID() + (extension.isEmpty() ? "" : "." + extension);
        String filePath  = Paths.get(uploadDir, fileName).toString();
        imageFile.transferTo(new File(filePath));

        String  growthResult  = dto.getGrowthResult()      != null ? dto.getGrowthResult()      : "no_detection";
        Double  growthConf    = dto.getGrowthConfidence()  != null ? dto.getGrowthConfidence()  : 0.0;
        String  diseaseResult = dto.getDiseaseResult()     != null ? dto.getDiseaseResult()     : "no_detection";
        Double  diseaseConf   = dto.getDiseaseConfidence() != null ? dto.getDiseaseConfidence() : 0.0;

        Photo savedPhoto = photoRepository.save(new Photo(
                device, dto.getPortIndex(), filePath, fileName,
                growthResult, growthConf,
                diseaseResult, diseaseConf
        ));

        return new PhotoResponseDto(savedPhoto);
    }

    @Transactional(readOnly = true)
    public PhotoResponseDto findLatestPhoto() {
        return photoRepository.findTopByOrderByIdDesc()
                .map(PhotoResponseDto::new)
                .orElseThrow(() -> new IllegalArgumentException("저장된 사진이 없습니다."));
    }

    private String getFileExtension(String fileName) {
        if (fileName == null || fileName.isBlank()) return "";

        int dotIndex = fileName.lastIndexOf(".");
        if (dotIndex < 0 || dotIndex == fileName.length() - 1) return "";

        return fileName.substring(dotIndex + 1);
    }
}