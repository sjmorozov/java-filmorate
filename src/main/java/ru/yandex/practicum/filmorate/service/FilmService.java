package ru.yandex.practicum.filmorate.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.Genre;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.storage.film.FilmStorage;
import ru.yandex.practicum.filmorate.storage.filmgenre.FilmGenreStorage;
import ru.yandex.practicum.filmorate.storage.filmlike.FilmLikeStorage;
import ru.yandex.practicum.filmorate.storage.genre.GenreStorage;
import ru.yandex.practicum.filmorate.storage.mparating.MpaRatingStorage;
import ru.yandex.practicum.filmorate.storage.user.UserStorage;

import java.time.LocalDate;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class FilmService {
    private static final LocalDate MIN_RELEASE_DATE = LocalDate.of(1895, 12, 28);
    private static final int MAX_DESCRIPTION_LENGTH = 200;

    private final FilmStorage filmStorage;
    private final UserStorage userStorage;
    private final MpaRatingStorage mpaRatingStorage;
    private final GenreStorage genreStorage;
    private final FilmGenreStorage filmGenreStorage;
    private final FilmLikeStorage filmLikeStorage;

    @Transactional
    public Film create(Film film) {
        validateReleaseDate(film.getReleaseDate());
        resolveMpa(film);
        resolveGenres(film);

        Film createdFilm = filmStorage.add(film);

        filmGenreStorage.replaceByFilmId(createdFilm.getId(), createdFilm.getGenres());

        log.info("Фильм добавлен: id={}, name={}", createdFilm.getId(), createdFilm.getName());
        return createdFilm;
    }

    @Transactional
    public Film update(Film film) {
        Film oldFilm = filmStorage.findById(film.getId());

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

        if (resolveMpa(film)) {
            oldFilm.setMpa(film.getMpa());
        }

        if (resolveGenres(film)) {
            oldFilm.setGenres(film.getGenres());
        }

        Film updatedFilm = filmStorage.update(oldFilm);

        if (film.getGenres() != null) {
            filmGenreStorage.replaceByFilmId(updatedFilm.getId(), updatedFilm.getGenres());
        }

        log.info("Фильм обновлён: id = {}, name = {}", updatedFilm.getId(), updatedFilm.getName());
        return updatedFilm;
    }

    public void delete(Long id) {
        filmStorage.delete(id);
        log.info("Фильм с id = {} удалён", id);
    }

    public Film findById(Long id) {
        Film film = filmStorage.findById(id);
        loadFilmRelations(List.of(film));
        return film;
    }

    public Collection<Film> findAll() {
        Collection<Film> films = filmStorage.findAll();
        loadFilmRelations(films);
        return films;
    }

    public void addLike(Long filmId, Long userId) {
        Film film = filmStorage.findById(filmId);
        User user = userStorage.findById(userId);

        filmLikeStorage.add(film.getId(), user.getId());

        log.info("Пользователь {} поставил лайк фильму {}", user.getName(), film.getName());
    }

    public void deleteLike(Long filmId, Long userId) {
        Film film = filmStorage.findById(filmId);
        User user = userStorage.findById(userId);

        filmLikeStorage.delete(film.getId(), user.getId());

        log.info("Пользователь {} убрал лайк с фильма {}", user.getName(), film.getName());
    }

    public Collection<Film> findPopular(int count) {
        List<Long> popularFilmIds = filmLikeStorage.findPopularFilmIds(count);
        Map<Long, Film> filmsById = filmStorage.findByIds(popularFilmIds).stream()
                .collect(Collectors.toMap(Film::getId, Function.identity()));
        List<Film> popularFilms = popularFilmIds.stream()
                .map(filmsById::get)
                .toList();

        loadFilmRelations(popularFilms);

        return popularFilms;
    }

    private void validateReleaseDate(LocalDate releaseDate) {
        if (releaseDate.isBefore(MIN_RELEASE_DATE)) {
            log.warn("Дата релиза {} раньше чем {}", releaseDate, MIN_RELEASE_DATE);
            throw new ValidationException("Дата релиза не может быть раньше " + MIN_RELEASE_DATE);
        }
    }

    private boolean resolveMpa(Film film) {
        if (film.getMpa() != null && film.getMpa().getId() != null) {
            film.setMpa(mpaRatingStorage.findById(film.getMpa().getId()));
            return true;
        }

        return false;
    }

    private boolean resolveGenres(Film film) {
        if (film.getGenres() != null) {
            LinkedHashSet<Integer> genreIds = film.getGenres().stream()
                    .map(Genre::getId)
                    .collect(Collectors.toCollection(LinkedHashSet::new));
            Set<Genre> genres = genreStorage.findByIds(genreIds);
            validateAllGenresFound(genreIds, genres);
            film.setGenres(genres);
            return true;
        }

        return false;
    }

    private void loadFilmRelations(Collection<Film> films) {
        if (films.isEmpty()) {
            return;
        }

        List<Long> filmIds = getFilmIds(films);
        Map<Long, Set<Genre>> genresByFilmId = filmGenreStorage.findByFilmIds(filmIds);
        Map<Long, Set<Long>> likesByFilmId = filmLikeStorage.findUserIdsByFilmIds(filmIds);

        films.forEach(film -> {
            Long filmId = film.getId();
            film.setGenres(genresByFilmId.getOrDefault(filmId, new LinkedHashSet<>()));
            film.setLikes(likesByFilmId.getOrDefault(filmId, new LinkedHashSet<>()));
        });
    }

    private List<Long> getFilmIds(Collection<Film> films) {
        return films.stream()
                .map(Film::getId)
                .toList();
    }

    private void validateAllGenresFound(Collection<Integer> requestedGenreIds, Collection<Genre> foundGenres) {
        Set<Integer> foundGenreIds = foundGenres.stream()
                .map(Genre::getId)
                .collect(Collectors.toSet());

        requestedGenreIds.stream()
                .filter(id -> !foundGenreIds.contains(id))
                .findFirst()
                .ifPresent(id -> {
                    throw new NotFoundException("Жанр с id = " + id + " не найден");
                });
    }
}
