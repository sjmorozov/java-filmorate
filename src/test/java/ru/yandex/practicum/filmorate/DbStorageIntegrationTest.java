package ru.yandex.practicum.filmorate;

import org.junit.jupiter.api.Test;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.context.annotation.Import;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.FriendRequest;
import ru.yandex.practicum.filmorate.model.Friendship;
import ru.yandex.practicum.filmorate.model.Genre;
import ru.yandex.practicum.filmorate.model.MpaRating;
import ru.yandex.practicum.filmorate.model.Review;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.storage.film.FilmDbStorage;
import ru.yandex.practicum.filmorate.storage.filmgenre.FilmGenreDbStorage;
import ru.yandex.practicum.filmorate.storage.filmlike.FilmLikeDbStorage;
import ru.yandex.practicum.filmorate.storage.friendrequest.FriendRequestDbStorage;
import ru.yandex.practicum.filmorate.storage.friendship.FriendshipDbStorage;
import ru.yandex.practicum.filmorate.storage.genre.GenreDbStorage;
import ru.yandex.practicum.filmorate.storage.mparating.MpaRatingDbStorage;
import ru.yandex.practicum.filmorate.storage.review.ReviewDbStorage;
import ru.yandex.practicum.filmorate.storage.reviewreaction.ReviewReactionDbStorage;
import ru.yandex.practicum.filmorate.storage.user.UserDbStorage;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@JdbcTest
@AutoConfigureTestDatabase
@Import({
        UserDbStorage.class,
        FilmDbStorage.class,
        GenreDbStorage.class,
        MpaRatingDbStorage.class,
        FilmGenreDbStorage.class,
        FilmLikeDbStorage.class,
        FriendRequestDbStorage.class,
        FriendshipDbStorage.class,
        ReviewDbStorage.class,
        ReviewReactionDbStorage.class
})
@RequiredArgsConstructor(onConstructor_ = @Autowired)
class DbStorageIntegrationTest {
    private static final LocalDate USER_BIRTHDAY = LocalDate.of(1990, 1, 1);
    private static final LocalDate FILM_RELEASE_DATE = LocalDate.of(2000, 1, 1);

    private final UserDbStorage userStorage;

    private final FilmDbStorage filmStorage;

    private final GenreDbStorage genreStorage;

    private final MpaRatingDbStorage mpaRatingStorage;

    private final FilmGenreDbStorage filmGenreStorage;

    private final FilmLikeDbStorage filmLikeStorage;

    private final FriendRequestDbStorage friendRequestStorage;

    private final FriendshipDbStorage friendshipStorage;

    private final ReviewDbStorage reviewStorage;

    private final ReviewReactionDbStorage reviewReactionStorage;

    @Test
    void userStorageShouldCreateUpdateFindAndDeleteUser() {
        User user = userStorage.add(createUser("neo"));

        assertThat(user.getId()).isPositive();
        assertThat(userStorage.findById(user.getId()))
                .hasFieldOrPropertyWithValue("login", "neo");
        assertThat(userStorage.findAll())
                .extracting(User::getId)
                .contains(user.getId());

        user.setName("Thomas Anderson");
        user.setEmail("anderson@zion.human");
        User updatedUser = userStorage.update(user);

        assertThat(updatedUser.getName()).isEqualTo("Thomas Anderson");
        assertThat(userStorage.findById(user.getId()).getEmail()).isEqualTo("anderson@zion.human");

        userStorage.delete(user.getId());

        assertThatThrownBy(() -> userStorage.findById(user.getId()))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void userStorageShouldDeleteRelatedFriendshipsAndRequestsByCascade() {
        User firstUser = userStorage.add(createUser("neo"));
        User secondUser = userStorage.add(createUser("morpheus"));
        User thirdUser = userStorage.add(createUser("trinity"));

        friendshipStorage.save(new Friendship(firstUser.getId(), secondUser.getId()));
        friendRequestStorage.save(new FriendRequest(secondUser.getId(), thirdUser.getId()));

        userStorage.delete(secondUser.getId());

        assertThat(friendshipStorage.existsByUserIds(firstUser.getId(), secondUser.getId()))
                .as("Подтверждённая дружба удалённого пользователя должна удалиться каскадом")
                .isFalse();
        assertThat(friendRequestStorage.existsByRequesterIdAndRecipientId(secondUser.getId(), thirdUser.getId()))
                .as("Заявка удалённого пользователя должна удалиться каскадом")
                .isFalse();
        assertThat(friendshipStorage.findFriendIdsByUserId(firstUser.getId()))
                .as("У оставшегося пользователя не должно остаться ссылки на удалённого друга")
                .isEmpty();
    }

    @Test
    void filmStorageShouldCreateUpdateFindAndDeleteFilm() {
        Film film = filmStorage.add(createFilm("Matrix", 136, 1));

        Film foundFilm = filmStorage.findById(film.getId());

        assertThat(foundFilm.getName()).isEqualTo("Matrix");
        assertThat(foundFilm.getMpa()).isEqualTo(new MpaRating(1, "G"));
        assertThat(filmStorage.findAll())
                .extracting(Film::getId)
                .contains(film.getId());

        foundFilm.setName("The Matrix");
        foundFilm.setDuration(137);
        foundFilm.setMpa(new MpaRating(2, null));
        filmStorage.update(foundFilm);

        Film updatedFilm = filmStorage.findById(film.getId());

        assertThat(updatedFilm.getName()).isEqualTo("The Matrix");
        assertThat(updatedFilm.getDuration()).isEqualTo(137);
        assertThat(updatedFilm.getMpa()).isEqualTo(new MpaRating(2, "PG"));

        filmStorage.delete(film.getId());

        assertThatThrownBy(() -> filmStorage.findById(film.getId()))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void filmStorageShouldFindFilmsByIds() {
        Film firstFilm = filmStorage.add(createFilm("First Film", 100, 1));
        Film secondFilm = filmStorage.add(createFilm("Second Film", 101, 2));

        assertThat(filmStorage.findByIds(List.of(firstFilm.getId(), secondFilm.getId())))
                .extracting(Film::getId)
                .containsExactlyInAnyOrder(firstFilm.getId(), secondFilm.getId());
    }

    @Test
    void genreStorageShouldFindGenresByIdAndFindAll() {
        Genre comedy = genreStorage.findById(1);

        assertThat(comedy).isEqualTo(new Genre(1, "Комедия"));
        assertThat(genreStorage.findAll())
                .extracting(Genre::getId)
                .containsExactly(1, 2, 3, 4, 5, 6);
    }

    @Test
    void genreStorageShouldFindGenresByIds() {
        assertThat(genreStorage.findByIds(Set.of(1, 3, 6)))
                .containsExactly(
                        new Genre(1, "Комедия"),
                        new Genre(3, "Мультфильм"),
                        new Genre(6, "Боевик")
                );
    }

    @Test
    void mpaRatingStorageShouldFindRatingsByIdAndFindAll() {
        MpaRating rating = mpaRatingStorage.findById(3);

        assertThat(rating).isEqualTo(new MpaRating(3, "PG-13"));
        assertThat(mpaRatingStorage.findAll())
                .extracting(MpaRating::getName)
                .containsExactly("G", "PG", "PG-13", "R", "NC-17");
    }

    @Test
    void filmGenreStorageShouldReplaceAndFindGenresByFilmId() {
        Film film = filmStorage.add(createFilm("Genre Film", 100, 1));

        filmGenreStorage.replaceByFilmId(film.getId(), Set.of(new Genre(1, null), new Genre(3, null)));

        assertThat(filmGenreStorage.findByFilmId(film.getId()))
                .extracting(Genre::getId)
                .containsExactly(1, 3);

        filmGenreStorage.replaceByFilmId(film.getId(), Set.of(new Genre(2, null)));

        assertThat(filmGenreStorage.findByFilmId(film.getId()))
                .containsExactly(new Genre(2, "Драма"));
    }

    @Test
    void filmGenreStorageShouldFindGenresByFilmIds() {
        Film firstFilm = filmStorage.add(createFilm("First Genre Film", 100, 1));
        Film secondFilm = filmStorage.add(createFilm("Second Genre Film", 101, 1));

        filmGenreStorage.replaceByFilmId(firstFilm.getId(), Set.of(new Genre(1, null), new Genre(3, null)));
        filmGenreStorage.replaceByFilmId(secondFilm.getId(), Set.of(new Genre(2, null)));

        assertThat(filmGenreStorage.findByFilmIds(Set.of(firstFilm.getId(), secondFilm.getId())))
                .containsEntry(firstFilm.getId(), Set.of(new Genre(1, "Комедия"), new Genre(3, "Мультфильм")))
                .containsEntry(secondFilm.getId(), Set.of(new Genre(2, "Драма")));
    }

    @Test
    void filmLikeStorageShouldAddFindAndDeleteLikes() {
        User user = userStorage.add(createUser("trinity"));
        Film film = filmStorage.add(createFilm("Liked Film", 120, 1));

        filmLikeStorage.add(film.getId(), user.getId());

        assertThat(filmLikeStorage.findUserIdsByFilmId(film.getId()))
                .containsExactly(user.getId());

        filmLikeStorage.delete(film.getId(), user.getId());

        assertThat(filmLikeStorage.findUserIdsByFilmId(film.getId()))
                .isEmpty();
    }

    @Test
    void filmLikeStorageShouldFindUserIdsByFilmIds() {
        User firstUser = userStorage.add(createUser("trinity"));
        User secondUser = userStorage.add(createUser("cypher"));
        Film firstFilm = filmStorage.add(createFilm("First Liked Film", 120, 1));
        Film secondFilm = filmStorage.add(createFilm("Second Liked Film", 121, 1));

        filmLikeStorage.add(firstFilm.getId(), firstUser.getId());
        filmLikeStorage.add(firstFilm.getId(), secondUser.getId());
        filmLikeStorage.add(secondFilm.getId(), secondUser.getId());

        assertThat(filmLikeStorage.findUserIdsByFilmIds(Set.of(firstFilm.getId(), secondFilm.getId())))
                .containsEntry(firstFilm.getId(), Set.of(firstUser.getId(), secondUser.getId()))
                .containsEntry(secondFilm.getId(), Set.of(secondUser.getId()));
    }

    @Test
    void filmLikeStorageShouldFindPopularFilmIds() {
        User firstUser = userStorage.add(createUser("trinity"));
        User secondUser = userStorage.add(createUser("cypher"));
        Film firstFilm = filmStorage.add(createFilm("Film Without Likes", 120, 1));
        Film secondFilm = filmStorage.add(createFilm("Film With Two Likes", 121, 1));
        Film thirdFilm = filmStorage.add(createFilm("Film With One Like", 122, 1));

        filmLikeStorage.add(secondFilm.getId(), firstUser.getId());
        filmLikeStorage.add(secondFilm.getId(), secondUser.getId());
        filmLikeStorage.add(thirdFilm.getId(), firstUser.getId());

        assertThat(filmLikeStorage.findPopularFilmIds(3, null, null))
                .containsExactly(secondFilm.getId(), thirdFilm.getId(), firstFilm.getId());
        assertThat(filmLikeStorage.findPopularFilmIds(2, null, null))
                .containsExactly(secondFilm.getId(), thirdFilm.getId());
    }

    /**
     * SQL findPopularFilmIds должен отбирать по genreId через film_genres, не искажая счётчик лайков.
     */
    @Test
    void filmLikeStorageShouldFilterPopularFilmIdsByGenre() {
        User user = userStorage.add(createUser("trinity"));
        Film comedyFilm = filmStorage.add(createFilm("Comedy Film", 100, 1));
        Film dramaFilm = filmStorage.add(createFilm("Drama Film", 101, 1));

        filmGenreStorage.replaceByFilmId(comedyFilm.getId(), Set.of(new Genre(1, null)));
        filmGenreStorage.replaceByFilmId(dramaFilm.getId(), Set.of(new Genre(2, null)));

        filmLikeStorage.add(comedyFilm.getId(), user.getId());
        filmLikeStorage.add(dramaFilm.getId(), user.getId());

        assertThat(filmLikeStorage.findPopularFilmIds(10, 1, null))
                .containsExactly(comedyFilm.getId());
    }

    /**
     * SQL findPopularFilmIds должен отбирать по году релиза через диапазон дат.
     */
    @Test
    void filmLikeStorageShouldFilterPopularFilmIdsByYear() {
        Film filmFromTargetYear = filmStorage.add(createFilm("Target Year Film", 100, 1, LocalDate.of(2010, 5, 1)));
        filmStorage.add(createFilm("Other Year Film", 101, 1, LocalDate.of(2015, 5, 1)));

        assertThat(filmLikeStorage.findPopularFilmIds(10, null, 2010))
                .containsExactly(filmFromTargetYear.getId());
    }

    /**
     * SQL findPopularFilmIds должен объединять фильтры по жанру и году через AND.
     */
    @Test
    void filmLikeStorageShouldFilterPopularFilmIdsByGenreAndYear() {
        Film matchingFilm = filmStorage.add(createFilm("Matching Film", 100, 1, LocalDate.of(2010, 5, 1)));
        Film wrongYearFilm = filmStorage.add(createFilm("Wrong Year Film", 101, 1, LocalDate.of(2015, 5, 1)));
        Film wrongGenreFilm = filmStorage.add(createFilm("Wrong Genre Film", 102, 1, LocalDate.of(2010, 5, 1)));

        filmGenreStorage.replaceByFilmId(matchingFilm.getId(), Set.of(new Genre(1, null)));
        filmGenreStorage.replaceByFilmId(wrongYearFilm.getId(), Set.of(new Genre(1, null)));
        filmGenreStorage.replaceByFilmId(wrongGenreFilm.getId(), Set.of(new Genre(2, null)));

        assertThat(filmLikeStorage.findPopularFilmIds(10, 1, 2010))
                .containsExactly(matchingFilm.getId());
    }

    @Test
    void reviewStorageShouldCreateAndFindReview() {
        User user = userStorage.add(createUser("reviewer"));
        Film film = filmStorage.add(createFilm("Фильм с отзывами", 120, 1));
        Review review = Review.builder()
                .content("Очень содержательный отзыв")
                .isPositive(true)
                .userId(user.getId())
                .filmId(film.getId())
                .build();

        Review savedReview = reviewStorage.add(review);

        assertThat(savedReview.getReviewId()).isPositive();
        assertThat(savedReview.getUseful()).isZero();
        assertThat(reviewStorage.findById(savedReview.getReviewId()))
                .isEqualTo(savedReview);
    }

    @Test
    void reviewStorageShouldUpdateAndDeleteReview() {
        User user = userStorage.add(createUser("reviewer"));
        Film film = filmStorage.add(createFilm("Фильм с отзывами", 120, 1));
        Review review = reviewStorage.add(createReview(user.getId(), film.getId(), "Первоначальный отзыв", true));

        review.setContent("Обновлённый отзыв");
        review.setIsPositive(false);

        Review updatedReview = reviewStorage.update(review);

        assertThat(updatedReview.getContent()).isEqualTo("Обновлённый отзыв");
        assertThat(updatedReview.getIsPositive()).isFalse();
        assertThat(updatedReview.getUserId()).isEqualTo(user.getId());
        assertThat(updatedReview.getFilmId()).isEqualTo(film.getId());

        reviewStorage.delete(review.getReviewId());

        assertThatThrownBy(() -> reviewStorage.findById(review.getReviewId()))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void reviewStorageShouldFilterSortAndLimitReviewsByUsefulness() {
        User author = userStorage.add(createUser("reviewer"));
        User firstReactor = userStorage.add(createUser("first-reactor"));
        User secondReactor = userStorage.add(createUser("second-reactor"));
        Film firstFilm = filmStorage.add(createFilm("Первый фильм с отзывами", 120, 1));
        Film secondFilm = filmStorage.add(createFilm("Второй фильм с отзывами", 121, 1));

        Review usefulReview = reviewStorage.add(
                createReview(author.getId(), firstFilm.getId(), "Полезный отзыв", true)
        );
        Review dislikedReview = reviewStorage.add(
                createReview(author.getId(), firstFilm.getId(), "Неполезный отзыв", false)
        );
        Review otherFilmReview = reviewStorage.add(
                createReview(author.getId(), secondFilm.getId(), "Отзыв к другому фильму", true)
        );

        reviewReactionStorage.save(usefulReview.getReviewId(), firstReactor.getId(), true);
        reviewReactionStorage.save(usefulReview.getReviewId(), secondReactor.getId(), true);
        reviewReactionStorage.save(dislikedReview.getReviewId(), firstReactor.getId(), false);

        assertThat(reviewStorage.findMostUseful(firstFilm.getId(), 10))
                .extracting(Review::getReviewId)
                .containsExactly(usefulReview.getReviewId(), dislikedReview.getReviewId());
        assertThat(reviewStorage.findMostUseful(firstFilm.getId(), 1))
                .extracting(Review::getReviewId)
                .containsExactly(usefulReview.getReviewId());
        assertThat(reviewStorage.findMostUseful(null, 10))
                .extracting(Review::getReviewId)
                .containsExactly(
                        usefulReview.getReviewId(),
                        otherFilmReview.getReviewId(),
                        dislikedReview.getReviewId()
                );
    }

    @Test
    void reviewReactionStorageShouldReplaceAndDeleteReaction() {
        User author = userStorage.add(createUser("reviewer"));
        User reactor = userStorage.add(createUser("reactor"));
        Film film = filmStorage.add(createFilm("Фильм с отзывами", 120, 1));
        Review review = reviewStorage.add(createReview(author.getId(), film.getId(), "Отзыв", true));

        reviewReactionStorage.save(review.getReviewId(), reactor.getId(), true);
        assertThat(reviewStorage.findById(review.getReviewId()).getUseful()).isEqualTo(1);

        reviewReactionStorage.save(review.getReviewId(), reactor.getId(), false);
        assertThat(reviewStorage.findById(review.getReviewId()).getUseful()).isEqualTo(-1);

        reviewReactionStorage.delete(review.getReviewId(), reactor.getId(), false);
        assertThat(reviewStorage.findById(review.getReviewId()).getUseful()).isZero();
    }

    @Test
    void friendRequestStorageShouldSaveFindDeleteAndCleanRequests() {
        User requester = userStorage.add(createUser("morpheus"));
        User recipient = userStorage.add(createUser("oracle"));
        User secondRequester = userStorage.add(createUser("tank"));

        friendRequestStorage.save(new FriendRequest(requester.getId(), recipient.getId()));
        friendRequestStorage.save(new FriendRequest(secondRequester.getId(), recipient.getId()));

        assertThat(friendRequestStorage.findByRequesterIdAndRecipientId(requester.getId(), recipient.getId()))
                .isPresent()
                .hasValue(new FriendRequest(requester.getId(), recipient.getId()));
        assertThat(friendRequestStorage.existsByRequesterIdAndRecipientId(requester.getId(), recipient.getId()))
                .isTrue();
        assertThat(friendRequestStorage.findRecipientIdsByRequesterId(requester.getId()))
                .containsExactly(recipient.getId());
        assertThat(friendRequestStorage.findRequesterIdsByRecipientId(recipient.getId()))
                .containsExactlyInAnyOrder(requester.getId(), secondRequester.getId());

        friendRequestStorage.deleteByRequesterIdAndRecipientId(requester.getId(), recipient.getId());

        assertThat(friendRequestStorage.existsByRequesterIdAndRecipientId(requester.getId(), recipient.getId()))
                .isFalse();

        friendRequestStorage.save(new FriendRequest(requester.getId(), recipient.getId()));

        assertThat(friendRequestStorage.deleteIfExistsByRequesterIdAndRecipientId(requester.getId(), recipient.getId()))
                .isTrue();
        assertThat(friendRequestStorage.deleteIfExistsByRequesterIdAndRecipientId(requester.getId(), recipient.getId()))
                .isFalse();

        friendRequestStorage.deleteAllByUserId(recipient.getId());

        assertThat(friendRequestStorage.findRequesterIdsByRecipientId(recipient.getId()))
                .isEmpty();
    }

    @Test
    void friendshipStorageShouldSaveFindDeleteAndCleanFriendships() {
        User firstUser = userStorage.add(createUser("apoc"));
        User secondUser = userStorage.add(createUser("switch"));
        User thirdUser = userStorage.add(createUser("mouse"));

        friendshipStorage.save(new Friendship(secondUser.getId(), firstUser.getId()));
        friendshipStorage.save(new Friendship(firstUser.getId(), thirdUser.getId()));

        assertThat(friendshipStorage.findByUserIds(firstUser.getId(), secondUser.getId()))
                .isPresent();
        assertThat(friendshipStorage.existsByUserIds(secondUser.getId(), firstUser.getId()))
                .isTrue();
        assertThat(friendshipStorage.findFriendIdsByUserId(firstUser.getId()))
                .containsExactlyInAnyOrder(secondUser.getId(), thirdUser.getId());

        friendshipStorage.deleteByUserIds(firstUser.getId(), secondUser.getId());

        assertThat(friendshipStorage.existsByUserIds(firstUser.getId(), secondUser.getId()))
                .isFalse();

        friendshipStorage.deleteAllByUserId(firstUser.getId());

        assertThat(friendshipStorage.findFriendIdsByUserId(thirdUser.getId()))
                .isEmpty();
    }

    private User createUser(String login) {
        return User.builder()
                .email(login + "@zion.human")
                .login(login)
                .name(login)
                .birthday(USER_BIRTHDAY)
                .build();
    }

    private Film createFilm(String name, int duration, int mpaId) {
        return createFilm(name, duration, mpaId, FILM_RELEASE_DATE);
    }

    private Film createFilm(String name, int duration, int mpaId, LocalDate releaseDate) {
        return Film.builder()
                .name(name)
                .description(name + " description")
                .releaseDate(releaseDate)
                .duration(duration)
                .mpa(new MpaRating(mpaId, null))
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
