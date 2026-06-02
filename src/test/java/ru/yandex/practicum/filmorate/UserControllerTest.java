package ru.yandex.practicum.filmorate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.yandex.practicum.filmorate.controller.UserController;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.User;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class UserControllerTest {
    UserController userController;

    private static final String VALID_EMAIL = "theone@zion.human";
    private static final String VALID_LOGIN = "theOne";
    private static final String VALID_NAME = "Нео";
    private static final LocalDate VALID_BIRTHDAY = LocalDate.of(1971, 9, 13);

    private static final LocalDate LATEST_ALLOWED_BIRTHDAY = LocalDate.now();

    @BeforeEach
    void setUserController() {
        userController = new UserController();
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
        assertEquals(1, resultUser.getId(), "Ожидается Id = 1");
        assertEquals(1, userController.getAllUsers().size(), "Ожидается общее количество пользователей 1");
    }

    @Test
    void shouldSetLoginAsNameWhenNameIsEmpty() {
        User user = createValidUser();
        user.setName("");
        User resultUser = userController.createUser(user);

        assertEquals(VALID_LOGIN, resultUser.getLogin(), "Ожидается логин " + VALID_LOGIN);
        assertEquals(VALID_LOGIN, resultUser.getName(), "Ожидается имя = логин " + VALID_LOGIN);
        assertEquals(1, userController.getAllUsers().size(), "Ожидается общее количество пользователей 1");
    }

    @Test
    void shouldSetLoginAsNameWhenNameIsBlank() {
        User user = createValidUser();
        user.setName("   ");
        User resultUser = userController.createUser(user);

        assertEquals(VALID_LOGIN, resultUser.getLogin(), "Ожидается логин " + VALID_LOGIN);
        assertEquals(VALID_LOGIN, resultUser.getName(), "Ожидается имя = логин " + VALID_LOGIN);
        assertEquals(1, userController.getAllUsers().size(), "Ожидается общее количество пользователей 1");
    }

    @Test
    void shouldSetLoginAsNameWhenNameIsNull() {
        User user = createValidUser();
        user.setName(null);
        User resultUser = userController.createUser(user);

        assertEquals(VALID_LOGIN, resultUser.getLogin(), "Ожидается логин " + VALID_LOGIN);
        assertEquals(VALID_LOGIN, resultUser.getName(), "Ожидается имя = логин " + VALID_LOGIN);
        assertEquals(1, userController.getAllUsers().size(), "Ожидается общее количество пользователей 1");
    }

    @Test
    void shouldThrowValidationExceptionWhenEmailIsEmpty() {
        User user = createValidUser();
        user.setEmail("");

        ValidationException validationException = assertThrows(ValidationException.class,
                () -> userController.createUser(user));

        String errorMessage = "Электронная почта не может быть пустой";
        assertEquals(errorMessage, validationException.getMessage());
        assertEquals(0, userController.getAllUsers().size(),
                "Пользователь с пустой электронной почтой не должен быть сохранён");
    }

    @Test
    void shouldThrowValidationExceptionWhenEmailIsBlank() {
        User user = createValidUser();
        user.setEmail("   ");

        ValidationException validationException = assertThrows(ValidationException.class,
                () -> userController.createUser(user));

        String errorMessage = "Электронная почта не может быть пустой";
        assertEquals(errorMessage, validationException.getMessage());
        assertEquals(0, userController.getAllUsers().size(),
                "Пользователь с пустой электронной почтой не должен быть сохранён");
    }

    @Test
    void shouldThrowValidationExceptionWhenEmailIsNull() {
        User user = createValidUser();
        user.setEmail(null);

        ValidationException validationException = assertThrows(ValidationException.class,
                () -> userController.createUser(user));

        String errorMessage = "Электронная почта не может быть пустой";
        assertEquals(errorMessage, validationException.getMessage());
        assertEquals(0, userController.getAllUsers().size(),
                "Пользователь с пустой электронной почтой не должен быть сохранён");
    }

    @Test
    void shouldThrowValidationExceptionWhenEmailDoesNotContainAt() {
        User user = createValidUser();
        user.setEmail("theonezion.human");

        ValidationException validationException = assertThrows(ValidationException.class,
                () -> userController.createUser(user));

        String errorMessage = "Некорректный формат имейл";
        assertEquals(errorMessage, validationException.getMessage());
        assertEquals(0, userController.getAllUsers().size(),
                "Пользователь с невалидным форматом электронной почты не должен быть сохранён");
    }

    @Test
    void shouldThrowValidationExceptionWhenLoginIsEmpty() {
        User user = createValidUser();
        user.setLogin("");

        ValidationException validationException = assertThrows(ValidationException.class,
                () -> userController.createUser(user));

        String errorMessage = "Логин не может быть пустым";
        assertEquals(errorMessage, validationException.getMessage());
        assertEquals(0, userController.getAllUsers().size(),
                "Пользователь с пустым логином не должен быть сохранён");
    }

    @Test
    void shouldThrowValidationExceptionWhenLoginIsBlank() {
        User user = createValidUser();
        user.setLogin("   ");

        ValidationException validationException = assertThrows(ValidationException.class,
                () -> userController.createUser(user));

        String errorMessage = "Логин не может быть пустым";
        assertEquals(errorMessage, validationException.getMessage());
        assertEquals(0, userController.getAllUsers().size(),
                "Пользователь с пустым логином не должен быть сохранён");
    }

    @Test
    void shouldThrowValidationExceptionWhenLoginIsNull() {
        User user = createValidUser();
        user.setLogin(null);

        ValidationException validationException = assertThrows(ValidationException.class,
                () -> userController.createUser(user));

        String errorMessage = "Логин не может быть пустым";
        assertEquals(errorMessage, validationException.getMessage());
        assertEquals(0, userController.getAllUsers().size(),
                "Пользователь с пустым логином не должен быть сохранён");
    }

    @Test
    void shouldThrowValidationExceptionWhenLoginContainsSpace() {
        User user = createValidUser();
        user.setLogin("the One");

        ValidationException validationException = assertThrows(ValidationException.class,
                () -> userController.createUser(user));

        String errorMessage = "Логин не может содержать пробелы";
        assertEquals(errorMessage, validationException.getMessage());
        assertEquals(0, userController.getAllUsers().size(),
                "Пользователь с логином с пробелами не должен быть сохранён");
    }

    @Test
    void shouldThrowValidationExceptionWhenLoginContainsTab() {
        User user = createValidUser();
        user.setLogin("the\tOne");

        ValidationException validationException = assertThrows(ValidationException.class,
                () -> userController.createUser(user));

        String errorMessage = "Логин не может содержать пробелы";
        assertEquals(errorMessage, validationException.getMessage());
        assertEquals(0, userController.getAllUsers().size(),
                "Пользователь с логином со знаком tab не должен быть сохранён");
    }

    @Test
    void shouldCreateUserWhenBirthdayIsToday() {
        User user = createValidUser();
        user.setBirthday(LATEST_ALLOWED_BIRTHDAY);
        User resultUser = userController.createUser(user);

        assertEquals(VALID_EMAIL, resultUser.getEmail(), "Ожидается имейл " + VALID_EMAIL);
        assertEquals(VALID_LOGIN, resultUser.getLogin(), "Ожидается логин " + VALID_LOGIN);
        assertEquals(VALID_NAME, resultUser.getName(), "Ожидается имя " + VALID_NAME);
        assertEquals(LATEST_ALLOWED_BIRTHDAY, resultUser.getBirthday(), "Ожидается дата рождения " + LATEST_ALLOWED_BIRTHDAY);
        assertEquals(1, resultUser.getId(), "Ожидается Id = 1");
        assertEquals(1, userController.getAllUsers().size(), "Ожидается общее количество пользователей 1");
    }

    @Test
    void shouldThrowValidationExceptionWhenBirthdayIsInFuture() {
        User user = createValidUser();
        user.setBirthday(LATEST_ALLOWED_BIRTHDAY.plusDays(1));

        ValidationException validationException = assertThrows(ValidationException.class,
                () -> userController.createUser(user));

        String errorMessage = "Дата рождения не может быть в будущем";
        assertEquals(errorMessage, validationException.getMessage());
        assertEquals(0, userController.getAllUsers().size(),
                "Пользователь с невалидной датой рождения не должен быть сохранён");
    }

    @Test
    void shouldUpdateUserWithValidData() {
        User user = createValidUser();
        User createdUser = userController.createUser(user);

        User userForUpdate = createValidUser();
        userForUpdate.setId(createdUser.getId());
        LocalDate newBirthday = VALID_BIRTHDAY.plusYears(10);
        userForUpdate.setBirthday(newBirthday);
        User updatedUser = userController.updateUser(userForUpdate);

        assertEquals(VALID_EMAIL, updatedUser.getEmail(), "Ожидается старый имейл " + VALID_EMAIL);
        assertEquals(VALID_LOGIN, updatedUser.getLogin(), "Ожидается старый логин " + VALID_LOGIN);
        assertEquals(VALID_NAME, updatedUser.getName(), "Ожидается старое имя " + VALID_NAME);
        assertEquals(newBirthday, updatedUser.getBirthday(), "Ожидается новая дата рождения " + newBirthday);
        assertEquals(1, updatedUser.getId(), "Ожидается старый Id = 1");
        assertEquals(1, userController.getAllUsers().size(), "Ожидается общее количество пользователей 1");
    }

    @Test
    void shouldUpdateUserAndKeepSameId() {
        User user = createValidUser();
        User createdUser = userController.createUser(user);

        String newEmail = "morpheus@zion.human";
        String newLogin = "morpheus";
        String newName = "Морфеус";

        User userForUpdate = User.builder()
                .id(createdUser.getId())
                .email(newEmail)
                .login(newLogin)
                .name(newName)
                .birthday(VALID_BIRTHDAY)
                .build();

        User updatedUser = userController.updateUser(userForUpdate);

        assertEquals(newEmail, updatedUser.getEmail(), "Ожидается новый имейл " + newEmail);
        assertEquals(newLogin, updatedUser.getLogin(), "Ожидается новый логин " + newLogin);
        assertEquals(newName, updatedUser.getName(), "Ожидается новое имя " + newName);
        assertEquals(createdUser.getId(), updatedUser.getId(), "Id пользователя не должен измениться");
        assertEquals(1, userController.getAllUsers().size(), "Ожидается общее количество пользователей 1");
    }

    @Test
    void shouldThrowValidationExceptionWhenUserIdIsZero() {
        User user = createValidUser();
        userController.createUser(user);
        User shadowUser = createValidUser();
        shadowUser.setId(0);

        ValidationException validationException = assertThrows(ValidationException.class,
                () -> userController.updateUser(shadowUser));

        String errorMessage = "Id должен быть указан";
        assertEquals(errorMessage, validationException.getMessage());
        assertEquals(1, userController.getAllUsers().size(),
                "Размер списка должен остаться без изменений");
        assertEquals(1, user.getId(), "Id пользователя не должен измениться");
    }

    @Test
    void shouldThrowValidationExceptionWhenUserIdIsNegative() {
        User user = createValidUser();
        userController.createUser(user);
        User shadowUser = createValidUser();
        shadowUser.setId(-1);

        ValidationException validationException = assertThrows(ValidationException.class,
                () -> userController.updateUser(shadowUser));

        String errorMessage = "Id должен быть указан";
        assertEquals(errorMessage, validationException.getMessage());
        assertEquals(1, userController.getAllUsers().size(),
                "Размер списка должен остаться без изменений");
        assertEquals(1, user.getId(), "Id пользователя не должен измениться");
    }

    @Test
    void shouldThrowNotFoundExceptionWhenUserDoesNotExist() {
        User user = createValidUser();
        userController.createUser(user);
        User shadowUser = createValidUser();
        shadowUser.setId(999);

        NotFoundException notFoundException = assertThrows(NotFoundException.class,
                () -> userController.updateUser(shadowUser));

        String errorMessage = "Пользователь с id = " + shadowUser.getId() + " не найден";
        assertEquals(errorMessage, notFoundException.getMessage());
        assertEquals(1, userController.getAllUsers().size(),
                "Размер списка должен остаться без изменений");
        assertEquals(1, user.getId(), "Id пользователя не должен измениться");
    }

    @Test
    void shouldAssignIncrementalIdsWhenSeveralUsersCreated() {
        User firstUser = createValidUser();
        User firstCreatedUser = userController.createUser(firstUser);

        User secondUser = User.builder()
                .email("morpheus@zion.human")
                .login("morpheus")
                .name("Морфеус")
                .birthday(VALID_BIRTHDAY)
                .build();

        User secondCreatedUser = userController.createUser(secondUser);

        assertEquals(1, firstCreatedUser.getId(), "Ожидается Id = 1");
        assertEquals(2, secondCreatedUser.getId(), "Ожидается Id = 2");
        assertEquals(2, userController.getAllUsers().size(),
                "Размер списка ожидается 2");
    }
}
