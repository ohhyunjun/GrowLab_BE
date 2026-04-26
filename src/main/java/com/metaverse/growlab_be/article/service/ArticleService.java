package com.metaverse.growlab_be.article.service;

import com.metaverse.growlab_be.article.domain.Article;
import com.metaverse.growlab_be.article.dto.ArticleRequestDto;
import com.metaverse.growlab_be.article.dto.ArticleResponseDto;
import com.metaverse.growlab_be.article.repository.ArticleRepository;
import com.metaverse.growlab_be.auth.domain.PrincipalDetails;
import com.metaverse.growlab_be.auth.domain.User;
import com.metaverse.growlab_be.comment.domain.Comment;
import com.metaverse.growlab_be.comment.dto.CommentResponseDto;
import com.metaverse.growlab_be.file.service.FileService;
import com.metaverse.growlab_be.likes.articleLike.domain.ArticleLike;
import com.metaverse.growlab_be.likes.articleLike.repository.ArticleLikeRepository;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
            String uploadedUrl = fileService.uploadFile(savedArticle, file);
            savedArticle.setImageUrl(uploadedUrl);
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
    @Transactional
    public ArticleResponseDto getArticleById(Long articleId, PrincipalDetails principalDetails,
                                             HttpServletRequest request, HttpServletResponse response) {
        Article foundArticle = getValidArticleById(articleId);

        updateViewCountWithCookie(foundArticle, request, response);

        User logginedUser = (principalDetails != null) ? principalDetails.user() : null;

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
    public ArticleResponseDto updateArticle(Long articleId, ArticleRequestDto articleRequestDto,
                                            PrincipalDetails principalDetails, MultipartFile file) {
        Article foundArticle = getValidArticleById(articleId);
        //  작성자 본인 확인 (수정 권한 체크 추가)
        if (!foundArticle.getUser().getId().equals(principalDetails.user().getId())) {
            throw new IllegalArgumentException("본인의 게시글만 수정할 수 있습니다.");
        }
        // 텍스트 내용 업데이트
        foundArticle.update(articleRequestDto);

        // 이미지 업데이트
        if (file != null && !file.isEmpty()) {
            // 새로운 파일이 들어온 경우에만 업로드 후 URL 세팅
            String uploadedUrl = fileService.uploadFile(foundArticle, file);
            foundArticle.setImageUrl(uploadedUrl);
        }

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

    // 조회수 중복 방지를 위한 쿠키 처리 로직 (Service 내부에서만 사용)
    private void updateViewCountWithCookie(Article article, HttpServletRequest request, HttpServletResponse response) {
        Cookie[] cookies = request.getCookies();
        Cookie oldCookie = null;
        String articleIdTag = "[" + article.getId() + "]";

        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (cookie.getName().equals("growLabPostView")) {
                    oldCookie = cookie;
                    break;
                }
            }
        }

        if (oldCookie != null) {
            // 쿠키가 이미 있을 때: 해당 게시글 ID가 포함 안 되어 있으면 +1
            if (!oldCookie.getValue().contains(articleIdTag)) {
                article.setViewCount(article.getViewCount() + 1);
                oldCookie.setValue(oldCookie.getValue() + articleIdTag);
                oldCookie.setPath("/");
                oldCookie.setMaxAge(60 * 60 * 24);
                response.addCookie(oldCookie);
            }
        } else {
            // 쿠키가 아예 없을 때 (첫 방문)
            article.setViewCount(article.getViewCount() + 1);
            Cookie newCookie = new Cookie("growLabPostView", articleIdTag);
            newCookie.setPath("/");
            newCookie.setHttpOnly(true); // 보안 강화
            newCookie.setMaxAge(60 * 60 * 24);
            response.addCookie(newCookie);

        }
    public Page<ArticleResponseDto> getMyArticle(PrincipalDetails principalDetails, Pageable pageable) {
        User user = principalDetails.getUser();
        return articleRepository.findAllByUserOrderByCreatedAtDesc(user, pageable)
                .map(ArticleResponseDto::new);
    }
}
