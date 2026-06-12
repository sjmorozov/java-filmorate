package ru.yandex.practicum.filmorate.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.storage.film.FilmStorage;

import java.time.LocalDate;
import java.util.Collection;

@Slf4j
@Service
public class FilmService {
    private static final LocalDate MIN_RELEASE_DATE = LocalDate.of(1895, 12, 28);
    private static final int MAX_DESCRIPTION_LENGTH = 200;

    private final FilmStorage filmStorage;

    @Autowired
    public FilmService(FilmStorage filmStorage) {
        this.filmStorage = filmStorage;
    }

    public Film createFilm(Film film) {
        validateReleaseDate(film.getReleaseDate());

        Film createdFilm = filmStorage.addFilm(film);

        log.info("Фильм добавлен: id={}, name={}", createdFilm.getId(), createdFilm.getName());
        return createdFilm;
    }

    public Film updateFilm(Film film) {
        if (film.getId() == null || film.getId() <= 0) {
            log.warn("Некорректный id фильма: {}", film.getId());
            throw new ValidationException("Id должен быть указан");
        }

        Film oldFilm = filmStorage.findFilmById(film.getId());

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

        Film updatedFilm = filmStorage.updateFilm(oldFilm);
        log.info("Фильм обновлён: id = {}, name = {}", updatedFilm.getId(), updatedFilm.getName());
        return updatedFilm;
    }

    public void deleteFilm(Long id) {
        filmStorage.deleteFilm(id);
    }

    public Film findFilmById(Long id) {
        return filmStorage.findFilmById(id);
    }

    public Collection<Film> getAllFilms() {
        return filmStorage.findAllFilms();
    }

    private void validateReleaseDate(LocalDate releaseDate) {

        if (releaseDate.isBefore(MIN_RELEASE_DATE)) {
            log.warn("Дата релиза {} раньше чем {}", releaseDate, MIN_RELEASE_DATE);
            throw new ValidationException("Дата релиза не может быть раньше " + MIN_RELEASE_DATE);
        }
    }
}
