package ru.yandex.practicum.filmorate; // (твой пакет)

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import ru.yandex.practicum.filmorate.controller.UserController;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.service.FilmService;
import ru.yandex.practicum.filmorate.service.UserService;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
class UserControllerMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private FilmService filmService;

    /**
     * DELETE /users/{id} должен возвращать 204 No Content — это HTTP-контракт из @ResponseStatus,
     * который не проверить, вызывая контроллер напрямую как обычный Java-объект.
     */
    @Test
    void shouldReturnNoContentWhenUserDeleted() throws Exception {
        mockMvc.perform(delete("/users/1"))
                .andExpect(status().isNoContent());

        verify(userService).delete(1L);
    }

    /**
     * Удаление несуществующего пользователя должно возвращать 404 с телом ошибки, а не 204 и не 500.
     */
    @Test
    void shouldReturnNotFoundWhenDeletingMissingUser() throws Exception {
        doThrow(new NotFoundException("Пользователь с id = 999 не найден")).when(userService).delete(999L);

        mockMvc.perform(delete("/users/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Объект не найден"))
                .andExpect(jsonPath("$.description").value("Пользователь с id = 999 не найден"));
    }
}
