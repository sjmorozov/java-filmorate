package ru.yandex.practicum.filmorate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.service.FilmService;
import ru.yandex.practicum.filmorate.service.UserService;

import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class UserControllerRecommendationsIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserService userService;

    @Autowired
    private FilmService filmService;

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
    void getRecommendations_shouldReturnRecommendedFilms() throws Exception {
        User u1 = createAndSaveUser("u1@test.com", "u1");
        User u2 = createAndSaveUser("u2@test.com", "u2");

        Film f1 = createAndSaveFilm("Film 1", 120);
        Film f2 = createAndSaveFilm("Film 2", 100);

        addLike(f1, u1);
        addLike(f1, u2);
        addLike(f2, u2);

        mockMvc.perform(get("/users/{id}/recommendations", u1.getId())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].id").value(f2.getId()))
                .andExpect(jsonPath("$[0].name").value("Film 2"));
    }

    @Test
    void getRecommendations_withCountParameter_shouldLimitResults() throws Exception {
        User u1 = createAndSaveUser("u1@test.com", "u1");
        User u2 = createAndSaveUser("u2@test.com", "u2");
        User u3 = createAndSaveUser("u3@test.com", "u3");

        Film f1 = createAndSaveFilm("Film 1", 120);
        Film f2 = createAndSaveFilm("Film 2", 100);
        Film f3 = createAndSaveFilm("Film 3", 110);

        addLike(f1, u1);
        addLike(f1, u2);
        addLike(f2, u2);
        addLike(f3, u2);
        addLike(f1, u3);

        mockMvc.perform(get("/users/{id}/recommendations", u1.getId())
                        .param("count", "1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void getRecommendations_withInvalidCount_shouldReturnBadRequest() throws Exception {
        User u1 = createAndSaveUser("u1@test.com", "u1");

        mockMvc.perform(get("/users/{id}/recommendations", u1.getId())
                        .param("count", "0")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getRecommendations_withNegativeCount_shouldReturnBadRequest() throws Exception {
        User u1 = createAndSaveUser("u1@test.com", "u1");

        mockMvc.perform(get("/users/{id}/recommendations", u1.getId())
                        .param("count", "-5")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getRecommendations_whenUserNotFound_shouldReturnNotFound() throws Exception {
        mockMvc.perform(get("/users/{id}/recommendations", 999L)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    void getRecommendations_whenUserHasNoLikes_shouldReturnPopularFilms() throws Exception {
        User u1 = createAndSaveUser("u1@test.com", "u1");
        User u2 = createAndSaveUser("u2@test.com", "u2");
        User u3 = createAndSaveUser("u3@test.com", "u3");

        Film f1 = createAndSaveFilm("Popular Film", 120);

        addLike(f1, u2);
        addLike(f1, u3);

        mockMvc.perform(get("/users/{id}/recommendations", u1.getId())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));
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
