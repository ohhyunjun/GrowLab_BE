package com.metaverse.growlab_be.article.controller;

import com.metaverse.growlab_be.article.service.ArticleService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("api/articles")
@RequiredArgsConstructor
public class ArticleController {
    private final ArticleService articleService;
}
