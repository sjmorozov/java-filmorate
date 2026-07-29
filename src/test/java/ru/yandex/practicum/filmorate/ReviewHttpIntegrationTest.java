package ru.yandex.practicum.filmorate;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.Review;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.service.ReviewService;
import ru.yandex.practicum.filmorate.storage.film.FilmStorage;
import ru.yandex.practicum.filmorate.storage.user.UserStorage;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "spring.datasource.url=jdbc:h2:mem:review-http-test;DB_CLOSE_DELAY=-1",
                "spring.sql.init.mode=always"
        }
)
class ReviewHttpIntegrationTest {
    private static final LocalDate FILM_RELEASE_DATE = LocalDate.of(2000, 1, 1);
    private static final LocalDate USER_BIRTHDAY = LocalDate.of(1990, 1, 1);

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private FilmStorage filmStorage;

    @Autowired
    private UserStorage userStorage;

    @Autowired
    private ReviewService reviewService;

    @Test
    void shouldReturnNotFoundWhenReviewUserDoesNotExist() {
        Film film = filmStorage.add(createFilm("Фильм для проверки пользователя"));
        Review review = createReview(-1L, film.getId());

        ResponseEntity<String> response = restTemplate.postForEntity("/reviews", review, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void shouldReturnNotFoundWhenReviewFilmDoesNotExist() {
        Review review = createReview(1L, -1L);

        ResponseEntity<String> response = restTemplate.postForEntity("/reviews", review, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void shouldReturnOkWhenDeletingReviewReactions() {
        User author = userStorage.add(createUser("review-author"));
        User reactor = userStorage.add(createUser("review-reactor"));
        Film film = filmStorage.add(createFilm("Фильм для проверки реакций"));
        Review review = reviewService.create(createReview(author.getId(), film.getId()));

        reviewService.addLike(review.getReviewId(), reactor.getId());
        ResponseEntity<Void> likeResponse = deleteReaction(review.getReviewId(), "like", reactor.getId());

        assertThat(likeResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(reviewService.findById(review.getReviewId()).getUseful()).isZero();

        reviewService.addDislike(review.getReviewId(), reactor.getId());
        ResponseEntity<Void> dislikeResponse = deleteReaction(review.getReviewId(), "dislike", reactor.getId());

        assertThat(dislikeResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(reviewService.findById(review.getReviewId()).getUseful()).isZero();
    }

    private ResponseEntity<Void> deleteReaction(Long reviewId, String reaction, Long userId) {
        return restTemplate.exchange(
                "/reviews/{reviewId}/{reaction}/{userId}",
                HttpMethod.DELETE,
                HttpEntity.EMPTY,
                Void.class,
                reviewId,
                reaction,
                userId
        );
    }

    private Review createReview(Long userId, Long filmId) {
        return Review.builder()
                .content("Содержательный отзыв")
                .isPositive(true)
                .userId(userId)
                .filmId(filmId)
                .build();
    }

    private Film createFilm(String name) {
        return Film.builder()
                .name(name)
                .description("Описание фильма")
                .releaseDate(FILM_RELEASE_DATE)
                .duration(120)
                .build();
    }

    private User createUser(String login) {
        return User.builder()
                .email(login + "@example.com")
                .login(login)
                .name("Пользователь " + login)
                .birthday(USER_BIRTHDAY)
                .build();
    }
}
