package com.metaverse.growlab_be.notice.service;

import com.metaverse.growlab_be.notice.repository.NoticeRepository;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor

public class NoticeService {

    private final NoticeRepository noticeRepository;
}
