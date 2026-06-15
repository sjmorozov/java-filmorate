package ru.yandex.practicum.filmorate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.yandex.practicum.filmorate.controller.FilmController;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.service.FilmService;
import ru.yandex.practicum.filmorate.storage.film.FilmStorage;
import ru.yandex.practicum.filmorate.storage.film.InMemoryFilmStorage;
import ru.yandex.practicum.filmorate.storage.user.InMemoryUserStorage;
import ru.yandex.practicum.filmorate.storage.user.UserStorage;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class FilmControllerTest {

    FilmController filmController;

    private static final String VALID_NAME = "Матрица";
    private static final String VALID_DESCRIPTION = "Человек узнаёт правду о реальности и выбирает красную таблетку.";
    private static final LocalDate VALID_RELEASE_DATE = LocalDate.of(1999, 3, 31);
    private static final int VALID_DURATION = 136;

    private static final LocalDate MIN_RELEASE_DATE = LocalDate.of(1895, 12, 28);

    @BeforeEach
    void setFilmController() {
        FilmStorage filmStorage = new InMemoryFilmStorage();
        UserStorage userStorage = new InMemoryUserStorage();
        FilmService filmService = new FilmService(filmStorage, userStorage);
        filmController = new FilmController(filmService);
    }

    private Film createValidFilm() {
        return Film.builder()
                .name(VALID_NAME)
                .description(VALID_DESCRIPTION)
                .releaseDate(VALID_RELEASE_DATE)
                .duration(VALID_DURATION)
                .build();
    }

    @Test
    void shouldCreateFilmWithValidData() {
        Film film = createValidFilm();
        Film result = filmController.createFilm(film);

        assertEquals(VALID_NAME, result.getName(), "Ожидается " + VALID_NAME);
        assertEquals(VALID_DESCRIPTION, result.getDescription(), "Ожидается " + VALID_DESCRIPTION);
        assertEquals(VALID_RELEASE_DATE, result.getReleaseDate(), "Ожидается " + VALID_RELEASE_DATE);
        assertEquals(VALID_DURATION, result.getDuration(), "Ожидается " + VALID_DURATION);
        assertEquals(1, result.getId(), "Ожидается 1");
        assertEquals(1, filmController.getAllFilms().size(), "Ожидается число фильмов 1");
    }

    @Test
    void shouldCreateFilmWithMinReleaseDate() {
        Film film = createValidFilm();
        film.setReleaseDate(MIN_RELEASE_DATE);
        Film result = filmController.createFilm(film);

        assertEquals(MIN_RELEASE_DATE, result.getReleaseDate(), "Ожидается " + MIN_RELEASE_DATE);
        assertEquals(1, filmController.getAllFilms().size(), "Ожидается число фильмов 1");
    }

    @Test
    void shouldThrowValidationExceptionWhenReleaseDateBeforeMin() {
        Film film = createValidFilm();
        film.setReleaseDate(MIN_RELEASE_DATE.minusDays(1));

        ValidationException validationException = assertThrows(ValidationException.class,
                () -> filmController.createFilm(film));

        String errorMessage = "Дата релиза не может быть раньше " + MIN_RELEASE_DATE;
        assertEquals(errorMessage, validationException.getMessage());
        assertEquals(0, filmController.getAllFilms().size(),
                "Фильм с невалидной датой не должен быть сохранён");
    }

    @Test
    void shouldUpdateFilmWithValidData() {
        Film film = createValidFilm();
        Film createdFilm = filmController.createFilm(film);

        String newName = "Матрица: Перезагрузка";
        String newDescription = "Нео продолжает борьбу с машинами и ищет путь к спасению Зиона.";
        LocalDate newReleaseDate = LocalDate.of(2003, 5, 15);
        int newDuration = 138;

        Film filmForUpdate = Film.builder()
                .id(createdFilm.getId())
                .name(newName)
                .description(newDescription)
                .releaseDate(newReleaseDate)
                .duration(newDuration)
                .build();

        Film updatedFilm = filmController.updateFilm(filmForUpdate);

        assertEquals(newName, updatedFilm.getName(), "Ожидается новое название:" + newName);
        assertEquals(newDescription, updatedFilm.getDescription(), "Ожидается новое описание:" + newDescription);
        assertEquals(newReleaseDate, updatedFilm.getReleaseDate(), "Ожидается новая дата релиза:" + newReleaseDate);
        assertEquals(newDuration, updatedFilm.getDuration(), "Ожидается новая продолжительность:" + newDuration);
        assertEquals(createdFilm.getId(), updatedFilm.getId(), "Id фильма не должен измениться");
        assertEquals(1, filmController.getAllFilms().size(), "Ожидается общее количество фильмов 1");
    }

    @Test
    void shouldThrowValidationExceptionWhenFilmIdIsZero() {
        Film film = createValidFilm();
        filmController.createFilm(film);
        Film shadowFilm = createValidFilm();
        shadowFilm.setId(0L);

        ValidationException validationException = assertThrows(ValidationException.class,
                () -> filmController.updateFilm(shadowFilm));

        String errorMessage = "Id должен быть указан";
        assertEquals(errorMessage, validationException.getMessage());
        assertEquals(1, filmController.getAllFilms().size(),
                "Размер списка должен остаться без изменений");
        assertEquals(1, film.getId(), "Id фильма не должен измениться");
    }

    @Test
    void shouldThrowValidationExceptionWhenFilmIdIsNegative() {
        Film film = createValidFilm();
        filmController.createFilm(film);
        Film shadowFilm = createValidFilm();
        shadowFilm.setId(-1L);

        ValidationException validationException = assertThrows(ValidationException.class,
                () -> filmController.updateFilm(shadowFilm));

        String errorMessage = "Id должен быть указан";
        assertEquals(errorMessage, validationException.getMessage());
        assertEquals(1, filmController.getAllFilms().size(),
                "Размер списка должен остаться без изменений");
        assertEquals(1, film.getId(), "Id фильма не должен измениться");
    }

    @Test
    void shouldThrowNotFoundExceptionWhenFilmDoesNotExist() {
        Film film = createValidFilm();
        filmController.createFilm(film);
        Film shadowFilm = createValidFilm();
        shadowFilm.setId(999L);

        NotFoundException notFoundException = assertThrows(NotFoundException.class,
                () -> filmController.updateFilm(shadowFilm));

        String errorMessage = "Фильм с id = " + shadowFilm.getId() + " не найден";
        assertEquals(errorMessage, notFoundException.getMessage());
        assertEquals(1, filmController.getAllFilms().size(),
                "Размер списка должен остаться без изменений");
        assertEquals(1, film.getId(), "Id фильма не должен измениться");
    }

    @Test
    void shouldAssignIncrementalIdsWhenSeveralFilmsCreated() {
        Film firstFilm = createValidFilm();
        Film firstCreatedFilm = filmController.createFilm(firstFilm);

        Film secondFilm = Film.builder()
                .name("Матрица: Революция")
                .description("Последняя битва людей и машин приближает финал войны за Зион.")
                .releaseDate(LocalDate.of(2003, 11, 5))
                .duration(129)
                .build();

        Film secondCreatedFilm = filmController.createFilm(secondFilm);

        assertEquals(1, firstCreatedFilm.getId(), "Ожидается Id = 1");
        assertEquals(2, secondCreatedFilm.getId(), "Ожидается Id = 2");
        assertEquals(2, filmController.getAllFilms().size(),
                "Размер списка ожидается 2");
    }
}
