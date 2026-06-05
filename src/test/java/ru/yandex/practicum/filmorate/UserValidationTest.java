package ru.yandex.practicum.filmorate;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import ru.yandex.practicum.filmorate.model.User;

import java.time.LocalDate;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

public class UserValidationTest {
    private Validator validator;

    private static final String VALID_EMAIL = "theone@zion.human";
    private static final String VALID_LOGIN = "theOne";
    private static final String VALID_NAME = "Нео";
    private static final LocalDate VALID_BIRTHDAY = LocalDate.of(1971, 9, 13);

    private static final LocalDate LATEST_ALLOWED_BIRTHDAY = LocalDate.now();

    private static final String FIELD_EMAIL = "email";
    private static final String FIELD_LOGIN = "login";
    private static final String FIELD_BIRTHDAY = "birthday";

    private static final String EMAIL_MUST_NOT_BE_BLANK_MESSAGE = "Электронная почта не может быть пустой";
    private static final String EMAIL_FORMAT_MESSAGE = "Некорректный формат имейл";
    private static final String LOGIN_MUST_NOT_BE_BLANK_MESSAGE = "Логин не может быть пустым";
    private static final String LOGIN_MUST_NOT_CONTAIN_SPACES_MESSAGE = "Логин не может содержать пробелы";
    private static final String BIRTHDAY_MUST_NOT_BE_IN_FUTURE_MESSAGE = "Дата рождения не может быть в будущем";

    @BeforeEach
    void setUp() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    private User createValidUser() {
        return User.builder()
                .email(VALID_EMAIL)
                .login(VALID_LOGIN)
                .name(VALID_NAME)
                .birthday(VALID_BIRTHDAY)
                .build();
    }

    private void assertHasViolation(User user, String fieldName, String message) {
        Set<ConstraintViolation<User>> violations = validator.validate(user);

        assertThat(violations)
                .as("Ожидается ошибка валидации поля '%s' с сообщением '%s'", fieldName, message)
                .anySatisfy(violation -> {
                    assertThat(violation.getPropertyPath().toString()).isEqualTo(fieldName);
                    assertThat(violation.getMessage()).isEqualTo(message);
                });
    }

    private void assertHasNoViolations(User user) {
        Set<ConstraintViolation<User>> violations = validator.validate(user);

        assertThat(violations).as("Ожидается отсутствие ошибок валидации").isEmpty();
    }

    @ParameterizedTest(name = "[{index}] invalid email = ''{0}''")
    @NullAndEmptySource
    @ValueSource(strings = {"    "})
    void shouldHaveEmailViolationWhenEmailIsInvalid(String invalidEmail) {
        User user = createValidUser();
        user.setEmail(invalidEmail);

        assertHasViolation(user, FIELD_EMAIL, EMAIL_MUST_NOT_BE_BLANK_MESSAGE);
    }

    @Test
    void shouldHaveEmailViolationWhenEmailDoesNotContainAt() {
        User user = createValidUser();
        user.setEmail("theonezion.human");

        assertHasViolation(user, FIELD_EMAIL, EMAIL_FORMAT_MESSAGE);
    }

    @ParameterizedTest(name = "[{index}] invalid login = ''{0}''")
    @NullAndEmptySource
    @ValueSource(strings = {"    "})
    void shouldHaveLoginViolationWhenLoginIsInvalid(String invalidLogin) {
        User user = createValidUser();
        user.setLogin(invalidLogin);

        assertHasViolation(user, FIELD_LOGIN, LOGIN_MUST_NOT_BE_BLANK_MESSAGE);
    }

    @ParameterizedTest(name = "[{index}] invalid login = ''{0}''")
    @ValueSource(strings = {"the One", "the\tOne"})
    void shouldHaveLoginViolationWhenLoginContainsSpaces(String invalidLogin) {
        User user = createValidUser();
        user.setLogin(invalidLogin);

        assertHasViolation(user, FIELD_LOGIN, LOGIN_MUST_NOT_CONTAIN_SPACES_MESSAGE);
    }

    @Test
    void shouldHaveNoViolationsWhenBirthdayIsToday() {
        User user = createValidUser();
        user.setBirthday(LATEST_ALLOWED_BIRTHDAY);

        assertHasNoViolations(user);
    }

    @Test
    void shouldHaveBirthdayViolationWhenBirthdayIsInFuture() {
        User user = createValidUser();
        user.setBirthday(LATEST_ALLOWED_BIRTHDAY.plusDays(1));

        assertHasViolation(user, FIELD_BIRTHDAY, BIRTHDAY_MUST_NOT_BE_IN_FUTURE_MESSAGE);
    }

    @Test
    void shouldHaveNoViolationsWhenUserIsValid() {
        User user = createValidUser();

        assertHasNoViolations(user);
    }
}
