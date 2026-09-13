package ru.yandex.practicum.filmorate;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.context.annotation.Import;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
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
        ReviewService.class,
        ReviewDbStorage.class,
        ReviewReactionDbStorage.class,
        FilmDbStorage.class,
        UserDbStorage.class,
        EventDbStorage.class
})
@RequiredArgsConstructor(onConstructor_ = @Autowired)
class ReviewServiceTest {
    private static final LocalDate USER_BIRTHDAY = LocalDate.of(1990, 1, 1);
    private static final LocalDate FILM_RELEASE_DATE = LocalDate.of(2000, 1, 1);
    private static final Long NON_EXISTENT_FILM_ID = 999L;

    private final ReviewService reviewService;
    private final UserDbStorage userStorage;
    private final FilmDbStorage filmStorage;

    @Test
    void shouldCreateAndFindReview() {
        User author = userStorage.add(createUser("reviewer"));
        Film film = filmStorage.add(createFilm("Фильм с отзывами"));
        Review review = createReview(author.getId(), film.getId(), "Первоначальный отзыв", true);

        Review createdReview = reviewService.create(review);

        assertThat(createdReview.getReviewId()).isPositive();
        assertThat(createdReview.getUseful()).isZero();
        assertThat(reviewService.findById(createdReview.getReviewId()))
                .isEqualTo(createdReview);
    }

    @Test
    void shouldNotCreateReviewWhenFilmDoesNotExist() {
        User author = userStorage.add(createUser("reviewer"));
        Review review = createReview(author.getId(), NON_EXISTENT_FILM_ID, "Первоначальный отзыв", true);

        assertThatThrownBy(() -> reviewService.create(review))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void shouldPartiallyUpdateReview() {
        User author = userStorage.add(createUser("reviewer"));
        Film film = filmStorage.add(createFilm("Фильм с отзывами"));
        Review createdReview = reviewService.create(
                createReview(author.getId(), film.getId(), "Первоначальный отзыв", true)
        );
        Review patch = Review.builder()
                .reviewId(createdReview.getReviewId())
                .content("Обновлённый отзыв")
                .build();

        Review updatedReview = reviewService.update(patch);

        assertThat(updatedReview.getContent()).isEqualTo("Обновлённый отзыв");
        assertThat(updatedReview.getIsPositive()).isTrue();
        assertThat(updatedReview.getUserId()).isEqualTo(author.getId());
        assertThat(updatedReview.getFilmId()).isEqualTo(film.getId());
    }

    @Test
    void shouldRejectBlankContentDuringUpdate() {
        User author = userStorage.add(createUser("reviewer"));
        Film film = filmStorage.add(createFilm("Фильм с отзывами"));
        Review createdReview = reviewService.create(
                createReview(author.getId(), film.getId(), "Первоначальный отзыв", true)
        );
        Review patch = Review.builder()
                .reviewId(createdReview.getReviewId())
                .content("   ")
                .build();

        assertThatThrownBy(() -> reviewService.update(patch))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void shouldRejectTooLongContentDuringUpdate() {
        User author = userStorage.add(createUser("reviewer"));
        Film film = filmStorage.add(createFilm("Фильм с отзывами"));
        Review createdReview = reviewService.create(
                createReview(author.getId(), film.getId(), "Первоначальный отзыв", true)
        );
        Review patch = Review.builder()
                .reviewId(createdReview.getReviewId())
                .content("а".repeat(10_001))
                .build();

        assertThatThrownBy(() -> reviewService.update(patch))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void shouldFindReviewsForSpecifiedFilm() {
        User author = userStorage.add(createUser("reviewer"));
        Film firstFilm = filmStorage.add(createFilm("Первый фильм"));
        Film secondFilm = filmStorage.add(createFilm("Второй фильм"));
        Review firstReview = reviewService.create(
                createReview(author.getId(), firstFilm.getId(), "Отзыв к первому фильму", true)
        );
        reviewService.create(
                createReview(author.getId(), secondFilm.getId(), "Отзыв ко второму фильму", false)
        );

        assertThat(reviewService.findMostUseful(firstFilm.getId(), 10))
                .extracting(Review::getReviewId)
                .containsExactly(firstReview.getReviewId());
    }

    @Test
    void shouldAddAndDeleteLikesAndDislikes() {
        User author = userStorage.add(createUser("reviewer"));
        User reactor = userStorage.add(createUser("reactor"));
        Film film = filmStorage.add(createFilm("Фильм с отзывами"));
        Review review = reviewService.create(
                createReview(author.getId(), film.getId(), "Отзыв для оценки", true)
        );

        reviewService.saveReaction(review.getReviewId(), reactor.getId(), true);
        assertThat(reviewService.findById(review.getReviewId()).getUseful()).isEqualTo(1);

        reviewService.deleteReaction(review.getReviewId(), reactor.getId(), true);
        assertThat(reviewService.findById(review.getReviewId()).getUseful()).isZero();

        reviewService.saveReaction(review.getReviewId(), reactor.getId(), false);
        assertThat(reviewService.findById(review.getReviewId()).getUseful()).isEqualTo(-1);

        reviewService.deleteReaction(review.getReviewId(), reactor.getId(), false);
        assertThat(reviewService.findById(review.getReviewId()).getUseful()).isZero();
    }

    @Test
    void shouldDeleteReview() {
        User author = userStorage.add(createUser("reviewer"));
        Film film = filmStorage.add(createFilm("Фильм с отзывами"));
        Review review = reviewService.create(
                createReview(author.getId(), film.getId(), "Отзыв для удаления", true)
        );

        reviewService.delete(review.getReviewId());

        assertThatThrownBy(() -> reviewService.findById(review.getReviewId()))
                .isInstanceOf(NotFoundException.class);
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
