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
class FilmServiceRecommendationsIntegrationTest {

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
    void getRecommendations_whenUserHasNoLikes_shouldReturnEmptyList() {
        User user1 = createAndSaveUser("user1@test.com", "user1");
        Film film1 = createAndSaveFilm("Film 1", 120);
        Film film2 = createAndSaveFilm("Film 2", 100);

        User user2 = createAndSaveUser("user2@test.com", "user2");
        User user3 = createAndSaveUser("user3@test.com", "user3");

        addLike(film1, user2);
        addLike(film1, user3);
        addLike(film2, user3);

        Collection<Film> recommendations = filmService.getRecommendations(user1.getId(), 10);

        assertThat(recommendations).isEmpty();
    }

    @Test
    void getRecommendations_whenUserHasLikes_shouldReturnRecommendedFilms() {
        User u1 = createAndSaveUser("u1@test.com", "u1");
        User u2 = createAndSaveUser("u2@test.com", "u2");
        User u3 = createAndSaveUser("u3@test.com", "u3");

        Film f1 = createAndSaveFilm("Film 1", 120);
        Film f2 = createAndSaveFilm("Film 2", 100);
        Film f3 = createAndSaveFilm("Film 3", 110);
        Film f4 = createAndSaveFilm("Film 4", 90);

        addLike(f1, u1);
        addLike(f2, u1);

        addLike(f1, u2);
        addLike(f3, u2);

        addLike(f2, u3);
        addLike(f4, u3);

        Collection<Film> recommendations = filmService.getRecommendations(u1.getId(), 10);

        assertThat(recommendations).hasSize(2);
        List<Long> recommendedIds = recommendations.stream().map(Film::getId).toList();
        assertThat(recommendedIds).containsExactlyInAnyOrder(f3.getId(), f4.getId());
    }

    @Test
    void getRecommendations_shouldNotRecommendAlreadyLikedFilms() {
        User u1 = createAndSaveUser("u1@test.com", "u1");
        User u2 = createAndSaveUser("u2@test.com", "u2");

        Film f1 = createAndSaveFilm("Film 1", 120);
        Film f2 = createAndSaveFilm("Film 2", 100);

        addLike(f1, u1);
        addLike(f2, u1);
        addLike(f1, u2);
        addLike(f2, u2);

        Collection<Film> recommendations = filmService.getRecommendations(u1.getId(), 10);

        assertThat(recommendations).isEmpty();
    }

    @Test
    void getRecommendations_shouldRespectCountParameter() {
        User u1 = createAndSaveUser("u1@test.com", "u1");
        User u2 = createAndSaveUser("u2@test.com", "u2");
        User u3 = createAndSaveUser("u3@test.com", "u3");

        Film f1 = createAndSaveFilm("Film 1", 120);
        Film f2 = createAndSaveFilm("Film 2", 100);
        Film f3 = createAndSaveFilm("Film 3", 110);
        Film f4 = createAndSaveFilm("Film 4", 90);
        Film f5 = createAndSaveFilm("Film 5", 95);

        addLike(f1, u1);

        addLike(f1, u2);
        addLike(f2, u2);
        addLike(f3, u2);

        addLike(f1, u3);
        addLike(f4, u3);
        addLike(f5, u3);

        Collection<Film> recommendations = filmService.getRecommendations(u1.getId(), 2);

        assertThat(recommendations).hasSize(2);
    }

    private User createAndSaveUser(String email, String login) {
        User user = User.builder()
                .email(email)
                .login(login)
                .name("Test User")
                .birthday(LocalDate.of(1990, 1, 1))
                .build();
        return userService.create(user);
    }

    private Film createAndSaveFilm(String name, int duration) {
        Film film = Film.builder()
                .name(name)
                .description("Test description")
                .releaseDate(LocalDate.of(2020, 1, 1))
                .duration(duration)
                .build();
        return filmService.create(film);
    }

    private void addLike(Film film, User user) {
        filmService.addLike(film.getId(), user.getId());
    }
}