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
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.storage.film.FilmDbStorage;
import ru.yandex.practicum.filmorate.storage.filmgenre.FilmGenreDbStorage;
import ru.yandex.practicum.filmorate.storage.filmlike.FilmLikeDbStorage;
import ru.yandex.practicum.filmorate.storage.friendrequest.FriendRequestDbStorage;
import ru.yandex.practicum.filmorate.storage.friendship.FriendshipDbStorage;
import ru.yandex.practicum.filmorate.storage.genre.GenreDbStorage;
import ru.yandex.practicum.filmorate.storage.mparating.MpaRatingDbStorage;
import ru.yandex.practicum.filmorate.storage.user.UserDbStorage;

import java.time.LocalDate;
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
        FriendshipDbStorage.class
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
    void genreStorageShouldFindGenresByIdAndFindAll() {
        Genre comedy = genreStorage.findById(1);

        assertThat(comedy).isEqualTo(new Genre(1, "Комедия"));
        assertThat(genreStorage.findAll())
                .extracting(Genre::getId)
                .containsExactly(1, 2, 3, 4, 5, 6);
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
        return Film.builder()
                .name(name)
                .description(name + " description")
                .releaseDate(FILM_RELEASE_DATE)
                .duration(duration)
                .mpa(new MpaRating(mpaId, null))
                .build();
    }
}
