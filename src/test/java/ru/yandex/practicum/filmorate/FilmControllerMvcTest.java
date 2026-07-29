package ru.yandex.practicum.filmorate;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import ru.yandex.practicum.filmorate.controller.FilmController;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.service.FilmService;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(FilmController.class)
class FilmControllerMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private FilmService filmService;

    /**
     * DELETE /films/{id} должен возвращать 204 No Content — это HTTP-контракт из @ResponseStatus,
     * который не проверить, вызывая контроллер напрямую как обычный Java-объект.
     */
    @Test
    void shouldReturnNoContentWhenFilmDeleted() throws Exception {
        mockMvc.perform(delete("/films/1"))
                .andExpect(status().isNoContent());

        verify(filmService).delete(1L);
    }

    /**
     * Удаление несуществующего фильма должно возвращать 404 с телом ошибки, а не 204 и не 500.
     */
    @Test
    void shouldReturnNotFoundWhenDeletingMissingFilm() throws Exception {
        doThrow(new NotFoundException("Фильм с id = 999 не найден")).when(filmService).delete(999L);

        mockMvc.perform(delete("/films/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Объект не найден"))
                .andExpect(jsonPath("$.description").value("Фильм с id = 999 не найден"));
    }
}
