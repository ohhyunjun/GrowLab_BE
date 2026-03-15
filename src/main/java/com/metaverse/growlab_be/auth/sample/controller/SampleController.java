package com.metaverse.growlab_be.auth.sample.controller;

import com.metaverse.growlab_be.auth.sample.dto.SampleResponseDto;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("api/sample")
public class SampleController {
    @GetMapping("permitAll")
    public ResponseEntity<SampleResponseDto> getSampleWithoutPermission() {
        return ResponseEntity.ok(new SampleResponseDto("Login is not required."));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("adminOnly")
    public ResponseEntity<SampleResponseDto> getSampleWithAdmin() {
        return ResponseEntity.ok(new SampleResponseDto("You're an admin."));
    }
}
