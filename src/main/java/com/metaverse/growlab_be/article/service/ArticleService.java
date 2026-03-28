package com.metaverse.growlab_be.article.service;

import com.metaverse.growlab_be.article.domain.Article;
import com.metaverse.growlab_be.article.dto.ArticleRequestDto;
import com.metaverse.growlab_be.article.dto.ArticleResponseDto;
import com.metaverse.growlab_be.article.repository.ArticleRepository;
import com.metaverse.growlab_be.auth.domain.PrincipalDetails;
import com.metaverse.growlab_be.comment.domain.Comment;
import com.metaverse.growlab_be.comment.dto.CommentResponseDto;
import com.metaverse.growlab_be.file.service.FileService;
import com.metaverse.growlab_be.likes.articleLike.domain.ArticleLike;
import com.metaverse.growlab_be.likes.articleLike.repository.ArticleLikeRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import com.metaverse.growlab_be.auth.domain.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ArticleService {
    private final ArticleRepository articleRepository;
    private final ArticleLikeRepository articleLikeRepository;
    private final FileService fileService;

    @Transactional
    public ArticleResponseDto createArticle(ArticleRequestDto articleRequestDto, PrincipalDetails principalDetails, MultipartFile file) {
        User logginedUser = principalDetails.user();
        Article newArticle = new Article(articleRequestDto, logginedUser);
        Article savedArticle = articleRepository.save(newArticle);

        if (file != null && !file.isEmpty()) {
            fileService.uploadFile(savedArticle, file);
        }

        ArticleResponseDto articleResponseDto = new ArticleResponseDto(savedArticle);
        return articleResponseDto;
    }

    public Page<ArticleResponseDto> getArticles(Pageable pageable, PrincipalDetails principalDetails) {
        User logginedUser = principalDetails != null ? principalDetails.user() : null;
        return articleRepository.findAllByOrderByCreatedAtDesc(pageable).map(article -> {
            int likesCount = (int) articleLikeRepository.countByArticle(article);
            boolean liked = logginedUser != null && articleLikeRepository.findByArticleAndUser(article, logginedUser).isPresent();
            return new ArticleResponseDto(article, likesCount, liked);
        });
    }

    //인용쌤 코드에서 gpt한테 댓글 좋아요만 빼고 게시글 좋아요만 남겨달라고 해서 만듦
    @Transactional(readOnly = true)
    public ArticleResponseDto getArticleById(Long articleId, PrincipalDetails principalDetails) {
        Article foundArticle = getValidArticleById(articleId);
        User logginedUser = principalDetails.user();

        int currentLikesCount = (int) articleLikeRepository.countByArticle(foundArticle);
        boolean liked = false;
        if (logginedUser != null) {
            liked = articleLikeRepository.findByArticleAndUser(foundArticle, logginedUser).isPresent();
        }

        List<Comment> comments = foundArticle.getComments();
        List<CommentResponseDto> commentResponseDtos = new ArrayList<>();

        for (Comment comment : comments) {
            commentResponseDtos.add(new CommentResponseDto(comment));
        }

        ArticleResponseDto articleResponseDto = new ArticleResponseDto(foundArticle, currentLikesCount, liked, commentResponseDtos);
        return articleResponseDto;
    }

    @Transactional
    public ArticleResponseDto updateArticle(Long articleId, ArticleRequestDto articleRequestDto) {
        Article foundArticle = getValidArticleById(articleId);
        foundArticle.update(articleRequestDto);
        return new ArticleResponseDto(foundArticle);
    }

    @Transactional
    public void deleteArticle(Long articleId) {
        Article foundArticle = getValidArticleById(articleId);
        articleRepository.delete(foundArticle);
    }

    public Article getValidArticleById(Long articleId) {
        return articleRepository.findById(articleId).orElseThrow(() ->
                new IllegalArgumentException("선택한 id의 게시글은 존재하지 않습니다."));
    }

    @Transactional(readOnly = true)
    public List<ArticleResponseDto> getLikedArticles(Pageable pageable, PrincipalDetails principalDetails) {
        User logginedUser = principalDetails.user();
        Page<ArticleLike> articleLikes = articleLikeRepository.findByUser(logginedUser, pageable);

        List<ArticleResponseDto> articleResponseDtos = articleLikes.stream()
                .map(articleLike -> {
                    Article article = articleLike.getArticle();
                    int likesCount = (int) articleLikeRepository.countByArticle(article);
                    return new ArticleResponseDto(article, likesCount, true);
                })
                .toList();

        return articleResponseDtos;
    }
}
