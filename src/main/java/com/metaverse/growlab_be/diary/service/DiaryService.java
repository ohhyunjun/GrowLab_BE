package com.metaverse.growlab_be.diary.service;

import com.metaverse.growlab_be.diary.domain.Diary;
import com.metaverse.growlab_be.diary.dto.DiaryRequestDto;
import com.metaverse.growlab_be.diary.dto.DiaryResponseDto;
import com.metaverse.growlab_be.diary.repository.DiaryRepository;
import com.metaverse.growlab_be.plant.domain.Plant;
import com.metaverse.growlab_be.plant.repository.PlantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DiaryService {

    private final DiaryRepository diaryRepository;
    private final PlantRepository plantRepository;

    // Diary 생성
    @Transactional
    public DiaryResponseDto createDiary(DiaryRequestDto diaryRequestDto, Long plantId) {
        // 식물 존재 여부 확인
        Plant foundPlant = getValidPlantById(plantId);

        // RequestDto -> Entity 변환
        Diary newDiary = new Diary(diaryRequestDto, foundPlant);
        Diary savedDiary = diaryRepository.save(newDiary);

        // Entity -> ResponseDto 변환
        DiaryResponseDto diaryResponseDto = new DiaryResponseDto(savedDiary);
        return diaryResponseDto;
    }

    // 전체 Diary 조회
    @Transactional(readOnly = true)
    public List<DiaryResponseDto> getDiaries(Long plantId) {
        List<DiaryResponseDto> diaryResponseDtoList = diaryRepository.findAllByPlantIdOrderByCreatedAtDesc(plantId).stream()
                .map(DiaryResponseDto::new).toList();
        return diaryResponseDtoList;
    }

    // 특정 식물의 특정 Diary 조회
    @Transactional(readOnly = true)
    public DiaryResponseDto getDiaryById(Long id, Long plantId) {
        Diary foundDiary = getValidPlantAndDiaryById(id, plantId);
        DiaryResponseDto diaryResponseDto = new DiaryResponseDto(foundDiary);
        return diaryResponseDto;
    }

    // Diary 수정
    @Transactional
    public DiaryResponseDto updateDiary(Long id, DiaryRequestDto diaryRequestDto, Long plantId) {
        Diary foundDiary = getValidPlantAndDiaryById(id, plantId);

        // Diary 엔티티의 update 메서드를 호출하여 수정
        foundDiary.update(diaryRequestDto);
        return new DiaryResponseDto(foundDiary);
    }

    // Diary 삭제
    @Transactional
    public void deleteDiary(Long id, Long plantId) {
        Diary foundDiary = getValidPlantAndDiaryById(id, plantId);
        diaryRepository.delete(foundDiary);
    }

    // [공통 검증 메서드] Diary와 Plant의 유효성 검증

    // 특정 식물에 속한 diary인지 검증
    public Diary getValidPlantAndDiaryById(Long id, Long plantId) {
        return diaryRepository.findByIdAndPlantId(id, plantId).orElseThrow(() ->
                new IllegalArgumentException("해당 식물에 속한 diary를 찾을 수 없습니다. (ID: " + id + ")"));
    }

    // 식물 존재 여부 검증
    public Plant getValidPlantById(Long id) {
        return plantRepository.findById(id).orElseThrow(() ->
                new IllegalArgumentException("존재하지 않는 식물입니다. (ID: " + id + ")"));
    }
}
