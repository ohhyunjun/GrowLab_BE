package com.metaverse.growlab_be.article.service;

import com.metaverse.growlab_be.article.domain.Article;
import com.metaverse.growlab_be.article.dto.ArticleResponseDto;
import com.metaverse.growlab_be.article.repository.ArticleRepository;
import com.metaverse.growlab_be.auth.domain.PrincipalDetails;
import com.metaverse.growlab_be.auth.domain.User;
import com.metaverse.growlab_be.file.service.FileService;
import com.metaverse.growlab_be.likes.articleLike.repository.ArticleLikeRepository;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ArticleViewCountTest {
    private final ArticleRepository articles = mock(ArticleRepository.class);
    private final ArticleService service = new ArticleService(articles,
            mock(ArticleLikeRepository.class), mock(FileService.class));
    private final Map<String, Cookie> browserCookies = new LinkedHashMap<>();
    private Article article;
    private boolean secureRequest;
    private String forwardedProto;

    @BeforeEach
    void setUp() {
        article = new Article();
        article.setId(10L);
        article.setUser(viewer(1L).user());
        when(articles.findById(10L)).thenReturn(Optional.of(article));
    }

    @Test
    void switchingFromAuthorToAnotherAccountCountsTheOtherReader() {
        assertThat(read(10L, viewer(1L)).getViewCount()).isEqualTo(1);
        assertThat(read(10L, viewer(2L)).getViewCount()).isEqualTo(2);
    }

    @Test
    void repeatedReadsFromTheSameAccountAreNotCountedAgain() {
        assertThat(read(10L, viewer(2L)).getViewCount()).isEqualTo(1);
        assertThat(read(10L, viewer(2L)).getViewCount()).isEqualTo(1);
    }

    @Test
    void returningToAPreviouslyUsedAccountKeepsItsOwnReadHistory() {
        read(10L, viewer(2L));
        read(10L, viewer(3L));

        assertThat(read(10L, viewer(2L)).getViewCount()).isEqualTo(2);
    }

    @Test
    void legacyAnonymousCookieDoesNotSuppressALoggedInReadersFirstView() {
        browserCookies.put("growLabPostView", new Cookie("growLabPostView", "[10]"));

        assertThat(read(10L, viewer(2L)).getViewCount()).isEqualTo(1);
        assertThat(browserCookies.get("growLabPostView").getValue()).isEqualTo("[10]");
    }

    @Test
    void anonymousReadersKeepBrowserBasedDuplicatePrevention() {
        assertThat(read(10L, null).getViewCount()).isEqualTo(1);
        assertThat(read(10L, null).getViewCount()).isEqualTo(1);
    }

    @Test
    void anotherArticleCountsOnceAndKeepsCookieAttributes() {
        Article secondArticle = new Article();
        secondArticle.setId(11L);
        secondArticle.setUser(article.getUser());
        when(articles.findById(11L)).thenReturn(Optional.of(secondArticle));

        read(10L, viewer(2L));
        assertThat(read(11L, viewer(2L)).getViewCount()).isEqualTo(1);
        assertThat(read(11L, viewer(2L)).getViewCount()).isEqualTo(1);
        Cookie cookie = browserCookies.get("growLabPostView_u2");
        assertThat(cookie).isNotNull();
        assertThat(cookie.getValue()).isEqualTo("[10][11]");
        assertThat(cookie.getPath()).isEqualTo("/");
        assertThat(cookie.getMaxAge()).isEqualTo(24 * 60 * 60);
        assertThat(cookie.isHttpOnly()).isTrue();
        assertThat(cookie.getSecure()).isFalse();
        assertThat(cookie.getAttribute("SameSite")).isEqualTo("Lax");
    }

    @Test
    void httpsKeepsBothNewAndUpdatedCookiesAvailableToTheAwsFrontend() {
        secureRequest = true;
        Article second = new Article();
        second.setId(11L);
        when(articles.findById(11L)).thenReturn(Optional.of(second));
        read(10L, viewer(2L));
        assertSecureCookie();
        read(11L, viewer(2L));
        assertSecureCookie();
    }

    @ParameterizedTest
    @ValueSource(strings = {"https", "https, http"})
    void awsProxyHttpsHeaderEnablesCrossSiteCookie(String protocol) {
        forwardedProto = protocol;
        read(10L, viewer(2L));
        assertSecureCookie();
    }

    private void assertSecureCookie() {
        Cookie cookie = browserCookies.get("growLabPostView_u2");
        assertThat(cookie.getSecure()).isTrue();
        assertThat(cookie.isHttpOnly()).isTrue();
        assertThat(cookie.getAttribute("SameSite")).isEqualTo("None");
    }

    private ArticleResponseDto read(Long articleId, PrincipalDetails viewer) {
        var request = new MockHttpServletRequest();
        request.setSecure(secureRequest);
        if (forwardedProto != null) request.addHeader("X-Forwarded-Proto", forwardedProto);
        if (!browserCookies.isEmpty()) {
            // Incoming Cookie headers do not carry Path, Max-Age or HttpOnly attributes.
            request.setCookies(browserCookies.values().stream()
                    .map(cookie -> new Cookie(cookie.getName(), cookie.getValue()))
                    .toArray(Cookie[]::new));
        }
        var response = new MockHttpServletResponse();
        var result = service.getArticleById(articleId, viewer, request, response);
        for (Cookie cookie : response.getCookies()) {
            browserCookies.put(cookie.getName(), cookie);
        }
        return result;
    }

    private PrincipalDetails viewer(Long id) {
        User user = new User();
        user.setId(id);
        user.setUsername("user" + id);
        return new PrincipalDetails(user);
    }
}
