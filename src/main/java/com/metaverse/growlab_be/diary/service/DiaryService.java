package com.metaverse.growlab_be.diary.service;

import com.metaverse.growlab_be.auth.domain.User;
import com.metaverse.growlab_be.auth.repository.UserRepository;
import com.metaverse.growlab_be.diary.domain.Diary;
import com.metaverse.growlab_be.diary.dto.DiaryRequestDto;
import com.metaverse.growlab_be.diary.dto.DiaryResponseDto;
import com.metaverse.growlab_be.diary.repository.DiaryRepository;
import com.metaverse.growlab_be.file.service.FileService;
import com.metaverse.growlab_be.plant.domain.Plant;
import com.metaverse.growlab_be.plant.repository.PlantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DiaryService {

    private final DiaryRepository diaryRepository;
    private final PlantRepository plantRepository;
    private final UserRepository userRepository;
    private final FileService fileService;

    // Diary 생성
    @Transactional
    public DiaryResponseDto createDiary(DiaryRequestDto diaryRequestDto, Long plantId, Long userId, MultipartFile file) {
        Plant foundPlant = getValidPlantById(plantId);
        User foundUser = getValidUserById(userId);

        Diary newDiary = new Diary(diaryRequestDto, foundPlant, foundUser);
        Diary savedDiary = diaryRepository.save(newDiary);

        if (file != null && !file.isEmpty()) {
            String imageUrl = fileService.uploadFile(savedDiary, file);
            savedDiary.setImageUrl(imageUrl);
        }

        return new DiaryResponseDto(savedDiary);
    }

    // 전체 Diary 조회 (유저가 작성한 모든 식물의 일기)
    @Transactional(readOnly = true)
    public List<DiaryResponseDto> getDiaries(Long plantId, Long userId) {
        // 식물 존재 여부 확인
        Plant foundPlant = getValidPlantById(plantId);

        // 다이어리를 작성하려는 유저 정보 조회
        User foundUser = getValidUserById(userId);

        // TODO: [보안]: 해당 식물이 현재 로그인한 유저의 소유인지 확인, Device 엔티티 추가 후 활성화
        // validatePlantOwner(foundPlant, foundUser);

        List<DiaryResponseDto> diaryResponseDtoList = diaryRepository.findAllByUserOrderByCreatedAtDesc(foundUser).stream()
                .map(DiaryResponseDto::new).toList();

        return diaryResponseDtoList;
    }

    // 특정 식물의 Diary 조회
    @Transactional(readOnly = true)
    public List<DiaryResponseDto> getDiariesByPlantId(Long plantId, Long userId) {
        // 식물 존재 여부 확인
        Plant foundPlant = getValidPlantById(plantId);

        // 다이어리를 작성하려는 유저 정보 조회
        User foundUser = getValidUserById(userId);

        // TODO: [보안]: Device 엔티티 추가 후 활성화
        // validatePlantOwner(foundPlant, foundUser);

        // 식물 존재 여부 확인
        getValidPlantById(plantId);

        return diaryRepository.findByUserAndPlantIdOrderByCreatedAtDesc(foundUser, plantId).stream()
                .map(DiaryResponseDto::new)
                .toList();
    }

    // 특정 Diary 상세 조회
    @Transactional(readOnly = true)
    public DiaryResponseDto getDiaryById(Long id, Long plantId, Long userId) {
        // 식물 존재 여부 확인
        Plant foundPlant = getValidPlantById(plantId);
        // 다이어리를 작성하려는 유저 정보 조회
        User foundUser = getValidUserById(userId);

        // TODO: [보안]: 해당 식물이 현재 로그인한 유저의 소유인지 확인, Device 엔티티 추가 후 활성화
        // validatePlantOwner(foundPlant, foundUser);

        // Diary 존재 여부 및 해당 식물에 속한 Diary인지 확인
        Diary foundDiary = getValidPlantAndDiaryById(id, plantId);

        DiaryResponseDto diaryResponseDto = new DiaryResponseDto(foundDiary);
        return diaryResponseDto;
    }

    // 특정 날짜의 Diary 조회
    @Transactional(readOnly = true)
    public List<DiaryResponseDto> getDiariesByDate(Long userId, LocalDate date) {
        User foundUser = getValidUserById(userId);

        List<DiaryResponseDto> diaryResponseDtoList = diaryRepository.findByUserAndTargetDate(foundUser, date).stream()
                .map(DiaryResponseDto::new).toList();

        return diaryResponseDtoList;
    }

    // Diary 수정
    @Transactional
    public DiaryResponseDto updateDiary(Long id, DiaryRequestDto diaryRequestDto, Long plantId, Long userId, MultipartFile file) {
        Plant foundPlant = getValidPlantById(plantId);
        User foundUser = getValidUserById(userId);

        Diary foundDiary = diaryRepository.findByIdAndUser(id, foundUser)
                .orElseThrow(() -> new IllegalArgumentException("본인이 작성한 다이어리만 접근 가능합니다."));

        if (!foundDiary.getPlant().getId().equals(plantId)) {
            throw new IllegalArgumentException("해당 식물에 속한 다이어리가 아닙니다.");
        }

        foundDiary.update(diaryRequestDto);

        if (file != null && !file.isEmpty()) {
            String imageUrl = fileService.uploadFile(foundDiary, file);
            foundDiary.setImageUrl(imageUrl);
        }

        return new DiaryResponseDto(foundDiary);
    }

    // Diary 삭제
    @Transactional
    public void deleteDiary(Long id, Long plantId, Long userId) {
        // 식물 존재 여부 확인
        Plant foundPlant = getValidPlantById(plantId);
        // 다이어리를 작성하려는 유저 정보 조회
        User foundUser = getValidUserById(userId);

        // TODO: [보안]: 해당 식물이 현재 로그인한 유저의 소유인지 확인, Device 엔티티 추가 후 활성화
        // validatePlantOwner(foundPlant, foundUser);

        // [보안]: 해당 Diary가 현재 로그인한 유저가 작성한 것인지 확인
        Diary foundDiary = diaryRepository.findByIdAndUser(id, foundUser)
                .orElseThrow(() -> new IllegalArgumentException("본인이 작성한 다이어리만 삭제 가능합니다."));

        // [추가 보안]: 해당 Diary가 현재 조회하려는 식물에 속한 것인지 확인
        if (!foundDiary.getPlant().getId().equals(plantId)) {
            throw new IllegalArgumentException("해당 식물에 속한 다이어리가 아닙니다.");
        }

        diaryRepository.delete(foundDiary);
    }

    // [공통 검증 메서드]

    // 유저 존재 여부 검증
    public User getValidUserById(Long userId) {
        return userRepository.findById(userId).orElseThrow(() ->
                new IllegalArgumentException("존재하지 않는 사용자입니다. (ID: " + userId + ")"));
    }
    // 식물 존재 여부 검증
    public Plant getValidPlantById(Long id) {
        return plantRepository.findById(id).orElseThrow(() ->
                new IllegalArgumentException("존재하지 않는 식물입니다. (ID: " + id + ")"));
    }
    // 다이어리 존재 여부 및 해당 식물에 속한 다이어리인지 검증
    public Diary getValidPlantAndDiaryById(Long id, Long plantId) {
        return diaryRepository.findByIdAndPlantId(id, plantId).orElseThrow(() ->
                new IllegalArgumentException("해당 식물에서 다이어리를 찾을 수 없습니다. (ID: " + id + ")"));
    }
    // 다이어리 작성자 검증 (추가 보안)
    private void validateDiaryOwner(Diary diary, User user) {
        if (!diary.getUser().getId().equals(user.getId())) {
            throw new IllegalArgumentException("해당 다이어리에 대한 수정/삭제 권한이 없습니다.");
        }
    }
    // 식물 소유자 검증 (추가 보안, Device 엔티티 추가 후 활성화)
    private void validatePlantOwner(Plant plant, User user) {
        /* TODO: Device 필드 추가 후 활성화
        if (!plant.getDevice().getUser().getId().equals(user.getId())) {
            throw new IllegalArgumentException("해당 작업에 대한 접근 권한이 없습니다.");
        }
        */
    }
}
