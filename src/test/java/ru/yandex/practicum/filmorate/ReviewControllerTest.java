package ru.yandex.practicum.filmorate;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.context.annotation.Import;
import ru.yandex.practicum.filmorate.controller.ReviewController;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.Review;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.service.ReviewService;
import ru.yandex.practicum.filmorate.storage.event.EventDbStorage;
import ru.yandex.practicum.filmorate.storage.film.FilmDbStorage;
import ru.yandex.practicum.filmorate.storage.review.ReviewDbStorage;
import ru.yandex.practicum.filmorate.storage.reviewreaction.ReviewReactionDbStorage;
import ru.yandex.practicum.filmorate.storage.user.UserDbStorage;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@JdbcTest
@AutoConfigureTestDatabase
@Import({
        ReviewController.class,
        ReviewService.class,
        ReviewDbStorage.class,
        ReviewReactionDbStorage.class,
        FilmDbStorage.class,
        UserDbStorage.class,
        EventDbStorage.class
})
@RequiredArgsConstructor(onConstructor_ = @Autowired)
class ReviewControllerTest {
    private static final LocalDate USER_BIRTHDAY = LocalDate.of(1990, 1, 1);
    private static final LocalDate FILM_RELEASE_DATE = LocalDate.of(2000, 1, 1);

    private final ReviewController reviewController;
    private final UserDbStorage userStorage;
    private final FilmDbStorage filmStorage;

    @Test
    void shouldCreateUpdateFindAndDeleteReview() {
        User author = userStorage.add(createUser("reviewer"));
        Film film = filmStorage.add(createFilm("Фильм с отзывами"));
        Review review = createReview(author.getId(), film.getId(), "Первоначальный отзыв", true);

        Review createdReview = reviewController.create(review);

        assertThat(createdReview.getReviewId()).isPositive();
        assertThat(reviewController.findById(createdReview.getReviewId()))
                .isEqualTo(createdReview);

        Review patch = Review.builder()
                .reviewId(createdReview.getReviewId())
                .content("Обновлённый отзыв")
                .isPositive(false)
                .build();
        Review updatedReview = reviewController.update(patch);

        assertThat(updatedReview.getContent()).isEqualTo("Обновлённый отзыв");
        assertThat(updatedReview.getIsPositive()).isFalse();

        reviewController.delete(createdReview.getReviewId());

        assertThatThrownBy(() -> reviewController.findById(createdReview.getReviewId()))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void shouldFindReviewsByFilmAndCount() {
        User author = userStorage.add(createUser("reviewer"));
        User reactor = userStorage.add(createUser("reactor"));
        Film firstFilm = filmStorage.add(createFilm("Первый фильм"));
        Film secondFilm = filmStorage.add(createFilm("Второй фильм"));
        Review usefulReview = reviewController.create(
                createReview(author.getId(), firstFilm.getId(), "Полезный отзыв", true)
        );
        reviewController.create(
                createReview(author.getId(), firstFilm.getId(), "Обычный отзыв", false)
        );
        reviewController.create(
                createReview(author.getId(), secondFilm.getId(), "Отзыв к другому фильму", true)
        );
        reviewController.addLike(usefulReview.getReviewId(), reactor.getId());

        assertThat(reviewController.getUseful(firstFilm.getId(), 1))
                .extracting(Review::getReviewId)
                .containsExactly(usefulReview.getReviewId());
        assertThat(reviewController.getUseful(null, 10))
                .hasSize(3);
    }

    @Test
    void shouldAddAndDeleteLikesAndDislikes() {
        User author = userStorage.add(createUser("reviewer"));
        User firstReactor = userStorage.add(createUser("first-reactor"));
        User secondReactor = userStorage.add(createUser("second-reactor"));
        Film film = filmStorage.add(createFilm("Фильм с отзывами"));
        Review review = reviewController.create(
                createReview(author.getId(), film.getId(), "Отзыв для оценки", true)
        );

        reviewController.addLike(review.getReviewId(), firstReactor.getId());
        reviewController.addDislike(review.getReviewId(), secondReactor.getId());
        assertThat(reviewController.findById(review.getReviewId()).getUseful()).isZero();

        reviewController.deleteDislike(review.getReviewId(), secondReactor.getId());
        assertThat(reviewController.findById(review.getReviewId()).getUseful()).isEqualTo(1);

        reviewController.deleteLike(review.getReviewId(), firstReactor.getId());
        assertThat(reviewController.findById(review.getReviewId()).getUseful()).isZero();
    }

    private User createUser(String login) {
        return User.builder()
                .email(login + "@example.com")
                .login(login)
                .name("Пользователь " + login)
                .birthday(USER_BIRTHDAY)
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

    private Review createReview(Long userId, Long filmId, String content, boolean isPositive) {
        return Review.builder()
                .content(content)
                .isPositive(isPositive)
                .userId(userId)
                .filmId(filmId)
                .build();
    }
}
