package com.metaverse.growlab_be.diary.service;

import com.metaverse.growlab_be.auth.domain.User;
import com.metaverse.growlab_be.auth.repository.UserRepository;
import com.metaverse.growlab_be.device.domain.Device;
import com.metaverse.growlab_be.device.repository.DeviceRepository;
import com.metaverse.growlab_be.diary.domain.Diary;
import com.metaverse.growlab_be.diary.dto.DiaryRequestDto;
import com.metaverse.growlab_be.diary.dto.DiaryResponseDto;
import com.metaverse.growlab_be.diary.repository.DiaryRepository;
import com.metaverse.growlab_be.file.service.FileService;
import com.metaverse.growlab_be.plant.domain.Plant;
import com.metaverse.growlab_be.plant.repository.PlantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DiaryService {
    private final DiaryRepository diaryRepository;
    private final DeviceRepository deviceRepository;
    private final PlantRepository plantRepository;
    private final UserRepository userRepository;
    private final FileService fileService;

    @Transactional
    public DiaryResponseDto createDiary(DiaryRequestDto request, String serialNumber,
                                        Long userId, List<MultipartFile> files) {
        User user = getUser(userId);
        Device device = getOwnedDevice(serialNumber, user);
        if (request.getPlantId() == null) {
            throw new IllegalArgumentException("재배 중인 식물을 선택해주세요.");
        }
        Plant plant = plantRepository.findById(request.getPlantId())
                .filter(Plant::isActive)
                .filter(item -> item.getDevice().getId().equals(serialNumber))
                .orElseThrow(() -> new IllegalArgumentException("재배 중인 식물을 선택해주세요."));
        Diary diary = new Diary(request, device, user, plant);
        Diary saved = diaryRepository.save(diary);
        attachFiles(saved, files);
        return new DiaryResponseDto(saved);
    }

    @Transactional(readOnly = true)
    public List<DiaryResponseDto> getDiaries(String serialNumber, Long userId) {
        User user = getUser(userId);
        getOwnedDevice(serialNumber, user);
        return diaryRepository.findByUserAndDeviceIdOrderByTargetDateDescCreatedAtDesc(user, serialNumber)
                .stream().map(DiaryResponseDto::new).toList();
    }

    @Transactional(readOnly = true)
    public DiaryResponseDto getDiaryById(Long id, String serialNumber, Long userId) {
        User user = getUser(userId);
        getOwnedDevice(serialNumber, user);
        return new DiaryResponseDto(getOwnedDiary(id, serialNumber, user));
    }

    @Transactional
    public DiaryResponseDto updateDiary(Long id, DiaryRequestDto request, String serialNumber,
                                        Long userId, List<MultipartFile> files) {
        User user = getUser(userId);
        getOwnedDevice(serialNumber, user);
        Diary diary = getOwnedDiary(id, serialNumber, user);
        if (request.getPlantId() != null && diary.getPlant() != null
                && !request.getPlantId().equals(diary.getPlant().getId())) {
            throw new IllegalArgumentException("기록한 식물은 수정할 수 없습니다.");
        }
        diary.update(request);
        attachFiles(diary, files);
        return new DiaryResponseDto(diary);
    }

    @Transactional
    public void deleteDiary(Long id, String serialNumber, Long userId) {
        User user = getUser(userId);
        getOwnedDevice(serialNumber, user);
        diaryRepository.delete(getOwnedDiary(id, serialNumber, user));
    }

    private void attachFiles(Diary diary, List<MultipartFile> files) {
        if (files == null) return;
        for (MultipartFile file : files) {
            if (!file.isEmpty()) diary.addFile(fileService.uploadFile(diary, file));
        }
    }

    private User getUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
    }

    private Device getOwnedDevice(String serialNumber, User user) {
        Device device = deviceRepository.findById(serialNumber)
                .orElseThrow(() -> new IllegalArgumentException("기기를 찾을 수 없습니다."));
        if (device.getUser() == null || !device.getUser().getId().equals(user.getId())) {
            throw new AccessDeniedException("해당 기기의 일지에 접근할 수 없습니다.");
        }
        return device;
    }

    private Diary getOwnedDiary(Long id, String serialNumber, User user) {
        return diaryRepository.findByIdAndUserAndDeviceId(id, user, serialNumber)
                .orElseThrow(() -> new IllegalArgumentException("해당 기기의 일지를 찾을 수 없습니다."));
    }

}
