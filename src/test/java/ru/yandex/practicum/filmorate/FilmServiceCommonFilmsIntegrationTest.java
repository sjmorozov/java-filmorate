package ru.yandex.practicum.filmorate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.service.FilmService;
import ru.yandex.practicum.filmorate.service.UserService;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class FilmServiceCommonFilmsIntegrationTest {

    @Autowired
    private FilmService filmService;

    @Autowired
    private UserService userService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("DELETE FROM film_likes");
        jdbcTemplate.update("DELETE FROM film_genres");
        jdbcTemplate.update("DELETE FROM films");
        jdbcTemplate.update("DELETE FROM users");
    }

    @Test
    void findCommonFilms_shouldReturnFilmsLikedByBothUsers() {
        User u1 = createAndSaveUser("u1@test.com", "u1");
        User u2 = createAndSaveUser("u2@test.com", "u2");
        User u3 = createAndSaveUser("u3@test.com", "u3");

        Film f1 = createAndSaveFilm("Film 1", 120);
        Film f2 = createAndSaveFilm("Film 2", 100);
        Film f3 = createAndSaveFilm("Film 3", 110);

        addLike(f1, u1);
        addLike(f2, u1);
        addLike(f1, u2);
        addLike(f2, u2);

        addLike(f3, u1);

        addLike(f1, u3);

        Collection<Film> common = filmService.findCommonFilms(u1.getId(), u2.getId());

        assertThat(common).hasSize(2);

        List<Long> ids = common.stream().map(Film::getId).toList();

        assertThat(ids).containsExactly(f1.getId(), f2.getId());
    }

    @Test
    void findCommonFilms_whenNoCommonFilms_shouldReturnEmptyList() {
        User u1 = createAndSaveUser("u1@test.com", "u1");
        User u2 = createAndSaveUser("u2@test.com", "u2");

        Film f1 = createAndSaveFilm("Film 1", 120);
        Film f2 = createAndSaveFilm("Film 2", 100);

        addLike(f1, u1);
        addLike(f2, u2);

        Collection<Film> common = filmService.findCommonFilms(u1.getId(), u2.getId());

        assertThat(common).isEmpty();
    }

    @Test
    void findCommonFilms_shouldSortByPopularity() {
        User u1 = createAndSaveUser("u1@test.com", "u1");
        User u2 = createAndSaveUser("u2@test.com", "u2");
        User u3 = createAndSaveUser("u3@test.com", "u3");
        User u4 = createAndSaveUser("u4@test.com", "u4");

        Film f1 = createAndSaveFilm("Film 1", 120);
        Film f2 = createAndSaveFilm("Film 2", 100);
        Film f3 = createAndSaveFilm("Film 3", 110);

        addLike(f1, u1);
        addLike(f1, u2);

        addLike(f2, u1);
        addLike(f2, u2);

        addLike(f3, u1);
        addLike(f3, u2);

        addLike(f3, u3);
        addLike(f3, u4);

        addLike(f1, u3);

        Collection<Film> common = filmService.findCommonFilms(u1.getId(), u2.getId());

        List<Long> ids = common.stream().map(Film::getId).toList();
        assertThat(ids).containsExactly(f3.getId(), f1.getId(), f2.getId());
    }

    @Test
    void findCommonFilms_withSamePopularity_shouldSortById() {
        User u1 = createAndSaveUser("u1@test.com", "u1");
        User u2 = createAndSaveUser("u2@test.com", "u2");

        Film f1 = createAndSaveFilm("Film A", 110);
        Film f2 = createAndSaveFilm("Film B", 100);
        Film f3 = createAndSaveFilm("Film C", 120);

        addLike(f1, u1);
        addLike(f1, u2);
        addLike(f2, u1);
        addLike(f2, u2);
        addLike(f3, u1);
        addLike(f3, u2);

        Collection<Film> common = filmService.findCommonFilms(u1.getId(), u2.getId());

        List<Long> ids = common.stream().map(Film::getId).toList();

        assertThat(ids).containsExactly(f1.getId(), f2.getId(), f3.getId());
    }

    private User createAndSaveUser(String email, String login) {
        return userService.create(User.builder()
                .email(email)
                .login(login)
                .name("Test User")
                .birthday(LocalDate.of(1990, 1, 1))
                .build());
    }

    private Film createAndSaveFilm(String name, int duration) {
        return filmService.create(Film.builder()
                .name(name)
                .description("Test description")
                .releaseDate(LocalDate.of(2020, 1, 1))
                .duration(duration)
                .build());
    }

    private void addLike(Film film, User user) {
        filmService.addLike(film.getId(), user.getId());
    }
}
