package ru.yandex.practicum.filmorate.controller;

import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.Film;

import java.time.LocalDate;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/films")
public class FilmController {
    private static final LocalDate MIN_RELEASE_DATE = LocalDate.of(1895, 12, 28);
    private static final int MAX_DESCRIPTION_LENGTH = 200;

    private final Map<Integer, Film> films = new HashMap<>();

    @GetMapping
    public Collection<Film> getAllFilms() {
        return films.values();
    }

    @PostMapping
    public Film createFilm(@Valid @RequestBody Film film) {
        validateReleaseDate(film.getReleaseDate());

        film.setId(getNextId());
        films.put(film.getId(), film);
        log.info("Фильм добавлен: id={}, name={}", film.getId(), film.getName());
        return film;
    }

    private int getNextId() {
        int currentMaxId = films.keySet()
                .stream()
                .mapToInt(id -> id)
                .max()
                .orElse(0);
        return ++currentMaxId;
    }

    @PutMapping
    public Film updateFilm(@RequestBody Film film) {
        if (film.getId() == null || film.getId() <= 0) {
            log.warn("Некорректный id фильма: {}", film.getId());
            throw new ValidationException("Id должен быть указан");
        }

        if (!films.containsKey(film.getId())) {
            log.warn("Фильм с id = {} не найден", film.getId());
            throw new NotFoundException("Фильм с id = " + film.getId() + " не найден");
        }

        Film oldFilm = films.get(film.getId());

        if (film.getName() != null) {
            if (film.getName().isBlank()) {
                log.warn("Название не указано");
                throw new ValidationException("Название должно быть указано");
            }
            oldFilm.setName(film.getName());
        }

        if (film.getDescription() != null) {
            if (film.getDescription().length() > MAX_DESCRIPTION_LENGTH) {
                log.warn("Длина описания {} превышает максимальную в {} символов",
                        film.getDescription().length(), MAX_DESCRIPTION_LENGTH);
                throw new ValidationException("Максимальная длина описания — " + MAX_DESCRIPTION_LENGTH + " символов");
            }
            oldFilm.setDescription(film.getDescription());
        }

        if (film.getReleaseDate() != null) {
            validateReleaseDate(film.getReleaseDate());
            oldFilm.setReleaseDate(film.getReleaseDate());
        }

        if (film.getDuration() != null) {
            if (film.getDuration() <= 0) {
                log.warn("Указанная продолжительность фильма {} не является положительным числом", film.getDuration());
                throw new ValidationException("Продолжительность фильма должна быть положительным числом");
            }
            oldFilm.setDuration(film.getDuration());
        }

        log.info("Фильм обновлён: id = {}, name = {}", oldFilm.getId(), oldFilm.getName());
        return oldFilm;
    }

    private void validateReleaseDate(LocalDate releaseDate) {

        if (releaseDate.isBefore(MIN_RELEASE_DATE)) {
            log.warn("Дата релиза {} раньше чем {}", releaseDate, MIN_RELEASE_DATE);
            throw new ValidationException("Дата релиза не может быть раньше " + MIN_RELEASE_DATE);
        }
    }
}
