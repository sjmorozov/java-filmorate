package ru.yandex.practicum.filmorate.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.Director;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.Genre;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.storage.director.DirectorStorage;
import ru.yandex.practicum.filmorate.storage.film.FilmStorage;
import ru.yandex.practicum.filmorate.storage.filmdirector.FilmDirectorStorage;
import ru.yandex.practicum.filmorate.storage.filmgenre.FilmGenreStorage;
import ru.yandex.practicum.filmorate.storage.filmlike.FilmLikeStorage;
import ru.yandex.practicum.filmorate.storage.genre.GenreStorage;
import ru.yandex.practicum.filmorate.storage.mparating.MpaRatingStorage;
import ru.yandex.practicum.filmorate.storage.user.UserStorage;

import java.time.LocalDate;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class FilmService {
    private static final LocalDate MIN_RELEASE_DATE = LocalDate.of(1895, 12, 28);
    private static final int MAX_DESCRIPTION_LENGTH = 200;
    private static final String SORT_BY_YEAR = "year";
    private static final String SORT_BY_LIKES = "likes";
    private static final String SEARCH_BY_TITLE = "title";
    private static final String SEARCH_BY_DIRECTOR = "director";

    private final FilmStorage filmStorage;
    private final UserStorage userStorage;
    private final MpaRatingStorage mpaRatingStorage;
    private final GenreStorage genreStorage;
    private final FilmGenreStorage filmGenreStorage;
    private final DirectorStorage directorStorage;
    private final FilmDirectorStorage filmDirectorStorage;
    private final FilmLikeStorage filmLikeStorage;

    @Transactional
    public Film create(Film film) {
        validateReleaseDate(film.getReleaseDate());
        resolveMpa(film);
        resolveGenres(film);
        if (!resolveDirectors(film)) {
            film.setDirectors(new LinkedHashSet<>());
        }

        Film createdFilm = filmStorage.add(film);

        filmGenreStorage.replaceByFilmId(createdFilm.getId(), createdFilm.getGenres());
        filmDirectorStorage.replaceByFilmId(createdFilm.getId(), createdFilm.getDirectors());

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

        if (resolveDirectors(film)) {
            oldFilm.setDirectors(film.getDirectors());
        }

        Film updatedFilm = filmStorage.update(oldFilm);

        if (film.getGenres() != null) {
            filmGenreStorage.replaceByFilmId(updatedFilm.getId(), updatedFilm.getGenres());
        }

        if (film.getDirectors() != null) {
            filmDirectorStorage.replaceByFilmId(updatedFilm.getId(), updatedFilm.getDirectors());
        }

        loadFilmRelations(List.of(updatedFilm));

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

    /**
     * Возвращает топ-N фильмов по количеству лайков с опциональной фильтрацией по жанру и году релиза.
     *
     * @param count   максимальное число фильмов в ответе
     * @param genreId если не null, в результат попадут только фильмы этого жанра
     * @param year    если не null, в результат попадут только фильмы с этим годом релиза
     * @throws NotFoundException   если жанр с genreId не существует
     * @throws ValidationException если year выходит за диапазон [1895, текущий год]
     */
    public Collection<Film> findPopular(int count, Integer genreId, Integer year) {
        validateGenreExists(genreId);
        validatePopularYear(year);

        List<Long> popularFilmIds = filmLikeStorage.findPopularFilmIds(count, genreId, year);
        Map<Long, Film> filmsById = filmStorage.findByIds(popularFilmIds).stream()
                .collect(Collectors.toMap(Film::getId, Function.identity()));
        List<Film> popularFilms = popularFilmIds.stream()
                .map(filmsById::get)
                .toList();

        loadFilmRelations(popularFilms);

        return popularFilms;
    }

    public Collection<Film> findByDirector(Long directorId, String sortBy) {
        validateDirectorFilmSort(sortBy);
        directorStorage.findById(directorId);

        List<Long> filmIds = filmDirectorStorage.findFilmIdsByDirectorId(directorId, sortBy);
        Map<Long, Film> filmsById = filmStorage.findByIds(filmIds).stream()
                .collect(Collectors.toMap(Film::getId, Function.identity()));
        List<Film> films = filmIds.stream()
                .map(filmsById::get)
                .toList();

        loadFilmRelations(films);
        return films;
    }

    /**
     * Проверяет существование жанра, если он указан.
     *
     * @throws NotFoundException если жанр с genreId не найден
     */
    private void validateGenreExists(Integer genreId) {
        if (genreId != null) {
            genreStorage.findById(genreId);
        }
    }

    /**
     * Проверяет, что год, если он указан, не раньше появления кино и не позже текущего года.
     *
     * @throws ValidationException если year вне диапазона [1895, текущий год]
     */
    private void validatePopularYear(Integer year) {
        if (year == null) {
            return;
        }

        int currentYear = LocalDate.now().getYear();

        if (year < MIN_RELEASE_DATE.getYear() || year > currentYear) {
            log.warn("Год {} вне диапазона {}..{}", year, MIN_RELEASE_DATE.getYear(), currentYear);
            throw new ValidationException(
                    "Параметр year должен быть в диапазоне от " + MIN_RELEASE_DATE.getYear() + " до " + currentYear);
        }
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

    private void validateDirectorFilmSort(String sortBy) {
        if (!SORT_BY_YEAR.equals(sortBy) && !SORT_BY_LIKES.equals(sortBy)) {
            throw new ValidationException("Параметр sortBy должен иметь значение year или likes");
        }
    }

    private boolean resolveDirectors(Film film) {
        if (film.getDirectors() != null) {
            LinkedHashSet<Long> directorIds = film.getDirectors().stream()
                    .map(director -> director == null ? null : director.getId())
                    .collect(Collectors.toCollection(LinkedHashSet::new));

            if (directorIds.contains(null)) {
                throw new NotFoundException("Режиссёр с id = null не найден");
            }

            Set<Director> directors = directorStorage.findByIds(directorIds);
            validateAllDirectorsFound(directorIds, directors);
            film.setDirectors(directors);
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
        Map<Long, Set<Director>> directorsByFilmId = filmDirectorStorage.findByFilmIds(filmIds);
        Map<Long, Set<Long>> likesByFilmId = filmLikeStorage.findUserIdsByFilmIds(filmIds);

        films.forEach(film -> {
            Long filmId = film.getId();
            film.setGenres(genresByFilmId.getOrDefault(filmId, new LinkedHashSet<>()));
            film.setDirectors(directorsByFilmId.getOrDefault(filmId, new LinkedHashSet<>()));
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

    private void validateAllDirectorsFound(Collection<Long> requestedDirectorIds,
                                           Collection<Director> foundDirectors) {
        Set<Long> foundDirectorIds = foundDirectors.stream()
                .map(Director::getId)
                .collect(Collectors.toSet());

        requestedDirectorIds.stream()
                .filter(id -> !foundDirectorIds.contains(id))
                .findFirst()
                .ifPresent(id -> {
                    throw new NotFoundException("Режиссёр с id = " + id + " не найден");
                });
    }

    public Collection<Film> getRecommendations(Long userId) {
        Set<Long> userLikedFilmIds = filmLikeStorage.findFilmIdsByUserId(userId);

        if (userLikedFilmIds.isEmpty()) {
            return List.of();
        }

        MatrixData matrixData = buildCoOccurrenceMatrix();

        Map<Long, Double> filmScores = calculateRecommendationScores(userLikedFilmIds, matrixData);

        if (filmScores.isEmpty()) {
            return List.of();
        }

        return loadAndSortFilms(filmScores);
    }

    private static class MatrixData {
        final Map<Long, Integer> filmLikeCounts;
        final Map<Long, Map<Long, Integer>> coOccurrences;

        MatrixData(Map<Long, Integer> filmLikeCounts, Map<Long, Map<Long, Integer>> coOccurrences) {
            this.filmLikeCounts = filmLikeCounts;
            this.coOccurrences = coOccurrences;
        }
    }

    private MatrixData buildCoOccurrenceMatrix() {
        Map<Long, Set<Long>> allLikesByUser = filmLikeStorage.findAllFilmIdsGroupedByUser();
        Map<Long, Integer> filmLikeCounts = new HashMap<>();
        Map<Long, Map<Long, Integer>> coOccurrences = new HashMap<>();

        for (Set<Long> likedFilms : allLikesByUser.values()) {
            List<Long> likedFilmsList = new ArrayList<>(likedFilms);

            for (int i = 0; i < likedFilmsList.size(); i++) {
                Long filmI = likedFilmsList.get(i);
                filmLikeCounts.merge(filmI, 1, Integer::sum);

                for (int j = i + 1; j < likedFilmsList.size(); j++) {
                    Long filmJ = likedFilmsList.get(j);

                    coOccurrences
                            .computeIfAbsent(filmI, k -> new HashMap<>())
                            .merge(filmJ, 1, Integer::sum);
                    coOccurrences
                            .computeIfAbsent(filmJ, k -> new HashMap<>())
                            .merge(filmI, 1, Integer::sum);
                }
            }
        }

        return new MatrixData(filmLikeCounts, coOccurrences);
    }

    private Map<Long, Double> calculateRecommendationScores(
            Set<Long> userLikedFilmIds,
            MatrixData matrixData) {

        Map<Long, Double> filmScores = new HashMap<>();

        for (Long candidateFilmId : matrixData.filmLikeCounts.keySet()) {
            if (userLikedFilmIds.contains(candidateFilmId)) {
                continue;
            }

            double score = calculateScoreForFilm(candidateFilmId, userLikedFilmIds, matrixData);

            if (score > 0) {
                filmScores.put(candidateFilmId, score);
            }
        }

        return filmScores;
    }

    private double calculateScoreForFilm(
            Long candidateFilmId,
            Set<Long> userLikedFilmIds,
            MatrixData matrixData) {

        double score = 0.0;

        for (Long likedFilmId : userLikedFilmIds) {
            int coOccurrence = matrixData.coOccurrences
                    .getOrDefault(likedFilmId, Collections.emptyMap())
                    .getOrDefault(candidateFilmId, 0);

            int totalLikes = matrixData.filmLikeCounts.get(likedFilmId);

            if (totalLikes > 0) {
                score += (double) coOccurrence / totalLikes;
            }
        }

        return score;
    }

    private List<Film> loadAndSortFilms(Map<Long, Double> filmScores) {
        List<Long> filmIds = new ArrayList<>(filmScores.keySet());

        Map<Long, Film> filmsById = filmStorage.findByIds(filmIds).stream()
                .collect(Collectors.toMap(Film::getId, Function.identity()));

        List<Film> films = filmIds.stream()
                .map(filmsById::get)
                .filter(Objects::nonNull)
                .toList();

        loadFilmRelations(films);

        return films.stream()
                .sorted(Comparator
                        .<Film, Double>comparing(f -> filmScores.get(f.getId()), Comparator.reverseOrder())
                        .thenComparing(Film::getName, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    public Collection<Film> findCommonFilms(Long userId, Long friendId) {
        userStorage.findById(userId);
        userStorage.findById(friendId);

        List<Long> commonFilmIds = filmLikeStorage.findCommonFilmIdsSortedByPopularity(userId, friendId);

        if (commonFilmIds.isEmpty()) {
            return List.of();
        }

        Map<Long, Film> filmsById = filmStorage.findByIds(commonFilmIds).stream()
                .collect(Collectors.toMap(Film::getId, Function.identity()));

        List<Film> commonFilms = commonFilmIds.stream()
                .map(filmsById::get)
                .filter(Objects::nonNull)
                .toList();

        loadFilmRelations(commonFilms);

        return commonFilms;
    }

    /**
     * Ищет фильмы по подстроке в названии и/или в имени режиссёра, отсортированные по популярности.
     *
     * @param query текст для поиска
     * @param by    "title", "director" или оба через запятую — по каким полям искать
     * @throws ValidationException если query пустой или by содержит недопустимые значения
     */
    public Collection<Film> search(String query, String by) {
        if (query == null || query.isBlank()) {
            throw new ValidationException("Параметр query должен быть указан");
        }

        Set<String> searchFields = parseSearchFields(by);
        boolean searchByTitle = searchFields.contains(SEARCH_BY_TITLE);
        boolean searchByDirector = searchFields.contains(SEARCH_BY_DIRECTOR);

        List<Long> filmIds = filmLikeStorage.findSearchFilmIds(query, searchByTitle, searchByDirector);
        Map<Long, Film> filmsById = filmStorage.findByIds(filmIds).stream()
                .collect(Collectors.toMap(Film::getId, Function.identity()));
        List<Film> films = filmIds.stream()
                .map(filmsById::get)
                .toList();

        loadFilmRelations(films);

        return films;
    }

    private Set<String> parseSearchFields(String by) {
        if (by == null || by.isBlank()) {
            throw new ValidationException("Параметр by должен быть указан");
        }

        Set<String> fields = Arrays.stream(by.split(","))
                .map(String::trim)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        Set<String> unknownFields = fields.stream()
                .filter(field -> !SEARCH_BY_TITLE.equals(field) && !SEARCH_BY_DIRECTOR.equals(field))
                .collect(Collectors.toSet());

        if (!unknownFields.isEmpty()) {
            throw new ValidationException(
                    "Параметр by может содержать только значения " + SEARCH_BY_TITLE + ", " + SEARCH_BY_DIRECTOR
                            + ", получено: " + unknownFields);
        }

        return fields;
    }
}
