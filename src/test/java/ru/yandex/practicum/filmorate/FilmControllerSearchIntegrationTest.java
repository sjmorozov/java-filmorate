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
import ru.yandex.practicum.filmorate.service.FilmService;

import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class FilmControllerSearchIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private FilmService filmService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("DELETE FROM film_likes");
        jdbcTemplate.update("DELETE FROM film_genres");
        jdbcTemplate.update("DELETE FROM film_directors");
        jdbcTemplate.update("DELETE FROM films");
        jdbcTemplate.update("DELETE FROM directors");
        jdbcTemplate.update("DELETE FROM users");
    }

    @Test
    void search_shouldReturnMatchingFilms() throws Exception {
        createAndSaveFilm("Крадущийся тигр, затаившийся дракон");
        createAndSaveFilm("Матрица");

        mockMvc.perform(get("/films/search")
                        .param("query", "крад")
                        .param("by", "title")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("Крадущийся тигр, затаившийся дракон"));
    }

    @Test
    void search_shouldReturnEmptyArrayWhenNoMatches() throws Exception {
        createAndSaveFilm("Матрица");

        mockMvc.perform(get("/films/search")
                        .param("query", "крад")
                        .param("by", "title")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void search_shouldReturnBadRequestWhenByHasUnknownValue() throws Exception {
        mockMvc.perform(get("/films/search")
                        .param("query", "крад")
                        .param("by", "actor")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    void search_shouldReturnBadRequestWhenQueryParamIsMissing() throws Exception {
        mockMvc.perform(get("/films/search")
                        .param("by", "title")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    void search_shouldReturnBadRequestWhenByParamIsMissing() throws Exception {
        mockMvc.perform(get("/films/search")
                        .param("query", "крад")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    private Film createAndSaveFilm(String name) {
        return filmService.create(Film.builder()
                .name(name)
                .description("Test description")
                .releaseDate(LocalDate.of(2020, 1, 1))
                .duration(120)
                .build());
    }
}
