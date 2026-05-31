package com.metaverse.growlab_be.market_price.service;

import com.metaverse.growlab_be.market_price.domain.CropCode;
import com.metaverse.growlab_be.market_price.dto.CropCodeResponseDto;
import com.metaverse.growlab_be.market_price.repository.CropCodeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CropCodeService {

    private final CropCodeRepository cropCodeRepository;

    public List<CropCodeResponseDto> searchCrop(String name) {

        List<CropCode> cropCodes =
                cropCodeRepository.findByKindNameContaining(name);

        return cropCodes.stream()
                .map(CropCodeResponseDto::new)
                .toList();
    }
}
