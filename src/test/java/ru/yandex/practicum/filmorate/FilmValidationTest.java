package ru.yandex.practicum.filmorate;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import ru.yandex.practicum.filmorate.model.Film;

import java.time.LocalDate;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

public class FilmValidationTest {
    private Validator validator;

    private static final String VALID_NAME = "Матрица";
    private static final String VALID_DESCRIPTION = "Человек узнаёт правду о реальности и выбирает красную таблетку.";
    private static final LocalDate VALID_RELEASE_DATE = LocalDate.of(1999, 3, 31);
    private static final int VALID_DURATION = 136;

    private static final int MAX_DESCRIPTION_LENGTH = 200;

    private static final String FIELD_NAME = "name";
    private static final String FIELD_DESCRIPTION = "description";
    private static final String FIELD_RELEASE_DATE = "releaseDate";
    private static final String FIELD_DURATION = "duration";

    private static final String NAME_MUST_NOT_BE_BLANK_MESSAGE = "Название должно быть указано";
    private static final String MAX_DESCRIPTION_MESSAGE = "Максимальная длина описания — " + MAX_DESCRIPTION_LENGTH + " символов";
    private static final String RELEASE_DATE_MUST_NOT_BE_NULL_MESSAGE = "Дата релиза должна быть указана";
    private static final String DURATION_MUST_BE_POSITIVE_MESSAGE = "Продолжительность фильма должна быть положительным числом";

    @BeforeEach
    void setUp() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    private Film createValidFilm() {
        return Film.builder()
                .name(VALID_NAME)
                .description(VALID_DESCRIPTION)
                .releaseDate(VALID_RELEASE_DATE)
                .duration(VALID_DURATION)
                .build();
    }

    private void assertHasViolation(Film film, String fieldName, String message) {
        Set<ConstraintViolation<Film>> violations = validator.validate(film);

        assertThat(violations)
                .as("Ожидается ошибка валидации поля '%s' с сообщением '%s'", fieldName, message)
                .anySatisfy(violation -> {
                    assertThat(violation.getPropertyPath().toString()).isEqualTo(fieldName);
                    assertThat(violation.getMessage()).isEqualTo(message);
                });
    }

    private void assertHasNoViolations(Film film) {
        Set<ConstraintViolation<Film>> violations = validator.validate(film);

        assertThat(violations).as("Ожидается отсутствие ошибок валидации").isEmpty();
    }

    private String createDescription(int length) {
        return "А".repeat(length);
    }

    @ParameterizedTest(name = "[{index}] invalid name = ''{0}''")
    @NullAndEmptySource
    @ValueSource(strings = {"    "})
    void shouldHaveNameViolationWhenNameIsInvalid(String invalidName) {
        Film film = createValidFilm();
        film.setName(invalidName);

        assertHasViolation(film, FIELD_NAME, NAME_MUST_NOT_BE_BLANK_MESSAGE);
    }

    @Test
    void shouldHaveNoViolationsWhenDescriptionHasMaxLength() {
        Film film = createValidFilm();
        String description = createDescription(MAX_DESCRIPTION_LENGTH);
        film.setDescription(description);
        assertHasNoViolations(film);
    }

    @Test
    void shouldHaveNoViolationsWhenDescriptionIsNull() {
        Film film = createValidFilm();
        film.setDescription(null);
        assertHasNoViolations(film);
    }

    @Test
    void shouldHaveDescriptionViolationWhenDescriptionTooLong() {
        Film film = createValidFilm();
        String description = createDescription(MAX_DESCRIPTION_LENGTH + 1);
        film.setDescription(description);
        assertHasViolation(film, FIELD_DESCRIPTION, MAX_DESCRIPTION_MESSAGE);
    }

    @Test
    void shouldHaveReleaseDateViolationWhenReleaseDateIsNull() {
        Film film = createValidFilm();
        film.setReleaseDate(null);
        assertHasViolation(film, FIELD_RELEASE_DATE, RELEASE_DATE_MUST_NOT_BE_NULL_MESSAGE);
    }

    @Test
    void shouldHaveDurationViolationWhenDurationIsZero() {
        Film film = createValidFilm();
        film.setDuration(0);
        assertHasViolation(film, FIELD_DURATION, DURATION_MUST_BE_POSITIVE_MESSAGE);
    }

    @ParameterizedTest(name = "[{index}] invalid duration = {0}")
    @NullSource
    @ValueSource(ints = {0, -1})
    void shouldHaveDurationViolationWhenDurationIsInvalid(Integer duration) {
        Film film = createValidFilm();
        film.setDuration(duration);
        assertHasViolation(film, FIELD_DURATION, DURATION_MUST_BE_POSITIVE_MESSAGE);
    }
}
