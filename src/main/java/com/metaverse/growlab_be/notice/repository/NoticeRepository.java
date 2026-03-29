package com.metaverse.growlab_be.notice.repository;

import com.metaverse.growlab_be.notice.domain.Notice;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NoticeRepository extends JpaRepository<Notice, Long> {
}
