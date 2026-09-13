package ru.yandex.practicum.filmorate;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import ru.yandex.practicum.filmorate.controller.FilmController;
import ru.yandex.practicum.filmorate.service.FilmService;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(FilmController.class)
class ErrorHandlerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private FilmService filmService;

    /**
     * ConstraintViolationException от @Positive на @RequestParam должна маппиться на 400,
     * а не проваливаться в общий обработчик Exception.class с 500 (см. ErrorHandler#handleConstraintViolation).
     */
    @Test
    void shouldReturnBadRequestWhenPopularCountIsNotPositive() throws Exception {
        mockMvc.perform(get("/films/popular").param("count", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Ошибка валидации"))
                .andExpect(jsonPath("$.description", containsString("count")));
    }

    /**
     * То же самое для genreId — параметра, добавленного вместе с фильтрацией популярных фильмов.
     */
    @Test
    void shouldReturnBadRequestWhenPopularGenreIdIsNotPositive() throws Exception {
        mockMvc.perform(get("/films/popular").param("genreId", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Ошибка валидации"))
                .andExpect(jsonPath("$.description", containsString("genreId")));
    }
}
