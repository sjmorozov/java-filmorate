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

//    @Test
//    void shouldCreateUserWithValidData() {
//        User user = createValidUser();
//        User resultUser = userController.createUser(user);
//
//        assertEquals(VALID_EMAIL, resultUser.getEmail(), "Ожидается имейл " + VALID_EMAIL);
//        assertEquals(VALID_LOGIN, resultUser.getLogin(), "Ожидается логин " + VALID_LOGIN);
//        assertEquals(VALID_NAME, resultUser.getName(), "Ожидается имя " + VALID_NAME);
//        assertEquals(VALID_BIRTHDAY, resultUser.getBirthday(), "Ожидается дата рождения " + VALID_BIRTHDAY);
//        assertEquals(1, resultUser.getId(), "Ожидается Id = 1");
//        assertEquals(1, userController.getAllUsers().size(), "Ожидается общее количество пользователей 1");
//    }

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
