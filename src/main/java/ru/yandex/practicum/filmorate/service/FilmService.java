package ru.yandex.practicum.filmorate.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.storage.film.FilmStorage;
import ru.yandex.practicum.filmorate.storage.user.UserStorage;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Comparator;
import java.util.Set;

@Slf4j
@Service
public class FilmService {
    private static final LocalDate MIN_RELEASE_DATE = LocalDate.of(1895, 12, 28);
    private static final int MAX_DESCRIPTION_LENGTH = 200;

    private final FilmStorage filmStorage;
    private final UserStorage userStorage;

    @Autowired
    public FilmService(FilmStorage filmStorage, UserStorage userStorage) {
        this.filmStorage = filmStorage;
        this.userStorage = userStorage;
    }

    public Film createFilm(Film film) {
        validateReleaseDate(film.getReleaseDate());

        Film createdFilm = filmStorage.addFilm(film);

        log.info("Фильм добавлен: id={}, name={}", createdFilm.getId(), createdFilm.getName());
        return createdFilm;
    }

    public Film updateFilm(Film film) {
        validateId(film.getId());

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
        validateId(id);
        filmStorage.deleteFilm(id);
    }

    public Film findFilmById(Long id) {
        validateId(id);
        return filmStorage.findFilmById(id);
    }

    public Collection<Film> getAllFilms() {
        return filmStorage.findAllFilms();
    }

    public void likeFilm(Long filmId, Long userId) {
        validateId(filmId);
        validateId(userId);

        Film film = filmStorage.findFilmById(filmId);
        User user = userStorage.findUserById(userId);

        film.getLikes().add(userId);

        Film likedFilm = filmStorage.updateFilm(film);
        log.info("Пользователь {} поставил лайк фильму {}", user.getName(), likedFilm.getName());
    }

    public void deleteLike(Long filmId, Long userId) {
        validateId(filmId);
        validateId(userId);

        Film film = filmStorage.findFilmById(filmId);
        User user = userStorage.findUserById(userId);

        film.getLikes().remove(userId);

        Film unlikedFilm = filmStorage.updateFilm(film);
        log.info("Пользователь {} убрал лайк с фильма {}", user.getName(), unlikedFilm.getName());
    }

    public Collection<Film> getPopularFilms(int count) {
        if (count <= 0) {
            throw new ValidationException("Параметр count должен быть больше нуля. Передан count = " + count);
        }
        Comparator<Film> likesComparator = Comparator.comparingInt(film -> film.getLikes().size());
        return getAllFilms().stream()
                .sorted(likesComparator.reversed())
                .limit(count)
                .toList();
    }

    private void validateReleaseDate(LocalDate releaseDate) {

        if (releaseDate.isBefore(MIN_RELEASE_DATE)) {
            log.warn("Дата релиза {} раньше чем {}", releaseDate, MIN_RELEASE_DATE);
            throw new ValidationException("Дата релиза не может быть раньше " + MIN_RELEASE_DATE);
        }
    }

    private void validateId(Long id) {
        if (id == null || id <= 0) {
            log.warn("Указан невалидный Id = {}", id);
            throw new ValidationException("Id должен быть указан");
        }
    }
}
