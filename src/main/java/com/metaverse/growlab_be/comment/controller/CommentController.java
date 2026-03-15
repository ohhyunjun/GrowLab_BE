package com.metaverse.growlab_be.comment.controller;

import com.metaverse.growlab_be.comment.service.CommentService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("api/comments")
@RequiredArgsConstructor
public class CommentController {
    private final CommentService commentService;
}
