package ru.yandex.practicum.filmorate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.yandex.practicum.filmorate.controller.UserController;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.service.UserService;
import ru.yandex.practicum.filmorate.storage.user.InMemoryUserStorage;
import ru.yandex.practicum.filmorate.storage.user.UserStorage;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class UserControllerTest {
    private UserController userController;

    private static final String VALID_EMAIL = "theone@zion.human";
    private static final String VALID_LOGIN = "theOne";
    private static final String VALID_NAME = "Нео";
    private static final LocalDate VALID_BIRTHDAY = LocalDate.of(1971, 9, 13);

    @BeforeEach
    void setUserController() {
        UserStorage userStorage = new InMemoryUserStorage();
        UserService userService = new UserService(userStorage);
        userController = new UserController(userService);
    }

    private User createValidUser() {
        return User.builder()
                .email(VALID_EMAIL)
                .login(VALID_LOGIN)
                .name(VALID_NAME)
                .birthday(VALID_BIRTHDAY)
                .build();
    }

    @Test
    void shouldCreateUserWithValidData() {
        User user = createValidUser();

        User resultUser = userController.createUser(user);

        assertEquals(VALID_EMAIL, resultUser.getEmail(), "Ожидается имейл " + VALID_EMAIL);
        assertEquals(VALID_LOGIN, resultUser.getLogin(), "Ожидается логин " + VALID_LOGIN);
        assertEquals(VALID_NAME, resultUser.getName(), "Ожидается имя " + VALID_NAME);
        assertEquals(VALID_BIRTHDAY, resultUser.getBirthday(), "Ожидается дата рождения " + VALID_BIRTHDAY);
        assertEquals(1L, resultUser.getId(), "Ожидается Id = 1");
        assertEquals(1, userController.getAllUsers().size(), "Ожидается общее количество пользователей 1");
    }
}
